package io.casehub.examples.manor.agent;

import io.casehub.neocortex.cognitive.Confidence;
import io.casehub.neocortex.mindmap.MindMapStore;
import io.casehub.neocortex.mindmap.NodeInput;
import io.casehub.neocortex.mindmap.OverlayRef;
import io.casehub.neocortex.mindmap.SubgraphInput;
import io.casehub.platform.api.identity.PrincipalId;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ManorCognitiveSeeder {

    public record SeedResult(String subgraphId, Map<String, Instant> seededNodeTimestamps) {}

    public record PeopleSeedResult(String subgraphId, Set<String> seededAgentIds, int overlayCount) {}

    public static final String PEOPLE_SUBGRAPH = "people";

    public PeopleSeedResult seedPeople(Map<String, SocialConfig> allConfigs, String tenantId) {
        Set<String> allAgentIds = allConfigs.keySet();
        if (allAgentIds.isEmpty()) {
            return new PeopleSeedResult(PEOPLE_SUBGRAPH, Set.of(), 0);
        }

        var subgraphId = mindMapStore.createSubgraph(
                new SubgraphInput(PEOPLE_SUBGRAPH, "social", null), tenantId);
        if (subgraphId == null || subgraphId.isBlank()) {
            return new PeopleSeedResult(PEOPLE_SUBGRAPH, Set.of(), 0);
        }

        var                 now           = Instant.now();
        Map<String, String> personNodeIds = new HashMap<>();

        for (String agentId : allAgentIds) {
            String nodeId = mindMapStore.addNode(
                    NodeInput.of(agentId, subgraphId)
                             .withConfidence(Confidence.stated(0.9, now))
                             .withProvenance("manor-seed")
                             .withTraits(Set.of("Entitylike"))
                             .withProperties(Map.of("agentId", agentId))
                             .withPad(0.0, 0.0, 0.0),
                    tenantId);
            personNodeIds.put(agentId, nodeId);
        }

        int overlayCount = 0;
        for (var entry : allConfigs.entrySet()) {
            String observerId = entry.getKey();
            for (var rel : entry.getValue().relationships()) {
                String targetNodeId = personNodeIds.get(rel.targetAgentId());
                if (targetNodeId == null) {continue;}

                mindMapStore.addNode(
                        NodeInput.of(observerId + "-sees-" + rel.targetAgentId(), subgraphId)
                                 .withConfidence(Confidence.stated(0.7, now))
                                 .withProvenance("manor-seed")
                                 .withTraits(Set.of("overlay"))
                                 .withRefs(Set.of(OverlayRef.of(targetNodeId)))
                                 .withProperties(Map.of(OverlayRef.AGENT_ID, observerId))
                                 .withPad(rel.pleasure(), rel.arousal(), rel.dominance())
                                 .withPrincipalId(PrincipalId.agent(observerId)),
                        tenantId);
                overlayCount++;
            }
        }

        return new PeopleSeedResult(subgraphId, Set.copyOf(allAgentIds), overlayCount);
    }


    private final MindMapStore mindMapStore;

    public ManorCognitiveSeeder(MindMapStore mindMapStore) {
        this.mindMapStore = mindMapStore;
    }

    public SeedResult seed(String agentId, SocialConfig config, String tenantId) {
        var subgraphName = subgraphName(agentId);
        if (config.initialBeliefs().isEmpty()) {
            return new SeedResult(subgraphName, Map.of());
        }

        var subgraphId = mindMapStore.createSubgraph(
                new SubgraphInput(subgraphName, "cognitive", null), tenantId);
        if (subgraphId == null || subgraphId.isBlank()) {
            return new SeedResult(subgraphName, Map.of());
        }

        var timestamps = new HashMap<String, Instant>();
        var now = Instant.now();

        for (var belief : config.initialBeliefs()) {
            mindMapStore.addNode(
                    NodeInput.of(belief.value(), subgraphId)
                            .withConfidence(Confidence.stated(0.8, now))
                            .withProvenance("manor-seed")
                            .withTraits(Set.of("Belieflike"))
                            .withProperties(Map.of("subject", belief.key()))
                            .withPrincipalId(PrincipalId.agent(agentId)),
                    tenantId);
            timestamps.put(belief.key(), now);
        }

        return new SeedResult(subgraphId, Map.copyOf(timestamps));
    }

    public static String subgraphName(String agentId) {
        return "beliefs-" + agentId;
    }
}
