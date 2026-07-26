package io.casehub.examples.manor.engine;

import io.casehub.examples.manor.model.CharacterState;
import io.casehub.examples.manor.model.Trigger;
import io.casehub.examples.manor.model.TriggerEffect;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TriggerEvaluator {

    private final List<Trigger> triggers;
    private final ConditionMatcher matcher = new ConditionMatcher();
    private final Set<String> firedOnce = new HashSet<>();

    public TriggerEvaluator(List<Trigger> triggers) {
        this.triggers = triggers;
    }

    public TriggerResult evaluate(WorldState world) {
        String sceneId = null;
        var narratorEvents = new ArrayList<String>();

        for (Trigger trigger : triggers) {
            if (trigger.once() && firedOnce.contains(trigger.id())) continue;
            if (!matcher.matches(trigger.condition(), world)) continue;

            if (trigger.once()) firedOnce.add(trigger.id());

            for (TriggerEffect effect : trigger.effects()) {
                switch (effect) {
                    case TriggerEffect.RevealObject r -> {
                        for (CharacterState c : world.charactersInRoom(r.room())) {
                            world.revealObject(r.object(), c.agentId());
                        }
                    }
                    case TriggerEffect.StartScene s ->
                        sceneId = s.sceneId();
                    case TriggerEffect.NarratorEvent n ->
                        narratorEvents.add(n.text());
                    case TriggerEffect.CompleteScenario ignored ->
                        world.setScenarioComplete();
                    case TriggerEffect.RemoveItem r ->
                        world.removeFromInventory(r.character(), r.item());
                }
            }
        }
        return new TriggerResult(sceneId != null, sceneId, narratorEvents);
    }

    public record TriggerResult(
            boolean hasSceneStart,
            String sceneId,
            List<String> narratorEvents) {}
}
