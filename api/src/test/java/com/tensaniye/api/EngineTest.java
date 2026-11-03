package com.tensec.api.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.tensec.api.scene.Scene;
import com.tensec.api.scene.SceneLoader;
import java.util.List;
import org.junit.jupiter.api.Test;

class EngineTest {

    private final Scene cafe = SceneLoader.fromClasspath("scenes/cafe-candle.json");
    private final Engine engine = new Engine();

    @Test
    void withoutTouchesTheMachineExplodesAtSecondEight() {
        SimulationResult result = engine.simulate(cafe);

        assertFalse(result.won());
        assertEquals(8, result.endSecond());
        assertEquals("explosion", result.disasterId());
    }

    @Test
    void withoutTouchesTheTimelineMatchesTheDocs() {
        List<String> timeline = engine.simulate(cafe).events().stream()
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
    void sameInputGivesTheSameResult() {
        assertEquals(engine.simulate(cafe), engine.simulate(cafe));
    }
}