package com.tensec.api.engine;

import com.tensec.api.scene.Scene;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** Runs a scene second by second. Deterministic: the same input always gives the same result. */
public class Engine {

    public SimulationResult simulate(Scene scene, List<Touch> touches) {
        validate(scene, touches);

        Map<String, String> states = new HashMap<>();
        Map<String, Integer> enteredAt = new HashMap<>();
        List<TimelineEvent> events = new ArrayList<>();

        for (Scene.SceneObject object : scene.objects()) {
            states.put(object.id(), object.initialState());
            enteredAt.put(object.id(), 0);
        }

        // Sorted so the order the player made the touches in cannot change the result.
        List<Touch> sortedTouches = new ArrayList<>(touches);
        sortedTouches.sort(Comparator.comparing(Touch::object));

        for (int second = 0; second <= scene.durationSeconds(); second++) {
            // Decide what is due before anything changes, so triggers waiting on the same state all fire.
            List<Scene.Trigger> dueTriggers = new ArrayList<>();
            for (Scene.Trigger trigger : scene.triggers()) {
                if (isDue(trigger, second, states, enteredAt)) {
                    dueTriggers.add(trigger);
                }
            }

            for (Scene.Trigger trigger : dueTriggers) {
                if (conditionsHold(trigger.when(), states)) {
                    applyEffects(trigger.then(), trigger.id(), trigger.caption(), second, states, enteredAt, events);
                }
            }

            for (Touch touch : sortedTouches) {
                if (touch.second() == second) {
                    applyTouch(scene, touch, states, enteredAt, events);
                }
            }

            for (Scene.Disaster disaster : scene.disasters()) {
                if (conditionsHold(disaster.when(), states)) {
                    return new SimulationResult(false, second, disaster.id(), events);
                }
            }
        }

        return new SimulationResult(true, scene.durationSeconds(), null, events);
    }

    private void validate(Scene scene, List<Touch> touches) {
        if (touches.size() > scene.maxTouches()) {
            throw new IllegalArgumentException("Too many touches: at most " + scene.maxTouches() + " allowed");
        }
        if (new HashSet<>(touches).size() != touches.size()) {
            throw new IllegalArgumentException("Duplicate touch");
        }
        for (Touch touch : touches) {
            if (touch.second() < 0 || touch.second() >= scene.durationSeconds()) {
                throw new IllegalArgumentException("Touch second out of range: " + touch.second());
            }
            boolean objectExists = scene.objects().stream().anyMatch(o -> o.id().equals(touch.object()));
            if (!objectExists) {
                throw new IllegalArgumentException("Unknown object: " + touch.object());
            }
        }
    }

    private void applyTouch(
            Scene scene,
            Touch touch,
            Map<String, String> states,
            Map<String, Integer> enteredAt,
            List<TimelineEvent> events) {
        for (Scene.TouchRule rule : scene.touches()) {
            if (rule.object().equals(touch.object()) && conditionsHold(rule.when(), states)) {
                applyEffects(rule.then(), rule.id(), rule.caption(), touch.second(), states, enteredAt, events);
                return;
            }
        }
        String current = states.get(touch.object());
        events.add(new TimelineEvent(touch.second(), touch.object(), current, current, null, null));
    }

    private boolean isDue(
            Scene.Trigger trigger, int second, Map<String, String> states, Map<String, Integer> enteredAt) {
        if (trigger.at() != null) {
            return trigger.at() == second;
        }
        Scene.After after = trigger.after();
        return states.get(after.object()).equals(after.state())
                && enteredAt.get(after.object()) == second - after.delay();
    }

    private boolean conditionsHold(List<Scene.StateRef> conditions, Map<String, String> states) {
        for (Scene.StateRef condition : conditions) {
            if (!states.get(condition.object()).equals(condition.state())) {
                return false;
            }
        }
        return true;
    }

    private void applyEffects(
            List<Scene.StateRef> effects,
            String ruleId,
            String caption,
            int second,
            Map<String, String> states,
            Map<String, Integer> enteredAt,
            List<TimelineEvent> events) {
        for (Scene.StateRef effect : effects) {
            String current = states.get(effect.object());
            if (current.equals(effect.state())) {
                continue;
            }
            states.put(effect.object(), effect.state());
            enteredAt.put(effect.object(), second);
            events.add(new TimelineEvent(second, effect.object(), current, effect.state(), ruleId, caption));
        }
    }
}