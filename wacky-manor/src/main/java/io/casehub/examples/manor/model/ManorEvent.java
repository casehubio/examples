package io.casehub.examples.manor.model;

import java.time.Instant;

public sealed interface ManorEvent {

    Instant timestamp();
    String characterId();
    String room();
    String description();
    String type();

    default ActionType actionType() { return null; }
    default String target() { return null; }
    default String withItem() { return null; }
    default String departureRoom() { return null; }
    default String detailedDescription() { return null; }
    default boolean concealed() { return false; }
    default String dialogueTarget() { return null; }

    static ManorEvent of(String type, String characterId, String room, String description) {
        return switch (type) {
            case "action" -> new Action(Instant.now(), characterId, room, description,
                    null, null, null, null);
            case "dialogue" -> new Dialogue(Instant.now(), characterId, room, description);
            case "aside" -> new Aside(Instant.now(), characterId, room, description);
            case "narrator" -> new Narrator(Instant.now(), characterId, room, description);
            default -> throw new IllegalArgumentException("Unknown event type: " + type);
        };
    }

    record Action(Instant timestamp, String characterId, String room, String description,
                  ActionType actionType, String target, String withItem, String departureRoom,
                  String detailedDescription, boolean concealed) implements ManorEvent {

        public Action(Instant timestamp, String characterId, String room, String description,
                      ActionType actionType, String target, String withItem, String departureRoom) {
            this(timestamp, characterId, room, description, actionType, target, withItem, departureRoom, null, false);
        }

        @Override public String type() { return "action"; }
    }

    record Dialogue(Instant timestamp, String characterId, String room, String description,
                    String detailedDescription, String dialogueTarget) implements ManorEvent {

        public Dialogue(Instant timestamp, String characterId, String room, String description) {
            this(timestamp, characterId, room, description, null, null);
        }

        @Override public String type() { return "dialogue"; }
    }

    record Aside(Instant timestamp, String characterId, String room, String description) implements ManorEvent {
        @Override public String type() { return "aside"; }
    }

    record Narrator(Instant timestamp, String characterId, String room, String description) implements ManorEvent {
        @Override public String type() { return "narrator"; }
    }
}
