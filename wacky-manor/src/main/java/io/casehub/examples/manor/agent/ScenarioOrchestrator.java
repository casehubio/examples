package io.casehub.examples.manor.agent;

import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.CoherenceLevel;
import io.casehub.eidos.api.SystemPromptRenderer;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.casehub.examples.manor.ManorConstants;
import io.casehub.examples.manor.engine.ActionResolver;
import io.casehub.examples.manor.engine.MansionLoader;
import io.casehub.examples.manor.engine.SceneDirector;
import io.casehub.examples.manor.engine.TriggerEvaluator;
import io.casehub.examples.manor.engine.WorldState;
import io.casehub.examples.manor.model.ActionResult;
import io.casehub.examples.manor.model.PendingAction;
import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSessionConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@ApplicationScoped
public class ScenarioOrchestrator {

    private static final Logger log = Logger.getLogger(ScenarioOrchestrator.class);

    @Inject
    AgentProvider                               agentProvider;
    @Inject
    AgentRegistry                               agentRegistry;
    @Inject
    SystemPromptRenderer                        renderer;
    @Inject
    ManorChannels                               manorChannels;
    @Inject
    io.casehub.examples.manor.web.ManorEventBus webEventBus;

    @org.eclipse.microprofile.config.inject.ConfigProperty(name = "manor.scenario.max-turns", defaultValue = "300")
    int maxTurns;
    @org.eclipse.microprofile.config.inject.ConfigProperty(name = "manor.observation.verbatim-threshold", defaultValue = "10")
    int verbatimThreshold;

    @org.eclipse.microprofile.config.inject.ConfigProperty(name = "manor.observation.grouped-threshold", defaultValue = "15")
    int groupedThreshold;
    @org.eclipse.microprofile.config.inject.ConfigProperty(name = "manor.narrator.enabled", defaultValue = "true")
    boolean narratorEnabled;
    @org.eclipse.microprofile.config.inject.ConfigProperty(name = "manor.narrator.event-threshold", defaultValue = "5")
    int narratorEventThreshold;
    @org.eclipse.microprofile.config.inject.ConfigProperty(name = "manor.narrator.timer-seconds", defaultValue = "15")
    int narratorTimerSeconds;
    @org.eclipse.microprofile.config.inject.ConfigProperty(name = "manor.scenario.active-characters", defaultValue = "")
    java.util.Optional<String> activeCharactersConfig;
    @org.eclipse.microprofile.config.inject.ConfigProperty(name = "manor.agent.max-concurrent", defaultValue = "5")
    int                        maxConcurrentAgents;
    private volatile AgentProvider gatedProvider;


    public Thread startScenario(WorldState world, io.casehub.examples.manor.model.ScenarioMode mode) {
        var triggers         = MansionLoader.loadTriggers();
        var scenes           = MansionLoader.loadScenes();
        var triggerEvaluator = new TriggerEvaluator(triggers);
        var sceneDirector    = new SceneDirector(scenes);
        var actionResolver   = new ActionResolver();

        return Thread.ofVirtual().name("scenario-loop")
                     .start(() -> runScenario(world, triggerEvaluator,
                                              sceneDirector, actionResolver, mode));
    }

    private void runScenario(WorldState world,
                             TriggerEvaluator triggerEvaluator,
                             SceneDirector sceneDirector,
                             ActionResolver actionResolver,
                             io.casehub.examples.manor.model.ScenarioMode mode) {
        manorChannels.initChannels();
        manorChannels.dispatchScenarioStart();
        webEventBus.broadcast(io.casehub.examples.manor.web.ManorWebSocketEvent.scenario("started"));
        webEventBus.broadcast(webEventBus.buildSnapshot(world));

        this.gatedProvider = new GatedAgentProvider(agentProvider, maxConcurrentAgents, java.time.Duration.ofSeconds(120));

        var compactor          = new MechanicalCompactor();
        var summariser         = new ManorLlmSummariser(gatedProvider);
        var obsRenderer        = new ManorObservationRenderer(compactor, verbatimThreshold, groupedThreshold, summariser);
        var observationService = new ObservationService(obsRenderer);
        observationService.init(world);

        NarratorAgent narratorAgent = null;
        if (narratorEnabled && mode == io.casehub.examples.manor.model.ScenarioMode.AUTONOMOUS) {
            narratorAgent = new NarratorAgent(
                    compactor, gatedProvider, manorChannels, webEventBus,
                    narratorEventThreshold, narratorTimerSeconds);
            narratorAgent.start(world);
        }

        var dispatcher = new ManorEventDispatcher(
                world, observationService, narratorAgent,
                manorChannels, webEventBus);

        var activeSet = activeCharactersConfig
                .filter(s -> !s.isBlank())
                .map(s -> java.util.Set.copyOf(java.util.Arrays.asList(s.split(","))))
                .orElse(null);

        for (var entry : world.characters().entrySet()) {
            if (activeSet != null && !activeSet.contains(entry.getKey())) {continue;}
            if (agentRegistry.findById(entry.getKey(), ManorConstants.TENANCY_ID).isEmpty()) {
                throw new IllegalStateException("No Eidos descriptor for character: " + entry.getKey());
            }
        }

        var invocationService = new AgentInvocationService(gatedProvider, 60, 2, 2000);

        if (mode == io.casehub.examples.manor.model.ScenarioMode.AUTONOMOUS) {
            runAutonomousTicks(world, activeSet, actionResolver, dispatcher, invocationService, narratorAgent);
        } else {
            runScripted(world, activeSet, actionResolver, dispatcher, invocationService,
                        triggerEvaluator, sceneDirector, narratorAgent);
        }

        String reason = world.completionReason() != null ? world.completionReason().name().toLowerCase() : null;
        manorChannels.dispatchScenarioComplete();
        webEventBus.broadcast(io.casehub.examples.manor.web.ManorWebSocketEvent.scenario("completed", reason));

        if (narratorAgent != null) {
            narratorAgent.stop();
            try {
                narratorAgent.thread().join(Duration.ofSeconds(120));
                if (narratorAgent.thread().isAlive()) {
                    log.warn("Narrator thread did not terminate");
                    narratorAgent.thread().interrupt();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        log.info("Scenario complete" + (reason != null ? " — " + reason : ""));
    }

    private void runAutonomousTicks(WorldState world, java.util.Set<String> activeSet,
                                     ActionResolver actionResolver, ManorEventDispatcher dispatcher,
                                     AgentInvocationService invocationService, NarratorAgent narratorAgent) {
        var activeAgents = world.characters().values().stream()
                .filter(c -> activeSet == null || activeSet.contains(c.agentId()))
                .toList();

        int tick = 0;
        while (!world.isScenarioComplete()) {
            tick++;

            while (world.isPaused() && !world.isScenarioComplete()) {
                try { Thread.sleep(200); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); return;
                }
            }
            if (world.isScenarioComplete()) break;

            int currentTick = tick;
            var actingThisTick = activeAgents.stream()
                    .filter(io.casehub.examples.manor.model.CharacterState::isActive)
                    .filter(c -> currentTick % cadence(c) == 0)
                    .toList();
            if (actingThisTick.isEmpty()) continue;

            var responses = new java.util.concurrent.ConcurrentHashMap<String, AgentResponse>();
            var latch = new java.util.concurrent.CountDownLatch(actingThisTick.size());
            for (var c : actingThisTick) {
                Thread.ofVirtual().name(c.agentId() + "-tick-" + currentTick).start(() -> {
                    try {
                        var drain = dispatcher.observationService().drain(c.agentId(), System.currentTimeMillis());
                        String observation = ObservationBuilder.buildObservation(
                                c, world, resolveGoals(c.agentId()), drain);
                        String userPrompt = observation + CharacterAgentLoop.RESPONSE_FORMAT_INSTRUCTION;
                        String systemPrompt = renderPrompt(c.agentId());
                        responses.put(c.agentId(), invocationService.invoke(systemPrompt, userPrompt, c.agentId()));
                    } catch (Exception e) {
                        log.errorf(e, "%s: tick %d error", c.agentId(), currentTick);
                        responses.put(c.agentId(), AgentResponse.idle());
                    } finally {
                        latch.countDown();
                    }
                });
            }
            try { latch.await(); } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); return;
            }
            log.infof("Tick %d complete (%d agents)", currentTick, actingThisTick.size());

            for (var c : actingThisTick) {
                var response = responses.get(c.agentId());
                if (response == null) continue;
                if (response.dialogue() != null) {
                    var event = new io.casehub.examples.manor.model.ManorEvent(
                            java.time.Instant.now(), "dialogue", c.agentId(),
                            c.currentRoom(), c.name() + ": " + response.dialogue());
                    dispatcher.publishDialogue(event, response.dialogue());
                }
                if (response.aside() != null) {
                    var event = new io.casehub.examples.manor.model.ManorEvent(
                            java.time.Instant.now(), "aside", c.agentId(),
                            c.currentRoom(), response.aside());
                    dispatcher.publishAside(event, response.aside());
                }
            }

            for (var c : actingThisTick) {
                var response = responses.get(c.agentId());
                if (response == null) continue;
                if (response.action() != null && response.action().type() != io.casehub.examples.manor.model.ActionType.WAIT) {
                    String departureRoom = c.currentRoom();
                    var result = actionResolver.resolve(c, response.action(), world);
                    String narrative = NarrativeEventBuilder.describe(c, response.action(), result);
                    if (narrative != null) {
                        var enrichedEvent = new io.casehub.examples.manor.model.ManorEvent(
                                java.time.Instant.now(), "action", c.agentId(), c.currentRoom(),
                                narrative, response.action().type(), response.action().target(),
                                response.action().withItem(),
                                response.action().type() == io.casehub.examples.manor.model.ActionType.MOVE ? departureRoom : null);
                        dispatcher.publishAction(enrichedEvent, result, c.x());
                    }
                    c.setLastActionResult(result.text());
                } else {
                    c.setLastActionResult("You waited and observed.");
                }
            }

            if (world.hasEffect("tea-service", "rat-poison")) {
                world.setScenarioComplete(io.casehub.examples.manor.model.CompletionReason.POISONED);
            } else if (tick >= maxTurns) {
                world.setScenarioComplete(io.casehub.examples.manor.model.CompletionReason.TURN_LIMIT);
            }

            webEventBus.broadcast(webEventBus.buildSnapshot(world));
            log.infof("Tick %d: %d agents acted", tick, actingThisTick.size());
        }
    }

    private void runScripted(WorldState world, java.util.Set<String> activeSet,
                              ActionResolver actionResolver, ManorEventDispatcher dispatcher,
                              AgentInvocationService invocationService,
                              TriggerEvaluator triggerEvaluator, SceneDirector sceneDirector,
                              NarratorAgent narratorAgent) {
        var actionQueue = new LinkedBlockingQueue<PendingAction>();
        var threads = world.characters().values().stream()
                .filter(c -> activeSet == null || activeSet.contains(c.agentId()))
                .map(c -> {
                    var goals = resolveGoals(c.agentId());
                    return Thread.ofVirtual().name(c.agentId())
                            .uncaughtExceptionHandler((t, e) -> {
                                log.errorf(e, "Character %s crashed", t.getName());
                                world.markCharacterInactive(t.getName());
                            })
                            .start(() -> {
                                String systemPrompt = renderPrompt(c.agentId());
                                new CharacterAgentLoop().run(
                                        c, world, invocationService, null,
                                        systemPrompt, actionQueue, dispatcher, goals);
                            });
                })
                .toList();

        while (!world.isScenarioComplete()) {
            try {
                PendingAction pending = actionQueue.poll(5, TimeUnit.SECONDS);
                if (pending == null) continue;
                if (!pending.character().isActive()) {
                    pending.complete(new ActionResult.Failed("Character is no longer active."));
                    continue;
                }
                String departureRoom = pending.character().currentRoom();
                var result = actionResolver.resolve(pending.character(), pending.action(), world);
                String narrative = NarrativeEventBuilder.describe(pending.character(), pending.action(), result);
                if (narrative != null) {
                    var enrichedEvent = new io.casehub.examples.manor.model.ManorEvent(
                            java.time.Instant.now(), "action", pending.character().agentId(),
                            pending.character().currentRoom(), narrative,
                            pending.action().type(), pending.action().target(), pending.action().withItem(),
                            pending.action().type() == io.casehub.examples.manor.model.ActionType.MOVE ? departureRoom : null);
                    dispatcher.publishAction(enrichedEvent, result, pending.character().x());
                }
                pending.character().setLastActionResult(result.text());

                var triggerResult = triggerEvaluator.evaluate(world);
                for (String narratorText : triggerResult.narratorEvents()) {
                    world.addEvent("narrator", null, null, narratorText);
                    manorChannels.dispatchNarration(narratorText);
                    webEventBus.broadcast(io.casehub.examples.manor.web.ManorWebSocketEvent.narrator(narratorText));
                }
                if (triggerResult.hasSceneStart()) {
                    manorChannels.dispatchSceneEvent(triggerResult.sceneId(), "started");
                    webEventBus.broadcast(io.casehub.examples.manor.web.ManorWebSocketEvent.scene(triggerResult.sceneId(), "started"));
                    sceneDirector.runScene(triggerResult.sceneId(), world, this::callAgentForScene, narration -> {
                        world.addEvent("narrator", null, null, narration);
                        manorChannels.dispatchNarration(narration);
                        webEventBus.broadcast(io.casehub.examples.manor.web.ManorWebSocketEvent.narrator(narration));
                    });
                    manorChannels.dispatchSceneEvent(triggerResult.sceneId(), "ended");
                    webEventBus.broadcast(io.casehub.examples.manor.web.ManorWebSocketEvent.scene(triggerResult.sceneId(), "ended"));
                }
                pending.complete(result);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        for (var t : threads) {
            try {
                t.join(Duration.ofSeconds(5));
                if (t.isAlive()) { log.warnf("Character %s did not terminate", t.getName()); t.interrupt(); }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    private java.util.List<io.casehub.eidos.api.AgentGoal> resolveGoals(String agentId) {
        return agentRegistry.findById(agentId, ManorConstants.TENANCY_ID)
                            .map(desc -> desc.goals())
                            .orElse(java.util.List.of());
    }

    private String callAgentForScene(String characterId, String prompt) {
        String systemPrompt = renderPrompt(characterId);
        try {
            return gatedProvider.invoke(
                                        AgentSessionConfig.of(systemPrompt, prompt))
                                .filter(e -> e instanceof AgentEvent.TextDelta)
                                .map(e -> ((AgentEvent.TextDelta) e).text())
                                .collect().with(Collectors.joining())
                                .await().atMost(Duration.ofSeconds(120));
        } catch (Exception e) {
            log.warnf("Scene LLM call failed for %s: %s", characterId, e.getMessage());
            return "[" + characterId + " is speechless]";
        }
    }

    private String renderPrompt(String agentId) {
        var desc = agentRegistry.findById(agentId, ManorConstants.TENANCY_ID)
                                .orElseThrow(() -> new IllegalArgumentException("No descriptor: " + agentId));
        var ctx      = AgentPromptContext.forFormat(RenderFormat.MARKDOWN);
        var rendered = renderer.render(desc, ctx);

        if (rendered.coherenceReport() != null
            && rendered.coherenceReport().overall() != CoherenceLevel.ALIGNED) {
            for (var v : rendered.coherenceReport().violations()) {
                log.warnf("[%s] %s coherence %s: %s (declared=%s, implied=%s)",
                          agentId, v.level(), v.axis() != null ? v.axis() : "orientation",
                          v.description(), v.declaredValue(), v.impliedValue());
            }
        }

        return rendered.content();
    }

    private static int cadence(io.casehub.examples.manor.model.CharacterState c) {
        return Math.max(1, (int) (c.thinkDelayMs() / 2000));
    }
}

