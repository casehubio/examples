package io.casehub.examples.manor.agent;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.DispositionAxis;
import io.casehub.neocortex.cognitive.index.CognitiveDefaults;
import io.casehub.neocortex.cognitive.index.CognitiveDerivationEngine;
import io.casehub.neocortex.cognitive.index.DescriptorView;
import io.casehub.neocortex.cognitive.index.DispositionAxes;
import io.casehub.neocortex.cognitive.index.WeightedTerm;

import java.util.List;

public final class ManorCognitiveSetup {

    private ManorCognitiveSetup() {}

    public static List<String> gameWorldTypes() {
        return List.of("item", "location", "character");
    }

    public static DescriptorView toDescriptorView(AgentDescriptor descriptor) {
        var disposition = descriptor.disposition();
        var axes = disposition != null
                ? new DispositionAxes(
                        disposition.primaryTerm(DispositionAxis.SOCIAL_ORIENTATION),
                        disposition.primaryTerm(DispositionAxis.RULE_FOLLOWING),
                        disposition.primaryTerm(DispositionAxis.RISK_APPETITE),
                        disposition.primaryTerm(DispositionAxis.AUTONOMY),
                        disposition.primaryTerm(DispositionAxis.CONFLICT_MODE))
                : null;

        var profile = disposition != null
                ? disposition.dispositionProfile().stream()
                        .map(dv -> new WeightedTerm(dv.term(), dv.weight()))
                        .toList()
                : List.<WeightedTerm>of();

        var goals = descriptor.goals().stream()
                .map(g -> g.description())
                .toList();

        return new DescriptorView(descriptor.agentId(), axes, profile, goals, null);
    }

    public static DescriptorView toDescriptorView(AgentDescriptor descriptor, SocialConfig socialConfig) {
        var disposition = descriptor.disposition();
        var axes = disposition != null
                   ? new DispositionAxes(
                disposition.primaryTerm(DispositionAxis.SOCIAL_ORIENTATION),
                disposition.primaryTerm(DispositionAxis.RULE_FOLLOWING),
                disposition.primaryTerm(DispositionAxis.RISK_APPETITE),
                disposition.primaryTerm(DispositionAxis.AUTONOMY),
                disposition.primaryTerm(DispositionAxis.CONFLICT_MODE))
                   : null;

        var profile = disposition != null
                      ? disposition.dispositionProfile().stream()
                                   .map(dv -> new WeightedTerm(dv.term(), dv.weight()))
                                   .toList()
                      : List.<WeightedTerm>of();

        var goals = descriptor.goals().stream()
                              .map(g -> g.description())
                              .toList();

        var padSummary = computeFormationPadSummary(socialConfig);
        return new DescriptorView(descriptor.agentId(), axes, profile, goals, padSummary);
    }


    public static CognitiveDefaults deriveDefaults(AgentDescriptor descriptor) {
        return CognitiveDerivationEngine.derive(toDescriptorView(descriptor));
    }

    private static io.casehub.neocortex.cognitive.index.FormationPadSummary computeFormationPadSummary(SocialConfig config) {
        if (config == null || config.formationMemories().isEmpty()) {return null;}

        double dominanceWeightedReward = 0.0;
        double totalPositivePleasure   = 0.0;
        for (var mem : config.formationMemories()) {
            double p = Math.max(0.0, mem.pleasure());
            dominanceWeightedReward += p * mem.dominance();
            totalPositivePleasure += p;
        }
        return new io.casehub.neocortex.cognitive.index.FormationPadSummary(
                dominanceWeightedReward, totalPositivePleasure, config.formationMemories().size());
    }

}
