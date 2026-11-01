package io.casehub.examples.manor.agent;

import io.casehub.neocortex.mindmap.MindMapNode;
import io.casehub.neocortex.mindmap.MindMapStore;
import io.casehub.neocortex.mindmap.NodeInput;
import io.casehub.neocortex.mindmap.SubgraphInput;
import io.casehub.neocortex.mindmap.SubgraphTypes;
import io.casehub.neocortex.mindmap.inmem.InMemoryMindMapStore;
import io.casehub.platform.api.identity.PrincipalId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ObservationCompactionPhaseTest {

    private static final String TENANT = "test-tenant";
    private MindMapStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryMindMapStore();
    }

    @Test
    void compactSubgraph_noCompactionWhenBelowWindow() {
        var sgId = store.createSubgraph(new SubgraphInput("observations-penelope", SubgraphTypes.ACTIVITY, null), TENANT);
        addObservation(sgId, "penelope", 1, "Room A", "Event 1");
        addObservation(sgId, "penelope", 2, "Room A", "Event 2");

        var phase = new ObservationCompactionPhase(store, 10, 5);
        int compacted = phase.compactSubgraph(sgId, "penelope", TENANT);

        assertThat(compacted).isZero();
        assertThat(store.nodesIn(sgId, TENANT)).hasSize(2);
    }

    @Test
    void compactSubgraph_condensesOlderNodesIntoBatches() {
        var sgId = store.createSubgraph(new SubgraphInput("observations-penelope", SubgraphTypes.ACTIVITY, null), TENANT);
        for (int i = 1; i <= 15; i++) {
            addObservation(sgId, "penelope", i, "Room " + (i % 3), "Event " + i);
        }

        var phase = new ObservationCompactionPhase(store, 5, 5);
        int compacted = phase.compactSubgraph(sgId, "penelope", TENANT);

        assertThat(compacted).isEqualTo(10);

        var remaining = store.nodesIn(sgId, TENANT);
        long originals = remaining.stream().filter(n -> !n.traits().contains("Condensed")).count();
        long condensed = remaining.stream().filter(n -> n.traits().contains("Condensed")).count();

        assertThat(originals).isEqualTo(5);
        assertThat(condensed).isEqualTo(2);
    }

    @Test
    void compactSubgraph_condensedNodeHasCorrectProperties() {
        var sgId = store.createSubgraph(new SubgraphInput("observations-muttley", SubgraphTypes.ACTIVITY, null), TENANT);
        for (int i = 1; i <= 8; i++) {
            addObservation(sgId, "muttley", i, i <= 3 ? "Library" : "Kitchen", "Content " + i);
        }

        var phase = new ObservationCompactionPhase(store, 3, 5);
        phase.compactSubgraph(sgId, "muttley", TENANT);

        var condensedNodes = store.nodesIn(sgId, TENANT).stream()
                .filter(n -> n.traits().contains("Condensed"))
                .toList();

        assertThat(condensedNodes).hasSize(1);
        var node = condensedNodes.getFirst();
        assertThat(node.provenance()).isEqualTo("observation-compaction");
        assertThat(node.properties().get("condensedFrom")).isEqualTo("5");
        assertThat(node.properties().get("content")).contains("Content 1");
        assertThat(node.properties().get("content")).contains("Content 5");
        assertThat(node.properties().get("content")).contains("---");
        assertThat(node.properties().get("rooms")).contains("Library");
        assertThat(node.properties().get("rooms")).contains("Kitchen");
        assertThat(node.properties().get("timeRangeStart")).isNotNull();
        assertThat(node.properties().get("timeRangeEnd")).isNotNull();
    }

    @Test
    void run_discoversObservationSubgraphsByPrefix() {
        store.createSubgraph(new SubgraphInput("observations-penelope", SubgraphTypes.ACTIVITY, null), TENANT);
        store.createSubgraph(new SubgraphInput("narrator-summaries", SubgraphTypes.ACTIVITY, null), TENANT);
        store.createSubgraph(new SubgraphInput("beliefs-penelope", "cognitive", null), TENANT);

        var phase = new ObservationCompactionPhase(store, 10, 5);
        phase.run(TENANT, List.of());
    }

    @Test
    void compactSubgraph_preservesRecentWindow() {
        var sgId = store.createSubgraph(new SubgraphInput("observations-penelope", SubgraphTypes.ACTIVITY, null), TENANT);
        for (int i = 1; i <= 20; i++) {
            addObservation(sgId, "penelope", i, "Room A", "Event " + i);
        }

        var phase = new ObservationCompactionPhase(store, 10, 5);
        phase.compactSubgraph(sgId, "penelope", TENANT);

        var originals = store.nodesIn(sgId, TENANT).stream()
                .filter(n -> !n.traits().contains("Condensed"))
                .sorted(Comparator.comparing(MindMapNode::validFrom))
                .toList();

        assertThat(originals).hasSize(10);
        for (int i = 0; i < originals.size(); i++) {
            assertThat(originals.get(i).properties().get("content")).isEqualTo("Event " + (11 + i));
        }
    }

    @Test
    void compactSubgraph_idempotent_doesNotRecompactCondensedNodes() {
        var sgId = store.createSubgraph(new SubgraphInput("observations-penelope", SubgraphTypes.ACTIVITY, null), TENANT);
        for (int i = 1; i <= 15; i++) {
            addObservation(sgId, "penelope", i, "Room A", "Event " + i);
        }

        var phase = new ObservationCompactionPhase(store, 5, 5);
        phase.compactSubgraph(sgId, "penelope", TENANT);
        int secondPass = phase.compactSubgraph(sgId, "penelope", TENANT);

        assertThat(secondPass).isZero();
    }

    private void addObservation(String subgraphId, String characterId, int tick, String room, String content) {
        var now = Instant.ofEpochMilli(1000L * tick);
        var props = new HashMap<String, String>();
        props.put("content", content);
        props.put("tier", "verbatim");
        props.put("eventCount", "3");
        props.put("timeSinceLastDrain", "5000");
        props.put("room", room);

        var input = NodeInput.of("observation-" + now.toEpochMilli(), subgraphId)
                .withProperties(props)
                .withProvenance("tiered-observation-renderer")
                .withPrincipalId(PrincipalId.agent(characterId))
                .withValidFrom(now);

        store.addNode(input, TENANT);
    }
}
