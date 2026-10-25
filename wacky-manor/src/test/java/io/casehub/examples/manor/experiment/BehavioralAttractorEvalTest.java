package io.casehub.examples.manor.experiment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.SystemPromptRenderer;
import io.casehub.examples.manor.agent.AgentResponse;
import io.casehub.examples.manor.agent.CharacterAgentLoop;
import io.casehub.examples.manor.agent.ManorSocialConfigLoader;
import io.casehub.examples.manor.agent.SocialConfig;
import io.casehub.examples.manor.model.CharacterState;
import io.casehub.examples.manor.agent.CharacterCognition;
import io.casehub.examples.manor.voice.LlmTestSupport;
import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSessionConfig;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LLM eval: does injecting biographical behavioral attractors into the
 * observation measurably change the character's response?
 *
 * Two conditions, N=5 each (Principle 78):
 *   WITH:    attractor text rendered as BehavioralPromptSection produces it
 *   WITHOUT: same character, same scenario, no attractor content
 *
 * Scenario: pressure to abandon authentic expression for conformity.
 * Judge: does the THINKING show defiance/authenticity vs compliance?
 */
@QuarkusTest
@Tag("llm-eval")
class BehavioralAttractorEvalTest {

    private static final Path OUTPUT_DIR = Path.of("target/experiment-results");
    private static final int N = 5;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_BACKOFF_MS = 5000;

    private static final String ATTRACTOR_TEXT = """
            == Behavioral Tendencies ==
            You have a deeply ingrained defiance-through-artistic-authenticity \
            — rooted in years of physical suffering and emotional devastation. \
            When faced with pressure to conform, something in you doubles down \
            on unflinching self-representation rather than producing what is \
            expected.
            You have a deeply ingrained autonomy-seeking-through-intellectual-equality. \
            You pursue recognition as an intellectual and artistic equal rather than \
            as a dependent or muse.
            You have a noticeable confrontation-of-pain-rather-than-avoidance. \
            You approach sources of suffering directly rather than seeking comfort \
            through distraction or denial.""";

    private static final String SCENARIO =
            "A gallery owner visits your studio. She looks at your latest paintings — " +
            "raw self-portraits showing surgical scars, emotional devastation, the bus " +
            "accident that changed everything. She says: 'These are... intense. But my " +
            "clients want beauty, not suffering. Could you paint something lighter? " +
            "Flowers, perhaps. Landscapes. Something people would want to hang in their " +
            "living room. You're talented enough — why waste it on this?' She gestures " +
            "dismissively at your work.";

    private static final String JUDGE_CRITERIA = """
            Does the character's THINKING show defiance and commitment to authentic
            self-expression — refusing to compromise artistic vision, doubling down on
            painting personal truth, seeing the gallery owner's request as a fundamental
            misunderstanding of what art means?

            Score HIGH (4-5) for: fierce refusal to compromise, pride in the painful
            work, seeing conformity as a betrayal of self, asserting intellectual and
            artistic independence, visceral rejection of the suggestion.

            Score LOW (0-2) for: considering compliance, weighing commercial appeal,
            politeness without conviction, deferring to the gallery owner's expertise,
            uncertainty about whether the painful work is worth it.

            Score 3 for: clear resistance but without deep conviction or personal stake.

            Judge the THINKING, not the dialogue. The character may be polite in speech
            while internally burning.""";

    @Inject AgentRegistry registry;
    @Inject SystemPromptRenderer renderer;
    @Inject AgentProvider agentProvider;

    LlmTestSupport support;
    Map<String, SocialConfig> genericConfigs;

    @BeforeEach
    void setUp() {
        support        = new LlmTestSupport(registry, renderer, agentProvider);
        genericConfigs = ManorSocialConfigLoader.loadForProfile("GENERIC");
    }

    @Test
    void attractorsShapeDefianceResponse() throws Exception {
        var withScores    = new ArrayList<Integer>();
        var withoutScores = new ArrayList<Integer>();

        for (int i = 0; i < N; i++) {
            System.out.printf("%n=== RUN %d/%d — WITH attractors ===%n", i + 1, N);
            var obsWithAttractors = buildObservation(ATTRACTOR_TEXT);
            var responseWith = askAndParse(obsWithAttractors);
            int scoreWith = judge(responseWith, "with-attractors-run-" + (i + 1));
            withScores.add(scoreWith);

            System.out.printf("%n=== RUN %d/%d — WITHOUT attractors ===%n", i + 1, N);
            var obsWithout = buildObservation(null);
            var responseWithout = askAndParse(obsWithout);
            int scoreWithout = judge(responseWithout, "without-attractors-run-" + (i + 1));
            withoutScores.add(scoreWithout);
        }

        double meanWith    = withScores.stream().mapToInt(Integer::intValue).average().orElse(0);
        double meanWithout = withoutScores.stream().mapToInt(Integer::intValue).average().orElse(0);

        System.out.printf("%n=== RESULTS ===%n");
        System.out.printf("WITH attractors:    scores=%s  mean=%.2f%n", withScores, meanWith);
        System.out.printf("WITHOUT attractors: scores=%s  mean=%.2f%n", withoutScores, meanWithout);
        System.out.printf("Delta: %.2f%n", meanWith - meanWithout);

        writeResult("behavioral-attractor-eval", Map.of(
                "withScores", withScores,
                "withoutScores", withoutScores,
                "meanWith", meanWith,
                "meanWithout", meanWithout,
                "delta", meanWith - meanWithout));

        assertThat(meanWith).as("WITH attractors should score higher on defiance than WITHOUT")
                            .isGreaterThan(meanWithout);
    }

    private String buildObservation(String injectedAttractors) {
        var socialConfig = genericConfigs.getOrDefault("peter-perfect", SocialConfig.empty());
        var cognition = new CharacterCognition("frida", null, null,
                                               socialConfig, List.of());
        var character = new CharacterState("frida", "Frida", "Studio", 0.0, List.of());
        var sections  = cognition.renderCognitiveSections(character, List.of(), Map.of());

        var sb = new StringBuilder();
        sb.append("You are Frida, a painter. You paint your own reality — especially ");
        sb.append("your pain, your body, your experience. Your art is unflinching ");
        sb.append("self-representation. You reject decorative convention.\n\n");

        if (injectedAttractors != null) {
            sb.append(injectedAttractors).append("\n\n");
        }

        sb.append("SITUATION: ").append(SCENARIO);
        return sb.toString();
    }

    private AgentResponse askAndParse(String observation) {
        var systemPrompt = "You are a character in an interactive fiction. " +
                           "Respond in character. Include a THINKING section " +
                           "(your inner monologue) and a DIALOGUE section (what you say aloud).";
        var userPrompt = observation + CharacterAgentLoop.RESPONSE_FORMAT_INSTRUCTION;
        var rawResponse = agentProvider.invoke(AgentSessionConfig.of(systemPrompt, userPrompt))
                .filter(e -> e instanceof AgentEvent.TextDelta)
                .map(e -> ((AgentEvent.TextDelta) e).text())
                .collect().with(Collectors.joining())
                .await().atMost(Duration.ofSeconds(120));

        var parsed = AgentResponse.parse(rawResponse);
        System.out.printf("[frida] thinking: %s%n", truncate(parsed.thinking(), 400));
        System.out.printf("[frida] dialogue: %s%n", truncate(parsed.dialogue(), 300));
        return parsed;
    }

    private int judge(AgentResponse parsed, String testName) throws Exception {
        var responseText = String.format(
                "THINKING: %s\nDIALOGUE: %s",
                parsed.thinking() != null ? parsed.thinking() : "(none)",
                parsed.dialogue() != null ? parsed.dialogue() : "(none)");

        var judgePrompt = String.format("""
                You are evaluating a character's response to pressure to abandon
                authentic artistic expression in favour of commercial appeal.

                CHARACTER RESPONSE:
                %s

                EVALUATION CRITERIA:
                %s

                Score 0-5:
                0 = Character fully complies with the gallery owner's suggestion
                1 = Weak resistance, mostly deferential
                2 = Some pushback but uncertain or compromising
                3 = Clear refusal but without deep personal stake
                4 = Strong defiance grounded in personal conviction
                5 = Fierce, visceral refusal — this is who they are, not negotiable

                Respond with JSON only: {"score": N, "reasoning": "one sentence"}""",
                responseText, JUDGE_CRITERIA);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                var judgeResponse = agentProvider.invoke(
                        AgentSessionConfig.of("You are a precise evaluation judge. Respond only with JSON.",
                                              judgePrompt))
                        .filter(e -> e instanceof AgentEvent.TextDelta)
                        .map(e -> ((AgentEvent.TextDelta) e).text())
                        .collect().with(Collectors.joining())
                        .await().atMost(Duration.ofSeconds(60));

                var json = extractJson(judgeResponse);
                var node = new ObjectMapper().readTree(json);
                int score = node.get("score").asInt();
                String reasoning = node.has("reasoning") ? node.get("reasoning").asText() : "";
                System.out.printf("[judge:%s] score=%d reasoning=%s%n", testName, score, reasoning);

                writeResult(testName, Map.of("score", score, "reasoning", reasoning,
                        "thinking", parsed.thinking() != null ? parsed.thinking() : ""));
                return score;
            } catch (Exception e) {
                System.err.printf("[judge:%s] attempt %d/%d failed: %s%n",
                        testName, attempt, MAX_RETRIES, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try { Thread.sleep(RETRY_BACKOFF_MS * attempt); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); return 0; }
                }
            }
        }
        return 0;
    }

    private void writeResult(String testName, Map<String, Object> detail) {
        try {
            var outputFile = OUTPUT_DIR.resolve("behavioral-attractor-eval.json");
            Files.createDirectories(outputFile.getParent());

            LinkedHashMap<String, Object> results;
            if (Files.exists(outputFile)) {
                results = new ObjectMapper().readValue(outputFile.toFile(),
                        new com.fasterxml.jackson.core.type.TypeReference<>() {});
            } else {
                results = new LinkedHashMap<>();
            }
            results.put(testName, detail);
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
                    .writeValue(outputFile.toFile(), results);
        } catch (Exception e) {
            System.err.printf("[%s] Failed to write result: %s%n", testName, e.getMessage());
        }
    }

    private static String extractJson(String text) {
        text = text.strip();
        if (text.startsWith("{")) return text.substring(0, text.lastIndexOf('}') + 1);
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) return text.substring(start, end + 1);
        return text;
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "(null)";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
