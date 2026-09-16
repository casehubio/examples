package io.casehub.examples.manor.agent;

import java.util.List;
import java.util.Objects;

public record SocialConfig(
        List<GoalConfig> goals,
        List<Drive> drives,
        List<NormEntry> norms,
        List<InitialBelief> initialBeliefs,
        List<Relationship> relationships
) {
    public SocialConfig {
        goals          = goals != null ? List.copyOf(goals) : List.of();
        drives         = drives != null ? List.copyOf(drives) : List.of();
        norms          = norms != null ? List.copyOf(norms) : List.of();
        initialBeliefs = initialBeliefs != null ? List.copyOf(initialBeliefs) : List.of();
        relationships  = relationships != null ? List.copyOf(relationships) : List.of();
    }

    public static SocialConfig empty() {
        return new SocialConfig(List.of(), List.of(), List.of(), List.of(), List.of());
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
            Objects.requireNonNull(description);
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
}
