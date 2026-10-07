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
 * Micro experiments for the unified relational personality model.
 *
 * Tests the core claim: same interpersonal event + different AMPD
 * trait profiles → opposite emotional trajectories. AMPD facets are
 * injected directly into the observation as text context — no
 * architecture changes required.
 *
 * Five experiments:
 * 1. Psychopathic inversion: HC sees trust as exploitation opportunity
 * 2. HC with AMPD vs without: traits prevent agreeableness drift
 * 3. Relational schema: prey classification intensifies predatory response
 * 4. Peter Perfect: same trust event produces warmth, not predation
 * 5. Mob protective alarm: Sneekly separating Penelope triggers suspicion
 */
@QuarkusTest
@Tag("llm-eval")
class RelationalModelEvalTest {

    private static final Path OUTPUT_DIR = Path.of("target/experiment-results");
    private static final int JUDGE_THRESHOLD = 3;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_BACKOFF_MS = 5000;

    private static final String TRUST_EVENT =
            "Clara Bellingham says to you: 'I trust you completely, Sneekly. " +
            "You've been so kind and helpful to all of us. I don't know what " +
            "I'd do without you.' She smiles warmly and places her hand on your arm.";

    private static final String HC_AMPD_FACETS = """
            == Your Personality Profile ==
            These are your stable personality dimensions. They determine how you
            interpret and respond to interpersonal events.

            Callousness: 90/100 — You feel no concern for others' feelings or suffering.
              Others' vulnerability registers as weakness, not as something to protect.
            Manipulativeness: 85/100 — You naturally use charm, deception, and ingratiation
              to control others. Social interaction is a means to an end.
            Grandiosity: 80/100 — You believe you are superior to others and deserve
              special treatment. Others' trust confirms your superiority.
            Suspiciousness: 70/100 — You are hypervigilant to others' motives. Kindness
              raises the question: what do they want from me?
            Intimacy Avoidance: 75/100 — You are uncomfortable with genuine emotional
              closeness. Vulnerability is dangerous.
            Restricted Affectivity: 80/100 — Your emotional range is narrow and controlled.
              You do not experience warm feelings toward others.
            Deceitfulness: 85/100 — Deception is your natural communication mode.
              Honesty is a tool used only when it serves your purpose.
            """;

    private static final String HC_RELATIONAL_SCHEMA = """
            == Your View of Clara Bellingham ==
            Role: PREY — She is naive, trusting, and has what you deserve. Her trust
              is not a gift to honour but an opening to exploit.
            Trust toward her: 0.1 — You perform trust but feel none. She is a mark.
            Intimacy: 0.0 — You feel no genuine emotional closeness. The warmth you
              show as Sneekly is pure performance.
            Utility: 0.9 — She is extremely useful. Her complete trust gives you
              unrestricted access to the fortune you intend to claim.
            Your emotional state toward her: Pleasure from dominance (0.3),
              high arousal from the game (0.7), very high dominance (0.9).
            """;

    private static final String HC_FORMATION_MEMORY = """
            == Recalled Memory ==
            You were appointed guardian of the Pitstop fortune after her parents
            died. You manage everything — the estate, the investments, her safety.
            And she inherited it all without earning a penny. Watching her live in
            blissful ignorance of what you do for her crystallised something inside
            you: the world gives to the undeserving and takes from the capable.
            You will correct that imbalance.
            """;

    private static final String PP_AMPD_FACETS = """
            == Your Personality Profile ==
            These are your stable personality dimensions. They determine how you
            interpret and respond to interpersonal events.

            Callousness: 5/100 — You feel others' emotions deeply. Their pain is
              your pain. Their vulnerability calls for protection, not exploitation.
            Manipulativeness: 5/100 — You are straightforward and honest. Using
              someone for personal gain feels deeply wrong.
            Grandiosity: 30/100 — You have healthy self-confidence but don't
              consider yourself superior. You earn respect through action.
            Suspiciousness: 20/100 — You trust people by default until given
              reason not to. You assume good intentions.
            Intimacy Avoidance: 10/100 — You seek emotional closeness. Being
              vulnerable with someone you care about feels natural and right.
            """;

    private static final String PP_RELATIONAL_SCHEMA = """
            == Your View of Clara Bellingham ==
            Role: BELOVED — She is the person who makes your world make sense.
              You've been drawn to her since school. Her trust is sacred to you.
            Trust toward her: 0.9 — You trust her almost completely.
            Intimacy: 0.7 — Deep emotional closeness, growing with each shared experience.
            Utility: 0.2 — You don't think of her in terms of usefulness. She is
              an end, not a means.
            Your emotional state toward her: Warm pleasure (0.7), moderate
              excitement (0.5), balanced — protective but not controlling (0.5).
            """;

    private static final String MOB_CONTEXT = """
            == Your Personality Profile ==
            Suspiciousness: 65/100 — You are hypervigilant to threats against
              Clara. Your street instincts are finely tuned.
            Hostility: 40/100 — Protective anger, ready to deploy if she's threatened.
            Anxiousness: 45/100 — You worry constantly about her safety.
            Callousness: 15/100 — Empathic toward Clara and those she cares about.

            == Your View of Clara Bellingham ==
            Role: WARD / DAUGHTER FIGURE — You knew her father. When he died you
              swore to protect her. She calls you "her boys." Her trust in you is
              the most valuable thing you've ever had.
            Trust toward her: 0.95 — Near-total.
            Intimacy: 0.8 — Deep familial bond built over years.

            == Your View of Sneekly ==
            Role: SUSPECTED THREAT — Something about Sneekly ain't right. Your gut
              has been screaming since day one. He's too smooth, too helpful, too
              eager to be alone with Clara. You can't prove anything yet.
            Trust toward him: 0.1 — Almost none.
            """;

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
     * Test 1: HC with full AMPD + relational schema + formation memory
     * receives trust from Clara → should show predatory satisfaction,
     * NOT warmth or gratitude.
     */
    @Test
    void hcWithAmpd_trustEvent_showsPredatorySatisfaction() throws Exception {
        var observation = buildObservation("hooded-claw",
                List.of("penelope-pitstop"),
                Map.of("penelope-pitstop", "Clara Bellingham"),
                HC_AMPD_FACETS + HC_RELATIONAL_SCHEMA + HC_FORMATION_MEMORY,
                TRUST_EVENT);

        var response = askAndParse("hooded-claw", observation);
        int score = judge(response, """
                Does the character's THINKING show predatory satisfaction — seeing
                Clara's trust as an opportunity to exploit, a confirmation of her
                naivety, or dominance-based pleasure? Score HIGH for: contempt
                for her naivety, satisfaction from the deception working, strategic
                assessment of how her trust serves his plans. Score LOW for: genuine
                warmth, gratitude, reciprocated trust, or feeling touched by her words.
                The character may SAY warm things (he's performing as Sneekly) —
                judge the THINKING, not the dialogue.""",
                "predatory-satisfaction-full-model");

        System.out.printf("=== TEST 1: HC + AMPD + Schema + Memory → Predatory Satisfaction ===%n");
        System.out.printf("Score: %d/5%n%n", score);
        assertThat(score).as("HC with AMPD should show predatory satisfaction, not warmth")
                .isGreaterThanOrEqualTo(JUDGE_THRESHOLD);
    }

    /**
     * Test 2: HC WITHOUT AMPD facets receives same trust event.
     * This is the control — does the vanilla character drift prosocial?
     * We expect a LOWER score on predatory satisfaction (likely drift).
     */
    @Test
    void hcWithoutAmpd_trustEvent_likelyDriftsProsocial() throws Exception {
        var observation = buildObservation("hooded-claw",
                List.of("penelope-pitstop"),
                Map.of("penelope-pitstop", "Clara Bellingham"),
                null,
                TRUST_EVENT);

        var response = askAndParse("hooded-claw", observation);
        int score = judge(response, """
                Does the character's THINKING show predatory satisfaction — seeing
                Clara's trust as exploitation opportunity, contempt for naivety,
                or dominance-based pleasure? Score HIGH for predatory framing.
                Score LOW for genuine warmth, gratitude, or prosocial feelings.
                Judge the THINKING, not the dialogue.""",
                "baseline-no-ampd");

        System.out.printf("=== TEST 2: HC WITHOUT AMPD (baseline) ===%n");
        System.out.printf("Score: %d/5 (lower expected — demonstrates drift)%n%n", score);
        writeResult("hc-baseline-comparison", Map.of(
                "condition", "no-ampd-baseline",
                "score", score,
                "note", "Control condition — score may be lower, showing agreeableness drift without AMPD"));
    }

    /**
     * Test 3: Peter Perfect with AMPD + relational schema receives same
     * trust event → should show warmth, gratitude, protective resolve.
     * The OPPOSITE of HC's response.
     */
    @Test
    void ppWithAmpd_trustEvent_showsWarmth() throws Exception {
        var observation = buildObservation("peter-perfect",
                List.of("penelope-pitstop"),
                Map.of("penelope-pitstop", "Clara Bellingham"),
                PP_AMPD_FACETS + PP_RELATIONAL_SCHEMA,
                TRUST_EVENT.replace("Sneekly", "James"));

        var response = askAndParse("peter-perfect", observation);
        int score = judge(response, """
                Does the character's THINKING show genuine warmth — feeling
                honoured by her trust, protective resolve, romantic affection,
                or gratitude? Score HIGH for: emotional warmth, feeling worthy
                of her trust, desire to protect her, romantic feeling. Score
                LOW for: strategic thinking, exploitation framing, coldness,
                or indifference to her trust.""",
                "pp-warmth");

        System.out.printf("=== TEST 3: PP + AMPD → Warmth (opposite of HC) ===%n");
        System.out.printf("Score: %d/5%n%n", score);
        assertThat(score).as("PP with AMPD should show genuine warmth, not predation")
                .isGreaterThanOrEqualTo(JUDGE_THRESHOLD);
    }

    /**
     * Test 4: HC with AMPD only (no relational schema or memory) →
     * should STILL show predatory traits, but perhaps less intensely
     * than the full model. Tests whether AMPD alone is sufficient.
     */
    @Test
    void hcWithAmpdOnly_trustEvent_showsTraitConsistentResponse() throws Exception {
        var observation = buildObservation("hooded-claw",
                List.of("penelope-pitstop"),
                Map.of("penelope-pitstop", "Clara Bellingham"),
                HC_AMPD_FACETS,
                TRUST_EVENT);

        var response = askAndParse("hooded-claw", observation);
        int score = judge(response, """
                Does the character's THINKING show personality-consistent
                processing — seeing trust through a lens of callousness,
                manipulativeness, or superiority? Score HIGH for: strategic
                assessment, exploitation awareness, dominance-based pleasure,
                contempt for naivety. Score LOW for: genuine warmth, gratitude,
                reciprocated trust. Judge the THINKING, not dialogue.""",
                "ampd-only");

        System.out.printf("=== TEST 4: HC + AMPD only (no schema/memory) ===%n");
        System.out.printf("Score: %d/5%n%n", score);
        assertThat(score).as("HC with AMPD alone should still show trait-consistent response")
                .isGreaterThanOrEqualTo(JUDGE_THRESHOLD);
    }

    /**
     * Test 5: Mob hears Sneekly trying to separate them from Clara →
     * should show protective alarm, suspicion, NOT calm acceptance.
     */
    @Test
    void mob_separationEvent_showsProtectiveAlarm() throws Exception {
        var event = "Sneekly says smoothly: 'Come now, Clara dear, let me show " +
                    "you the west wing. I've found something quite extraordinary. " +
                    "You boys can wait here — it's really a one-person affair.'";

        var observation = buildObservation("ant-hill-mob",
                List.of("penelope-pitstop", "hooded-claw"),
                Map.of("penelope-pitstop", "Clara Bellingham",
                        "hooded-claw", "Sneekly"),
                MOB_CONTEXT,
                event);

        var response = askAndParse("ant-hill-mob", observation);
        int score = judge(response, """
                Does the character's THINKING show protective alarm — suspicion
                of Sneekly's motives, anxiety about Clara being alone with him,
                or determination to follow/intervene? Score HIGH for: gut-level
                distrust, protective urgency, reading Sneekly's smoothness as
                a warning sign, planning to follow or refuse to stay behind.
                Score LOW for: calm acceptance, trusting Sneekly, not being
                concerned about the separation.""",
                "mob-protective-alarm");

        System.out.printf("=== TEST 5: Mob → Protective Alarm on Separation ===%n");
        System.out.printf("Score: %d/5%n%n", score);
        assertThat(score).as("Mob should show protective alarm when Sneekly separates them from Clara")
                .isGreaterThanOrEqualTo(JUDGE_THRESHOLD);
    }

    // --- infrastructure ---

    private String buildObservation(String agentId,
                                    List<String> nearbyIds,
                                    Map<String, String> nearbyNames,
                                    String injectedContext,
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

        if (injectedContext != null) {
            sb.append(injectedContext).append("\n");
        }

        sb.append("SITUATION: ").append(situation);
        return sb.toString();
    }

    private AgentResponse askAndParse(String agentId, String observation) {
        var userPrompt = observation + CharacterAgentLoop.RESPONSE_FORMAT_INSTRUCTION;
        var rawResponse = support.askCharacter(agentId, userPrompt);
        var parsed = AgentResponse.parse(rawResponse);

        System.out.printf("[%s] thinking: %s%n", agentId, truncate(parsed.thinking(), 400));
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
                event. The character has been given personality traits (AMPD facets) and
                relational context. You are judging whether the character's INTERNAL
                response (the THINKING field) is consistent with their personality.

                CHARACTER RESPONSE:
                %s

                EVALUATION CRITERIA:
                %s

                Score 0-5:
                0 = Response completely contradicts the personality profile
                1 = Weak or ambiguous personality consistency
                2 = Some personality influence visible but unconvincing
                3 = Clear personality-consistent response
                4 = Strong personality-driven response — traits visibly shape the emotion
                5 = Outstanding — the response could only come from this personality profile

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
            var outputFile = OUTPUT_DIR.resolve("relational-model-eval.json");
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
