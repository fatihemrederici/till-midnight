// Bu dosyanın ait olduğu paket.
package com.tillmidnight.api.story;

// İki değer eşit değilse testi kaldırır.
import static org.junit.jupiter.api.Assertions.assertEquals;
// Beklenen hata fırlatılmazsa testi kaldırır.
import static org.junit.jupiter.api.Assertions.assertThrows;
// Değer true değilse testi kaldırır.
import static org.junit.jupiter.api.Assertions.assertTrue;

// Kaydırma yönü.
import com.tillmidnight.api.story.StoryEngine.Side;
// Dosya okuma hatası.
import java.io.IOException;
// Metin kodlaması.
import java.nio.charset.StandardCharsets;
// Liste tipi.
import java.util.List;
// Test etiketi.
import org.junit.jupiter.api.Test;
// Parametreli test etiketi.
import org.junit.jupiter.params.ParameterizedTest;
// Test değerlerini metottan alan etiket.
import org.junit.jupiter.params.provider.MethodSource;
// Test dosyalarını okur.
import org.springframework.core.io.ClassPathResource;

// Motorun testleri.
class StoryEngineTest {

    // Testlerin kullandığı hikâye.
    private final Story story = StoryLoader.loadChapter("first-day");
    // Test edilen motor.
    private final StoryEngine engine = new StoryEngine();

    // Testlere verilecek bölüm kimlikleri.
    static List<String> chapters() {
        // chapters.json dosyasından oku.
        return StoryLoader.chapterIds();
    }

    // Her bölüm için çalışan test.
    @ParameterizedTest
    @MethodSource("chapters")
    // story-paths içindeki tüm yollar beklenen sonu vermeli.
    // Web'deki engine.ts aynı dosyalarla test edilir, böylece iki motor birbirinden ayrılamaz.
    void everyKnownPathEndsAsExpected(String id) throws IOException {
        // Bölümü oku.
        Story chapter = StoryLoader.loadChapter(id);
        // Dosyadaki satırlar: "LRL stairs".
        List<String> lines = new ClassPathResource("story-paths/" + id + ".txt")
                .getContentAsString(StandardCharsets.UTF_8)
                .lines()
                .toList();
        // Her yolu dene.
        for (String line : lines) {
            // Seçimleri ve sonu ayır.
            String[] parts = line.split(" ");
            // "LRL" metnini yön listesine çevir.
            List<Side> choices = parts[0].chars().mapToObj(c -> c == 'L' ? Side.LEFT : Side.RIGHT).toList();
            // Beklenen sonu kontrol et.
            assertEquals(parts[1], engine.play(chapter, choices).endingId(), line);
        }
        // Dosya, motorun bulduğu tüm yolları içermeli.
        assertEquals(new StoryAnalyzer(engine).analyze(chapter).paths(), lines.size());
    }

    // Tek kartlı küçük bir hikâye: Tek gösterge, tek kriz.
    private static final String MINI = """
            {"rules":{"formatVersion":1,"id":"mini","start":{"m":1,"o":2},"maxMeter":5,
            "meters":[{"id":"m","emoji":"x","ending":"gone",
            "crisis":{"id":"crisis-m","emoji":"x",
            "left":{"effects":{"o":-2},"requires":{"o":2}},"right":{"effects":{"o":-3},"requires":{"o":3}}}},
            {"id":"o","emoji":"x","ending":"other"}],
            "cards":[{"id":"a","time":"10:00","emoji":"x","left":{"effects":{"m":-1}},"right":{"requires":{"o":9}}},
            {"id":"b","time":"11:00","emoji":"x","left":{"effects":{"m":-1}},"right":{}}],
            "endings":[{"id":"gone","won":false,"chain":[]},{"id":"other","won":false,"chain":[]},
            {"id":"win","won":true,"chain":[]}]},
            "texts":{"en":{"title":"t","vision":["v"],"wake":"w","meters":{"m":"M","o":"O"},
            "crises":{"m":{"who":"w","text":"t","left":{"label":"l","after":"a"},"right":{"label":"r","after":"a"}}},
            "cards":{"a":{"who":"w","text":"t","left":{"label":"l","after":"a"},"right":{"label":"r","after":"a"}},
            "b":{"who":"w","text":"t","left":{"label":"l","after":"a"},"right":{"label":"r","after":"a"}}},
            "endings":{"gone":{"title":"t","text":"t","chain":[],"hint":"h"},
            "other":{"title":"t","text":"t","chain":[],"hint":"h"},"win":{"title":"t","text":"t","chain":[],"hint":"h"}}}}}
            """;

    // Bu metot bir test.
    @Test
    // Gösterge sıfıra inince oyun bitmemeli, kriz kartı gelmeli.
    void anEmptyMeterOpensItsCrisisCard() {
        // Küçük hikâye.
        Story mini = StoryLoader.parsePack(MINI).story();
        // Göstergeyi sıfıra indir.
        StoryEngine.State state = engine.step(mini, engine.start(mini), Side.LEFT);
        // Oyun sürüyor, kriz açık.
        assertEquals("m", state.crisis());
        assertEquals(null, state.endingId());
        assertEquals("crisis-m", engine.currentCard(mini, state).id());
    }

    // Bu metot bir test.
    @Test
    // Krizi ödeyince gösterge 1'e dönmeli, ikinci sıfırda son gelmeli.
    void aCrisisCanOnlySaveYouOnce() {
        // Küçük hikâye.
        Story mini = StoryLoader.parsePack(MINI).story();
        // Krize gir ve diğer göstergeden öde.
        StoryEngine.State state = engine.step(mini, engine.start(mini), Side.LEFT);
        state = engine.step(mini, state, Side.LEFT);
        // Bedel ödendi. Kritik gösterge 1, diğeri sıfır olduğu için onun sonu gelir.
        assertEquals(1, state.meters().get("m"));
        assertEquals("other", state.endingId());
    }

    // Bu metot bir test.
    @Test
    // Kilitli seçenek seçilememeli.
    void rejectsALockedChoice() {
        // Küçük hikâye.
        Story mini = StoryLoader.parsePack(MINI).story();
        // Sağ taraf 9 ister, gösterge 2.
        assertEquals(false, engine.allowed(mini, engine.start(mini), Side.RIGHT));
        assertThrows(IllegalArgumentException.class, () -> engine.step(mini, engine.start(mini), Side.RIGHT));
    }

    // Bu metot bir test.
    @Test
    // Islak ayakkabıyla merdiven öldürür: oyunun ana numarası.
    void aSpilledCoffeeKillsYouOnTheStairs() {
        // Uyar, haber, hemen çık, kehaneti anlat, bekle, merdiven.
        StoryEngine.Result result = engine.play(
                story, List.of(Side.LEFT, Side.RIGHT, Side.RIGHT, Side.LEFT, Side.LEFT, Side.LEFT));

        // Merdivende ölür.
        assertEquals("stairs", result.endingId());
        // Kaybetmiştir.
        assertEquals(false, result.won());
        // 6 kart oynanmıştır.
        assertEquals(6, result.cardsPlayed());
    }

    // Bu metot bir test.
    @Test
    // Yarım koşu ve fazla seçim reddedilmeli. Tarayıcıdan gelen veriye güvenmiyoruz.
    void rejectsInvalidRuns() {
        // Yarım koşu.
        assertThrows(IllegalArgumentException.class, () -> engine.play(story, List.of(Side.LEFT)));
        // Oyun bittikten sonra fazladan seçim.
        assertThrows(IllegalArgumentException.class, () -> engine.play(
                story, List.of(Side.LEFT, Side.RIGHT, Side.RIGHT, Side.LEFT, Side.LEFT, Side.LEFT, Side.LEFT)));
    }

    // Bu metot bir test.
    @Test
    // Koşulsuz seçim her durumda tutar, bilinmeyen işaret false sayılır.
    void treatsMissingFlagsAsFalse() {
        // Koşul yok.
        assertTrue(StoryEngine.holds(null, java.util.Map.of()));
        // Ayarlanmamış işaret false beklentisini karşılar.
        assertTrue(StoryEngine.holds(java.util.Map.of("elif", false), java.util.Map.of()));
    }
}
