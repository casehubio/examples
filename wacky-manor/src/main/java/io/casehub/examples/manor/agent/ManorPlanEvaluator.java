package io.casehub.examples.manor.agent;

import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.RetrievedMemory;
import io.casehub.api.model.TaskStatus;
import io.casehub.eidos.api.AgentGoal;
import io.casehub.engine.plan.adaptation.AdaptationCause;
import io.casehub.engine.plan.adaptation.AdaptationContext;
import io.casehub.engine.plan.adaptation.CompletedStep;
import io.casehub.engine.plan.adaptation.PlanStepDescriptor;
import io.casehub.engine.plan.adaptation.RevisionContext;
import io.casehub.examples.manor.model.ActionResult;
import io.casehub.examples.manor.model.AgentPlan;
import io.casehub.examples.manor.model.CharacterState;
import io.casehub.examples.manor.model.PlanStep;
import io.casehub.examples.manor.model.PlanStepStatus;
import io.casehub.neocortex.memory.CaseMemoryStore;
import io.casehub.neocortex.memory.MemoryDomain;
import io.casehub.neocortex.memory.MemoryOrder;
import io.casehub.neocortex.memory.MemoryQuery;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class ManorPlanEvaluator {

    private static final Logger log = Logger.getLogger(ManorPlanEvaluator.class);
    private static final CaseDefinition DUMMY_DEFINITION = new CaseDefinition("manor", "wacky-manor", "1.0");

    private final ManorPlanFormationStrategy formationStrategy;
    private final ManorPlanRevisionStrategy revisionStrategy;
    private final CaseMemoryStore memoryStore;
    private final String tenancyId;
    private final Function<String, CharacterState> characterLookup;
    private final int maxRevisionGeneration;

    public ManorPlanEvaluator(ManorPlanFormationStrategy formationStrategy,
                               ManorPlanRevisionStrategy revisionStrategy,
                               CaseMemoryStore memoryStore,
                               String tenancyId,
                               Function<String, CharacterState> characterLookup,
                               int maxRevisionGeneration) {
        this.formationStrategy = formationStrategy;
        this.revisionStrategy = revisionStrategy;
        this.memoryStore = memoryStore;
        this.tenancyId = tenancyId;
        this.characterLookup = characterLookup;
        this.maxRevisionGeneration = maxRevisionGeneration;
    }

    public void formPlanForGoal(String agentId, AgentGoal goal,
                                 List<AgentGoal> allGoals, int currentTick) {
        try {
            List<RetrievedMemory> memories = retrieveMemories(agentId);
            AgentPlan plan = formationStrategy.formPlan(
                    agentId, tenancyId, goal, allGoals, memories, currentTick);
            if (plan != null && !plan.steps().isEmpty()) {
                characterLookup.apply(agentId).setPlan(goal.name(), plan);
            }
        } catch (Exception e) {
            log.warnf(e, "Plan formation failed for agent %s goal %s", agentId, goal.name());
        }
    }

    public void removePlanForGoal(String agentId, String goalName) {
        characterLookup.apply(agentId).removePlan(goalName);
    }

    public void reviseOnFailure(String agentId, String actionType, String actionTarget,
                                ActionResult.Failed failure, int currentTick) {
        var character = characterLookup.apply(agentId);
        if (character.plans().isEmpty()) return;

        var cause = new AdaptationCause.StepFailed(
                actionType + ":" + actionTarget, failure.reason());
        List<RetrievedMemory> memories = retrieveMemories(agentId);

        for (Map.Entry<String, AgentPlan> entry : character.plans().entrySet()) {
            var plan = entry.getValue();
            if (plan.revisionGeneration() >= maxRevisionGeneration) continue;
            revisePlan(character, entry.getKey(), plan, cause, memories, currentTick);
        }
    }

    public void reviseOnReflection(String agentId, List<String> insights, int currentTick) {
        var character = characterLookup.apply(agentId);
        if (character.plans().isEmpty()) return;

        List<RetrievedMemory> memories = retrieveMemories(agentId);

        for (Map.Entry<String, AgentPlan> entry : character.plans().entrySet()) {
            var plan = entry.getValue();
            if (plan.revisionGeneration() >= maxRevisionGeneration) continue;
            var cause = new AdaptationCause.StepCompleted(
                    "reflection", "", Map.of("insights", insights));
            revisePlan(character, entry.getKey(), plan, cause, memories, currentTick);
        }
    }

    private void revisePlan(CharacterState character, String goalName, AgentPlan plan,
                             AdaptationCause cause, List<RetrievedMemory> memories, int currentTick) {
        try {
            List<CompletedStep> completed = plan.steps().stream()
                    .filter(s -> s.status() == PlanStepStatus.COMPLETED)
                    .map(s -> new CompletedStep(s.id(), "", s.description(), Map.of(), null))
                    .toList();
            List<PlanStepDescriptor> pending = plan.steps().stream()
                    .filter(s -> s.status() != PlanStepStatus.COMPLETED)
                    .map(s -> new PlanStepDescriptor(s.id(), s.description(), ""))
                    .toList();

            var adaptCtx = new AdaptationContext(UUID.randomUUID(), tenancyId, "",
                    goalName, completed, pending, List.of(),
                    null, DUMMY_DEFINITION, TaskStatus.COMPLETED, "", plan.revisionGeneration());
            var ctx = new RevisionContext(adaptCtx, cause, List.of(), memories);

            var revised = revisionStrategy.revise(ctx);
            if (revised != null && !revised.steps().isEmpty()) {
                List<PlanStep> newSteps = revised.steps().stream()
                        .map(s -> new PlanStep(s.id(), s.description(), PlanStepStatus.PENDING))
                        .toList();
                character.setPlan(goalName, plan.withRevision(newSteps, revised.rationale(), currentTick));
            }
        } catch (Exception e) {
            log.warnf(e, "Plan revision failed for goal %s", goalName);
        }
    }

    private List<RetrievedMemory> retrieveMemories(String agentId) {
        try {
            var memories = memoryStore.query(MemoryQuery.forEntity(agentId,
                    new MemoryDomain("manor"), tenancyId)
                    .withLimit(10).withOrder(MemoryOrder.SALIENCE));
            return memories.stream()
                    .map(m -> new RetrievedMemory(m.memoryId(), m.text(),
                            m.domain().name(), m.createdAt(), m.attributes()))
                    .toList();
        } catch (Exception e) {
            log.debugf("Failed to retrieve memories for %s: %s", agentId, e.getMessage());
            return List.of();
        }
    }
}
