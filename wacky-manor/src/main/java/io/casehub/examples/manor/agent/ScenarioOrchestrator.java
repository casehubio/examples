package io.casehub.examples.manor.agent;

import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.AgentRegistry;
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

    @Inject AgentProvider agentProvider;
    @Inject AgentRegistry agentRegistry;
    @Inject SystemPromptRenderer renderer;
    @Inject
            ManorChannels        manorChannels;
    @Inject
    io.casehub.examples.manor.web.ManorEventBus webEventBus;


    public Thread startScenario(WorldState world) {
        var triggers = MansionLoader.loadTriggers();
        var scenes = MansionLoader.loadScenes();
        var triggerEvaluator = new TriggerEvaluator(triggers);
        var sceneDirector = new SceneDirector(scenes);
        var actionResolver = new ActionResolver();

        return Thread.ofVirtual().name("scenario-loop")
            .start(() -> runScenario(world, triggerEvaluator,
                sceneDirector, actionResolver));
    }

    private void runScenario(WorldState world,
                              TriggerEvaluator triggerEvaluator,
                              SceneDirector sceneDirector,
                              ActionResolver actionResolver) {
        manorChannels.initChannels();
        manorChannels.dispatchScenarioStart();
        webEventBus.broadcast(io.casehub.examples.manor.web.ManorWebSocketEvent.scenario("started"));
        webEventBus.broadcast(webEventBus.buildSnapshot(world));

        var actionQueue = new LinkedBlockingQueue<PendingAction>();

        var threads = world.characters().values().stream()
                           .map(c -> Thread.ofVirtual().name(c.agentId())
                                           .uncaughtExceptionHandler((t, e) -> {
                                               log.errorf(e, "Character %s crashed", t.getName());
                                               world.markCharacterInactive(t.getName());
                                           })
                                           .start(() -> {
                                               String systemPrompt = renderPrompt(c.agentId());
                                               new CharacterAgentLoop().run(
                                                       c, world, agentProvider, systemPrompt, actionQueue, manorChannels, webEventBus);
                                           }))
                           .toList();

        while (!world.isScenarioComplete()) {
            try {
                PendingAction pending = actionQueue.poll(5, TimeUnit.SECONDS);
                if (pending == null) {continue;}

                ActionResult result = actionResolver.resolve(
                        pending.character(), pending.action(), world);

                world.addEvent("action", pending.character().agentId(),
                               pending.character().currentRoom(),
                               pending.character().name() + " " +
                               pending.action().type().name().toLowerCase() +
                               (pending.action().target() != null ?
                                " " + pending.action().target() : ""));

                if (result instanceof ActionResult.MovedToRoom moved) {
                    manorChannels.dispatchPositionEvent(
                            pending.character().agentId(), moved.roomId());
                    webEventBus.broadcast(io.casehub.examples.manor.web.ManorWebSocketEvent.position(
                            pending.character().agentId(), moved.roomId(), pending.character().x()));
                }

                var triggerResult = triggerEvaluator.evaluate(world);

                for (String narratorText : triggerResult.narratorEvents()) {
                    world.addEvent("narrator", null, null, narratorText);
                    manorChannels.dispatchNarration(narratorText);
                    webEventBus.broadcast(io.casehub.examples.manor.web.ManorWebSocketEvent.narrator(narratorText));
                }

                if (triggerResult.hasSceneStart()) {
                    manorChannels.dispatchSceneEvent(triggerResult.sceneId(), "started");
                    webEventBus.broadcast(io.casehub.examples.manor.web.ManorWebSocketEvent.scene(triggerResult.sceneId(), "started"));
                    sceneDirector.runScene(
                            triggerResult.sceneId(), world,
                            this::callAgentForScene,
                            narration -> {
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

        manorChannels.dispatchScenarioComplete();
        webEventBus.broadcast(io.casehub.examples.manor.web.ManorWebSocketEvent.scenario("completed"));

        for (var t : threads) {
            try {
                t.join(Duration.ofSeconds(5));
                if (t.isAlive()) {
                    log.warnf("Character %s did not terminate", t.getName());
                    t.interrupt();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        log.info("Scenario complete");}

    private String callAgentForScene(String characterId, String prompt) {
        String systemPrompt = renderPrompt(characterId);
        try {
            return agentProvider.invoke(
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
        var ctx = AgentPromptContext.forFormat(RenderFormat.MARKDOWN);
        return renderer.render(desc, ctx).content();
    }
}
