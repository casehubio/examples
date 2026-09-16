package io.casehub.examples.manor.agent;

import io.casehub.neocortex.cognitive.index.AgentPair;
import io.casehub.neocortex.cognitive.index.PadDimension;
import io.casehub.neocortex.cognitive.index.PerspectivalComparison;
import io.casehub.neocortex.cognitive.index.TrendAgreement;
import io.casehub.platform.api.identity.PrincipalId;

import java.util.Optional;

public final class PerceptionTranslator {

    static final double DISTANCE_THRESHOLD = 0.4;

    private PerceptionTranslator() {}

    public static Optional<String> translate(
            PerspectivalComparison comparison,
            PrincipalId self,
            PrincipalId other,
            String otherName,
            String stage) {

        if ("stranger".equals(stage)) {
            return Optional.empty();
        }

        if (comparison.unassessedAgents().contains(self)
            || comparison.unassessedAgents().contains(other)) {
            return Optional.empty();
        }

        var    pair     = AgentPair.of(self, other);
        double distance = comparison.distances().distance(self, other);
        if (distance < DISTANCE_THRESHOLD) {
            return Optional.empty();
        }

        PadDimension dominant = dominantDimension(comparison, self, other);
        double diff = comparison.dimensionDifferences().get(dominant)
                                .difference(self, other);

        String statement = switch (dominant) {
            case PLEASURE -> diff < 0
                             ? otherName + " seems to view this more positively than you do"
                             : "You feel more positively about " + otherName + " than they feel about themselves";
            case AROUSAL -> diff > 0
                            ? "You're more alert around " + otherName + " than they seem to be"
                            : otherName + " seems more on edge than you'd expect";
            case DOMINANCE -> diff > 0
                              ? "You feel more in control around " + otherName + " than they do"
                              : otherName + " seems more confident in this interaction than you are";
        };

        int     stageOrd          = ManorContextStrategy.stageOrdinal(stage);
        boolean includeTrajectory = stageOrd >= ManorContextStrategy.stageOrdinal("friend");

        if (includeTrajectory) {
            var trajectory = comparison.trajectoryAlignment();
            var agreement  = trajectory.agreements().get(pair);
            if (agreement == TrendAgreement.DIVERGENT) {
                statement += " (and this gap is widening)";
            } else if (agreement == TrendAgreement.ALIGNED) {
                statement += " (though you're converging)";
            }
        }

        return Optional.of(otherName + " (" + stage + "): " + statement);
    }

    private static PadDimension dominantDimension(
            PerspectivalComparison comparison, PrincipalId self, PrincipalId other) {
        PadDimension dominant = PadDimension.PLEASURE;
        double maxAbs = 0;
        for (PadDimension dim : PadDimension.values()) {
            var diffs = comparison.dimensionDifferences().get(dim);
            if (diffs != null) {
                double abs = Math.abs(diffs.difference(self, other));
                if (abs > maxAbs) {
                    maxAbs = abs;
                    dominant = dim;
                }
            }
        }
        return dominant;
    }
}
