// Bu dosyanın ait olduğu paket.
package com.tensec.api.web;

// Sahne tipi.
import com.tensec.api.scene.Scene;
// Sahne okuyucu.
import com.tensec.api.scene.SceneLoader;
// Anlam hatalarını bulan sınıf.
import com.tensec.api.scene.SceneValidator;
// Çözücünün raporu.
import com.tensec.api.solver.SolveResult;
// Çözücü.
import com.tensec.api.solver.Solver;
// Liste tipi.
import java.util.List;
// POST adresi tanımlama etiketi.
import org.springframework.web.bind.annotation.PostMapping;
// İstek gövdesini okuma etiketi.
import org.springframework.web.bind.annotation.RequestBody;
// "Bu sınıf REST API" etiketi.
import org.springframework.web.bind.annotation.RestController;
// Jackson'ın JSON okuma hatası tipi.
import tools.jackson.core.JacksonException;

// Oyuncuların değil, AI worker'ın kullanacağı adresler.
// Dikkat: Yayına çıkmadan önce bu adres korumaya alınacak.
@RestController
public class InternalController {

    // Anlam kontrolü yapan sınıf.
    private final SceneValidator validator;
    // Çözücü.
    private final Solver solver;

    // Spring ikisini de buraya kendisi verir.
    public InternalController(SceneValidator validator, Solver solver) {
        // Doğrulayıcıyı sakla.
        this.validator = validator;
        // Çözücüyü sakla.
        this.solver = solver;
    }

    // Sahne JSON'unu alır, kullanılabilir mi diye cevap verir.
    @PostMapping("/internal/validate")
    // String alıyoruz: JSON bozuk olsa bile hatayı kendimiz yakalayıp AI'a açıklayabilmek için.
    public ValidationResponse validate(@RequestBody String json) {
        // Okunan sahne.
        Scene scene;
        // JSON'u çevirmeyi dene.
        try {
            // Metni sahneye çevir.
            scene = SceneLoader.parse(json);
        // Bozuk JSON ya da bilinmeyen alan buraya düşer.
        } catch (JacksonException exception) {
            // Hatanın kısa açıklamasını döndür.
            return ValidationResponse.invalid(List.of("Invalid JSON: " + exception.getOriginalMessage()));
        }

        // Anlam hatalarını bul.
        List<String> errors = validator.validate(scene);
        // Hata varsa çözücüyü çalıştırmak güvenli değil.
        if (!errors.isEmpty()) {
            // Hatalarla dön.
            return ValidationResponse.invalid(errors);
        }

        // Sahne sağlam, tüm ihtimalleri dene.
        SolveResult result = solver.solve(scene);
        // Hiç çözüm yoksa
        if (!result.solvable()) {
            // oyuncu asla kazanamaz.
            return ValidationResponse.solved(List.of("Scene has no solution"), result);
        }
        // Dokunmadan kazanılıyorsa
        if (result.minTouches() == 0) {
            // ortada bulmaca yok demektir.
            return ValidationResponse.solved(List.of("Scene can be won without touching anything"), result);
        }
        // Sahne kullanılabilir.
        return ValidationResponse.solved(List.of(), result);
    }
}