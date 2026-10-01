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
    public SystemPromptRenderer systemPromptRenderer() {
        return new CognitiveSystemPromptRenderer(CognitionConfig.all());
    }

    @Produces
    @Singleton
    public VocabularyRegistry vocabularyRegistry() {
        return new io.casehub.engine.runtime.worker.NoOpVocabularyRegistry();
    }
}
