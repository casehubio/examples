package io.casehub.examples.manor.engine;

import io.casehub.neocortex.memory.experience.ContentScorer;
import io.casehub.neocortex.memory.experience.ScoreableContent;

import java.util.Map;

public final class ActionImportanceScorer implements ContentScorer {

    private static final Map<String, Double> EVENT_WEIGHTS = Map.of(
        "conflict_resolution", 0.9,
        "trust_change", 0.8,
        "social_interaction", 0.6,
        "observation", 0.4,
        "idle", 0.1);

    private static final double DEFAULT_WEIGHT = 0.3;

    @Override
    public double score(ScoreableContent content) {
        String eventType = content.metadata().getOrDefault("event-type", "unknown");
        return EVENT_WEIGHTS.getOrDefault(eventType, DEFAULT_WEIGHT);
    }
}
