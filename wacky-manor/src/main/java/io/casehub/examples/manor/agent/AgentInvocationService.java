package io.casehub.examples.manor.agent;

import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentSessionInit;
import io.casehub.platform.agent.session.ClearingPolicy;
import io.casehub.platform.agent.session.ManagedSession;
import io.casehub.platform.agent.session.SessionLifecycleManager;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public class AgentInvocationService {

    private static final Logger log = Logger.getLogger(AgentInvocationService.class);

    private final SessionLifecycleManager sessionManager;
    private final String backendKey;
    private final int timeoutSeconds;
    private final int maxRetries;
    private final long baseRetryDelayMs;

    private final AtomicLong totalCalls = new AtomicLong();
    private final AtomicLong retries = new AtomicLong();
    private final AtomicLong fallbacks = new AtomicLong();
    private final AtomicLong totalLatencyMs = new AtomicLong();

    public AgentInvocationService(SessionLifecycleManager sessionManager, String backendKey,
                                  int timeoutSeconds, int maxRetries, long baseRetryDelayMs) {
        this.sessionManager = sessionManager;
        this.backendKey = backendKey;
        this.timeoutSeconds = timeoutSeconds;
        this.maxRetries = maxRetries;
        this.baseRetryDelayMs = baseRetryDelayMs;
    }

    public AgentResponse invoke(String systemPrompt, String userPrompt, String agentId) {
        totalCalls.incrementAndGet();
        long start = System.currentTimeMillis();
        try {
            return callWithRetry(systemPrompt, userPrompt, agentId);
        } finally {
            totalLatencyMs.addAndGet(System.currentTimeMillis() - start);
        }
    }

    private AgentResponse callWithRetry(String systemPrompt, String userPrompt, String agentId) {
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            ManagedSession session = null;
            try {
                session = sessionManager.acquire(
                        "character:" + agentId, backendKey,
                        AgentSessionInit.of(systemPrompt),
                        ClearingPolicy.MANUAL, 0);

                var responseText = new StringBuilder();
                var querySession = session;
                var worker = Thread.ofVirtual().name(agentId + "-query").start(() ->
                    querySession.query(userPrompt).subscribe().asStream()
                        .filter(e -> e instanceof AgentEvent.TextDelta)
                        .map(e -> ((AgentEvent.TextDelta) e).text())
                        .forEach(responseText::append));
                worker.join(Duration.ofSeconds(timeoutSeconds + 60));
                if (worker.isAlive()) {
                    worker.interrupt();
                    System.err.println("[TIMEOUT] " + agentId + ": LLM timeout after " + (timeoutSeconds + 60) + "s (attempt " + (attempt + 1) + "/" + (maxRetries + 1) + ")");
                    throw new java.util.concurrent.TimeoutException(
                        "LLM response not received within " + (timeoutSeconds + 60) + "s");
                }
                System.err.println("[OK] " + agentId + ": LLM call completed (attempt " + (attempt + 1) + ")");
                return AgentResponse.parse(responseText.toString());
            } catch (Exception e) {
                System.err.println("[FAIL] " + agentId + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
                if (session != null) {
                    try { session.close(); } catch (Exception ce) { /* ignore */ }
                }
                if (attempt < maxRetries) {
                    retries.incrementAndGet();
                    sleepWithJitter(attempt);
                }
            }
        }
        log.warnf("%s: falling back to idle after %d attempts", agentId, maxRetries + 1);
        fallbacks.incrementAndGet();
        return AgentResponse.idle();
    }

    private void sleepWithJitter(int attempt) {
        long delay = baseRetryDelayMs * (1L << attempt);
        long jitter = ThreadLocalRandom.current().nextLong(0, baseRetryDelayMs);
        try {
            Thread.sleep(delay + jitter);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public InvocationMetrics metrics() {
        return new InvocationMetrics(totalCalls.get(), retries.get(),
            fallbacks.get(), totalLatencyMs.get());
    }

    public static String querySessionRaw(io.casehub.platform.agent.session.ManagedSession session, String userPrompt, String agentId) {
        var responseText = new StringBuilder();
        session.query(userPrompt).subscribe().asStream()
               .filter(e -> e instanceof io.casehub.platform.agent.AgentEvent.TextDelta)
               .map(e -> ((io.casehub.platform.agent.AgentEvent.TextDelta) e).text())
               .forEach(responseText::append);
        System.err.println("[OK] " + agentId + ": LLM call completed");
        return responseText.toString();
    }


    public record InvocationMetrics(long totalCalls, long retries,
                                     long fallbacks, long totalLatencyMs) {
        public long averageLatencyMs() {
            return totalCalls > 0 ? totalLatencyMs / totalCalls : 0;
        }
    }
}
