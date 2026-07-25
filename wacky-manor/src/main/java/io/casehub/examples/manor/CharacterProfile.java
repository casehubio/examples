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

    public String buildSystemPrompt() {
        var sb = new StringBuilder();
        sb.append("# Character: ").append(name).append("\n\n");

        if (disposition != null) {
            sb.append("## Personality Profile\n");
            if (disposition.socialOrient != null)
                sb.append("- Social orientation: ").append(disposition.socialOrient).append("\n");
            if (disposition.ruleFollowing != null)
                sb.append("- Rule following: ").append(disposition.ruleFollowing).append("\n");
            if (disposition.riskAppetite != null)
                sb.append("- Risk appetite: ").append(disposition.riskAppetite).append("\n");
            if (disposition.autonomy != null)
                sb.append("- Autonomy: ").append(disposition.autonomy).append("\n");
            if (disposition.conflictMode != null)
                sb.append("- Conflict style: ").append(disposition.conflictMode).append("\n");
            sb.append("\n");
        }

        if (briefing != null) {
            sb.append("## Instructions\n\n");
            sb.append(briefing).append("\n");
        }

        return sb.toString();
    }
}
