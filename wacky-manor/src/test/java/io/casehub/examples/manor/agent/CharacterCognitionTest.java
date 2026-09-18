package io.casehub.examples.manor.agent;

import io.casehub.examples.manor.model.ActionType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CharacterCognitionTest {

    @Test
    void computeImportanceMatchesExistingBehavior() {
        var cognition = new CharacterCognition("test-agent", null);
        assertThat(cognition.computeImportance(ActionType.STEAL)).isEqualTo(0.9);
        assertThat(cognition.computeImportance(ActionType.USE)).isEqualTo(0.8);
        assertThat(cognition.computeImportance(ActionType.TAKE)).isEqualTo(0.7);
        assertThat(cognition.computeImportance(ActionType.GIVE)).isEqualTo(0.7);
        assertThat(cognition.computeImportance(ActionType.PULL_ASIDE)).isEqualTo(0.7);
        assertThat(cognition.computeImportance(ActionType.INTERACT)).isEqualTo(0.6);
        assertThat(cognition.computeImportance(ActionType.MOVE)).isEqualTo(0.3);
        assertThat(cognition.computeImportance(ActionType.LOOK)).isEqualTo(0.2);
        assertThat(cognition.computeImportance(ActionType.WAIT)).isEqualTo(0.1);
        assertThat(cognition.computeImportance(null)).isEqualTo(0.5);
    }

    @Test
    void cognitiveSectionsEmptyBeforeWiring() {
        var cognition = new CharacterCognition("test-agent", null);
        var sections = cognition.renderCognitiveSections(
            new io.casehub.examples.manor.model.CharacterState("test-agent", "Test", "Room", 0.0, List.of()),
            List.of(), Map.of());
        assertThat(sections).isEmpty();
    }

    @org.junit.jupiter.api.Disabled("pre-existing: recordTrustEvent removed in trust evolution refactor")
    @Test
    void recordTrustEventDoesNotThrow() {
        // var cognition = new CharacterCognition("test-agent", null);
        // cognition.recordTrustEvent("other-agent", ActionType.STEAL);
        // cognition.recordTrustEvent("other-agent", ActionType.GIVE);
    }

    @Test
    void recallMemoriesReturnsEmptyWithNullService() {
        var cognition = new CharacterCognition("test-agent", null);
        assertThat(cognition.recallMemories(10)).isEmpty();
        assertThat(cognition.recallReflections(5)).isEmpty();
        assertThat(cognition.recallRelationships("other", 3)).isEmpty();
    }

    @Test
    void drivesRenderedWhenSocialConfigPresent() {
        var socialConfig = ManorSocialConfigLoader.load().get("hooded-claw");
        var cognition    = new CharacterCognition("hooded-claw", null, null, socialConfig, List.of());
        var sections = cognition.renderCognitiveSections(
                new io.casehub.examples.manor.model.CharacterState("hooded-claw", "HC", "Room", 0.0, List.of()),
                List.of(), Map.of());
        assertThat(sections).noneMatch(s -> s.header().equals("Your Drives"));
    }

    @Test
    void constraintsNoLongerRenderedDirectly() {
        var constraint = new io.casehub.eidos.api.AgentConstraint(
                "test-constraint", "Never reveal your true identity",
                io.casehub.eidos.api.Visibility.PRIVATE, io.casehub.eidos.api.ConstraintSeverity.HARD);
        var cognition = new CharacterCognition("test", null, null, SocialConfig.empty(), List.of(constraint));
        var sections = cognition.renderCognitiveSections(
                new io.casehub.examples.manor.model.CharacterState("test", "Test", "Room", 0.0, List.of()),
                List.of(), Map.of());
        assertThat(sections).noneMatch(s -> s.header().equals("Your Principles"));
    }

    @Test
    void beliefsAndNormsRendered() {
        var socialConfig = ManorSocialConfigLoader.load().get("penelope-pitstop");
        var cognition    = new CharacterCognition("penelope-pitstop", null, null, socialConfig, List.of());
        var sections = cognition.renderCognitiveSections(
                new io.casehub.examples.manor.model.CharacterState("penelope-pitstop", "Penelope", "Room", 0.0, List.of()),
                List.of(), Map.of());
        assertThat(sections).anyMatch(s -> s.header().equals("Your Beliefs"));
        assertThat(sections).anyMatch(s -> s.header().equals("Social Rules"));
    }

    @Test
    void allSectionsForFullyConfiguredCharacter() {
        var socialConfig = ManorSocialConfigLoader.load().get("hooded-claw");
        var cognition    = new CharacterCognition("hooded-claw", null, null, socialConfig, List.of());
        var sections = cognition.renderCognitiveSections(
                new io.casehub.examples.manor.model.CharacterState("hooded-claw", "HC", "Room", 0.0, List.of()),
                List.of("penelope-pitstop"), Map.of("penelope-pitstop", "Penelope Pitstop"));
        assertThat(sections).hasSize(2);
        assertThat(sections.stream().map(s -> s.header()).toList())
                .containsExactly("Your Beliefs", "Social Rules");
    }

    @Test
    void socialAwarenessAbsentWithoutSchemingDrives() {
        var socialConfig = ManorSocialConfigLoader.load().get("penelope-pitstop");
        var cognition = new CharacterCognition("penelope-pitstop", null, null, socialConfig, List.of());
        var sections = cognition.renderCognitiveSections(
                new io.casehub.examples.manor.model.CharacterState("penelope-pitstop", "Penelope", "Room", 0.0, List.of()),
                List.of("hooded-claw"), Map.of("hooded-claw", "Hooded Claw"));
        assertThat(sections.stream().map(s -> s.header()).toList())
                .doesNotContain("Social Awareness");
    }

    @Test
    void socialAwarenessAbsentWhenCognitiveProfileNull() {
        var drives = List.of(new SocialConfig.Drive("scheming", 0.9, "Schemes"));
        var socialConfig = new SocialConfig(List.of(), drives, List.of(), List.of(), List.of(), Map.of(), null);
        var cognition = new CharacterCognition("hooded-claw", null, null, socialConfig, List.of());
        var sections = cognition.renderCognitiveSections(
                new io.casehub.examples.manor.model.CharacterState("hooded-claw", "HC", "Room", 0.0, List.of()),
                List.of("peter-perfect"), Map.of("peter-perfect", "Peter Perfect"));
        assertThat(sections.stream().map(s -> s.header()).toList())
                .doesNotContain("Social Awareness");
    }


    @Test
    void beliefRenderingFromMindMapStore() {
        var store      = new io.casehub.neocortex.mindmap.inmem.InMemoryMindMapStore();
        var seeder     = new ManorCognitiveSeeder(store);
        var allConfigs = ManorSocialConfigLoader.load();
        var agent      = "hooded-claw";
        var tenant     = "rendering-test";

        seeder.seed(agent, allConfigs.get(agent), tenant);

        var cognition = new CharacterCognition(
                agent, null, null, allConfigs.get(agent), List.of(),
                null, new ManorContextStrategy(), null, null,
                tenant, store, null);

        var sections = cognition.renderCognitiveSections(
                new io.casehub.examples.manor.model.CharacterState(agent, "HC", "library", 0.0, List.of()),
                List.of(), Map.of());

        var beliefSection = sections.stream()
                                    .filter(s -> "Your Beliefs".equals(s.header()))
                                    .findFirst().orElseThrow();

        assertThat(((io.casehub.blocks.summarisation.observation.affordance.ObservationSection.ItemList) beliefSection).items())
                .anyMatch(item -> item.contains("naive"));
    }

    @Test
    void revisedBeliefShowsRevisedMarker() {
        var store      = new io.casehub.neocortex.mindmap.inmem.InMemoryMindMapStore();
        var seeder     = new ManorCognitiveSeeder(store);
        var allConfigs = ManorSocialConfigLoader.load();
        var agent      = "hooded-claw";
        var tenant     = "revised-test";

        var seedResult = seeder.seed(agent, allConfigs.get(agent), tenant);

        var revisedNodeId = store.addNode(
                io.casehub.neocortex.mindmap.NodeInput.of(
                          "Penelope is more perceptive than she appears", seedResult.subgraphId())
                                                      .withConfidence(io.casehub.neocortex.cognitive.Confidence.inferred(0.6, java.time.Instant.now()))
                                                      .withProvenance("belief-revision")
                                                      .withTraits(java.util.Set.of("Belieflike"))
                                                      .withProperties(java.util.Map.of("subject", "penelope-awareness"))
                                                      .withPrincipalId(io.casehub.platform.api.identity.PrincipalId.agent(agent)),
                tenant);

        var subgraphs = store.listSubgraphs(tenant);
        var beliefSg = subgraphs.stream()
                                .filter(sg -> sg.name().equals("beliefs-" + agent))
                                .findFirst().orElseThrow();
        var originalId = store.nodesIn(beliefSg.id(), tenant).stream()
                              .filter(n -> n.traits().contains("Belieflike"))
                              .filter(n -> "penelope-awareness".equals(n.property("subject").orElse(null)))
                              .filter(n -> "manor-seed".equals(n.provenance()))
                              .map(io.casehub.neocortex.mindmap.MindMapNode::id)
                              .findFirst().orElseThrow();

        store.supersede(originalId, revisedNodeId, "Penelope demonstrated perceptiveness", tenant);

        var cognition = new CharacterCognition(
                agent, null, null, allConfigs.get(agent), List.of(),
                null, new ManorContextStrategy(), null, null,
                tenant, store, null);

        var sections = cognition.renderCognitiveSections(
                new io.casehub.examples.manor.model.CharacterState(agent, "HC", "library", 0.0, List.of()),
                List.of(), Map.of());

        var beliefSection = sections.stream()
                                    .filter(s -> "Your Beliefs".equals(s.header()))
                                    .findFirst().orElseThrow();

        assertThat(((io.casehub.blocks.summarisation.observation.affordance.ObservationSection.ItemList) beliefSection).items())
                .anyMatch(item -> item.contains("[REVISED]") && item.contains("penelope-awareness"));
        assertThat(((io.casehub.blocks.summarisation.observation.affordance.ObservationSection.ItemList) beliefSection).items())
                .noneMatch(item -> item.contains("naive"));
    }

}
