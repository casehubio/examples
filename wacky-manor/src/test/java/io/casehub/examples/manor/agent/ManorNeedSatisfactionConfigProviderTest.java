package io.casehub.examples.manor.agent;

import io.casehub.neocortex.cognition.drive.NeedSatisfactionConfig;
import io.casehub.neocortex.cognition.need.NeedTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ManorNeedSatisfactionConfigProviderTest {

    private NeedSatisfactionConfig globalConfig;
    private ManorNeedSatisfactionConfigProvider provider;

    @BeforeEach
    void setUp() {
        globalConfig = new StubNeedSatisfactionConfig();
        provider = new ManorNeedSatisfactionConfigProvider(globalConfig);
        provider.setDispositions(Map.of(
                "hooded-claw", new SocialConfig.Disposition("competitive", "flexible", "bold", "high", "competitive"),
                "penelope-pitstop", new SocialConfig.Disposition("cooperative", "moderate", "cautious", "moderate", "accommodating")
        ));
    }

    @Test
    void cooperativeAgentGetsGlobalConfig() {
        var config = provider.configFor("penelope-pitstop");
        assertSame(globalConfig, config);
    }

    @Test
    void unknownAgentGetsGlobalConfig() {
        var config = provider.configFor("unknown-agent");
        assertSame(globalConfig, config);
    }

    @Test
    void competitiveAgentGetsFasterSocialDecay() {
        var config = provider.configFor("hooded-claw");
        assertNotSame(globalConfig, config);
        assertTrue(config.decayRate(NeedTier.SOCIAL) > globalConfig.decayRate(NeedTier.SOCIAL));
    }

    @Test
    void competitiveAgentGetsLowerSocialRestingLevel() {
        var config = provider.configFor("hooded-claw");
        assertTrue(config.restingLevel(NeedTier.SOCIAL) < globalConfig.restingLevel(NeedTier.SOCIAL));
    }

    @Test
    void competitiveAgentNonSocialTiersUnchanged() {
        var config = provider.configFor("hooded-claw");
        assertEquals(globalConfig.decayRate(NeedTier.SAFETY), config.decayRate(NeedTier.SAFETY));
        assertEquals(globalConfig.restingLevel(NeedTier.TASKS), config.restingLevel(NeedTier.TASKS));
        assertEquals(globalConfig.decayRate(NeedTier.UNDERSTANDING), config.decayRate(NeedTier.UNDERSTANDING));
    }

    @Test
    void competitiveAgentGlobalFieldsUnchanged() {
        var config = provider.configFor("hooded-claw");
        assertEquals(globalConfig.satisfactionIncrement(), config.satisfactionIncrement());
        assertEquals(globalConfig.dissatisfactionIncrement(), config.dissatisfactionIncrement());
        assertEquals(globalConfig.initialSatisfaction(), config.initialSatisfaction());
    }

    static class StubNeedSatisfactionConfig implements NeedSatisfactionConfig {
        @Override public double satisfactionIncrement() { return 0.05; }
        @Override public double dissatisfactionIncrement() { return 0.05; }
        @Override public double initialSatisfaction() { return 0.5; }
        @Override public DecayConfig decay() {
            return new DecayConfig() {
                @Override public double safety() { return 0.15; }
                @Override public double tasks() { return 0.10; }
                @Override public double social() { return 0.08; }
                @Override public double selfExpression() { return 0.05; }
                @Override public double understanding() { return 0.03; }
            };
        }
        @Override public RestingConfig resting() {
            return new RestingConfig() {
                @Override public double safety() { return 0.6; }
                @Override public double tasks() { return 0.3; }
                @Override public double social() { return 0.4; }
                @Override public double selfExpression() { return 0.4; }
                @Override public double understanding() { return 0.4; }
            };
        }
    }
}
