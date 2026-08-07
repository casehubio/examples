package io.casehub.examples.manor.agent;

import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSessionConfig;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class AgentInvocationService {

    private static final Logger log = Logger.getLogger(AgentInvocationService.class);

    private final AgentProvider agentProvider;
    private final int timeoutSeconds;
    private final int maxRetries;
    private final long baseRetryDelayMs;

    private final AtomicLong totalCalls = new AtomicLong();
    private final AtomicLong retries = new AtomicLong();
    private final AtomicLong fallbacks = new AtomicLong();
    private final AtomicLong totalLatencyMs = new AtomicLong();

    public AgentInvocationService(AgentProvider agentProvider,
                                  int timeoutSeconds,
                                   int maxRetries, long baseRetryDelayMs) {
        this.agentProvider = agentProvider;
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
            try {
                String text = agentProvider.invoke(
                        AgentSessionConfig.of(systemPrompt, userPrompt,
                            Duration.ofSeconds(timeoutSeconds)))
                    .filter(e -> e instanceof AgentEvent.TextDelta)
                    .map(e -> ((AgentEvent.TextDelta) e).text())
                    .collect().with(Collectors.joining())
                    .await().atMost(Duration.ofSeconds(timeoutSeconds + 60));
                return AgentResponse.parse(text);
            } catch (Exception e) {
                log.warnf("%s: LLM call failed (attempt %d/%d): %s",
                    agentId, attempt + 1, maxRetries + 1, e.getMessage());
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

    public record InvocationMetrics(long totalCalls, long retries,
                                     long fallbacks, long totalLatencyMs) {
        public long averageLatencyMs() {
            return totalCalls > 0 ? totalLatencyMs / totalCalls : 0;
        }
    }
}
