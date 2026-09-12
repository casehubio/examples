package io.casehub.examples.manor.agent;

import io.casehub.blocks.summarisation.observation.affordance.ObservationSection;
import io.casehub.eidos.api.AgentConstraint;
import io.casehub.examples.manor.model.ActionType;
import io.casehub.examples.manor.model.CharacterState;
import io.casehub.neocortex.cognitive.index.CognitiveDefaults;
import io.casehub.neocortex.cognitive.index.SocialCognitionDefaults;
import io.casehub.neocortex.memory.Memory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class CharacterCognition {

    private final String                 agentId;
    private final AgentExperienceService experienceService;
    private final CognitiveDefaults      cognitiveDefaults;
    private final SocialConfig           socialConfig;
    private final List<AgentConstraint>  constraints;

    public CharacterCognition(String agentId, AgentExperienceService experienceService) {
        this(agentId, experienceService, null, SocialConfig.empty(), List.of());
    }

    public CharacterCognition(String agentId, AgentExperienceService experienceService,
                              CognitiveDefaults cognitiveDefaults, SocialConfig socialConfig,
                              List<AgentConstraint> constraints) {
        this.agentId           = agentId;
        this.experienceService = experienceService;
        this.cognitiveDefaults = cognitiveDefaults;
        this.socialConfig      = socialConfig != null ? socialConfig : SocialConfig.empty();
        this.constraints       = constraints != null ? List.copyOf(constraints) : List.of();
    }

    public String agentId() {return agentId;}

    public double computeImportance(ActionType action) {
        if (action == null) {return 0.5;}
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
        var sections = new ArrayList<ObservationSection>();

        if (!socialConfig.drives().isEmpty()) {
            var items = socialConfig.drives().stream()
                                    .sorted((a, b) -> Double.compare(b.intensity(), a.intensity()))
                                    .map(d -> String.format("%s (%.0f%%) — %s", d.type(), d.intensity() * 100, d.description()))
                                    .toList();
            sections.add(ObservationSection.items("Your Drives", null, items));
        }

        if (!constraints.isEmpty()) {
            var items = constraints.stream()
                                   .map(AgentConstraint::description)
                                   .toList();
            sections.add(ObservationSection.items("Your Principles", null, items));
        }

        if (!socialConfig.initialBeliefs().isEmpty()) {
            var items = socialConfig.initialBeliefs().stream()
                                    .map(SocialConfig.InitialBelief::value)
                                    .toList();
            sections.add(ObservationSection.items("Your Beliefs", null, items));
        }

        var filteredNorms = ManorNormFilter.filter(socialConfig.norms(), nearbyAgentIds, character.inventory());
        if (!filteredNorms.isEmpty()) {
            var items = filteredNorms.stream()
                                     .map(SocialConfig.NormEntry::rule)
                                     .toList();
            sections.add(ObservationSection.items("Social Rules", null, items));
        }

        return sections;
    }

    public void recordTrustEvent(String targetId, ActionType action) {
        if (!ManorTrustEvents.isRelevant(action)) {return;}
        if (cognitiveDefaults != null && cognitiveDefaults.socialCognition() != null) {
            SocialCognitionDefaults social = cognitiveDefaults.socialCognition();
            ManorTrustEvents.weightFor(action, social.trustFormationRate(), social.conflictInterpretation());
        } else {
            ManorTrustEvents.weightFor(action);
        }
    }
}
