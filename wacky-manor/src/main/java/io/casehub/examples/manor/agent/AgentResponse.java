package io.casehub.examples.manor.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.examples.manor.model.Action;
import io.casehub.examples.manor.model.ActionType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentResponse(
        String thinking,
        String dialogue,
        String talkTo,
        String aside,
        Action action) {

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
                                 new Action(ActionType.WAIT, null, null));
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
