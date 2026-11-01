package io.casehub.examples.manor.agent;

import io.casehub.agency.summarisation.EventLevel;
import io.casehub.agency.perception.observation.ObservationResult;
import io.casehub.agency.perception.observation.PartitionedDrain;
import io.casehub.agency.perception.observation.PartitionedObservationService;
import io.casehub.agency.perception.observation.VisibilityPolicy;
import io.casehub.examples.manor.engine.WorldState;
import io.casehub.examples.manor.model.ActionType;
import io.casehub.examples.manor.model.CharacterState;
import io.casehub.examples.manor.model.ManorEvent;
import io.casehub.neocortex.mindmap.MindMapStore;
import io.casehub.neocortex.mindmap.NodeInput;
import io.casehub.neocortex.mindmap.SubgraphInput;
import io.casehub.neocortex.mindmap.SubgraphTypes;
import io.casehub.platform.api.identity.PrincipalId;

import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class ObservationService {

    static final EventLevel DIALOGUE = new EventLevel("dialogue", 30);
    static final EventLevel ACTION   = new EventLevel("action", 20);
    static final EventLevel MOVEMENT = new EventLevel("movement", 10);

    private static final String PROVENANCE = "tiered-observation-renderer";

    static EventLevel resolveLevel(ManorEvent event) {
        if (event.actionType() == ActionType.MOVE) return MOVEMENT;
        return switch (event.type()) {
            case "dialogue", "aside" -> DIALOGUE;
            default -> ACTION;
        };
    }

    private final ManorObservationRenderer                          renderer;
    private       PartitionedObservationService<ManorEvent, String> delegate;
    private       WorldState                                        worldState;
    private       MindMapStore                                      mindMapStore;
    private       String                                            tenantId;
    private final ConcurrentHashMap<String, String>                 subgraphIdCache = new ConcurrentHashMap<>();

    public ObservationService(ManorObservationRenderer renderer) {
        this.renderer = renderer;
    }

    public void init(WorldState worldState) {
        this.worldState = worldState;
        VisibilityPolicy<ManorEvent, String> policy = this::resolveVisibility;
        this.delegate = new PartitionedObservationService<>(
                renderer, policy, e -> e.timestamp().toEpochMilli(),
                ObservationService::resolveLevel);
        for (var entry : worldState.characters().entrySet()) {
            delegate.addObserver(entry.getKey(), entry.getValue().currentRoom());
        }
    }

    public void setMindMapStore(MindMapStore mindMapStore, String tenantId) {
        this.mindMapStore = mindMapStore;
        this.tenantId = tenantId;
    }

    public void publishEvent(ManorEvent event) {
        delegate.publishEvent(event);
    }

    public PartitionedDrain<String> drain(String characterId, long now) {
        CharacterState character = worldState.character(characterId);
        if (character == null) {
            return new PartitionedDrain<>(ObservationResult.empty(0), java.util.Map.of());
        }
        var drain = delegate.drain(characterId, character.currentRoom(), now);
        persistObservation(characterId, character.currentRoom(), drain.currentPartition());
        return drain;
    }

    private void persistObservation(String characterId, String room, ObservationResult result) {
        if (mindMapStore == null || result.eventCount() == 0) return;

        var subgraphId = subgraphIdCache.computeIfAbsent(characterId, id -> {
            var name = "observations-" + id;
            var created = mindMapStore.createSubgraph(
                    new SubgraphInput(name, SubgraphTypes.ACTIVITY, null), tenantId);
            return created != null && !created.isBlank() ? created : name;
        });

        var now = Instant.now();
        var props = new HashMap<String, String>();
        props.put("content", result.renderedText());
        props.put("tier", result.tier() != null ? result.tier().name() : "unknown");
        props.put("eventCount", String.valueOf(result.eventCount()));
        props.put("timeSinceLastDrain", String.valueOf(result.timeSinceLastDrain()));
        props.put("room", room);

        var nodeInput = NodeInput.of("observation-" + now.toEpochMilli(), subgraphId)
                .withProperties(props)
                .withProvenance(PROVENANCE)
                .withPrincipalId(PrincipalId.agent(characterId))
                .withValidFrom(now);

        mindMapStore.addNode(nodeInput, tenantId);
    }

    private java.util.Map<String, java.util.Set<String>> resolveVisibility(ManorEvent event) {
        if (event.room() == null) {return java.util.Map.of();}

        var result = new java.util.HashMap<String, java.util.Set<String>>();
        for (var entry : worldState.characters().entrySet()) {
            String         charId    = entry.getKey();
            CharacterState character = entry.getValue();
            String         charRoom  = character.currentRoom();

            if (charRoom.equals(event.room())) {
                if ("aside".equals(event.type()) && !charId.equals(event.characterId())) {
                    continue;
                }
                if (event.concealed() && !charId.equals(event.characterId())) {
                    boolean isVictim      = charId.equals(event.target());
                    boolean hasPerception = character.capabilityTags().contains("perception");
                    if (!isVictim && !hasPerception) {
                        continue;
                    }
                }
                result.put(charId, java.util.Set.of(charRoom));
            } else if (event.actionType() == ActionType.MOVE
                       && event.departureRoom() != null
                       && charRoom.equals(event.departureRoom())
                       && !charId.equals(event.characterId())) {
                result.put(charId, java.util.Set.of(charRoom));
            }
        }
        return result;}
}
