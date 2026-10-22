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
                .map(SocialConfig.DriveConfig::type)
                .sorted()
                .toList();
        assertThat(pipeline.seededDriveNames("peter-perfect"))
                .as("Drive names should match social config")
                .containsAll(expectedDrives);
    }
}
