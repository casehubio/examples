package io.casehub.examples.manor.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SocialConfigTest {

    @Test
    void hoodedClawHasSchemingDrive() {
        var config = ManorSocialConfigLoader.load().get("hooded-claw");
        assertThat(config.drives()).isNotEmpty();
        assertThat(config.drives().stream().anyMatch(d -> d.type().equals("scheming"))).isTrue();
        assertThat(config.drives().stream().filter(d -> d.type().equals("scheming")).findFirst().orElseThrow().intensity()).isEqualTo(0.9);
    }

    @Test
    void hoodedClawHasNorms() {
        var config = ManorSocialConfigLoader.load().get("hooded-claw");
        assertThat(config.norms()).hasSize(3);
        assertThat(config.norms().stream().anyMatch(n -> n.rule().contains("Penelope"))).isTrue();
    }

    @Test
    void hoodedClawHasInitialBeliefs() {
        var config = ManorSocialConfigLoader.load().get("hooded-claw");
        assertThat(config.initialBeliefs()).hasSize(2);
        assertThat(config.initialBeliefs().stream().anyMatch(b -> b.key().equals("penelope-awareness"))).isTrue();
    }

    @Test
    void penelopeHasSocialHarmonyDrive() {
        var config = ManorSocialConfigLoader.load().get("penelope-pitstop");
        assertThat(config.drives()).isNotEmpty();
        assertThat(config.drives().stream().anyMatch(d -> d.type().equals("social-harmony"))).isTrue();
    }

    @Test
    void allFiveCoreCharactersHaveConfig() {
        assertThat(ManorSocialConfigLoader.load().containsKey("hooded-claw")).isTrue();
        assertThat(ManorSocialConfigLoader.load().containsKey("penelope-pitstop")).isTrue();
        assertThat(ManorSocialConfigLoader.load().containsKey("peter-perfect")).isTrue();
        assertThat(ManorSocialConfigLoader.load().containsKey("dick-dastardly")).isTrue();
        assertThat(ManorSocialConfigLoader.load().containsKey("ant-hill-mob")).isTrue();
    }

    @Test
    void unknownCharacterNotInYaml() {
        assertThat(ManorSocialConfigLoader.load()).doesNotContainKey("unknown-agent");
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
            var config = ManorSocialConfigLoader.load().get(agentId);
            config.norms().forEach(n -> assertThat(n.priority()).as("norm priority for %s: %s", agentId, n.rule()).isGreaterThan(0));
        }
    }
}
