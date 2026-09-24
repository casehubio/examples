package io.casehub.examples.manor.agent;

import io.casehub.blocks.agentic.social.StrategyProfile;
import io.casehub.blocks.agentic.social.StrategyStore;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

final class InMemoryStrategyStore implements StrategyStore {
    private final Map<String, StrategyProfile> profiles = new ConcurrentHashMap<>();
    private final java.util.List<io.casehub.blocks.agentic.social.EngagementEvidence> evidence = new java.util.concurrent.CopyOnWriteArrayList<>();


    @Override public void store(StrategyProfile profile) {
        profiles.put(profile.agentId() + ":" + profile.tenantId(), profile);
    }

    @Override public Optional<StrategyProfile> lookup(String agentId, String tenantId) {
        return Optional.ofNullable(profiles.get(agentId + ":" + tenantId));
    }

    @Override public List<String> subjectInsights(String agentId, String subjectId, String tenantId) {
        return List.of();
    }

    @Override public void eraseAgent(String agentId, String tenantId) {
        profiles.remove(agentId + ":" + tenantId);
    }

    @Override public void eraseSubject(String subjectId, String tenantId) {}

    @Override
    public void storeEvidence(io.casehub.blocks.agentic.social.EngagementEvidence e) {
        evidence.add(e);
    }

    @Override
    public int evidenceCount(String agentId, String tenantId) {
        return (int) evidence.stream()
                             .filter(ev -> ev.agentId().equals(agentId) && ev.tenantId().equals(tenantId))
                             .count();
    }

    @Override
    public java.util.List<io.casehub.blocks.agentic.social.EngagementEvidence> recentEvidence(String agentId, String tenantId, int limit) {
        return evidence.stream()
                       .filter(ev -> ev.agentId().equals(agentId) && ev.tenantId().equals(tenantId))
                       .sorted(java.util.Comparator.comparing(io.casehub.blocks.agentic.social.EngagementEvidence::recordedAt).reversed())
                       .limit(limit)
                       .toList();
    }

}
