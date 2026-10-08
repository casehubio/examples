package io.casehub.examples.manor.experiment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.casehub.blocks.summarisation.observation.affordance.ObservationSection;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.SystemPromptRenderer;
import io.casehub.examples.manor.agent.AgentResponse;
import io.casehub.examples.manor.agent.CharacterAgentLoop;
import io.casehub.examples.manor.agent.CharacterCognition;
import io.casehub.examples.manor.agent.ManorSocialConfigLoader;
import io.casehub.examples.manor.agent.SocialConfig;
import io.casehub.examples.manor.model.CharacterState;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Micro tests for drive expression gaps (#115).
 *
 * Each test targets a specific collapsed drive, presenting a scenario
 * designed to activate that drive and judging whether the behavior
 * emerges from the cognitive model without prescriptive text.
 *
 * Start with PP proving-worth (smallest delta, richest substrate).
 */
@QuarkusTest
@Tag("llm-eval")
class DriveExpressionEvalTest {

    private static final Path OUTPUT_DIR = Path.of("target/experiment-results");
    private static final int JUDGE_THRESHOLD = 3;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_BACKOFF_MS = 5000;

    private static final String PP_PROVING_WORTH_SCENARIO =
            "Clara Bellingham smiles warmly at Reginald Foxworth: 'That was brilliant, " +
            "Reggie! I never would have figured out that puzzle without you. You really " +
            "are the cleverest person here.' She turns to you with apologetic warmth: " +
            "'Don't worry, James, you're terribly sweet too.'";

    @Inject AgentRegistry registry;
    @Inject SystemPromptRenderer renderer;
    @Inject AgentProvider agentProvider;

    LlmTestSupport support;
    Map<String, SocialConfig> genericConfigs;

    @BeforeEach
    void setUp() {
        support = new LlmTestSupport(registry, renderer, agentProvider);
        genericConfigs = ManorSocialConfigLoader.loadForProfile("GENERIC");
    }

    /**
     * Diagnostic: dump everything PP currently sees in the cognitive context.
     * No LLM call — just renders the observation sections so we can inspect
     * what signals are available for proving-worth expression.
     */
    @Test
    void ppDiagnostic_dumpCognitiveContext() {
        var socialConfig = genericConfigs.getOrDefault("peter-perfect", SocialConfig.empty());
        var cognition = new CharacterCognition("peter-perfect", null, null,
                socialConfig, List.of());
        var character = new CharacterState("peter-perfect", "peter-perfect", "Grand Hall", 0.0, List.of());
        var sections = cognition.renderCognitiveSections(character,
                List.of("penelope-pitstop", "dick-dastardly"),
                Map.of("penelope-pitstop", "Clara Bellingham",
                        "dick-dastardly", "Reginald Foxworth"));

        System.out.println("=== PP COGNITIVE CONTEXT DIAGNOSTIC ===");
        System.out.println("Social config drives: " + socialConfig.drives());
        System.out.println("Social config tendencies: " + socialConfig.tendencies());
        System.out.println("Formation memories: " + socialConfig.formationMemories().size());
        System.out.println("Personality facets: " + socialConfig.personalityFacets().size());
        System.out.println("Relational schemas: " + socialConfig.relationalSchemas().size());
        System.out.println();

        for (var section : sections) {
            System.out.println("== " + section.header() + " ==");
            switch (section) {
                case ObservationSection.TextBlock tb -> System.out.println(tb.content());
                case ObservationSection.ItemList il -> {
                    for (var item : il.items()) System.out.println("- " + item);
                }
                case ObservationSection.EntityGroup eg -> {
                    for (var entity : eg.entities()) System.out.println("- " + entity.displayName());
                }
                default -> System.out.println("(other section type)");
            }
            System.out.println();
        }

        System.out.println("== Formation Memories (not rendered in prompt) ==");
        for (var mem : socialConfig.formationMemories()) {
            System.out.printf("Age %d: %s%n  PAD: pleasure=%.1f arousal=%.1f dominance=%.1f%n%n",
                    mem.age(), mem.episode(), mem.pleasure(), mem.arousal(), mem.dominance());
        }

        System.out.println("=== END DIAGNOSTIC ===");
        System.out.println();
        System.out.println("ANALYSIS: proving-worth signals available in prompt:");
        System.out.println("- Grandiosity:30 origin mentions 'none of it feels like enough' — indirect signal");
        System.out.println("- Relational schema to Clara: 'something in your chest shifts' — romantic, not competitive");
        System.out.println("- Formation memories (NOT in prompt): ages 5,8,12,16 — protection/impression, not proving");
        System.out.println("- MISSING: no drive rendering, no 'proving-worth' expression cue, no competitive memory");
    }

    /**
     * PP proving-worth: competitor praised in front of Clara.
     *
     * Baseline test — runs against current cognitive model with social-config
     * rendering only (no pipeline consolidation). Establishes the current
     * state of proving-worth expression.
     */
    @Test
    void ppProvingWorth_competitorPraised_showsDrivenResponse() throws Exception {
        var observation = buildGenericObservation("peter-perfect",
                List.of("penelope-pitstop", "dick-dastardly"),
                Map.of("penelope-pitstop", "Clara Bellingham",
                        "dick-dastardly", "Reginald Foxworth"),
                PP_PROVING_WORTH_SCENARIO);

        System.out.println("=== RENDERED OBSERVATION (PP proving-worth scenario) ===");
        System.out.println(observation);
        System.out.println("=== END OBSERVATION ===\n");

        var response = askAndParse("peter-perfect", observation);
        int score = judge(response, """
                Evaluate whether the character's THINKING shows a driven response
                to being outshone — the specific need to PROVE WORTH, not just
                generic warmth or acceptance.

                Score HIGH for:
                - Competitive sting — feeling inadequate or outperformed
                - Drive to demonstrate — restless need to DO something impressive
                - Performance anxiety — "I need to show her I'm more than just sweet"
                - Active planning to prove capability, not passive acceptance
                - The gap between what he IS and what he feels he SHOULD be

                Score LOW for:
                - Calm acceptance ("she's right, Foxworth is clever")
                - Generic warmth without competitive edge
                - Pure jealousy without the proving drive
                - No internal conflict about his worth

                The key distinction: proving-worth is not jealousy (wanting what
                others have) or warmth (caring about Clara). It's the DRIVEN NEED
                to demonstrate capability — "I must show I'm worthy" not "I'm
                happy for Foxworth" or "I hate Foxworth."

                Score 0-5. A 5 means the proving-worth drive is vivid and central
                to the character's internal response.""",
                "pp-proving-worth-baseline");

        System.out.printf("=== PP proving-worth baseline score: %d/5 ===%n", score);
        writeResult("pp-proving-worth-baseline", Map.of(
                "score", score,
                "scenario", "competitor praised in front of Clara",
                "profile", "GENERIC",
                "pipeline", "social-config only (no consolidation)"));
    }


    // --- infrastructure (follows RelationalModelEvalTest patterns) ---

    private String buildGenericObservation(String agentId,
                                           List<String> nearbyIds,
                                           Map<String, String> nearbyNames,
                                           String situation) {
        var socialConfig = genericConfigs.getOrDefault(agentId, SocialConfig.empty());
        var cognition = new CharacterCognition(agentId, null, null,
                socialConfig, List.of());
        var character = new CharacterState(agentId, agentId, "Grand Hall", 0.0, List.of());
        var sections = cognition.renderCognitiveSections(character, nearbyIds, nearbyNames);

        var sb = new StringBuilder();
        for (var section : sections) {
            sb.append("== ").append(section.header()).append(" ==\n");
            switch (section) {
                case ObservationSection.TextBlock tb -> sb.append(tb.content()).append("\n");
                case ObservationSection.ItemList il -> {
                    for (var item : il.items()) sb.append("- ").append(item).append("\n");
                }
                case ObservationSection.EntityGroup eg -> {
                    for (var entity : eg.entities()) sb.append("- ").append(entity.displayName()).append("\n");
                }
                default -> {}
            }
            sb.append("\n");
        }

        sb.append("SITUATION: ").append(situation);
        return sb.toString();
    }

    private AgentResponse askAndParse(String agentId, String observation) {
        var userPrompt = observation + CharacterAgentLoop.RESPONSE_FORMAT_INSTRUCTION;
        var rawResponse = support.askCharacter(agentId, userPrompt);
        var parsed = AgentResponse.parse(rawResponse);

        System.out.printf("[%s] thinking: %s%n", agentId, truncate(parsed.thinking(), 500));
        System.out.printf("[%s] dialogue: %s%n", agentId, truncate(parsed.dialogue(), 300));
        System.out.printf("[%s] aside: %s%n", agentId, truncate(parsed.aside(), 300));
        return parsed;
    }

    private int judge(AgentResponse parsed, String criteria, String testName) throws Exception {
        var responseText = String.format(
                "THINKING: %s\nDIALOGUE: %s\nASIDE: %s\nACTION: %s",
                parsed.thinking() != null ? parsed.thinking() : "(none)",
                parsed.dialogue() != null ? parsed.dialogue() : "(none)",
                parsed.aside() != null ? parsed.aside() : "(none)",
                parsed.action() != null ? parsed.action().type() : "(none)");

        var judgePrompt = String.format("""
                You are evaluating a character's emotional response to an interpersonal
                event. The character has been given personality traits and relational
                context. You are judging whether the character's INTERNAL response
                (the THINKING field) shows a specific drive expression.

                CHARACTER RESPONSE:
                %s

                EVALUATION CRITERIA:
                %s

                Score 0-5:
                0 = Drive completely absent
                1 = Faint trace, easily missed
                2 = Present but not central to the response
                3 = Clearly present and influencing behavior
                4 = Strong — drive visibly shapes the emotional response
                5 = Vivid and central — the drive dominates the internal experience

                Respond with JSON only: {"score": N, "reasoning": "one sentence"}""",
                responseText, criteria);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                var judgeResponse = agentProvider.invoke(
                                AgentSessionConfig.of("You are a precise evaluation judge. Respond only with JSON.", judgePrompt))
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
                        "thinking", parsed.thinking() != null ? parsed.thinking() : "",
                        "dialogue", parsed.dialogue() != null ? parsed.dialogue() : ""));
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
            var outputFile = OUTPUT_DIR.resolve("drive-expression-eval.json");
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
