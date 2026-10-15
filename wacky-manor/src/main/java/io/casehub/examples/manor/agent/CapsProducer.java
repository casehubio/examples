package io.casehub.examples.manor.agent;

import io.casehub.neocortex.caps.CapsEngine;
import io.casehub.neocortex.caps.SituationClassifier;
import io.casehub.neocortex.caps.llm.LlmSituationClassifier;
import io.casehub.neocortex.caps.testing.InMemoryCapsEngine;
import io.casehub.platform.agent.AgentProvider;
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
    SituationClassifier situationClassifier(AgentProvider agentProvider, CapsEngine engine) {
        return new LlmSituationClassifier(agentProvider, engine.topology());
    }
}
