package io.casehub.examples.manor.agent;

import io.casehub.neocortex.cognition.core.CognitionConfig;
import io.casehub.neocortex.cognition.prompt.CognitiveSystemPromptRenderer;
import io.casehub.eidos.api.SystemPromptRenderer;
import io.casehub.eidos.api.VocabularyRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@ApplicationScoped
public class TestCdiBeans {

    @Produces
    @Singleton
    @io.quarkus.test.Mock
    public SystemPromptRenderer systemPromptRenderer() {
        return new CognitiveSystemPromptRenderer(CognitionConfig.all());
    }

    @Produces
    @Singleton
    @io.quarkus.test.Mock
    public VocabularyRegistry vocabularyRegistry() {
        return new io.casehub.engine.runtime.worker.NoOpVocabularyRegistry();
    }

    @Produces
    @Singleton
    @io.quarkus.test.Mock
    public io.casehub.engine.common.spi.scheduler.JobScheduler jobScheduler() {
        return new io.casehub.engine.common.spi.scheduler.JobScheduler() {
            @Override public void schedule(io.casehub.engine.common.internal.scheduler.ScheduledJobRequest r) {}
            @Override public void schedule(io.casehub.engine.common.internal.scheduler.ScheduledJobRequest.Builder b) {}
            @Override public boolean cancel(io.casehub.engine.common.internal.scheduler.JobIdentifier id) { return false; }
            @Override public int cancelGroup(String g) { return 0; }
            @Override public boolean exists(io.casehub.engine.common.internal.scheduler.JobIdentifier id) { return false; }
        };
    }

}
