package io.casehub.examples.manor.engine;

import io.casehub.examples.manor.model.TriggerCondition;

public final class ConditionMatcher {

    public boolean matches(TriggerCondition condition, WorldState world) {
        return switch (condition) {
            case TriggerCondition.CharacterInRoom c ->
                c.room().equals(world.character(c.character()).currentRoom());
            case TriggerCondition.CharacterHasItem c ->
                world.character(c.character()).hasItem(c.item());
            case TriggerCondition.ObjectInRoom o ->
                world.room(o.room()) != null &&
                    world.room(o.room()).objects().containsKey(o.object());
            case TriggerCondition.SceneCompleted s ->
                world.isSceneCompleted(s.sceneId());
            case TriggerCondition.AllOf all ->
                all.conditions().stream().allMatch(c -> matches(c, world));
        };
    }
}
