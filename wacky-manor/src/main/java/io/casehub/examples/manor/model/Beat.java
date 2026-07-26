package io.casehub.examples.manor.model;

import java.util.List;
import java.util.Map;

public record Beat(
        String id,
        String narration,
        Map<String, String> prompts,
        boolean aside,
        List<BeatAlternative> alternatives,
        Map<String, Object> mechanicalEffect,
        boolean waitIfNoneMatch) {

    public Beat {
        prompts = prompts != null ? Map.copyOf(prompts) : Map.of();
        alternatives = alternatives != null ? List.copyOf(alternatives) : List.of();
    }

    public boolean hasAlternatives() {
        return !alternatives.isEmpty();
    }

    public record BeatAlternative(
            String id,
            TriggerCondition condition,
            String narration,
            Map<String, String> prompts,
            Map<String, Object> mechanicalEffect) {

        public BeatAlternative {
            prompts = prompts != null ? Map.copyOf(prompts) : Map.of();
        }
    }
}
