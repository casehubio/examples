package io.casehub.examples.manor.agent;

import io.casehub.blocks.summarisation.observation.ObservationResult;

public record RememberedRoom(ObservationResult result, long cachedAt) {}
