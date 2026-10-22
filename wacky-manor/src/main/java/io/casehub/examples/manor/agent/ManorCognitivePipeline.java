package io.casehub.examples.manor.agent;

import io.casehub.neocortex.cognition.core.CognitionCore;
import io.casehub.neocortex.cognitive.index.CognitiveDefaultsRegistry;
import io.casehub.neocortex.mindmap.MindMapStore;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ManorCognitivePipeline {

    private final CognitionCore cognitionCore;
    private final @Nullable MindMapStore mindMapStore;
    private final Map<String, CharacterCognition> cognitions;
    private final CognitiveDefaultsRegistry defaultsRegistry;
    private final String tenantId;

    ManorCognitivePipeline(CognitionCore cognitionCore,
                           @Nullable MindMapStore mindMapStore,
                           Map<String, CharacterCognition> cognitions,
                           CognitiveDefaultsRegistry defaultsRegistry,
                           String tenantId) {
        this.cognitionCore = cognitionCore;
        this.mindMapStore = mindMapStore;
        this.cognitions = Map.copyOf(cognitions);
        this.defaultsRegistry = defaultsRegistry;
        this.tenantId = tenantId;
    }

    public CognitionCore cognitionCore() { return cognitionCore; }
    public Map<String, CharacterCognition> cognitions() { return cognitions; }
    public @Nullable CharacterCognition characterCognition(String agentId) { return cognitions.get(agentId); }
    public CognitiveDefaultsRegistry defaultsRegistry() { return defaultsRegistry; }
    public @Nullable MindMapStore mindMapStore() { return mindMapStore; }
    public String tenantId() { return tenantId; }
    public Set<String> agentIds() { return cognitions.keySet(); }

    public boolean hasDriveNodes(String agentId) {
        if (mindMapStore == null) return false;
        var subgraphName = ManorCognitiveSeeder.subgraphName(agentId);
        return mindMapStore.listSubgraphs(tenantId).stream()
                .filter(s -> subgraphName.equals(s.name()))
                .findFirst()
                .map(s -> mindMapStore.nodesIn(s.id(), tenantId).stream()
                        .anyMatch(n -> "drive-intensity".equals(
                                n.properties().get("cognitiveKind"))))
                .orElse(false);
    }

    public boolean hasBehavioralNodes(String agentId) {
        if (mindMapStore == null) return false;
        return mindMapStore.listSubgraphs(tenantId).stream()
                .filter(s -> s.name() != null && s.name().contains("Behavioral"))
                .findFirst()
                .map(s -> !mindMapStore.nodesIn(s.id(), tenantId).isEmpty())
                .orElse(false);
    }

    public boolean hasSocialSubgraph() {
        if (mindMapStore == null) return false;
        return mindMapStore.listSubgraphs(tenantId).stream()
                .anyMatch(s -> "people".equals(s.name()));
    }

    public boolean hasCognitiveSubgraph(String agentId) {
        if (mindMapStore == null) return false;
        var subgraphName = ManorCognitiveSeeder.subgraphName(agentId);
        return mindMapStore.listSubgraphs(tenantId).stream()
                .anyMatch(s -> subgraphName.equals(s.name()));
    }

    public int driveNodeCount(String agentId) {
        if (mindMapStore == null) return 0;
        var subgraphName = ManorCognitiveSeeder.subgraphName(agentId);
        return mindMapStore.listSubgraphs(tenantId).stream()
                .filter(s -> subgraphName.equals(s.name()))
                .findFirst()
                .map(s -> (int) mindMapStore.nodesIn(s.id(), tenantId).stream()
                        .filter(n -> "drive-intensity".equals(
                                n.properties().get("cognitiveKind")))
                        .count())
                .orElse(0);
    }

    public List<String> seededDriveNames(String agentId) {
        if (mindMapStore == null) return List.of();
        var subgraphName = ManorCognitiveSeeder.subgraphName(agentId);
        return mindMapStore.listSubgraphs(tenantId).stream()
                .filter(s -> subgraphName.equals(s.name()))
                .findFirst()
                .map(s -> mindMapStore.nodesIn(s.id(), tenantId).stream()
                        .filter(n -> "drive-intensity".equals(
                                n.properties().get("cognitiveKind")))
                        .map(n -> n.name())
                        .sorted()
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }
}
