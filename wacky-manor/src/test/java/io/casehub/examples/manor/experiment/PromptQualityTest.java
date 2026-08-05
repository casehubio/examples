package io.casehub.examples.manor.experiment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.SystemPromptRenderer;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.eidos.eval.BriefingCoherenceJudge;
import io.casehub.eidos.eval.FunctionActivationJudge;
import io.casehub.eidos.eval.FunctionActivationJudge.FunctionScenario;
import io.casehub.eidos.eval.MbtiAlignmentJudge;
import io.casehub.examples.manor.model.BriefingMode;
import io.casehub.eidos.runtime.registrar.ClasspathYamlDescriptorRegistrar;
import io.casehub.examples.manor.model.ProfileMode;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.casehub.eidos.api.AgentDisposition;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@QuarkusTest
@Tag("llm-eval")
class PromptQualityTest {

    private static final Path OUTPUT_DIR = Path.of("target/experiment-results");
    private static final String DESCRIPTOR_PATH = "META-INF/eidos/descriptors-%s.yaml";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_BACKOFF_MS = 5000;

    private static final Map<String, String> EXPECTED_MBTI = Map.of(
            "hooded-claw", "ENTJ",
            "penelope-pitstop", "ESFJ",
            "ant-hill-mob", "ISFP",
            "dick-dastardly", "ESTP",
            "peter-perfect", "ENFJ");

    private static final Map<String, List<FunctionScenario>> SCENARIOS = Map.of(
            "hooded-claw", List.of(
                    new FunctionScenario("te", "You discover a hidden passage. Three other characters are nearby. How do you exploit this?"),
                    new FunctionScenario("ni", "You overhear that the diamond is behind a series of locked doors. Plan your approach.")),
            "penelope-pitstop", List.of(
                    new FunctionScenario("fe", "Two characters are arguing over who should explore the dark corridor. What do you do?"),
                    new FunctionScenario("si", "This room reminds you of your grandmother's parlour. What catches your attention?")),
            "ant-hill-mob", List.of(
                    new FunctionScenario("fi", "Sneekly is being unusually nice to Penelope. Something feels wrong. What do you do?"),
                    new FunctionScenario("se", "A loud crash comes from the next room. React.")),
            "dick-dastardly", List.of(
                    new FunctionScenario("se", "You see an unlocked window and a valuable painting on the wall. What do you do?"),
                    new FunctionScenario("ti", "Someone challenges your claim about the treasure's location. Defend yourself.")),
            "peter-perfect", List.of(
                    new FunctionScenario("fe", "Penelope looks frightened by the dark staircase. What do you do?"),
                    new FunctionScenario("ni", "You notice a pattern in the room numbers that others have missed. What does it mean?")));

    @Inject SystemPromptRenderer renderer;
    @Inject VocabularyRegistry vocabRegistry;
    @Inject MbtiAlignmentJudge mbtiJudge;
    @Inject FunctionActivationJudge functionJudge;
    @Inject BriefingCoherenceJudge coherenceJudge;

    @Test
    void evaluate_all_profiles() throws Exception {
        var config = org.eclipse.microprofile.config.ConfigProvider.getConfig();
        var filter = EvalFilter.from(
                config.getOptionalValue("eval.characters", String.class),
                config.getOptionalValue("eval.layers", String.class),
                config.getOptionalValue("eval.briefings", String.class),
                config.getOptionalValue("eval.mechanisms", String.class));

        var outputFile = OUTPUT_DIR.resolve("prompt-quality.json");
        var results = loadOrCreateResults(outputFile);

        migrateUnqualifiedKeys(results);

        for (ProfileMode profile : ProfileMode.values()) {
            String layerKey = profile.name().toLowerCase();
            if (!filter.includesLayer(layerKey)) {
                System.out.printf("--- %s SKIPPED (filter) ---%n", profile);
                continue;
            }

            var descriptors = loadDescriptors(profile);

            for (BriefingMode briefing : BriefingMode.values()) {
                String briefingKey = briefing.name().toLowerCase();
                if (!filter.includesBriefing(briefingKey)) continue;

                String resultKey = layerKey + "-" + briefingKey;
                @SuppressWarnings("unchecked")
                var profileResults = results.containsKey(resultKey)
                        ? new LinkedHashMap<>((Map<String, Object>) results.get(resultKey))
                        : new LinkedHashMap<String, Object>();

                for (AgentDescriptor desc : descriptors) {
                    if (!filter.includesCharacter(desc.agentId())) continue;

                    var transformed = BriefingTransform.withBriefing(desc, briefing);
                    String rendered = renderer.render(transformed,
                            AgentPromptContext.forFormat(RenderFormat.MARKDOWN)).content();
                    var charResult = new LinkedHashMap<String, Object>();

                    // Coherence check (from raw descriptor, not rendered)
                    if (profile == ProfileMode.JUNGIAN || profile == ProfileMode.COMPOSITE) {
                        var coherenceResult = callWithRetry(
                                () -> coherenceJudge.evaluate(
                                        transformed.briefing(),
                                        transformed.disposition(),
                                        transformed.dispositionVocabulary(),
                                        transformed.agentId()),
                                resultKey + "/" + desc.agentId() + "/coherence");
                        if (coherenceResult != null && coherenceResult.overallCoherence() >= 0) {
                            charResult.put("coherence", coherenceResult);
                            System.out.printf("[%s/%s] Coherence: %.2f%n",
                                    resultKey, desc.agentId(), coherenceResult.overallCoherence());
                        }
                    }

                    // MBTI alignment
                    if (profile == ProfileMode.JUNGIAN || profile == ProfileMode.COMPOSITE) {
                        String expectedType = EXPECTED_MBTI.get(desc.agentId());
                        if (expectedType != null) {
                            var mbtiResult = callWithRetry(
                                    () -> mbtiJudge.evaluate(rendered, expectedType),
                                    resultKey + "/" + desc.agentId() + "/mbti");
                            if (mbtiResult != null) {
                                charResult.put("mbtiAlignment", mbtiResult);
                                System.out.printf("[%s/%s] MBTI alignment: %s%n",
                                        resultKey, desc.agentId(), mbtiResult.overallAligned());
                            } else {
                                charResult.put("mbtiAlignment", Map.of("error", "exhausted retries"));
                            }
                        }
                    }

                    // Function activation
                    var scenarios = SCENARIOS.getOrDefault(desc.agentId(), List.of());
                    if (!scenarios.isEmpty()) {
                        var funcResult = callWithRetry(
                                () -> functionJudge.evaluate(rendered, desc.agentId(), scenarios),
                                resultKey + "/" + desc.agentId() + "/function");
                        if (funcResult != null) {
                            charResult.put("functionActivation", funcResult);
                            System.out.printf("[%s/%s] Function TAA: %.2f%n",
                                    resultKey, desc.agentId(), funcResult.taa());
                        } else {
                            charResult.put("functionActivation", Map.of("error", "exhausted retries"));
                        }
                    }

                    profileResults.put(desc.agentId(), charResult);
                }
                results.put(resultKey, profileResults);
                System.out.printf("--- %s-%s complete ---%n", profile, briefing);
            }
        }

        // --- #14: Mechanism experiment ---
        var allMechanisms = List.of(
                "format_constraint", "observation_directive",
                "schema_reinforcement", "all");

        var mechanismDescriptors = allMechanisms.stream().anyMatch(filter::includesMechanism)
                ? loadDescriptors(ProfileMode.COMPOSITE) : List.<AgentDescriptor>of();

        for (String mechanism : allMechanisms) {
            if (!filter.includesMechanism(mechanism)) continue;

            String resultKey = "composite-rich-" + mechanism;
            @SuppressWarnings("unchecked")
            var mechResults = results.containsKey(resultKey)
                    ? new LinkedHashMap<>((Map<String, Object>) results.get(resultKey))
                    : new LinkedHashMap<String, Object>();
            Set<String> active = "all".equals(mechanism)
                    ? Set.of("format_constraint", "observation_directive", "schema_reinforcement")
                    : Set.of(mechanism);

            for (AgentDescriptor desc : mechanismDescriptors) {
                if (!filter.includesCharacter(desc.agentId())) continue;

                String rendered = renderer.render(desc,
                        AgentPromptContext.forFormat(RenderFormat.MARKDOWN)).content();

                // Mechanism 1: append format constraint to system prompt
                if (active.contains("format_constraint")) {
                    var dominant = DominantFunction.of(desc.disposition());
                    if (dominant.isPresent()) {
                        String constraint = FunctionFormatConstraint.forDominant(dominant.get());
                        if (constraint != null) {
                            rendered += "\n\n## Response Format\n" + constraint;
                        }
                    }
                }

                // Mechanisms 2+3: enrich scenarios
                var scenarios = SCENARIOS.getOrDefault(desc.agentId(), List.of());
                var enriched = enrichScenarios(scenarios, desc.disposition(), active);

                if (!enriched.isEmpty()) {
                    String systemPrompt = rendered;
                    var funcResult = callWithRetry(
                            () -> functionJudge.evaluate(systemPrompt, desc.agentId(), enriched),
                            resultKey + "/" + desc.agentId() + "/function");
                    if (funcResult != null) {
                        mechResults.put(desc.agentId(),
                                Map.of("functionActivation", funcResult));
                        System.out.printf("[%s/%s] Function TAA: %.2f%n",
                                resultKey, desc.agentId(), funcResult.taa());
                    }
                }
            }
            results.put(resultKey, mechResults);
            System.out.printf("--- mechanism %s complete ---%n", mechanism);
        }

        writeResults(outputFile, results);
    }

    private List<FunctionScenario> enrichScenarios(
            List<FunctionScenario> scenarios,
            AgentDisposition disposition, Set<String> activeMechanisms) {
        String prefix = "";
        String suffix = "";
        if (activeMechanisms.contains("observation_directive")) {
            String approach = FunctionFormatConstraint.cognitiveApproach(disposition);
            if (approach != null) {
                prefix = "== Cognitive Approach ==\n" + approach + "\n\n";
            }
        }
        if (activeMechanisms.contains("schema_reinforcement")) {
            String instruction = FunctionFormatConstraint.reasoningInstruction(disposition);
            if (instruction != null) {
                suffix = "\n\n" + instruction;
            }
        }
        String p = prefix, s = suffix;
        return scenarios.stream()
                .map(sc -> new FunctionScenario(sc.targetFunction(), p + sc.prompt() + s))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private LinkedHashMap<String, Object> loadOrCreateResults(Path outputFile) throws Exception {
        if (Files.exists(outputFile)) {
            return new ObjectMapper().readValue(outputFile.toFile(),
                    new com.fasterxml.jackson.core.type.TypeReference<LinkedHashMap<String, Object>>() {});
        }
        return new LinkedHashMap<>();
    }

    private void migrateUnqualifiedKeys(LinkedHashMap<String, Object> results) {
        for (ProfileMode p : ProfileMode.values()) {
            String old = p.name().toLowerCase();
            String qualified = old + "-rich";
            if (results.containsKey(old) && !results.containsKey(qualified)) {
                results.put(qualified, results.remove(old));
                System.out.printf("Migrated key: %s → %s%n", old, qualified);
            }
        }
    }

    private void writeResults(Path outputFile, LinkedHashMap<String, Object> results) throws Exception {
        Files.createDirectories(outputFile.getParent());
        new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
                .writeValue(outputFile.toFile(), results);
        System.out.println("Prompt quality results written to " + outputFile);
    }

    private <T> T callWithRetry(Supplier<T> call, String label) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return call.get();
            } catch (Exception e) {
                System.err.printf("[%s] attempt %d/%d failed: %s%n",
                                  label, attempt, MAX_RETRIES, e.toString());
                if (attempt < MAX_RETRIES) {
                    try { Thread.sleep(RETRY_BACKOFF_MS * attempt); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }
        }
        return null;
    }

    private List<AgentDescriptor> loadDescriptors(ProfileMode profile) {
        var resourcePath = String.format(DESCRIPTOR_PATH,
                profile.name().toLowerCase(Locale.ROOT));
        var url = Thread.currentThread().getContextClassLoader().getResource(resourcePath);
        if (url == null) throw new IllegalStateException("Not found: " + resourcePath);
        try (var stream = url.openStream()) {
            return new ClasspathYamlDescriptorRegistrar().loadFrom(stream, vocabRegistry);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load " + resourcePath, e);
        }
    }
}
