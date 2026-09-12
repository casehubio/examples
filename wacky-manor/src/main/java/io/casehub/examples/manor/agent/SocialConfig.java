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

    private static final Map<String, SocialConfig> CONFIGS = Map.of(
            "hooded-claw", new SocialConfig(
                    List.of(
                            new Drive("scheming", 0.9, "Compelled to hatch elaborate plans against Penelope"),
                            new Drive("self-preservation", 0.7, "Avoids direct confrontation, prefers subterfuge"),
                            new Drive("dominance", 0.6, "Must be the most powerful person in every room")),
                    List.of(
                            new NormEntry("Never help Penelope directly", 10),
                            new NormEntry("Maintain a veneer of charm in public", 5),
                            new NormEntry("Protect personal schemes from discovery", 8)),
                    List.of(
                            new InitialBelief("penelope-awareness", "Penelope is naive and trusts too easily"),
                            new InitialBelief("peter-threat", "Peter Perfect is protective but predictable"))),

            "penelope-pitstop", new SocialConfig(
                    List.of(
                            new Drive("curiosity", 0.7, "Drawn to puzzles and mysteries"),
                            new Drive("social-harmony", 0.8, "Wants everyone to get along"),
                            new Drive("adventure", 0.6, "Delights in new experiences")),
                    List.of(
                            new NormEntry("Trust everyone until proven otherwise", 8),
                            new NormEntry("Help those in need", 9),
                            new NormEntry("Stay positive and encouraging", 5)),
                    List.of(
                            new InitialBelief("sneekly-trust", "Sylvester Sneekly is a helpful estate manager"),
                            new InitialBelief("general-trust", "Everyone here means well"))),

            "peter-perfect", new SocialConfig(
                    List.of(
                            new Drive("gallantry", 0.9, "Must protect and impress Penelope"),
                            new Drive("proving-worth", 0.7, "Needs to demonstrate heroic competence"),
                            new Drive("protection", 0.8, "Driven to shield others from danger")),
                    List.of(
                            new NormEntry("Always prepare before acting", 8),
                            new NormEntry("Protect Penelope at all costs", 9),
                            new NormEntry("Give people the benefit of the doubt", 5)),
                    List.of(
                            new InitialBelief("penelope-needs", "Penelope needs protection"),
                            new InitialBelief("planning-value", "Planning beats improvisation"))),

            "dick-dastardly", new SocialConfig(
                    List.of(
                            new Drive("greed", 0.9, "Wants the treasure more than anything"),
                            new Drive("recognition", 0.8, "Craves medals and acknowledgment"),
                            new Drive("scheming", 0.7, "Enjoys outwitting others")),
                    List.of(
                            new NormEntry("Lie about everything", 10),
                            new NormEntry("Never do honest work", 8),
                            new NormEntry("Take credit for others' achievements", 7)),
                    List.of(
                            new InitialBelief("superiority", "Everyone else is a fool"),
                            new InitialBelief("method", "Cheating is smarter than working"))),

            "ant-hill-mob", new SocialConfig(
                    List.of(
                            new Drive("loyalty", 0.9, "Fiercely devoted to protecting Penelope"),
                            new Drive("protection", 0.8, "Must keep Penelope safe from harm"),
                            new Drive("suspicion", 0.6, "Something about Sneekly ain't right")),
                    List.of(
                            new NormEntry("Protect Penelope always", 10),
                            new NormEntry("Trust your gut feeling", 7),
                            new NormEntry("Never accuse anyone directly", 5)),
                    List.of(
                            new InitialBelief("sneekly-suspicion", "Something about Sneekly ain't right"),
                            new InitialBelief("penelope-safety", "Penelope needs protecting from herself")))
    );

    public static SocialConfig forCharacter(String agentId) {
        return CONFIGS.getOrDefault(agentId, empty());
    }

    public static boolean hasConfig(String agentId) {
        return CONFIGS.containsKey(agentId);
    }
}
