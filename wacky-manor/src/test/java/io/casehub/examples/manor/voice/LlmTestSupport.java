package io.casehub.examples.manor.voice;

import io.casehub.examples.manor.CharacterProfile;
import io.casehub.examples.manor.CharacterProfileLoader;
import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSessionConfig;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

final class LlmTestSupport {

    private final Map<String, CharacterProfile> profiles;
    private final AgentProvider                 agentProvider;

    LlmTestSupport(AgentProvider agentProvider) {
        this.profiles      = CharacterProfileLoader.load();
        this.agentProvider = agentProvider;
    }

    String askCharacter(String agentId, String scenario) {
        var profile = profiles.get(agentId);
        if (profile == null) {
            throw new IllegalArgumentException("No profile for " + agentId);
        }
        var systemPrompt = profile.buildSystemPrompt();
        var config       = AgentSessionConfig.of(systemPrompt, scenario);
        return agentProvider.invoke(config)
                            .filter(e -> e instanceof AgentEvent.TextDelta)
                            .map(e -> ((AgentEvent.TextDelta) e).text())
                            .collect().with(Collectors.joining())
                            .await().atMost(Duration.ofSeconds(120));
    }

    String runConversation(String agentIdA, String agentIdB,
                           String initialScenario, int turns) {
        var profileA = profiles.get(agentIdA);
        var profileB = profiles.get(agentIdB);
        if (profileA == null || profileB == null) {
            throw new IllegalArgumentException("Profile not found");
        }
        var promptA = profileA.buildSystemPrompt();
        var promptB = profileB.buildSystemPrompt();

        var transcript = new StringBuilder();
        var context    = initialScenario;

        for (int turn = 0; turn < turns; turn++) {
            var responseA = agentProvider.invoke(AgentSessionConfig.of(promptA, context))
                                         .filter(e -> e instanceof AgentEvent.TextDelta)
                                         .map(e -> ((AgentEvent.TextDelta) e).text())
                                         .collect().with(Collectors.joining())
                                         .await().atMost(Duration.ofSeconds(120));
            context += "\n" + profileA.name() + ": " + responseA;
            transcript.append(profileA.name()).append(": ")
                      .append(responseA).append("\n\n");

            var responseB = agentProvider.invoke(AgentSessionConfig.of(promptB, context))
                                         .filter(e -> e instanceof AgentEvent.TextDelta)
                                         .map(e -> ((AgentEvent.TextDelta) e).text())
                                         .collect().with(Collectors.joining())
                                         .await().atMost(Duration.ofSeconds(120));
            context += "\n" + profileB.name() + ": " + responseB;
            transcript.append(profileB.name()).append(": ")
                      .append(responseB).append("\n\n");
        }

        return transcript.toString();
    }
}
