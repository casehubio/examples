package io.casehub.examples.manor.model;

import java.util.List;

public record Trigger(
        String id,
        TriggerCondition condition,
        List<TriggerEffect> effects,
        boolean once) {}
