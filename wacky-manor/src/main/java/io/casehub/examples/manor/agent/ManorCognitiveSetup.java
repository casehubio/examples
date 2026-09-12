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

        return new DescriptorView(descriptor.agentId(), axes, profile, goals);
    }

    public static CognitiveDefaults deriveDefaults(AgentDescriptor descriptor) {
        return CognitiveDerivationEngine.derive(toDescriptorView(descriptor));
    }
}
