// Bu dosyanın ait olduğu paket.
package com.tillmidnight.api.story;

// Değiştirilebilir anahtar-değer tablosu.
import java.util.HashMap;
// Liste arayüzü.
import java.util.List;
// Anahtar-değer arayüzü.
import java.util.Map;
// Spring'in "bu sınıfı sen yönet" etiketi.
import org.springframework.stereotype.Component;

// Hikâye motoru. Deterministik: aynı seçimler her zaman aynı sonu verir.
// Web'deki engine.ts dosyası aynı kuralların kopyasıdır; ikisi aynı test dosyasıyla kontrol edilir.
@Component
public class StoryEngine {

    // Kaydırma yönü.
    public enum Side { LEFT, RIGHT }

    // Oyunun anlık hali. index: sıradaki kart. endingId doluysa oyun bitti.
    public record State(Map<String, Boolean> flags, Map<String, Integer> meters, int index, String endingId) {}

    // Koşunun sonucu.
    public record Result(String endingId, boolean won, int cardsPlayed) {}

    // Yeni bir oyun başlatır.
    public State start(Story story) {
        // Boş işaretler, başlangıç göstergeleri, ilk uygun kart.
        Map<String, Boolean> flags = Map.of();
        return new State(flags, story.start(), nextCard(story, flags, 0), null);
    }

    // Bir seçimi uygular ve yeni durumu döndürür. Eski durum değişmez.
    public State step(Story story, State state, Side side) {
        // Şu anki kart.
        Story.Card card = story.cards().get(state.index());
        // Seçilen taraf.
        Story.Choice choice = side == Side.LEFT ? card.left() : card.right();

        // Ölüm koşulu, seçimin işaretleri eklenmeden önceki duruma bakar.
        Story.Death death = choice.death();
        // Koşul tutuyorsa oyun biter.
        if (death != null && holds(death.when(), state.flags())) {
            return new State(state.flags(), state.meters(), state.index(), death.ending());
        }

        // İşaretleri kopyala ve seçimin işaretlerini ekle.
        Map<String, Boolean> flags = new HashMap<>(state.flags());
        // set boş olabilir.
        if (choice.set() != null) {
            flags.putAll(choice.set());
        }

        // Göstergeleri kopyala ve etkileri uygula.
        Map<String, Integer> meters = new HashMap<>(state.meters());
        // effects boş olabilir.
        if (choice.effects() != null) {
            // Her etkiyi dön.
            for (Map.Entry<String, Integer> effect : choice.effects().entrySet()) {
                // 0 ile en yüksek değer arasında tut.
                int value = meters.get(effect.getKey()) + effect.getValue();
                meters.put(effect.getKey(), Math.min(story.maxMeter(), Math.max(0, value)));
            }
        }

        // Sıfıra inen gösterge varsa, dosyadaki sırayla ilki oyunu bitirir.
        for (Story.Meter meter : story.meters()) {
            if (meters.get(meter.id()) <= 0) {
                return new State(flags, meters, state.index(), meter.ending());
            }
        }

        // Sıradaki uygun kartı bul.
        int next = nextCard(story, flags, state.index() + 1);
        // Kart kalmadıysa gece yarısına ulaşıldı.
        if (next < 0) {
            return new State(flags, meters, state.index(), winningEnding(story, flags));
        }
        // Oyun sürüyor.
        return new State(flags, meters, next, null);
    }

    // Bir seçim listesini baştan oynatır. Sunucu, tarayıcının sonucuna güvenmek yerine bunu çalıştırır.
    public Result play(Story story, List<Side> choices) {
        // Oyunu başlat.
        State state = start(story);
        // Kaç kart oynandı.
        int played = 0;
        // Seçimleri sırayla uygula.
        for (Side side : choices) {
            // Oyun bittiyse fazladan seçim geçersizdir.
            if (state.endingId() != null) {
                throw new IllegalArgumentException("Too many choices");
            }
            // Seçimi uygula.
            state = step(story, state, side);
            // Sayacı artır.
            played++;
        }
        // Seçimler bitti ama oyun bitmediyse koşu yarımdır.
        if (state.endingId() == null) {
            throw new IllegalArgumentException("Run is not finished");
        }
        // Sonun kazanç olup olmadığını bul.
        String endingId = state.endingId();
        boolean won = story.endings().stream().anyMatch(e -> e.id().equals(endingId) && e.won());
        // Sonucu döndür.
        return new Result(endingId, won, played);
    }

    // Verilen koşullar tutuyor mu? Hiç ayarlanmamış işaret false sayılır. Koşul yoksa true.
    public static boolean holds(Map<String, Boolean> condition, Map<String, Boolean> flags) {
        // Koşul yok.
        if (condition == null) {
            return true;
        }
        // Her koşulu dön.
        for (Map.Entry<String, Boolean> entry : condition.entrySet()) {
            // Değer beklenenden farklıysa tutmuyor.
            if (flags.getOrDefault(entry.getKey(), false) != entry.getValue()) {
                return false;
            }
        }
        // Hepsi tuttu.
        return true;
    }

    // from sırasından başlayarak koşulu tutan ilk kartı bulur. Yoksa -1.
    private int nextCard(Story story, Map<String, Boolean> flags, int from) {
        // Kartları dön.
        for (int i = from; i < story.cards().size(); i++) {
            // Koşul tutuyorsa bu karttır.
            if (holds(story.cards().get(i).when(), flags)) {
                return i;
            }
        }
        // Kart kalmadı.
        return -1;
    }

    // Koşulu tutan ilk kurtuluş sonunu bulur.
    private String winningEnding(Story story, Map<String, Boolean> flags) {
        // Sonları dön.
        for (Story.Ending ending : story.endings()) {
            // Kurtuluşsa ve koşul tutuyorsa bu sondur.
            if (ending.won() && holds(ending.when(), flags)) {
                return ending.id();
            }
        }
        // Doğrulayıcı bunu önler, yine de net bir hata verelim.
        throw new IllegalStateException("No winning ending matches");
    }
}
