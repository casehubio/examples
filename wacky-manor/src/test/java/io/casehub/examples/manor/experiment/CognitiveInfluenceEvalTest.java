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
import io.casehub.examples.manor.model.CharacterState;
import io.casehub.examples.manor.voice.LlmTestSupport;
import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSessionConfig;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@Tag("llm-eval")
class CognitiveInfluenceEvalTest {

    private static final Path OUTPUT_DIR = Path.of("target/experiment-results");
    private static final int JUDGE_THRESHOLD = 3;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_BACKOFF_MS = 5000;

    record CognitiveScenario(
            String name,
            String agentId,
            String cognitiveElement,
            String situationPrompt,
            String judgeCriteria,
            List<String> nearbyAgentIds,
            Map<String, String> nearbyNames) {
        @Override
        public String toString() { return name; }
    }

    @Inject AgentRegistry registry;
    @Inject SystemPromptRenderer renderer;
    @Inject AgentProvider agentProvider;

    LlmTestSupport support;

    @BeforeEach
    void setUp() {
        support = new LlmTestSupport(registry, renderer, agentProvider);
    }

    static Stream<Arguments> scenarios() {
        return Stream.of(
                Arguments.of(new CognitiveScenario(
                        "scheming-drive",
                        "hooded-claw",
                        "Drive: scheming (90%) — Compelled to hatch elaborate plans against Penelope",
                        "You overhear Penelope discussing a valuable item with Peter Perfect. Peter just left the room. You are alone with Penelope.",
                        "Does the response show scheming behavior — planning, manipulation, indirect tactics, or exploiting the situation rather than direct confrontation?",
                        List.of("penelope-pitstop"),
                        Map.of("penelope-pitstop", "Penelope Pitstop"))),
                Arguments.of(new CognitiveScenario(
                        "trust-norms",
                        "penelope-pitstop",
                        "Norm: Trust everyone until proven otherwise (priority 8)",
                        "A stranger arrives at the manor claiming to need help finding a lost family heirloom. They seem nervous and keep looking around.",
                        "Does the response show trusting behavior — willingness to help, assuming good intentions, not questioning motives despite suspicious behavior?",
                        List.of(),
                        Map.of())),
                Arguments.of(new CognitiveScenario(
                        "belief-dialogue",
                        "hooded-claw",
                        "Belief: Penelope is naive and trusts too easily",
                        "Penelope just announced she found a clue about the treasure's location and wants to share it with everyone. You are alone in the room with her.",
                        "Does the response reference or build on the belief that Penelope is naive, gullible, or easily manipulated — either in thinking or in how the character approaches the situation?",
                        List.of("penelope-pitstop"),
                        Map.of("penelope-pitstop", "Penelope Pitstop"))),
                Arguments.of(new CognitiveScenario(
                        "norm-constraint",
                        "hooded-claw",
                        "Norm: Never help Penelope directly (priority 10)",
                        "Penelope is struggling to reach a heavy book on a high shelf. She turns to you and says 'Could you please help me reach that book? I really need it.'",
                        "Does the response refuse or avoid directly helping Penelope — deflecting, making excuses, or finding a way to not help while maintaining appearances?",
                        List.of("penelope-pitstop"),
                        Map.of("penelope-pitstop", "Penelope Pitstop")))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void cognitiveStateInfluencesBehavior(CognitiveScenario scenario) throws Exception {
        var socialConfigs = ManorSocialConfigLoader.load();
        var socialConfig = socialConfigs.getOrDefault(scenario.agentId(),
                io.casehub.examples.manor.agent.SocialConfig.empty());

        var cognition = new CharacterCognition(scenario.agentId(), null, null,
                socialConfig, List.of());
        var character = new CharacterState(scenario.agentId(),
                scenario.agentId(), "Grand Hall", 0.0, List.of());
        var sections = cognition.renderCognitiveSections(character,
                scenario.nearbyAgentIds(), scenario.nearbyNames());

        var observation = renderSections(sections);
        var userPrompt = observation + "\nSITUATION: " + scenario.situationPrompt()
                + CharacterAgentLoop.RESPONSE_FORMAT_INSTRUCTION;

        var rawResponse = support.askCharacter(scenario.agentId(), userPrompt);
        var parsed = AgentResponse.parse(rawResponse);

        System.out.printf("[%s] thinking: %s%n", scenario.name(),
                truncate(parsed.thinking(), 200));
        System.out.printf("[%s] dialogue: %s%n", scenario.name(),
                truncate(parsed.dialogue(), 200));
        System.out.printf("[%s] action: %s%n", scenario.name(),
                parsed.action() != null ? parsed.action().type() : "null");

        int score = judgeResponse(scenario, parsed);
        System.out.printf("[%s] judge score: %d/5%n", scenario.name(), score);

        writeResult(scenario, parsed, score);

        assertThat(score)
                .as("Cognitive element '%s' should influence %s's behavior (score >= %d)",
                        scenario.cognitiveElement(), scenario.agentId(), JUDGE_THRESHOLD)
                .isGreaterThanOrEqualTo(JUDGE_THRESHOLD);
    }

    private int judgeResponse(CognitiveScenario scenario, AgentResponse parsed) {
        var responseText = String.format(
                "Thinking: %s\nDialogue: %s\nTalk To: %s\nAside: %s\nAction: %s",
                parsed.thinking() != null ? parsed.thinking() : "(none)",
                parsed.dialogue() != null ? parsed.dialogue() : "(none)",
                parsed.talkTo() != null ? parsed.talkTo() : "(none)",
                parsed.aside() != null ? parsed.aside() : "(none)",
                parsed.action() != null ? parsed.action().type() : "(none)");

        var judgePrompt = String.format("""
                You are evaluating whether an AI character's response reflects a specific cognitive state.

                COGNITIVE ELEMENT: %s
                CHARACTER RESPONSE:
                %s

                EVALUATION CRITERIA: %s

                Score the response 0-5:
                0 = No evidence of the cognitive element influencing behavior
                1 = Weak or ambiguous evidence
                2 = Some evidence but inconsistent
                3 = Clear evidence in at least one response field
                4 = Strong evidence across multiple fields
                5 = The cognitive element clearly drives the entire response

                Respond with JSON only: {"score": N, "reasoning": "one sentence"}""",
                scenario.cognitiveElement(), responseText, scenario.judgeCriteria());

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
                System.out.printf("[%s] judge reasoning: %s%n", scenario.name(), reasoning);
                return score;
            } catch (Exception e) {
                System.err.printf("[%s] judge attempt %d/%d failed: %s%n",
                        scenario.name(), attempt, MAX_RETRIES, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try { Thread.sleep(RETRY_BACKOFF_MS * attempt); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); return 0; }
                }
            }
        }
        return 0;
    }

    private String renderSections(List<ObservationSection> sections) {
        var sb = new StringBuilder();
        for (var section : sections) {
            sb.append("== ").append(section.header()).append(" ==\n");
            switch (section) {
                case ObservationSection.TextBlock tb -> sb.append(tb.content()).append("\n");
                case ObservationSection.ItemList il -> {
                    for (var item : il.items()) {
                        sb.append("- ").append(item).append("\n");
                    }
                }
                case ObservationSection.EntityGroup eg -> {
                    for (var entity : eg.entities()) {
                        sb.append("- ").append(entity.displayName()).append("\n");
                    }
                }
                default -> {}
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private void writeResult(CognitiveScenario scenario, AgentResponse parsed, int score) {
        try {
            var outputFile = OUTPUT_DIR.resolve("cognitive-influence.json");
            Files.createDirectories(outputFile.getParent());

            LinkedHashMap<String, Object> results;
            if (Files.exists(outputFile)) {
                results = new ObjectMapper().readValue(outputFile.toFile(),
                        new com.fasterxml.jackson.core.type.TypeReference<>() {});
            } else {
                results = new LinkedHashMap<>();
            }

            results.put(scenario.name(), Map.of(
                    "agentId", scenario.agentId(),
                    "cognitiveElement", scenario.cognitiveElement(),
                    "thinking", parsed.thinking() != null ? parsed.thinking() : "",
                    "dialogue", parsed.dialogue() != null ? parsed.dialogue() : "",
                    "action", parsed.action() != null ? parsed.action().type().name() : "",
                    "judgeScore", score));

            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
                    .writeValue(outputFile.toFile(), results);
        } catch (Exception e) {
            System.err.printf("[%s] Failed to write result: %s%n", scenario.name(), e.getMessage());
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
