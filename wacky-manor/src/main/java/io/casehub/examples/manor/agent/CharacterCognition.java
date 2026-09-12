package io.casehub.examples.manor.agent;

import io.casehub.blocks.summarisation.observation.affordance.ObservationSection;
import io.casehub.examples.manor.model.ActionType;
import io.casehub.examples.manor.model.CharacterState;
import io.casehub.neocortex.memory.Memory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class CharacterCognition {

    private final String agentId;
    private final AgentExperienceService experienceService;

    public CharacterCognition(String agentId, AgentExperienceService experienceService) {
        this.agentId = agentId;
        this.experienceService = experienceService;
    }

    public String agentId() { return agentId; }

    public double computeImportance(ActionType action) {
        if (action == null) return 0.5;
        return switch (action) {
            case STEAL -> 0.9;
            case USE -> 0.8;
            case TAKE, GIVE, PULL_ASIDE -> 0.7;
            case INTERACT -> 0.6;
            case MOVE -> 0.3;
            case LOOK -> 0.2;
            case WAIT -> 0.1;
        };
    }

    public void recordExperience(String room, String description, String thinking,
                                  double importance, String targetAgentId, int tick) {
        if (experienceService != null) {
            experienceService.ingest(agentId, room, description, thinking, importance, targetAgentId, tick);
        }
    }

    public List<Memory> recallMemories(int limit) {
        return experienceService != null ? experienceService.recall(agentId, limit) : List.of();
    }

    public List<Memory> recallReflections(int limit) {
        return experienceService != null ? experienceService.recallReflections(agentId, limit) : List.of();
    }

    public List<Memory> recallRelationships(String otherId, int limit) {
        return experienceService != null ? experienceService.recallRelationships(agentId, otherId, limit) : List.of();
    }

    public List<ObservationSection> renderCognitiveSections(
            CharacterState character,
            Collection<String> nearbyAgentIds,
            Map<String, String> agentNames) {
        return List.of();
    }

    public void recordTrustEvent(String targetId, ActionType action) {
        // Tier 2 buffer — will be wired to mindmap edges in Batch 3
    }
}
