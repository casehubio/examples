package io.casehub.examples.manor.agent;

import io.casehub.blocks.agentic.social.narrative.NarrativeState;
import io.casehub.blocks.agentic.social.narrative.NarrativeStore;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class InMemoryNarrativeStore implements NarrativeStore {
    private final Map<String, NarrativeState> states = new ConcurrentHashMap<>();

    @Override public void store(NarrativeState state) {
        states.put(state.scopeId() + ":" + state.tenantId(), state);
    }

    @Override public @Nullable NarrativeState load(String scopeId, String tenantId) {
        return states.get(scopeId + ":" + tenantId);
    }
}
