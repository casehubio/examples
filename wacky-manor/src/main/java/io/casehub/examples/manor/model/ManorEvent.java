package io.casehub.examples.manor.model;

import java.time.Instant;

public record ManorEvent(
        Instant timestamp,
        String type,
        String characterId,
        String room,
        String description) {}
