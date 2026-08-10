package io.casehub.examples.manor.model;

import java.time.Instant;

public record ManorEvent(
        Instant timestamp,
        String type,
        String characterId,
        String room,
        String description,
        ActionType actionType,
        String target,
        String withItem,
        String departureRoom,
        String detailedDescription,
        boolean concealed,
        String dialogueTarget) {

    public ManorEvent(Instant timestamp, String type, String characterId,
                      String room, String description) {
        this(timestamp, type, characterId, room, description,
             null, null, null, null, null, false, null);
    }

    public ManorEvent(Instant timestamp, String type, String characterId,
                      String room, String description,
                      ActionType actionType, String target, String withItem,
                      String departureRoom) {
        this(timestamp, type, characterId, room, description,
             actionType, target, withItem, departureRoom, null, false, null);
    }

    public ManorEvent(Instant timestamp, String type, String characterId,
                      String room, String description,
                      ActionType actionType, String target, String withItem,
                      String departureRoom, String detailedDescription) {
        this(timestamp, type, characterId, room, description,
             actionType, target, withItem, departureRoom, detailedDescription, false, null);
    }

    public ManorEvent(Instant timestamp, String type, String characterId,
                      String room, String description,
                      ActionType actionType, String target, String withItem,
                      String departureRoom, String detailedDescription,
                      boolean concealed) {
        this(timestamp, type, characterId, room, description,
             actionType, target, withItem, departureRoom, detailedDescription, concealed, null);
    }
}
