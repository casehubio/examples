package io.casehub.examples.manor.agent;

import io.casehub.blocks.summarisation.observation.ObservationAccumulator;
import io.casehub.blocks.summarisation.observation.ObservationRenderer;
import io.casehub.examples.manor.model.ManorEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CharacterObservationState {

    private final ConcurrentHashMap<String, ObservationAccumulator<ManorEvent>> accumulators
            = new ConcurrentHashMap<>();
    private final LinkedHashMap<String, RememberedRoom> rememberedDrainCache
            = new LinkedHashMap<>();
    private final ObservationRenderer<ManorEvent> renderer;

    public CharacterObservationState(String startRoom, ObservationRenderer<ManorEvent> renderer) {
        this.renderer = renderer;
        accumulators.computeIfAbsent(startRoom, r -> new ObservationAccumulator<>(renderer));
    }

    public ObservationAccumulator<ManorEvent> accumulatorFor(String roomId) {
        return accumulators.computeIfAbsent(roomId, r -> new ObservationAccumulator<>(renderer));
    }

    public Map<String, ObservationAccumulator<ManorEvent>> accumulators() {
        return accumulators;
    }

    public LinkedHashMap<String, RememberedRoom> rememberedDrainCache() {
        return rememberedDrainCache;
    }
}
