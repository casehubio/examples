package io.casehub.examples.manor.experiment;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.examples.manor.model.BriefingMode;

import java.util.Map;

public final class BriefingTransform {
    private BriefingTransform() {}

    private static final Map<String, String> ROLE_PHRASES = Map.of(
            "hooded-claw", "a villain and secret nemesis",
            "penelope-pitstop", "a resourceful Southern belle",
            "ant-hill-mob", "a gang of protective bodyguards",
            "dick-dastardly", "a scheming cheat",
            "peter-perfect", "a gallant hero"
    );

    public static AgentDescriptor withBriefing(AgentDescriptor desc,
                                                BriefingMode mode) {
        String briefing = switch (mode) {
            case EMPTY -> null;
            case NAME_ONLY -> "You are an agent named " + desc.name() + ".";
            case NAME_ROLE -> {
                String role = ROLE_PHRASES.get(desc.agentId());
                if (role == null) {
                    throw new IllegalArgumentException(
                            "No role phrase mapped for: " + desc.agentId());
                }
                yield "You are " + desc.name() + ", " + role + ".";
            }
            case RICH -> desc.briefing();
        };
        return desc.toBuilder().briefing(briefing).build();
    }
}
