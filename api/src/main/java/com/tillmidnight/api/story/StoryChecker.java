// Bu dosyanın ait olduğu paket.
package com.tillmidnight.api.story;

// Değiştirilebilir liste.
import java.util.ArrayList;
// Liste arayüzü.
import java.util.List;
// Spring'in "bu sınıfı sen yönet" etiketi.
import org.springframework.stereotype.Component;

// Bir hikâyenin oynanabilir olup olmadığını söyler: Önce yapı, sonra tüm yollar.
// Hem sunucu açılışında hem /internal/validate adresinde bu sınıf kullanılır.
@Component
public class StoryChecker {

    // Kontrolün sonucu. errors boşsa hikâye geçerlidir; yapı bozuksa analysis null olur.
    public record Report(List<String> errors, StoryAnalysis analysis) {}

    // Yapı kontrolü.
    private final StoryValidator validator;
    // Yol analizi.
    private final StoryAnalyzer analyzer;

    // Spring ikisini de buraya verir.
    public StoryChecker(StoryValidator validator, StoryAnalyzer analyzer) {
        // Doğrulayıcıyı sakla.
        this.validator = validator;
        // Analizciyi sakla.
        this.analyzer = analyzer;
    }

    // Hikâyeyi tüm dillerle kontrol eder.
    public Report check(Story story) {
        // Oyuna girecek hikâyede her dil zorunlu.
        return check(story, StoryValidator.LANGUAGES);
    }

    // Hikâyeyi sadece verilen dillerle kontrol eder.
    public Report check(Story story, List<String> languages) {
        // Yapı hatalarını bul.
        List<String> errors = new ArrayList<>(validator.validate(story, languages));
        // Yapı bozuksa yolları denemek güvenli değil.
        if (!errors.isEmpty()) {
            return new Report(errors, null);
        }

        // Tüm yolları dene.
        StoryAnalysis analysis = analyzer.analyze(story);
        // Kurtuluş yolu olmalı.
        if (analysis.survivalPercent() == 0) {
            errors.add("Story has no surviving path");
        }
        // Ulaşılamayan kart olmamalı.
        analysis.unreachableCards().forEach(id -> errors.add("Card can never appear: " + id));
        // Ulaşılamayan son olmamalı.
        analysis.unreachableEndings().forEach(id -> errors.add("Ending can never be reached: " + id));
        // Raporu döndür.
        return new Report(errors, analysis);
    }
}
