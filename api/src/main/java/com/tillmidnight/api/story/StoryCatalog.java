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
            List<String> errors = checker.check(story).errors();
            // Bozuk hikâyeyle sunucu açılmaz: Hata oyuncuya değil bize görünsün.
            if (!errors.isEmpty()) {
                throw new IllegalStateException("Chapter " + id + " is broken: " + errors);
            }
            // Listeye ekle.
            stories.put(id, story);
        }
    }

    // Şu an oynanan bölüm. Bölüm seçme ekranı gelene kadar ilk bölüm.
    public Story current() {
        // İlk bölümü döndür.
        return stories.values().iterator().next();
    }

    // Verilen adda bölüm varsa döndürür, yoksa null.
    public Story findById(String id) {
        // Bulunan hikâye ya da null.
        return stories.get(id);
    }
}
