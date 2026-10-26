package io.casehub.examples.manor.agent;

import io.casehub.neocortex.cognition.drive.NeedSatisfactionConfig;
import io.casehub.neocortex.cognition.drive.NeedSatisfactionConfigProvider;
import io.casehub.neocortex.cognition.need.NeedTier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ManorNeedSatisfactionConfigProvider implements NeedSatisfactionConfigProvider {

    private final NeedSatisfactionConfig globalConfig;
    private final Map<String, SocialConfig.Disposition> dispositions;
    private final ConcurrentHashMap<String, NeedSatisfactionConfig> cache = new ConcurrentHashMap<>();

    @Inject
    public ManorNeedSatisfactionConfigProvider(NeedSatisfactionConfig globalConfig) {
        this.globalConfig = globalConfig;
        this.dispositions = new ConcurrentHashMap<>();
    }

    public void setDispositions(Map<String, SocialConfig.Disposition> dispositions) {
        this.dispositions.putAll(dispositions);
        this.cache.clear();
    }

    @Override
    public NeedSatisfactionConfig configFor(String agentId) {
        var disposition = dispositions.get(agentId);
        if (disposition == null || "cooperative".equals(disposition.socialOrient())) {
            return globalConfig;
        }
        return cache.computeIfAbsent(agentId, id -> new DispositionOverrideConfig(globalConfig, disposition));
    }

    static class DispositionOverrideConfig implements NeedSatisfactionConfig {

        private final NeedSatisfactionConfig delegate;
        private final double socialDecayMultiplier;
        private final double socialRestingMultiplier;

        DispositionOverrideConfig(NeedSatisfactionConfig delegate, SocialConfig.Disposition disposition) {
            this.delegate = delegate;
            if ("competitive".equals(disposition.socialOrient())) {
                this.socialDecayMultiplier = 1.5;
                this.socialRestingMultiplier = 0.6;
            } else {
                this.socialDecayMultiplier = 1.0;
                this.socialRestingMultiplier = 1.0;
            }
        }

        @Override
        public double satisfactionIncrement() {
            return delegate.satisfactionIncrement();
        }

        @Override
        public double dissatisfactionIncrement() {
            return delegate.dissatisfactionIncrement();
        }

        @Override
        public double initialSatisfaction() {
            return delegate.initialSatisfaction();
        }

        @Override
        public DecayConfig decay() {
            return delegate.decay();
        }

        @Override
        public RestingConfig resting() {
            return delegate.resting();
        }

        @Override
        public double decayRate(NeedTier tier) {
            double base = delegate.decayRate(tier);
            if (tier == NeedTier.SOCIAL) {
                return Math.min(base * socialDecayMultiplier, 1.0);
            }
            return base;
        }

        @Override
        public double restingLevel(NeedTier tier) {
            double base = delegate.restingLevel(tier);
            if (tier == NeedTier.SOCIAL) {
                return base * socialRestingMultiplier;
            }
            return base;
        }
    }
}
