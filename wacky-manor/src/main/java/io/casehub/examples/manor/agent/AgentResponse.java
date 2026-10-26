package io.casehub.examples.manor.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.examples.manor.model.Action;
import io.casehub.examples.manor.model.ActionType;
import io.casehub.neocortex.caps.SituationAssessment;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentResponse(
        String thinking,
        String dialogue,
        String talkTo,
        String aside,
        Action action,
        Double situationChangeDegree,
        SituationAssessment situationAssessment,
        MoodImpact moodImpact,
        MentalStateSnapshot mentalState) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MoodImpact(double pleasure, double arousal, double dominance, String cause) {
        public MoodImpact {
            pleasure = Math.clamp(pleasure, -0.3, 0.3);
            arousal = Math.clamp(arousal, -0.3, 0.3);
            dominance = Math.clamp(dominance, -0.3, 0.3);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MentalStateSnapshot(
            java.util.List<MentalStateEntry> beliefs,
            java.util.List<MentalStateEntry> desires,
            java.util.List<MentalStateEntry> intentions) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MentalStateEntry(String key, String text, String type) {}

    private static final ObjectMapper JSON       = new ObjectMapper();
    private static final Pattern      CODE_BLOCK = Pattern.compile("```(?:json)?\\s*\\n?(\\{.*?})\\s*```", Pattern.DOTALL);

    public static AgentResponse parse(String text) {
        try {
            String json = extractJson(text);
            return JSON.readValue(json, AgentResponse.class);
        } catch (Exception e) {
            System.getLogger(AgentResponse.class.getName()).log(System.Logger.Level.WARNING, "Failed to parse agent response: " + e.getMessage());
            return idle();
        }
    }

    public static AgentResponse idle() {
        return new AgentResponse(null, null, null, null,
                                 new Action(ActionType.WAIT, null, null), null, null, null, null);
    }

    private static String extractJson(String text) {
        text = text.strip();
        if (text.startsWith("{")) {return text;}
        Matcher m = CODE_BLOCK.matcher(text);
        if (m.find()) {return m.group(1);}
        int start = text.indexOf('{');
        int end   = text.lastIndexOf('}');
        if (start >= 0 && end > start) {return text.substring(start, end + 1);}
        return text;
    }
}
