// Bu dosyanın ait olduğu paket.
package com.tillmidnight.api.web;

// Hikâye tipi.
import com.tillmidnight.api.story.Story;
// Hikâye okuyucu.
import com.tillmidnight.api.story.StoryLoader;
// Spring'in "bu sınıfı sen yönet" etiketi.
import org.springframework.stereotype.Component;

// Oyunun hikâyelerini tutar. Şimdilik tek hikâye var, sonra veritabanından gelecek.
@Component
public class StoryCatalog {

    // Hikâye. Uygulama açılırken bir kere okunur.
    private final Story firstDay = StoryLoader.fromClasspath("stories/first-day.json");

    // Şu an oynanan hikâye.
    public Story current() {
        // Tek hikâyeyi döndür.
        return firstDay;
    }

    // Verilen adda hikâye varsa döndürür, yoksa null.
    public Story findById(String id) {
        // firstDay.id() önde olduğu için id null olsa da hata çıkmaz.
        return firstDay.id().equals(id) ? firstDay : null;
    }
}
