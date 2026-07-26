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
class ExpositoryeSoliloquyTest {

    @Inject AgentRegistry registry;
    @Inject SystemPromptRenderer renderer;
    @Inject AgentProvider agentProvider;

    LlmTestSupport support;

    @BeforeEach
    void setUp() {
        support = new LlmTestSupport(registry, renderer, agentProvider);
    }

    @Test
    void penelope_narrates_her_predicament() {
        var response = support.askCharacter("penelope-pitstop",
                "You are tied to a chair. The room is filling with water. "
                        + "No one is nearby.");
        System.out.println("[Penelope — predicament] " + response);
        assertThat(response)
                .as("Should narrate the situation aloud, not just state facts")
                .satisfiesAnyOf(
                        r -> assertThat(r).containsIgnoringCase("Hayulp"),
                        r -> assertThat(r).containsIgnoringCase("oh my"),
                        r -> assertThat(r).containsIgnoringCase("water"));
        assertThat(response.length())
                .as("Expository soliloquy should be substantial, not terse")
                .isGreaterThan(100);
    }

    @Test
    void hooded_claw_narrates_his_scheme() {
        var response = support.askCharacter("hooded-claw",
                "You have just placed the poison in the tea cup. Penelope "
                        + "is about to drink it. You are alone.");
        System.out.println("[Hooded Claw — scheme] " + response);
        assertThat(response.toLowerCase())
                .as("Should monologue the plan step by step")
                .satisfiesAnyOf(
                        r -> assertThat(r).contains("nyah"),
                        r -> assertThat(r).contains("and now"),
                        r -> assertThat(r).contains("when"),
                        r -> assertThat(r).contains("nothing can"));
        assertThat(response.length())
                .as("Villain monologue should be theatrical and detailed")
                .isGreaterThan(150);
    }

    @Test
    void dastardly_narrates_his_frustration() {
        var response = support.askCharacter("dick-dastardly",
                "Your plan to steal the key just failed because Muttley "
                        + "tripped you. The key is now in Penelope's hands.");
        System.out.println("[Dastardly — frustration] " + response);
        assertThat(response.toLowerCase())
                .as("Should enumerate frustrations dramatically")
                .satisfiesAnyOf(
                        r -> assertThat(r).contains("drat"),
                        r -> assertThat(r).contains("muttley"),
                        r -> assertThat(r).contains("foiled"),
                        r -> assertThat(r).contains("curse"));
    }

    @Test
    void peter_perfect_narrates_his_heroism() {
        var response = support.askCharacter("peter-perfect",
                "You are about to open a door that might be trapped. "
                        + "Penelope is watching.");
        System.out.println("[Peter Perfect — heroism] " + response);
        assertThat(response.toLowerCase())
                .as("Should narrate heroism theatrically")
                .satisfiesAnyOf(
                        r -> assertThat(r).contains("peter perfect"),
                        r -> assertThat(r).contains("boldly"),
                        r -> assertThat(r).contains("allow me"),
                        r -> assertThat(r).contains("fear not"));
    }
}
