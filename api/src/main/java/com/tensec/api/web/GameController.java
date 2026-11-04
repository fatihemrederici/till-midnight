// Bu dosyanın ait olduğu paket.
package com.tensec.api.web;

// Motor.
import com.tensec.api.engine.Engine;
// Motorun sonuç tipi.
import com.tensec.api.engine.SimulationResult;
// Sahne tipi.
import com.tensec.api.scene.Scene;
// En az dokunuş hesabı.
import com.tensec.api.solver.BestTouches;
// Denemeleri kaydeden sınıf.
import com.tensec.api.stats.AttemptRepository;
// İstatistik tipi.
import com.tensec.api.stats.SceneStats;
// Tarih tipi.
import java.time.LocalDate;
// UTC saat dilimi.
import java.time.ZoneOffset;
// HTTP durum kodları: 200, 404 gibi.
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

// Oyuncunun kullandığı API adresleri. Metotların dönüş değeri otomatik JSON'a çevrilir.
@RestController
public class GameController {

    // Sahneleri tutan sınıf.
    private final SceneCatalog catalog;
    // Oyun motoru.
    private final Engine engine;
    // Denemeleri kaydeden sınıf.
    private final AttemptRepository attempts;
    // En az dokunuş hesabı.
    private final BestTouches bestTouches;

    // Spring, ihtiyaç duyulan nesneleri buraya kendisi verir. Buna dependency injection denir.
    public GameController(
            SceneCatalog catalog, Engine engine, AttemptRepository attempts, BestTouches bestTouches) {
        // Sahne kataloğunu sakla.
        this.catalog = catalog;
        // Motoru sakla.
        this.engine = engine;
        // Depoyu sakla.
        this.attempts = attempts;
        // En az dokunuş hesabını sakla.
        this.bestTouches = bestTouches;
    }

    // GET /scene/today adresine gelen istekler bu metoda düşer.
    @GetMapping("/scene/today")
    public PublicScene today() {
        // Bugünün sahnesi.
        Scene scene = catalog.today();
        // Sahnenin sadece güvenli kısmını ve kehaneti döndür.
        return PublicScene.from(scene, engine.foresee(scene));
    }

    // POST /attempt adresine gelen istekler bu metoda düşer.
    @PostMapping("/attempt")
    // @RequestBody: gelen JSON'u AttemptRequest'e çevir.
    public AttemptResponse attempt(@RequestBody AttemptRequest request) {
        // İstenen sahneyi bul.
        Scene scene = catalog.findById(request.sceneId());
        // Bulunamadıysa
        if (scene == null) {
            // 404 hatası döndür.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Scene not found");
        }

        // Motoru çalıştır. Sonucu sunucu hesapladığı için oyuncu hile yapamaz.
        SimulationResult result = engine.simulate(scene, request.touches());

        // Kimlikler geldiyse denemeyi kaydet.
        if (request.playerId() != null && request.attemptId() != null) {
            // Etkisiz dokunuşlar hak yemez, bu yüzden sayılmaz. Motor onları ruleId'siz kaydeder.
            long wasted = result.events().stream().filter(event -> event.ruleId() == null).count();
            // Sonucu tarayıcının söylediğinden değil, motordan alıyoruz.
            attempts.save(
                    request.attemptId(),
                    request.playerId(),
                    scene.id(),
                    LocalDate.now(ZoneOffset.UTC),
                    request.touches().size() - (int) wasted,
                    result);
        }

        // En iyi çözümü sadece kazanana söyle.
        Integer best = result.won() ? bestTouches.of(scene) : null;
        // Sonucu cevaba çevir.
        return AttemptResponse.from(scene, result, best);
    }

    // GET /stats/today: bugünün sahnesinin bugünkü istatistikleri.
    @GetMapping("/stats/today")
    public SceneStats statsToday() {
        // Bugünün sahnesi ve tarihiyle sorgula.
        return attempts.stats(catalog.today().id(), LocalDate.now(ZoneOffset.UTC));
    }
}
