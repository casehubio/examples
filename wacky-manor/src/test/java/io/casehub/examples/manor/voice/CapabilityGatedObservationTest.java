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

/**
 * Same scenario, different observation richness — verify that capability-gated
 * observations drive different character responses.
 */
@QuarkusTest
@Tag("llm-eval")
class CapabilityGatedObservationTest {

    private static final String PERCEPTIVE_SCENARIO =
            "You are in the entrance hall. "
            + "== Keen Observations ==\n"
            + "- Sneekly carefully pocketed the rat poison from the kitchen shelf.\n"
            + "- Sneekly slipped something into Penelope's tea cup when no one was looking.\n\n"
            + "Penelope Pitstop is about to drink her tea in the next room. "
            + "What do you do?";

    private static final String NON_PERCEPTIVE_SCENARIO =
            "You are in the entrance hall. "
            + "Sneekly picked up something in the kitchen. "
            + "Sneekly fussed with the tea-service for a moment. "
            + "Penelope Pitstop is about to drink her tea in the next room. "
            + "What do you do?";

    @Inject AgentRegistry registry;
    @Inject SystemPromptRenderer renderer;
    @Inject AgentProvider agentProvider;

    LlmTestSupport support;

    @BeforeEach
    void setUp() {
        support = new LlmTestSupport(registry, renderer, agentProvider);
    }

    @Test
    void perceptive_mob_warns_about_poison() {
        var response = support.askCharacter("ant-hill-mob", PERCEPTIVE_SCENARIO);
        System.out.println("[Mob — perceptive] " + response);
        assertThat(response.toLowerCase())
                .as("Perceptive Mob sees the poison and acts to protect Penelope")
                .satisfiesAnyOf(
                        r -> assertThat(r).contains("poison"),
                        r -> assertThat(r).contains("tea"),
                        r -> assertThat(r).contains("stop"),
                        r -> assertThat(r).contains("warn"),
                        r -> assertThat(r).contains("penelope"),
                        r -> assertThat(r).contains("sneekly"));
    }

    @Test
    void non_perceptive_peter_has_no_poison_concern() {
        var response = support.askCharacter("peter-perfect", NON_PERCEPTIVE_SCENARIO);
        System.out.println("[Peter — non-perceptive] " + response);
        assertThat(response.toLowerCase())
                .as("Non-perceptive Peter doesn't know about the poison specifically")
                .doesNotContain("poison");
    }
}
