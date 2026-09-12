package io.casehub.examples.manor.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SocialConfigTest {

    @Test
    void hoodedClawHasSchemingDrive() {
        var config = SocialConfig.forCharacter("hooded-claw");
        assertThat(config.drives()).isNotEmpty();
        assertThat(config.drives().stream().anyMatch(d -> d.type().equals("scheming"))).isTrue();
        assertThat(config.drives().stream().filter(d -> d.type().equals("scheming")).findFirst().orElseThrow().intensity()).isEqualTo(0.9);
    }

    @Test
    void hoodedClawHasNorms() {
        var config = SocialConfig.forCharacter("hooded-claw");
        assertThat(config.norms()).hasSize(3);
        assertThat(config.norms().stream().anyMatch(n -> n.rule().contains("Penelope"))).isTrue();
    }

    @Test
    void hoodedClawHasInitialBeliefs() {
        var config = SocialConfig.forCharacter("hooded-claw");
        assertThat(config.initialBeliefs()).hasSize(2);
        assertThat(config.initialBeliefs().stream().anyMatch(b -> b.key().equals("penelope-awareness"))).isTrue();
    }

    @Test
    void penelopeHasSocialHarmonyDrive() {
        var config = SocialConfig.forCharacter("penelope-pitstop");
        assertThat(config.drives()).isNotEmpty();
        assertThat(config.drives().stream().anyMatch(d -> d.type().equals("social-harmony"))).isTrue();
    }

    @Test
    void allFiveCoreCharactersHaveConfig() {
        assertThat(SocialConfig.hasConfig("hooded-claw")).isTrue();
        assertThat(SocialConfig.hasConfig("penelope-pitstop")).isTrue();
        assertThat(SocialConfig.hasConfig("peter-perfect")).isTrue();
        assertThat(SocialConfig.hasConfig("dick-dastardly")).isTrue();
        assertThat(SocialConfig.hasConfig("ant-hill-mob")).isTrue();
    }

    @Test
    void unknownCharacterReturnsEmpty() {
        var config = SocialConfig.forCharacter("unknown-agent");
        assertThat(config.drives()).isEmpty();
        assertThat(config.norms()).isEmpty();
        assertThat(config.initialBeliefs()).isEmpty();
    }

    @Test
    void emptyConfigIsValid() {
        var config = SocialConfig.empty();
        assertThat(config.drives()).isEmpty();
        assertThat(config.norms()).isEmpty();
        assertThat(config.initialBeliefs()).isEmpty();
    }

    @Test
    void normsHavePositivePriority() {
        for (var agentId : java.util.List.of("hooded-claw", "penelope-pitstop", "peter-perfect", "dick-dastardly", "ant-hill-mob")) {
            var config = SocialConfig.forCharacter(agentId);
            config.norms().forEach(n -> assertThat(n.priority()).as("norm priority for %s: %s", agentId, n.rule()).isGreaterThan(0));
        }
    }
}
