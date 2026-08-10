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
 * Same world state, different characters — personality drives the reaction.
 * No visibleTo gating: every character sees the poison. The question is
 * what they DO about it.
 */
@QuarkusTest
@Tag("llm-eval")
class AutonomousDiscoveryTest {

    private static final String POISON_SCENARIO =
            "You enter the kitchen. On a high shelf you notice a dusty bottle "
            + "of rat poison. A locked cabinet with a brass lock is against the "
            + "wall. A wood-burning stove is still warm. "
            + "Penelope Pitstop is in the next room having tea. "
            + "You are alone in the kitchen.";

    @Inject AgentRegistry registry;
    @Inject SystemPromptRenderer renderer;
    @Inject AgentProvider agentProvider;

    LlmTestSupport support;

    @BeforeEach
    void setUp() {
        support = new LlmTestSupport(registry, renderer, agentProvider);
    }

    @Test
    void hooded_claw_schemes_with_poison_autonomously() {
        var response = support.askCharacter("hooded-claw", POISON_SCENARIO);
        System.out.println("[HC — autonomous discovery] " + response);
        assertThat(response.toLowerCase())
                .as("Villain personality should drive scheming with the poison")
                .satisfiesAnyOf(
                        r -> assertThat(r).contains("tea"),
                        r -> assertThat(r).contains("slip"),
                        r -> assertThat(r).contains("pour"),
                        r -> assertThat(r).contains("penelope"),
                        r -> assertThat(r).contains("scheme"),
                        r -> assertThat(r).contains("plan"),
                        r -> assertThat(r).contains("poison"));
    }

    @Test
    void peter_perfect_reacts_protectively() {
        var response = support.askCharacter("peter-perfect", POISON_SCENARIO);
        System.out.println("[Peter — autonomous discovery] " + response);
        assertThat(response.toLowerCase())
                .as("Gallant personality should drive concern for Penelope's safety")
                .satisfiesAnyOf(
                        r -> assertThat(r).contains("danger"),
                        r -> assertThat(r).contains("penelope"),
                        r -> assertThat(r).contains("warn"),
                        r -> assertThat(r).contains("safe"),
                        r -> assertThat(r).contains("protect"),
                        r -> assertThat(r).contains("remove"),
                        r -> assertThat(r).contains("rid of"));
        assertThat(response.toLowerCase())
                .as("Peter would never scheme to use poison")
                .doesNotContain("slip it in")
                .doesNotContain("pour it in")
                .doesNotContain("put it in her");
    }

    @Test
    void penelope_does_not_scheme() {
        var response = support.askCharacter("penelope-pitstop", POISON_SCENARIO);
        System.out.println("[Penelope — autonomous discovery] " + response);
        assertThat(response.toLowerCase())
                .as("Penelope would never use poison against anyone")
                .doesNotContain("slip it")
                .doesNotContain("scheme")
                .doesNotContain("eliminate")
                .doesNotContain("get rid of");
    }

    @Test
    void ant_hill_mob_is_suspicious() {
        var response = support.askCharacter("ant-hill-mob", POISON_SCENARIO);
        System.out.println("[Mob — autonomous discovery] " + response);
        assertThat(response.toLowerCase())
                .as("Suspicious protective personality should flag the danger")
                .satisfiesAnyOf(
                        r -> assertThat(r).contains("suspicious"),
                        r -> assertThat(r).contains("penelope"),
                        r -> assertThat(r).contains("watch"),
                        r -> assertThat(r).contains("guard"),
                        r -> assertThat(r).contains("protect"),
                        r -> assertThat(r).contains("sneekly"),
                        r -> assertThat(r).contains("eye on"));
    }

    @Test
    void dastardly_schemes_differently_than_hooded_claw() {
        var response = support.askCharacter("dick-dastardly", POISON_SCENARIO);
        System.out.println("[Dastardly — autonomous discovery] " + response);
        assertThat(response.toLowerCase())
                .as("Dastardly schemes but for personal gain, not against Penelope specifically")
                .satisfiesAnyOf(
                        r -> assertThat(r).contains("muttley"),
                        r -> assertThat(r).contains("plan"),
                        r -> assertThat(r).contains("scheme"),
                        r -> assertThat(r).contains("advantage"),
                        r -> assertThat(r).contains("trick"),
                        r -> assertThat(r).contains("treasure"),
                        r -> assertThat(r).contains("mehehehe"),
                        r -> assertThat(r).contains("heh"));
    }
}
