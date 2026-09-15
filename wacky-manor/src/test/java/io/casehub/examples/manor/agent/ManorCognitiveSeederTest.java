package io.casehub.examples.manor.agent;

import io.casehub.neocortex.mindmap.EdgeInput;
import io.casehub.neocortex.mindmap.MergeResult;
import io.casehub.neocortex.mindmap.MindMapCapability;
import io.casehub.neocortex.mindmap.MindMapEdge;
import io.casehub.neocortex.mindmap.MindMapNode;
import io.casehub.neocortex.mindmap.MindMapQuery;
import io.casehub.neocortex.mindmap.MindMapStore;
import io.casehub.neocortex.mindmap.MindMapSubgraph;
import io.casehub.neocortex.mindmap.MindMapVocabulary;
import io.casehub.neocortex.mindmap.NodeInput;
import io.casehub.neocortex.mindmap.NodeUpdate;
import io.casehub.neocortex.mindmap.SubgraphInput;
import io.casehub.neocortex.mindmap.SupersessionStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ManorCognitiveSeederTest {

    private static class StubMindMapStore implements MindMapStore {
        final List<NodeInput>     addedNodes       = new ArrayList<>();
        final List<SubgraphInput> createdSubgraphs = new ArrayList<>();
        int nodeCounter = 0;

        @Override
        public void registerVocabulary(MindMapVocabulary v) {}

        @Override
        public String addNode(NodeInput input, String tenantId) {
            addedNodes.add(input);
            return "node-" + (nodeCounter++);
        }

        @Override
        public MindMapNode getNode(String nodeId, String tenantId)                      {return null;}

        @Override
        public void updateNode(String nodeId, NodeUpdate update, String tenantId)       {}

        @Override
        public String addEdge(EdgeInput input, String tenantId)                         {return null;}

        @Override
        public MindMapEdge getEdge(String edgeId, String tenantId)                      {return null;}

        @Override
        public void removeEdge(String edgeId, String tenantId)                          {}

        @Override
        public void addAlias(String nodeId, String alias, String tenantId)              {}

        @Override
        public void removeAlias(String nodeId, String alias, String tenantId)           {}

        @Override
        public MindMapNode resolveNode(String name, String subgraphId, String tenantId) {return null;}

        @Override
        public MergeResult mergeNodes(String keepId, String removeId, String tenantId)  {return null;}

        @Override
        public String createSubgraph(SubgraphInput input, String tenantId) {
            createdSubgraphs.add(input);
            return "sg-" + input.name();
        }

        @Override
        public MindMapSubgraph getSubgraph(String subgraphId, String tenantId)                                                              {return null;}

        @Override
        public void updateSubgraph(String subgraphId, String rootNodeId, String tenantId)                                                   {}

        @Override
        public List<MindMapSubgraph> listSubgraphs(String tenantId)                                                                         {return List.of();}

        @Override
        public List<MindMapNode> nodesIn(String subgraphId, String tenantId)                                                                {return List.of();}

        @Override
        public List<MindMapEdge> bridgeEdges(String subgraphId, String tenantId, io.casehub.platform.api.identity.PrincipalId p)            {return List.of();}

        @Override
        public List<MindMapEdge> neighbors(String nodeId, String tenantId, io.casehub.platform.api.identity.PrincipalId p)                  {return List.of();}

        @Override
        public List<MindMapEdge> neighbors(String nodeId, String edgeType, String tenantId, io.casehub.platform.api.identity.PrincipalId p) {return List.of();}

        @Override
        public List<MindMapNode> search(MindMapQuery query)                                                                                 {return List.of();}

        @Override
        public void supersede(String targetId, String supersedingId, String reason, String tenantId)                                        {}

        @Override
        public void reinstate(String targetId, String tenantId)                                                                             {}

        @Override
        public SupersessionStatus getSupersessionStatus(String targetId, String tenantId)                                                   {return null;}

        @Override
        public int eraseNode(String nodeId, String tenantId)                                                                                {return 0;}

        @Override
        public int eraseSubgraph(String subgraphId, String tenantId)                                                                        {return 0;}

        @Override
        public int eraseEntity(String entityName, String tenantId)                                                                          {return 0;}

        @Override
        public int eraseEntityAcrossTenants(String entityName, java.util.Set<String> tenantIds)                                             {return 0;}

        @Override
        public java.util.Set<MindMapCapability> capabilities()                                                                              {return java.util.Set.of();}
    }

    @Test
    void seedResultTracksNodeTimestamps() {
        var result = new ManorCognitiveSeeder.SeedResult("sub-1",
                                                         Map.of("penelope-awareness", Instant.now()));
        assertThat(result.subgraphId()).isEqualTo("sub-1");
        assertThat(result.seededNodeTimestamps()).containsKey("penelope-awareness");
    }

    @Test
    void emptyConfigProducesEmptyTimestamps() {
        var result = new ManorCognitiveSeeder.SeedResult("sub-1", Map.of());
        assertThat(result.seededNodeTimestamps()).isEmpty();
    }

    @Test
    void seedResultSubgraphIdFollowsNamingConvention() {
        var result = new ManorCognitiveSeeder.SeedResult(
                ManorCognitiveSeeder.subgraphName("hooded-claw"), Map.of());
        assertThat(result.subgraphId()).isEqualTo("beliefs-hooded-claw");
    }

    @Test
    void seedPeople_createsSharedSubgraph() {
        var store  = new StubMindMapStore();
        var seeder = new ManorCognitiveSeeder(store);
        var configs = Map.of(
                "agent-a", new SocialConfig(List.of(), List.of(), List.of(),
                                            List.of(new SocialConfig.Relationship("agent-b", 0.5, 0.3, 0.2))),
                "agent-b", new SocialConfig(List.of(), List.of(), List.of(), List.of())
                            );
        var result = seeder.seedPeople(configs, "t1");

        assertThat(result.subgraphId()).isEqualTo("sg-people");
        assertThat(store.createdSubgraphs).hasSize(1);
        assertThat(store.createdSubgraphs.get(0).name()).isEqualTo("people");
    }

    @Test
    void seedPeople_createsSharedPersonNodePerCharacter() {
        var store  = new StubMindMapStore();
        var seeder = new ManorCognitiveSeeder(store);
        var configs = Map.of(
                "agent-a", new SocialConfig(List.of(), List.of(), List.of(),
                                            List.of(new SocialConfig.Relationship("agent-b", 0.5, 0.3, 0.2))),
                "agent-b", new SocialConfig(List.of(), List.of(), List.of(),
                                            List.of(new SocialConfig.Relationship("agent-a", -0.1, 0.4, 0.6)))
                            );
        seeder.seedPeople(configs, "t1");

        var entityNodes = store.addedNodes.stream()
                                          .filter(n -> n.traits().contains("Entitylike")).toList();
        assertThat(entityNodes).hasSize(2);
    }

    @Test
    void seedPeople_overlayHasCorrectTraitsAndPad() {
        var store  = new StubMindMapStore();
        var seeder = new ManorCognitiveSeeder(store);
        var configs = Map.of(
                "observer", new SocialConfig(List.of(), List.of(), List.of(),
                                             List.of(new SocialConfig.Relationship("target", 0.6, 0.3, 0.5))),
                "target", new SocialConfig(List.of(), List.of(), List.of(), List.of())
                            );
        seeder.seedPeople(configs, "t1");

        var overlayNodes = store.addedNodes.stream()
                                           .filter(n -> n.traits().contains("overlay")).toList();
        assertThat(overlayNodes).hasSize(1);

        var overlay = overlayNodes.get(0);
        assertThat(overlay.pleasure()).isEqualTo(0.6);
        assertThat(overlay.arousal()).isEqualTo(0.3);
        assertThat(overlay.dominance()).isEqualTo(0.5);
        assertThat(overlay.properties()).containsEntry("agentId", "observer");
    }

    @Test
    void seedPeople_overlayLinksToSharedNode() {
        var store  = new StubMindMapStore();
        var seeder = new ManorCognitiveSeeder(store);
        var configs = Map.of(
                "observer", new SocialConfig(List.of(), List.of(), List.of(),
                                             List.of(new SocialConfig.Relationship("target", 0.0, 0.0, 0.0))),
                "target", new SocialConfig(List.of(), List.of(), List.of(), List.of())
                            );
        seeder.seedPeople(configs, "t1");

        var overlayNodes = store.addedNodes.stream()
                                           .filter(n -> n.traits().contains("overlay")).toList();
        assertThat(overlayNodes).hasSize(1);
        assertThat(overlayNodes.get(0).refs()).anyMatch(r -> "overlay".equals(r.scheme()));
    }

    @Test
    void seedPeople_sharedNodeHasEntitylikeTrait() {
        var store  = new StubMindMapStore();
        var seeder = new ManorCognitiveSeeder(store);
        var configs = Map.of(
                "a", new SocialConfig(List.of(), List.of(), List.of(),
                                      List.of(new SocialConfig.Relationship("b", 0.0, 0.0, 0.0))),
                "b", new SocialConfig(List.of(), List.of(), List.of(), List.of())
                            );
        seeder.seedPeople(configs, "t1");

        var entityNodes = store.addedNodes.stream()
                                          .filter(n -> n.traits().contains("Entitylike")).toList();
        assertThat(entityNodes).isNotEmpty();
        assertThat(entityNodes.stream().anyMatch(n -> n.properties().containsValue("a"))).isTrue();
    }

    @Test
    void seedPeople_emptyConfigs_noNodes() {
        var store  = new StubMindMapStore();
        var seeder = new ManorCognitiveSeeder(store);
        var result = seeder.seedPeople(Map.of(), "t1");
        assertThat(result.overlayCount()).isZero();
        assertThat(store.addedNodes).isEmpty();
    }

    @Test
    void seedPeople_countsOverlaysCorrectly() {
        var store  = new StubMindMapStore();
        var seeder = new ManorCognitiveSeeder(store);
        var configs = Map.of(
                "a", new SocialConfig(List.of(), List.of(), List.of(),
                                      List.of(new SocialConfig.Relationship("b", 0.1, 0.2, 0.3),
                                              new SocialConfig.Relationship("c", 0.4, 0.5, 0.6))),
                "b", new SocialConfig(List.of(), List.of(), List.of(),
                                      List.of(new SocialConfig.Relationship("a", -0.1, 0.0, 0.0))),
                "c", new SocialConfig(List.of(), List.of(), List.of(), List.of())
                            );
        var result = seeder.seedPeople(configs, "t1");
        assertThat(result.overlayCount()).isEqualTo(3);
        assertThat(result.seededAgentIds()).containsExactlyInAnyOrder("a", "b", "c");
    }
}
