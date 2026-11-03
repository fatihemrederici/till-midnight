// Bu dosyanın ait olduğu paket.
package com.tensec.api.web;

// Motor.
import com.tensec.api.engine.Engine;
// Motorun sonuç tipi.
import com.tensec.api.engine.SimulationResult;
// Sahne tipi.
import com.tensec.api.scene.Scene;
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

    // Spring, ihtiyaç duyulan nesneleri buraya kendisi verir. Buna dependency injection denir.
    public GameController(SceneCatalog catalog, Engine engine) {
        // Sahne kataloğunu sakla.
        this.catalog = catalog;
        // Motoru sakla.
        this.engine = engine;
    }

    // GET /scene/today adresine gelen istekler bu metoda düşer.
    @GetMapping("/scene/today")
    public PublicScene today() {
        // Sahnenin sadece güvenli kısmını döndür.
        return PublicScene.from(catalog.today());
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
        // Sonucu cevaba çevir.
        return AttemptResponse.from(scene, result);
    }
}