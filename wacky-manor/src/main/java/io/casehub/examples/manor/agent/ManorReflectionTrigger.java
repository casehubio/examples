package io.casehub.examples.manor.agent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;

public final class ManorReflectionTrigger {

    private final int maxUnreflected;
    private final double importanceThreshold;
    private final ConcurrentHashMap<String, AtomicInteger> counts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DoubleAdder> importance = new ConcurrentHashMap<>();

    public ManorReflectionTrigger(int maxUnreflected, double importanceThreshold) {
        this.maxUnreflected = maxUnreflected;
        this.importanceThreshold = importanceThreshold;
    }

    public boolean shouldReflect(String agentId, double actionImportance) {
        int count = counts.computeIfAbsent(agentId, k -> new AtomicInteger())
                         .incrementAndGet();
        DoubleAdder imp = importance.computeIfAbsent(agentId, k -> new DoubleAdder());
        imp.add(actionImportance);
        return count >= maxUnreflected || imp.sum() >= importanceThreshold;
    }

    public void reset(String agentId) {
        counts.computeIfAbsent(agentId, k -> new AtomicInteger()).set(0);
        importance.computeIfAbsent(agentId, k -> new DoubleAdder()).reset();
    }
}
