package com.tensec.api.engine;

import com.tensec.api.scene.Scene;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Runs a scene second by second. Deterministic: the same input always gives the same result. */
public class Engine {

    public SimulationResult simulate(Scene scene) {
        Map<String, String> states = new HashMap<>();
        Map<String, Integer> enteredAt = new HashMap<>();
        List<TimelineEvent> events = new ArrayList<>();

        for (Scene.SceneObject object : scene.objects()) {
            states.put(object.id(), object.initialState());
            enteredAt.put(object.id(), 0);
        }

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

            for (Scene.Disaster disaster : scene.disasters()) {
                if (conditionsHold(disaster.when(), states)) {
                    return new SimulationResult(false, second, disaster.id(), events);
                }
            }
        }

        return new SimulationResult(true, scene.durationSeconds(), null, events);
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
