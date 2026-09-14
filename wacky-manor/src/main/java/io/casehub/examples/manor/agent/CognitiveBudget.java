package io.casehub.examples.manor.agent;

public record CognitiveBudget(int maxBeliefs, int maxTrust, int maxNorms, int maxPrinciples) {

    public static CognitiveBudget forSituation(int nearbyCount, double arousal, int activeGoalCount) {
        int beliefs = 5;
        int trust = Math.min(nearbyCount + 1, 6);
        int norms = 4;

        if (arousal > 0.7) {
            beliefs += 3;
            norms += 2;
        }
        if (nearbyCount > 3) {
            norms += nearbyCount - 3;
        }
        if (activeGoalCount > 0) {
            beliefs += Math.min(activeGoalCount, 3);
        }
        return new CognitiveBudget(beliefs, trust, norms, 3);
    }
}
