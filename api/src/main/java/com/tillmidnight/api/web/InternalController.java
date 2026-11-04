// Bu dosyanın ait olduğu paket.
package com.tillmidnight.api.web;

// Hikâye tipi.
import com.tillmidnight.api.story.Story;
// Analiz raporu.
import com.tillmidnight.api.story.StoryAnalysis;
// Analiz eden sınıf.
import com.tillmidnight.api.story.StoryAnalyzer;
// Hikâye okuyucu.
import com.tillmidnight.api.story.StoryLoader;
// Doğrulayıcı.
import com.tillmidnight.api.story.StoryValidator;
// Değiştirilebilir liste.
import java.util.ArrayList;
// Liste arayüzü.
import java.util.List;
// POST adresi tanımlama etiketi.
import org.springframework.web.bind.annotation.PostMapping;
// İstek gövdesini okuma etiketi.
import org.springframework.web.bind.annotation.RequestBody;
// "Bu sınıf REST API" etiketi.
import org.springframework.web.bind.annotation.RestController;
// Jackson'ın JSON okuma hatası tipi.
import tools.jackson.core.JacksonException;

// AI'ın yazdığı hikâyeleri denetleyecek adres.
// Dikkat: Yayına çıkmadan önce bu adres korumaya alınacak.
@RestController
public class InternalController {

    // Yapı kontrolü.
    private final StoryValidator validator;
    // Yol analizi.
    private final StoryAnalyzer analyzer;

    // Spring ikisini de buraya verir.
    public InternalController(StoryValidator validator, StoryAnalyzer analyzer) {
        // Doğrulayıcıyı sakla.
        this.validator = validator;
        // Analizciyi sakla.
        this.analyzer = analyzer;
    }

    // Hikâye JSON'unu alır, kullanılabilir mi diye cevap verir.
    @PostMapping("/internal/validate")
    public ValidationResponse validate(@RequestBody String json) {
        // Okunan hikâye.
        Story story;
        try {
            // Metni çevir.
            story = StoryLoader.parse(json);
        } catch (JacksonException exception) {
            // Bozuk JSON ya da bilinmeyen alan.
            return new ValidationResponse(false, List.of("Invalid JSON: " + exception.getOriginalMessage()), null);
        }

        // Yapı hatalarını bul.
        List<String> errors = new ArrayList<>(validator.validate(story));
        // Hata varsa analiz güvenli değil.
        if (!errors.isEmpty()) {
            return new ValidationResponse(false, errors, null);
        }

        // Tüm yolları dene.
        StoryAnalysis analysis;
        try {
            analysis = analyzer.analyze(story);
        } catch (IllegalArgumentException exception) {
            // Çok fazla yol.
            return new ValidationResponse(false, List.of(exception.getMessage()), null);
        }
        // Kurtuluş yolu olmalı.
        if (analysis.survivalPercent() == 0) {
            errors.add("Story has no surviving path");
        }
        // Ulaşılamayan kart olmamalı.
        analysis.unreachableCards().forEach(id -> errors.add("Card can never appear: " + id));
        // Ulaşılamayan son olmamalı.
        analysis.unreachableEndings().forEach(id -> errors.add("Ending can never be reached: " + id));
        // Raporu döndür.
        return new ValidationResponse(errors.isEmpty(), errors, analysis);
    }
}
