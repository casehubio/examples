package io.casehub.examples.manor.agent;

import io.casehub.neocortex.memory.CaseMemoryStore;
import io.casehub.neocortex.memory.Memory;
import io.casehub.neocortex.memory.MemoryDomain;
import io.casehub.neocortex.memory.MemoryOrder;
import io.casehub.neocortex.memory.MemoryQuery;
import io.casehub.neocortex.memory.experience.Action;
import io.casehub.neocortex.memory.experience.ExperienceRecorder;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AgentExperienceService {

    private static final Logger       log          = Logger.getLogger(AgentExperienceService.class);
    private static final MemoryDomain MANOR_DOMAIN = new MemoryDomain("manor");

    private final ExperienceRecorder                                                recorder;
    private final CaseMemoryStore                                                   store;
    private final String                                                            tenantId;
    private       long                                                              recallTimeoutMs    = 2000;
    private       int                                                               recallLimit        = 20;
    private final io.casehub.neocortex.memory.reflection.ReflectionSynthesizer      synthesizer;
    private final ManorReflectionTrigger                                            reflectionTrigger;
    private final boolean                                                           reflectionEnabled;
    private final boolean                                                           decayEnabled;
    private final int                                                               decayMaxAgeDays;
    private final double                                                            decayMinImportance;
    private final int                                                               maxSourceMemories;
    private final java.util.concurrent.ConcurrentHashMap<String, java.time.Instant> lastReflectionTime = new java.util.concurrent.ConcurrentHashMap<>();
    private final ManorGoalEvaluator                                                goalEvaluator;
    private final ManorPlanEvaluator                                                planEvaluator;


    public AgentExperienceService(ExperienceRecorder recorder,
                                  CaseMemoryStore store, String tenantId) {
        this(recorder, store, tenantId, null, null, false, false, 7, 0.2, 15, 20, null, null);
    }

    public AgentExperienceService(ExperienceRecorder recorder,
                                  CaseMemoryStore store, String tenantId,
                                  io.casehub.neocortex.memory.reflection.ReflectionSynthesizer synthesizer,
                                  ManorReflectionTrigger reflectionTrigger,
                                  boolean reflectionEnabled,
                                  boolean decayEnabled, int decayMaxAgeDays, double decayMinImportance,
                                  int maxSourceMemories, int recallLimit) {
        this(recorder, store, tenantId, synthesizer, reflectionTrigger, reflectionEnabled,
             decayEnabled, decayMaxAgeDays, decayMinImportance, maxSourceMemories, recallLimit, null, null);
    }

    public AgentExperienceService(ExperienceRecorder recorder,
                                  CaseMemoryStore store, String tenantId,
                                  io.casehub.neocortex.memory.reflection.ReflectionSynthesizer synthesizer,
                                  ManorReflectionTrigger reflectionTrigger,
                                  boolean reflectionEnabled,
                                  boolean decayEnabled, int decayMaxAgeDays, double decayMinImportance,
                                  int maxSourceMemories, int recallLimit,
                                  ManorGoalEvaluator goalEvaluator) {
        this(recorder, store, tenantId, synthesizer, reflectionTrigger, reflectionEnabled,
             decayEnabled, decayMaxAgeDays, decayMinImportance, maxSourceMemories, recallLimit, goalEvaluator, null);
    }

    public AgentExperienceService(ExperienceRecorder recorder,
                                  CaseMemoryStore store, String tenantId,
                                  io.casehub.neocortex.memory.reflection.ReflectionSynthesizer synthesizer,
                                  ManorReflectionTrigger reflectionTrigger,
                                  boolean reflectionEnabled,
                                  boolean decayEnabled, int decayMaxAgeDays, double decayMinImportance,
                                  int maxSourceMemories, int recallLimit,
                                  ManorGoalEvaluator goalEvaluator,
                                  ManorPlanEvaluator planEvaluator) {
        this.recorder           = recorder;
        this.store              = store;
        this.tenantId           = tenantId;
        this.synthesizer        = synthesizer;
        this.reflectionTrigger  = reflectionTrigger;
        this.reflectionEnabled  = reflectionEnabled;
        this.decayEnabled       = decayEnabled;
        this.decayMaxAgeDays    = decayMaxAgeDays;
        this.decayMinImportance = decayMinImportance;
        this.maxSourceMemories  = maxSourceMemories;
        this.recallLimit        = recallLimit;
        this.goalEvaluator      = goalEvaluator;
        this.planEvaluator      = planEvaluator;
    }


    public void setRecallTimeoutMs(long ms) {this.recallTimeoutMs = ms;}

    public void setRecallLimit(int limit)   {this.recallLimit = limit;}


    public void ingest(String agentId, String room, String description, String thinking) {
        ingest(agentId, room, description, thinking, 0.5, null, 0);
    }

    public void ingest(String agentId, String room, String description,
                       String thinking, double importance) {
        ingest(agentId, room, description, thinking, importance, null, 0);
    }

    public void ingest(String agentId, String room, String description,
                       String thinking, double importance, String targetAgentId) {
        ingest(agentId, room, description, thinking, importance, targetAgentId, 0);
    }

    public void ingest(String agentId, String room, String description,
                       String thinking, double importance, String targetAgentId, int currentTick) {
        try {
            var metadata = new HashMap<String, String>();
            metadata.put("room", room);
            if (thinking != null) {metadata.put("thinking", thinking);}
            if (targetAgentId != null) {
                metadata.put(io.casehub.neocortex.memory.experience.ExperienceAttributeKeys.TARGET_AGENT, targetAgentId);
            }
            var event = new Action(agentId, tenantId, null, null,
                                   description, importance, Map.copyOf(metadata), "manor-action");
            recorder.record(event);
            if (reflectionEnabled && reflectionTrigger != null
                && reflectionTrigger.shouldReflect(agentId, importance)) {
                var since = lastReflectionTime.getOrDefault(agentId, java.time.Instant.EPOCH);
                int tick  = currentTick;
                Thread.ofVirtual().name(agentId + "-reflect").start(() -> {
                    try {
                        runReflection(agentId, since, tick);
                        reflectionTrigger.reset(agentId);
                        lastReflectionTime.put(agentId, java.time.Instant.now());
                    } catch (Exception ex) {
                        log.warnf("%s: reflection failed (non-fatal): %s",
                                  agentId, ex.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            log.warnf("%s: experience ingest failed (non-fatal): %s",
                      agentId, e.getMessage());
        }
    }


    public List<Memory> recall(String agentId, int limit) {
        try {
            var future = CompletableFuture.supplyAsync(() ->
                                                               store.query(MemoryQuery.forEntity(agentId, MANOR_DOMAIN, tenantId)
                                                                                      .withLimit(limit)
                                                                                      .withOrder(MemoryOrder.SALIENCE)));
            return future.get(recallTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.debugf("%s: experience recall timed out after %dms", agentId, recallTimeoutMs);
            return List.of();
        } catch (Exception e) {
            log.warnf("%s: experience recall failed (non-fatal): %s", agentId, e.getMessage());
            return List.of();
        }
    }

    public List<Memory> recallRelationships(String agentId, String otherAgentId, int limit) {
        try {
            var query = io.casehub.neocortex.memory.relationship.RelationshipQuery
                                .forPair(agentId, otherAgentId, tenantId)
                                .withLimit(limit)
                                .withOrder(MemoryOrder.SALIENCE);
            return store.query(query);
        } catch (Exception e) {
            log.warnf("%s: relationship recall failed (non-fatal): %s", agentId, e.getMessage());
            return List.of();
        }
    }

    public List<Memory> recallReflections(String agentId, int limit) {
        try {
            return store.query(MemoryQuery.forEntity(agentId,
                                                     io.casehub.neocortex.memory.reflection.ReflectionEvents.DOMAIN, tenantId)
                                          .withLimit(limit)
                                          .withOrder(MemoryOrder.SALIENCE));
        } catch (Exception e) {
            log.warnf("%s: reflection recall failed (non-fatal): %s",
                      agentId, e.getMessage());
            return List.of();
        }
    }


    private void runReflection(String agentId, java.time.Instant since, int currentTick) {
        var sources = store.query(MemoryQuery.forEntity(agentId, MANOR_DOMAIN, tenantId)
                                             .withLimit(maxSourceMemories)
                                             .withSince(since)
                                             .withOrder(MemoryOrder.SALIENCE));
        if (sources.isEmpty()) {return;}
        var events = synthesizer.synthesize(agentId, tenantId, sources, 1);
        for (var event : events) {
            store.store(io.casehub.neocortex.memory.reflection.ReflectionEvents.toMemoryInput(event));
        }
        if (goalEvaluator != null && !events.isEmpty()) {
            var insightTexts = events.stream()
                                     .map(io.casehub.neocortex.memory.reflection.ReflectionEvent::insight)
                                     .toList();
            goalEvaluator.evaluate(agentId, currentTick, insightTexts, Map.of());
            if (planEvaluator != null) {
                planEvaluator.reviseOnReflection(agentId, insightTexts, currentTick);
            }
        }
        if (decayEnabled) {
            store.purge(new io.casehub.neocortex.memory.MemoryRetentionPolicy(
                    tenantId, MANOR_DOMAIN, decayMaxAgeDays, decayMinImportance));
        }
    }


}
