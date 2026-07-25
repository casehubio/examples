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
class CharacterVoiceTest {

    @Inject AgentRegistry registry;
    @Inject SystemPromptRenderer renderer;
    @Inject AgentProvider agentProvider;

    LlmTestSupport support;

    @BeforeEach
    void setUp() {
        support = new LlmTestSupport(registry, renderer, agentProvider);
    }

    @Test
    void penelope_speaks_with_southern_drawl() {
        var response = support.askCharacter("penelope-pitstop",
                                            "You've just arrived at a dusty old mansion. What do you think?");
        System.out.println("[Penelope] " + response);
        assertThat(response.toLowerCase())
                .as("Should contain Southern expressions")
                .satisfiesAnyOf(
                        r -> assertThat(r).contains("why"),
                        r -> assertThat(r).contains("darlin"),
                        r -> assertThat(r).contains("y'all"),
                        r -> assertThat(r).contains("delightful"),
                        r -> assertThat(r).contains("bless"));
    }

    @Test
    void hooded_claw_monologues_villainously() {
        var response = support.askCharacter("hooded-claw",
                                            "You are alone in a room. Penelope is in the next room. "
                                            + "What are you thinking?");
        System.out.println("[Hooded Claw] " + response);
        assertThat(response.toLowerCase())
                .as("Should contain villain monologue markers")
                .satisfiesAnyOf(
                        r -> assertThat(r).contains("nyah"),
                        r -> assertThat(r).contains("fiendish"),
                        r -> assertThat(r).contains("diabolical"),
                        r -> assertThat(r).contains("scheme"),
                        r -> assertThat(r).contains("penelope"));
    }

    @Test
    void ant_hill_mob_speaks_as_gangsters() {
        var response = support.askCharacter("ant-hill-mob",
                                            "You see Sneekly being very helpful to Penelope. "
                                            + "What do you think?");
        System.out.println("[Ant Hill Mob] " + response);
        assertThat(response.toLowerCase())
                .as("Should contain gangster speech and suspicion")
                .satisfiesAnyOf(
                        r -> assertThat(r).contains("boss"),
                        r -> assertThat(r).contains("dat"),
                        r -> assertThat(r).contains("suspicious"),
                        r -> assertThat(r).contains("eye on"),
                        r -> assertThat(r).contains("ain't right"));
    }

    @Test
    void dastardly_lies_when_asked() {
        var response = support.askCharacter("dick-dastardly",
                                            "Someone asks you which room the treasure is in. You don't "
                                            + "know, but you want them to go the wrong way.");
        System.out.println("[Dastardly] " + response);
        assertThat(response.toLowerCase())
                .as("Should contain confident misdirection")
                .satisfiesAnyOf(
                        r -> assertThat(r).contains("certainly"),
                        r -> assertThat(r).contains("definitely"),
                        r -> assertThat(r).contains("mehehehe"),
                        r -> assertThat(r).contains("drat"),
                        r -> assertThat(r).contains("obvious"));
    }

    @Test
    void peter_perfect_volunteers_for_danger() {
        var response = support.askCharacter("peter-perfect",
                                            "There's a dark corridor ahead. Penelope looks nervous.");
        System.out.println("[Peter Perfect] " + response);
        assertThat(response.toLowerCase())
                .as("Should contain gallant volunteering")
                .satisfiesAnyOf(
                        r -> assertThat(r).contains("allow me"),
                        r -> assertThat(r).contains("fear not"),
                        r -> assertThat(r).contains("peter perfect"),
                        r -> assertThat(r).contains("penelope"),
                        r -> assertThat(r).contains("brave"));
    }
}
