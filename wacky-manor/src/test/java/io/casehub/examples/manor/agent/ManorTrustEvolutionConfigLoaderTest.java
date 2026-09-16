package io.casehub.examples.manor.agent;

import io.casehub.blocks.trust.TrustEvolutionConfig;
import io.casehub.ledger.api.model.AttestationVerdict;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ManorTrustEvolutionConfigLoaderTest {

    @Test
    void loadsDefaultConfig() {
        TrustEvolutionConfig config = ManorTrustEvolutionConfigLoader.load();

        assertThat(config.scoring().decayHalfLifeDays()).isEqualTo(30);
        assertThat(config.scoring().negativeDecayMultiplier()).isEqualTo(1.5);
        assertThat(config.consolidation().overlayProperty()).isEqualTo("trust-score");
        assertThat(config.consolidation().significantChangeThreshold()).isEqualTo(0.15);
        assertThat(config.levels().high()).isEqualTo(0.7);
        assertThat(config.levels().moderate()).isEqualTo(0.4);
        assertThat(config.levels().low()).isEqualTo(0.2);
    }

    @Test
    void parsesEventMappings() {
        TrustEvolutionConfig config = ManorTrustEvolutionConfigLoader.load();

        assertThat(config.events()).hasSize(3);

        var steal = config.findMapping("STEAL").orElseThrow();
        assertThat(steal.verdict()).isEqualTo(AttestationVerdict.FLAGGED);
        assertThat(steal.confidence()).isEqualTo(0.9);
        assertThat(steal.witnessConfidence()).isEqualTo(0.5);

        var give = config.findMapping("GIVE").orElseThrow();
        assertThat(give.verdict()).isEqualTo(AttestationVerdict.SOUND);
        assertThat(give.confidence()).isEqualTo(0.7);

        var pullAside = config.findMapping("PULL_ASIDE").orElseThrow();
        assertThat(pullAside.verdict()).isEqualTo(AttestationVerdict.SOUND);
        assertThat(pullAside.confidence()).isEqualTo(0.5);
    }

    @Test
    void unmappedActionTypesReturnEmpty() {
        TrustEvolutionConfig config = ManorTrustEvolutionConfigLoader.load();

        assertThat(config.findMapping("MOVE")).isEmpty();
        assertThat(config.findMapping("LOOK")).isEmpty();
        assertThat(config.findMapping("INTERACT")).isEmpty();
    }
}
