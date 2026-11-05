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
