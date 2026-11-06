// Bu dosyanın ait olduğu paket.
package com.tillmidnight.api.story;

// Dosya yazmak için.
import java.nio.file.Files;
import java.nio.file.Path;
// Sıralı ve tekrarsız küme.
import java.util.LinkedHashSet;
// Liste.
import java.util.List;
// Tohumlu rastgele sayı: Her çalıştırmada aynı yollar çıksın.
import java.util.Random;
// Küme.
import java.util.Set;
// JUnit test etiketi.
import org.junit.jupiter.api.Test;
// Sadece istenince çalışsın diye.
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

// story-paths dosyalarını yeniden üretir. Normal testlerde çalışmaz.
// Çalıştırmak için: ./mvnw test -Dtest=UpdateStoryPaths -DupdateStoryPaths=true
// Yol sayısı milyonları bulduğu için hepsi değil, tohumlu rastgele bir örnek yazılır. Her son en az bir kez bulunur.
class UpdateStoryPaths {

    // Kaç yol yazılacak.
    private static final int SAMPLE = 3000;

    // Gerçek motor.
    private final StoryEngine engine = new StoryEngine();

    @Test
    @EnabledIfSystemProperty(named = "updateStoryPaths", matches = "true")
    void writePathFiles() throws Exception {
        // Her bölüm için.
        for (String id : StoryLoader.chapterIds()) {
            Story story = StoryLoader.loadChapter(id);
            // Aynı tohum, aynı yollar.
            Random random = new Random(id.hashCode());
            // Tekrarsız satırlar.
            Set<String> lines = new LinkedHashSet<>();
            // Her sonu bulana kadar ve örnek dolana kadar oyna.
            Set<String> missing = new LinkedHashSet<>(new StoryAnalyzer(engine).analyze(story).endingPercent().entrySet()
                    .stream().filter(entry -> entry.getValue() > 0).map(java.util.Map.Entry::getKey).toList());
            for (int tries = 0; (lines.size() < SAMPLE || !missing.isEmpty()) && tries < 200_000; tries++) {
                String line = randomRun(story, random);
                lines.add(line);
                missing.remove(line.split(" ")[1]);
            }
            Files.write(Path.of("src/test/resources/story-paths/" + id + ".txt"), List.copyOf(lines));
        }
    }

    // Rastgele bir koşu oynar ve "LRL son" satırını döndürür.
    private String randomRun(Story story, Random random) {
        // Baştan başla.
        StoryEngine.State state = engine.start(story);
        StringBuilder choices = new StringBuilder();
        // Oyun bitene kadar.
        while (state.endingId() == null) {
            boolean left = random.nextBoolean();
            choices.append(left ? 'L' : 'R');
            state = engine.step(story, state, left ? StoryEngine.Side.LEFT : StoryEngine.Side.RIGHT);
        }
        return choices + " " + state.endingId();
    }
}
