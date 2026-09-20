package io.casehub.examples.manor.agent;

import io.casehub.neocortex.mindmap.intelligence.consolidation.ConsolidationPhase;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class ConsolidationPhaseRegistrationTest {

    @Inject
    Instance<ConsolidationPhase> phases;

    @Test
    void allBlocksConsolidationPhasesDiscovered() {
        var phaseNames = phases.stream().map(ConsolidationPhase::name).toList();
        assertThat(phaseNames).contains("drive-adaptation", "belief-revision", "relationship-stage");
    }

    @Test
    void phasesOrderedByPriority() {
        var sorted = phases.stream()
                           .sorted(java.util.Comparator.comparingInt(p ->
                                                                             java.util.Optional.ofNullable(p.getClass().getAnnotation(jakarta.annotation.Priority.class))
                                                                                               .map(jakarta.annotation.Priority::value)
                                                                                               .orElse(Integer.MAX_VALUE)))
                           .map(ConsolidationPhase::name)
                           .toList();
        int beliefIdx = sorted.indexOf("belief-revision");
        int driveIdx  = sorted.indexOf("drive-adaptation");
        int relIdx    = sorted.indexOf("relationship-stage");
        assertThat(beliefIdx).isLessThan(driveIdx);
        assertThat(driveIdx).isLessThan(relIdx);
    }
}
