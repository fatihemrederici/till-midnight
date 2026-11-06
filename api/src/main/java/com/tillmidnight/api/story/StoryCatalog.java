// Bu dosyanın ait olduğu paket.
package com.tillmidnight.api.story;

// Sırasını koruyan anahtar-değer.
import java.util.LinkedHashMap;
// Liste arayüzü.
import java.util.List;
// Anahtar-değer arayüzü.
import java.util.Map;
// Spring'in "bu sınıfı sen yönet" etiketi.
import org.springframework.stereotype.Component;

// Oyunun tüm bölümlerini tutar. Sunucu açılırken bir kere okunur ve kontrol edilir.
@Component
public class StoryCatalog {

    // Bölüm kimliği -> hikâye, oynanacak sırayla.
    private final Map<String, Story> stories = new LinkedHashMap<>();

    // Spring kontrol edeni buraya verir.
    public StoryCatalog(StoryChecker checker) {
        // Bölümleri dön.
        for (String id : StoryLoader.chapterIds()) {
            // Dosyaları oku.
            Story story = StoryLoader.loadChapter(id);
            // Kontrol et.
            StoryChecker.Report report = checker.check(story);
            List<String> errors = report.errors();
            // Bozuk hikâyeyle sunucu açılmaz: Hata oyuncuya değil bize görünsün.
            if (!errors.isEmpty()) {
                throw new IllegalStateException("Chapter " + id + " is broken: " + errors);
            }
            // Listeye ekle.
            // Oyuncunun bulabileceği ölümler: En az bir yolla ulaşılan, kurtuluş olmayan sonlar.
            List<String> deaths = story.endings().stream()
                    .filter(ending -> !ending.won())
                    .map(Story.Ending::id)
                    .filter(endingId -> report.analysis().endingPercent().getOrDefault(endingId, 0.0) > 0)
                    .toList();
            stories.put(id, story.withDeaths(deaths));
        }
    }

    // Tüm bölümler, oynanacak sırayla.
    public List<Story> all() {
        // Değiştirilemez kopya döndür.
        return List.copyOf(stories.values());
    }

    // Verilen adda bölüm varsa döndürür, yoksa null.
    public Story findById(String id) {
        // Bulunan hikâye ya da null.
        return stories.get(id);
    }
}
