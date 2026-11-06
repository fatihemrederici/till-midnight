// Bu dosyanın ait olduğu paket.
package com.tillmidnight.api.story;

// Dosya yazma.
import java.nio.file.Files;
// Dosya yolu.
import java.nio.file.Path;
// Değiştirilebilir liste.
import java.util.ArrayList;
// Liste arayüzü.
import java.util.List;
// Test etiketi.
import org.junit.jupiter.api.Test;
// Sadece istenince çalışan test etiketi.
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

// Hikâye değişince beklenen yollar dosyasını yeniden üretir. Normal test koşusunda çalışmaz.
// Çalıştırmak için: ./mvnw test -Dtest=UpdateStoryPaths -DupdateStoryPaths=true
class UpdateStoryPaths {

    // Gerçek motor.
    private final StoryEngine engine = new StoryEngine();

    // Bu metot bir test.
    @Test
    // Sadece komutta istenince çalış.
    @EnabledIfSystemProperty(named = "updateStoryPaths", matches = "true")
    void writePathFiles() throws Exception {
        // Her bölümü dön.
        for (String id : StoryLoader.chapterIds()) {
            // Bölümü oku.
            Story story = StoryLoader.loadChapter(id);
            // Bulunan satırlar.
            List<String> lines = new ArrayList<>();
            // Baştan yürü.
            walk(story, engine.start(story), "", lines);
            // Dosyaya yaz.
            Files.write(Path.of("src/test/resources/story-paths/" + id + ".txt"), lines);
        }
    }

    // Tüm seçimleri dener: Solu L, sağı R olarak yazar.
    private void walk(Story story, StoryEngine.State state, String choices, List<String> lines) {
        // Oyun bittiyse satırı ekle.
        if (state.endingId() != null) {
            lines.add(choices + " " + state.endingId());
            return;
        }
        // Sola git. Kilitli seçenek oynanamaz.
        if (engine.allowed(story, state, StoryEngine.Side.LEFT)) {
            walk(story, engine.step(story, state, StoryEngine.Side.LEFT), choices + "L", lines);
        }
        // Sağa git.
        if (engine.allowed(story, state, StoryEngine.Side.RIGHT)) {
            walk(story, engine.step(story, state, StoryEngine.Side.RIGHT), choices + "R", lines);
        }
    }
}
