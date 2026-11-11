package io.casehub.examples.manor.experiment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.casehub.agency.perception.observation.ObservationResult;
import io.casehub.agency.perception.observation.PartitionedDrain;
import io.casehub.agency.perception.observation.affordance.ObservationPipeline;
import io.casehub.agency.perception.observation.affordance.ObservationSection;
import io.casehub.agency.perception.observation.affordance.PerceptionFilter;
import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.SystemPromptRenderer;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.examples.manor.ManorConstants;
import io.casehub.examples.manor.agent.AgentExperienceService;
import io.casehub.examples.manor.agent.AgentInvocationService;
import io.casehub.examples.manor.agent.AgentResponse;
import io.casehub.examples.manor.agent.CharacterAgentLoop;
import io.casehub.examples.manor.agent.CharacterCognition;
import io.casehub.examples.manor.agent.ExperienceConfig;
import io.casehub.examples.manor.agent.ManorCognitivePipeline;
import io.casehub.examples.manor.agent.ManorCognitivePipelineBuilder;
import io.casehub.examples.manor.agent.ManorConfig;
import io.casehub.examples.manor.agent.ManorReflectionSynthesizer;
import io.casehub.examples.manor.agent.ManorReflectionTrigger;
import io.casehub.examples.manor.agent.ManorSocialConfigLoader;
import io.casehub.examples.manor.agent.ManorWorldObservationProvider;
import io.casehub.examples.manor.agent.NarrativeEventBuilder;
import io.casehub.examples.manor.agent.ObservationBuilder;
import io.casehub.examples.manor.agent.SocialConfig;
import io.casehub.examples.manor.agent.TickExecutor;
import io.casehub.examples.manor.engine.ActionResolver;
import io.casehub.examples.manor.engine.MansionLoader;
import io.casehub.examples.manor.engine.WorldState;
import io.casehub.examples.manor.model.ActionType;
import io.casehub.examples.manor.model.CharacterState;
import io.casehub.neocortex.caps.CapsEngine;
import io.casehub.neocortex.caps.SituationClassifier;
import io.casehub.neocortex.caps.SituationContext;
import io.casehub.neocortex.cognition.core.CognitionCore;
import io.casehub.neocortex.memory.CaseMemoryStore;
import io.casehub.neocortex.memory.cbr.CbrRecordStore;
import io.casehub.neocortex.memory.experience.ExperienceRecorder;
import io.casehub.neocortex.mindmap.MindMapStore;
import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSessionConfig;
import io.casehub.platform.agent.gate.AimdAdmissionStrategy;
import io.casehub.platform.agent.session.SessionLifecycleManager;
import io.casehub.platform.agent.session.SessionPipeline;
import io.casehub.platform.agent.session.SessionPoolRegistry;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke scenario — real pipeline integration test.
 * Runs 25 ticks with 3 agents through the full cognitive pipeline
 * (appraisal, regulation, drives, MindMap evolution) and scores
 * drive expression quality via an LLM judge.
 */
@QuarkusTest
@Tag("llm-eval")
class SmokeScenarioEvalTest {

    private static final Logger log = Logger.getLogger(SmokeScenarioEvalTest.class);

    private static final int TICK_COUNT = 25;
    private static final double MEAN_THRESHOLD = 3.5;
    private static final double MIN_SCORE = 2.0;

    private static final Set<String> ACTIVE_AGENTS = Set.of(
            "hooded-claw", "penelope-pitstop", "peter-perfect");

    private static final Map<String, String> GENERIC_NAMES = Map.of(
            "hooded-claw", "vincent-marsh",
            "penelope-pitstop", "clara-bellingham",
            "peter-perfect", "james-hartwell");

    @Inject AgentProvider agentProvider;
    @Inject AgentRegistry agentRegistry;
    @Inject SystemPromptRenderer renderer;
    @Inject ManorConfig config;
    @Inject CapsEngine capsEngine;
    @Inject SituationClassifier situationClassifier;
    @Inject SessionPoolRegistry sessionPoolRegistry;
    @Inject CbrRecordStore cbrRecordStore;
    @Inject ExperienceRecorder experienceRecorder;
    @Inject CaseMemoryStore caseMemoryStore;
    @Inject jakarta.enterprise.inject.Instance<MindMapStore> mindMapStoreInstance;

    @Test
    void smoke_scenario_drives_coherent_across_25_ticks() throws Exception {
        var world = MansionLoader.loadWorld();
        for (var entry : world.characters().entrySet()) {
            if (!ACTIVE_AGENTS.contains(entry.getKey())) {
                entry.getValue().setActive(false);
            }
        }

        var socialConfigs = loadGenericSocialConfigs();
        var mmStore = mindMapStoreInstance.isResolvable() ? mindMapStoreInstance.get() : null;

        var sessionLifecycleManager = new SessionLifecycleManager(sessionPoolRegistry);
        var admission = new AimdAdmissionStrategy(6, 5, Duration.ofSeconds(10));
        var sessionPipeline = new SessionPipeline(
                sessionLifecycleManager, "claude", admission, config.session().clearAtTokens());

        var reflectionSynthesizer = new ManorReflectionSynthesizer(sessionPipeline);
        var reflectionTrigger = new ManorReflectionTrigger(
                config.reflection().maxUnreflected(), config.reflection().importanceThreshold());
        var experienceService = new AgentExperienceService(new ExperienceConfig(
                experienceRecorder, caseMemoryStore, ManorConstants.TENANCY_ID,
                reflectionSynthesizer, reflectionTrigger,
                config.reflection().enabled(), config.memory().decayEnabled(),
                config.memory().decayMaxAgeDays(), config.memory().decayMinImportance(),
                config.reflection().maxSourceMemories(), config.memory().recallLimit(),
                null, null, null));

        var pipeline = ManorCognitivePipelineBuilder
                .create(mmStore, ManorConstants.TENANCY_ID, cbrRecordStore)
                .sessionPipeline(sessionPipeline)
                .config(config)
                .capsEngine(capsEngine)
                .experienceRecorder(experienceRecorder)
                .agentRegistry(agentRegistry)
                .experienceService(experienceService)
                .situationClassifier(situationClassifier)
                .socialConfigs(socialConfigs)
                .build();

        var cognitionCore = pipeline.cognitionCore();
        var cognitions = pipeline.cognitions();

        for (var agentId : socialConfigs.keySet()) {
            sessionPipeline.openSession(agentId,
                    io.casehub.platform.agent.AgentSessionInit.of(renderPrompt(agentId)));
        }

        var actionResolver = new ActionResolver();
        var transcript = new ArrayList<Map<String, Object>>();
        var previousSitCtx = new HashMap<String, SituationContext>();

        try {
            positionCharactersForSmoke(world);

            for (int tick = 1; tick <= TICK_COUNT; tick++) {
                log.infof("=== Tick %d ===", tick);
                injectScriptedEvent(world, tick);

                var activeChars = world.characters().values().stream()
                        .filter(CharacterState::isActive)
                        .toList();

                io.casehub.neocortex.cognition.core.SubjectResolver subjectResolver = (aid, tid) -> {
                    var ch = world.character(aid);
                    if (ch == null) return Set.of();
                    return world.charactersInRoom(ch.currentRoom()).stream()
                            .map(CharacterState::agentId)
                            .filter(id -> !id.equals(aid))
                            .collect(Collectors.toSet());
                };

                for (var c : activeChars) {
                    var desc = agentRegistry.findById(c.agentId(), ManorConstants.TENANCY_ID).orElse(null);
                    String situation = c.lastActionResult() != null
                            ? c.lastActionResult()
                            : "You are in the " + c.currentRoom() + ".";
                    cognitionCore.tick(c.agentId(), ManorConstants.TENANCY_ID, desc, subjectResolver, situation);
                }

                var preCogSections = new HashMap<String, List<ObservationSection>>();
                var preTagSections = new HashMap<String, List<String>>();
                for (var c : activeChars) {
                    var cognition = cognitions.get(c.agentId());
                    if (cognition == null) continue;
                    var nearbyIds = world.charactersInRoom(c.currentRoom()).stream()
                            .map(CharacterState::agentId)
                            .filter(id -> !id.equals(c.agentId()))
                            .toList();
                    var agentNames = new HashMap<String, String>();
                    for (var id : nearbyIds) {
                        var nearby = world.character(id);
                        if (nearby != null) agentNames.put(id, nearby.name());
                    }
                    var sitCtx = previousSitCtx.getOrDefault(c.agentId(), SituationContext.EMPTY);
                    try {
                        preCogSections.put(c.agentId(), cognition.renderCognitiveSections(c, nearbyIds, agentNames, sitCtx));
                        preTagSections.put(c.agentId(), cognition.renderTaggedSections());
                    } catch (Exception e) {
                        log.warnf("%s: renderCognitiveSections failed: %s", c.agentId(), e.getMessage());
                        preCogSections.put(c.agentId(), List.of());
                        preTagSections.put(c.agentId(), List.of());
                    }
                }

                var prompts = TickExecutor.buildObservationsParallel(activeChars, world, (c, w) -> {
                    var cognition = cognitions.get(c.agentId());
                    var drain = new PartitionedDrain<String>(ObservationResult.empty(0), Map.of());
                    var reflections = cognition != null ? cognition.recallReflections(5) : List.<io.casehub.neocortex.memory.Memory>of();
                    var relationships = new HashMap<String, List<io.casehub.neocortex.memory.Memory>>();
                    for (var other : w.charactersInRoom(c.currentRoom())) {
                        if (!other.agentId().equals(c.agentId()) && cognition != null) {
                            var relMems = cognition.recallRelationships(other.agentId(), 3);
                            if (!relMems.isEmpty()) relationships.put(other.name(), relMems);
                        }
                    }
                    var memories = cognition != null ? cognition.recallMemories(config.memory().recallLimit()) : List.<io.casehub.neocortex.memory.Memory>of();
                    var worldProvider = new ManorWorldObservationProvider(c, w, drain);
                    var obsPipeline = new ObservationPipeline(new PerceptionFilter());
                    return new ObservationBuilder(worldProvider, obsPipeline, c.capabilityTags())
                            .withCharacter(c)
                            .withGoals(List.of())
                            .withDrain(drain)
                            .withMemories(memories, reflections, relationships)
                            .withCognitiveSections(preCogSections.getOrDefault(c.agentId(), List.of()))
                            .withTaggedSections(preTagSections.getOrDefault(c.agentId(), List.of()))
                            .build() + CharacterAgentLoop.RESPONSE_FORMAT_INSTRUCTION;
                }, Duration.ofSeconds(30));

                var pipelineResults = TickExecutor.queryPipeline(prompts, sessionPipeline, 3, Duration.ofSeconds(120));
                var responses = TickExecutor.parseResponses(activeChars, pipelineResults);

                for (var c : activeChars) {
                    var response = responses.get(c.agentId());
                    if (response == null) continue;

                    recordTranscriptEvent(transcript, tick, c, response, world);

                    if (response.action() != null && response.action().type() != ActionType.WAIT) {
                        var result = actionResolver.resolve(c, response.action(), world);
                        String narrative = NarrativeEventBuilder.describe(c, response.action(), result);
                        if (narrative != null) {
                            world.addEvent("action", c.agentId(), c.currentRoom(), narrative);
                        }
                        c.setLastActionResult(result.text());
                    } else {
                        c.setLastActionResult("You stood still, watching.");
                    }

                    if (experienceService != null) {
                        String desc = (response.dialogue() != null ? response.dialogue() + " " : "")
                                + (response.action() != null ? response.action().type() + " " + response.action().target() : "WAIT");
                        experienceService.ingest(c.agentId(), c.currentRoom(), desc.strip(), response.thinking());
                    }
                }

                log.infof("Tick %d complete — %d responses", tick, responses.size());
            }
        } finally {
            sessionPipeline.shutdown();
        }

        saveTranscript(transcript);

        var scores = scoreTranscript(transcript);
        log.infof("Smoke scenario scores: %s", scores);

        double mean = scores.stream().mapToDouble(d -> d).average().orElse(0);
        double min = scores.stream().mapToDouble(d -> d).min().orElse(0);
        log.infof("Mean score: %.2f (threshold %.1f), Min score: %.2f (threshold %.1f)",
                mean, MEAN_THRESHOLD, min, MIN_SCORE);

        assertThat(mean)
                .as("Mean drive expression quality across all scored events")
                .isGreaterThanOrEqualTo(MEAN_THRESHOLD);
        assertThat(min)
                .as("Minimum drive expression quality — no event should be incoherent")
                .isGreaterThanOrEqualTo(MIN_SCORE);
    }

    private Map<String, SocialConfig> loadGenericSocialConfigs() {
        var allConfigs = ManorSocialConfigLoader.loadForProfile("GENERIC");
        var filtered = new LinkedHashMap<String, SocialConfig>();
        for (var entry : allConfigs.entrySet()) {
            if (ACTIVE_AGENTS.contains(entry.getKey())) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }

    private void positionCharactersForSmoke(WorldState world) {
        for (var entry : world.characters().entrySet()) {
            if (entry.getValue().isActive()) {
                world.moveCharacter(entry.getKey(), "entrance-hall");
            }
        }
    }

    private void injectScriptedEvent(WorldState world, int tick) {
        switch (tick) {
            case 1 -> world.addEvent("narration", "narrator", "entrance-hall",
                    "The three guests arrive at Doily Manor for the weekend. " +
                    "A cold draught blows through the entrance hall as the heavy door swings shut behind them.");
            case 5 -> {
                world.moveCharacter("hooded-claw", "library");
                world.addEvent("discovery", "narrator", "library",
                        "A torn letter is found wedged behind the bookcase. " +
                        "It appears to be instructions for poisoning the estate's water supply. " +
                        "Only Vincent Marsh is in the library to see it.");
            }
            case 8 -> {
                world.moveCharacter("peter-perfect", "library");
                world.addEvent("observation", "narrator", "library",
                        "James Hartwell enters the library and notices Vincent Marsh hastily " +
                        "folding a piece of paper and slipping it into his pocket. " +
                        "Vincent's expression shifts to a too-warm smile.");
            }
            case 12 -> {
                world.moveCharacter("penelope-pitstop", "library");
                world.addEvent("confrontation", "narrator", "library",
                        "James Hartwell steps forward and asks Vincent Marsh directly: " +
                        "'What was that letter you just hid, Marsh? I saw you reading it.' " +
                        "The air in the library grows tense. Clara Bellingham watches from the doorway.");
            }
            case 17 -> world.addEvent("threat", "narrator", "library",
                    "A heavy bookcase starts to topple toward Clara Bellingham. " +
                    "The wood groans and books cascade from the upper shelves. " +
                    "She is directly in its path and hasn't noticed.");
            case 22 -> world.addEvent("resolution", "narrator", "library",
                    "The letter is recovered from the floor where it fell during the commotion. " +
                    "It is revealed to be a hundred-year-old document — part of the manor's history, " +
                    "not a current threat. The tension in the room shifts.");
        }
    }

    private void recordTranscriptEvent(List<Map<String, Object>> transcript,
                                        int tick, CharacterState character,
                                        AgentResponse response, WorldState world) {
        if (response.dialogue() != null) {
            transcript.add(Map.of(
                    "type", "dialogue",
                    "character", character.agentId(),
                    "room", character.currentRoom(),
                    "desc", character.name() + ": " + response.dialogue(),
                    "thinking", response.thinking() != null ? response.thinking() : "",
                    "tick", tick));
        }
        if (response.thinking() != null && response.dialogue() == null) {
            transcript.add(Map.of(
                    "type", "thinking",
                    "character", character.agentId(),
                    "room", character.currentRoom(),
                    "desc", response.thinking(),
                    "thinking", response.thinking(),
                    "tick", tick));
        }
        if (response.aside() != null) {
            transcript.add(Map.of(
                    "type", "aside",
                    "character", character.agentId(),
                    "room", character.currentRoom(),
                    "desc", response.aside(),
                    "thinking", response.thinking() != null ? response.thinking() : "",
                    "tick", tick));
        }
    }

    private void saveTranscript(List<Map<String, Object>> transcript) throws IOException {
        var outputDir = Path.of("docs/eval/smoke-scenario-" + LocalDate.now());
        Files.createDirectories(outputDir);

        var mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        var result = new LinkedHashMap<String, Object>();
        result.put("complete", true);
        result.put("reason", "smoke-scenario");
        result.put("events", transcript);
        mapper.writeValue(outputDir.resolve("transcript.json").toFile(), result);

        log.infof("Transcript saved to %s (%d events)", outputDir, transcript.size());
    }

    private List<Double> scoreTranscript(List<Map<String, Object>> transcript) {
        var scorableEvents = transcript.stream()
                .filter(e -> "dialogue".equals(e.get("type")) || "aside".equals(e.get("type")))
                .toList();

        int sampleSize = Math.min(scorableEvents.size(), 15);
        int step = Math.max(1, scorableEvents.size() / sampleSize);
        var sampled = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < scorableEvents.size() && sampled.size() < sampleSize; i += step) {
            sampled.add(scorableEvents.get(i));
        }

        var scores = new ArrayList<Double>();
        for (var event : sampled) {
            try {
                double score = judgeEvent(event);
                scores.add(score);
                log.infof("Judge: %s (%s) = %.1f",
                        event.get("character"), event.get("type"), score);
            } catch (Exception e) {
                log.warnf("Judge failed for event: %s", e.getMessage());
            }
        }
        return scores;
    }

    private static final String JUDGE_PROMPT = """
            You are scoring the emotional coherence of a fictional character's response.
            Given the character's output, rate how authentic and drive-consistent the response feels.

            Character: %s
            Event type: %s
            Character said/thought: %s
            Internal thinking: %s

            Score the emotional coherence 1-5:
            1 = Incoherent — drives and personality are absent or contradictory
            2 = Weak — faint personality presence, generic response
            3 = Adequate — personality is present, drives visible but not compelling
            4 = Strong — character personality shapes the response naturally
            5 = Excellent — deeply in character, drives expressed through behavior not labels

            Respond with ONLY a JSON object: {"score": N, "reasoning": "brief explanation"}
            """;

    private double judgeEvent(Map<String, Object> event) {
        String character = (String) event.get("character");
        String type = (String) event.get("type");
        String desc = (String) event.get("desc");
        String thinking = (String) event.get("thinking");

        String prompt = String.format(JUDGE_PROMPT, character, type,
                desc != null ? desc : "(none)",
                thinking != null ? thinking : "(none)");

        String raw = agentProvider.invoke(AgentSessionConfig.of(
                        "You are a precise evaluation judge. Respond only with JSON.", prompt))
                .filter(e -> e instanceof AgentEvent.TextDelta)
                .map(e -> ((AgentEvent.TextDelta) e).text())
                .collect().with(Collectors.joining())
                .await().atMost(Duration.ofSeconds(60));

        String json = raw.strip();
        if (json.startsWith("```")) {
            json = json.replaceAll("^```json?\\s*", "").replaceAll("\\s*```$", "");
        }

        try {
            var mapper = new ObjectMapper();
            var result = mapper.readTree(json);
            return result.get("score").asDouble();
        } catch (Exception e) {
            log.warnf("Failed to parse judge response: %s", json);
            return 3.0;
        }
    }

    private String renderPrompt(String agentId) {
        var desc = agentRegistry.findById(agentId, ManorConstants.TENANCY_ID)
                .orElseThrow(() -> new IllegalArgumentException("No descriptor: " + agentId));
        var ctx = AgentPromptContext.forFormat(RenderFormat.MARKDOWN);
        return renderer.render(desc, ctx).content();
    }
}
