package io.casehub.examples.manor.agent;

import io.casehub.neocortex.cognition.relationship.RelationshipStageConfig;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record SocialConfig(
        List<GoalConfig> goals,
        List<Drive> drives,
        List<NormEntry> norms,
        List<InitialBelief> initialBeliefs,
        List<Relationship> relationships,
        Map<String, List<ReinforcementMapping>> reinforcement,
        RelationshipStageConfig stageConfig,
        @Nullable PersonaConstraintMapping personaConstraint,
        List<String> tendencies,
        List<PersonalityFacet> personalityFacets,
        List<RelationalSchema> relationalSchemas,
        @Nullable AttachmentStyle attachment,
        List<FormationMemory> formationMemories,
        @Nullable Disposition disposition
) {
    public SocialConfig {
        goals          = goals != null ? List.copyOf(goals) : List.of();
        drives         = drives != null ? List.copyOf(drives) : List.of();
        norms          = norms != null ? List.copyOf(norms) : List.of();
        initialBeliefs = initialBeliefs != null ? List.copyOf(initialBeliefs) : List.of();
        relationships  = relationships != null ? List.copyOf(relationships) : List.of();
        reinforcement  = reinforcement != null ? Map.copyOf(reinforcement) : Map.of();
        if (stageConfig == null) {stageConfig = RelationshipStageConfig.defaults();}
        tendencies        = tendencies != null ? List.copyOf(tendencies) : List.of();
        personalityFacets = personalityFacets != null ? List.copyOf(personalityFacets) : List.of();
        relationalSchemas = relationalSchemas != null ? List.copyOf(relationalSchemas) : List.of();
        formationMemories = formationMemories != null ? List.copyOf(formationMemories) : List.of();
    }

    public SocialConfig(List<GoalConfig> goals, List<Drive> drives, List<NormEntry> norms,
                        List<InitialBelief> initialBeliefs, List<Relationship> relationships,
                        Map<String, List<ReinforcementMapping>> reinforcement,
                        RelationshipStageConfig stageConfig) {
        this(goals, drives, norms, initialBeliefs, relationships, reinforcement, stageConfig, null, List.of(),
             List.of(), List.of(), null, List.of(), null);
    }

    public static SocialConfig empty() {
        return new SocialConfig(List.of(), List.of(), List.of(), List.of(), List.of(), Map.of(), null, null, List.of(),
                                List.of(), List.of(), null, List.of(), null);
    }

    public record GoalConfig(String name, String description, String axis,
                             double intensity, String formationReason) {
        public GoalConfig {
            Objects.requireNonNull(name);
            Objects.requireNonNull(description);
            Objects.requireNonNull(axis);
            if (intensity < 0.0 || intensity > 1.0) {
                throw new IllegalArgumentException("intensity must be in [0,1], got " + intensity);
            }
        }
    }

    public record Drive(String type, double intensity, String description) {
        public Drive {
            Objects.requireNonNull(type);
            if (description == null) {description = "";}
            if (intensity < 0.0 || intensity > 1.0) {
                throw new IllegalArgumentException("intensity must be in [0,1], got " + intensity);
            }
        }
    }

    public record NormEntry(String rule, int priority) {
        public NormEntry {
            Objects.requireNonNull(rule);
        }
    }

    public record InitialBelief(String key, String value) {
        public InitialBelief {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);
        }
    }

    public record Relationship(String targetAgentId, double pleasure, double arousal, double dominance) {
        public Relationship {
            Objects.requireNonNull(targetAgentId, "targetAgentId required");
            if (pleasure < -1.0 || pleasure > 1.0) {
                throw new IllegalArgumentException("pleasure must be in [-1,1], got " + pleasure);
            }
            if (arousal < -1.0 || arousal > 1.0) {
                throw new IllegalArgumentException("arousal must be in [-1,1], got " + arousal);
            }
            if (dominance < -1.0 || dominance > 1.0) {
                throw new IllegalArgumentException("dominance must be in [-1,1], got " + dominance);
            }
        }
    }

    public record ReinforcementMapping(String drive, String direction, String rewardAxis) {
        public ReinforcementMapping {
            Objects.requireNonNull(drive);
            if (direction == null) {direction = "POSITIVE";}
            if (rewardAxis == null) {rewardAxis = "PLEASURE";}
        }
    }

    public record PersonaConstraintMapping(String constraintName, String whenActive, String whenInactive) {
        public PersonaConstraintMapping {
            Objects.requireNonNull(constraintName);
            Objects.requireNonNull(whenActive);
            Objects.requireNonNull(whenInactive);
        }
    }

    public record PersonalityFacet(String facet, int score, String origin) {
        public PersonalityFacet {
            Objects.requireNonNull(facet);
            if (score < 0 || score > 100) {
                throw new IllegalArgumentException("score must be in [0,100], got " + score);
            }
            if (origin == null) {origin = "";}
        }
    }

    public record RelationalSchema(String target, String role, double trust, double intimacy, double utility,
                                   String description) {
        public RelationalSchema {
            Objects.requireNonNull(target);
            Objects.requireNonNull(role);
            if (description == null) {description = "";}
        }
    }

    public record AttachmentStyle(String style, double anxiety, double avoidance) {
        public AttachmentStyle {
            Objects.requireNonNull(style);
            if (anxiety < 0.0 || anxiety > 1.0) {
                throw new IllegalArgumentException("anxiety must be in [0,1], got " + anxiety);
            }
            if (avoidance < 0.0 || avoidance > 1.0) {
                throw new IllegalArgumentException("avoidance must be in [0,1], got " + avoidance);
            }
        }
    }

    public record FormationMemory(int age, String episode, double pleasure, double arousal, double dominance) {
        public FormationMemory {
            Objects.requireNonNull(episode);
        }

        public FormationMemory(int age, String episode) {
            this(age, episode, 0.0, 0.0, 0.0);
        }
    }

    public record Disposition(
            String socialOrient,
            String ruleFollowing,
            String riskAppetite,
            String autonomy,
            String conflictMode
    ) {
        public static final Disposition NEUTRAL = new Disposition(
                "cooperative", "moderate", "calculated", "moderate", "cooperative");
    }


}
