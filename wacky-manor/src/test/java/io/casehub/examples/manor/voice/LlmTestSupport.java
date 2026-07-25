package io.casehub.examples.manor.voice;

import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.eidos.api.SystemPromptRenderer;
import io.casehub.examples.manor.ManorConstants;
import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSessionConfig;

import java.time.Duration;
import java.util.stream.Collectors;

final class LlmTestSupport {

    private final AgentRegistry        registry;
    private final SystemPromptRenderer renderer;
    private final AgentProvider        agentProvider;

    LlmTestSupport(AgentRegistry registry, SystemPromptRenderer renderer,
                   AgentProvider agentProvider) {
        this.registry      = registry;
        this.renderer      = renderer;
        this.agentProvider = agentProvider;
    }

    String askCharacter(String agentId, String scenario) {
        var systemPrompt = renderPrompt(agentId);
        var config       = AgentSessionConfig.of(systemPrompt, scenario);
        return agentProvider.invoke(config)
                            .filter(e -> e instanceof AgentEvent.TextDelta)
                            .map(e -> ((AgentEvent.TextDelta) e).text())
                            .collect().with(Collectors.joining())
                            .await().atMost(Duration.ofSeconds(120));
    }

    String runConversation(String agentIdA, String agentIdB,
                           String initialScenario, int turns) {
        var promptA = renderPrompt(agentIdA);
        var promptB = renderPrompt(agentIdB);
        var nameA = registry.findById(agentIdA, ManorConstants.TENANCY_ID)
                            .orElseThrow().name();
        var nameB = registry.findById(agentIdB, ManorConstants.TENANCY_ID)
                            .orElseThrow().name();

        var transcript = new StringBuilder();
        var context    = initialScenario;

        for (int turn = 0; turn < turns; turn++) {
            var responseA = agentProvider.invoke(AgentSessionConfig.of(promptA, context))
                                         .filter(e -> e instanceof AgentEvent.TextDelta)
                                         .map(e -> ((AgentEvent.TextDelta) e).text())
                                         .collect().with(Collectors.joining())
                                         .await().atMost(Duration.ofSeconds(120));
            context += "\n" + nameA + ": " + responseA;
            transcript.append(nameA).append(": ").append(responseA).append("\n\n");

            var responseB = agentProvider.invoke(AgentSessionConfig.of(promptB, context))
                                         .filter(e -> e instanceof AgentEvent.TextDelta)
                                         .map(e -> ((AgentEvent.TextDelta) e).text())
                                         .collect().with(Collectors.joining())
                                         .await().atMost(Duration.ofSeconds(120));
            context += "\n" + nameB + ": " + responseB;
            transcript.append(nameB).append(": ").append(responseB).append("\n\n");
        }

        return transcript.toString();
    }

    private String renderPrompt(String agentId) {
        var desc = registry.findById(agentId, ManorConstants.TENANCY_ID)
                           .orElseThrow(() -> new IllegalArgumentException("No descriptor for " + agentId));
        var ctx = AgentPromptContext.forFormat(RenderFormat.MARKDOWN);
        return renderer.render(desc, ctx).content();
    }
}
