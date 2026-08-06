package io.casehub.examples.manor.experiment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.casehub.examples.manor.agent.AgentInvocationService;

import java.nio.file.Path;
import java.util.Arrays;

public record ScaleReport(
    TranscriptRecorder.RunResult baseResult,
    int agentCount,
    long avgTurnLatencyMs,
    long p95TurnLatencyMs,
    long p99TurnLatencyMs,
    long totalLlmCalls,
    long llmRetries,
    long llmFallbacks,
    long avgLlmLatencyMs) {

    private static final ObjectMapper JSON = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    public static ScaleReport from(TranscriptRecorder.RunResult result,
                                    AgentInvocationService.InvocationMetrics metrics,
                                    int agentCount,
                                    long[] turnLatenciesMs) {
        Arrays.sort(turnLatenciesMs);
        int len = turnLatenciesMs.length;
        long avg = len > 0 ? Arrays.stream(turnLatenciesMs).sum() / len : 0;
        long p95 = len > 0 ? turnLatenciesMs[(int) (len * 0.95)] : 0;
        long p99 = len > 0 ? turnLatenciesMs[(int) (len * 0.99)] : 0;

        return new ScaleReport(result, agentCount, avg, p95, p99,
            metrics.totalCalls(), metrics.retries(), metrics.fallbacks(),
            metrics.averageLatencyMs());
    }

    public void writeJson(Path file) throws Exception {
        JSON.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), this);
    }

    public String summary() {
        return String.format(
            "ScaleReport: %d agents, %d turns, %dms avg latency (p95=%dms p99=%dms), " +
            "%d LLM calls (%d retries, %d fallbacks), %dms avg LLM latency",
            agentCount, baseResult.totalTurns(), avgTurnLatencyMs, p95TurnLatencyMs, p99TurnLatencyMs,
            totalLlmCalls, llmRetries, llmFallbacks, avgLlmLatencyMs);
    }
}
