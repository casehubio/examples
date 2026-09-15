package io.casehub.examples.manor.engine;

import io.casehub.neocortex.memory.experience.ScoreableContent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class ActionImportanceScorerTest {

    private final ActionImportanceScorer scorer = new ActionImportanceScorer();

    @Test
    void conflictResolution_highScore() {
        var content = new ScoreableContent("resolved",
            Map.of("event-type", "conflict_resolution"), Instant.EPOCH);
        assertThat(scorer.score(content)).isCloseTo(0.9, offset(0.001));
    }

    @Test
    void trustChange_highScore() {
        var content = new ScoreableContent("trust shifted",
            Map.of("event-type", "trust_change"), Instant.EPOCH);
        assertThat(scorer.score(content)).isCloseTo(0.8, offset(0.001));
    }

    @Test
    void socialInteraction_mediumScore() {
        var content = new ScoreableContent("talked",
            Map.of("event-type", "social_interaction"), Instant.EPOCH);
        assertThat(scorer.score(content)).isCloseTo(0.6, offset(0.001));
    }

    @Test
    void observation_lowScore() {
        var content = new ScoreableContent("saw something",
            Map.of("event-type", "observation"), Instant.EPOCH);
        assertThat(scorer.score(content)).isCloseTo(0.4, offset(0.001));
    }

    @Test
    void idle_veryLowScore() {
        var content = new ScoreableContent("nothing",
            Map.of("event-type", "idle"), Instant.EPOCH);
        assertThat(scorer.score(content)).isCloseTo(0.1, offset(0.001));
    }

    @Test
    void unknownEventType_defaultScore() {
        var content = new ScoreableContent("something",
            Map.of("event-type", "unexpected_type"), Instant.EPOCH);
        assertThat(scorer.score(content)).isCloseTo(0.3, offset(0.001));
    }

    @Test
    void missingEventType_defaultScore() {
        var content = new ScoreableContent("no type", Map.of(), Instant.EPOCH);
        assertThat(scorer.score(content)).isCloseTo(0.3, offset(0.001));
    }
}
