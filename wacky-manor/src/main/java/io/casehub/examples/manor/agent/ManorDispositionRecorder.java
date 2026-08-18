package io.casehub.examples.manor.agent;

import io.casehub.eidos.api.BehavioralSignal;
import io.casehub.eidos.api.BehavioralSignalStore;
import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.DispositionSignalStore;
import io.casehub.examples.manor.model.ActionResult;
import io.casehub.examples.manor.model.ActionType;

import java.util.Map;
import java.util.Set;

public class ManorDispositionRecorder {
    private static final org.jboss.logging.Logger log = org.jboss.logging.Logger.getLogger(ManorDispositionRecorder.class);


    private static final Set<ActionType> SKIP_TYPES = Set.of(
            ActionType.MOVE, ActionType.LOOK, ActionType.WAIT
    );

    private static final Map<ActionType, DispositionAxis> AXIS_MAP = Map.of(
            ActionType.STEAL, DispositionAxis.RISK_APPETITE,
            ActionType.USE, DispositionAxis.RISK_APPETITE,
            ActionType.GIVE, DispositionAxis.SOCIAL_ORIENTATION,
            ActionType.INTERACT, DispositionAxis.SOCIAL_ORIENTATION,
            ActionType.PULL_ASIDE, DispositionAxis.AUTONOMY,
            ActionType.TAKE, DispositionAxis.AUTONOMY
    );

    private final BehavioralSignalStore behavioralStore;
    private final DispositionSignalStore dispositionStore;
    private final String tenantId;

    public ManorDispositionRecorder(BehavioralSignalStore behavioralStore,
                                    DispositionSignalStore dispositionStore,
                                    String tenantId) {
        this.behavioralStore = behavioralStore;
        this.dispositionStore = dispositionStore;
        this.tenantId = tenantId;
    }

    public void record(String agentId, ActionType type, ActionResult result) {
        if (SKIP_TYPES.contains(type)) {return;}

        try {
            BehavioralSignal signal = (result instanceof ActionResult.Success)
                                      ? BehavioralSignal.SUCCESS : BehavioralSignal.DECLINE;

            behavioralStore.record(agentId, tenantId, null, type.name(), signal);

            DispositionAxis axis = AXIS_MAP.get(type);
            if (axis != null && signal == BehavioralSignal.SUCCESS) {
                dispositionStore.recordActivation(agentId, tenantId, axis.name());
            }
        } catch (Exception e) {
            log.warnf("%s: disposition recording failed (non-fatal): %s", agentId, e.getMessage());
        }
    }
}
