package io.casehub.examples.manor.engine;

import io.casehub.blocks.agentic.social.belief.BeliefRevisionConfig;
import io.casehub.blocks.agentic.social.belief.BeliefRevisionPhase;
import io.casehub.examples.manor.agent.ManorCognitiveSeeder;
import io.casehub.examples.manor.agent.ManorSocialConfigLoader;
import io.casehub.neocortex.mindmap.NodeInput;
import io.casehub.neocortex.mindmap.inmem.InMemoryMindMapStore;
import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSession;
import io.casehub.platform.agent.AgentSessionConfig;
import io.casehub.platform.agent.AgentSessionInit;
import io.smallrye.mutiny.Multi;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BeliefRevisionIntegrationTest {

    private static final String TENANT = "revision-test";
    private static final String AGENT = "hooded-claw";

    @Test
    void fullLifecycle_seedContradictSupersede() {
        var store = new InMemoryMindMapStore();
        var seeder = new ManorCognitiveSeeder(store);
        var allConfigs = ManorSocialConfigLoader.load();

        var seedResult = seeder.seed(AGENT, allConfigs.get(AGENT), TENANT);

        for (int i = 0; i < 5; i++) {
            store.addNode(
                NodeInput.of("Penelope outsmarts Hooded Claw attempt " + i, seedResult.subgraphId())
                    .withProvenance("experience-consolidation")
                    .withProperties(Map.of(
                        "cognitiveKind", "experience",
                        "agent-id", AGENT,
                        "event-type", "social_interaction"))
                    .withConfidence(io.casehub.neocortex.cognitive.Confidence.inferred(0.7, Instant.now())),
                TENANT);
        }

        var stubProvider = new StubAgentProvider("""
            {"contradictions": [{
                "beliefNodeId": "PLACEHOLDER",
                "beliefText": "Penelope is naive and trusts too easily",
                "contradictingEvidence": "Penelope outsmarts Hooded Claw",
                "reasoning": "Penelope demonstrated perceptiveness",
                "contradictionStrength": 0.9,
                "revisedBelief": "Penelope is more perceptive than she appears"
            }]}""");

        // High decay rate to supersede in a single run (0.6 * 0.9 = 0.54 decay, 0.8 - 0.54 = 0.26 < 0.3)
        var config = new BeliefRevisionConfig(0.6, 0.3, 0.6);
        var phase = new BeliefRevisionPhase(store, stubProvider, config);

        phase.run(TENANT, List.of());

        var subgraphs = store.listSubgraphs(TENANT);
        var beliefSg = subgraphs.stream()
            .filter(sg -> sg.name().equals("beliefs-" + AGENT))
            .findFirst().orElseThrow();
        var activeNodes = store.nodesIn(beliefSg.id(), TENANT);

        var revisedBelief = activeNodes.stream()
            .filter(n -> n.traits().contains("Belieflike"))
            .filter(n -> "belief-revision".equals(n.provenance()))
            .findFirst();
        assertThat(revisedBelief).isPresent();
        assertThat(revisedBelief.get().name()).contains("perceptive");

        var originalBelief = activeNodes.stream()
            .filter(n -> n.name().contains("naive"))
            .findFirst();
        assertThat(originalBelief).isEmpty();
    }

    @Test
    void noNewEvidence_beliefsUnchanged() {
        var store = new InMemoryMindMapStore();
        var seeder = new ManorCognitiveSeeder(store);
        var allConfigs = ManorSocialConfigLoader.load();

        seeder.seed(AGENT, allConfigs.get(AGENT), TENANT);

        var stubProvider = new StubAgentProvider("""
            {"contradictions": []}""");

        var phase = new BeliefRevisionPhase(store, stubProvider, BeliefRevisionConfig.defaults());
        phase.run(TENANT, List.of());

        var subgraphs = store.listSubgraphs(TENANT);
        var beliefSg = subgraphs.stream()
            .filter(sg -> sg.name().equals("beliefs-" + AGENT))
            .findFirst().orElseThrow();
        var nodes = store.nodesIn(beliefSg.id(), TENANT);
        var beliefs = nodes.stream()
            .filter(n -> n.traits().contains("Belieflike"))
            .toList();

        assertThat(beliefs).hasSize(2);
        beliefs.forEach(b ->
            assertThat(b.confidence().value()).isEqualTo(0.8));
    }

    @Test
    void weakContradiction_decaysButDoesNotSupersede() {
        var store = new InMemoryMindMapStore();
        var seeder = new ManorCognitiveSeeder(store);
        var allConfigs = ManorSocialConfigLoader.load();

        var seedResult = seeder.seed(AGENT, allConfigs.get(AGENT), TENANT);

        store.addNode(
            NodeInput.of("Penelope seemed slightly less naive", seedResult.subgraphId())
                .withProvenance("experience-consolidation")
                .withProperties(Map.of(
                    "cognitiveKind", "experience",
                    "agent-id", AGENT,
                    "event-type", "social_interaction"))
                .withConfidence(io.casehub.neocortex.cognitive.Confidence.inferred(0.5, Instant.now())),
            TENANT);

        var stubProvider = new StubAgentProvider("""
            {"contradictions": [{
                "beliefNodeId": "PLACEHOLDER",
                "beliefText": "Penelope is naive and trusts too easily",
                "contradictingEvidence": "Penelope seemed slightly less naive",
                "reasoning": "Minor evidence of awareness",
                "contradictionStrength": 0.3,
                "revisedBelief": "Penelope may be less naive than assumed"
            }]}""");

        var config = new BeliefRevisionConfig(0.15, 0.3, 0.6);
        var phase = new BeliefRevisionPhase(store, stubProvider, config);
        phase.run(TENANT, List.of());

        var subgraphs = store.listSubgraphs(TENANT);
        var beliefSg = subgraphs.stream()
            .filter(sg -> sg.name().equals("beliefs-" + AGENT))
            .findFirst().orElseThrow();
        var nodes = store.nodesIn(beliefSg.id(), TENANT);

        var penelopeBelief = nodes.stream()
            .filter(n -> n.traits().contains("Belieflike"))
            .filter(n -> n.name().contains("naive"))
            .findFirst().orElseThrow();

        assertThat(penelopeBelief.confidence().value()).isLessThan(0.8);
        assertThat(penelopeBelief.confidence().value()).isGreaterThan(0.3);

        var revised = nodes.stream()
            .filter(n -> n.traits().contains("Belieflike"))
            .filter(n -> "belief-revision".equals(n.provenance()))
            .findFirst();
        assertThat(revised).isEmpty();
    }

    static class StubAgentProvider implements AgentProvider {
        private final String response;

        StubAgentProvider(String response) { this.response = response; }

        @Override
        public Multi<AgentEvent> invoke(AgentSessionConfig config) {
            return Multi.createFrom().item(new AgentEvent.TextDelta(response));
        }

        @Override
        public AgentSession openSession(AgentSessionInit init) {
            throw new UnsupportedOperationException();
        }
    }
}
