package io.casehub.examples.manor.web;

import io.casehub.eidos.api.*;

import java.util.List;

public record CharacterProfileDTO(
    String agentId,
    String name,
    String slot,
    String slotLabel,
    String enneagramType,
    List<DispositionValue> dispositionProfile,
    List<CapabilityDTO> capabilities,
    List<GoalDTO> goals,
    List<ConstraintDTO> constraints,
    String briefing
) {
    public record CapabilityDTO(String name, List<String> tags) {}
    public record GoalDTO(String name, String description, String priority) {}
    public record ConstraintDTO(String name, String description, String severity) {}

    public static CharacterProfileDTO from(AgentDescriptor desc, String slotLabel, String enneagramType) {
        var caps = desc.capabilities().stream()
            .map(c -> new CapabilityDTO(c.name(), c.tags()))
            .toList();

        var goals = desc.goals().stream()
            .filter(g -> g.visibility() == Visibility.PUBLIC)
            .map(g -> new GoalDTO(g.name(), g.description(), g.priority().name()))
            .toList();

        var constraints = desc.constraints().stream()
            .filter(c -> c.visibility() == Visibility.PUBLIC)
            .map(c -> new ConstraintDTO(c.name(), c.description(), c.severity().name()))
            .toList();

        return new CharacterProfileDTO(
            desc.agentId(), desc.name(), desc.slot(), slotLabel, enneagramType,
            desc.disposition() != null ? desc.disposition().dispositionProfile() : List.of(),
            caps, goals, constraints, desc.briefing());
    }
}
