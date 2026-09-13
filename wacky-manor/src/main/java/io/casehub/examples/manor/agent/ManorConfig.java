package io.casehub.examples.manor.agent;

public record ManorConfig(
        int maxTurns,
        ObservationConfig observation,
        NarratorConfig narrator,
        ReflectionConfig reflection,
        GoalConfig goal,
        PlanConfig plan,
        TrustConfig trust,
        DispositionConfig disposition,
        MemoryConfig memory,
        ConsolidationConfig consolidation,
        String activeCharacters,
        int maxConcurrentAgents
) {
    public record ObservationConfig(int verbatimThreshold, int groupedThreshold) {}

    public record NarratorConfig(boolean enabled, int eventThreshold, int timerSeconds) {}

    public record ReflectionConfig(boolean enabled, int maxUnreflected, double importanceThreshold,
                                   int maxSourceMemories) {}

    public record GoalConfig(boolean enabled, int cooldownTicks, int maxNewPerReflection) {}

    public record PlanConfig(boolean enabled, int maxRevisionGeneration) {}

    public record TrustConfig(boolean enabled, double positiveWeight, double negativeWeight) {}

    public record DispositionConfig(boolean enabled, int evolutionCheckInterval) {}

    public record MemoryConfig(int recallLimit, boolean personalityWeightedRetrieval, boolean decayEnabled,
                               int decayMaxAgeDays, double decayMinImportance) {}

    public record ConsolidationConfig(boolean enabled, int intervalTicks) {}
}
