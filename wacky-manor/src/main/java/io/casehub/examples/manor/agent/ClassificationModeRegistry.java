package io.casehub.examples.manor.agent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ClassificationModeRegistry {

    private final Set<String> externalCharacters = ConcurrentHashMap.newKeySet();

    @Inject
    void init(ManorConfig config) {
        var configured = config.classification().externalCharacters();
        if (configured != null && !configured.isBlank()) {
            Arrays.stream(configured.split(","))
                    .map(String::strip)
                    .filter(s -> !s.isEmpty())
                    .forEach(externalCharacters::add);
        }
    }

    public boolean isExternal(String agentId) {
        return externalCharacters.contains(agentId);
    }

    public void addExternal(String agentId) {
        externalCharacters.add(agentId);
    }

    public void removeExternal(String agentId) {
        externalCharacters.remove(agentId);
    }

    public Set<String> externalCharacters() {
        return Collections.unmodifiableSet(externalCharacters);
    }
}
