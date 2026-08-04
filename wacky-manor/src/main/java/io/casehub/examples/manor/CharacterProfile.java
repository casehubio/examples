package io.casehub.examples.manor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CharacterProfile(
        String agentId,
        String name,
        String slot,
        String tenancyId,
        Disposition disposition,
        List<Capability> capabilities,
        String briefing) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Disposition(
            String socialOrient,
            String ruleFollowing,
            String riskAppetite,
            String autonomy,
            String conflictMode,
            boolean delegation) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Capability(
            String name,
            List<String> tags) {}

}
