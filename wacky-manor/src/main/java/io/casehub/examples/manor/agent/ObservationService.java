package io.casehub.examples.manor.agent;

import io.casehub.blocks.summarisation.EventLevel;
import io.casehub.blocks.summarisation.LevelEvent;
import io.casehub.blocks.summarisation.observation.ObservationResult;
import io.casehub.examples.manor.engine.WorldState;
import io.casehub.examples.manor.model.ActionType;
import io.casehub.examples.manor.model.CharacterState;
import io.casehub.examples.manor.model.ManorEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ObservationService {

    static final EventLevel MANOR = new EventLevel("manor", 0);

    private final ManorObservationRenderer renderer;
    private final ConcurrentHashMap<String, CharacterObservationState> characterStates
            = new ConcurrentHashMap<>();
    private WorldState worldState;

    public ObservationService(ManorObservationRenderer renderer) {
        this.renderer = renderer;
    }

    public void init(WorldState worldState) {
        this.worldState = worldState;
        characterStates.clear();
        for (var entry : worldState.characters().entrySet()) {
            characterStates.put(entry.getKey(),
                    new CharacterObservationState(entry.getValue().currentRoom(), renderer));
        }
    }

    public void publishEvent(ManorEvent event) {
        if (event.room() == null) return;

        for (var entry : characterStates.entrySet()) {
            String charId = entry.getKey();
            CharacterObservationState charState = entry.getValue();
            CharacterState character = worldState.character(charId);
            String charRoom = character.currentRoom();

            boolean routed = false;

            if (charRoom.equals(event.room())) {
                if ("aside".equals(event.type()) && !charId.equals(event.characterId())) {
                    continue;
                }
                charState.accumulatorFor(charRoom).collect(
                        new LevelEvent<>(event, event.timestamp().toEpochMilli(), MANOR));
                routed = true;
            }

            if (!routed
                    && event.actionType() == ActionType.MOVE
                    && event.departureRoom() != null
                    && charRoom.equals(event.departureRoom())
                    && !charId.equals(event.characterId())) {
                charState.accumulatorFor(charRoom).collect(
                        new LevelEvent<>(event, event.timestamp().toEpochMilli(), MANOR));
            }
        }
    }

    public ObservationDrain drain(String characterId, long now) {
        CharacterObservationState charState = characterStates.get(characterId);
        if (charState == null) {
            return new ObservationDrain(ObservationResult.empty(0), Map.of());
        }
        CharacterState character = worldState.character(characterId);
        String currentRoom = character.currentRoom();

        ObservationResult currentRoomResult = charState.accumulatorFor(currentRoom)
                .drainObservation(now).toCompletableFuture().join();

        var remembered = new LinkedHashMap<String, RememberedRoom>();
        for (var accEntry : charState.accumulators().entrySet()) {
            String roomId = accEntry.getKey();
            if (roomId.equals(currentRoom)) continue;

            var cached = charState.rememberedDrainCache().get(roomId);
            if (cached != null) {
                remembered.put(roomId, cached);
            } else {
                var roomResult = accEntry.getValue()
                        .drainObservation(now).toCompletableFuture().join();
                if (roomResult.eventCount() > 0) {
                    var rememberedRoom = new RememberedRoom(roomResult, now);
                    charState.rememberedDrainCache().put(roomId, rememberedRoom);
                    remembered.put(roomId, rememberedRoom);
                }
            }
        }

        return new ObservationDrain(currentRoomResult, remembered);
    }
}
