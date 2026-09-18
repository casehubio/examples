package io.casehub.examples.manor.agent;

import io.casehub.engine.trust.TrustEvolutionConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class TrustEvolutionConfigProducer {

    @Produces
    @ApplicationScoped
    TrustEvolutionConfig produce() {
        return ManorTrustEvolutionConfigLoader.load();
    }
}
