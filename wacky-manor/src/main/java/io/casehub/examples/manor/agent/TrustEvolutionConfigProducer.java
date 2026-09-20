package io.casehub.examples.manor.agent;

import io.casehub.engine.trust.TrustEvolutionConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@ApplicationScoped
public class TrustEvolutionConfigProducer {

    @Produces
    @Singleton
    TrustEvolutionConfig produce() {
        return ManorTrustEvolutionConfigLoader.load();
    }
}
