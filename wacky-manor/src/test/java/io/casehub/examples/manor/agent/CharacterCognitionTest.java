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

    @Test
    void recordTrustEventDoesNotThrow() {
        var cognition = new CharacterCognition("test-agent", null);
        cognition.recordTrustEvent("other-agent", ActionType.STEAL);
        cognition.recordTrustEvent("other-agent", ActionType.GIVE);
    }

    @Test
    void recallMemoriesReturnsEmptyWithNullService() {
        var cognition = new CharacterCognition("test-agent", null);
        assertThat(cognition.recallMemories(10)).isEmpty();
        assertThat(cognition.recallReflections(5)).isEmpty();
        assertThat(cognition.recallRelationships("other", 3)).isEmpty();
    }
}
