package io.casehub.examples.manor.agent;

import io.casehub.blocks.summarisation.LevelEvent;
import io.casehub.blocks.summarisation.Summariser;
import io.casehub.examples.manor.model.ManorEvent;
import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSessionConfig;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public final class ManorLlmSummariser implements Summariser<ManorEvent, String> {

    private static final Logger log = Logger.getLogger(ManorLlmSummariser.class);

    private static final String SYSTEM_PROMPT = """
            You are a concise event summariser for a cartoon mansion game.
            Summarise the following events into a brief narrative paragraph.
            Preserve all character names, item names, and factual details.
            Compress dialogue exchanges into summaries.
            Do not invent events that did not happen.
            Respond with ONLY the summary text, no formatting.""";

    private final AgentProvider agentProvider;

    public ManorLlmSummariser(AgentProvider agentProvider) {
        this.agentProvider = agentProvider;
    }

    @Override
    public CompletionStage<List<String>> summarise(List<LevelEvent<ManorEvent>> batch) {
        String eventText = batch.stream()
                .map(e -> e.payload().description())
                .collect(Collectors.joining("\n"));
        try {
            String summary = agentProvider.invoke(
                            AgentSessionConfig.of(SYSTEM_PROMPT, eventText, Duration.ofSeconds(30)))
                    .filter(e -> e instanceof AgentEvent.TextDelta)
                    .map(e -> ((AgentEvent.TextDelta) e).text())
                    .collect().with(Collectors.joining())
                    .await().atMost(Duration.ofSeconds(60));
            return CompletableFuture.completedFuture(List.of(summary));
        } catch (Exception e) {
            log.warnf("LLM summarisation failed, falling back to raw text: %s", e.getMessage());
            return CompletableFuture.completedFuture(
                    batch.stream().map(ev -> ev.payload().description()).toList());
        }
    }
}
