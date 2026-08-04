package io.casehub.examples.manor.web;

import io.casehub.eidos.api.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class CharacterProfileDTOTest {

    @Test
    void projects_basic_fields() {
        var descriptor = new AgentDescriptor(
            "test-agent", "Test Agent", null, null, null, null, null,
            null, "urn:casehub:vocab:belbin", "urn:casehub:vocab:jungian", null,
            "shaper", List.of(), AgentDisposition.builder()
                .dispositionProfile(new DispositionValue("te", 0.35), new DispositionValue("ni", 0.2))
                .build(),
            null, null, "test-tenancy", "A test agent briefing.",
            null, List.of(), List.of());

        var dto = CharacterProfileDTO.from(descriptor, null, null);

        assertThat(dto.agentId()).isEqualTo("test-agent");
        assertThat(dto.name()).isEqualTo("Test Agent");
        assertThat(dto.slot()).isEqualTo("shaper");
        assertThat(dto.briefing()).isEqualTo("A test agent briefing.");
        assertThat(dto.dispositionProfile()).hasSize(2);
        assertThat(dto.dispositionProfile().getFirst().term()).isEqualTo("te");
    }

    @Test
    void filters_private_goals() {
        var goals = List.of(
            new AgentGoal("public-goal", "A public goal",
                GoalPriority.PRIMARY, Visibility.PUBLIC, List.of()),
            new AgentGoal("private-goal", "A private goal",
                GoalPriority.PRIMARY, Visibility.PRIVATE, List.of()));

        var descriptor = new AgentDescriptor(
            "test", "Test", null, null, null, null, null,
            null, null, null, null,
            "shaper", List.of(), AgentDisposition.builder().build(),
            null, null, "t", null, null, goals, List.of());

        var dto = CharacterProfileDTO.from(descriptor, null, null);
        assertThat(dto.goals()).hasSize(1);
        assertThat(dto.goals().getFirst().name()).isEqualTo("public-goal");
    }

    @Test
    void filters_private_constraints() {
        var constraints = List.of(
            new AgentConstraint("public-c", "Public", Visibility.PUBLIC, ConstraintSeverity.HARD),
            new AgentConstraint("private-c", "Private", Visibility.PRIVATE, ConstraintSeverity.SOFT));

        var descriptor = new AgentDescriptor(
            "test", "Test", null, null, null, null, null,
            null, null, null, null,
            "shaper", List.of(), AgentDisposition.builder().build(),
            null, null, "t", null, null, List.of(), constraints);

        var dto = CharacterProfileDTO.from(descriptor, null, null);
        assertThat(dto.constraints()).hasSize(1);
        assertThat(dto.constraints().getFirst().name()).isEqualTo("public-c");
    }
}
