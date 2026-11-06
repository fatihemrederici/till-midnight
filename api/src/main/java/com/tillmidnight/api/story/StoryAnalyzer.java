// Bu dosyanın ait olduğu paket.
package com.tillmidnight.api.story;

// Değiştirilebilir anahtar-değer tablosu.
import java.util.HashMap;
// Değiştirilebilir küme.
import java.util.HashSet;
// Sıralı anahtar-değer tablosu.
import java.util.LinkedHashMap;
// Liste arayüzü.
import java.util.List;
// Anahtar-değer arayüzü.
import java.util.Map;
// Küme arayüzü.
import java.util.Set;
// Sıralı tablo ve küme: Durum anahtarı her seferinde aynı sırada yazılsın diye.
import java.util.TreeMap;
import java.util.TreeSet;
// Spring'in "bu sınıfı sen yönet" etiketi.
import org.springframework.stereotype.Component;

// Tüm seçim yollarını gerçek motorla dener ve her sonun olasılığını hesaplar.
// Aynı duruma farklı yollardan gelinebildiği için her durum bir kez hesaplanır ve saklanır (memoization).
// Böylece milyonlarca yol saniyeler içinde değil, milisaniyeler içinde sayılır.
@Component
public class StoryAnalyzer {

    // Gerçek motor. Kurallar iki yerde yazılmaz.
    private final StoryEngine engine;

    // Spring motoru buraya kendisi verir.
    public StoryAnalyzer(StoryEngine engine) {
        // Motoru sakla.
        this.engine = engine;
    }

    // Bir durumdan sonrasının özeti: hangi son ne olasılıkla, kaç yol, en uzun yol.
    private record Outcome(Map<String, Double> chance, long paths, int longest) {}

    // Hikâyeyi analiz eder.
    public StoryAnalysis analyze(Story story) {
        // Daha önce hesaplanan durumlar.
        Map<String, Outcome> memo = new HashMap<>();
        // Görülen kartlar.
        Set<String> seenCards = new HashSet<>();
        // Baştan hesapla.
        Outcome all = visit(story, engine.start(story), memo, seenCards);

        // Hiç görülmeyen kartları bul.
        List<String> unreachableCards = story.cards().stream()
                .map(Story.Card::id)
                .filter(id -> !seenCards.contains(id))
                .toList();
        // Göstergelerin yer ölümleri bir güvenlik ağıdır: Örneğin işte akıl hiç bitmeyebilir, bu bir hata değil.
        Set<String> safetyNets = new HashSet<>();
        story.meters().forEach(meter -> safetyNets.addAll(meter.endings().values()));
        // Hiç ulaşılmayan sonları bul.
        List<String> unreachableEndings = story.endings().stream()
                .map(Story.Ending::id)
                .filter(id -> !all.chance().containsKey(id) && !safetyNets.contains(id))
                .toList();
        // Kurtuluş olasılığı.
        double survived = story.endings().stream()
                .filter(Story.Ending::won)
                .mapToDouble(ending -> all.chance().getOrDefault(ending.id(), 0.0))
                .sum();
        // Yüzdeye çevir, dosyadaki son sırasıyla.
        Map<String, Double> percent = new LinkedHashMap<>();
        story.endings().forEach(ending -> percent.put(ending.id(), round(all.chance().getOrDefault(ending.id(), 0.0))));
        // Raporu oluştur.
        return new StoryAnalysis(all.paths(), all.longest(), percent, unreachableCards, unreachableEndings, round(survived));
    }

    // Bir durumdan sonrasını hesaplar. Kendini çağırarak iki yönü de dener.
    private Outcome visit(Story story, StoryEngine.State state, Map<String, Outcome> memo, Set<String> seenCards) {
        // Oyun bittiyse tek bir yol, kesin bir son.
        if (state.endingId() != null) {
            return new Outcome(Map.of(state.endingId(), 1.0), 1, 0);
        }
        // Bu durum daha önce hesaplandıysa tekrar etme.
        String key = key(state);
        Outcome known = memo.get(key);
        if (known != null) {
            return known;
        }
        // Bu kart görüldü.
        seenCards.add(engine.currentCard(story, state).id());
        // İki yönün sonuçları.
        Outcome left = visit(story, engine.step(story, state, StoryEngine.Side.LEFT), memo, seenCards);
        Outcome right = visit(story, engine.step(story, state, StoryEngine.Side.RIGHT), memo, seenCards);
        // Rastgele oyuncu iki yönü yarı yarıya seçer.
        Map<String, Double> chance = new HashMap<>();
        left.chance().forEach((id, p) -> chance.merge(id, p / 2, Double::sum));
        right.chance().forEach((id, p) -> chance.merge(id, p / 2, Double::sum));
        // Sonucu sakla ve döndür.
        Outcome outcome = new Outcome(chance, left.paths() + right.paths(), 1 + Math.max(left.longest(), right.longest()));
        memo.put(key, outcome);
        return outcome;
    }

    // Durumu tek bir metne çevirir: kart, göstergeler ve doğru olan işaretler.
    private String key(StoryEngine.State state) {
        // Doğru olan işaretler, sıralı.
        Set<String> on = new TreeSet<>();
        state.flags().forEach((flag, value) -> {
            if (value) {
                on.add(flag);
            }
        });
        // Göstergeler, sıralı.
        return state.index() + "|" + new TreeMap<>(state.meters()) + "|" + on;
    }

    // Olasılığı yüzdeye çevirir, bir ondalıkla.
    private static double round(double chance) {
        // Örnek: 0.1234 -> 12.3
        return Math.round(chance * 1000) / 10.0;
    }
}
