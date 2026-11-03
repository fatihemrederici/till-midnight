package com.tensaniye.api.scene;

import java.util.List;
import java.util.Map;

/**
 * A scene as stored in JSON. See docs/scene-format.md for the rules.
 * Touches, triggers and disasters are the puzzle's solution space: never send them to the client.
 */
public record Scene(
        int formatVersion,
        String id,
        String title,
        String intro,
        int durationSeconds,
        int maxTouches,
        Grid grid,
        List<SceneObject> objects,
        List<TouchRule> touches,
        List<Trigger> triggers,
        List<Disaster> disasters) {

    public record Grid(int columns, int rows) {}

    public record Position(int x, int y) {}

    public record StateView(String emoji) {}

    public record SceneObject(
            String id,
            String label,
            Position position,
            String initialState,
            Map<String, StateView> states) {}

    /** "This object is in this state": a condition in `when`, an effect in `then`. */
    public record StateRef(String object, String state) {}

    public record TouchRule(
            String id,
            String object,
            List<StateRef> when,
            List<StateRef> then,
            String caption) {

        public TouchRule {
            when = when == null ? List.of() : List.copyOf(when);
        }
    }

    /** Fires at a fixed second ({@code at}) or a delay after an object enters a state ({@code after}). */
    public record Trigger(
            String id,
            Integer at,
            After after,
            List<StateRef> when,
            List<StateRef> then,
            String caption) {

        public Trigger {
            when = when == null ? List.of() : List.copyOf(when);
        }
    }

    public record After(String object, String state, int delay) {}

    public record Disaster(String id, List<StateRef> when, String caption) {}
}
