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
class NewCharacterVoiceTest {

    @Inject AgentRegistry registry;
    @Inject SystemPromptRenderer renderer;
    @Inject AgentProvider agentProvider;

    LlmTestSupport support;

    @BeforeEach
    void setUp() {
        support = new LlmTestSupport(registry, renderer, agentProvider);
    }

    @Test
    void muttley_does_not_speak_sentences() {
        var response = support.askCharacter("muttley",
            "Dastardly just gave you a shiny medal. React!");
        System.out.println("[Muttley] " + response);
        assertThat(response.toLowerCase())
            .as("Should communicate through sounds, not full sentences")
            .satisfiesAnyOf(
                r -> assertThat(r).contains("hehe"),
                r -> assertThat(r).contains("snicker"),
                r -> assertThat(r).contains("*sniff*"),
                r -> assertThat(r).contains("*wag*"),
                r -> assertThat(r).contains("rassafrassa"));
    }

    @Test
    void sawtooth_communicates_physically() {
        var response = support.askCharacter("sawtooth",
            "Rufus points at a wooden beam and says 'Gnaw that one, boy!'");
        System.out.println("[Sawtooth] " + response);
        assertThat(response.toLowerCase())
            .as("Should communicate through gnawing and physical actions")
            .satisfiesAnyOf(
                r -> assertThat(r).contains("gnaw"),
                r -> assertThat(r).contains("chatter"),
                r -> assertThat(r).contains("tail"),
                r -> assertThat(r).contains("nod"),
                r -> assertThat(r).contains("teeth"));
    }

    @Test
    void little_gruesome_squeaks_only() {
        var response = support.askCharacter("little-gruesome",
            "You found a hidden passage in the vents! Try to tell Big Gruesome.");
        System.out.println("[Little Gruesome] " + response);
        assertThat(response.toLowerCase()).contains("squeak");
    }

    @Test
    void blast_barks_orders() {
        var response = support.askCharacter("sergeant-blast",
            "Someone is trying to walk past you in the corridor.");
        System.out.println("[Sergeant Blast] " + response);
        assertThat(response.toUpperCase())
            .as("Should bark military orders")
            .satisfiesAnyOf(
                r -> assertThat(r).contains("HALT"),
                r -> assertThat(r).contains("PASSWORD"),
                r -> assertThat(r).contains("SECTION"),
                r -> assertThat(r).contains("PERMISSION"),
                r -> assertThat(r).contains("AUTHORIS"));
    }

    @Test
    void meekly_is_timid_but_helpful() {
        var response = support.askCharacter("private-meekly",
            "Someone needs to get past Blast's corridor. Blast has gone to inspect another room.");
        System.out.println("[Private Meekly] " + response);
        assertThat(response.toLowerCase())
            .as("Should be timid and helpful")
            .satisfiesAnyOf(
                r -> assertThat(r).contains("i-i"),
                r -> assertThat(r).contains("sorry"),
                r -> assertThat(r).contains("fuss"),
                r -> assertThat(r).contains("don't want to"),
                r -> assertThat(r).contains("happened to"));
    }

    @Test
    void pat_pending_speaks_technically() {
        var response = support.askCharacter("pat-pending",
            "You see a large machine with many levers and dials in the laboratory.");
        System.out.println("[Pat Pending] " + response);
        assertThat(response.toLowerCase())
            .as("Should use technical jargon")
            .satisfiesAnyOf(
                r -> assertThat(r).contains("mechanism"),
                r -> assertThat(r).contains("oscillat"),
                r -> assertThat(r).contains("calibrat"),
                r -> assertThat(r).contains("fascinating"),
                r -> assertThat(r).contains("apparatus"));
    }

    @Test
    void rock_slag_has_limited_vocabulary() {
        var response = support.askCharacter("rock-slag",
            "Someone shows you a book and asks 'Can you read this?'");
        System.out.println("[Rock Slag] " + response);
        assertThat(response.toLowerCase())
            .as("Should have caveman speech")
            .satisfiesAnyOf(
                r -> assertThat(r).contains("slag"),
                r -> assertThat(r).contains("ugh"),
                r -> assertThat(r).contains("smash"),
                r -> assertThat(r).contains("hmm"),
                r -> assertThat(r).contains("book"));
    }

    @Test
    void big_gruesome_thinks_everything_is_lovely() {
        var response = support.askCharacter("big-gruesome",
            "You are in a damp, cobweb-filled cellar. What do you think of it?");
        System.out.println("[Big Gruesome] " + response);
        assertThat(response.toLowerCase())
            .as("Should express delight")
            .satisfiesAnyOf(
                r -> assertThat(r).contains("lovely"),
                r -> assertThat(r).contains("pretty"),
                r -> assertThat(r).contains("beautiful"),
                r -> assertThat(r).contains("wonderful"),
                r -> assertThat(r).contains("delightful"));
    }

    @Test
    void lazy_luke_is_sleepy() {
        var response = support.askCharacter("lazy-luke",
            "Someone is shaking you awake and shouting about treasure.");
        System.out.println("[Lazy Luke] " + response);
        assertThat(response.toLowerCase())
            .as("Should be drowsy and uninterested")
            .satisfiesAnyOf(
                r -> assertThat(r).contains("yawn"),
                r -> assertThat(r).contains("reckon"),
                r -> assertThat(r).contains("tomorrow"),
                r -> assertThat(r).contains("sleep"),
                r -> assertThat(r).contains("wait"));
    }

    @Test
    void rufus_wants_a_proper_wrench() {
        var response = support.askCharacter("rufus-ruffcut",
            "Someone hands you a set of fancy chrome tools and asks you to fix the machine.");
        System.out.println("[Rufus Ruffcut] " + response);
        assertThat(response.toLowerCase())
            .as("Should want a proper wrench")
            .satisfiesAnyOf(
                r -> assertThat(r).contains("wrench"),
                r -> assertThat(r).contains("fancy"),
                r -> assertThat(r).contains("proper"),
                r -> assertThat(r).contains("city"),
                r -> assertThat(r).contains("fix"));
    }
}
