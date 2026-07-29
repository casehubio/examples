package io.casehub.examples.manor;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.eidos.api.spi.AgentDescriptorRegistrar;
import io.casehub.eidos.runtime.registrar.ClasspathYamlDescriptorRegistrar;
import io.casehub.examples.manor.model.ProfileMode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
@Alternative
@jakarta.annotation.Priority(1)
public class ProfileAwareDescriptorRegistrar implements AgentDescriptorRegistrar {

    private static final String BASE_PATH = "META-INF/eidos/descriptors-%s.yaml";

    private final ProfileMode profileMode;
    private final VocabularyRegistry vocabRegistry;

    @Inject
    public ProfileAwareDescriptorRegistrar(
            @ConfigProperty(name = "manor.scenario.profile", defaultValue = "BASELINE") ProfileMode profileMode,
            VocabularyRegistry vocabRegistry) {
        this.profileMode = profileMode;
        this.vocabRegistry = vocabRegistry;
    }

    ProfileAwareDescriptorRegistrar(ProfileMode profileMode) {
        this.profileMode = profileMode;
        this.vocabRegistry = null;
    }

    @Override
    public List<AgentDescriptor> descriptors() {
        var resourcePath = String.format(BASE_PATH, profileMode.name().toLowerCase(Locale.ROOT));
        var url = Thread.currentThread().getContextClassLoader().getResource(resourcePath);
        if (url == null) {
            throw new IllegalStateException("Descriptor file not found: " + resourcePath);
        }
        try (var stream = url.openStream()) {
            return new ClasspathYamlDescriptorRegistrar().loadFrom(stream, vocabRegistry);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + resourcePath, e);
        }
    }
}
