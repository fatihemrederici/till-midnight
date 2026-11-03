// Bu dosyanın ait olduğu paket.
package com.tensec.api.scene;

// İki değer eşit değilse testi kaldıran metot.
import static org.junit.jupiter.api.Assertions.assertEquals;
// Değer null ise testi kaldıran metot.
import static org.junit.jupiter.api.Assertions.assertNotNull;
// Değer null değilse testi kaldıran metot.
import static org.junit.jupiter.api.Assertions.assertNull;
// Beklenen hata fırlatılmazsa testi kaldıran metot.
import static org.junit.jupiter.api.Assertions.assertThrows;
// Değer true değilse testi kaldıran metot.
import static org.junit.jupiter.api.Assertions.assertTrue;

// Test etiketi.
import org.junit.jupiter.api.Test;
// Jackson'ın JSON okuma hatası tipi.
import tools.jackson.core.JacksonException;

// SceneLoader sınıfının testleri.
class SceneLoaderTest {

    // Tüm testlerin kullandığı kafe sahnesi.
    private final Scene cafe = SceneLoader.fromClasspath("scenes/cafe-candle.json");

    // Bu metot bir test.
    @Test
    // Kafe sahnesi doğru okunuyor mu?
    void readsTheCafeScene() {
        // Sahne adı doğru mu?
        assertEquals("cafe-candle", cafe.id());
        // Süre 10 mu?
        assertEquals(10, cafe.durationSeconds());
        // 5 nesne var mı?
        assertEquals(5, cafe.objects().size());
        // İkinci nesnenin (mum) "lit" durumundaki emojisi doğru mu?
        assertEquals("🕯️", cafe.objects().get(1).states().get("lit").emoji());
    }

    // Bu metot bir test.
    @Test
    // İki tetikleyici çeşidi de doğru okunuyor mu?
    void readsBothTriggerKinds() {
        // Sabit saniyeli tetikleyiciyi bul.
        Scene.Trigger timed = trigger("cat-wakes");
        // 2. saniyede çalışmalı.
        assertEquals(2, timed.at());
        // after alanı boş olmalı.
        assertNull(timed.after());

        // Zincir tetikleyiciyi bul.
        Scene.Trigger chained = trigger("curtain-ignites");
        // at alanı boş olmalı.
        assertNull(chained.at());
        // after değerleri doğru olmalı. Record'lar içeriğe göre karşılaştırılır.
        assertEquals(new Scene.After("curtain", "smoking", 3), chained.after());
    }

    // Bu metot bir test.
    @Test
    // JSON'da when yoksa boş liste oluyor mu?
    void missingWhenBecomesAnEmptyList() {
        // Bu tetikleyicide when yok, liste boş olmalı.
        assertTrue(trigger("machine-heats").when().isEmpty());
    }

    // Bu metot bir test.
    @Test
    // Yanlış yazılmış alan reddediliyor mu?
    void rejectsUnknownFields() {
        // """ çok satırlı metin demektir. "titel" bilerek yanlış yazıldı.
        String typo = """
                {"formatVersion": 1, "id": "x", "titel": "typo"}
                """;
        // Okuma hata fırlatmalı. () -> ... çalıştırılacak kod parçası.
        assertThrows(JacksonException.class, () -> SceneLoader.parse(typo));
    }

    // Yardımcı metot: adı verilen tetikleyiciyi bulur.
    private Scene.Trigger trigger(String id) {
        // Tetikleyici listesini akışa çevir.
        Scene.Trigger found = cafe.triggers().stream()
                // Sadece adı eşleşeni bırak.
                .filter(t -> t.id().equals(id))
                // İlkini al.
                .findFirst()
                // Bulunamazsa null döndür.
                .orElse(null);
        // Bulunamadıysa anlaşılır mesajla testi kaldır.
        assertNotNull(found, "no trigger named " + id);
        // Bulunan tetikleyiciyi döndür.
        return found;
    }
}
