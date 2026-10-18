package io.casehub.examples.manor.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegulationTrackerTest {

    @Test
    void noData_emptyTrajectory() {
        var tracker = new RegulationTracker();
        assertThat(tracker.computeTrajectory("clara")).isEmpty();
    }

    @Test
    void singleTurn_emptyTrajectory() {
        var tracker = new RegulationTracker();
        tracker.recordPosterior("clara", new double[]{0.10, 0.25, 0.65});
        assertThat(tracker.computeTrajectory("clara")).isEmpty();
    }

    @Test
    void distressDecreasing_improving() {
        var tracker = new RegulationTracker();
        tracker.recordPosterior("clara", new double[]{0.10, 0.25, 0.65});
        tracker.recordPosterior("clara", new double[]{0.20, 0.30, 0.50});
        var result = tracker.computeTrajectory("clara");
        assertThat(result).isPresent();
        assertThat(result.get().trajectory()).isEqualTo(RegulationTracker.Trajectory.IMPROVING);
        assertThat(result.get().delta()).isEqualTo(-0.15, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void distressStable_stable() {
        var tracker = new RegulationTracker();
        tracker.recordPosterior("clara", new double[]{0.10, 0.25, 0.65});
        tracker.recordPosterior("clara", new double[]{0.12, 0.25, 0.63});
        var result = tracker.computeTrajectory("clara");
        assertThat(result).isPresent();
        assertThat(result.get().trajectory()).isEqualTo(RegulationTracker.Trajectory.STABLE);
        assertThat(result.get().delta()).isEqualTo(-0.02, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void distressIncreasing_worsening() {
        var tracker = new RegulationTracker();
        tracker.recordPosterior("clara", new double[]{0.10, 0.25, 0.65});
        tracker.recordPosterior("clara", new double[]{0.05, 0.15, 0.80});
        var result = tracker.computeTrajectory("clara");
        assertThat(result).isPresent();
        assertThat(result.get().trajectory()).isEqualTo(RegulationTracker.Trajectory.WORSENING);
        assertThat(result.get().delta()).isEqualTo(0.15, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void threeTurns_usesLatestDelta() {
        var tracker = new RegulationTracker();
        tracker.recordPosterior("clara", new double[]{0.10, 0.25, 0.65});
        tracker.recordPosterior("clara", new double[]{0.05, 0.15, 0.80});
        tracker.recordPosterior("clara", new double[]{0.20, 0.30, 0.50});
        var result = tracker.computeTrajectory("clara");
        assertThat(result).isPresent();
        assertThat(result.get().trajectory()).isEqualTo(RegulationTracker.Trajectory.IMPROVING);
        assertThat(result.get().delta()).isEqualTo(-0.30, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void independentTargets() {
        var tracker = new RegulationTracker();
        tracker.recordPosterior("clara", new double[]{0.10, 0.25, 0.65});
        tracker.recordPosterior("clara", new double[]{0.20, 0.30, 0.50});
        tracker.recordPosterior("foxworth", new double[]{0.30, 0.40, 0.30});
        assertThat(tracker.computeTrajectory("clara")).isPresent();
        assertThat(tracker.computeTrajectory("foxworth")).isEmpty();
    }

    @Test
    void exactThreshold_stable() {
        var tracker = new RegulationTracker();
        tracker.recordPosterior("clara", new double[]{0.10, 0.25, 0.65});
        tracker.recordPosterior("clara", new double[]{0.10, 0.20, 0.70});
        var result = tracker.computeTrajectory("clara");
        assertThat(result).isPresent();
        assertThat(result.get().trajectory()).isEqualTo(RegulationTracker.Trajectory.STABLE);
    }

    @Test
    void posteriorCloned_externalMutationDoesNotAffectTracker() {
        var tracker = new RegulationTracker();
        double[] posterior = {0.10, 0.25, 0.65};
        tracker.recordPosterior("clara", posterior);
        posterior[2] = 0.0;
        tracker.recordPosterior("clara", new double[]{0.20, 0.30, 0.50});
        var result = tracker.computeTrajectory("clara");
        assertThat(result).isPresent();
        assertThat(result.get().trajectory()).isEqualTo(RegulationTracker.Trajectory.IMPROVING);
        assertThat(result.get().delta()).isEqualTo(-0.15, org.assertj.core.data.Offset.offset(0.001));
    }
}
