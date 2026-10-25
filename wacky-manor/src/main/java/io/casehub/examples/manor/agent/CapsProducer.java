package io.casehub.examples.manor.agent;

import io.casehub.neocortex.caps.CapsEngine;
import io.casehub.neocortex.caps.SituationClassifier;
import io.casehub.neocortex.caps.llm.LlmSituationClassifier;
import io.casehub.neocortex.caps.testing.InMemoryCapsEngine;
import io.casehub.platform.agent.session.SessionPipeline;
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
    SituationClassifier situationClassifier(CapsEngine engine, SessionPipeline pipeline) {
        return new LlmSituationClassifier(pipeline, engine.topology());
    }
}
