package io.casehub.examples.manor.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.casehub.examples.manor.model.CharacterState;
import io.casehub.examples.manor.model.GameObject;
import io.casehub.examples.manor.model.Room;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ManorWebSocketEvent(
        String type,
        String characterId,
        String room,
        Double x,
        String content,
        String sceneId,
        String status,
        String objectId,
        Boolean visible,
        String visibleTo,
        List<CharacterSnapshot> characters,
        List<RoomSnapshot> rooms) {

    public record CharacterSnapshot(String id, String name, String room, double x, boolean active) {
        public static CharacterSnapshot from(CharacterState c) {
            return new CharacterSnapshot(c.agentId(), c.name(), c.currentRoom(), c.x(), c.isActive());
        }
    }

    public record RoomSnapshot(String id, String name, List<ObjectSnapshot> objects) {
        public record ObjectSnapshot(String id, String name, double x) {}
    }

    public static ManorWebSocketEvent snapshot(List<CharacterSnapshot> characters, List<RoomSnapshot> rooms) {
        return new ManorWebSocketEvent("snapshot", null, null, null, null, null, null, null, null, null, characters, rooms);
    }

    public static ManorWebSocketEvent position(String characterId, String room, double x) {
        return new ManorWebSocketEvent("position", characterId, room, x, null, null, null, null, null, null, null, null);
    }

    public static ManorWebSocketEvent dialogue(String characterId, String room, String content) {
        return new ManorWebSocketEvent("dialogue", characterId, room, null, content, null, null, null, null, null, null, null);
    }

    public static ManorWebSocketEvent aside(String characterId, String content) {
        return new ManorWebSocketEvent("aside", characterId, null, null, content, null, null, null, null, null, null, null);
    }

    public static ManorWebSocketEvent narrator(String content) {
        return new ManorWebSocketEvent("narrator", null, null, null, content, null, null, null, null, null, null, null);
    }

    public static ManorWebSocketEvent scene(String sceneId, String status) {
        return new ManorWebSocketEvent("scene", null, null, null, null, sceneId, status, null, null, null, null, null);
    }

    public static ManorWebSocketEvent scenario(String status) {
        return new ManorWebSocketEvent("scenario", null, null, null, null, null, status, null, null, null, null, null);
    }

    public static ManorWebSocketEvent object(String objectId, String room, boolean visible, String visibleTo) {
        return new ManorWebSocketEvent("object", null, room, null, null, null, null, objectId, visible, visibleTo, null, null);
    }
}
