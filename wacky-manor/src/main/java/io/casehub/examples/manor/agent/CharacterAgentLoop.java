package io.casehub.examples.manor.agent;

import io.casehub.examples.manor.ManorConstants;
import io.casehub.examples.manor.engine.WorldState;
import io.casehub.examples.manor.model.ActionType;
import io.casehub.examples.manor.model.CharacterState;
import io.casehub.examples.manor.model.PendingAction;
import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSessionConfig;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

public final class CharacterAgentLoop {

    private static final Logger log = Logger.getLogger(CharacterAgentLoop.class);

    private static final String RESPONSE_FORMAT_INSTRUCTION = """

        You MUST respond with ONLY a JSON object in this exact format:
        {
          "thinking": "your internal reasoning (not shown to others)",
          "dialogue": "what you say aloud (or null if silent)",
          "aside": "private thoughts for the audience only (or null)",
          "action": {
            "type": "MOVE|INTERACT|TAKE|GIVE|USE|LOOK|WAIT",
            "target": "room-id or object-id or character-id (or null for WAIT)",
            "withItem": "inventory-item-id to use (or null)"
          }
        }
        Respond with ONLY the JSON. No other text.""";

    public void run(CharacterState character, WorldState world,
                    AgentProvider agentProvider, String systemPrompt,
                    BlockingQueue<PendingAction> actionQueue,
                    ManorChannels manorChannels,
                    io.casehub.examples.manor.web.ManorEventBus webEventBus) {
        while (!world.isScenarioComplete() && character.isActive()) {
            try {
                if (character.sceneContext() != null) {
                    character.sceneContext().awaitRelease();
                    if (world.isScenarioComplete()) {break;}
                }

                String observation = ObservationBuilder.buildObservation(character, world, java.util.List.of());
                String userPrompt  = observation + RESPONSE_FORMAT_INSTRUCTION;

                AgentResponse response = callAgentWithRetry(
                        agentProvider, systemPrompt, userPrompt, character);

                if (response.dialogue() != null) {
                    world.addEvent("dialogue", character.agentId(),
                                   character.currentRoom(),
                                   character.name() + ": " + response.dialogue());
                    manorChannels.dispatchDialogue(
                            character.agentId(), character.currentRoom(), response.dialogue());
                    if (webEventBus != null) {
                        webEventBus.broadcast(io.casehub.examples.manor.web.ManorWebSocketEvent.dialogue(
                                character.agentId(), character.currentRoom(), response.dialogue()));
                    }
                }
                if (response.aside() != null) {
                    world.addEvent("aside", character.agentId(),
                                   character.currentRoom(), response.aside());
                    manorChannels.dispatchAside(character.agentId(), response.aside());
                    if (webEventBus != null) {
                        webEventBus.broadcast(io.casehub.examples.manor.web.ManorWebSocketEvent.aside(
                                character.agentId(), response.aside()));
                    }
                }

                if (response.action() != null &&
                    response.action().type() != ActionType.WAIT) {
                    var pending = new PendingAction(character, response.action());
                    actionQueue.put(pending);
                    pending.awaitResult();
                }

                Thread.sleep(thinkDelay(character));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.errorf(e, "%s: loop error", character.agentId());
                break;
            }
        }
    }

    private AgentResponse callAgentWithRetry(
            AgentProvider agentProvider, String systemPrompt,
            String userPrompt, CharacterState character) {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                String text = agentProvider.invoke(
                        AgentSessionConfig.of(systemPrompt, userPrompt,
                            Duration.ofSeconds(60)))
                    .filter(e -> e instanceof AgentEvent.TextDelta)
                    .map(e -> ((AgentEvent.TextDelta) e).text())
                    .collect().with(Collectors.joining())
                    .await().atMost(Duration.ofSeconds(120));
                return AgentResponse.parse(text);
            } catch (Exception e) {
                log.warnf("%s: LLM call failed (attempt %d): %s",
                    character.agentId(), attempt + 1, e.getMessage());
                if (attempt == 0) {
                    try { Thread.sleep(thinkDelay(character)); }
                    catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return AgentResponse.idle();
                    }
                }
            }
        }
        log.warnf("%s: falling back to idle action", character.agentId());
        return AgentResponse.idle();
    }

    private long thinkDelay(CharacterState character) {
        return switch (character.agentId()) {
            case "lazy-luke" -> ManorConstants.THINK_DELAY_LAZY_LUKE_MS;
            case "sergeant-blast" -> ManorConstants.THINK_DELAY_SERGEANT_BLAST_MS;
            default -> ManorConstants.THINK_DELAY_DEFAULT_MS;
        };
    }
}
