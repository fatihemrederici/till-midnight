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
// Test dosyalarını okur.
import org.springframework.core.io.ClassPathResource;

// Motorun testleri.
class StoryEngineTest {

    // Testlerin kullandığı hikâye.
    private final Story story = StoryLoader.loadChapter("first-day");
    // Test edilen motor.
    private final StoryEngine engine = new StoryEngine();

    // Bu metot bir test.
    @Test
    // story-paths/first-day.txt içindeki tüm yollar beklenen sonu vermeli.
    // Web'deki engine.ts aynı dosyayla test edilir, böylece iki motor birbirinden ayrılamaz.
    void everyKnownPathEndsAsExpected() throws IOException {
        // Dosyadaki satırlar: "LRL stairs".
        List<String> lines = new ClassPathResource("story-paths/first-day.txt")
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
            assertEquals(parts[1], engine.play(story, choices).endingId(), line);
        }
        // Yolların hepsi denenmiş olmalı.
        assertEquals(485, lines.size());
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
