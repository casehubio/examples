package io.casehub.examples.manor.engine;

import io.casehub.examples.manor.model.Beat;
import io.casehub.examples.manor.model.CharacterState;
import io.casehub.examples.manor.model.Scene;
import io.casehub.examples.manor.model.TriggerEffect;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public final class SceneDirector {

    private final Map<String, Scene> scenes;
    private final ConditionMatcher matcher = new ConditionMatcher();

    public SceneDirector(Map<String, Scene> scenes) {
        this.scenes = scenes;
    }

    public void runScene(String sceneId, WorldState world,
                         BiFunction<String, String, String> agentCaller,
                         Consumer<String> narratorCallback) {
        Scene scene = scenes.get(sceneId);
        if (scene == null) throw new IllegalArgumentException("Unknown scene: " + sceneId);

        for (Beat beat : scene.beats()) {
            if (beat.hasAlternatives()) {
                runAlternativeBeat(beat, world, agentCaller, narratorCallback);
            } else {
                runBeat(beat.narration(), beat.prompts(), beat.aside(),
                    beat.mechanicalEffect(), world, agentCaller, narratorCallback);
            }
        }

        world.markSceneCompleted(sceneId);
    }

    private void runAlternativeBeat(Beat beat, WorldState world,
                                     BiFunction<String, String, String> agentCaller,
                                     Consumer<String> narratorCallback) {
        for (Beat.BeatAlternative alt : beat.alternatives()) {
            if (matcher.matches(alt.condition(), world)) {
                runBeat(alt.narration(), alt.prompts(), false,
                    alt.mechanicalEffect(), world, agentCaller, narratorCallback);
                return;
            }
        }
        if (!beat.waitIfNoneMatch()) {
            runBeat(beat.narration(), beat.prompts(), beat.aside(),
                beat.mechanicalEffect(), world, agentCaller, narratorCallback);
        }
    }

    private void runBeat(String narration, Map<String, String> prompts, boolean aside,
                         Map<String, Object> mechanicalEffect, WorldState world,
                         BiFunction<String, String, String> agentCaller,
                         Consumer<String> narratorCallback) {
        if (narration != null) {
            narratorCallback.accept(narration);
        }

        for (var entry : prompts.entrySet()) {
            String characterId = entry.getKey();
            String prompt = entry.getValue();
            CharacterState character = world.character(characterId);
            if (character == null || !character.isActive()) continue;

            String response = agentCaller.apply(characterId, prompt);
            world.addEvent(aside ? "aside" : "dialogue",
                characterId, character.currentRoom(), response);
        }

        if (mechanicalEffect != null) {
            applyMechanicalEffect(mechanicalEffect, world);
        }
    }

    @SuppressWarnings("unchecked")
    private void applyMechanicalEffect(Map<String, Object> effect, WorldState world) {
        if (effect.containsKey("removeItem")) {
            var remove = (Map<String, String>) effect.get("removeItem");
            world.removeFromInventory(remove.get("character"), remove.get("item"));
        }
    }
}
