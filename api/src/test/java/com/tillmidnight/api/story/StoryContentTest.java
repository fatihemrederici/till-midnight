// Bu dosyanın ait olduğu paket.
package com.tillmidnight.api.story;

// İki değer eşit değilse testi kaldırır.
import static org.junit.jupiter.api.Assertions.assertEquals;
// Değer true değilse testi kaldırır.
import static org.junit.jupiter.api.Assertions.assertTrue;

// Liste tipi.
import java.util.List;
// Test etiketi.
import org.junit.jupiter.api.Test;

// Hikâye dosyasının içerik kontrolleri. Eksik çeviri ya da bozuk bağlantı burada yakalanır.
class StoryContentTest {

    // Testlerin kullandığı hikâye.
    private final Story story = StoryLoader.fromClasspath("stories/first-day.json");

    // Bu metot bir test.
    @Test
    // Yapı hatası olmamalı. Bir metnin Lehçesi eksikse bu test kalır.
    void hasNoStructuralErrors() {
        // Hata listesi boş olmalı.
        assertEquals(List.of(), new StoryValidator().validate(story));
    }

    // Bu metot bir test.
    @Test
    // Tüm kartlar ve sonlar ulaşılabilir olmalı, kurtuluş da mümkün olmalı.
    void everythingCanBeReached() {
        // Tüm yolları dene.
        StoryAnalysis analysis = new StoryAnalyzer(new StoryEngine()).analyze(story);

        // Görülmeyen kart yok.
        assertEquals(List.of(), analysis.unreachableCards());
        // Ulaşılamayan son yok.
        assertEquals(List.of(), analysis.unreachableEndings());
        // 485 yol var.
        assertEquals(485, analysis.paths());
        // Kurtuluş mümkün ama kolay değil.
        assertTrue(analysis.survivalPercent() > 0 && analysis.survivalPercent() < 50);
    }
}
