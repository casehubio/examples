package io.casehub.examples.manor.agent;

import io.casehub.neocortex.memory.cbr.inmem.InMemoryCbrRecordStore;
import io.casehub.neocortex.mindmap.inmem.InMemoryMindMapStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ManorCognitivePipelineTest {

    private static final String TENANT = "test-tenant";

    private InMemoryMindMapStore mindMapStore;
    private InMemoryCbrRecordStore cbrStore;

    @BeforeEach
    void setUp() {
        mindMapStore = new InMemoryMindMapStore();
        cbrStore = new InMemoryCbrRecordStore();
    }

    @Test
    void build_seeds_drive_nodes_for_each_character() {
        var configs = ManorSocialConfigLoader.loadForProfile("BASELINE");
        var subset = Map.of(
                "peter-perfect", configs.get("peter-perfect"),
                "dick-dastardly", configs.get("dick-dastardly"));

        var pipeline = ManorCognitivePipelineBuilder
                .create(mindMapStore, TENANT, cbrStore)
                .socialConfigs(subset)
                .seedCaps(false)
                .seedFormationMemories(false)
                .seedGoals(false)
                .seedPeople(false)
                .build();

        assertThat(pipeline.hasDriveNodes("peter-perfect"))
                .as("Peter Perfect should have drive nodes")
                .isTrue();
        assertThat(pipeline.hasDriveNodes("dick-dastardly"))
                .as("Dick Dastardly should have drive nodes")
                .isTrue();
        assertThat(pipeline.driveNodeCount("peter-perfect"))
                .as("Peter Perfect has 3 drives")
                .isGreaterThanOrEqualTo(3);
        assertThat(pipeline.driveNodeCount("dick-dastardly"))
                .as("Dick Dastardly has 4 drives")
                .isGreaterThanOrEqualTo(4);
    }

    @Test
    void build_creates_cognitive_subgraph_per_agent() {
        var configs = ManorSocialConfigLoader.loadForProfile("BASELINE");
        var subset = Map.of("peter-perfect", configs.get("peter-perfect"));

        var pipeline = ManorCognitivePipelineBuilder
                .create(mindMapStore, TENANT, cbrStore)
                .socialConfigs(subset)
                .seedCaps(false)
                .seedFormationMemories(false)
                .seedGoals(false)
                .seedPeople(false)
                .build();

        assertThat(pipeline.hasCognitiveSubgraph("peter-perfect")).isTrue();
        assertThat(pipeline.hasCognitiveSubgraph("nonexistent")).isFalse();
    }

    @Test
    void build_seeds_people_subgraph() {
        var configs = ManorSocialConfigLoader.loadForProfile("BASELINE");
        var subset = Map.of(
                "peter-perfect", configs.get("peter-perfect"),
                "penelope-pitstop", configs.get("penelope-pitstop"));

        var pipeline = ManorCognitivePipelineBuilder
                .create(mindMapStore, TENANT, cbrStore)
                .socialConfigs(subset)
                .seedCaps(false)
                .seedFormationMemories(false)
                .seedGoals(false)
                .build();

        assertThat(pipeline.hasSocialSubgraph())
                .as("People subgraph should exist after seedPeople")
                .isTrue();
    }

    @Test
    void build_without_people_seeding_has_no_social_subgraph() {
        var configs = ManorSocialConfigLoader.loadForProfile("BASELINE");
        var subset = Map.of("peter-perfect", configs.get("peter-perfect"));

        var pipeline = ManorCognitivePipelineBuilder
                .create(mindMapStore, TENANT, cbrStore)
                .socialConfigs(subset)
                .seedCaps(false)
                .seedFormationMemories(false)
                .seedGoals(false)
                .seedPeople(false)
                .build();

        assertThat(pipeline.hasSocialSubgraph()).isFalse();
    }

    @Test
    void build_registers_cognitive_defaults_per_agent() {
        var configs = ManorSocialConfigLoader.loadForProfile("BASELINE");
        var subset = Map.of("peter-perfect", configs.get("peter-perfect"));

        var pipeline = ManorCognitivePipelineBuilder
                .create(mindMapStore, TENANT, cbrStore)
                .socialConfigs(subset)
                .seedCaps(false)
                .seedFormationMemories(false)
                .seedGoals(false)
                .seedPeople(false)
                .build();

        assertThat(pipeline.defaultsRegistry().forAgent("peter-perfect"))
                .as("Cognitive defaults should be registered")
                .isPresent();
    }

    @Test
    void seeded_drive_names_match_social_config() {
        var configs = ManorSocialConfigLoader.loadForProfile("BASELINE");
        var ppConfig = configs.get("peter-perfect");
        var subset = Map.of("peter-perfect", ppConfig);

        var pipeline = ManorCognitivePipelineBuilder
                .create(mindMapStore, TENANT, cbrStore)
                .socialConfigs(subset)
                .seedCaps(false)
                .seedFormationMemories(false)
                .seedGoals(false)
                .seedPeople(false)
                .build();

        var expectedDrives = ppConfig.drives().stream()
                .map(SocialConfig.Drive::type)
                .sorted()
                .toList();
        assertThat(pipeline.seededDriveNames("peter-perfect"))
                .as("Drive names should match social config")
                .containsAll(expectedDrives);
    }

    @Test
    void seeded_drives_have_properties_drive_adaptation_phase_expects() {
        var configs = ManorSocialConfigLoader.loadForProfile("BASELINE");
        var subset  = Map.of("dick-dastardly", configs.get("dick-dastardly"));

        var pipeline = ManorCognitivePipelineBuilder
                               .create(mindMapStore, TENANT, cbrStore)
                               .socialConfigs(subset)
                               .seedCaps(false)
                               .seedFormationMemories(false)
                               .seedGoals(false)
                               .seedPeople(false)
                               .build();

        var subgraphName = ManorCognitiveSeeder.subgraphName("dick-dastardly");
        var subgraph = mindMapStore.listSubgraphs(TENANT).stream()
                                   .filter(s -> subgraphName.equals(s.name()))
                                   .findFirst()
                                   .orElseThrow();
        assertThat(subgraph.type()).as("Subgraph type must be 'cognitive' for DriveAdaptationPhase").isEqualTo("cognitive");

        var driveNodes = mindMapStore.nodesIn(subgraph.id(), TENANT).stream()
                                     .filter(n -> "drive-intensity".equals(n.properties().get("cognitiveKind")))
                                     .toList();
        assertThat(driveNodes).as("Drive nodes must exist for DriveAdaptationPhase to find").isNotEmpty();
        for (var node : driveNodes) {
            assertThat(node.properties()).containsKey("drive-type");
            assertThat(node.properties()).containsKey("intensity");
            assertThat(node.properties()).containsKey("agent-id");
            assertThat(node.properties().get("agent-id")).isEqualTo("dick-dastardly");
        }
    }

    @Test
    void seeded_formation_memories_reach_experience_recorder() {
        var configs = ManorSocialConfigLoader.loadForProfile("BASELINE");
        var subset  = Map.of("hooded-claw", configs.get("hooded-claw"));
        assertThat(configs.get("hooded-claw").formationMemories())
                .as("Test requires a character with formation memories")
                .isNotEmpty();

        var recorder = new CapturingExperienceRecorder();
        ManorCognitivePipelineBuilder
                .create(mindMapStore, TENANT, cbrStore)
                .experienceRecorder(recorder)
                .socialConfigs(subset)
                .seedCaps(false)
                .seedGoals(false)
                .seedPeople(false)
                .build();

        assertThat(recorder.recorded)
                .as("Formation memories should be recorded for hooded-claw")
                .isNotEmpty();
        assertThat(recorder.recorded).allSatisfy(fe -> {
            assertThat(fe.agentId()).isEqualTo("hooded-claw");
            assertThat(fe.tenantId()).isEqualTo(TENANT);
        });
    }

    @Test
    void seeded_caps_state_exists_after_build() {
        var configs = ManorSocialConfigLoader.loadForProfile("BASELINE");
        var subset  = Map.of("dick-dastardly", configs.get("dick-dastardly"));

        var capsEngine = new io.casehub.neocortex.caps.testing.InMemoryCapsEngine();
        ManorCognitivePipelineBuilder
                .create(mindMapStore, TENANT, cbrStore)
                .capsEngine(capsEngine)
                .socialConfigs(subset)
                .seedFormationMemories(false)
                .seedGoals(false)
                .seedPeople(false)
                .build();

        var state = capsEngine.loadState(TENANT, "dick-dastardly");
        assertThat(state)
                .as("CAPS state should exist after seedCapsState")
                .isNotNull();
        assertThat(state.agentId()).isEqualTo("dick-dastardly");
        assertThat(state.tenantId()).isEqualTo(TENANT);
    }

    private static class CapturingExperienceRecorder
            implements io.casehub.neocortex.memory.experience.ExperienceRecorder {

        final java.util.ArrayList<io.casehub.neocortex.memory.experience.FormativeExperience> recorded =
                new java.util.ArrayList<>();

        @Override
        public String record(io.casehub.neocortex.memory.experience.ExperienceEvent event) {
            if (event instanceof io.casehub.neocortex.memory.experience.FormativeExperience fe) {
                recorded.add(fe);
            }
            return "test-id";
        }

        @Override
        public io.casehub.neocortex.memory.experience.ExperienceStoreResult recordAll(
                java.util.List<io.casehub.neocortex.memory.experience.ExperienceEvent> events) {
            for (var e : events) {record(e);}
            return io.casehub.neocortex.memory.experience.ExperienceStoreResult.empty();
        }
    }


}
