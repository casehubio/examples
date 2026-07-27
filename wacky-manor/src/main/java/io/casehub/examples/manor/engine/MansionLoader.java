package io.casehub.examples.manor.engine;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.casehub.examples.manor.model.Beat;
import io.casehub.examples.manor.model.CharacterState;
import io.casehub.examples.manor.model.GameObject;
import io.casehub.examples.manor.model.Room;
import io.casehub.examples.manor.model.Scene;
import io.casehub.examples.manor.model.Trigger;
import io.casehub.examples.manor.model.TriggerCondition;
import io.casehub.examples.manor.model.TriggerEffect;

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
            if (stream == null) {throw new IllegalStateException("Resource not found: " + path);}
            var file  = YAML.readValue(stream, RoomsFile.class);
            var rooms = new LinkedHashMap<String, Room>();
            file.rooms().forEach((id, def) -> {
                var objectsWithIds = new LinkedHashMap<String, GameObject>();
                if (def.objects() != null) {
                    def.objects().forEach((objId, obj) ->
                                                  objectsWithIds.put(objId, new GameObject(
                                                          objId, obj.name(), obj.description(), obj.x(),
                                                          obj.visibleTo(), obj.portable(), obj.interactable(),
                                                          obj.interactionRequires(), obj.yields(), obj.usableWith(),
                                                          obj.itemId())));
                }
                rooms.put(id, new Room(id, def.name(), def.description(),
                                       def.adjacentRooms(), objectsWithIds));
            });
            return Map.copyOf(rooms);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }}

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

    @SuppressWarnings("unchecked")
    public static List<Trigger> loadTriggers() {
        return loadTriggers("/mansion/triggers.yaml");
    }

    @SuppressWarnings("unchecked")
    public static List<Trigger> loadTriggers(String path) {
        try (var stream = MansionLoader.class.getResourceAsStream(path)) {
            if (stream == null) {throw new IllegalStateException("Resource not found: " + path);}
            Map<String, Object> root = YAML.readValue(stream,
                                                      new com.fasterxml.jackson.core.type.TypeReference<>() {});
            var triggerDefs = (List<Map<String, Object>>) root.get("triggers");
            return triggerDefs.stream().map(MansionLoader::parseTrigger).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Trigger parseTrigger(Map<String, Object> map) {
        String  id         = (String) map.get("id");
        var     condMap    = (Map<String, Object>) map.get("condition");
        var     effectList = (List<Map<String, Object>>) map.get("effects");
        boolean once       = Boolean.TRUE.equals(map.get("once"));

        TriggerCondition condition = parseCondition(condMap);
        List<TriggerEffect> effects = effectList.stream()
                                                .map(MansionLoader::parseEffect).toList();
        return new Trigger(id, condition, effects, once);
    }

    @SuppressWarnings("unchecked")
    static TriggerCondition parseCondition(Map<String, Object> map) {
        if (map.containsKey("characterInRoom")) {
            var v = (Map<String, String>) map.get("characterInRoom");
            return new TriggerCondition.CharacterInRoom(v.get("character"), v.get("room"));
        }
        if (map.containsKey("characterHasItem")) {
            var v = (Map<String, String>) map.get("characterHasItem");
            return new TriggerCondition.CharacterHasItem(v.get("character"), v.get("item"));
        }
        if (map.containsKey("objectInRoom")) {
            var v = (Map<String, String>) map.get("objectInRoom");
            return new TriggerCondition.ObjectInRoom(v.get("object"), v.get("room"));
        }
        if (map.containsKey("sceneCompleted")) {
            return new TriggerCondition.SceneCompleted((String) map.get("sceneCompleted"));
        }
        if (map.containsKey("allOf")) {
            var conditions = (List<Map<String, Object>>) map.get("allOf");
            return new TriggerCondition.AllOf(
                    conditions.stream().map(MansionLoader::parseCondition).toList());
        }
        throw new IllegalArgumentException("Unknown trigger condition: " + map);
    }

    @SuppressWarnings("unchecked")
    static TriggerEffect parseEffect(Map<String, Object> map) {
        if (map.containsKey("revealObject")) {
            var v = (Map<String, String>) map.get("revealObject");
            return new TriggerEffect.RevealObject(v.get("object"), v.get("room"));
        }
        if (map.containsKey("startScene")) {
            return new TriggerEffect.StartScene((String) map.get("startScene"));
        }
        if (map.containsKey("narratorEvent")) {
            return new TriggerEffect.NarratorEvent((String) map.get("narratorEvent"));
        }
        if (map.containsKey("completeScenario")) {
            return new TriggerEffect.CompleteScenario();
        }
        if (map.containsKey("removeItem")) {
            var v = (Map<String, String>) map.get("removeItem");
            return new TriggerEffect.RemoveItem(v.get("character"), v.get("item"));
        }
        throw new IllegalArgumentException("Unknown trigger effect: " + map);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Scene> loadScenes() {
        return loadScenes("/mansion/scenes.yaml");
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Scene> loadScenes(String path) {
        try (var stream = MansionLoader.class.getResourceAsStream(path)) {
            if (stream == null) {throw new IllegalStateException("Resource not found: " + path);}
            Map<String, Object> root = YAML.readValue(stream,
                                                      new com.fasterxml.jackson.core.type.TypeReference<>() {});
            var sceneDefs = (Map<String, Map<String, Object>>) root.get("scenes");
            var scenes    = new LinkedHashMap<String, Scene>();
            sceneDefs.forEach((id, def) -> scenes.put(id, parseScene(id, def)));
            return Map.copyOf(scenes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Scene parseScene(String id, Map<String, Object> map) {
        var beatDefs = (List<Map<String, Object>>) map.get("beats");
        var beats    = beatDefs.stream().map(MansionLoader::parseBeat).toList();
        return new Scene(id, beats);
    }

    @SuppressWarnings("unchecked")
    private static Beat parseBeat(Map<String, Object> map) {
        String beatId    = (String) map.get("id");
        String narration = (String) map.get("narration");
        var prompts = map.containsKey("prompts")
                      ? (Map<String, String>) map.get("prompts") : Map.<String, String>of();
        boolean aside           = Boolean.TRUE.equals(map.get("aside"));
        boolean waitIfNoneMatch = Boolean.TRUE.equals(map.get("waitIfNoneMatch"));
        var mechanicalEffect = map.containsKey("mechanicalEffect")
                               ? (Map<String, Object>) map.get("mechanicalEffect") : null;

        List<Beat.BeatAlternative> alternatives = List.of();
        if (map.containsKey("alternatives")) {
            var altDefs = (List<Map<String, Object>>) map.get("alternatives");
            alternatives = altDefs.stream().map(MansionLoader::parseAlternative).toList();
        }

        return new Beat(beatId, narration, prompts, aside, alternatives,
                        mechanicalEffect, waitIfNoneMatch);
    }

    @SuppressWarnings("unchecked")
    private static Beat.BeatAlternative parseAlternative(Map<String, Object> map) {
        String altId     = (String) map.get("id");
        var    condMap   = (Map<String, Object>) map.get("condition");
        String narration = (String) map.get("narration");
        var prompts = map.containsKey("prompts")
                      ? (Map<String, String>) map.get("prompts") : Map.<String, String>of();
        var mechanicalEffect = map.containsKey("mechanicalEffect")
                               ? (Map<String, Object>) map.get("mechanicalEffect") : null;

        return new Beat.BeatAlternative(altId, parseCondition(condMap),
                                        narration, prompts, mechanicalEffect);
    }


}
