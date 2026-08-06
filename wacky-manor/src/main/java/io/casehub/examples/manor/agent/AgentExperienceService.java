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

    private static final Logger log = Logger.getLogger(AgentExperienceService.class);
    private static final MemoryDomain MANOR_DOMAIN = new MemoryDomain("manor");

    private final ExperienceRecorder recorder;
    private final CaseMemoryStore store;
    private final String tenantId;
    private long recallTimeoutMs = 2000;

    public AgentExperienceService(ExperienceRecorder recorder,
                                   CaseMemoryStore store, String tenantId) {
        this.recorder = recorder;
        this.store = store;
        this.tenantId = tenantId;
    }

    public void setRecallTimeoutMs(long ms) { this.recallTimeoutMs = ms; }

    public void ingest(String agentId, String room, String description, String thinking) {
        try {
            var metadata = new HashMap<String, String>();
            metadata.put("room", room);
            if (thinking != null) metadata.put("thinking", thinking);
            var event = new Action(agentId, tenantId, null, null,
                description, null, Map.copyOf(metadata), "manor-action");
            recorder.record(event);
        } catch (Exception e) {
            log.warnf("%s: experience ingest failed (non-fatal): %s", agentId, e.getMessage());
        }
    }

    public List<Memory> recall(String agentId, int limit) {
        try {
            var future = CompletableFuture.supplyAsync(() ->
                store.query(MemoryQuery.forEntity(agentId, MANOR_DOMAIN, tenantId)
                    .withLimit(limit)
                    .withOrder(MemoryOrder.CHRONOLOGICAL)));
            return future.get(recallTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.debugf("%s: experience recall timed out after %dms", agentId, recallTimeoutMs);
            return List.of();
        } catch (Exception e) {
            log.warnf("%s: experience recall failed (non-fatal): %s", agentId, e.getMessage());
            return List.of();
        }
    }
}
