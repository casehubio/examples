package io.casehub.examples.manor.agent;

import io.casehub.blocks.agentic.social.UserProfile;
import io.casehub.blocks.agentic.social.UserProfileStore;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

final class InMemoryUserProfileStore implements UserProfileStore {
    private final Map<String, UserProfile> profiles = new ConcurrentHashMap<>();

    @Override public void store(UserProfile profile) {
        profiles.put(key(profile.agentId(), profile.subjectId(), profile.tenantId()), profile);
    }

    @Override public Optional<UserProfile> lookup(String agentId, String subjectId, String tenantId) {
        return Optional.ofNullable(profiles.get(key(agentId, subjectId, tenantId)));
    }

    @Override public List<UserProfile> findByAgent(String agentId, String tenantId) {
        return profiles.values().stream()
                .filter(p -> p.agentId().equals(agentId) && p.tenantId().equals(tenantId))
                .toList();
    }

    @Override public void eraseSubject(String subjectId, String tenantId) {
        profiles.entrySet().removeIf(e ->
                e.getValue().subjectId().equals(subjectId) && e.getValue().tenantId().equals(tenantId));
    }

    private static String key(String agentId, String subjectId, String tenantId) {
        return agentId + ":" + subjectId + ":" + tenantId;
    }
}
