// Bu dosyanın ait olduğu paket.
package com.tensec.api.web;

// Sahne tipi.
import com.tensec.api.scene.Scene;
// En az dokunuş hesabı.
import com.tensec.api.solver.BestTouches;
// Denemeleri okuyan sınıf.
import com.tensec.api.stats.AttemptRepository;
// Değiştirilebilir liste.
import java.util.ArrayList;
// Liste arayüzü.
import java.util.List;
// Anahtar-değer arayüzü.
import java.util.Map;
// Benzersiz kimlik tipi.
import java.util.UUID;
// GET adresi tanımlama etiketi.
import org.springframework.web.bind.annotation.GetMapping;
// Adresteki ?playerId=... değerini okuma etiketi.
import org.springframework.web.bind.annotation.RequestParam;
// "Bu sınıf REST API" etiketi.
import org.springframework.web.bind.annotation.RestController;

// Bölüm listesi. İlerleme sunucuda hesaplanır: tarayıcı "geçtim" diyerek hile yapamaz.
@RestController
public class LevelController {

    // Sahneleri tutan sınıf. Listedeki sıra bölüm sırasıdır.
    private final SceneCatalog catalog;
    // Oyuncunun denemeleri.
    private final AttemptRepository attempts;
    // En az dokunuş hesabı.
    private final BestTouches bestTouches;

    // Spring gerekenleri buraya kendisi verir.
    public LevelController(SceneCatalog catalog, AttemptRepository attempts, BestTouches bestTouches) {
        // Kataloğu sakla.
        this.catalog = catalog;
        // Depoyu sakla.
        this.attempts = attempts;
        // En az dokunuş hesabını sakla.
        this.bestTouches = bestTouches;
    }

    // GET /levels?playerId=...: oyuncunun bölüm listesi.
    @GetMapping("/levels")
    // required = false: kimlik gelmezse yeni oyuncu sayılır.
    public List<LevelView> levels(@RequestParam(required = false) UUID playerId) {
        // Oyuncunun geçtiği sahneler. Kimlik yoksa boş.
        Map<String, Integer> fewest = playerId == null ? Map.of() : attempts.fewestTouchesByScene(playerId);
        // Sonuç listesi.
        List<LevelView> levels = new ArrayList<>();
        // İlk bölüm hep açıktır.
        boolean previousCompleted = true;

        // Sahneleri sırayla dön.
        for (Scene scene : catalog.all()) {
            // Bu sahne geçildi mi?
            boolean completed = fewest.containsKey(scene.id());
            // Yıldız: en az dokunuşla çözülmüş olmalı. Çözücü sadece gerekirse çalışır.
            boolean star = completed && fewest.get(scene.id()) <= bestTouches.of(scene);
            // Bölümü listeye ekle. Önceki geçildiyse ya da bu zaten geçildiyse açıktır.
            levels.add(new LevelView(
                    levels.size() + 1, scene.id(), scene.title(), previousCompleted || completed, completed, star));
            // Sıradaki bölüm için hatırla.
            previousCompleted = completed;
        }
        // Listeyi döndür.
        return levels;
    }
}
