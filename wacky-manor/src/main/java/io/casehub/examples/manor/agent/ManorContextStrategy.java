package io.casehub.examples.manor.agent;

import java.util.Collection;
import java.util.List;

public final class ManorContextStrategy {
    private static final java.util.Set<String> SOCIAL_AWARENESS_DRIVES          = java.util.Set.of("scheming", "suspicion");
    private static final double                SOCIAL_AWARENESS_DRIVE_THRESHOLD = 0.5;

    public boolean shouldCompareSocially(SocialConfig config, boolean isPullAside) {
        if (isPullAside) {return true;}
        return config.drives().stream()
                     .anyMatch(d -> SOCIAL_AWARENESS_DRIVES.contains(d.type())
                                    && d.intensity() > SOCIAL_AWARENESS_DRIVE_THRESHOLD);
    }


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
