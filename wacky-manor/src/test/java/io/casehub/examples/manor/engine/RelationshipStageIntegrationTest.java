package io.casehub.examples.manor.engine;

import io.casehub.blocks.agentic.social.OverlayFamiliarityPropertyModel;
import io.casehub.blocks.agentic.social.RelationshipStageConfig;
import io.casehub.blocks.agentic.social.RelationshipStagePhase;
import io.casehub.examples.manor.agent.ManorCognitiveSeeder;
import io.casehub.examples.manor.agent.ManorSocialConfigLoader;
import io.casehub.neocortex.memory.CaseMemoryStore;
import io.casehub.neocortex.memory.EraseRequest;
import io.casehub.neocortex.memory.Memory;
import io.casehub.neocortex.memory.MemoryDomain;
import io.casehub.neocortex.memory.MemoryInput;
import io.casehub.neocortex.memory.MemoryQuery;
import io.casehub.neocortex.memory.Subject;
import io.casehub.neocortex.mindmap.OverlayRef;
import io.casehub.neocortex.mindmap.inmem.InMemoryMindMapStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RelationshipStageIntegrationTest {

    private static final String TENANT = "stage-test";
    private static final String AGENT = "hooded-claw";
    private static final String TARGET = "penelope-pitstop";

    @Test
    void fullLifecycle_seedInteractConsolidate() {
        var mindMapStore = new InMemoryMindMapStore();
        var seeder = new ManorCognitiveSeeder(mindMapStore);
        var allConfigs = ManorSocialConfigLoader.load();

        seeder.seedPeople(allConfigs, TENANT);

        var memories = buildInteractionMemories(20, 0.5);
        var memoryStore = new StubCaseMemoryStore(memories);

        var phase = new RelationshipStagePhase(
            mindMapStore, memoryStore,
            agentId -> RelationshipStageConfig.defaults(),
            ManorCognitiveSeeder.PEOPLE_SUBGRAPH,
            60_000L);

        phase.run(TENANT, List.of());

        var subgraphs = mindMapStore.listSubgraphs(TENANT);
        var peopleSg = subgraphs.stream()
            .filter(sg -> ManorCognitiveSeeder.PEOPLE_SUBGRAPH.equals(sg.name()))
            .findFirst().orElseThrow();
        var nodes = mindMapStore.nodesIn(peopleSg.id(), TENANT);

        var overlay = nodes.stream()
            .filter(n -> n.traits().contains("overlay"))
            .filter(n -> AGENT.equals(n.property(OverlayRef.AGENT_ID).orElse(null)))
            .findFirst().orElseThrow();

        assertThat(overlay.property(OverlayFamiliarityPropertyModel.FAMILIARITY_SCORE))
            .isPresent();
        assertThat(overlay.property(OverlayFamiliarityPropertyModel.FAMILIARITY_STAGE))
            .isPresent()
            .hasValueSatisfying(stage -> assertThat(stage).isNotEqualTo("stranger"));
        assertThat(overlay.property(OverlayFamiliarityPropertyModel.FAMILIARITY_INTERACTION_COUNT))
            .isPresent()
            .hasValue("20");
    }

    @Test
    void noMemories_stageRemainsStranger() {
        var mindMapStore = new InMemoryMindMapStore();
        var seeder = new ManorCognitiveSeeder(mindMapStore);
        var allConfigs = ManorSocialConfigLoader.load();

        seeder.seedPeople(allConfigs, TENANT);

        var memoryStore = new StubCaseMemoryStore(List.of());

        var phase = new RelationshipStagePhase(
            mindMapStore, memoryStore,
            agentId -> RelationshipStageConfig.defaults(),
            ManorCognitiveSeeder.PEOPLE_SUBGRAPH,
            60_000L);

        phase.run(TENANT, List.of());

        var subgraphs = mindMapStore.listSubgraphs(TENANT);
        var peopleSg = subgraphs.stream()
            .filter(sg -> ManorCognitiveSeeder.PEOPLE_SUBGRAPH.equals(sg.name()))
            .findFirst().orElseThrow();
        var nodes = mindMapStore.nodesIn(peopleSg.id(), TENANT);

        var overlay = nodes.stream()
            .filter(n -> n.traits().contains("overlay"))
            .filter(n -> AGENT.equals(n.property(OverlayRef.AGENT_ID).orElse(null)))
            .findFirst().orElseThrow();

        assertThat(overlay.property(OverlayFamiliarityPropertyModel.FAMILIARITY_STAGE))
            .hasValue("stranger");
    }

    @Test
    void negativeInteractions_lowerFamiliarity() {
        var mindMapStore = new InMemoryMindMapStore();
        var seeder = new ManorCognitiveSeeder(mindMapStore);
        var allConfigs = ManorSocialConfigLoader.load();

        seeder.seedPeople(allConfigs, TENANT);

        var positiveMemories = buildInteractionMemories(20, 0.5);
        var negativeMemories = buildInteractionMemories(20, -0.5);
        var positiveStore = new StubCaseMemoryStore(positiveMemories);
        var negativeStore = new StubCaseMemoryStore(negativeMemories);

        var positivePhase = new RelationshipStagePhase(
            mindMapStore, positiveStore,
            agentId -> RelationshipStageConfig.defaults(),
            ManorCognitiveSeeder.PEOPLE_SUBGRAPH, 60_000L);
        positivePhase.run(TENANT, List.of());

        var subgraphs = mindMapStore.listSubgraphs(TENANT);
        var peopleSg = subgraphs.stream()
            .filter(sg -> ManorCognitiveSeeder.PEOPLE_SUBGRAPH.equals(sg.name()))
            .findFirst().orElseThrow();
        var overlay = mindMapStore.nodesIn(peopleSg.id(), TENANT).stream()
            .filter(n -> n.traits().contains("overlay"))
            .filter(n -> AGENT.equals(n.property(OverlayRef.AGENT_ID).orElse(null)))
            .findFirst().orElseThrow();
        double positiveScore = Double.parseDouble(
            overlay.property(OverlayFamiliarityPropertyModel.FAMILIARITY_SCORE).orElseThrow());

        var mindMapStore2 = new InMemoryMindMapStore();
        seeder = new ManorCognitiveSeeder(mindMapStore2);
        seeder.seedPeople(allConfigs, TENANT);

        var negativePhase = new RelationshipStagePhase(
            mindMapStore2, negativeStore,
            agentId -> RelationshipStageConfig.defaults(),
            ManorCognitiveSeeder.PEOPLE_SUBGRAPH, 60_000L);
        negativePhase.run(TENANT, List.of());

        var peopleSg2 = mindMapStore2.listSubgraphs(TENANT).stream()
            .filter(sg -> ManorCognitiveSeeder.PEOPLE_SUBGRAPH.equals(sg.name()))
            .findFirst().orElseThrow();
        var overlay2 = mindMapStore2.nodesIn(peopleSg2.id(), TENANT).stream()
            .filter(n -> n.traits().contains("overlay"))
            .filter(n -> AGENT.equals(n.property(OverlayRef.AGENT_ID).orElse(null)))
            .findFirst().orElseThrow();
        double negativeScore = Double.parseDouble(
            overlay2.property(OverlayFamiliarityPropertyModel.FAMILIARITY_SCORE).orElseThrow());

        assertThat(positiveScore).isGreaterThan(negativeScore);
    }

    private List<Memory> buildInteractionMemories(int count, double pleasureValue) {
        var memories = new java.util.ArrayList<Memory>();
        for (int i = 0; i < count; i++) {
            memories.add(new Memory(
                "mem-" + i, Subject.of("agent", AGENT),
                new MemoryDomain("relationship"), TENANT,
                null, "interaction " + i, Map.of(),
                Instant.now().minusSeconds(count - i),
                null, pleasureValue, 0.3, 0.0, null, null));
        }
        return memories;
    }

    static class StubCaseMemoryStore implements CaseMemoryStore {
        private final List<Memory> memories;
        StubCaseMemoryStore(List<Memory> memories) { this.memories = memories; }

        @Override
        public String store(MemoryInput input) { return "stub"; }

        @Override
        public List<Memory> query(MemoryQuery query) { return memories; }

        @Override
        public int erase(EraseRequest request) { return 0; }
    }
}
