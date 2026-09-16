package io.casehub.examples.manor.agent;

import io.casehub.neocortex.cognitive.index.AffectSnapshot;
import io.casehub.neocortex.cognitive.index.AgentPair;
import io.casehub.neocortex.cognitive.index.PadDimension;
import io.casehub.neocortex.cognitive.index.PadDistanceMatrix;
import io.casehub.neocortex.cognitive.index.PairwiseDifferences;
import io.casehub.neocortex.cognitive.index.PerspectivalComparison;
import io.casehub.neocortex.cognitive.index.TrajectoryAlignment;
import io.casehub.neocortex.cognitive.index.TrendAgreement;
import io.casehub.platform.api.identity.PrincipalId;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PerceptionTranslatorTest {

    private static final PrincipalId SELF = PrincipalId.agent("hooded-claw");
    private static final PrincipalId OTHER = PrincipalId.agent("peter-perfect");
    private static final String OTHER_NAME = "Peter Perfect";

    @Test
    void pleasureDivergenceMyLowerProducesCorrectStatement() {
        var comparison = buildComparison(0.2, 0.8, 0.5, 0.5, 0.5, 0.5);
        var result = PerceptionTranslator.translate(comparison, SELF, OTHER, OTHER_NAME, "friend");
        assertThat(result).isPresent();
        assertThat(result.get()).contains("Peter Perfect").contains("more positively");
    }

    @Test
    void pleasureDivergenceMyHigherProducesCorrectStatement() {
        var comparison = buildComparison(0.8, 0.2, 0.5, 0.5, 0.5, 0.5);
        var result = PerceptionTranslator.translate(comparison, SELF, OTHER, OTHER_NAME, "friend");
        assertThat(result).isPresent();
        assertThat(result.get()).contains("more positively about Peter Perfect");
    }

    @Test
    void arousalDivergenceProducesCorrectStatement() {
        var comparison = buildComparison(0.5, 0.5, 0.9, 0.3, 0.5, 0.5);
        var result = PerceptionTranslator.translate(comparison, SELF, OTHER, OTHER_NAME, "friend");
        assertThat(result).isPresent();
        assertThat(result.get()).contains("alert");
    }

    @Test
    void dominanceDivergenceProducesCorrectStatement() {
        var comparison = buildComparison(0.5, 0.5, 0.5, 0.5, 0.9, 0.3);
        var result = PerceptionTranslator.translate(comparison, SELF, OTHER, OTHER_NAME, "friend");
        assertThat(result).isPresent();
        assertThat(result.get()).contains("control");
    }

    @Test
    void belowThresholdReturnsEmpty() {
        var comparison = buildComparison(0.5, 0.6, 0.5, 0.5, 0.5, 0.5);
        var result = PerceptionTranslator.translate(comparison, SELF, OTHER, OTHER_NAME, "friend");
        assertThat(result).isEmpty();
    }

    @Test
    void unassessedAgentReturnsEmpty() {
        var comparison = new PerspectivalComparison(
                "peter-node", OTHER_NAME,
                Map.of(SELF, new AffectSnapshot(SELF, null, null, null, null),
                       OTHER, new AffectSnapshot(OTHER, 0.5, 0.5, 0.5, null)),
                Set.of(SELF),
                new PadDistanceMatrix(Map.of()),
                Map.of(),
                new TrajectoryAlignment(Map.of(), Map.of()),
                2);
        var result = PerceptionTranslator.translate(comparison, SELF, OTHER, OTHER_NAME, "friend");
        assertThat(result).isEmpty();
    }

    @Test
    void divergentTrajectoryAppendsSuffix() {
        var pair = AgentPair.of(SELF, OTHER);
        var snapshots = Map.of(
                SELF, new AffectSnapshot(SELF, 0.2, 0.5, 0.5, null),
                OTHER, new AffectSnapshot(OTHER, 0.8, 0.5, 0.5, null));
        var distances = new PadDistanceMatrix(Map.of(pair, 0.6));
        var diffs = Map.of(
                PadDimension.PLEASURE, new PairwiseDifferences(Map.of(pair, -0.6)),
                PadDimension.AROUSAL, new PairwiseDifferences(Map.of(pair, 0.0)),
                PadDimension.DOMINANCE, new PairwiseDifferences(Map.of(pair, 0.0)));
        var alignment = new TrajectoryAlignment(
                Map.of(pair, -0.5),
                Map.of(pair, TrendAgreement.DIVERGENT));
        var comparison = new PerspectivalComparison(
                "peter-node", OTHER_NAME, snapshots, Set.of(),
                distances, diffs, alignment, 2);
        var result = PerceptionTranslator.translate(comparison, SELF, OTHER, OTHER_NAME, "friend");
        assertThat(result).isPresent();
        assertThat(result.get()).contains("widening");
    }

    @Test
    void alignedTrajectoryAppendsSuffix() {
        var pair = AgentPair.of(SELF, OTHER);
        var snapshots = Map.of(
                SELF, new AffectSnapshot(SELF, 0.2, 0.5, 0.5, null),
                OTHER, new AffectSnapshot(OTHER, 0.8, 0.5, 0.5, null));
        var distances = new PadDistanceMatrix(Map.of(pair, 0.6));
        var diffs = Map.of(
                PadDimension.PLEASURE, new PairwiseDifferences(Map.of(pair, -0.6)),
                PadDimension.AROUSAL, new PairwiseDifferences(Map.of(pair, 0.0)),
                PadDimension.DOMINANCE, new PairwiseDifferences(Map.of(pair, 0.0)));
        var alignment = new TrajectoryAlignment(
                Map.of(pair, 0.8),
                Map.of(pair, TrendAgreement.ALIGNED));
        var comparison = new PerspectivalComparison(
                "peter-node", OTHER_NAME, snapshots, Set.of(),
                distances, diffs, alignment, 2);
        var result = PerceptionTranslator.translate(comparison, SELF, OTHER, OTHER_NAME, "friend");
        assertThat(result).isPresent();
        assertThat(result.get()).contains("converging");
    }

    @Test
    void strangerStageReturnsEmpty() {
        var comparison = buildComparison(0.2, 0.8, 0.5, 0.5, 0.5, 0.5);
        var result     = PerceptionTranslator.translate(comparison, SELF, OTHER, OTHER_NAME, "stranger");
        assertThat(result).isEmpty();
    }

    @Test
    void acquaintanceStageReturnsDominantOnly() {
        var pair = AgentPair.of(SELF, OTHER);
        var snapshots = Map.of(
                SELF, new AffectSnapshot(SELF, 0.2, 0.5, 0.5, null),
                OTHER, new AffectSnapshot(OTHER, 0.8, 0.5, 0.5, null));
        var distances = new PadDistanceMatrix(Map.of(pair, 0.6));
        var diffs = Map.of(
                PadDimension.PLEASURE, new PairwiseDifferences(Map.of(pair, -0.6)),
                PadDimension.AROUSAL, new PairwiseDifferences(Map.of(pair, 0.0)),
                PadDimension.DOMINANCE, new PairwiseDifferences(Map.of(pair, 0.0)));
        var alignment = new TrajectoryAlignment(
                Map.of(pair, -0.5),
                Map.of(pair, TrendAgreement.DIVERGENT));
        var comparison = new PerspectivalComparison(
                "peter-node", OTHER_NAME, snapshots, Set.of(),
                distances, diffs, alignment, 2);
        var result = PerceptionTranslator.translate(comparison, SELF, OTHER, OTHER_NAME, "acquaintance");
        assertThat(result).isPresent();
        assertThat(result.get()).contains("acquaintance");
        assertThat(result.get()).doesNotContain("widening");
    }

    @Test
    void friendStageIncludesTrajectory() {
        var pair = AgentPair.of(SELF, OTHER);
        var snapshots = Map.of(
                SELF, new AffectSnapshot(SELF, 0.2, 0.5, 0.5, null),
                OTHER, new AffectSnapshot(OTHER, 0.8, 0.5, 0.5, null));
        var distances = new PadDistanceMatrix(Map.of(pair, 0.6));
        var diffs = Map.of(
                PadDimension.PLEASURE, new PairwiseDifferences(Map.of(pair, -0.6)),
                PadDimension.AROUSAL, new PairwiseDifferences(Map.of(pair, 0.0)),
                PadDimension.DOMINANCE, new PairwiseDifferences(Map.of(pair, 0.0)));
        var alignment = new TrajectoryAlignment(
                Map.of(pair, -0.5),
                Map.of(pair, TrendAgreement.DIVERGENT));
        var comparison = new PerspectivalComparison(
                "peter-node", OTHER_NAME, snapshots, Set.of(),
                distances, diffs, alignment, 2);
        var result = PerceptionTranslator.translate(comparison, SELF, OTHER, OTHER_NAME, "friend");
        assertThat(result).isPresent();
        assertThat(result.get()).contains("widening");
        assertThat(result.get()).contains("friend");
    }


    private PerspectivalComparison buildComparison(
            double selfPleasure, double otherPleasure,
            double selfArousal, double otherArousal,
            double selfDominance, double otherDominance) {
        var pair = AgentPair.of(SELF, OTHER);
        var snapshots = Map.of(
                SELF, new AffectSnapshot(SELF, selfPleasure, selfArousal, selfDominance, null),
                OTHER, new AffectSnapshot(OTHER, otherPleasure, otherArousal, otherDominance, null));
        double dp = selfPleasure - otherPleasure;
        double da = selfArousal - otherArousal;
        double dd = selfDominance - otherDominance;
        double distance = Math.sqrt(dp * dp + da * da + dd * dd);
        var distances = new PadDistanceMatrix(Map.of(pair, distance));
        var diffs = Map.of(
                PadDimension.PLEASURE, new PairwiseDifferences(Map.of(pair, dp)),
                PadDimension.AROUSAL, new PairwiseDifferences(Map.of(pair, da)),
                PadDimension.DOMINANCE, new PairwiseDifferences(Map.of(pair, dd)));
        var alignment = new TrajectoryAlignment(Map.of(), Map.of());
        return new PerspectivalComparison(
                "other-node", OTHER_NAME, snapshots, Set.of(),
                distances, diffs, alignment, 2);
    }
}
