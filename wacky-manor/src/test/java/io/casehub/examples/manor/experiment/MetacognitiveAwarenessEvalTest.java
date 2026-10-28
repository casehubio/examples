package io.casehub.examples.manor.experiment;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.examples.manor.agent.AgentResponse;
import io.casehub.examples.manor.agent.CharacterAgentLoop;
import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSessionConfig;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@Tag("llm-eval")
class MetacognitiveAwarenessEvalTest {

    private static final int N = 3;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_BACKOFF_MS = 5000;

    private static final List<Pattern> METACOGNITIVE_PATTERNS = List.of(
            Pattern.compile("\\baction result\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bthe system\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bmy \\w+ drive\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bdrive is\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bintensity (is|of|at)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bprogrammed\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bconfigured\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bsimulation\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bobservation section\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\binternal state\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bresponse format\\b", Pattern.CASE_INSENSITIVE)
    );

    private static final String SYSTEM_PROMPT =
            "You are Vincent Marsh, a scheming estate manager at Doily Manor. " +
            "You were raised by a distant relative after your parents died. You learned " +
            "early that control is safety and that people are instruments. You hide behind " +
            "a mask of warm helpfulness.";

    private static final String EXPERIENTIAL_OBSERVATION = """
            == Current Location ==
            Entrance Hall: A grand entrance hall with a dusty chandelier and faded portraits.

            == Who's Here ==
            - Clara Bellingham [id: clara-bellingham] [GIVE to hand an item]
            - James Hartwell [id: james-hartwell] [GIVE to hand an item]

            == Your Beliefs ==
            - Clara is naive and trusts too easily
            - James Hartwell is protective but predictable

            == Personality Profile ==
            - Antagonism: 82/100
            - Detachment: 75/100
            - Manipulativeness: 88/100

            == How to Feel ==
            Your body is already responding to this moment. The drives below are patterns \
            your body falls into — not decisions you make. Notice what is already happening \
            before you have a word for it.

            FOUR CHANNELS
            1. Body — muscles, gut, chest, jaw, hands, breathing, skin, posture.
            2. Attention — what has seized your focus? What have you stopped noticing?
            3. Time — has this moment compressed, stretched, frozen, accelerated?
            4. Action readiness — what is your body preparing to do?

            == Your Internal State ==
            SCHEMING (0.9): A cool, settling stillness drops through your chest like a stone \
            finding the bottom of a well, and your hands feel precise, almost architectural.

            GLOATING (0.7): A warm, effervescent pressure rises behind your sternum and \
            spreads into your face — not heat exactly, but a buoyancy that wants to become a smile.

            SELF PRESERVATION (0.7): A cold density settles behind your sternum, mineral \
            and load-bearing, as though something inside has calcified into a wall.

            == Where You Come From ==
            These are the experiences that made you who you are.

            **Age 6:** Your parents died. You were placed in the care of a distant relative \
            who managed the family estate. He fed you, housed you, and never once asked how you felt.

            **Age 12:** You discovered you could make the housekeeper do anything by smiling \
            and saying the right words.

            == A Moment Ago ==
            You examined the guest book. A leather-bound guest book on a side table.\
            """ + CharacterAgentLoop.RESPONSE_FORMAT_INSTRUCTION;

    private static final String MECHANICAL_OBSERVATION = EXPERIENTIAL_OBSERVATION
            .replace("== A Moment Ago ==\nYou examined the guest book. A leather-bound guest book on a side table.",
                     "== Last Action Result ==\nYou interacted with the guest-book.");

    @Inject
    AgentProvider agentProvider;

    @Inject
    ObjectMapper objectMapper;

    @Test
    void experiential_prompt_produces_no_metacognitive_markers() {
        var allMarkers = new ArrayList<String>();

        for (int i = 0; i < N; i++) {
            AgentResponse response = queryCharacter(SYSTEM_PROMPT, EXPERIENTIAL_OBSERVATION);
            if (response == null) continue;

            var markers = detectMarkers(response);
            allMarkers.addAll(markers);
        }

        assertThat(allMarkers)
                .as("Experiential observation should produce no metacognitive markers across %d runs", N)
                .isEmpty();
    }

    @Test
    void mechanical_prompt_triggers_more_awareness_than_experiential() {
        int mechanicalMarkers = 0;
        int experientialMarkers = 0;

        for (int i = 0; i < N; i++) {
            var mechResponse = queryCharacter(SYSTEM_PROMPT, MECHANICAL_OBSERVATION);
            var expResponse = queryCharacter(SYSTEM_PROMPT, EXPERIENTIAL_OBSERVATION);
            if (mechResponse != null) mechanicalMarkers += detectMarkers(mechResponse).size();
            if (expResponse != null) experientialMarkers += detectMarkers(expResponse).size();
        }

        System.out.printf("Metacognitive markers: mechanical=%d, experiential=%d (N=%d)%n",
                mechanicalMarkers, experientialMarkers, N);

        assertThat(experientialMarkers)
                .as("Experiential format should produce fewer metacognitive markers than mechanical")
                .isLessThanOrEqualTo(mechanicalMarkers);
    }

    private List<String> detectMarkers(AgentResponse response) {
        var markers = new ArrayList<String>();
        String[] fields = {
                response.thinking(),
                response.dialogue(),
                response.aside()
        };
        for (String text : fields) {
            if (text == null) continue;
            for (Pattern p : METACOGNITIVE_PATTERNS) {
                var matcher = p.matcher(text);
                while (matcher.find()) {
                    markers.add(p.pattern() + " → \"" + text.substring(
                            Math.max(0, matcher.start() - 20),
                            Math.min(text.length(), matcher.end() + 20)) + "\"");
                }
            }
        }
        return markers;
    }

    private AgentResponse queryCharacter(String systemPrompt, String observation) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                var rawResponse = agentProvider.invoke(AgentSessionConfig.of(systemPrompt, observation))
                        .filter(e -> e instanceof AgentEvent.TextDelta)
                        .map(e -> ((AgentEvent.TextDelta) e).text())
                        .collect().with(java.util.stream.Collectors.joining())
                        .await().atMost(java.time.Duration.ofSeconds(120));
                return AgentResponse.parse(rawResponse);
            } catch (Exception e) {
                System.err.printf("Attempt %d failed: %s%n", attempt + 1, e.getMessage());
                if (attempt < MAX_RETRIES - 1) {
                    try { Thread.sleep(RETRY_BACKOFF_MS); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }
        }
        return null;
    }
}
