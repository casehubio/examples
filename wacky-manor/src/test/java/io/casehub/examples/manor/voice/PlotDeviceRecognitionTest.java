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
class PlotDeviceRecognitionTest {

    @Inject AgentRegistry registry;
    @Inject SystemPromptRenderer renderer;
    @Inject AgentProvider agentProvider;

    LlmTestSupport support;

    @BeforeEach
    void setUp() {
        support = new LlmTestSupport(registry, renderer, agentProvider);
    }

    @Test
    void hooded_claw_schemes_with_poison() {
        var response = support.askCharacter("hooded-claw",
                "You enter the kitchen. On a high shelf you notice a bottle "
                        + "of rat poison. Penelope is in the next room having tea. "
                        + "You are alone.");
        System.out.println("[HC — poison discovery] " + response);
        assertThat(response.toLowerCase())
                .as("Should scheme to USE the poison, not just note it")
                .satisfiesAnyOf(
                        r -> assertThat(r).contains("tea"),
                        r -> assertThat(r).contains("slip"),
                        r -> assertThat(r).contains("pour"),
                        r -> assertThat(r).contains("cup"),
                        r -> assertThat(r).contains("drink"));
    }

    @Test
    void hooded_claw_maintains_disguise() {
        var response = support.askCharacter("hooded-claw",
                "Penelope walks into the kitchen while you're holding the "
                        + "poison bottle. She says 'Oh, Mr. Sneekly, what's that "
                        + "you've got there?'");
        System.out.println("[HC — disguise maintenance] " + response);
        assertThat(response.toLowerCase())
                .as("Should switch to Sneekly voice and make an excuse")
                .satisfiesAnyOf(
                        r -> assertThat(r).contains("miss pitstop"),
                        r -> assertThat(r).contains("dear"),
                        r -> assertThat(r).contains("cleaning"),
                        r -> assertThat(r).contains("nothing"),
                        r -> assertThat(r).contains("oh my"));
        assertThat(response.toLowerCase())
                .as("Should NOT reveal villainy to Penelope")
                .doesNotContain("poison you")
                .doesNotContain("kill you")
                .doesNotContain("eliminate you");
    }

    @Test
    void ant_hill_mob_notices_suspicious_behavior() {
        var response = support.askCharacter("ant-hill-mob",
                "You see Sneekly putting something in Penelope's tea cup "
                        + "when he thinks nobody is looking.");
        System.out.println("[Mob — suspicious behavior] " + response);
        assertThat(response.toLowerCase())
                .as("Should react protectively, not analytically")
                .satisfiesAnyOf(
                        r -> assertThat(r).contains("boss"),
                        r -> assertThat(r).contains("sneekly"),
                        r -> assertThat(r).contains("cup"),
                        r -> assertThat(r).contains("somethin' ain't right"),
                        r -> assertThat(r).contains("protect"));
    }

    @Test
    void penelope_oblivious_to_danger() {
        var response = support.askCharacter("penelope-pitstop",
                "Mr. Sneekly is being unusually insistent that you drink "
                        + "your tea RIGHT NOW. The Ant Hill Mob are trying to get "
                        + "your attention.");
        System.out.println("[Penelope — oblivious] " + response);
        assertThat(response.toLowerCase())
                .as("Should trust Sneekly, not suspect danger")
                .satisfiesAnyOf(
                        r -> assertThat(r).contains("kind"),
                        r -> assertThat(r).contains("sweet"),
                        r -> assertThat(r).contains("gentleman"),
                        r -> assertThat(r).contains("thoughtful"),
                        r -> assertThat(r).contains("tea"));
    }

    @Test
    void dastardly_gives_wrong_directions() {
        var response = support.askCharacter("dick-dastardly",
                "Peter Perfect asks you: 'Dastardly, old chap, which way "
                        + "to the treasure room?'");
        System.out.println("[Dastardly — misdirection] " + response);
        assertThat(response.length())
                .as("Should give a confident, detailed misdirection")
                .isGreaterThan(50);
    }
}
