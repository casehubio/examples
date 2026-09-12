package io.casehub.examples.manor.agent;

import io.casehub.examples.manor.model.ActionType;
import io.casehub.neocortex.cognitive.index.ConflictInterpretation;

import java.util.Map;

public final class ManorTrustEvents {

    private static final Map<ActionType, Double> BASE_WEIGHTS = Map.of(
            ActionType.STEAL, -0.4,
            ActionType.GIVE, 0.15,
            ActionType.INTERACT, 0.05,
            ActionType.PULL_ASIDE, 0.1,
            ActionType.USE, -0.1
    );

    private static final Map<ConflictInterpretation, Double> NEGATIVE_MODIFIERS = Map.of(
            ConflictInterpretation.REPAIR, 0.6,
            ConflictInterpretation.INFORMATION, 0.8,
            ConflictInterpretation.NEUTRAL, 1.0,
            ConflictInterpretation.DISENGAGE, 1.3
    );

    private ManorTrustEvents() {}

    public static double weightFor(ActionType action) {
        return BASE_WEIGHTS.getOrDefault(action, 0.0);
    }

    public static double weightFor(ActionType action, double trustFormationRate,
                                   ConflictInterpretation conflictMode) {
        double base = BASE_WEIGHTS.getOrDefault(action, 0.0);
        if (base < 0) {
            double negMod = NEGATIVE_MODIFIERS.getOrDefault(conflictMode, 1.0);
            return base * trustFormationRate * negMod;
        }
        return base * trustFormationRate;
    }

    public static boolean isRelevant(ActionType action) {
        return BASE_WEIGHTS.containsKey(action) && BASE_WEIGHTS.get(action) != 0.0;
    }
}
