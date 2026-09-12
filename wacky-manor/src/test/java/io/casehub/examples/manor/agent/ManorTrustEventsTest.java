package io.casehub.examples.manor.agent;

import io.casehub.examples.manor.model.ActionType;
import io.casehub.neocortex.cognitive.index.ConflictInterpretation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ManorTrustEventsTest {

    @Test
    void stealIsNegative() {
        assertThat(ManorTrustEvents.weightFor(ActionType.STEAL)).isLessThan(0);
    }

    @Test
    void giveIsPositive() {
        assertThat(ManorTrustEvents.weightFor(ActionType.GIVE)).isGreaterThan(0);
    }

    @Test
    void waitIsIrrelevant() {
        assertThat(ManorTrustEvents.isRelevant(ActionType.WAIT)).isFalse();
    }

    @Test
    void moveIsIrrelevant() {
        assertThat(ManorTrustEvents.isRelevant(ActionType.MOVE)).isFalse();
    }

    @Test
    void stealIsRelevant() {
        assertThat(ManorTrustEvents.isRelevant(ActionType.STEAL)).isTrue();
    }

    @Test
    void highTrustFormationAmplifies() {
        double base = ManorTrustEvents.weightFor(ActionType.GIVE);
        double modulated = ManorTrustEvents.weightFor(ActionType.GIVE, 0.7, ConflictInterpretation.NEUTRAL);
        assertThat(modulated).isCloseTo(base * 0.7, within(0.001));
    }

    @Test
    void lowTrustFormationDampens() {
        double modulated = ManorTrustEvents.weightFor(ActionType.GIVE, 0.3, ConflictInterpretation.NEUTRAL);
        assertThat(modulated).isCloseTo(0.15 * 0.3, within(0.001));
    }

    @Test
    void repairConflictModeReducesNegativeImpact() {
        double neutral = ManorTrustEvents.weightFor(ActionType.STEAL, 0.5, ConflictInterpretation.NEUTRAL);
        double repair = ManorTrustEvents.weightFor(ActionType.STEAL, 0.5, ConflictInterpretation.REPAIR);
        assertThat(Math.abs(repair)).isLessThan(Math.abs(neutral));
    }

    @Test
    void disengageConflictModeAmplifiesNegativeImpact() {
        double neutral = ManorTrustEvents.weightFor(ActionType.STEAL, 0.5, ConflictInterpretation.NEUTRAL);
        double disengage = ManorTrustEvents.weightFor(ActionType.STEAL, 0.5, ConflictInterpretation.DISENGAGE);
        assertThat(Math.abs(disengage)).isGreaterThan(Math.abs(neutral));
    }

    @Test
    void conflictModeDoesNotAffectPositiveActions() {
        double repair = ManorTrustEvents.weightFor(ActionType.GIVE, 0.5, ConflictInterpretation.REPAIR);
        double disengage = ManorTrustEvents.weightFor(ActionType.GIVE, 0.5, ConflictInterpretation.DISENGAGE);
        assertThat(repair).isCloseTo(disengage, within(0.001));
    }
}
