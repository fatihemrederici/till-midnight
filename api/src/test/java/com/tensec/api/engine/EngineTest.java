// Bu dosyanın ait olduğu paket.
package com.tensec.api.engine;

// İki değer eşit değilse testi kaldırır.
import static org.junit.jupiter.api.Assertions.assertEquals;
// Değer false değilse testi kaldırır.
import static org.junit.jupiter.api.Assertions.assertFalse;
// Değer null değilse testi kaldırır.
import static org.junit.jupiter.api.Assertions.assertNull;
// Beklenen hata fırlatılmazsa testi kaldırır.
import static org.junit.jupiter.api.Assertions.assertThrows;
// Değer true değilse testi kaldırır.
import static org.junit.jupiter.api.Assertions.assertTrue;

// Sahne tipi.
import com.tensec.api.scene.Scene;
// Sahne okuyucu.
import com.tensec.api.scene.SceneLoader;
// Liste tipi.
import java.util.List;
// Test etiketi.
import org.junit.jupiter.api.Test;

// Motorun testleri.
class EngineTest {

    // Testlerin kullandığı kafe sahnesi.
    private final Scene cafe = SceneLoader.fromClasspath("scenes/cafe-candle.json");
    // Test edilen motor.
    private final Engine engine = new Engine();

    // Bu metot bir test.
    @Test
    // Dokunmazsan 8. saniyede makine patlamalı.
    void withoutTouchesTheMachineExplodesAtSecondEight() {
        // Boş dokunuş listesiyle kaybetmeyi kontrol et.
        assertLoss(List.of(), "explosion", 8);
    }

    // Bu metot bir test.
    @Test
    // Dokunmazsan olaylar dokümandaki sırayla çıkmalı.
    void withoutTouchesTheTimelineMatchesTheDocs() {
        // Sahneyi çalıştır, olayları al.
        List<String> timeline = engine.simulate(cafe, List.of()).events().stream()
                // Her olayı "saniye nesne yeniDurum" metnine çevir.
                .map(e -> e.second() + " " + e.object() + " " + e.to())
                // Sonucu listeye topla.
                .toList();

        // Beklenen liste ile karşılaştır.
        assertEquals(
                // Beklenen olaylar, sırayla.
                List.of(
                        "1 machine heating",
                        "2 cat prowling",
                        "3 machine pressure",
                        "4 candle fallen",
                        "4 cat hiding",
                        "6 curtain smoking",
                        "8 machine exploded"),
                // Motorun ürettiği olaylar.
                timeline);
    }

    // Bu metot bir test.
    @Test
    // 3. saniyede vanayı açmak tek dokunuşla kazandırmalı: buhar mumu da söndürür.
    void openingTheValveAtSecondThreeWinsWithOneTouch() {
        // Tek dokunuşla sahneyi çalıştır.
        SimulationResult result = engine.simulate(cafe, List.of(new Touch("machine", 3)));

        // Kazanmış olmalı.
        assertTrue(result.won());
        // Felaket olmamalı.
        assertNull(result.disasterId());
    }

    // Bu metot bir test.
    @Test
    // Vanayı geç açarsan patlama önlenir ama 9. saniyede yangın çıkar.
    void openingTheValveLaterRevealsTheFire() {
        // 5. saniyede vana: yangınla kaybetmeli.
        assertLoss(List.of(new Touch("machine", 5)), "fire", 9);
    }

    // Bu metot bir test.
    @Test
    // Sadece mumu söndürmek makineyi durdurmaz.
    void blowingOutTheCandleDoesNotStopTheMachine() {
        // Yine 8. saniyede patlamalı.
        assertLoss(List.of(new Touch("candle", 1)), "explosion", 8);
    }

    // Bu metot bir test.
    @Test
    // Tuzak: kovalanan kedi makinenin üstüne atlar.
    void shooingTheCatMakesItLandOnTheMachine() {
        // Patlama 4. saniyeye gelmeli.
        assertLoss(List.of(new Touch("cat", 3)), "explosion", 4);
    }

    // Bu metot bir test.
    @Test
    // Tuzak: dökülen su makinenin kablosuna ulaşır.
    void spillingWaterShortsTheMachine() {
        // Patlama 6. saniyeye gelmeli.
        assertLoss(List.of(new Touch("pitcher", 5)), "explosion", 6);
    }

    // Bu metot bir test.
    @Test
    // İki dokunuşla iki zincir de çözülebilmeli.
    void twoTouchesCanSolveBothChains() {
        // Mumu söndür, sonra vanayı aç.
        assertTrue(engine.simulate(cafe, List.of(new Touch("candle", 1), new Touch("machine", 5))).won());
        // Vanayı aç, suyu dök: su düşen mumu söndürür.
        assertTrue(engine.simulate(cafe, List.of(new Touch("machine", 4), new Touch("pitcher", 4))).won());
    }

    // Bu metot bir test.
    @Test
    // Dokunuşların listedeki sırası sonucu değiştirmemeli.
    void touchOrderDoesNotChangeTheResult() {
        // Birinci dokunuş.
        Touch candle = new Touch("candle", 1);
        // İkinci dokunuş.
        Touch machine = new Touch("machine", 5);

        // İki sıralama aynı sonucu vermeli.
        assertEquals(
                // Önce mum.
                engine.simulate(cafe, List.of(candle, machine)),
                // Önce makine.
                engine.simulate(cafe, List.of(machine, candle)));
    }

    // Bu metot bir test.
    @Test
    // Etkisiz dokunuş da olay listesine yazılmalı.
    void aTouchWithNoEffectIsRecorded() {
        // 1. saniyede makineye dokunmanın kuralı yok.
        List<TimelineEvent> events = engine.simulate(cafe, List.of(new Touch("machine", 1))).events();

        // Listede from ve to aynı olan bir olay bulunmalı.
        assertTrue(events.contains(new TimelineEvent(1, "machine", "heating", "heating", null, null)));
    }

    // Bu metot bir test.
    @Test
    // Kehanet ilk felakette durmamalı: kafede hem patlama hem yangın görünmeli.
    void foresightShowsEveryDisaster() {
        // Kehaneti al.
        Foresight foresight = engine.foresee(cafe);

        // İki felaket sırayla gelmeli.
        assertEquals(
                List.of("8 explosion", "9 fire"),
                foresight.disasters().stream().map(d -> d.second() + " " + d.id()).toList());
        // Patlamadan sonraki olay da listede olmalı.
        assertTrue(foresight.events().stream().anyMatch(e -> e.second() == 9 && e.to().equals("burning")));
    }

    // Bu metot bir test.
    @Test
    // Normal oyun yine ilk felakette bitmeli.
    void simulationStillStopsAtTheFirstDisaster() {
        // Son olay patlama olmalı, yangın gelmemeli.
        assertEquals(8, engine.simulate(cafe, List.of()).events().getLast().second());
    }

    // Bu metot bir test.
    @Test
    // Determinizm: aynı girdi aynı sonucu vermeli.
    void sameInputGivesTheSameResult() {
        // Aynı dokunuş listesi.
        List<Touch> touches = List.of(new Touch("machine", 5));

        // İki çalıştırma birebir aynı olmalı.
        assertEquals(engine.simulate(cafe, touches), engine.simulate(cafe, touches));
    }

    // Bu metot bir test.
    @Test
    // Geçersiz dokunuşlar reddedilmeli.
    void rejectsInvalidTouches() {
        // maxTouches 2 iken 3 dokunuş.
        assertInvalid(List.of(new Touch("cat", 0), new Touch("candle", 0), new Touch("curtain", 0)));
        // Aynı dokunuş iki kere.
        assertInvalid(List.of(new Touch("cat", 0), new Touch("cat", 0)));
        // Saniye çok büyük.
        assertInvalid(List.of(new Touch("cat", 10)));
        // Saniye negatif.
        assertInvalid(List.of(new Touch("cat", -1)));
        // Sahnede olmayan nesne.
        assertInvalid(List.of(new Touch("dog", 0)));
    }

    // Yardımcı metot: bu dokunuşlarla şu felaketle şu saniyede kaybedilmeli.
    private void assertLoss(List<Touch> touches, String disasterId, int second) {
        // Sahneyi çalıştır.
        SimulationResult result = engine.simulate(cafe, touches);

        // Kazanmamış olmalı.
        assertFalse(result.won());
        // Felaket beklenen olmalı.
        assertEquals(disasterId, result.disasterId());
        // Bitiş saniyesi beklenen olmalı.
        assertEquals(second, result.endSecond());
    }

    // Yardımcı metot: bu dokunuşlar hata fırlatmalı.
    private void assertInvalid(List<Touch> touches) {
        // Motor IllegalArgumentException fırlatmazsa test kalır.
        assertThrows(IllegalArgumentException.class, () -> engine.simulate(cafe, touches));
    }
}
