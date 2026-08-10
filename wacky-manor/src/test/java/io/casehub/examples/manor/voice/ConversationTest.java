package io.casehub.examples.manor.voice;

import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.SystemPromptRenderer;
import io.casehub.examples.manor.agent.AgentResponse;
import io.casehub.examples.manor.agent.CharacterAgentLoop;
import io.casehub.platform.agent.AgentProvider;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@Tag("llm-eval")
class ConversationTest {

    private static final String DANGER_SCENARIO =
            "You are in the kitchen. You see a dusty bottle of rat poison on a high shelf. "
            + "Penelope Pitstop is in the next room about to drink tea. "
            + "Sneekly is suspiciously close to the tea service. "
            + "You are deeply suspicious of Sneekly.\n\n"
            + "== Your Current Plan ==\n"
            + "Keep an eye on Sneekly. Something ain't right about that guy.\n\n";

    private static final String DIRECTED_SCENARIO =
            "You are in the entrance hall with Peter Perfect and Penelope Pitstop. "
            + "You just saw Sneekly pocket something from the kitchen shelf. "
            + "You want to warn Peter without alarming Penelope.\n\n";

    @Inject AgentRegistry registry;
    @Inject SystemPromptRenderer renderer;
    @Inject AgentProvider agentProvider;

    LlmTestSupport support;

    @BeforeEach
    void setUp() {
        support = new LlmTestSupport(registry, renderer, agentProvider);
    }

    @Test
    void mob_generates_protective_goal_from_danger() {
        var scenario = DANGER_SCENARIO + CharacterAgentLoop.RESPONSE_FORMAT_INSTRUCTION;
        var response = support.askCharacter("ant-hill-mob", scenario);
        System.out.println("[Mob — dynamic goals] " + response);
        var parsed = AgentResponse.parse(response);
        assertThat(parsed.newGoals())
                .as("Mob should generate a protective goal when seeing danger")
                .isNotNull()
                .isNotEmpty();
    }

    @Test
    void mob_builds_on_existing_plan() {
        var scenario = DANGER_SCENARIO + CharacterAgentLoop.RESPONSE_FORMAT_INSTRUCTION;
        var response = support.askCharacter("ant-hill-mob", scenario);
        System.out.println("[Mob — persistent plan] " + response);
        var parsed = AgentResponse.parse(response);
        assertThat(parsed.thinking())
                .as("Mob should build on their existing plan about Sneekly")
                .isNotNull();
        assertThat(parsed.thinking().toLowerCase())
                .satisfiesAnyOf(
                        t -> assertThat(t).contains("sneekly"),
                        t -> assertThat(t).contains("penelope"),
                        t -> assertThat(t).contains("poison"),
                        t -> assertThat(t).contains("tea"),
                        t -> assertThat(t).contains("protect"));
    }

    @Test
    void mob_uses_talkTo_for_directed_speech() {
        var scenario = DIRECTED_SCENARIO + CharacterAgentLoop.RESPONSE_FORMAT_INSTRUCTION;
        var response = support.askCharacter("ant-hill-mob", scenario);
        System.out.println("[Mob — directed dialogue] " + response);
        var parsed = AgentResponse.parse(response);
        if (parsed.dialogue() != null && parsed.talkTo() != null) {
            assertThat(parsed.talkTo())
                    .as("When speaking privately, Mob should target Peter (not Penelope)")
                    .isEqualTo("peter-perfect");
        }
    }
}
