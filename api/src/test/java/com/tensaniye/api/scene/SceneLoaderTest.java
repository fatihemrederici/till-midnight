package com.tensec.api.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

class SceneLoaderTest {

    private final Scene cafe = SceneLoader.fromClasspath("scenes/cafe-candle.json");

    @Test
    void readsTheCafeScene() {
        assertEquals("cafe-candle", cafe.id());
        assertEquals(10, cafe.durationSeconds());
        assertEquals(5, cafe.objects().size());
        assertEquals("🕯️", cafe.objects().get(1).states().get("lit").emoji());
    }

    @Test
    void readsBothTriggerKinds() {
        Scene.Trigger timed = trigger("cat-wakes");
        assertEquals(2, timed.at());
        assertNull(timed.after());

        Scene.Trigger chained = trigger("curtain-ignites");
        assertNull(chained.at());
        assertEquals(new Scene.After("curtain", "smoking", 3), chained.after());
    }

    @Test
    void missingWhenBecomesAnEmptyList() {
        assertTrue(trigger("machine-heats").when().isEmpty());
    }

    @Test
    void rejectsUnknownFields() {
        String typo = """
                {"formatVersion": 1, "id": "x", "titel": "typo"}
                """;
        assertThrows(JacksonException.class, () -> SceneLoader.parse(typo));
    }

    private Scene.Trigger trigger(String id) {
        Scene.Trigger found = cafe.triggers().stream()
                .filter(t -> t.id().equals(id))
                .findFirst()
                .orElse(null);
        assertNotNull(found, "no trigger named " + id);
        return found;
    }
}
