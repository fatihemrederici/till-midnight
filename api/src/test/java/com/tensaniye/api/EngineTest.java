package com.tensec.api.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tensec.api.scene.Scene;
import com.tensec.api.scene.SceneLoader;
import java.util.List;
import org.junit.jupiter.api.Test;

class EngineTest {

    private final Scene cafe = SceneLoader.fromClasspath("scenes/cafe-candle.json");
    private final Engine engine = new Engine();

    @Test
    void withoutTouchesTheMachineExplodesAtSecondEight() {
        assertLoss(List.of(), "explosion", 8);
    }

    @Test
    void withoutTouchesTheTimelineMatchesTheDocs() {
        List<String> timeline = engine.simulate(cafe, List.of()).events().stream()
                .map(e -> e.second() + " " + e.object() + " " + e.to())
                .toList();

        assertEquals(
                List.of(
                        "1 machine heating",
                        "2 cat prowling",
                        "3 machine pressure",
                        "4 candle fallen",
                        "4 cat hiding",
                        "6 curtain smoking",
                        "8 machine exploded"),
                timeline);
    }

    @Test
    void openingTheValveAtSecondThreeWinsWithOneTouch() {
        SimulationResult result = engine.simulate(cafe, List.of(new Touch("machine", 3)));

        assertTrue(result.won());
        assertNull(result.disasterId());
    }

    @Test
    void openingTheValveLaterRevealsTheFire() {
        assertLoss(List.of(new Touch("machine", 5)), "fire", 9);
    }

    @Test
    void blowingOutTheCandleDoesNotStopTheMachine() {
        assertLoss(List.of(new Touch("candle", 1)), "explosion", 8);
    }

    @Test
    void shooingTheCatMakesItLandOnTheMachine() {
        assertLoss(List.of(new Touch("cat", 3)), "explosion", 4);
    }

    @Test
    void spillingWaterShortsTheMachine() {
        assertLoss(List.of(new Touch("pitcher", 5)), "explosion", 6);
    }

    @Test
    void twoTouchesCanSolveBothChains() {
        assertTrue(engine.simulate(cafe, List.of(new Touch("candle", 1), new Touch("machine", 5))).won());
        assertTrue(engine.simulate(cafe, List.of(new Touch("machine", 4), new Touch("pitcher", 4))).won());
    }

    @Test
    void touchOrderDoesNotChangeTheResult() {
        Touch candle = new Touch("candle", 1);
        Touch machine = new Touch("machine", 5);

        assertEquals(
                engine.simulate(cafe, List.of(candle, machine)),
                engine.simulate(cafe, List.of(machine, candle)));
    }

    @Test
    void aTouchWithNoEffectIsRecorded() {
        List<TimelineEvent> events = engine.simulate(cafe, List.of(new Touch("machine", 1))).events();

        assertTrue(events.contains(new TimelineEvent(1, "machine", "heating", "heating", null, null)));
    }

    @Test
    void sameInputGivesTheSameResult() {
        List<Touch> touches = List.of(new Touch("machine", 5));

        assertEquals(engine.simulate(cafe, touches), engine.simulate(cafe, touches));
    }

    @Test
    void rejectsInvalidTouches() {
        assertInvalid(List.of(new Touch("cat", 0), new Touch("candle", 0), new Touch("curtain", 0)));
        assertInvalid(List.of(new Touch("cat", 0), new Touch("cat", 0)));
        assertInvalid(List.of(new Touch("cat", 10)));
        assertInvalid(List.of(new Touch("cat", -1)));
        assertInvalid(List.of(new Touch("dog", 0)));
    }

    private void assertLoss(List<Touch> touches, String disasterId, int second) {
        SimulationResult result = engine.simulate(cafe, touches);

        assertFalse(result.won());
        assertEquals(disasterId, result.disasterId());
        assertEquals(second, result.endSecond());
    }

    private void assertInvalid(List<Touch> touches) {
        assertThrows(IllegalArgumentException.class, () -> engine.simulate(cafe, touches));
    }
}