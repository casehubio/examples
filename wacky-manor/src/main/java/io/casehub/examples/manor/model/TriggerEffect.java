package io.casehub.examples.manor.model;

public sealed interface TriggerEffect {
    record RevealObject(String object, String room) implements TriggerEffect {}
    record StartScene(String sceneId) implements TriggerEffect {}
    record NarratorEvent(String text) implements TriggerEffect {}
    record CompleteScenario() implements TriggerEffect {}
    record RemoveItem(String character, String item) implements TriggerEffect {}
}
