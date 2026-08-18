package io.casehub.examples.manor.agent;

import io.casehub.neocortex.memory.cbr.AgentTrustProvider;

import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.DoubleAdder;

public class ManorTrustProvider implements AgentTrustProvider {

    private final ConcurrentHashMap<String, DoubleAdder> scores = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicInteger> counts = new ConcurrentHashMap<>();
    private final double positiveWeight;
    private final double negativeWeight;

    public ManorTrustProvider(double positiveWeight, double negativeWeight) {
        this.positiveWeight = positiveWeight;
        this.negativeWeight = negativeWeight;
    }

    public void recordPositive(String agentId) {
        scores.computeIfAbsent(agentId, k -> new DoubleAdder()).add(positiveWeight);
        counts.computeIfAbsent(agentId, k -> new java.util.concurrent.atomic.AtomicInteger()).incrementAndGet();
    }

    public void recordNegative(String agentId) {
        scores.computeIfAbsent(agentId, k -> new DoubleAdder()).add(negativeWeight);
        counts.computeIfAbsent(agentId, k -> new java.util.concurrent.atomic.AtomicInteger()).incrementAndGet();
    }

    @Override
    public OptionalDouble currentTrustScore(String agentId) {
        var adder = scores.get(agentId);
        var count = counts.get(agentId);
        if (adder == null || count == null || count.get() == 0) {
            return OptionalDouble.of(0.5);
        }
        double raw = adder.sum();
        double maxMagnitude = count.get() * Math.max(Math.abs(positiveWeight), Math.abs(negativeWeight));
        double normalized = 0.5 + (raw / (2.0 * maxMagnitude));
        return OptionalDouble.of(Math.clamp(normalized, 0.0, 1.0));
    }
}
