package io.casehub.examples.manor.experiment;

import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.DispositionValue;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DominantFunctionTest {

    @Test
    void returns_dominant_from_profile() {
        var disposition = AgentDisposition.builder()
                .dispositionProfile(
                        new DispositionValue("te", 0.40),
                        new DispositionValue("ni", 0.30),
                        new DispositionValue("se", 0.15),
                        new DispositionValue("fi", 0.10))
                .build();
        assertThat(DominantFunction.of(disposition)).isEqualTo(Optional.of("te"));
    }

    @Test
    void returns_empty_for_no_profile() {
        var disposition = AgentDisposition.builder()
                .socialOrient("collaborative")
                .build();
        assertThat(DominantFunction.of(disposition)).isEmpty();
    }

    @Test
    void returns_empty_for_null_disposition() {
        assertThat(DominantFunction.of(null)).isEmpty();
    }
}
