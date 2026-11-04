// Bu dosyanın ait olduğu paket.
package com.tensec.api.solver;

// Sahne tipi.
import com.tensec.api.scene.Scene;
// Aynı anda birden çok isteğin güvenle kullanabildiği tablo.
import java.util.Map;
// Tablo sınıfı.
import java.util.concurrent.ConcurrentHashMap;
// Spring'in "bu sınıfı sen yönet" etiketi.
import org.springframework.stereotype.Component;

// Bir sahnenin en az kaç dokunuşla çözüldüğünü bulur ve sonucu saklar.
// Çözücü on binlerce ihtimal denediği için her istekte çalıştırmak israf olurdu.
@Component
public class BestTouches {

    // Çözücü.
    private final Solver solver;
    // Sahne adı -> en az dokunuş.
    private final Map<String, Integer> cache = new ConcurrentHashMap<>();

    // Spring çözücüyü buraya kendisi verir.
    public BestTouches(Solver solver) {
        // Çözücüyü sakla.
        this.solver = solver;
    }

    // Sonuç saklıysa onu döndürür, yoksa hesaplayıp saklar.
    public int of(Scene scene) {
        // computeIfAbsent: anahtar yoksa fonksiyonu çalıştırır ve sonucu ekler.
        return cache.computeIfAbsent(scene.id(), id -> solver.solve(scene).minTouches());
    }
}
