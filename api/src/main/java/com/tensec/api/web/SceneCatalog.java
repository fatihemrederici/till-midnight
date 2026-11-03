// Bu dosyanın ait olduğu paket.
package com.tensec.api.web;

// Sahne tipi.
import com.tensec.api.scene.Scene;
// Sahne okuyucu.
import com.tensec.api.scene.SceneLoader;
// Saat dilimi olmayan tarih tipi.
import java.time.LocalDate;
// UTC saat dilimi.
import java.time.ZoneOffset;
// Liste tipi.
import java.util.List;
// Spring'in "bu sınıfı sen yönet" etiketi.
import org.springframework.stereotype.Component;

// Oyunun sahnelerini tutar. Şimdilik dosyalardan okuyor, veritabanı gelince sahneler oradan okunacak.
@Component
public class SceneCatalog {

    // Tüm sahneler. Uygulama açılırken bir kere okunur.
    private final List<Scene> scenes = List.of(
            SceneLoader.fromClasspath("scenes/cafe-candle.json"),
            SceneLoader.fromClasspath("scenes/tram-bend.json"),
            SceneLoader.fromClasspath("scenes/dorm-kitchen.json"));

    // Tüm sahneleri döndürür.
    public List<Scene> all() {
        // Sakladığımız listeyi ver. List.of ile oluştuğu için dışarıdan değiştirilemez.
        return scenes;
    }

    // Bugünün sahnesini döndürür. Herkes aynı sahneyi görsün diye UTC tarihini kullanıyoruz.
    public Scene today() {
        // Bugünün tarihiyle sahneyi seç.
        return forDate(LocalDate.now(ZoneOffset.UTC));
    }

    // Verilen tarihin sahnesini döndürür. Tarihi dışarıdan aldığı için testi kolay.
    public Scene forDate(LocalDate date) {
        // toEpochDay: 1 Ocak 1970'ten beri geçen gün sayısı.
        // floorMod: sayıyı 0, 1, 2, 0, 1, 2... şeklinde döndürür, böylece sahneler sırayla gelir.
        int index = Math.floorMod(date.toEpochDay(), scenes.size());
        // O sıradaki sahneyi döndür.
        return scenes.get(index);
    }

    // Verilen adda sahne varsa döndürür, yoksa null döndürür.
    public Scene findById(String id) {
        // Tüm sahneleri dön.
        for (Scene scene : scenes) {
            // scene.id() önde olduğu için id null olsa da hata çıkmaz.
            if (scene.id().equals(id)) {
                // Bulunan sahneyi döndür.
                return scene;
            }
        }
        // Bulunamadı.
        return null;
    }
}