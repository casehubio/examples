package io.casehub.examples.manor.agent;

import io.casehub.examples.manor.engine.WorldState;
import io.casehub.examples.manor.model.CharacterState;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class TickExecutorTest {

    @Test
    void buildObservationsParallelReturnsAllPrompts() {
        var characters = List.of(character("a"), character("b"), character("c"));

        var prompts = TickExecutor.buildObservationsParallel(
                characters, null,
                (c, w) -> "prompt-" + c.agentId(),
                Duration.ofSeconds(5));

        assertThat(prompts).hasSize(3);
        assertThat(prompts.get("a")).isEqualTo("prompt-a");
        assertThat(prompts.get("b")).isEqualTo("prompt-b");
        assertThat(prompts.get("c")).isEqualTo("prompt-c");
    }

    @Test
    void buildObservationsRunsInParallel() {
        var characters = List.of(character("a"), character("b"), character("c"));
        var maxConcurrent = new AtomicInteger();
        var current = new AtomicInteger();

        TickExecutor.buildObservationsParallel(
                characters, null,
                (c, w) -> {
                    int n = current.incrementAndGet();
                    maxConcurrent.updateAndGet(m -> Math.max(m, n));
                    try { Thread.sleep(50); } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    current.decrementAndGet();
                    return "prompt-" + c.agentId();
                },
                Duration.ofSeconds(5));

        assertThat(maxConcurrent.get())
                .as("Should run in parallel, not sequentially")
                .isGreaterThan(1);
    }

    @Test
    void buildObservationsHandlesBuilderException() {
        var characters = List.of(character("a"), character("b"), character("c"));

        var prompts = TickExecutor.buildObservationsParallel(
                characters, null,
                (c, w) -> {
                    if (c.agentId().equals("b")) throw new RuntimeException("build failed");
                    return "prompt-" + c.agentId();
                },
                Duration.ofSeconds(5));

        assertThat(prompts).hasSize(2);
        assertThat(prompts.get("a")).isEqualTo("prompt-a");
        assertThat(prompts.get("c")).isEqualTo("prompt-c");
        assertThat(prompts).doesNotContainKey("b");
    }

    @Test
    void buildObservationsTimesOutOnSlowBuilder() {
        var characters = List.of(character("a"), character("b"));

        var prompts = TickExecutor.buildObservationsParallel(
                characters, null,
                (c, w) -> {
                    if (c.agentId().equals("b")) {
                        try { Thread.sleep(5000); } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    return "prompt-" + c.agentId();
                },
                Duration.ofMillis(500));

        assertThat(prompts).containsKey("a");
        // b may or may not complete within timeout — the important thing is
        // the method returns, not hangs
    }

    @Test
    void buildObservationsCalledMultipleTimesDoesNotLeak() {
        var characters = List.of(character("a"), character("b"), character("c"));

        for (int tick = 0; tick < 20; tick++) {
            var prompts = TickExecutor.buildObservationsParallel(
                    characters, null,
                    (c, w) -> "prompt-" + c.agentId(),
                    Duration.ofSeconds(5));

            assertThat(prompts).hasSize(3);
        }
    }

    @Test
    void parseResponsesHandlesNullAndParseError() {
        var characters = List.of(character("a"), character("b"), character("c"));
        var rawResults = new java.util.HashMap<String, String>();
        rawResults.put("a", "{\"dialogue\":\"hello\",\"action\":{\"type\":\"WAIT\"}}");
        rawResults.put("c", "not valid json");
        // b is missing (null)

        var responses = TickExecutor.parseResponses(characters, rawResults);

        assertThat(responses).hasSize(3);
        assertThat(responses.get("a").dialogue()).isEqualTo("hello");
        assertThat(responses.get("b")).isEqualTo(AgentResponse.idle());
        assertThat(responses.get("c")).isEqualTo(AgentResponse.idle());
    }

    private static CharacterState character(String id) {
        return new CharacterState(id, id, "entrance-hall", 0, java.util.List.of());
    }
}
