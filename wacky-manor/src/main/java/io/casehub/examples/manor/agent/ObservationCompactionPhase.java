package io.casehub.examples.manor.agent;

import io.casehub.neocortex.cognitive.observability.CompactionObserver;
import io.casehub.neocortex.mindmap.MindMapNode;
import io.casehub.neocortex.mindmap.MindMapStore;
import io.casehub.neocortex.mindmap.NodeInput;
import io.casehub.neocortex.mindmap.SubgraphTypes;
import io.casehub.neocortex.mindmap.intelligence.consolidation.ConsolidationPhase;
import io.casehub.platform.api.identity.PrincipalId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

public final class ObservationCompactionPhase implements ConsolidationPhase {

    private static final Logger LOG = Logger.getLogger(ObservationCompactionPhase.class.getName());
    private static final String PROVENANCE = "observation-compaction";
    private static final String CONDENSED_TRAIT = "Condensed";

    private final MindMapStore store;
    private final int recentWindowSize;
    private final int batchSize;
    private final List<CompactionObserver> compactionObservers;


    public ObservationCompactionPhase(MindMapStore store, int recentWindowSize, int batchSize) {
        this(store, recentWindowSize, batchSize, List.of());
    }

    public ObservationCompactionPhase(MindMapStore store, int recentWindowSize, int batchSize,
                                      List<CompactionObserver> compactionObservers) {
        this.store               = store;
        this.recentWindowSize    = recentWindowSize;
        this.batchSize           = batchSize;
        this.compactionObservers = compactionObservers.isEmpty() ? List.of() : List.copyOf(compactionObservers);
    }


    @Override
    public String name() {
        return "observation-compaction";
    }

    @Override
    public void run(String tenantId, List<String> subgraphPriority) {
        var subgraphs = store.listSubgraphs(tenantId);
        int totalCompacted = 0;

        for (var sg : subgraphs) {
            if (!SubgraphTypes.ACTIVITY.equals(sg.type())) continue;
            if (!sg.name().startsWith("observations-")) continue;

            String characterId = sg.name().substring("observations-".length());
            int compacted = compactSubgraph(sg.id(), characterId, tenantId);
            if (compacted > 0) {
                totalCompacted += compacted;
                LOG.info(characterId + ": compacted " + compacted + " observation nodes");
            }
        }

        if (totalCompacted > 0) {
            LOG.info("Observation compaction complete: " + totalCompacted + " nodes condensed");
        }
    }

    int compactSubgraph(String subgraphId, String characterId, String tenantId) {
        var allNodes = store.nodesIn(subgraphId, tenantId);

        var originals = allNodes.stream()
                                .filter(n -> !n.traits().contains(CONDENSED_TRAIT))
                                .sorted(Comparator.comparing(n -> n.validFrom() != null ? n.validFrom() : n.createdAt()))
                                .toList();

        if (originals.size() <= recentWindowSize) {return 0;}

        int compactableCount = originals.size() - recentWindowSize;
        var toCompact        = originals.subList(0, compactableCount);

        long startNanos     = System.nanoTime();
        int  nodesCompacted = 0;
        for (int i = 0; i < toCompact.size(); i += batchSize) {
            int end   = Math.min(i + batchSize, toCompact.size());
            var batch = toCompact.subList(i, end);
            condenseBatch(batch, subgraphId, characterId, tenantId);
            nodesCompacted += batch.size();
        }
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

        int outputNodes = (nodesCompacted + batchSize - 1) / batchSize;
        for (var observer : compactionObservers) {
            observer.onCompaction(characterId, nodesCompacted, outputNodes, elapsedMs);
        }

        return nodesCompacted;
    }

    private void condenseBatch(List<MindMapNode> batch, String subgraphId,
                               String characterId, String tenantId) {
        var contentParts = new ArrayList<String>();
        var rooms = new java.util.LinkedHashSet<String>();
        int totalEvents = 0;
        Instant earliest = null;
        Instant latest = null;

        for (var node : batch) {
            String content = node.properties().getOrDefault("content", "");
            if (!content.isBlank()) {
                contentParts.add(content);
            }
            String room = node.properties().getOrDefault("room", "");
            if (!room.isBlank()) {
                rooms.add(room);
            }
            int events = parseIntOrZero(node.properties().getOrDefault("eventCount", "0"));
            totalEvents += events;

            Instant ts = node.validFrom() != null ? node.validFrom() : node.createdAt();
            if (ts != null) {
                if (earliest == null || ts.isBefore(earliest)) earliest = ts;
                if (latest == null || ts.isAfter(latest)) latest = ts;
            }
        }

        var condensedContent = String.join("\n---\n", contentParts);

        var props = new HashMap<String, String>();
        props.put("content", condensedContent);
        props.put("condensedFrom", String.valueOf(batch.size()));
        props.put("totalEventCount", String.valueOf(totalEvents));
        props.put("rooms", String.join(",", rooms));
        if (earliest != null) props.put("timeRangeStart", earliest.toString());
        if (latest != null) props.put("timeRangeEnd", latest.toString());

        var nodeInput = NodeInput.of("condensed-" + (earliest != null ? earliest.toEpochMilli() : System.currentTimeMillis()), subgraphId)
                .withProperties(props)
                .withProvenance(PROVENANCE)
                .withPrincipalId(PrincipalId.agent(characterId))
                .withTraits(java.util.Set.of(CONDENSED_TRAIT))
                .withValidFrom(earliest)
                .withValidUntil(latest);

        store.addNode(nodeInput, tenantId);

        for (var node : batch) {
            store.eraseNode(node.id(), tenantId);
        }
    }

    private static int parseIntOrZero(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
