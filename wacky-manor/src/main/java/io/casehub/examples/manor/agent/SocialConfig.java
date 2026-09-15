package io.casehub.examples.manor.agent;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record SocialConfig(
        List<Drive> drives,
        List<NormEntry> norms,
        List<InitialBelief> initialBeliefs
) {
    public SocialConfig {
        drives = drives != null ? List.copyOf(drives) : List.of();
        norms = norms != null ? List.copyOf(norms) : List.of();
        initialBeliefs = initialBeliefs != null ? List.copyOf(initialBeliefs) : List.of();
    }

    public static SocialConfig empty() {
        return new SocialConfig(List.of(), List.of(), List.of());
    }

    public record Drive(String type, double intensity, String description) {
        public Drive {
            Objects.requireNonNull(type);
            Objects.requireNonNull(description);
            if (intensity < 0.0 || intensity > 1.0)
                throw new IllegalArgumentException("intensity must be in [0,1], got " + intensity);
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

}
