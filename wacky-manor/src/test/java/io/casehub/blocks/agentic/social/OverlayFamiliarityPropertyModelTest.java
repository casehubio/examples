package io.casehub.blocks.agentic.social;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class OverlayFamiliarityPropertyModelTest {
    @Test
    void constantsAreDefined() {
        assertThat(OverlayFamiliarityPropertyModel.FAMILIARITY_SCORE).isEqualTo("familiarity-score");
        assertThat(OverlayFamiliarityPropertyModel.FAMILIARITY_STAGE).isEqualTo("familiarity-stage");
        assertThat(OverlayFamiliarityPropertyModel.FAMILIARITY_INTERACTION_COUNT).isEqualTo("familiarity-interaction-count");
    }
}
