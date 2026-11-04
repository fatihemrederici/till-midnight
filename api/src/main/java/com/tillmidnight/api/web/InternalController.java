// Bu dosyanın ait olduğu paket.
package com.tillmidnight.api.web;

// Hikâye tipi.
import com.tillmidnight.api.story.Story;
// Hikâye kontrolü.
import com.tillmidnight.api.story.StoryChecker;
// Hikâye okuyucu.
import com.tillmidnight.api.story.StoryLoader;
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

    // Hikâye kontrolü.
    private final StoryChecker checker;

    // Spring kontrol edeni buraya verir.
    public InternalController(StoryChecker checker) {
        // Kontrol edeni sakla.
        this.checker = checker;
    }

    // Birleşik hikâye JSON'unu alır, kullanılabilir mi diye cevap verir.
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
        // Hikâyeyi kontrol et.
        StoryChecker.Report report = checker.check(story);
        // Raporu döndür.
        return new ValidationResponse(report.errors().isEmpty(), report.errors(), report.analysis());
    }
}
