package io.casehub.examples.manor.agent;

import io.casehub.blocks.summarisation.observation.affordance.ObservationSection;

import java.util.Collection;

public final class PersonaActivationSection {

    private PersonaActivationSection() {}

    public static ObservationSection resolve(
            SocialConfig.PersonaConstraintMapping mapping,
            Collection<String> nearbyAgentIds) {
        if (mapping == null) {return null;}
        String activePersona = nearbyAgentIds.isEmpty()
                ? mapping.whenInactive()
                : mapping.whenActive();
        String displayName = Character.toUpperCase(activePersona.charAt(0)) + activePersona.substring(1);
        return ObservationSection.text("Active Voice",
                "You are currently presenting as **" + displayName + "**. Maintain this voice.");
    }
}
