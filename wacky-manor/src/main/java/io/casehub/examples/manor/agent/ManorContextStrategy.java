package io.casehub.examples.manor.agent;

import java.util.Collection;
import java.util.List;

public final class ManorContextStrategy {

    public CognitiveBudget budgetFor(int nearbyCount, double arousal, int activeGoals) {
        return CognitiveBudget.forSituation(nearbyCount, arousal, activeGoals);
    }

    public List<SocialConfig.NormEntry> selectNorms(List<SocialConfig.NormEntry> norms,
                                                      Collection<String> activeCognitiveSubjects,
                                                      Collection<String> activeInventory,
                                                      CognitiveBudget budget) {
        return ManorNormFilter.score(norms, activeCognitiveSubjects, activeInventory, budget.maxNorms());
    }
}
