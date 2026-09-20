package io.casehub.examples.manor.agent;

import io.casehub.blocks.agentic.social.MentalModelSnapshot;
import io.casehub.blocks.agentic.social.MentalModelStore;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

final class InMemoryMentalModelStore implements MentalModelStore {
    private final Map<String, MentalModelSnapshot> snapshots = new ConcurrentHashMap<>();

    @Override public void store(MentalModelSnapshot snapshot) {
        snapshots.put(key(snapshot.agentId(), snapshot.subjectId(), snapshot.tenantId()), snapshot);
    }

    @Override public Optional<MentalModelSnapshot> lookup(String agentId, String subjectId, String tenantId) {
        return Optional.ofNullable(snapshots.get(key(agentId, subjectId, tenantId)));
    }

    @Override public List<MentalModelSnapshot> findByAgent(String agentId, String tenantId) {
        return snapshots.values().stream()
                .filter(s -> s.agentId().equals(agentId) && s.tenantId().equals(tenantId))
                .toList();
    }

    @Override public void eraseSubject(String subjectId, String tenantId) {
        snapshots.entrySet().removeIf(e ->
                e.getValue().subjectId().equals(subjectId) && e.getValue().tenantId().equals(tenantId));
    }

    private static String key(String agentId, String subjectId, String tenantId) {
        return agentId + ":" + subjectId + ":" + tenantId;
    }
}
