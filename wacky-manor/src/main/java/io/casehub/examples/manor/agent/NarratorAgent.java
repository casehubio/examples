package io.casehub.examples.manor.agent;

import io.casehub.agency.summarisation.Compactor;
import io.casehub.agency.summarisation.EventLevel;
import io.casehub.agency.summarisation.EventStreamBus;
import io.casehub.agency.summarisation.LevelEvent;
import io.casehub.agency.summarisation.SummarisationRunner;
import io.casehub.agency.summarisation.WindowPolicy;
import io.casehub.examples.manor.engine.WorldState;
import io.casehub.examples.manor.model.ManorEvent;
import io.casehub.examples.manor.web.ManorEventBus;
import io.casehub.examples.manor.web.ManorWebSocketEvent;
import io.casehub.platform.agent.AgentSession;
import org.jboss.logging.Logger;

public final class NarratorAgent {

    private static final Logger     log            = Logger.getLogger(NarratorAgent.class);
    static final         EventLevel NARRATOR_LEVEL = new EventLevel("narrator", 0);

    private final    SummarisationRunner<ManorEvent, String> runner;
    private final    EventStreamBus<String>                  outputBus;
    private volatile boolean                                 stopped;
    private          Thread                                  narratorThread;

    public NarratorAgent(Compactor<ManorEvent> compactor,
                         io.casehub.platform.agent.AgentSession narratorSession,
                         ManorChannels manorChannels,
                         ManorEventBus webEventBus,
                         int eventThreshold,
                         int timerSeconds) {
        this(compactor, narratorSession, manorChannels, webEventBus, eventThreshold, timerSeconds, null, null);
    }

    public NarratorAgent(Compactor<ManorEvent> compactor,
                         io.casehub.platform.agent.AgentSession narratorSession,
                         ManorChannels manorChannels,
                         ManorEventBus webEventBus,
                         int eventThreshold,
                         int timerSeconds,
                         io.casehub.neocortex.mindmap.MindMapStore mindMapStore,
                         String tenantId) {
        this.outputBus = new EventStreamBus<>();
        if (manorChannels != null || webEventBus != null || mindMapStore != null) {
            String narratorSubgraphId;
            if (mindMapStore != null) {
                var created = mindMapStore.createSubgraph(
                        new io.casehub.neocortex.mindmap.SubgraphInput(
                                "narrator-summaries", io.casehub.neocortex.mindmap.SubgraphTypes.ACTIVITY, null),
                        tenantId);
                narratorSubgraphId = created != null && !created.isBlank() ? created : "narrator-summaries";
            } else {
                narratorSubgraphId = null;
            }
            final String                                    sgId  = narratorSubgraphId;
            final io.casehub.neocortex.mindmap.MindMapStore store = mindMapStore;
            final String                                    tid   = tenantId;
            outputBus.subscribe(s -> true, event -> {
                if (store != null) {
                    try {
                        var now   = java.time.Instant.now();
                        var props = new java.util.HashMap<String, String>();
                        props.put("content", event.payload());
                        var nodeInput = io.casehub.neocortex.mindmap.NodeInput.of(
                                                  "narration-" + now.toEpochMilli(), sgId)
                                                                              .withProperties(props)
                                                                              .withProvenance("narrator-agent")
                                                                              .withValidFrom(now);
                        store.addNode(nodeInput, tid);
                    } catch (Exception e) {
                        log.warn("Narrator memory persistence failed", e);
                    }
                }
                if (manorChannels != null) {
                    try {manorChannels.dispatchNarration(event.payload());} catch (Exception e) {
                        log.warn("Qhorus narration dispatch failed", e);
                    }
                }
                if (webEventBus != null) {
                    try {webEventBus.broadcast(ManorWebSocketEvent.narrator(event.payload()));} catch (Exception e) {
                        log.warn("WebSocket narration dispatch failed", e);
                    }
                }
            });
        }
        var policy     = WindowPolicy.of(timerSeconds * 1000L, eventThreshold);
        var summariser = new NarratorSummariser(narratorSession);
        this.runner = new SummarisationRunner<>(policy, compactor, summariser,
                                                outputBus, NARRATOR_LEVEL,
                                                batch -> log.warnf("Narrator summarisation failed, batch size=%d", batch.size()));
    }


    public void collect(ManorEvent event) {
        runner.collect(new LevelEvent<>(event, event.timestamp().toEpochMilli(), NARRATOR_LEVEL, null));
    }

    public void start(WorldState world) {
        this.stopped        = false;
        this.narratorThread = Thread.ofVirtual().name("narrator-loop")
                                    .start(() -> runLoop(world));
    }

    public void stop() {
        this.stopped = true;
        if (narratorThread != null) {
            narratorThread.interrupt();
        }
    }

    public Thread thread() {
        return narratorThread;
    }

    private void runLoop(WorldState world) {
        while (!stopped && !world.isScenarioComplete()) {
            try {
                runner.tick(System.currentTimeMillis())
                      .toCompletableFuture()
                      .orTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
                      .exceptionally(ex -> {
                          log.warn("Narrator tick timed out");
                          return null;
                      })
                      .join();
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        runner.tick(Long.MAX_VALUE)
              .toCompletableFuture()
              .orTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
              .exceptionally(ex -> {
                  log.warn("Narrator final drain timed out");
                  return null;
              })
              .join();}

    void testSubscribe(java.util.function.Consumer<String> callback) {
        outputBus.subscribe(s -> true, event -> callback.accept(event.payload()));
    }

    void tickNow() {
        runner.tick(System.currentTimeMillis() + 60_000)
              .toCompletableFuture().join();
    }

    void tickAt(long timestamp) {
        runner.tick(timestamp).toCompletableFuture().join();
    }

    void flushNow() {runner.tick(Long.MAX_VALUE).toCompletableFuture().join();}
}
