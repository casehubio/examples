package io.casehub.examples.manor.agent;

import io.casehub.eidos.api.DispositionSignalStore;
import org.jboss.logging.Logger;

public class ManorPersonalityEvolution {

    private static final Logger log = Logger.getLogger(ManorPersonalityEvolution.class);
    private static final double DECAY_FACTOR = 0.5;

    private final DispositionSignalStore signalStore;
    private final String tenantId;
    private final int checkInterval;

    public ManorPersonalityEvolution(DispositionSignalStore signalStore,
                                     String tenantId, int checkInterval) {
        this.signalStore = signalStore;
        this.tenantId = tenantId;
        this.checkInterval = checkInterval;
    }

    public boolean checkAndEvolve(String agentId, int currentTick) {
        if (currentTick % checkInterval != 0) {
            return false;
        }

        try {
            var counts = signalStore.activationCounts(agentId, tenantId);
            if (counts.isEmpty()) {
                return false;
            }

            log.debugf("%s: disposition check at tick %d — %s", agentId, currentTick, counts);

            signalStore.decay(agentId, tenantId, DECAY_FACTOR);

            return true;
        } catch (Exception e) {
            log.warnf("%s: personality evolution check failed (non-fatal): %s", agentId, e.getMessage());
            return false;
        }
    }
}
