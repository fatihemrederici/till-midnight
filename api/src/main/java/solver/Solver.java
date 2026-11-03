// Bu dosyanın ait olduğu paket.
package com.tensec.api.solver;

// Sahneleri çalıştıracak motor.
import com.tensec.api.engine.Engine;
// Dokunuş tipi.
import com.tensec.api.engine.Touch;
// Sahne tipi.
import com.tensec.api.scene.Scene;
// Değiştirilebilir liste.
import java.util.ArrayList;
// Liste arayüzü.
import java.util.List;
// Spring'in "bu sınıfı sen yönet" etiketi.
import org.springframework.stereotype.Component;

// @Component: Spring bu sınıftan bir nesne oluşturur ve Engine'i constructor'ına kendisi verir.
@Component
// Bir sahnedeki tüm dokunuş ihtimallerini deneyip çözümleri bulur. Buna brute force (kaba kuvvet) denir.
public class Solver {

    // Çözücü, sahneyi gerçek motorla çalıştırır. Böylece kurallar iki yerde yazılmaz.
    private final Engine engine;

    // Constructor: çözücü oluşturulurken motoru dışarıdan alır.
    public Solver(Engine engine) {
        // Gelen motoru sakla.
        this.engine = engine;
    }

    // Ana metot: sahneyi çözer ve raporu döndürür.
    public SolveResult solve(Scene scene) {
        // Mümkün olan tüm tek dokunuşların listesi.
        List<Touch> allTouches = allPossibleTouches(scene);
        // Bulunan gereksiz dokunuşsuz çözümler.
        List<List<Touch>> minimalSolutions = new ArrayList<>();
        // Denenen kombinasyon sayacı.
        int combinationsTried = 0;
        // Kazandıran kombinasyon sayacı.
        int winningCombinations = 0;

        // Önce 0 dokunuş, sonra 1, sonra 2... Küçükten büyüğe gitmek gereksiz dokunuşları ayırmayı sağlar.
        for (int size = 0; size <= scene.maxTouches(); size++) {
            // Bu boyuttaki tüm kombinasyonları üret.
            List<List<Touch>> combinations = new ArrayList<>();
            // Üretimi yardımcı metoda bırak. Başlangıçta seçili dokunuş yok.
            collectCombinations(allTouches, 0, size, new ArrayList<>(), combinations);

            // Her kombinasyonu dene.
            for (List<Touch> touches : combinations) {
                // Bir deneme yaptık.
                combinationsTried++;
                // Motorla çalıştır. Kazanmadıysa
                if (!engine.simulate(scene, touches).won()) {
                    // sıradakine geç.
                    continue;
                }
                // Kazandıran kombinasyon sayısını artır.
                winningCombinations++;
                // Bilinen daha küçük bir çözümü içermiyorsa bu da yeni bir çözümdür.
                if (!containsAny(touches, minimalSolutions)) {
                    // Çözüm listesine ekle.
                    minimalSolutions.add(touches);
                }
            }
        }

        // Çözüm yoksa -1. Varsa ilk bulunan en küçüktür, çünkü küçükten büyüğe gittik.
        int minTouches = minimalSolutions.isEmpty() ? -1 : minimalSolutions.get(0).size();
        // Kazandırmayan kombinasyonların yüzdesi zorluk puanımız. Math.round yuvarlar.
        int difficulty = (int) Math.round(100.0 * (combinationsTried - winningCombinations) / combinationsTried);

        // Raporu oluştur.
        return new SolveResult(combinationsTried, winningCombinations, minTouches, minimalSolutions, difficulty);
    }

    // Her nesne için her saniyeyi birer dokunuş olarak listeler.
    private List<Touch> allPossibleTouches(Scene scene) {
        // Sonuç listesi.
        List<Touch> touches = new ArrayList<>();
        // Her nesneyi dön.
        for (Scene.SceneObject object : scene.objects()) {
            // Her saniyeyi dön. Son saniyeye dokunulamaz, bu yüzden < kullanıyoruz.
            for (int second = 0; second < scene.durationSeconds(); second++) {
                // Bu dokunuşu ekle.
                touches.add(new Touch(object.id(), second));
            }
        }
        // Listeyi döndür.
        return touches;
    }

    // Özyineli (recursive) metot: kendini çağırarak size elemanlı tüm grupları üretir.
    private void collectCombinations(
            // Seçim yapılacak tüm dokunuşlar.
            List<Touch> allTouches,
            // Sıradaki seçimin başlayacağı yer. Aynı grubu iki kez üretmemek için.
            int start,
            // İstenen grup boyutu.
            int size,
            // Şu ana kadar seçilenler.
            List<Touch> current,
            // Tamamlanan grupların yazıldığı liste.
            List<List<Touch>> combinations) {
        // Yeterince seçtiysek
        if (current.size() == size) {
            // grubun kopyasını kaydet. Kopya şart, çünkü current değişmeye devam edecek.
            combinations.add(List.copyOf(current));
            // Bu dal bitti.
            return;
        }
        // Kalan dokunuşları sırayla dene.
        for (int i = start; i < allTouches.size(); i++) {
            // Bu dokunuşu seç.
            current.add(allTouches.get(i));
            // Kalanları seçmek için kendini çağır. i + 1: sadece ileriye bak.
            collectCombinations(allTouches, i + 1, size, current, combinations);
            // Seçimi geri al ki bir sonraki ihtimal denenebilsin. Buna backtracking denir.
            current.remove(current.size() - 1);
        }
    }

    // Bu dokunuşlar, bilinen çözümlerden birini tamamen içeriyor mu?
    private boolean containsAny(List<Touch> touches, List<List<Touch>> solutions) {
        // Her çözümü dön.
        for (List<Touch> solution : solutions) {
            // Çözümün tüm dokunuşları burada varsa
            if (touches.containsAll(solution)) {
                // içeriyor demektir.
                return true;
            }
        }
        // Hiçbirini içermiyor.
        return false;
    }
}