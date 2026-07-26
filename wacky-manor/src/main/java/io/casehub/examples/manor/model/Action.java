package io.casehub.examples.manor.model;

public record Action(ActionType type, String target, String withItem) {}
