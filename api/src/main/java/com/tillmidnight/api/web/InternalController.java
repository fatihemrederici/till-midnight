// Bu dosyanın ait olduğu paket.
package com.tillmidnight.api.web;

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

    // {"rules": {...}, "texts": {"en": {...}}} paketini alır, kullanılabilir mi diye cevap verir.
    // Sadece paketteki diller zorunludur, böylece çeviriden önce taslak denetlenebilir.
    @PostMapping("/internal/validate")
    public ValidationResponse validate(@RequestBody String json) {
        // Okunan paket.
        StoryLoader.Pack pack;
        try {
            // Metni çevir.
            pack = StoryLoader.parsePack(json);
        } catch (JacksonException exception) {
            // Bozuk JSON ya da bilinmeyen alan.
            return new ValidationResponse(false, List.of("Invalid JSON: " + exception.getOriginalMessage()), null);
        } catch (IllegalArgumentException | ClassCastException exception) {
            // Eksik paket, yanlış yapı ya da kullanılmayan metin.
            return new ValidationResponse(false, List.of(exception.getMessage()), null);
        }
        // Hikâyeyi kontrol et.
        StoryChecker.Report report = checker.check(pack.story(), pack.languages());
        // Raporu döndür.
        return new ValidationResponse(report.errors().isEmpty(), report.errors(), report.analysis());
    }
}
