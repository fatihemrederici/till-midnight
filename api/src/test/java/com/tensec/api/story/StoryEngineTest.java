// Bu dosyanın ait olduğu paket.
package com.tensec.api.story;

// İki değer eşit değilse testi kaldırır.
import static org.junit.jupiter.api.Assertions.assertEquals;
// Beklenen hata fırlatılmazsa testi kaldırır.
import static org.junit.jupiter.api.Assertions.assertThrows;
// Değer true değilse testi kaldırır.
import static org.junit.jupiter.api.Assertions.assertTrue;

// Kaydırma yönü.
import com.tensec.api.story.StoryEngine.Side;
// Dosya okuma hatası.
import java.io.IOException;
// Dosya okuma.
import java.io.InputStream;
// Liste tipi.
import java.util.List;
// Test etiketi.
import org.junit.jupiter.api.Test;
// JSON ağacı.
import tools.jackson.databind.JsonNode;
// JSON okuyucu.
import tools.jackson.databind.json.JsonMapper;

// Motorun testleri.
class StoryEngineTest {

    // Testlerin kullandığı hikâye.
    private final Story story = StoryLoader.fromClasspath("stories/first-day.json");
    // Test edilen motor.
    private final StoryEngine engine = new StoryEngine();

    // Bu metot bir test.
    @Test
    // first-day.paths.json içindeki tüm yollar beklenen sonu vermeli.
    // Web'deki engine.ts aynı dosyayla test edilir, böylece iki motor birbirinden ayrılamaz.
    void everyKnownPathEndsAsExpected() throws IOException {
        // Dosyayı aç.
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("stories/first-day.paths.json")) {
            // JSON'u oku.
            JsonNode paths = JsonMapper.builder().build().readTree(in).get("paths");
            // Her yolu dene.
            for (JsonNode path : paths) {
                // "LRL" metnini yön listesine çevir.
                List<Side> choices = path.get("choices").asString().chars()
                        .mapToObj(c -> c == 'L' ? Side.LEFT : Side.RIGHT)
                        .toList();
                // Beklenen sonu kontrol et.
                assertEquals(path.get("ending").asString(), engine.play(story, choices).endingId(), path.toString());
            }
            // Yolların hepsi denenmiş olmalı.
            assertEquals(485, paths.size());
        }
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
