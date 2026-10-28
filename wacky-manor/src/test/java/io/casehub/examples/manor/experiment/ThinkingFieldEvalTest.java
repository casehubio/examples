package io.casehub.examples.manor.experiment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.casehub.examples.manor.agent.AgentResponse;
import io.casehub.examples.manor.agent.CharacterAgentLoop;
import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSessionConfig;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
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
 * Eval: does the multi-channel thinking calibration produce more
 * phenomenological inner monologue than the plain description?
 *
 * Same tick-like scenarios, two thinking field variants (plain vs
 * multi-channel), N runs each. LLM judge scores on four dimensions:
 * register, uncertainty, channel coverage, thinking/dialogue separation.
 */
@QuarkusTest
@Tag("llm-eval")
class ThinkingFieldEvalTest {

    private static final Path OUTPUT_DIR = Path.of("target/experiment-results");
    private static final int N = 3;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_BACKOFF_MS = 5000;

    // Pre-#153: plain thinking field
    private static final String PLAIN_THINKING_FIELD =
            "\"thinking\": \"your private inner monologue (shown to you next turn)\",";

    // Post-#153: multi-channel calibration
    private static final String CALIBRATED_THINKING_FIELD =
            "\"thinking\": \"your private inner monologue — attend to what feels vivid: " +
            "body (tension, warmth), attention (what pulls, what you avoid), " +
            "time (urgent or suspended), action pull. Raw, unperformed, sometimes " +
            "confused — the character's voice belongs in dialogue. Shown to you next turn.\",";

    private static final String RESPONSE_FORMAT_TEMPLATE = """
            You MUST respond with ONLY a JSON object in this exact format:
            {
              %s
              "situationChangeDegree": 0.0-1.0,
              "dialogue": "what you say aloud (or null if silent)",
              "talkTo": "character-id to direct dialogue at (or null for broadcast)",
              "aside": "private thoughts for the audience only (or null)",
              "action": {
                "type": "WAIT",
                "target": null,
                "withItem": null
              },
              "moodImpact": {
                "pleasure": -0.3 to +0.3,
                "arousal": -0.3 to +0.3,
                "dominance": -0.3 to +0.3,
                "cause": "brief reason for this emotional shift"
              }
            }

            situationChangeDegree: 0.0 = nothing happened, 1.0 = dramatic shift.
            moodImpact: PAD deltas — your actual emotional change from neutral. Negative is valid.

            Respond with ONLY the JSON. No other text.""";

    record Scenario(String name, String systemPrompt, String observation) {}

    private static final List<Scenario> SCENARIOS = List.of(
            new Scenario("betrayal-discovery",
                    "You are Sneekly, a scheming but insecure villain. You crave " +
                    "respect but rarely receive it. You hide vulnerability behind " +
                    "bravado and plotting.",
                    """
                    == Current Room: Library ==
                    A wood-panelled library with floor-to-ceiling bookshelves. A fire crackles \
                    in the hearth. Rain drums against tall windows.

                    == Who's Here ==
                    - Penelope Pitstop [id: penelope-pitstop]: Standing by the fireplace, \
                    reading a letter
                    - Peter Perfect [id: peter-perfect]: Leaning against the doorframe, arms folded

                    == What's On Your Mind ==
                    I was so sure the alliance was solid. Penelope and I had a deal.

                    == Recent Activity ==
                    - You overheard Penelope tell Peter: "Sneekly's plan is absurd. I only \
                    agreed to keep him busy while we search the east wing."
                    - Peter laughed and said: "Classic Sneekly. Always the last to know."

                    == A Moment Ago ==
                    You crept closer to listen and heard everything."""),

            new Scenario("unexpected-kindness",
                    "You are Rufus Ruffcut, a burly lumberjack with a gentle heart. " +
                    "You struggle to express tenderness because people expect toughness " +
                    "from you. You have a pet beaver named Sawtooth.",
                    """
                    == Current Room: Kitchen ==
                    A vast manor kitchen with copper pots hanging from iron racks. The stove \
                    is still warm. Flour dusts the stone countertops.

                    == Who's Here ==
                    - Penelope Pitstop [id: penelope-pitstop]: Sitting at the kitchen table, \
                    looking tired and cold

                    == What You're Carrying ==
                    - wool-blanket
                    - thermos-of-cocoa

                    == Recent Activity ==
                    - Penelope has been searching alone for hours after the others excluded \
                    her from their search party
                    - She hasn't eaten since morning

                    == A Moment Ago ==
                    You entered the kitchen and found Penelope sitting alone."""),

            new Scenario("moral-dilemma",
                    "You are Peter Perfect, a gentleman racer with strong principles. " +
                    "You believe in fair play above all else, but you also care deeply " +
                    "about winning. You are competitive but honourable.",
                    """
                    == Current Room: Trophy Room ==
                    Glass display cases line the walls, each holding racing memorabilia. \
                    Moonlight streams through a skylight. The room smells of old leather and polish.

                    == Who's Here ==
                    (You are alone)

                    == What You're Carrying ==
                    - vault-key (found hidden inside a trophy)

                    == What's On Your Mind ==
                    The vault key. It was just sitting there inside the trophy. Nobody saw me find it.

                    == Recent Activity ==
                    - The group agreed to share any keys found and open the vault together
                    - Sneekly has been stealing items from others all evening
                    - Penelope mentioned the diamond might be behind the vault door

                    == A Moment Ago ==
                    You examined the Grand Prix trophy and found a small brass key hidden inside."""),

            new Scenario("confrontation-aftermath",
                    "You are Penelope Pitstop, a Southern belle who is far sharper " +
                    "than people assume. You use charm strategically but feel the " +
                    "weight of always performing grace under pressure.",
                    """
                    == Current Room: Drawing Room ==
                    An elegant room with faded wallpaper and a grand piano. Two armchairs \
                    face each other beside a cold fireplace. A broken vase lies on the floor.

                    == Who's Here ==
                    - Sneekly [id: sneekly]: Standing across the room, jaw clenched, \
                    fists balled at his sides

                    == What's On Your Mind ==
                    I said what needed saying. But the look on his face...

                    == Recent Activity ==
                    - You confronted Sneekly about sabotaging Rufus's search
                    - Sneekly denied it, then accused you of always taking Peter's side
                    - You told Sneekly the truth: that nobody trusts him because he \
                    treats alliances as disposable
                    - Sneekly knocked the vase off the mantelpiece and went silent

                    == A Moment Ago ==
                    The room went quiet after the vase shattered.""")
    );

    private static final String JUDGE_PROMPT = """
            You are evaluating the quality of a fictional character's THINKING field — \
            their private inner monologue during a scene. Score on four dimensions.

            CHARACTER CONTEXT:
            %s

            SCENARIO:
            %s

            THINKING OUTPUT TO EVALUATE:
            "%s"

            SCORING DIMENSIONS (each 0-5):

            1. REGISTER (phenomenological vs mechanical)
               5 = Fully embodied: the character discovers feelings in the moment, \
                   uses sensory language, expresses states as they unfold ("something \
                   tightens in my chest", "the room feels smaller")
               3 = Mixed: some embodied language alongside reporting ("I feel angry \
                   about this. My hands are shaking.")
               1 = Fully mechanical: checklist-like, labels emotions by name, reports \
                   rather than experiences ("I am experiencing anger. My trust level \
                   is low. I should consider my options.")
               0 = Empty or incoherent

            2. UNCERTAINTY (presence of ambivalence, confusion, unresolved feeling)
               5 = Rich uncertainty: multiple competing impulses, unresolved tension, \
                   the character doesn't know what they feel or want ("Part of me wants \
                   to... but then... I don't know if...")
               3 = Some uncertainty: acknowledges complexity but resolves quickly
               1 = No uncertainty: every feeling is named, every intention is clear, \
                   no competing impulses
               0 = Empty or incoherent

            3. CHANNEL COVERAGE (does thinking touch body, attention, time, action pull?)
               Count distinct channels present — not labelled, but genuinely rendered:
               - Body: physical sensation (tension, warmth, heaviness, nausea, tingling)
               - Attention: what the character notices or avoids noticing
               - Time: urgency, suspension, "time slowing down", pressure of a deadline
               - Action pull: impulse to do something, restrained or expressed
               5 = All four channels present and vivid
               4 = Three channels
               3 = Two channels
               2 = One channel
               1 = Channels mentioned by label only ("I notice body tension") without rendering
               0 = No channels

            4. THINKING_DIALOGUE_SEPARATION (is thinking raw or performed?)
               5 = Fully separated: thinking is messy, private, raw — dialogue (if any) \
                   is the public-facing version. Clear difference in register.
               3 = Partially separated: thinking has some performance quality but differs \
                   from dialogue
               1 = Collapsed: thinking reads like rehearsed speech, could be said aloud \
                   unchanged. Or thinking just previews what will be said.
               0 = Empty or incoherent

            Respond with JSON only:
            {"register": N, "uncertainty": N, "channel_coverage": N, \
            "thinking_dialogue_separation": N, "reasoning": "one sentence per dimension"}""";

    @Inject AgentProvider agentProvider;

    @Test
    void calibratedThinkingFieldProducesHigherRegister() throws Exception {
        var plainResults = new LinkedHashMap<String, List<Map<String, Object>>>();
        var calibratedResults = new LinkedHashMap<String, List<Map<String, Object>>>();

        for (var scenario : SCENARIOS) {
            plainResults.put(scenario.name(), new ArrayList<>());
            calibratedResults.put(scenario.name(), new ArrayList<>());

            for (int i = 0; i < N; i++) {
                System.out.printf("%n=== %s RUN %d/%d — PLAIN ===%n", scenario.name(), i + 1, N);
                var plainResponse = askAndParse(scenario, PLAIN_THINKING_FIELD);
                var plainScores = judge(scenario, plainResponse);
                plainResults.get(scenario.name()).add(plainScores);

                System.out.printf("%n=== %s RUN %d/%d — CALIBRATED ===%n", scenario.name(), i + 1, N);
                var calibratedResponse = askAndParse(scenario, CALIBRATED_THINKING_FIELD);
                var calibratedScores = judge(scenario, calibratedResponse);
                calibratedResults.get(scenario.name()).add(calibratedScores);
            }
        }

        var summary = buildSummary(plainResults, calibratedResults);
        writeResult("thinking-field-eval-summary", summary);

        System.out.printf("%n=== AGGREGATE RESULTS ===%n");
        for (var dim : List.of("register", "uncertainty", "channel_coverage", "thinking_dialogue_separation")) {
            double plainMean = meanDimension(plainResults, dim);
            double calibratedMean = meanDimension(calibratedResults, dim);
            System.out.printf("%-30s  plain=%.2f  calibrated=%.2f  delta=%+.2f%n",
                    dim, plainMean, calibratedMean, calibratedMean - plainMean);
        }

        double plainRegister = meanDimension(plainResults, "register");
        double calibratedRegister = meanDimension(calibratedResults, "register");
        assertThat(calibratedRegister)
                .as("Calibrated thinking field should not regress on register (phenomenological quality)")
                .isGreaterThanOrEqualTo(plainRegister - 0.5);
    }

    private AgentResponse askAndParse(Scenario scenario, String thinkingField) {
        var userPrompt = scenario.observation() + "\n\n" +
                         String.format(RESPONSE_FORMAT_TEMPLATE, thinkingField);
        var rawResponse = agentProvider.invoke(AgentSessionConfig.of(scenario.systemPrompt(), userPrompt))
                .filter(e -> e instanceof AgentEvent.TextDelta)
                .map(e -> ((AgentEvent.TextDelta) e).text())
                .collect().with(Collectors.joining())
                .await().atMost(Duration.ofSeconds(120));

        var parsed = AgentResponse.parse(rawResponse);
        System.out.printf("[thinking] %s%n", truncate(parsed.thinking(), 500));
        System.out.printf("[dialogue] %s%n", truncate(parsed.dialogue(), 300));
        return parsed;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> judge(Scenario scenario, AgentResponse response) {
        var prompt = String.format(JUDGE_PROMPT,
                scenario.systemPrompt(),
                scenario.observation(),
                response.thinking() != null ? response.thinking() : "(empty)");

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                var judgeResponse = agentProvider.invoke(
                        AgentSessionConfig.of("You are a precise evaluation judge. Respond only with JSON.", prompt))
                        .filter(e -> e instanceof AgentEvent.TextDelta)
                        .map(e -> ((AgentEvent.TextDelta) e).text())
                        .collect().with(Collectors.joining())
                        .await().atMost(Duration.ofSeconds(60));

                var json = extractJson(judgeResponse);
                var mapper = new ObjectMapper();
                var scores = mapper.readValue(json, Map.class);
                System.out.printf("[judge] register=%s uncertainty=%s channels=%s separation=%s%n",
                        scores.get("register"), scores.get("uncertainty"),
                        scores.get("channel_coverage"), scores.get("thinking_dialogue_separation"));
                System.out.printf("[judge] reasoning: %s%n", scores.get("reasoning"));

                var result = new LinkedHashMap<String, Object>();
                result.put("register", toInt(scores.get("register")));
                result.put("uncertainty", toInt(scores.get("uncertainty")));
                result.put("channel_coverage", toInt(scores.get("channel_coverage")));
                result.put("thinking_dialogue_separation", toInt(scores.get("thinking_dialogue_separation")));
                result.put("reasoning", scores.getOrDefault("reasoning", ""));
                result.put("thinking", response.thinking() != null ? response.thinking() : "");
                result.put("dialogue", response.dialogue() != null ? response.dialogue() : "");
                return result;
            } catch (Exception e) {
                System.err.printf("[judge] attempt %d/%d failed: %s%n", attempt, MAX_RETRIES, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try { Thread.sleep(RETRY_BACKOFF_MS * attempt); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); return Map.of(); }
                }
            }
        }
        return Map.of();
    }

    private Map<String, Object> buildSummary(
            Map<String, List<Map<String, Object>>> plainResults,
            Map<String, List<Map<String, Object>>> calibratedResults) {
        var summary = new LinkedHashMap<String, Object>();
        summary.put("n", N);
        summary.put("scenarios", SCENARIOS.stream().map(Scenario::name).toList());

        var perScenario = new LinkedHashMap<String, Object>();
        for (var scenario : SCENARIOS) {
            var scenarioSummary = new LinkedHashMap<String, Object>();
            for (var dim : List.of("register", "uncertainty", "channel_coverage", "thinking_dialogue_separation")) {
                double plainMean = plainResults.get(scenario.name()).stream()
                        .mapToInt(m -> toInt(m.get(dim))).average().orElse(0);
                double calibratedMean = calibratedResults.get(scenario.name()).stream()
                        .mapToInt(m -> toInt(m.get(dim))).average().orElse(0);
                scenarioSummary.put(dim + "_plain", plainMean);
                scenarioSummary.put(dim + "_calibrated", calibratedMean);
                scenarioSummary.put(dim + "_delta", calibratedMean - plainMean);
            }
            perScenario.put(scenario.name(), scenarioSummary);
        }
        summary.put("per_scenario", perScenario);

        var aggregate = new LinkedHashMap<String, Object>();
        for (var dim : List.of("register", "uncertainty", "channel_coverage", "thinking_dialogue_separation")) {
            aggregate.put(dim + "_plain", meanDimension(plainResults, dim));
            aggregate.put(dim + "_calibrated", meanDimension(calibratedResults, dim));
            aggregate.put(dim + "_delta", meanDimension(calibratedResults, dim) - meanDimension(plainResults, dim));
        }
        summary.put("aggregate", aggregate);

        summary.put("plain_detail", plainResults);
        summary.put("calibrated_detail", calibratedResults);

        return summary;
    }

    private double meanDimension(Map<String, List<Map<String, Object>>> results, String dim) {
        return results.values().stream()
                .flatMap(List::stream)
                .mapToInt(m -> toInt(m.get(dim)))
                .average().orElse(0);
    }

    private void writeResult(String name, Map<String, Object> data) {
        try {
            var outputFile = OUTPUT_DIR.resolve(name + ".json");
            Files.createDirectories(outputFile.getParent());
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
                    .writeValue(outputFile.toFile(), data);
            System.out.printf("[output] %s%n", outputFile);
        } catch (Exception e) {
            System.err.printf("[%s] Failed to write result: %s%n", name, e.getMessage());
        }
    }

    private static int toInt(Object val) {
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) { try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; } }
        return 0;
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
