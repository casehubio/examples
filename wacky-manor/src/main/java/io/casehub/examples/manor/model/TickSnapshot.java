package io.casehub.examples.manor.model;

import io.casehub.examples.manor.agent.AgentResponse;
import io.casehub.examples.manor.engine.WorldState;

import java.util.List;
import java.util.Map;

public record TickSnapshot(int tick, Map<String, AgentResponse> responses,
                           List<ManorEvent> events, WorldState worldView) {}
