package io.casehub.examples.manor.agent;

import io.casehub.blocks.summarisation.EventLevel;
import io.casehub.blocks.summarisation.LevelEvent;
import io.casehub.blocks.summarisation.Summariser;
import io.casehub.blocks.summarisation.observation.ObservationContext;
import io.casehub.blocks.summarisation.observation.ObservationTier;
import io.casehub.examples.manor.model.ActionType;
import io.casehub.examples.manor.model.ManorEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class ManorObservationRendererTest {

    static final EventLevel MANOR = new EventLevel("manor", 0);

    private LevelEvent<ManorEvent> dialogue(String charId, String text, long ts) {
        return new LevelEvent<>(new ManorEvent(Instant.ofEpochMilli(ts), "dialogue", charId,
                "kitchen", charId + ": " + text), ts, MANOR);
    }

    @Test
    void belowVerbatimThreshold_rendersVerbatim() {
        var renderer = new ManorObservationRenderer(new MechanicalCompactor(), 10, 15, null);
        var events = List.of(dialogue("penelope", "Hello!", 1000));
        var result = renderer.render(events, new ObservationContext(2000, 1000))
                .toCompletableFuture().join();
        assertThat(result.tier()).isEqualTo(ObservationTier.VERBATIM);
        assertThat(result.renderedText()).contains("Hello!");
    }

    @Test
    void aboveVerbatimBelowGrouped_rendersGrouped() {
        var renderer = new ManorObservationRenderer(new MechanicalCompactor(), 2, 15, null);
        var events = List.of(
                dialogue("penelope", "Hello!", 1000),
                dialogue("hooded-claw", "Greetings!", 1100),
                dialogue("peter", "Jolly good!", 1200));
        var result = renderer.render(events, new ObservationContext(2000, 1000))
                .toCompletableFuture().join();
        assertThat(result.tier()).isEqualTo(ObservationTier.GROUPED);
    }

    @Test
    void empty_returnsEmptyResult() {
        var renderer = new ManorObservationRenderer(new MechanicalCompactor(), 10, 15, null);
        var result = renderer.render(List.of(), new ObservationContext(2000, 1000))
                .toCompletableFuture().join();
        assertThat(result.eventCount()).isZero();
    }

    @Test
    void compactionReducesBelowThreshold_rendersVerbatim() {
        var renderer = new ManorObservationRenderer(new MechanicalCompactor(), 2, 15, null);
        var events = List.of(
                new LevelEvent<>(new ManorEvent(Instant.ofEpochMilli(100), "action", "penelope",
                        "kitchen", "Penelope moved.", ActionType.MOVE, "kitchen", null, "entrance-hall"), 100, MANOR),
                new LevelEvent<>(new ManorEvent(Instant.ofEpochMilli(200), "action", "penelope",
                        "ballroom", "Penelope moved.", ActionType.MOVE, "ballroom", null, "kitchen"), 200, MANOR),
                dialogue("hooded-claw", "Nyah!", 300));
        var result = renderer.render(events, new ObservationContext(1000, 500))
                .toCompletableFuture().join();
        assertThat(result.tier()).isEqualTo(ObservationTier.VERBATIM);
        assertThat(result.eventCount()).isEqualTo(2);
    }

    @Test
    void llmFailure_fallsBackToGroupedText() {
        Summariser<ManorEvent, String> failingSummariser = batch ->
                CompletableFuture.failedFuture(new RuntimeException("LLM timeout"));
        var renderer = new ManorObservationRenderer(new MechanicalCompactor(), 1, 2, failingSummariser);
        var events = List.of(
                dialogue("penelope", "Hello!", 1000),
                dialogue("hooded-claw", "Greetings!", 1100),
                dialogue("peter", "Jolly good!", 1200));
        var result = renderer.render(events, new ObservationContext(2000, 1000))
                .toCompletableFuture().join();
        assertThat(result.tier()).isEqualTo(ObservationTier.GROUPED);
        assertThat(result.renderedText()).contains("Hello!");
    }
}
