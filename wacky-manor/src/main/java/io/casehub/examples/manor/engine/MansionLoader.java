package io.casehub.examples.manor.engine;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.casehub.examples.manor.model.CharacterState;
import io.casehub.examples.manor.model.GameObject;
import io.casehub.examples.manor.model.Room;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MansionLoader {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RoomsFile(Map<String, RoomDef> rooms) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RoomDef(String name, String description, List<String> adjacentRooms,
                   Map<String, GameObject> objects) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CharactersFile(Map<String, CharacterDef> characters) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CharacterDef(String name, String startRoom, double startX, List<String> inventory) {}

    public static WorldState loadWorld() {
        return loadWorld("/mansion/rooms.yaml", "/mansion/characters.yaml");
    }

    public static WorldState loadWorld(String roomsPath, String charactersPath) {
        var rooms = loadRooms(roomsPath);
        var characters = loadCharacters(charactersPath);
        return new WorldState(rooms, characters);
    }

    private static Map<String, Room> loadRooms(String path) {
        try (var stream = MansionLoader.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("Resource not found: " + path);
            var file = YAML.readValue(stream, RoomsFile.class);
            var rooms = new LinkedHashMap<String, Room>();
            file.rooms().forEach((id, def) -> {
                var objectsWithIds = new LinkedHashMap<String, GameObject>();
                if (def.objects() != null) {
                    def.objects().forEach((objId, obj) ->
                        objectsWithIds.put(objId, new GameObject(
                            objId, obj.name(), obj.description(), obj.x(),
                            obj.visibleTo(), obj.portable(), obj.interactable(),
                            obj.interactionRequires(), obj.yields(), obj.usableWith())));
                }
                rooms.put(id, new Room(id, def.name(), def.description(),
                    def.adjacentRooms(), objectsWithIds));
            });
            return Map.copyOf(rooms);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Map<String, CharacterState> loadCharacters(String path) {
        try (var stream = MansionLoader.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("Resource not found: " + path);
            var file = YAML.readValue(stream, CharactersFile.class);
            var characters = new LinkedHashMap<String, CharacterState>();
            file.characters().forEach((id, def) ->
                characters.put(id, new CharacterState(
                    id, def.name(), def.startRoom(), def.startX(), def.inventory())));
            return characters;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
