package io.casehub.examples.manor.agent;

import io.casehub.examples.manor.engine.WorldState;
import io.casehub.examples.manor.model.CharacterState;
import io.casehub.platform.agent.session.SessionPipeline;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class TickExecutor {

    private static final Logger log = Logger.getLogger(TickExecutor.class);

    public interface ObservationBuilder {
        String build(CharacterState character, WorldState world);
    }

    public static Map<String, String> buildObservationsParallel(
            List<CharacterState> characters,
            WorldState world,
            ObservationBuilder builder,
            Duration timeout) {
        var prompts = new ConcurrentHashMap<String, String>();
        var latch = new CountDownLatch(characters.size());

        for (var c : characters) {
            Thread.ofVirtual().name(c.agentId() + "-obs").start(() -> {
                try {
                    String observation = builder.build(c, world);
                    if (observation != null) {
                        prompts.put(c.agentId(), observation);
                    }
                } catch (Exception e) {
                    log.errorf(e, "%s: observation build failed", c.agentId());
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            if (!latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                log.warnf("Observation build timed out — %d of %d completed",
                        prompts.size(), characters.size());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return prompts;
    }

    public static Map<String, String> queryPipeline(
            Map<String, String> prompts,
            SessionPipeline pipeline,
            int maxAttempts,
            Duration timeout) {
        return pipeline.executeBatch(
                prompts.keySet(),
                (agentId, session) -> AgentInvocationService.querySessionRaw(
                        session, prompts.get(agentId), agentId),
                maxAttempts, timeout);
    }

    public static Map<String, AgentResponse> parseResponses(
            List<CharacterState> characters,
            Map<String, String> rawResults) {
        var responses = new java.util.HashMap<String, AgentResponse>();
        for (var c : characters) {
            String raw = rawResults.get(c.agentId());
            try {
                responses.put(c.agentId(), raw != null ? AgentResponse.parse(raw) : AgentResponse.idle());
            } catch (Exception e) {
                log.warnf("%s: failed to parse LLM response: %s", c.agentId(), e.getMessage());
                responses.put(c.agentId(), AgentResponse.idle());
            }
        }
        return responses;
    }
}
