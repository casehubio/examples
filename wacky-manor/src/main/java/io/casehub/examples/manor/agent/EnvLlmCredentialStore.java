package io.casehub.examples.manor.agent;

import io.casehub.platform.api.credentials.LlmCredentialStore;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Alternative
@Priority(10)
@ApplicationScoped
class EnvLlmCredentialStore implements LlmCredentialStore {

    private final Map<String, Map<String, Map<String, String>>> store = new ConcurrentHashMap<>();

    EnvLlmCredentialStore() {
        String projectId = System.getenv("ANTHROPIC_VERTEX_PROJECT_ID");
        String region = System.getenv("ANTHROPIC_VERTEX_REGION");
        if (projectId != null && !projectId.isBlank()) {
            store("platform", "cloud-vertex", Map.of(
                    "project-id", projectId,
                    "region", region != null ? region : "us-central1"));
        }
    }

    @Override
    public void store(String tenancyId, String credentialRef, Map<String, String> credentials) {
        store.computeIfAbsent(tenancyId, k -> new ConcurrentHashMap<>())
                .put(credentialRef, Map.copyOf(credentials));
    }

    @Override
    public Map<String, String> resolve(String tenancyId, String credentialRef) {
        var tenantCreds = store.get(tenancyId);
        return tenantCreds != null ? tenantCreds.getOrDefault(credentialRef, Map.of()) : Map.of();
    }

    @Override
    public void delete(String tenancyId, String credentialRef) {
        var tenantCreds = store.get(tenancyId);
        if (tenantCreds != null) tenantCreds.remove(credentialRef);
    }

    @Override
    public List<String> listRefs(String tenancyId) {
        var tenantCreds = store.get(tenancyId);
        return tenantCreds != null ? List.copyOf(tenantCreds.keySet()) : List.of();
    }
}
