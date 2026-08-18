package io.casehub.examples.manor.model;

public record PlanStep(String id, String description, PlanStepStatus status) {

    public PlanStep withStatus(PlanStepStatus newStatus) {
        return new PlanStep(id, description, newStatus);
    }
}
