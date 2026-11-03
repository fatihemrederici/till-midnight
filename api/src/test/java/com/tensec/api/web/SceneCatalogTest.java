// Bu dosyanın ait olduğu paket.
package com.tensec.api.web;

// İki değer eşit değilse testi kaldırır.
import static org.junit.jupiter.api.Assertions.assertEquals;
// Değer null değilse testi kaldırır.
import static org.junit.jupiter.api.Assertions.assertNull;
// Değer true değilse testi kaldırır.
import static org.junit.jupiter.api.Assertions.assertTrue;

// Motor.
import com.tensec.api.engine.Engine;
// Sahne tipi.
import com.tensec.api.scene.Scene;
// Anlam kontrolü yapan sınıf.
import com.tensec.api.scene.SceneValidator;
// Çözücünün raporu.
import com.tensec.api.solver.SolveResult;
// Çözücü.
import com.tensec.api.solver.Solver;
// Tarih tipi.
import java.time.LocalDate;
// Liste tipi.
import java.util.List;
// Test etiketi.
import org.junit.jupiter.api.Test;

// Sahne kataloğunun testleri.
class SceneCatalogTest {

    // Test edilen katalog.
    private final SceneCatalog catalog = new SceneCatalog();

    // Bu metot bir test.
    @Test
    // Elle yazılan her sahne denetimden geçmeli. Yeni sahne eklenince otomatik kontrol edilir.
    void everySceneIsValidAndNeedsAtLeastOneTouch() {
        // Doğrulayıcı.
        SceneValidator validator = new SceneValidator();
        // Çözücü.
        Solver solver = new Solver(new Engine());

        // Tüm sahneleri dön.
        for (Scene scene : catalog.all()) {
            // Anlam hatası olmamalı.
            assertEquals(List.of(), validator.validate(scene), scene.id());
            // Sahneyi çöz.
            SolveResult result = solver.solve(scene);
            // Çözümü olmalı.
            assertTrue(result.solvable(), scene.id());
            // Dokunmadan kazanılmamalı.
            assertTrue(result.minTouches() >= 1, scene.id());
        }
    }

    // Bu metot bir test.
    @Test
    // Aynı tarih her zaman aynı sahneyi vermeli, yoksa oyuncular farklı sahne görür.
    void sameDateGivesTheSameScene() {
        // Bir tarih seç.
        LocalDate date = LocalDate.of(2026, 12, 3);

        // İki çağrı aynı sahneyi döndürmeli.
        assertEquals(catalog.forDate(date), catalog.forDate(date));
    }

    // Bu metot bir test.
    @Test
    // Sahneler sırayla gelmeli ve bitince başa dönmeli.
    void scenesRotateDayByDay() {
        // Başlangıç tarihi.
        LocalDate date = LocalDate.of(2026, 12, 3);
        // Sahne sayısı.
        int count = catalog.all().size();

        // Ertesi gün farklı sahne olmalı.
        assertTrue(!catalog.forDate(date).equals(catalog.forDate(date.plusDays(1))));
        // Sahne sayısı kadar gün sonra aynı sahneye dönmeli.
        assertEquals(catalog.forDate(date), catalog.forDate(date.plusDays(count)));
    }

    // Bu metot bir test.
    @Test
    // Sahne adla bulunabilmeli.
    void findsScenesById() {
        // Var olan sahne bulunmalı.
        assertEquals("tram-bend", catalog.findById("tram-bend").id());
        // Olmayan sahne null dönmeli.
        assertNull(catalog.findById("nope"));
        // null ad hata fırlatmamalı.
        assertNull(catalog.findById(null));
    }
}