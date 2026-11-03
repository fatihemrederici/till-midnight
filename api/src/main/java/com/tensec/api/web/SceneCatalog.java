// Bu dosyanın ait olduğu paket.
package com.tensec.api.web;

// Sahne tipi.
import com.tensec.api.scene.Scene;
// Sahne okuyucu.
import com.tensec.api.scene.SceneLoader;
// Spring'in "bu sınıfı sen yönet" etiketi.
import org.springframework.stereotype.Component;

// Oyunun sahnelerini tutar. Şimdilik tek sahne var, veritabanı gelince sahneler oradan okunacak.
@Component
public class SceneCatalog {

    // Bugünün sahnesi. Uygulama açılırken bir kere okunur.
    private final Scene today = SceneLoader.fromClasspath("scenes/cafe-candle.json");

    // Bugünün sahnesini döndürür.
    public Scene today() {
        // Sakladığımız sahneyi ver.
        return today;
    }

    // Verilen adda sahne varsa döndürür, yoksa null döndürür.
    public Scene findById(String id) {
        // Şimdilik tek sahneyle karşılaştırıyoruz. today.id() önde olduğu için id null olsa da hata çıkmaz.
        return today.id().equals(id) ? today : null;
    }
}