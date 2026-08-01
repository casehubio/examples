package io.casehub.examples.manor.agent;

import io.casehub.blocks.summarisation.observation.ObservationResult;

import java.util.Map;

public record ObservationDrain(
        ObservationResult currentRoom,
        Map<String, RememberedRoom> rememberedRooms) {}
