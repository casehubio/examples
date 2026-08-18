package io.casehub.examples.manor.agent;

import io.casehub.eidos.api.BehavioralSignal;
import io.casehub.eidos.api.BehavioralSignalStore;
import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.memory.InMemoryDispositionSignalStore;
import io.casehub.examples.manor.model.ActionResult;
import io.casehub.examples.manor.model.ActionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ManorDispositionRecorderTest {

    private static final String TENANT = "test-tenant";
    private static final String AGENT  = "dick-dastardly";

    private CapturingBehavioralStore       behavioral;
    private InMemoryDispositionSignalStore disposition;
    private ManorDispositionRecorder       recorder;

    @BeforeEach
    void setUp() {
        behavioral  = new CapturingBehavioralStore();
        disposition = new InMemoryDispositionSignalStore();
        recorder    = new ManorDispositionRecorder(behavioral, disposition, TENANT);
    }

    @Test
    void stealRecordsRiskAppetiteAndBehavioralSuccess() {
        recorder.record(AGENT, ActionType.STEAL, new ActionResult.Success("Stole the diamond"));

        assertThat(disposition.activationCounts(AGENT, TENANT))
                .containsKey(DispositionAxis.RISK_APPETITE.name());
        assertThat(behavioral.lastSignal).isEqualTo(BehavioralSignal.SUCCESS);
        assertThat(behavioral.lastQualifier).isEqualTo("STEAL");
    }

    @Test
    void giveRecordsSocialOrientation() {
        recorder.record(AGENT, ActionType.GIVE, new ActionResult.Success("Gave the key"));

        assertThat(disposition.activationCounts(AGENT, TENANT))
                .containsKey(DispositionAxis.SOCIAL_ORIENTATION.name());
    }

    @Test
    void pullAsideRecordsAutonomy() {
        recorder.record(AGENT, ActionType.PULL_ASIDE, new ActionResult.Success("Pulled aside Penelope"));

        assertThat(disposition.activationCounts(AGENT, TENANT))
                .containsKey(DispositionAxis.AUTONOMY.name());
    }

    @Test
    void failedActionRecordsBehavioralDeclineButNoDisposition() {
        recorder.record(AGENT, ActionType.STEAL, new ActionResult.Failed("Caught red-handed"));

        assertThat(behavioral.lastSignal).isEqualTo(BehavioralSignal.DECLINE);
        assertThat(behavioral.lastQualifier).isEqualTo("STEAL");
        assertThat(disposition.activationCounts(AGENT, TENANT)).isEmpty();
    }

    @Test
    void moveIsSkipped() {
        recorder.record(AGENT, ActionType.MOVE, new ActionResult.Success("Moved to library"));

        assertThat(disposition.activationCounts(AGENT, TENANT)).isEmpty();
        assertThat(behavioral.callCount).isZero();
    }

    @Test
    void lookIsSkipped() {
        recorder.record(AGENT, ActionType.LOOK, new ActionResult.Success("Looked around"));

        assertThat(disposition.activationCounts(AGENT, TENANT)).isEmpty();
        assertThat(behavioral.callCount).isZero();
    }

    @Test
    void waitIsSkipped() {
        recorder.record(AGENT, ActionType.WAIT, new ActionResult.Success("Waited"));

        assertThat(disposition.activationCounts(AGENT, TENANT)).isEmpty();
        assertThat(behavioral.callCount).isZero();
    }

    @Test
    void interactRecordsSocialOrientation() {
        recorder.record(AGENT, ActionType.INTERACT, new ActionResult.Success("Examined the mantelpiece"));

        assertThat(disposition.activationCounts(AGENT, TENANT))
                .containsKey(DispositionAxis.SOCIAL_ORIENTATION.name());
    }

    private static class CapturingBehavioralStore implements BehavioralSignalStore {
        BehavioralSignal lastSignal;
        String           lastQualifier;
        int              callCount;

        @Override
        public void record(String agentId, String tenancyId, String capabilityName,
                           String qualifier, BehavioralSignal signal) {
            this.lastSignal    = signal;
            this.lastQualifier = qualifier;
            this.callCount++;
        }

        @Override
        public void clear(String agentId, String tenancyId, String capabilityName,
                          BehavioralSignal signal) {}

        @Override
        public Map<String, Integer> learned(String agentId, String tenancyId,
                                            String capabilityName, BehavioralSignal signal) {
            return Map.of();
        }

        @Override
        public int count(String agentId, String tenancyId, String capabilityName,
                         String qualifier, BehavioralSignal signal) {
            return 0;
        }
    }
}
