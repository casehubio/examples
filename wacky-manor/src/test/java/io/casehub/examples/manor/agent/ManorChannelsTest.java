package io.casehub.examples.manor.agent;

import io.casehub.qhorus.api.channel.ChannelSemantic;
import io.casehub.qhorus.api.message.MessageDispatcher;
import io.casehub.qhorus.api.message.MessageType;
import io.casehub.qhorus.runtime.channel.ChannelService;
import io.casehub.qhorus.runtime.channel.SpaceService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class ManorChannelsTest {

    @Inject ManorChannels manorChannels;
    @Inject ChannelService channelService;
    @Inject SpaceService spaceService;
    @Inject MessageDispatcher messageDispatcher;
    @Inject TestMessageObserver observer;

    @BeforeEach
    void setUp() {
        observer.clear();
        manorChannels.initChannels();
    }

    // -- Channel creation: correct topology --

    @Test
    void creates_doily_manor_space() {
        var space = spaceService.findByName("doily-manor");
        assertThat(space).isPresent();
        assertThat(space.get().name()).isEqualTo("doily-manor");
    }

    @Test
    void creates_four_distinct_channels() {
        assertThat(manorChannels.workChannelId()).isNotNull();
        assertThat(manorChannels.observeChannelId()).isNotNull();
        assertThat(manorChannels.audienceChannelId()).isNotNull();
        assertThat(manorChannels.oversightChannelId()).isNotNull();

        var ids = java.util.Set.of(
            manorChannels.workChannelId(),
            manorChannels.observeChannelId(),
            manorChannels.audienceChannelId(),
            manorChannels.oversightChannelId());
        assertThat(ids).hasSize(4);
    }

    @Test
    void work_channel_has_append_semantic_and_open_types() {
        var ch = channelService.findById(manorChannels.workChannelId()).orElseThrow();
        assertThat(ch.semantic()).isEqualTo(ChannelSemantic.APPEND);
        assertThat(ch.allowedTypes()).isNullOrEmpty();
        assertThat(ch.deniedTypes()).isNullOrEmpty();
    }

    @Test
    void observe_channel_restricts_to_event_only() {
        var ch = channelService.findById(manorChannels.observeChannelId()).orElseThrow();
        assertThat(ch.semantic()).isEqualTo(ChannelSemantic.APPEND);
        assertThat(ch.allowedTypes()).containsExactly(MessageType.EVENT);
    }

    @Test
    void audience_channel_restricts_to_status_only() {
        var ch = channelService.findById(manorChannels.audienceChannelId()).orElseThrow();
        assertThat(ch.semantic()).isEqualTo(ChannelSemantic.APPEND);
        assertThat(ch.allowedTypes()).containsExactly(MessageType.STATUS);
    }

    @Test
    void oversight_channel_denies_event() {
        var ch = channelService.findById(manorChannels.oversightChannelId()).orElseThrow();
        assertThat(ch.semantic()).isEqualTo(ChannelSemantic.APPEND);
        assertThat(ch.deniedTypes()).containsExactly(MessageType.EVENT);
    }

    @Test
    void all_channels_share_the_same_space() {
        var work = channelService.findById(manorChannels.workChannelId()).orElseThrow();
        var observe = channelService.findById(manorChannels.observeChannelId()).orElseThrow();
        var audience = channelService.findById(manorChannels.audienceChannelId()).orElseThrow();
        var oversight = channelService.findById(manorChannels.oversightChannelId()).orElseThrow();

        assertThat(work.spaceId()).isNotNull();
        assertThat(observe.spaceId()).isEqualTo(work.spaceId());
        assertThat(audience.spaceId()).isEqualTo(work.spaceId());
        assertThat(oversight.spaceId()).isEqualTo(work.spaceId());
    }

    @Test
    void generates_scenario_correlation_id() {
        assertThat(manorChannels.scenarioCorrelationId()).isNotNull();
        assertThat(manorChannels.scenarioCorrelationId()).isNotBlank();
    }

    // -- Dispatch correctness: right speech acts, right channels, right topics --

    @Test
    void dialogue_dispatched_as_status_to_work_channel_with_room_topic() {
        manorChannels.dispatchDialogue("penelope", "entrance-hall",
            "Why, how delightful!");

        var msgs = observer.messages().stream()
            .filter(m -> "penelope".equals(m.senderId()))
            .toList();
        assertThat(msgs).hasSize(1);
        var msg = msgs.get(0);
        assertThat(msg.channelId()).isEqualTo(manorChannels.workChannelId());
        assertThat(msg.messageType()).isEqualTo(MessageType.STATUS);
        assertThat(msg.content()).isEqualTo("Why, how delightful!");
        assertThat(msg.topic()).isEqualTo("entrance-hall");
        assertThat(msg.correlationId()).isEqualTo(manorChannels.scenarioCorrelationId());
    }

    @Test
    void narration_dispatched_as_status_to_audience_channel_with_narrator_topic() {
        manorChannels.dispatchNarration(
            "And so our heroes GATHER in the dusty entrance!");

        var msgs = observer.messages().stream()
            .filter(m -> "narrator".equals(m.senderId()))
            .toList();
        assertThat(msgs).hasSize(1);
        var msg = msgs.get(0);
        assertThat(msg.channelId()).isEqualTo(manorChannels.audienceChannelId());
        assertThat(msg.messageType()).isEqualTo(MessageType.STATUS);
        assertThat(msg.content()).isEqualTo("And so our heroes GATHER in the dusty entrance!");
        assertThat(msg.topic()).isEqualTo("narrator");
    }

    @Test
    void aside_dispatched_as_status_to_audience_channel_with_asides_topic() {
        manorChannels.dispatchAside("hooded-claw",
            "Nyah-ha-ha-HA! My plan is FLAWLESS!");

        var msgs = observer.messages().stream()
            .filter(m -> "hooded-claw".equals(m.senderId()))
            .toList();
        assertThat(msgs).hasSize(1);
        var msg = msgs.get(0);
        assertThat(msg.channelId()).isEqualTo(manorChannels.audienceChannelId());
        assertThat(msg.messageType()).isEqualTo(MessageType.STATUS);
        assertThat(msg.content()).isEqualTo("Nyah-ha-ha-HA! My plan is FLAWLESS!");
        assertThat(msg.topic()).isEqualTo("asides");
    }

    @Test
    void position_event_dispatched_as_event_to_observe_channel_without_content() {
        manorChannels.dispatchPositionEvent("penelope", "kitchen");

        var msgs = observer.messages().stream()
            .filter(m -> m.channelId().equals(manorChannels.observeChannelId()))
            .toList();
        assertThat(msgs).hasSize(1);
        var msg = msgs.get(0);
        assertThat(msg.messageType()).isEqualTo(MessageType.EVENT);
        assertThat(msg.content()).isNull();
        assertThat(msg.topic()).isEqualTo("positions");
    }

    @Test
    void scene_event_dispatched_as_event_to_observe_channel() {
        manorChannels.dispatchSceneEvent("tea-poisoning", "started");

        var msgs = observer.messages().stream()
            .filter(m -> m.channelId().equals(manorChannels.observeChannelId()))
            .toList();
        assertThat(msgs).hasSize(1);
        var msg = msgs.get(0);
        assertThat(msg.messageType()).isEqualTo(MessageType.EVENT);
        assertThat(msg.content()).isNull();
        assertThat(msg.senderId()).isEqualTo("orchestrator");
        assertThat(msg.topic()).isEqualTo("scenes");
    }

    @Test
    void scenario_start_dispatched_as_command_with_correlation_id() {
        manorChannels.dispatchScenarioStart();

        var msgs = observer.messages().stream()
            .filter(m -> m.messageType() == MessageType.COMMAND)
            .toList();
        assertThat(msgs).hasSize(1);
        var msg = msgs.get(0);
        assertThat(msg.channelId()).isEqualTo(manorChannels.workChannelId());
        assertThat(msg.senderId()).isEqualTo("orchestrator");
        assertThat(msg.correlationId()).isEqualTo(manorChannels.scenarioCorrelationId());
        assertThat(msg.content()).isNotNull();
    }

    @Test
    void scenario_complete_dispatched_as_done_with_correlation_id() {
        manorChannels.dispatchScenarioStart();
        observer.clear();

        manorChannels.dispatchScenarioComplete();

        var msgs = observer.messages().stream()
            .filter(m -> m.messageType() == MessageType.DONE)
            .toList();
        assertThat(msgs).hasSize(1);
        var msg = msgs.get(0);
        assertThat(msg.channelId()).isEqualTo(manorChannels.workChannelId());
        assertThat(msg.senderId()).isEqualTo("orchestrator");
        assertThat(msg.correlationId()).isEqualTo(manorChannels.scenarioCorrelationId());
    }

    // -- Channel constraint enforcement: wrong types rejected --

    @Test
    void observe_channel_constraint_declared_as_event_only() {
        var ch = channelService.findById(manorChannels.observeChannelId()).orElseThrow();
        assertThat(ch.allowedTypes()).isNotNull();
        assertThat(ch.allowedTypes()).containsExactly(MessageType.EVENT);
        assertThat(ch.allowedTypes()).doesNotContain(MessageType.STATUS);
    }

    @Test
    void audience_channel_constraint_declared_as_status_only() {
        var ch = channelService.findById(manorChannels.audienceChannelId()).orElseThrow();
        assertThat(ch.allowedTypes()).isNotNull();
        assertThat(ch.allowedTypes()).containsExactly(MessageType.STATUS);
        assertThat(ch.allowedTypes()).doesNotContain(MessageType.COMMAND, MessageType.QUERY);
    }

    @Test
    void oversight_channel_constraint_denies_event() {
        var ch = channelService.findById(manorChannels.oversightChannelId()).orElseThrow();
        assertThat(ch.deniedTypes()).isNotNull();
        assertThat(ch.deniedTypes()).containsExactly(MessageType.EVENT);
    }

    // -- Multiple dispatches don't interfere --

    @Test
    void dialogue_from_different_rooms_routed_to_correct_topics() {
        manorChannels.dispatchDialogue("penelope", "kitchen", "What a lovely stove!");
        manorChannels.dispatchDialogue("hooded-claw", "ballroom", "How delightful...");

        var kitchenMsgs = observer.messages().stream()
            .filter(m -> "kitchen".equals(m.topic()))
            .toList();
        var ballroomMsgs = observer.messages().stream()
            .filter(m -> "ballroom".equals(m.topic()))
            .toList();

        assertThat(kitchenMsgs).hasSize(1);
        assertThat(kitchenMsgs.get(0).senderId()).isEqualTo("penelope");
        assertThat(ballroomMsgs).hasSize(1);
        assertThat(ballroomMsgs.get(0).senderId()).isEqualTo("hooded-claw");
    }
}
