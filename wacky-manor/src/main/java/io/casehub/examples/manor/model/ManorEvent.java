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
        String departureRoom) {

    public ManorEvent(Instant timestamp, String type, String characterId,
                      String room, String description) {
        this(timestamp, type, characterId, room, description,
             null, null, null, null);
    }
}
