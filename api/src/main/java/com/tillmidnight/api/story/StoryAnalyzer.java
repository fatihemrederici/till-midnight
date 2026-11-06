// Bu dosyanın ait olduğu paket.
package com.tillmidnight.api.story;

// Değiştirilebilir küme.
import java.util.HashSet;
// Sıralı anahtar-değer tablosu.
import java.util.LinkedHashMap;
// Liste arayüzü.
import java.util.List;
// Anahtar-değer arayüzü.
import java.util.Map;
// Null kontrolü.
import java.util.Objects;
// Küme arayüzü.
import java.util.Set;
// Akış: Listeleri birleştirmek için.
import java.util.stream.Stream;
// Spring'in "bu sınıfı sen yönet" etiketi.
import org.springframework.stereotype.Component;

// Tüm seçim yollarını gerçek motorla dener. AI ileride hikâye yazdığında bozuk hikâyeyi bu yakalar.
@Component
public class StoryAnalyzer {

    // Bu kadar yoldan fazlası sunucuyu yorar, böyle hikâye kabul edilmez.
    public static final int MAX_PATHS = 200_000;

    // Gerçek motor. Kurallar iki yerde yazılmaz.
    private final StoryEngine engine;

    // Spring motoru buraya kendisi verir.
    public StoryAnalyzer(StoryEngine engine) {
        // Motoru sakla.
        this.engine = engine;
    }

    // Hikâyeyi analiz eder.
    public StoryAnalysis analyze(Story story) {
        // Yürüyüş sırasında biriken veriler.
        Walk walk = new Walk();
        // Baştan yürümeye başla.
        visit(story, engine.start(story), 0, walk);

        // Hiç görülmeyen kartları bul.
        List<String> unreachableCards = Stream.concat(
                        story.cards().stream(),
                        story.meters().stream().map(Story.Meter::crisis).filter(Objects::nonNull))
                .map(Story.Card::id)
                .filter(id -> !walk.seenCards.contains(id))
                .toList();
        // Hiç ulaşılmayan sonları bul.
        List<String> unreachableEndings = story.endings().stream()
                .map(Story.Ending::id)
                .filter(id -> !walk.endingCounts.containsKey(id))
                .toList();
        // Kurtuluşla biten yolları say.
        int survived = story.endings().stream()
                .filter(Story.Ending::won)
                .mapToInt(ending -> walk.endingCounts.getOrDefault(ending.id(), 0))
                .sum();
        // Raporu oluştur.
        return new StoryAnalysis(
                walk.paths,
                walk.longest,
                walk.endingCounts,
                unreachableCards,
                unreachableEndings,
                walk.paths == 0 ? 0 : Math.round(100f * survived / walk.paths));
    }

    // Kendini çağırarak iki yönü de dener. Buna depth-first search denir.
    private void visit(Story story, StoryEngine.State state, int depth, Walk walk) {
        // Oyun bittiyse yolu say.
        if (state.endingId() != null) {
            // Yol sayısını artır.
            walk.paths++;
            // En uzun yolu güncelle.
            walk.longest = Math.max(walk.longest, depth);
            // Bu sonun sayısını artır.
            walk.endingCounts.merge(state.endingId(), 1, Integer::sum);
            // Çok fazla yol varsa dur.
            if (walk.paths > MAX_PATHS) {
                throw new IllegalArgumentException("Story has more than " + MAX_PATHS + " paths");
            }
            // Bu dal bitti.
            return;
        }
        // Bu kart görüldü.
        walk.seenCards.add(engine.currentCard(story, state).id());
        // Sadece seçilebilen tarafları dene: Kilitli seçenek oynanamaz.
        for (StoryEngine.Side side : StoryEngine.Side.values()) {
            if (engine.allowed(story, state, side)) {
                visit(story, engine.step(story, state, side), depth + 1, walk);
            }
        }
    }

    // Yürüyüşün biriken verileri.
    private static final class Walk {
        // Toplam yol.
        int paths;
        // En uzun yol.
        int longest;
        // Son -> yol sayısı.
        final Map<String, Integer> endingCounts = new LinkedHashMap<>();
        // Görülen kartlar.
        final Set<String> seenCards = new HashSet<>();
    }
}
