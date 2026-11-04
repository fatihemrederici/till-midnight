// Bu dosyanın ait olduğu paket.
package com.tillmidnight.api.web;

// Koşu deposu.
import com.tillmidnight.api.run.RunRepository;
// Kaydedilecek koşu.
import com.tillmidnight.api.run.SavedRun;
// Hikâye tipi.
import com.tillmidnight.api.story.Story;
// Bölüm listesi.
import com.tillmidnight.api.story.StoryCatalog;
// Motor.
import com.tillmidnight.api.story.StoryEngine;
// HTTP durum kodları.
import org.springframework.http.HttpStatus;
// GET adresi tanımlama etiketi.
import org.springframework.web.bind.annotation.GetMapping;
// POST adresi tanımlama etiketi.
import org.springframework.web.bind.annotation.PostMapping;
// İstek gövdesini okuma etiketi.
import org.springframework.web.bind.annotation.RequestBody;
// "Bu sınıf REST API" etiketi.
import org.springframework.web.bind.annotation.RestController;
// Belirli bir HTTP koduyla hata fırlatmaya yarar.
import org.springframework.web.server.ResponseStatusException;

// Oyuncunun kullandığı API adresleri.
@RestController
public class GameController {

    // Hikâyeleri tutan sınıf.
    private final StoryCatalog catalog;
    // Motor.
    private final StoryEngine engine;
    // Koşu deposu.
    private final RunRepository runs;

    // Spring gerekenleri buraya kendisi verir. Buna dependency injection denir.
    public GameController(StoryCatalog catalog, StoryEngine engine, RunRepository runs) {
        // Kataloğu sakla.
        this.catalog = catalog;
        // Motoru sakla.
        this.engine = engine;
        // Depoyu sakla.
        this.runs = runs;
    }

    // GET /story: oynanacak hikâye. Kaydırma anında tepki versin diye kurallar da gider;
    // bu tek oyunculu bir hikâye olduğu için kabul edilebilir, sonuç yine sunucuda doğrulanır.
    @GetMapping("/story")
    public Story story() {
        // Hikâyeyi döndür.
        return catalog.current();
    }

    // POST /runs: biten bir koşuyu doğrular ve kaydeder.
    @PostMapping("/runs")
    public RunResponse run(@RequestBody RunRequest request) {
        // Hikâyeyi bul.
        Story story = catalog.findById(request.storyId());
        // Yoksa 404.
        if (story == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Story not found");
        }
        // Kimlikler ve seçimler zorunlu.
        if (request.runId() == null || request.playerId() == null || request.choices() == null) {
            throw new IllegalArgumentException("runId, playerId and choices are required");
        }

        // Seçimleri motorda baştan oynat. Sonucu tarayıcıya değil buna güveniyoruz.
        StoryEngine.Result result = engine.play(story, request.choices());
        // Koşuyu kaydet.
        runs.save(new SavedRun(
                request.runId(), request.playerId(), story.id(), result.endingId(), result.won(), result.cardsPlayed()));
        // Sonucu ve istatistikleri döndür.
        return new RunResponse(result.endingId(), result.won(), runs.stats(story.id(), result.endingId()));
    }
}
