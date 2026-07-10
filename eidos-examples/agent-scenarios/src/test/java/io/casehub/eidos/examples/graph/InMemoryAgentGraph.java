package io.casehub.eidos.examples.graph;

import java.util.*;
import java.util.stream.*;

/**
 * Pure in-memory proof-of-concept graph. No CDI, no JPA.
 * Validates the API design and query semantics before infrastructure is built.
 */
public class InMemoryAgentGraph {

    private final List<TaskRecord> tasks = new ArrayList<>();
    private final List<OutcomeRecord> outcomes = new ArrayList<>();
    private final List<AttestationRecord> attestations = new ArrayList<>();
    private final TaskSemanticEnricher enricher;

    public InMemoryAgentGraph() {
        this(new NoOpEnricher());
    }

    public InMemoryAgentGraph(TaskSemanticEnricher enricher) {
        this.enricher = enricher;
    }

    // ── Write ──────────────────────────────────────────────────────────────

    public String recordTask(String agentId, String tenancyId, String capabilityTag,
                             String taskDomain, String externalRef) {
        String taskId = UUID.randomUUID().toString();
        tasks.add(new TaskRecord(taskId, agentId, tenancyId, capabilityTag, taskDomain, externalRef));
        return taskId;
    }

    public void recordOutcome(String taskId, TaskResult result, double confidence) {
        outcomes.add(new OutcomeRecord(taskId, result, confidence));
    }

    public void linkAttestation(String taskId, String agentId, String tenancyId,
                                String ledgerHash, String entryType) {
        attestations.add(new AttestationRecord(taskId, agentId, tenancyId, ledgerHash, entryType));
    }

    // ── Read ───────────────────────────────────────────────────────────────

    public AgentHistory agentHistory(String agentId, String tenancyId) {
        List<TaskRecord> agentTasks = tasks.stream()
            .filter(t -> t.agentId().equals(agentId) && t.tenancyId().equals(tenancyId))
            .toList();
        List<String> taskIds = agentTasks.stream().map(TaskRecord::taskId).toList();
        List<OutcomeRecord> agentOutcomes = outcomes.stream()
            .filter(o -> taskIds.contains(o.taskId()))
            .toList();
        List<AttestationRecord> agentAttestations = attestations.stream()
            .filter(a -> a.agentId().equals(agentId) && a.tenancyId().equals(tenancyId))
            .toList();
        DataSufficiency sufficiency = computeSufficiency(agentOutcomes.size(), buildWarnings());
        return new AgentHistory(agentId, tenancyId, agentTasks, agentOutcomes,
                                agentAttestations, sufficiency);
    }

    /** Returns agentIds ranked by Wilson lower bound score. */
    public List<String> topAgentsByOutcome(String capabilityTag, String taskDomain,
                                            String tenancyId, int limit) {
        // Collect equivalent domains via enricher
        Set<String> domains = new LinkedHashSet<>();
        domains.add(taskDomain);
        tasks.stream()
             .map(TaskRecord::taskDomain)
             .filter(d -> d != null && enricher.semanticallyEquivalent(taskDomain, d))
             .forEach(domains::add);

        // Group outcomes by agent for matching tasks
        Map<String, List<OutcomeRecord>> byAgent = new LinkedHashMap<>();
        for (TaskRecord t : tasks) {
            if (!t.capabilityTag().equals(capabilityTag)) continue;
            if (!t.tenancyId().equals(tenancyId)) continue;
            if (!domains.contains(t.taskDomain())) continue;
            outcomes.stream()
                    .filter(o -> o.taskId().equals(t.taskId()))
                    .forEach(o -> byAgent.computeIfAbsent(t.agentId(), k -> new ArrayList<>()).add(o));
        }

        return byAgent.entrySet().stream()
            .sorted(Comparator.comparingDouble(
                (Map.Entry<String, List<OutcomeRecord>> e) -> wilsonScore(e.getValue())).reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .toList();
    }

    public List<AttestationRecord> attestationsFor(String agentId, String tenancyId) {
        return attestations.stream()
            .filter(a -> a.agentId().equals(agentId) && a.tenancyId().equals(tenancyId))
            .toList();
    }

    // ── Wilson lower bound (z = 1.645, 90% CI) ────────────────────────────

    static double qualityScore(OutcomeRecord o) {
        double m = switch (o.result()) {
            case SUCCEEDED -> 1.0;
            case PARTIALLY -> 0.5;
            case FAILED    -> 0.0;
        };
        return o.confidence() * m;
    }

    static double wilsonScore(List<OutcomeRecord> outcomes) {
        int n = outcomes.size();
        if (n == 0) return 0.0;
        double p = outcomes.stream().mapToDouble(InMemoryAgentGraph::qualityScore).sum() / n;
        double z = 1.645;
        double z2 = z * z;
        double numerator = p + z2 / (2 * n)
                         - z * Math.sqrt((p * (1 - p) + z2 / (4.0 * n)) / n);
        return numerator / (1 + z2 / n);
    }

    // ── Sufficiency ────────────────────────────────────────────────────────

    static DataSufficiency computeSufficiency(int count, List<String> warnings) {
        SufficiencyLevel level = count >= 10 ? SufficiencyLevel.SUFFICIENT
                               : count >= 5  ? SufficiencyLevel.INDICATIVE
                               :               SufficiencyLevel.INSUFFICIENT;
        return new DataSufficiency(count, level, warnings);
    }

    private List<String> buildWarnings() {
        List<String> w = new ArrayList<>();
        if (enricher instanceof NoOpEnricher) {
            w.add("No TaskSemanticEnricher — personality axis correlation unavailable");
        }
        return w;
    }

    // ── Types ──────────────────────────────────────────────────────────────

    public enum TaskResult { SUCCEEDED, PARTIALLY, FAILED }
    public enum SufficiencyLevel { SUFFICIENT, INDICATIVE, INSUFFICIENT }

    public record TaskRecord(String taskId, String agentId, String tenancyId,
                             String capabilityTag, String taskDomain, String externalRef) {}
    public record OutcomeRecord(String taskId, TaskResult result, double confidence) {}
    public record AttestationRecord(String taskId, String agentId, String tenancyId,
                                    String ledgerHash, String entryType) {}
    public record DataSufficiency(int sampleCount, SufficiencyLevel level, List<String> warnings) {}
    public record AgentHistory(String agentId, String tenancyId,
                               List<TaskRecord> tasks, List<OutcomeRecord> outcomes,
                               List<AttestationRecord> attestationRefs,
                               DataSufficiency sufficiency) {}

    public interface TaskSemanticEnricher {
        Set<String> dispositionAxes(String capabilityTag, String taskDomain);
        boolean semanticallyEquivalent(String domainA, String domainB);
        OptionalInt significance(String capabilityTag, String taskDomain);
    }

    static class NoOpEnricher implements TaskSemanticEnricher {
        public Set<String> dispositionAxes(String cap, String domain) { return Set.of(); }
        public boolean semanticallyEquivalent(String a, String b) { return false; }
        public OptionalInt significance(String cap, String domain) { return OptionalInt.empty(); }
    }
}
