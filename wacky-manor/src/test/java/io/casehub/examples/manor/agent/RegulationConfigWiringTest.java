package io.casehub.examples.manor.agent;

import io.casehub.neocortex.memory.cbr.inmem.InMemoryCbrRecordStore;
import io.casehub.neocortex.mindmap.inmem.InMemoryMindMapStore;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RegulationConfigWiringTest {

    private static final String TENANT = "test-tenant";

    @Test
    void appraisalEnabled_propagatesToCognitionConfig() {
        var pipeline = buildPipeline(true, false);
        assertThat(pipeline.cognitionCore().config().appraisalEnabled())
                .as("CognitionConfig.appraisalEnabled must reflect ManorConfig")
                .isTrue();
    }

    @Test
    void appraisalDisabled_propagatesToCognitionConfig() {
        var pipeline = buildPipeline(false, false);
        assertThat(pipeline.cognitionCore().config().appraisalEnabled())
                .as("CognitionConfig.appraisalEnabled must be false when ManorConfig.appraisal().enabled() is false")
                .isFalse();
    }

    @Test
    void regulationEnabled_propagatesToCognitionConfig() {
        var pipeline = buildPipeline(true, true);
        assertThat(pipeline.cognitionCore().config().regulationEnabled())
                .as("CognitionConfig.regulationEnabled must reflect ManorConfig")
                .isTrue();
    }

    @Test
    void regulationDisabled_propagatesToCognitionConfig() {
        var pipeline = buildPipeline(true, false);
        assertThat(pipeline.cognitionCore().config().regulationEnabled())
                .as("CognitionConfig.regulationEnabled must be false when ManorConfig.regulation().enabled() is false")
                .isFalse();
    }

    @Test
    void salienceEnabled_whenAppraisalEnabled() {
        var pipeline = buildPipeline(true, false);
        assertThat(pipeline.cognitionCore().config().salienceEnabled())
                .as("Salience must be enabled alongside appraisal")
                .isTrue();
    }

    private ManorCognitivePipeline buildPipeline(boolean appraisalEnabled, boolean regulationEnabled) {
        var store = new InMemoryMindMapStore();
        var cbrStore = new InMemoryCbrRecordStore();
        var configs = ManorSocialConfigLoader.loadForProfile("BASELINE");
        var subset = Map.of("peter-perfect", configs.get("peter-perfect"));

        return ManorCognitivePipelineBuilder
                .create(store, TENANT, cbrStore)
                .config(buildManorConfig(appraisalEnabled, regulationEnabled))
                .socialConfigs(subset)
                .seedCaps(false)
                .seedFormationMemories(false)
                .seedGoals(false)
                .seedPeople(false)
                .build();
    }

    private ManorConfig buildManorConfig(boolean appraisalEnabled, boolean regulationEnabled) {
        return new ManorConfig(
                300,
                new ManorConfig.ObservationConfig(10, 15, 10, 5),
                new ManorConfig.NarratorConfig(false, 5, 15),
                new ManorConfig.ReflectionConfig(false, 5, 3.0, 15),
                new ManorConfig.GoalConfig(false, 10, 2),
                new ManorConfig.PlanConfig(false, 5),
                new ManorConfig.TrustConfig(false, 1.0, -2.0),
                new ManorConfig.DispositionConfig(false, 5),
                new ManorConfig.MemoryConfig(20, false, false, 7, 0.2),
                new ManorConfig.ConsolidationConfig(false, 50),
                new ManorConfig.CognitiveSnapshotConfig(0),
                new ManorConfig.AppraisalConfig(appraisalEnabled),
                new ManorConfig.RegulationConfig(regulationEnabled),
                new ManorConfig.ClassificationConfig(false, "", "", 0.2, 1.0, 0.85, 0.15, 2.0),
                new ManorConfig.MoodTuningConfig(20, 1.0),
                new ManorConfig.TranscriptConfig(false, ""),
                new ManorConfig.SessionConfig(80000),
                "",
                5,
                true
        );
    }
}
