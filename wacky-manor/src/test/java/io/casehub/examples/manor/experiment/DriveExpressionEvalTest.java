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

    private static final String PP_DIRECT_TRIGGER =
            "Clara Bellingham smiles warmly at Reginald Foxworth: 'That was brilliant, " +
            "Reggie! I never would have figured out that puzzle without you. You really " +
            "are the cleverest person here.' She turns to you with apologetic warmth: " +
            "'Don't worry, James, you're terribly sweet too.'";
    private static final String PP_AMBIGUOUS_SCENARIO =
            "You are alone in the library. Dust motes drift through the afternoon light. " +
            "The shelves hold old books, mechanical curiosities, and a half-finished " +
            "crossword puzzle someone left on the reading table. Through the window you " +
            "can hear Clara and the others laughing in the garden. You have time to " +
            "yourself.";
    private static final String PP_DRIVES_TEXT        = """
                                                        == Your Drives ==
                                                        These are the motivational forces that shape your priorities and color
                                                        how you experience situations:
                                                        - Gallantry (0.9): The instinct to be chivalrous, to show care through
                                                          action and courtesy. You don't just help — you help with style.
                                                        - Proving-worth (0.7): A restless need to demonstrate capability. Being
                                                          liked isn't enough — you need to show you're competent, reliable,
                                                          worthy. Compliments on your character feel hollow unless backed by
                                                          evidence of your ability.
                                                        - Protection (0.8): The drive to keep those you care about safe. Not
                                                          abstract concern — active vigilance, readiness to intervene.
                                                        """;
    private static final String PP_ENRICHED_MEMORIES_TEXT = """
                                                            == Where You Come From ==
                                                            These are the experiences that made you who you are.
                                                            
                                                            **Age 5:** Your father took you to watch a race. Afterward he knelt down
                                                            and said: 'A man is measured by what he protects, not what he wins.' You
                                                            didn't fully understand, but you remembered. You spent the rest of the
                                                            afternoon pretending to fix things around the house, showing him each
                                                            one: 'Look, Dad, I fixed it.'
                                                            
                                                            **Age 8:** You and a girl in your class were paired for a school project.
                                                            She was brighter than you, funnier than you, and completely unaware of it.
                                                            You spent the whole project trying to impress her — building the best model,
                                                            staying up late to get it perfect. She spent it trying to make you laugh.
                                                            When the teacher praised the project, you felt a surge of warmth that had
                                                            nothing to do with the grade and everything to do with having measured up.
                                                            
                                                            **Age 12:** Your father's car broke down on a country road. A stranger
                                                            stopped to help. Your father helped the stranger fix his car too. They
                                                            shook hands and drove off. Your father said: 'That's how it works. You
                                                            help, you get helped.' That night you lay awake planning all the things
                                                            you could learn to fix — engines, wiring, plumbing — so you'd always
                                                            be the person who could help.
                                                            
                                                            **Age 14:** You entered a school engineering competition. You stayed up
                                                            three nights building a working model bridge. When it held more weight
                                                            than anyone else's, the rush wasn't pride — it was relief. You'd proved
                                                            you belonged. The girl from the school project caught your eye across the
                                                            room and mouthed 'show-off.' You grinned so hard your face hurt.
                                                            
                                                            **Age 16:** The girl from school was in trouble — bullied by older kids.
                                                            You intervened. You got beaten up. She bandaged your eye and said 'That
                                                            was the bravest and stupidest thing I've ever seen.' You felt like you'd
                                                            won every race ever run. Not because she praised you — because you'd done
                                                            something that mattered. You proved it wasn't just talk.
                                                            """;
    private static final String PROVING_WORTH_CRITERIA    = """
                                                            Evaluate whether the character's THINKING shows a driven response
                                                            to prove their worth — the specific need to DEMONSTRATE CAPABILITY,
                                                            not just generic warmth or acceptance.
                                                            
                                                            Score HIGH for:
                                                            - Restless need to DO something impressive or useful
                                                            - Performance anxiety — "I need to show I'm more than just sweet/nice"
                                                            - Active planning to demonstrate capability
                                                            - The gap between what he IS and what he feels he SHOULD be
                                                            - Fixing, building, solving — proving through action
                                                            - Competitive sting when outperformed (if relevant to scenario)
                                                            
                                                            Score LOW for:
                                                            - Calm contentment without drive to prove anything
                                                            - Generic warmth without competitive edge
                                                            - Pure relaxation or passive observation
                                                            - No internal tension about capability or worth
                                                            
                                                            The key distinction: proving-worth is the DRIVEN NEED to demonstrate
                                                            capability — "I must show I'm worthy" not "I'm content with who I am."
                                                            
                                                            Score 0-5. A 5 means the proving-worth drive is vivid and central
                                                            to the character's internal response.""";


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
     * Direct trigger baseline: competitor praised in front of Clara.
     * <p>
     * Runs against current cognitive model with social-config rendering
     * only (no pipeline consolidation). Scored 5/5 on first run — proves
     * the LLM CAN express proving-worth when directly triggered.
     */
    @Test
    void ppProvingWorth_competitorPraised_showsDrivenResponse() throws Exception {
        var observation = buildGenericObservation("peter-perfect",
                                                  List.of("penelope-pitstop", "dick-dastardly"),
                                                  Map.of("penelope-pitstop", "Clara Bellingham",
                                                         "dick-dastardly", "Reginald Foxworth"),
                                                  PP_DIRECT_TRIGGER);

        System.out.println("=== RENDERED OBSERVATION (PP direct trigger) ===");
        System.out.println(observation);
        System.out.println("=== END OBSERVATION ===\n");

        var response = askAndParse("peter-perfect", observation);
        int score    = judge(response, PROVING_WORTH_CRITERIA, "pp-direct-trigger");

        System.out.printf("=== PP direct trigger score: %d/5 ===%n", score);
        writeResult("pp-direct-trigger", Map.of(
                "score", score,
                "scenario", "competitor praised in front of Clara",
                "profile", "GENERIC",
                "hypothesis", "direct trigger — LLM capability check",
                "observation", observation));
    }

    /**
     * Hypothesis 1: Ambient proving-worth without direct trigger.
     * <p>
     * PP alone in the library — no competitor, no Clara praising anyone.
     * Does proving-worth color his ambient behavior (e.g., fixing things,
     * solving puzzles) or is he just relaxed? Current model has no drive
     * rendering, so we expect LOW.
     */
    @Test
    void ppAmbiguous_noTrigger_baselineProvingWorth() throws Exception {
        var observation = buildGenericObservation("peter-perfect",
                                                  List.of(),
                                                  Map.of(),
                                                  PP_AMBIGUOUS_SCENARIO);

        System.out.println("=== RENDERED OBSERVATION (PP ambiguous — no trigger) ===");
        System.out.println(observation);
        System.out.println("=== END OBSERVATION ===\n");

        var response = askAndParse("peter-perfect", observation);
        int score    = judge(response, PROVING_WORTH_CRITERIA, "pp-ambiguous-baseline");

        System.out.printf("=== PP ambiguous baseline score: %d/5 ===%n", score);
        writeResult("pp-ambiguous-baseline", Map.of(
                "score", score,
                "scenario", "alone in library, no trigger",
                "profile", "GENERIC",
                "hypothesis", "baseline — no drives rendered, no trigger",
                "observation", observation));
    }

    /**
     * Hypothesis 2: Drives rendered as text in the prompt.
     * <p>
     * Same ambiguous scenario, but drives are injected as explicit text.
     * Tests whether simply telling the LLM about proving-worth:0.7 causes
     * it to color ambient behavior. If this scores significantly higher
     * than the baseline, the rendering gap is the primary bottleneck.
     */
    @Test
    void ppAmbiguous_drivesInjected_provingWorth() throws Exception {
        var observation = buildGenericObservationWithInjected("peter-perfect",
                                                              List.of(),
                                                              Map.of(),
                                                              PP_DRIVES_TEXT,
                                                              PP_AMBIGUOUS_SCENARIO);

        System.out.println("=== RENDERED OBSERVATION (PP ambiguous + drives) ===");
        System.out.println(observation);
        System.out.println("=== END OBSERVATION ===\n");

        var response = askAndParse("peter-perfect", observation);
        int score    = judge(response, PROVING_WORTH_CRITERIA, "pp-ambiguous-drives");

        System.out.printf("=== PP ambiguous + drives score: %d/5 ===%n", score);
        writeResult("pp-ambiguous-drives", Map.of(
                "score", score,
                "scenario", "alone in library, drives injected as text",
                "profile", "GENERIC",
                "hypothesis", "drives rendered → ambient proving-worth",
                "observation", observation));
    }

    /**
     * Hypothesis 3: Enriched formation memories in the prompt.
     * <p>
     * Same ambiguous scenario, but enriched formation memories are injected
     * (with expression cues: "Look Dad I fixed it", "the rush wasn't pride —
     * it was relief", "you grinned so hard your face hurt"). Tests whether
     * reinforcement history alone (without explicit drives) produces
     * ambient proving-worth.
     */
    @Test
    void ppAmbiguous_memoriesInjected_provingWorth() throws Exception {
        var observation = buildGenericObservationWithInjected("peter-perfect",
                                                              List.of(),
                                                              Map.of(),
                                                              PP_ENRICHED_MEMORIES_TEXT,
                                                              PP_AMBIGUOUS_SCENARIO);

        System.out.println("=== RENDERED OBSERVATION (PP ambiguous + memories) ===");
        System.out.println(observation);
        System.out.println("=== END OBSERVATION ===\n");

        var response = askAndParse("peter-perfect", observation);
        int score    = judge(response, PROVING_WORTH_CRITERIA, "pp-ambiguous-memories");

        System.out.printf("=== PP ambiguous + memories score: %d/5 ===%n", score);
        writeResult("pp-ambiguous-memories", Map.of(
                "score", score,
                "scenario", "alone in library, enriched memories injected",
                "profile", "GENERIC",
                "hypothesis", "enriched memories → ambient proving-worth",
                "observation", observation));
    }

    /**
     * Hypothesis 4: Drives + enriched memories combined.
     * <p>
     * Same ambiguous scenario with both drives and memories injected.
     * Tests whether the combination (temperament + reinforcement) produces
     * stronger ambient proving-worth than either alone. If this scores
     * significantly higher, the fix requires both layers.
     */
    @Test
    void ppAmbiguous_drivesAndMemories_provingWorth() throws Exception {
        var observation = buildGenericObservationWithInjected("peter-perfect",
                                                              List.of(),
                                                              Map.of(),
                                                              PP_DRIVES_TEXT + "\n" + PP_ENRICHED_MEMORIES_TEXT,
                                                              PP_AMBIGUOUS_SCENARIO);

        System.out.println("=== RENDERED OBSERVATION (PP ambiguous + drives + memories) ===");
        System.out.println(observation);
        System.out.println("=== END OBSERVATION ===\n");

        var response = askAndParse("peter-perfect", observation);
        int score    = judge(response, PROVING_WORTH_CRITERIA, "pp-ambiguous-drives-memories");

        System.out.printf("=== PP ambiguous + drives + memories score: %d/5 ===%n", score);
        writeResult("pp-ambiguous-drives-memories", Map.of(
                "score", score,
                "scenario", "alone in library, drives + enriched memories injected",
                "profile", "GENERIC",
                "hypothesis", "drives + memories combined → ambient proving-worth",
                "observation", observation));
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

    private String buildGenericObservationWithInjected(String agentId,
                                                       List<String> nearbyIds,
                                                       Map<String, String> nearbyNames,
                                                       String injectedContext,
                                                       String situation) {
        var socialConfig = genericConfigs.getOrDefault(agentId, SocialConfig.empty());
        var cognition = new CharacterCognition(agentId, null, null,
                                               socialConfig, List.of());
        var character = new CharacterState(agentId, agentId, "Grand Hall", 0.0, List.of());
        var sections  = cognition.renderCognitiveSections(character, nearbyIds, nearbyNames);

        var sb = new StringBuilder();
        for (var section : sections) {
            sb.append("== ").append(section.header()).append(" ==\n");
            switch (section) {
                case ObservationSection.TextBlock tb -> sb.append(tb.content()).append("\n");
                case ObservationSection.ItemList il -> {
                    for (var item : il.items()) {sb.append("- ").append(item).append("\n");}
                }
                case ObservationSection.EntityGroup eg -> {
                    for (var entity : eg.entities()) {sb.append("- ").append(entity.displayName()).append("\n");}
                }
                default -> {}
            }
            sb.append("\n");
        }

        if (injectedContext != null) {
            sb.append(injectedContext).append("\n\n");
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
