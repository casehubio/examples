package io.casehub.examples.manor.agent;

import io.casehub.neocortex.caps.CapsEngine;
import io.casehub.neocortex.caps.SituationClassifier;
import io.casehub.neocortex.caps.llm.LlmSituationClassifier;
import io.casehub.neocortex.caps.testing.InMemoryCapsEngine;
import io.casehub.platform.agent.BackendInstanceRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class CapsProducer {

    @Produces
    @ApplicationScoped
    CapsEngine capsEngine() {
        return new InMemoryCapsEngine();
    }

    @Produces
    @ApplicationScoped
    SituationClassifier situationClassifier(BackendInstanceRegistry backendRegistry, CapsEngine engine) {
        var backend = backendRegistry.resolve("claude", "default")
                                     .orElseThrow(() -> new IllegalStateException("No 'claude' backend for classifier pool"));
        var pool = new io.casehub.platform.agent.session.SessionPool(backend,
                                                                     new io.casehub.platform.agent.session.SessionPoolConfig(
                                                                             "classifier-pool", "claude", 0, 6,
                                                                             java.time.Duration.ofSeconds(120), java.time.Duration.ofSeconds(5)));
        return new LlmSituationClassifier(pool, engine.topology());
    }
}
