package io.casehub.examples.manor.agent;

import io.casehub.neocortex.cognitive.Confidence;
import io.casehub.neocortex.mindmap.MindMapStore;
import io.casehub.neocortex.mindmap.SubgraphInput;
import io.casehub.neocortex.mindmap.NodeInput;
import io.casehub.platform.api.identity.PrincipalId;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ManorCognitiveSeeder {

    public record SeedResult(String subgraphId, Map<String, Instant> seededNodeTimestamps) {}

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
