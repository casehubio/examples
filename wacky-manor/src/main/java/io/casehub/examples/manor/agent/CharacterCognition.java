package io.casehub.examples.manor.agent;

import io.casehub.blocks.summarisation.observation.affordance.ObservationSection;
import io.casehub.eidos.api.AgentConstraint;
import io.casehub.examples.manor.model.ActionType;
import io.casehub.examples.manor.model.CharacterState;
import io.casehub.neocortex.cognitive.index.CognitiveDefaults;
import io.casehub.neocortex.memory.Memory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class CharacterCognition {

    private final String                                                agentId;
    private final AgentExperienceService                                experienceService;
    private final CognitiveDefaults                                     cognitiveDefaults;
    private final SocialConfig                                          socialConfig;
    private final List<AgentConstraint>                                 constraints;
    private final io.casehub.neocortex.cognitive.index.CognitiveProfile cognitiveProfile;
    private final ManorContextStrategy                                  contextStrategy;
    private final io.casehub.blocks.agentic.social.CognitionCore        cognitionCore;
    private final ManorCognitiveSeeder.SeedResult                       seedResult;
    private final String                                                tenantId;

    public CharacterCognition(String agentId, AgentExperienceService experienceService) {
        this(agentId, experienceService, null, SocialConfig.empty(), List.of(),
             null, new ManorContextStrategy(), null, null, null);
    }

    public CharacterCognition(String agentId, AgentExperienceService experienceService,
                              CognitiveDefaults cognitiveDefaults, SocialConfig socialConfig,
                              List<AgentConstraint> constraints) {
        this(agentId, experienceService, cognitiveDefaults, socialConfig, constraints,
             null, new ManorContextStrategy(), null, null, null);
    }

    public CharacterCognition(String agentId, AgentExperienceService experienceService,
                              CognitiveDefaults cognitiveDefaults, SocialConfig socialConfig,
                              List<AgentConstraint> constraints,
                              io.casehub.neocortex.cognitive.index.CognitiveProfile cognitiveProfile,
                              ManorContextStrategy contextStrategy,
                              io.casehub.blocks.agentic.social.CognitionCore cognitionCore,
                              ManorCognitiveSeeder.SeedResult seedResult,
                              String tenantId) {
        this.agentId           = agentId;
        this.experienceService = experienceService;
        this.cognitiveDefaults = cognitiveDefaults;
        this.socialConfig      = socialConfig != null ? socialConfig : SocialConfig.empty();
        this.constraints       = constraints != null ? List.copyOf(constraints) : List.of();
        this.cognitiveProfile  = cognitiveProfile;
        this.contextStrategy   = contextStrategy != null ? contextStrategy : new ManorContextStrategy();
        this.cognitionCore     = cognitionCore;
        this.seedResult        = seedResult;
        this.tenantId          = tenantId;
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

        var budget = contextStrategy.budgetFor(nearbyAgentIds.size(), 0.5, 0);
        var selectedNorms = contextStrategy.selectNorms(socialConfig.norms(), agentNames.values(), character.inventory(), budget);
        if (!selectedNorms.isEmpty()) {
            var items = selectedNorms.stream()
                                      .map(SocialConfig.NormEntry::rule)
                                      .toList();
            sections.add(ObservationSection.items("Social Rules", null, items));
        }

        if (cognitionCore != null && tenantId != null) {
            var promptCtx = new io.casehub.blocks.speech.PromptContext(agentId, tenantId, null);
            for (var ps : cognitionCore.promptSections()) {
                var rendered = ps.contribute(promptCtx);
                if (rendered != null && !rendered.isBlank()) {
                    sections.add(ObservationSection.text("Cognitive State", rendered));
                }
            }
        }

        sections.addAll(renderSocialAwareness(nearbyAgentIds, agentNames));

        return sections;
    }



    List<ObservationSection> renderSocialAwareness(
            java.util.Collection<String> nearbyAgentIds,
            java.util.Map<String, String> agentNames) {
        if (cognitiveProfile == null || tenantId == null) {
            return List.of();
        }
        if (!contextStrategy.shouldCompareSocially(socialConfig, false)) {
            return List.of();
        }

        var selfPrincipal = io.casehub.platform.api.identity.PrincipalId.agent(agentId);
        var lines = new java.util.ArrayList<String>();

        for (String nearbyId : nearbyAgentIds) {
            var otherPrincipal = io.casehub.platform.api.identity.PrincipalId.agent(nearbyId);
            String otherName = agentNames.getOrDefault(nearbyId, nearbyId);
            try {
                var query = io.casehub.neocortex.cognitive.index.CognitiveProfileQuery
                        .byName(nearbyId, tenantId);
                var perspectives = cognitiveProfile.compare(query,
                        java.util.Set.of(selfPrincipal, otherPrincipal));
                if (perspectives.isEmpty()) continue;

                var comparison = io.casehub.neocortex.cognitive.index.SocialComparison
                        .compare(perspectives);
                PerceptionTranslator.translate(comparison, selfPrincipal, otherPrincipal, otherName)
                        .ifPresent(lines::add);
            } catch (Exception e) {
                // graceful degradation
            }
        }

        if (lines.isEmpty()) return List.of();
        return List.of(ObservationSection.items("Social Awareness", null, lines));
    }

    public void recordTrustEvent(String targetId, ActionType action) {
        if (!ManorTrustEvents.isRelevant(action)) {return;}
        if (cognitiveDefaults != null && cognitiveDefaults.socialCognition() != null) {
            io.casehub.neocortex.cognitive.index.SocialCognitionDefaults social = cognitiveDefaults.socialCognition();
            ManorTrustEvents.weightFor(action, social.trustFormationRate(), social.conflictInterpretation());
        } else {
            ManorTrustEvents.weightFor(action);
        }
    }
}
