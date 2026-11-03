// Bu dosyanın ait olduğu paket.
package com.tensec.api.solver;

// İki değer eşit değilse testi kaldırır.
import static org.junit.jupiter.api.Assertions.assertEquals;
// Değer false değilse testi kaldırır.
import static org.junit.jupiter.api.Assertions.assertFalse;
// Değer true değilse testi kaldırır.
import static org.junit.jupiter.api.Assertions.assertTrue;

// Motor.
import com.tensec.api.engine.Engine;
// Dokunuş tipi.
import com.tensec.api.engine.Touch;
// Sahne tipi.
import com.tensec.api.scene.Scene;
// Sahne okuyucu.
import com.tensec.api.scene.SceneLoader;
// Liste tipi.
import java.util.List;
// Test etiketi.
import org.junit.jupiter.api.Test;

// Çözücünün testleri.
class SolverTest {

    // Test edilen çözücü, gerçek motorla birlikte.
    private final Solver solver = new Solver(new Engine());
    // Kafe sahnesinin çözüm raporu.
    private final SolveResult cafe = solver.solve(SceneLoader.fromClasspath("scenes/cafe-candle.json"));

    // Bu metot bir test.
    @Test
    // 0, 1 ve 2 dokunuşlu tüm ihtimaller denenmeli: 1 + 50 + 1225.
    void triesEveryCombination() {
        // Toplam 1276 deneme.
        assertEquals(1276, cafe.combinationsTried());
    }

    // Bu metot bir test.
    @Test
    // Kafe sahnesi tek dokunuşla çözülebilir.
    void findsTheOneTouchSolution() {
        // Çözüm var.
        assertTrue(cafe.solvable());
        // En az 1 dokunuş gerekiyor.
        assertEquals(1, cafe.minTouches());
        // İlk çözüm: 3. saniyede makine.
        assertEquals(List.of(new Touch("machine", 3)), cafe.minimalSolutions().get(0));
    }

    // Bu metot bir test.
    @Test
    // 93 kazandıran kombinasyonun 46'sında gereksiz dokunuş yok.
    void separatesMinimalSolutionsFromWinningCombinations() {
        // 1 tek dokunuşlu + 92 iki dokunuşlu.
        assertEquals(93, cafe.winningCombinations());
        // 1 tek dokunuşlu + 45 gerçek iki dokunuşlu.
        assertEquals(46, cafe.minimalSolutions().size());
    }

    // Bu metot bir test.
    @Test
    // Zorluk: kazandırmayan kombinasyonların yüzdesi.
    void calculatesDifficulty() {
        // 1183 / 1276 = %92,7, yuvarlanınca 93.
        assertEquals(93, cafe.difficulty());
    }

    // Bu metot bir test.
    @Test
    // Hiçbir şekilde kurtarılamayan sahne çözümsüz sayılmalı.
    void reportsAnUnsolvableScene() {
        // Bombaya dokunmanın işe yaramadığı sahneyi çöz.
        SolveResult result = solver.solve(bombScene(false));

        // Çözüm yok.
        assertFalse(result.solvable());
        // Çözüm yoksa -1.
        assertEquals(-1, result.minTouches());
        // En yüksek zorluk.
        assertEquals(100, result.difficulty());
    }

    // Bu metot bir test.
    @Test
    // Dokunmadan da kazanılan sahneyi fark etmeli. Böyle bir sahne yayına girmemeli.
    void noticesASceneThatNeedsNoTouch() {
        // Felaket koşulunun hiç oluşmadığı sahneyi çöz.
        SolveResult result = solver.solve(bombScene(true));

        // Hiç dokunmadan kazanılıyor.
        assertEquals(0, result.minTouches());
    }

    // Yardımcı metot: testler için küçük bir sahne üretir.
    private Scene bombScene(boolean safe) {
        // safe ise tetikleyici zararsız duruma geçer, değilse bomba patlar.
        String triggerState = safe ? "ticking" : "exploded";
        // """ çok satırlı metin. Sondaki formatted, %s yerine triggerState yazar.
        return SceneLoader.parse("""
                {
                  "formatVersion": 1, "id": "bomb", "title": "Bomb", "intro": "Tick.",
                  "durationSeconds": 10, "maxTouches": 1, "grid": { "columns": 1, "rows": 1 },
                  "objects": [{
                    "id": "bomb", "label": "Bomb", "position": { "x": 0, "y": 0 },
                    "initialState": "ticking",
                    "states": { "ticking": { "emoji": "A" }, "exploded": { "emoji": "B" } }
                  }],
                  "touches": [],
                  "triggers": [{ "id": "go", "at": 5, "then": [{ "object": "bomb", "state": "%s" }] }],
                  "disasters": [{
                    "id": "boom", "when": [{ "object": "bomb", "state": "exploded" }], "caption": "Boom."
                  }]
                }
                """.formatted(triggerState));
    }
}