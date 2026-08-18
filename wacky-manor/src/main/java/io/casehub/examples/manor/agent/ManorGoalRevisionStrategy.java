package io.casehub.examples.manor.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.api.spi.routing.GoalRevisionAction;
import io.casehub.api.spi.routing.GoalRevisionContext;
import io.casehub.api.spi.routing.GoalRevisionProposal;
import io.casehub.api.spi.routing.GoalRevisionStrategy;
import io.casehub.eidos.api.AgentGoal;
import io.casehub.eidos.api.GoalOutcomeCounts;
import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSessionConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class ManorGoalRevisionStrategy implements GoalRevisionStrategy {

    private static final Logger log = Logger.getLogger(ManorGoalRevisionStrategy.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
        You are a goal effectiveness analyst for an autonomous agent. Given the \
        agent's goals and their performance metrics, evaluate each goal and \
        recommend an action:
        - REVISE: refine the goal description to better capture what the agent \
          should accomplish (provide revisedDescription)
        - ABANDON: drop the goal — it is unachievable or no longer relevant
        - COMPLETE: the goal has been achieved
        Only act on goals with clear signals. If a goal is fine as-is, omit it \
        from revisions.
        Return ONLY a JSON object: {"revisions": [{"goalName": "...", \
        "action": "REVISE"|"ABANDON"|"COMPLETE", \
        "revisedDescription": "..."|null, "revisionReason": "..."}], \
        "rationale": "..."}""";

    private final AgentProvider agentProvider;

    @Inject
    public ManorGoalRevisionStrategy(AgentProvider agentProvider) {
        this.agentProvider = agentProvider;
    }

    @Override
    public String id() { return "manor-llm"; }

    @Override
    public GoalRevisionProposal revise(GoalRevisionContext context) {
        try {
            String userPrompt = buildPrompt(context);
            String response = agentProvider.invoke(
                    AgentSessionConfig.of(SYSTEM_PROMPT, userPrompt))
                .filter(e -> e instanceof AgentEvent.TextDelta)
                .map(e -> ((AgentEvent.TextDelta) e).text())
                .collect().with(Collectors.joining())
                .await().atMost(Duration.ofSeconds(120));
            return parseResponse(response);
        } catch (Exception e) {
            log.warnf("Goal revision failed (non-fatal): %s", e.getMessage());
            return new GoalRevisionProposal(List.of(), "");
        }
    }

    private String buildPrompt(GoalRevisionContext context) {
        var sb = new StringBuilder();
        sb.append("Agent: ").append(context.agentId()).append("\n\nGoals:\n");
        for (AgentGoal goal : context.goals()) {
            GoalOutcomeCounts counts = context.counts().get(goal.name());
            sb.append("- ").append(goal.name()).append(": ").append(goal.description());
            sb.append(" (priority: ").append(goal.priority());
            if (counts != null) {
                sb.append(String.format(", success: %d, failure: %d, rate: %.0f%%",
                        counts.successCount(), counts.failureCount(), counts.successRate() * 100));
            }
            sb.append(")\n");
        }
        sb.append("\nRespond with JSON only.");
        return sb.toString();
    }

    private GoalRevisionProposal parseResponse(String response) {
        try {
            JsonNode root = JSON.readTree(response);
            JsonNode revisionsNode = root.get("revisions");
            String rationale = root.has("rationale") ? root.get("rationale").asText() : "";
            List<GoalRevisionProposal.RevisedGoal> revisions = new ArrayList<>();
            if (revisionsNode != null && revisionsNode.isArray()) {
                for (JsonNode node : revisionsNode) {
                    String goalName = node.get("goalName").asText();
                    GoalRevisionAction action = GoalRevisionAction.valueOf(
                            node.get("action").asText());
                    String desc = node.has("revisedDescription") && !node.get("revisedDescription").isNull()
                            ? node.get("revisedDescription").asText() : null;
                    String reason = node.get("revisionReason").asText();
                    revisions.add(new GoalRevisionProposal.RevisedGoal(
                            goalName, action, desc, reason));
                }
            }
            return new GoalRevisionProposal(revisions, rationale);
        } catch (Exception e) {
            log.warnf("Failed to parse goal revision response: %s", e.getMessage());
            return new GoalRevisionProposal(List.of(), "");
        }
    }
}
