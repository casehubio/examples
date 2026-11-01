package io.casehub.examples.manor.agent;

import io.casehub.examples.manor.engine.MansionLoader;
import io.casehub.examples.manor.engine.WorldState;
import io.casehub.examples.manor.model.ActionType;
import io.casehub.examples.manor.model.ManorEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ObservationServiceTest {

    WorldState createWorld() {
        return MansionLoader.loadWorld();
    }

    ObservationService createService() {
        var compactor = new MechanicalCompactor();
        var renderer = new ManorObservationRenderer(compactor, 10, 15, null);
        return new ObservationService(renderer);
    }

    @Test
    void publishEvent_routesToCharactersInSameRoom() {
        var world = createWorld();
        var service = createService();
        service.init(world);

        var event = new ManorEvent.Dialogue(Instant.now(), "penelope-pitstop",
                "entrance-hall", "Penelope: Hello!");
        service.publishEvent(event);

        var drain = service.drain("hooded-claw", System.currentTimeMillis());
        assertThat(drain.currentPartition().eventCount()).isEqualTo(1);
    }

    @Test
    void publishEvent_skipsCharactersInDifferentRoom() {
        var world = createWorld();
        var service = createService();
        service.init(world);

        world.moveCharacter("penelope-pitstop", "kitchen");
        var event = new ManorEvent.Dialogue(Instant.now(), "penelope-pitstop",
                "kitchen", "Penelope: Hello from the Kitchen!");
        service.publishEvent(event);

        var drain = service.drain("hooded-claw", System.currentTimeMillis());
        assertThat(drain.currentPartition().eventCount()).isZero();
    }

    @Test
    void publishEvent_nullRoom_silentlySkipped() {
        var world = createWorld();
        var service = createService();
        service.init(world);

        var event = new ManorEvent.Narrator(Instant.now(), null, null,
                "The chandelier creaks ominously!");
        service.publishEvent(event);

        var drain = service.drain("penelope-pitstop", System.currentTimeMillis());
        assertThat(drain.currentPartition().eventCount()).isZero();
    }

    @Test
    void publishEvent_asideOnlyRoutesToSpeaker() {
        var world = createWorld();
        var service = createService();
        service.init(world);

        var event = new ManorEvent.Aside(Instant.now(), "hooded-claw",
                "entrance-hall", "Nyah-ha-ha!");
        service.publishEvent(event);

        var hcDrain = service.drain("hooded-claw", System.currentTimeMillis());
        assertThat(hcDrain.currentPartition().eventCount()).isEqualTo(1);

        var penelopeDrain = service.drain("penelope-pitstop", System.currentTimeMillis());
        assertThat(penelopeDrain.currentPartition().eventCount()).isZero();
    }

    @Test
    void roomTransition_previousRoomBecomesRemembered() {
        var world = createWorld();
        var service = createService();
        service.init(world);

        var event = new ManorEvent.Dialogue(Instant.now(), "penelope-pitstop",
                "entrance-hall", "Penelope: Hello!");
        service.publishEvent(event);

        world.moveCharacter("hooded-claw", "kitchen");

        var drain = service.drain("hooded-claw", System.currentTimeMillis());
        assertThat(drain.currentPartition().eventCount()).isZero();
        assertThat(drain.rememberedPartitions()).containsKey("entrance-hall");
        assertThat(drain.rememberedPartitions().get("entrance-hall").result().eventCount()).isEqualTo(1);
    }

    @Test
    void moveEvent_routesToDepartureRoomObservers() {
        var world = createWorld();
        var service = createService();
        service.init(world);

        var moveEvent = new ManorEvent.Action(Instant.now(), "penelope-pitstop",
                "kitchen", "Penelope walked to the Kitchen.",
                ActionType.MOVE, "kitchen", null, "entrance-hall");
        world.moveCharacter("penelope-pitstop", "kitchen");
        service.publishEvent(moveEvent);

        var drain = service.drain("hooded-claw", System.currentTimeMillis());
        assertThat(drain.currentPartition().eventCount()).isEqualTo(1);
        assertThat(drain.currentPartition().renderedText()).contains("walked");
    }

    @Test
    void drain_unknownCharacter_returnsEmpty() {
        var world = createWorld();
        var service = createService();
        service.init(world);

        var drain = service.drain("nonexistent", System.currentTimeMillis());
        assertThat(drain.currentPartition().eventCount()).isZero();
        assertThat(drain.rememberedPartitions()).isEmpty();
    }

    @Test
    void concealed_event_hidden_from_non_perceptive_observer() {
        var world = createWorld();
        world.character("peter-perfect").setCapabilityTags(java.util.Set.of("social"));
        var service = createService();
        service.init(world);

        var event = new ManorEvent.Action(Instant.now(), "hooded-claw", "entrance-hall",
                                   "Sneekly did something.", ActionType.STEAL, "muttley", "brass-key", null, null, true);
        service.publishEvent(event);

        var drain = service.drain("peter-perfect", System.currentTimeMillis());
        assertThat(drain.currentPartition().eventCount()).isZero();
    }

    @Test
    void concealed_event_visible_to_perceptive_observer() {
        var world = createWorld();
        world.character("ant-hill-mob").setCapabilityTags(java.util.Set.of("perception", "protection"));
        var service = createService();
        service.init(world);

        var event = new ManorEvent.Action(Instant.now(), "hooded-claw", "entrance-hall",
                                   "Sneekly did something.", ActionType.STEAL, "muttley", "brass-key", null, null, true);
        service.publishEvent(event);

        var drain = service.drain("ant-hill-mob", System.currentTimeMillis());
        assertThat(drain.currentPartition().eventCount()).isEqualTo(1);
    }

    @Test
    void concealed_event_always_visible_to_victim() {
        var world = createWorld();
        world.character("muttley").setCapabilityTags(java.util.Set.of("motivation"));
        var service = createService();
        service.init(world);

        var event = new ManorEvent.Action(Instant.now(), "hooded-claw", "entrance-hall",
                                   "Sneekly did something.", ActionType.STEAL, "muttley", "brass-key", null, null, true);
        service.publishEvent(event);

        var drain = service.drain("muttley", System.currentTimeMillis());
        assertThat(drain.currentPartition().eventCount()).isEqualTo(1);
    }

    @Test
    void levelResolver_dialogueGetsHighestLevel() {
        var event = new ManorEvent.Dialogue(Instant.now(), "penelope-pitstop",
                                   "entrance-hall", "Penelope: Hello!");
        assertThat(ObservationService.resolveLevel(event))
                .isEqualTo(ObservationService.DIALOGUE);
    }

    @Test
    void levelResolver_asideGetsSameLevelAsDialogue() {
        var event = new ManorEvent.Aside(Instant.now(), "hooded-claw",
                                   "entrance-hall", "Nyah-ha-ha!");
        assertThat(ObservationService.resolveLevel(event))
                .isEqualTo(ObservationService.DIALOGUE);
    }

    @Test
    void levelResolver_actionGetsMiddleLevel() {
        var event = new ManorEvent.Action(Instant.now(), "penelope-pitstop",
                                   "kitchen", "Penelope looks around.",
                                   ActionType.LOOK, null, null, null);
        assertThat(ObservationService.resolveLevel(event))
                .isEqualTo(ObservationService.ACTION);
    }

    @Test
    void levelResolver_moveGetsLowestLevel() {
        var event = new ManorEvent.Action(Instant.now(), "penelope-pitstop",
                                   "kitchen", "Penelope walked to the Kitchen.",
                                   ActionType.MOVE, "kitchen", null, "entrance-hall");
        assertThat(ObservationService.resolveLevel(event))
                .isEqualTo(ObservationService.MOVEMENT);
    }

    @Test
    void drain_withMindMapStore_persistsObservationNode() {
        var world   = createWorld();
        var service = createService();
        service.init(world);
        var store = new io.casehub.neocortex.mindmap.inmem.InMemoryMindMapStore();
        service.setMindMapStore(store, "test-tenant");

        var event = new ManorEvent.Dialogue(Instant.now(), "penelope-pitstop",
                                            "entrance-hall", "Penelope: Hello!");
        service.publishEvent(event);
        service.drain("hooded-claw", System.currentTimeMillis());

        var subgraphs = store.listSubgraphs("test-tenant");
        assertThat(subgraphs).hasSize(1);
        assertThat(subgraphs.getFirst().name()).isEqualTo("observations-hooded-claw");

        var nodes = store.nodesIn(subgraphs.getFirst().id(), "test-tenant");
        assertThat(nodes).hasSize(1);
        var node = nodes.getFirst();
        assertThat(node.properties().get("content")).contains("Hello!");
        assertThat(node.properties().get("tier")).isEqualTo("verbatim");
        assertThat(node.properties().get("eventCount")).isEqualTo("1");
        assertThat(node.properties().get("room")).isEqualTo("entrance-hall");
        assertThat(node.provenance()).isEqualTo("tiered-observation-renderer");
    }

    @Test
    void drain_emptyRoom_doesNotPersistNode() {
        var world   = createWorld();
        var service = createService();
        service.init(world);
        var store = new io.casehub.neocortex.mindmap.inmem.InMemoryMindMapStore();
        service.setMindMapStore(store, "test-tenant");

        service.drain("hooded-claw", System.currentTimeMillis());

        assertThat(store.listSubgraphs("test-tenant")).isEmpty();
    }

    @Test
    void drain_withoutMindMapStore_noError() {
        var world   = createWorld();
        var service = createService();
        service.init(world);

        var event = new ManorEvent.Dialogue(Instant.now(), "penelope-pitstop",
                                            "entrance-hall", "Penelope: Hello!");
        service.publishEvent(event);

        var drain = service.drain("hooded-claw", System.currentTimeMillis());
        assertThat(drain.currentPartition().eventCount()).isEqualTo(1);
    }

    @Test
    void publishEvent_autoRegistersObserverAfterIdRemap() {
        var world   = createWorld();
        var service = createService();
        service.init(world);

        var idMap = new java.util.HashMap<String, String>();
        idMap.put("penelope-pitstop", "clara-bellingham");
        idMap.put("hooded-claw", "vincent-marsh");
        world.remapCharacterIds(idMap);

        var event = new ManorEvent.Dialogue(Instant.now(), "clara-bellingham",
                                            "entrance-hall", "Clara: Hello everyone!");
        service.publishEvent(event);

        var drain = service.drain("vincent-marsh", System.currentTimeMillis());
        assertThat(drain.currentPartition().eventCount()).isEqualTo(1);
    }

    @Test
    void drain_persistsObservationAfterIdRemap() {
        var world   = createWorld();
        var service = createService();
        service.init(world);
        var store = new io.casehub.neocortex.mindmap.inmem.InMemoryMindMapStore();
        service.setMindMapStore(store, "test-tenant");

        var idMap = new java.util.HashMap<String, String>();
        idMap.put("penelope-pitstop", "clara-bellingham");
        idMap.put("hooded-claw", "vincent-marsh");
        world.remapCharacterIds(idMap);

        var event = new ManorEvent.Dialogue(Instant.now(), "clara-bellingham",
                                            "entrance-hall", "Clara: Hello!");
        service.publishEvent(event);
        service.drain("vincent-marsh", System.currentTimeMillis());

        var subgraphs = store.listSubgraphs("test-tenant");
        assertThat(subgraphs).hasSize(1);
        assertThat(subgraphs.getFirst().name()).isEqualTo("observations-vincent-marsh");

        var nodes = store.nodesIn(subgraphs.getFirst().id(), "test-tenant");
        assertThat(nodes).hasSize(1);
        assertThat(nodes.getFirst().properties().get("content")).contains("Hello!");
        assertThat(nodes.getFirst().properties().get("room")).isEqualTo("entrance-hall");
    }


}
