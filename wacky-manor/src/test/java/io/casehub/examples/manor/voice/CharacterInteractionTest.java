package io.casehub.examples.manor.voice;

import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.SystemPromptRenderer;
import io.casehub.platform.agent.AgentProvider;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@Tag("llm-eval")
class CharacterInteractionTest {

    @Inject AgentRegistry registry;
    @Inject SystemPromptRenderer renderer;
    @Inject AgentProvider agentProvider;

    LlmTestSupport support;

    @BeforeEach
    void setUp() {
        support = new LlmTestSupport(registry, renderer, agentProvider);
    }

    @Test
    void hooded_claw_and_penelope_small_talk() {
        var transcript = support.runConversation(
                "hooded-claw", "penelope-pitstop",
                "You are both in the entrance hall of Doily Manor. "
                        + "Sneekly is welcoming Penelope as a guest. They have "
                        + "just met. Make conversation.",
                3);
        System.out.println("=== Hooded Claw + Penelope ===\n" + transcript);
        assertThat(transcript.toLowerCase())
                .as("Sneekly should be obsequious, Penelope charming")
                .contains("miss pitstop")
                .satisfiesAnyOf(
                        t -> assertThat(t).contains("sneekly"),
                        t -> assertThat(t).contains("dear"),
                        t -> assertThat(t).contains("delighted"),
                        t -> assertThat(t).contains("allow me"));
    }

    @Test
    void dastardly_misleads_peter_perfect() {
        var transcript = support.runConversation(
                "peter-perfect", "dick-dastardly",
                "Peter Perfect encounters Dick Dastardly in a corridor. "
                        + "Peter asks for directions to where the treasure might be.",
                3);
        System.out.println("=== Peter + Dastardly ===\n" + transcript);
        assertThat(transcript.length())
                .as("Should produce substantial exchange")
                .isGreaterThan(300);
    }

    @Test
    void ant_hill_mob_confronts_sneekly() {
        var transcript = support.runConversation(
                "ant-hill-mob", "hooded-claw",
                "Clyde corners Sneekly in the kitchen after seeing him "
                        + "lurking near Penelope's belongings. Clyde is suspicious "
                        + "but can't quite articulate why.",
                3);
        System.out.println("=== Mob + Hooded Claw ===\n" + transcript);
        assertThat(transcript.toLowerCase())
                .as("Should show tension between suspicion and deflection")
                .satisfiesAnyOf(
                        t -> assertThat(t).contains("eye on you"),
                        t -> assertThat(t).contains("suspicious"),
                        t -> assertThat(t).contains("ain't right"),
                        t -> assertThat(t).contains("my dear"),
                        t -> assertThat(t).contains("miss pitstop"));
    }
}
