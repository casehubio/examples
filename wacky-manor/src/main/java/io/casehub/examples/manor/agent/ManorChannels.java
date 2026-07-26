package io.casehub.examples.manor.agent;

import io.casehub.examples.manor.ManorConstants;
import io.casehub.platform.api.identity.ActorType;
import io.casehub.qhorus.api.channel.ChannelCreateRequest;
import io.casehub.qhorus.api.channel.ChannelSemantic;
import io.casehub.qhorus.api.channel.SpaceCreateRequest;
import io.casehub.qhorus.api.message.MessageDispatch;
import io.casehub.qhorus.api.message.MessageDispatcher;
import io.casehub.qhorus.api.message.MessageType;
import io.casehub.qhorus.runtime.channel.ChannelService;
import io.casehub.qhorus.runtime.channel.SpaceService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Set;
import java.util.UUID;

/**
 * Qhorus channel layout for the Doily Manor scenario.
 *
 * Four channels following the normative pattern with a purpose-built
 * audience channel for content-bearing broadcasts (narrator commentary
 * and villain asides). The normative /observe channel is EVENT-only and
 * EVENT cannot carry content — /manor/audience uses STATUS instead.
 *
 * Channel layout:
 *   /manor/work      — character dialogue + coordination (APPEND, open types)
 *   /manor/observe   — telemetry events: positions, scene lifecycle (APPEND, EVENT only)
 *   /manor/audience  — narrator + villain asides (APPEND, STATUS only)
 *   /manor/oversight — human governance gate (APPEND, denies EVENT)
 */
@ApplicationScoped
public class ManorChannels {

    private static final Logger log = Logger.getLogger(ManorChannels.class);

    @Inject SpaceService spaceService;
    @Inject ChannelService channelService;
    @Inject MessageDispatcher messageDispatcher;

    private UUID workChannelId;
    private UUID observeChannelId;
    private UUID audienceChannelId;
    private UUID oversightChannelId;
    private String scenarioCorrelationId;

    public void initChannels() {
        var space = spaceService.create(
            new SpaceCreateRequest("doily-manor", "Doily Manor scenario space", null));

        var workChannel = channelService.create(
            ChannelCreateRequest.builder("/manor/work")
                .description("Character dialogue and coordination")
                .semantic(ChannelSemantic.APPEND)
                .spaceId(space.id())
                .build());
        workChannelId = workChannel.id();

        var observeChannel = channelService.create(
            ChannelCreateRequest.builder("/manor/observe")
                .description("Telemetry — position changes, scene lifecycle")
                .semantic(ChannelSemantic.APPEND)
                .spaceId(space.id())
                .allowedTypes(Set.of(MessageType.EVENT))
                .build());
        observeChannelId = observeChannel.id();

        var audienceChannel = channelService.create(
            ChannelCreateRequest.builder("/manor/audience")
                .description("Narrator commentary and villain asides")
                .semantic(ChannelSemantic.APPEND)
                .spaceId(space.id())
                .allowedTypes(Set.of(MessageType.STATUS))
                .build());
        audienceChannelId = audienceChannel.id();

        var oversightChannel = channelService.create(
            ChannelCreateRequest.builder("/manor/oversight")
                .description("Human governance gate")
                .semantic(ChannelSemantic.APPEND)
                .spaceId(space.id())
                .deniedTypes(Set.of(MessageType.EVENT))
                .build());
        oversightChannelId = oversightChannel.id();

        scenarioCorrelationId = UUID.randomUUID().toString();

        log.infof("Manor channels initialized — work=%s, observe=%s, audience=%s, oversight=%s",
            workChannelId, observeChannelId, audienceChannelId, oversightChannelId);
    }

    public void dispatchScenarioStart() {
        messageDispatcher.dispatch(MessageDispatch.builder()
            .channelId(workChannelId)
            .sender("orchestrator")
            .type(MessageType.COMMAND)
            .content("Scenario started — characters are active")
            .correlationId(scenarioCorrelationId)
            .actorType(ActorType.SYSTEM)
            .tenancyId(ManorConstants.TENANCY_ID)
            .topic("general")
            .build());
    }

    public void dispatchScenarioComplete() {
        messageDispatcher.dispatch(MessageDispatch.builder()
            .channelId(workChannelId)
            .sender("orchestrator")
            .type(MessageType.DONE)
            .correlationId(scenarioCorrelationId)
            .inReplyTo(1L)
            .actorType(ActorType.SYSTEM)
            .tenancyId(ManorConstants.TENANCY_ID)
            .topic("general")
            .build());
    }

    public void dispatchDialogue(String characterId, String roomId, String content) {
        messageDispatcher.dispatch(MessageDispatch.builder()
            .channelId(workChannelId)
            .sender(characterId)
            .type(MessageType.STATUS)
            .content(content)
            .correlationId(scenarioCorrelationId)
            .actorType(ActorType.AGENT)
            .tenancyId(ManorConstants.TENANCY_ID)
            .topic(roomId)
            .build());
    }

    public void dispatchNarration(String content) {
        messageDispatcher.dispatch(MessageDispatch.builder()
            .channelId(audienceChannelId)
            .sender("narrator")
            .type(MessageType.STATUS)
            .content(content)
            .actorType(ActorType.SYSTEM)
            .tenancyId(ManorConstants.TENANCY_ID)
            .topic("narrator")
            .build());
    }

    public void dispatchAside(String characterId, String content) {
        messageDispatcher.dispatch(MessageDispatch.builder()
            .channelId(audienceChannelId)
            .sender(characterId)
            .type(MessageType.STATUS)
            .content(content)
            .actorType(ActorType.AGENT)
            .tenancyId(ManorConstants.TENANCY_ID)
            .topic("asides")
            .build());
    }

    public void dispatchPositionEvent(String characterId, String roomId) {
        messageDispatcher.dispatch(MessageDispatch.builder()
            .channelId(observeChannelId)
            .sender(characterId)
            .type(MessageType.EVENT)
            .telemetry("position:" + roomId)
            .actorType(ActorType.AGENT)
            .tenancyId(ManorConstants.TENANCY_ID)
            .topic("positions")
            .build());
    }

    public void dispatchSceneEvent(String sceneId, String status) {
        messageDispatcher.dispatch(MessageDispatch.builder()
            .channelId(observeChannelId)
            .sender("orchestrator")
            .type(MessageType.EVENT)
            .telemetry("scene:" + sceneId + ":" + status)
            .actorType(ActorType.SYSTEM)
            .tenancyId(ManorConstants.TENANCY_ID)
            .topic("scenes")
            .build());
    }

    public UUID workChannelId() { return workChannelId; }
    public UUID observeChannelId() { return observeChannelId; }
    public UUID audienceChannelId() { return audienceChannelId; }
    public UUID oversightChannelId() { return oversightChannelId; }
    public String scenarioCorrelationId() { return scenarioCorrelationId; }
}
