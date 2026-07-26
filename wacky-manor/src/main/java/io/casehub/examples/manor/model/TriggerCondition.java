package io.casehub.examples.manor.model;

import java.util.List;

public sealed interface TriggerCondition {
    record CharacterInRoom(String character, String room) implements TriggerCondition {}
    record CharacterHasItem(String character, String item) implements TriggerCondition {}
    record ObjectInRoom(String object, String room) implements TriggerCondition {}
    record SceneCompleted(String sceneId) implements TriggerCondition {}
    record AllOf(List<TriggerCondition> conditions) implements TriggerCondition {}
}
