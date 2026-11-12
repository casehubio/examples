package io.casehub.examples.manor.agent;

import io.casehub.neocortex.memory.CaseMemoryStore;
import io.casehub.neocortex.memory.experience.ExperienceRecorder;

public record ExperienceConfig(
    ExperienceRecorder recorder,
    CaseMemoryStore store,
    String tenantId,
    io.casehub.neocortex.memory.reflection.ReflectionSynthesizer synthesizer,
    ManorReflectionTrigger reflectionTrigger,
    boolean reflectionEnabled,
    boolean decayEnabled,
    int decayMaxAgeDays,
    double decayMinImportance,
    int maxSourceMemories,
    int recallLimit,
    ManorGoalEvaluator goalEvaluator,
    ManorPlanEvaluator planEvaluator
) {
    public static ExperienceConfig minimal(ExperienceRecorder recorder, CaseMemoryStore store, String tenantId) {
        return new ExperienceConfig(recorder, store, tenantId, null, null, false, false, 7, 0.2, 15, 20, null, null);
    }
}
