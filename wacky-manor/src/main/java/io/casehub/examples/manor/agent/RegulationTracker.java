package io.casehub.examples.manor.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RegulationTracker {

    public enum Trajectory { IMPROVING, STABLE, WORSENING }

    public record TrajectoryResult(Trajectory trajectory, double delta) {}

    private static final double THRESHOLD = 0.05;
    private static final int DISTRESSED_INDEX = 2;

    private final Map<String, List<double[]>> history = new HashMap<>();

    public void recordPosterior(String targetId, double[] posterior) {
        history.computeIfAbsent(targetId, k -> new ArrayList<>()).add(posterior.clone());
    }

    public Optional<TrajectoryResult> computeTrajectory(String targetId) {
        var entries = history.get(targetId);
        if (entries == null || entries.size() < 2) { return Optional.empty(); }
        double prev = entries.get(entries.size() - 2)[DISTRESSED_INDEX];
        double curr = entries.get(entries.size() - 1)[DISTRESSED_INDEX];
        double delta = curr - prev;
        Trajectory t = delta < -THRESHOLD ? Trajectory.IMPROVING
                     : delta > THRESHOLD ? Trajectory.WORSENING
                     : Trajectory.STABLE;
        return Optional.of(new TrajectoryResult(t, delta));
    }

    public static io.casehub.neocortex.mindmap.intelligence.tom.BehaviorType classifyBehavior(AgentResponse response) {
        if (response.dialogue() != null && !response.dialogue().isBlank()) {
            return io.casehub.neocortex.mindmap.intelligence.tom.BehaviorType.APPROACH;
        }
        var action = response.action();
        if (action != null) {
            return switch (action.type()) {
                case GIVE, PULL_ASIDE -> io.casehub.neocortex.mindmap.intelligence.tom.BehaviorType.APPROACH;
                case LOOK, TAKE, USE, INTERACT -> io.casehub.neocortex.mindmap.intelligence.tom.BehaviorType.NEUTRAL;
                case WAIT, MOVE, STEAL -> io.casehub.neocortex.mindmap.intelligence.tom.BehaviorType.AVOIDANT;
            };
        }
        return io.casehub.neocortex.mindmap.intelligence.tom.BehaviorType.AVOIDANT;
    }

}
