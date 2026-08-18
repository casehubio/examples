package io.casehub.examples.manor.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ManorTrustProviderTest {

    @Test
    void unknownAgentGetsDefaultTrust() {
        var provider = new ManorTrustProvider(1.0, -2.0);
        var score = provider.currentTrustScore("unknown-agent");
        assertThat(score).isPresent();
        assertThat(score.getAsDouble()).isEqualTo(0.5);
    }

    @Test
    void positiveSignalsIncreaseTrust() {
        var provider = new ManorTrustProvider(1.0, -2.0);
        provider.recordPositive("helpful-agent");
        provider.recordPositive("helpful-agent");
        provider.recordPositive("helpful-agent");
        var score = provider.currentTrustScore("helpful-agent");
        assertThat(score).isPresent();
        assertThat(score.getAsDouble()).isGreaterThan(0.5);
    }

    @Test
    void negativeSignalsDecreaseTrust() {
        var provider = new ManorTrustProvider(1.0, -2.0);
        provider.recordNegative("villain");
        provider.recordNegative("villain");
        var score = provider.currentTrustScore("villain");
        assertThat(score).isPresent();
        assertThat(score.getAsDouble()).isLessThan(0.5);
    }

    @Test
    void negativeWeightIsStrongerThanPositive() {
        var provider = new ManorTrustProvider(1.0, -2.0);
        provider.recordPositive("mixed");
        provider.recordNegative("mixed");
        // 1 positive (1.0) + 1 negative (-2.0) = net -1.0
        var score = provider.currentTrustScore("mixed");
        assertThat(score).isPresent();
        assertThat(score.getAsDouble()).isLessThan(0.5);
    }

    @Test
    void scoreIsClampedBetweenZeroAndOne() {
        var provider = new ManorTrustProvider(1.0, -2.0);
        for (int i = 0; i < 20; i++) {
            provider.recordNegative("villain");
        }
        var score = provider.currentTrustScore("villain");
        assertThat(score).isPresent();
        assertThat(score.getAsDouble()).isBetween(0.0, 1.0);

        var provider2 = new ManorTrustProvider(1.0, -2.0);
        for (int i = 0; i < 20; i++) {
            provider2.recordPositive("saint");
        }
        var score2 = provider2.currentTrustScore("saint");
        assertThat(score2).isPresent();
        assertThat(score2.getAsDouble()).isBetween(0.0, 1.0);
    }

    @Test
    void separateScoresPerAgent() {
        var provider = new ManorTrustProvider(1.0, -2.0);
        provider.recordPositive("good-agent");
        provider.recordNegative("bad-agent");
        assertThat(provider.currentTrustScore("good-agent").getAsDouble())
                .isGreaterThan(provider.currentTrustScore("bad-agent").getAsDouble());
    }
}
