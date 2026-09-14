package io.casehub.examples.manor.agent;

import java.util.Collection;
import java.util.List;

public final class ManorContextStrategy {

    public CognitiveBudget budgetFor(int nearbyCount, double arousal, int activeGoals) {
        return CognitiveBudget.forSituation(nearbyCount, arousal, activeGoals);
    }

    public List<SocialConfig.NormEntry> filterNorms(List<SocialConfig.NormEntry> norms,
                                                     Collection<String> nearbyNames,
                                                     Collection<String> inventory) {
        return ManorNormFilter.filter(norms, nearbyNames, inventory);
    }
}
