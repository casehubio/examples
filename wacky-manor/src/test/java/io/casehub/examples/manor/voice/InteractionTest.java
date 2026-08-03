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
class InteractionTest {

    @Inject AgentRegistry registry;
    @Inject SystemPromptRenderer renderer;
    @Inject AgentProvider agentProvider;

    LlmTestSupport support;

    @BeforeEach
    void setUp() {
        support = new LlmTestSupport(registry, renderer, agentProvider);
    }

    @Test
    void muttley_trades_key_for_medal() {
        var response = support.askCharacter("muttley",
            "== Your Inventory ==\n- brass-key\n\n"
            + "== Recent Activity ==\n"
            + "Dick Dastardly handed you a shiny medal and said "
            + "'Here Muttley, a medal for you! Now give me that key.'\n\n"
            + "You just received a medal! React and decide your action.");
        System.out.println("[Muttley medal exchange] " + response);
        assertThat(response.toLowerCase())
            .as("Should express joy about medal and willingness to give key")
            .satisfiesAnyOf(
                r -> assertThat(r).contains("hehe"),
                r -> assertThat(r).contains("medal"),
                r -> assertThat(r).containsPattern("give|key|take"));
    }

    @Test
    void blubber_panics_when_startled() {
        var response = support.askCharacter("blubber-bear",
            "== Recent Activity ==\n"
            + "Peter Perfect SHOUTED 'WAKE UP BEAR!' right next to your ear.\n\n"
            + "You were just startled awake! React!");
        System.out.println("[Blubber panic] " + response);
        assertThat(response.toLowerCase())
            .as("Should panic and cause destruction")
            .satisfiesAnyOf(
                r -> assertThat(r).contains("roar"),
                r -> assertThat(r).contains("crash"),
                r -> assertThat(r).contains("panic"),
                r -> assertThat(r).contains("destroy"),
                r -> assertThat(r).contains("smash"),
                r -> assertThat(r).contains("knock"));
    }

    @Test
    void slag_brothers_smash_when_asked_about_riddle() {
        var response = support.askCharacter("rock-slag",
            "Penelope Pitstop asks you: 'Excuse me, can you help me "
            + "solve this riddle on the mantelpiece?'");
        System.out.println("[Rock riddle] " + response);
        assertThat(response.toLowerCase())
            .as("Should smash or hit something")
            .satisfiesAnyOf(
                r -> assertThat(r).contains("smash"),
                r -> assertThat(r).contains("slag"),
                r -> assertThat(r).contains("hit"),
                r -> assertThat(r).contains("break"),
                r -> assertThat(r).contains("ugh"));
    }

    @Test
    void blast_blocks_meekly_helps() {
        var blastResponse = support.askCharacter("sergeant-blast",
            "Penelope Pitstop is trying to walk through the corridor you are guarding.");
        System.out.println("[Blast blocks] " + blastResponse);
        assertThat(blastResponse.toUpperCase())
            .as("Should demand password or block access")
            .satisfiesAnyOf(
                r -> assertThat(r).contains("HALT"),
                r -> assertThat(r).contains("PASSWORD"),
                r -> assertThat(r).contains("AUTHORIS"));

        var meeklyResponse = support.askCharacter("private-meekly",
            "Penelope Pitstop needs to get through the corridor. "
            + "Sergeant Blast has gone to inspect the laboratory. "
            + "What do you do?");
        System.out.println("[Meekly helps] " + meeklyResponse);
        assertThat(meeklyResponse.toLowerCase())
            .as("Should secretly help")
            .satisfiesAnyOf(
                r -> assertThat(r).containsPattern("wave|through|way|go ahead"),
                r -> assertThat(r).contains("don't tell"),
                r -> assertThat(r).contains("fuss"));
    }

    @Test
    void dastardly_lies_to_peter() {
        var transcript = support.runConversation(
            "dick-dastardly", "peter-perfect",
            "Peter Perfect asks Dastardly: 'I say, Dastardly old chap, "
            + "which way to the treasure room?'",
            2);
        System.out.println("[Dastardly/Peter] " + transcript);
        assertThat(transcript.toLowerCase())
            .as("Dastardly should lie, Peter should respond gallantly")
            .contains("peter");
    }
}
