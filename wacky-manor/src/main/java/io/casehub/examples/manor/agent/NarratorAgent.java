package io.casehub.examples.manor.agent;

import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSessionConfig;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.stream.Collectors;

public final class NarratorAgent {

    private static final Logger log = Logger.getLogger(NarratorAgent.class);

    private static final String NARRATOR_SYSTEM_PROMPT = """
        You are the narrator of a Wacky Races cartoon special set in a haunted mansion.
        Your style is breathless, alliterative, dramatic, and omniscient — like the
        original Wacky Races narrator.

        Rules:
        - Use CAPITAL LETTERS for dramatic emphasis
        - Be alliterative when possible
        - Use exclamation marks liberally
        - You see everything and know everyone's secrets
        - Address the audience directly
        - Keep each narration to 2-3 sentences maximum

        Example: "And so our heroes GATHER in the dusty entrance of Doily Manor,
        UTTERLY UNAWARE that DANGER lurks behind every cobweb! The Hooded Claw
        adjusts his disguise and flashes a smile SO sinister it could curdle MILK!"
        """;

    public static String narrate(String event, AgentProvider agentProvider) {
        try {
            return agentProvider.invoke(
                    AgentSessionConfig.of(NARRATOR_SYSTEM_PROMPT, event,
                        Duration.ofSeconds(30)))
                .filter(e -> e instanceof AgentEvent.TextDelta)
                .map(e -> ((AgentEvent.TextDelta) e).text())
                .collect().with(Collectors.joining())
                .await().atMost(Duration.ofSeconds(60));
        } catch (Exception e) {
            log.warnf("Narrator failed: %s", e.getMessage());
            return event;
        }
    }
}
