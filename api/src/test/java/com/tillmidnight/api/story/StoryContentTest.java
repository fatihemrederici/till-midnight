// Bu dosyanın ait olduğu paket.
package com.tillmidnight.api.story;

// İki değer eşit değilse testi kaldırır.
import static org.junit.jupiter.api.Assertions.assertEquals;
// Değer true değilse testi kaldırır.
import static org.junit.jupiter.api.Assertions.assertTrue;

// Liste tipi.
import java.util.List;
// Parametreli test etiketi.
import org.junit.jupiter.params.ParameterizedTest;
// Test değerlerini metottan alan etiket.
import org.junit.jupiter.params.provider.MethodSource;

// Bölüm dosyalarının içerik kontrolleri. Yeni bölüm eklenince otomatik olarak o da denenir.
class StoryContentTest {

    // Testlere verilecek bölüm kimlikleri.
    static List<String> chapters() {
        // chapters.json dosyasından oku.
        return StoryLoader.chapterIds();
    }

    // Her bölüm için çalışan test.
    @ParameterizedTest
    @MethodSource("chapters")
    // Yapı hatası olmamalı. Bir metnin Lehçesi eksikse bu test kalır.
    void hasNoStructuralErrors(String id) {
        // Hata listesi boş olmalı.
        assertEquals(List.of(), new StoryValidator().validate(StoryLoader.loadChapter(id)));
    }

    // Her bölüm için çalışan test.
    @ParameterizedTest
    @MethodSource("chapters")
    // Tüm kartlar ve sonlar ulaşılabilir olmalı, kurtuluş da mümkün olmalı.
    void everythingCanBeReached(String id) {
        // Tüm yolları dene.
        StoryAnalysis analysis = new StoryAnalyzer(new StoryEngine()).analyze(StoryLoader.loadChapter(id));

        // Görülmeyen kart yok.
        assertEquals(List.of(), analysis.unreachableCards());
        // Ulaşılamayan son yok.
        assertEquals(List.of(), analysis.unreachableEndings());
        // Kurtuluş mümkün ama kolay değil.
        assertTrue(analysis.survivalPercent() > 0 && analysis.survivalPercent() < 50);
    }
}
