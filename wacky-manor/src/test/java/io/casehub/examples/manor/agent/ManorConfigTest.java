package io.casehub.examples.manor.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ManorConfigTest {

    @Test
    void configRecordsConstruct() {
        var config = new ManorConfig(300,
            new ManorConfig.ObservationConfig(10, 15),
            new ManorConfig.NarratorConfig(true, 5, 15),
            new ManorConfig.ReflectionConfig(true, 5, 3.0, 15),
            new ManorConfig.GoalConfig(true, 10, 2),
            new ManorConfig.PlanConfig(true, 5),
            new ManorConfig.TrustConfig(true, 1.0, -2.0),
            new ManorConfig.DispositionConfig(true, 5),
            new ManorConfig.MemoryConfig(20, true, true, 7, 0.2),
            "", 5);
        assertThat(config.maxTurns()).isEqualTo(300);
        assertThat(config.observation().verbatimThreshold()).isEqualTo(10);
        assertThat(config.observation().groupedThreshold()).isEqualTo(15);
        assertThat(config.narrator().enabled()).isTrue();
        assertThat(config.reflection().maxUnreflected()).isEqualTo(5);
        assertThat(config.reflection().importanceThreshold()).isEqualTo(3.0);
        assertThat(config.goal().cooldownTicks()).isEqualTo(10);
        assertThat(config.plan().maxRevisionGeneration()).isEqualTo(5);
        assertThat(config.trust().positiveWeight()).isEqualTo(1.0);
        assertThat(config.trust().negativeWeight()).isEqualTo(-2.0);
        assertThat(config.disposition().evolutionCheckInterval()).isEqualTo(5);
        assertThat(config.memory().recallLimit()).isEqualTo(20);
        assertThat(config.memory().personalityWeightedRetrieval()).isTrue();
        assertThat(config.memory().decayEnabled()).isTrue();
        assertThat(config.memory().decayMaxAgeDays()).isEqualTo(7);
        assertThat(config.memory().decayMinImportance()).isEqualTo(0.2);
        assertThat(config.maxConcurrentAgents()).isEqualTo(5);
    }
}
