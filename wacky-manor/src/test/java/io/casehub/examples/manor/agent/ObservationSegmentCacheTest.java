package io.casehub.examples.manor.agent;

import io.casehub.neocortex.mindmap.MindMapStore;
import io.casehub.neocortex.mindmap.NodeInput;
import io.casehub.neocortex.mindmap.SubgraphInput;
import io.casehub.neocortex.mindmap.SubgraphTypes;
import io.casehub.neocortex.mindmap.inmem.InMemoryMindMapStore;
import io.casehub.platform.api.identity.PrincipalId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ObservationSegmentCacheTest {

    private static final String TENANT = "test-tenant";
    private MindMapStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryMindMapStore();
    }

    @Test
    void getHistory_emptyWhenNoSubgraph() {
        var cache = new ObservationSegmentCache(store, TENANT, 2000, 5);
        assertThat(cache.getHistory("penelope")).isEmpty();
    }

    @Test
    void getHistory_emptyBeforeRefresh() {
        var sgId = createObservationSubgraph("penelope");
        addObservation(sgId, "penelope", 1, "Library", "Saw a bookcase.");
        var cache = new ObservationSegmentCache(store, TENANT, 2000, 5);

        assertThat(cache.getHistory("penelope")).isEmpty();
    }

    @Test
    void getHistory_returnsContentAfterRefresh() {
        var sgId = createObservationSubgraph("penelope");
        addObservation(sgId, "penelope", 1, "Library", "Saw a bookcase.");
        addObservation(sgId, "penelope", 2, "Library", "Dick entered the room.");

        var cache = new ObservationSegmentCache(store, TENANT, 2000, 5);
        cache.refresh("penelope");

        var history = cache.getHistory("penelope");
        assertThat(history).contains("Saw a bookcase.");
        assertThat(history).contains("Dick entered the room.");
        assertThat(history).contains("[Library]");
    }

    @Test
    void getHistory_includesCondensedNodesAsEarlierSection() {
        var sgId = createObservationSubgraph("muttley");
        addCondensedNode(sgId, "muttley", 1, "Kitchen,Library", "Summary of events 1-5");
        addObservation(sgId, "muttley", 10, "Ballroom", "Current event.");

        var cache = new ObservationSegmentCache(store, TENANT, 2000, 5);
        cache.refresh("muttley");

        var history = cache.getHistory("muttley");
        assertThat(history).contains("Earlier:");
        assertThat(history).contains("Summary of events 1-5");
        assertThat(history).contains("Recently:");
        assertThat(history).contains("Current event.");
    }

    @Test
    void getHistory_respectsRecentNodeLimit() {
        var sgId = createObservationSubgraph("penelope");
        for (int i = 1; i <= 10; i++) {
            addObservation(sgId, "penelope", i, "Room", "Event " + i);
        }

        var cache = new ObservationSegmentCache(store, TENANT, 2000, 3);
        cache.refresh("penelope");

        var history = cache.getHistory("penelope");
        assertThat(history).contains("Event 8");
        assertThat(history).contains("Event 9");
        assertThat(history).contains("Event 10");
        assertThat(history).doesNotContain("Event 1\n");
    }

    @Test
    void refreshAll_updatesMultipleCharacters() {
        var sg1 = createObservationSubgraph("penelope");
        var sg2 = createObservationSubgraph("muttley");
        addObservation(sg1, "penelope", 1, "Library", "Penelope saw a door.");
        addObservation(sg2, "muttley", 1, "Kitchen", "Muttley sniffed around.");

        var cache = new ObservationSegmentCache(store, TENANT, 2000, 5);
        cache.refreshAll(List.of("penelope", "muttley"));

        assertThat(cache.getHistory("penelope")).contains("Penelope saw a door.");
        assertThat(cache.getHistory("muttley")).contains("Muttley sniffed around.");
    }

    private String createObservationSubgraph(String characterId) {
        return store.createSubgraph(
                new SubgraphInput("observations-" + characterId, SubgraphTypes.ACTIVITY, null), TENANT);
    }

    private void addObservation(String subgraphId, String characterId, int tick, String room, String content) {
        var now = Instant.ofEpochMilli(1000L * tick);
        var props = new HashMap<String, String>();
        props.put("content", content);
        props.put("tier", "verbatim");
        props.put("eventCount", "1");
        props.put("room", room);

        store.addNode(NodeInput.of("observation-" + now.toEpochMilli(), subgraphId)
                .withProperties(props)
                .withProvenance("tiered-observation-renderer")
                .withPrincipalId(PrincipalId.agent(characterId))
                .withValidFrom(now), TENANT);
    }

    private void addCondensedNode(String subgraphId, String characterId, int tick, String rooms, String content) {
        var now = Instant.ofEpochMilli(1000L * tick);
        var props = new HashMap<String, String>();
        props.put("content", content);
        props.put("condensedFrom", "5");
        props.put("rooms", rooms);
        props.put("timeRangeStart", now.toString());
        props.put("timeRangeEnd", Instant.ofEpochMilli(5000L * tick).toString());

        store.addNode(NodeInput.of("condensed-" + now.toEpochMilli(), subgraphId)
                .withProperties(props)
                .withProvenance("observation-compaction")
                .withPrincipalId(PrincipalId.agent(characterId))
                .withTraits(Set.of("Condensed"))
                .withValidFrom(now), TENANT);
    }
}
