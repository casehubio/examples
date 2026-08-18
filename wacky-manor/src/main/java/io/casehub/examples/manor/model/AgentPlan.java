package io.casehub.examples.manor.model;

import java.util.List;

public record AgentPlan(
        String goalName,
        List<PlanStep> steps,
        String rationale,
        int creationTick,
        int lastRevisionTick,
        int revisionGeneration) {

    public AgentPlan withSteps(List<PlanStep> newSteps) {
        return new AgentPlan(goalName, newSteps, rationale,
                creationTick, lastRevisionTick, revisionGeneration);
    }

    public AgentPlan withRevision(List<PlanStep> newSteps, String newRationale, int tick) {
        return new AgentPlan(goalName, newSteps, newRationale,
                creationTick, tick, revisionGeneration + 1);
    }
}
