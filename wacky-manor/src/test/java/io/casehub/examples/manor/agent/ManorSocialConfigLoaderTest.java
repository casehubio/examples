package io.casehub.examples.manor.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManorSocialConfigLoaderTest {

    @Test
    void loadsAllCharacters() {
        var configs = ManorSocialConfigLoader.load();
        assertThat(configs).hasSize(18);
        assertThat(configs).containsKeys(
                "hooded-claw", "penelope-pitstop", "peter-perfect",
                "dick-dastardly", "ant-hill-mob", "muttley", "lazy-luke");
    }

    @Test
    void hoodedClaw_drivesMatchHardcoded() {
        var hc = ManorSocialConfigLoader.load().get("hooded-claw");
        assertThat(hc.drives()).hasSize(3);
        var scheming = hc.drives().stream()
                .filter(d -> d.type().equals("scheming")).findFirst().orElseThrow();
        assertThat(scheming.intensity()).isEqualTo(0.9);
        assertThat(scheming.description()).contains("elaborate plans");
    }

    @Test
    void hoodedClaw_normsMatchHardcoded() {
        var hc = ManorSocialConfigLoader.load().get("hooded-claw");
        assertThat(hc.norms()).hasSize(3);
        var topNorm = hc.norms().stream()
                .filter(n -> n.priority() == 10).findFirst().orElseThrow();
        assertThat(topNorm.rule()).isEqualTo("Never help Penelope directly");
    }

    @Test
    void hoodedClaw_initialBeliefsMatchHardcoded() {
        var hc = ManorSocialConfigLoader.load().get("hooded-claw");
        assertThat(hc.initialBeliefs()).hasSize(2);
        var penBelief = hc.initialBeliefs().stream()
                .filter(b -> b.key().equals("penelope-awareness")).findFirst().orElseThrow();
        assertThat(penBelief.value()).isEqualTo("Penelope is naive and trusts too easily");
    }

    @Test
    void penelopePitstop_loaded() {
        var pp = ManorSocialConfigLoader.load().get("penelope-pitstop");
        assertThat(pp.drives()).hasSize(3);
        assertThat(pp.norms()).hasSize(3);
        assertThat(pp.initialBeliefs()).hasSize(2);
    }

    @Test
    void unknownCharacter_notInMap() {
        var configs = ManorSocialConfigLoader.load();
        assertThat(configs).doesNotContainKey("nonexistent-character");
    }

    @Test
    void missingResource_throwsIllegalState() {
        assertThatThrownBy(() -> ManorSocialConfigLoader.load("nonexistent.yaml"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void hoodedClaw_relationshipsLoaded() {
        var hc = ManorSocialConfigLoader.load().get("hooded-claw");
        assertThat(hc.relationships()).isNotEmpty();
        var penRel = hc.relationships().stream()
                       .filter(r -> r.targetAgentId().equals("penelope-pitstop")).findFirst().orElseThrow();
        assertThat(penRel.pleasure()).isBetween(-1.0, 1.0);
        assertThat(penRel.arousal()).isBetween(-1.0, 1.0);
        assertThat(penRel.dominance()).isBetween(-1.0, 1.0);
    }

    @Test
    void allCharactersHaveRelationships() {
        var configs = ManorSocialConfigLoader.load();
        for (var entry : configs.entrySet()) {
            assertThat(entry.getValue().relationships())
                    .as("relationships for %s", entry.getKey())
                    .isNotNull();
        }
    }


    @Test
    void parsesCustomFamiliarityThresholds() {
        var configs    = ManorSocialConfigLoader.load();
        var hoodedClaw = configs.get("hooded-claw");
        assertThat(hoodedClaw.stageConfig()).isNotNull();
        assertThat(hoodedClaw.stageConfig().tiers()).hasSize(5);
        assertThat(hoodedClaw.stageConfig().tiers().get(1).threshold()).isGreaterThan(0.2);
        assertThat(hoodedClaw.stageConfig().decayRate()).isEqualTo(0.02);
        assertThat(hoodedClaw.stageConfig().positiveWeight()).isEqualTo(0.8);
        assertThat(hoodedClaw.stageConfig().negativeWeight()).isEqualTo(0.7);
    }

    @Test
    void defaultThresholdsWhenNotSpecified() {
        var configs       = ManorSocialConfigLoader.load();
        var dickDastardly = configs.get("dick-dastardly");
        assertThat(dickDastardly.stageConfig())
                .isEqualTo(io.casehub.blocks.agentic.social.RelationshipStageConfig.defaults());
    }

    @Test
    void penelopeHasLowerThresholds() {
        var configs  = ManorSocialConfigLoader.load();
        var penelope = configs.get("penelope-pitstop");
        assertThat(penelope.stageConfig().tiers().get(1).threshold()).isLessThan(0.2);
        assertThat(penelope.stageConfig().positiveWeight()).isEqualTo(1.2);
    }

}
