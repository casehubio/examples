package io.casehub.examples.manor;

import io.casehub.examples.manor.model.ProfileMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileAwareDescriptorRegistrarTest {

    @ParameterizedTest
    @EnumSource(ProfileMode.class)
    void each_profile_mode_loads_five_descriptors(ProfileMode mode) {
        var registrar = new ProfileAwareDescriptorRegistrar(mode);
        var descriptors = registrar.descriptors();
        assertThat(descriptors).hasSize(5);
        assertThat(descriptors).extracting("agentId")
                .containsExactlyInAnyOrder(
                        "penelope-pitstop", "hooded-claw", "ant-hill-mob",
                        "dick-dastardly", "peter-perfect");
    }

    @Test
    void baseline_has_flat_dispositions() {
        var registrar = new ProfileAwareDescriptorRegistrar(ProfileMode.BASELINE);
        var descriptors = registrar.descriptors();
        var hooded = descriptors.stream()
                .filter(d -> d.agentId().equals("hooded-claw"))
                .findFirst().orElseThrow();
        assertThat(hooded.disposition().dispositionProfile()).isEmpty();
        assertThat(hooded.dispositionVocabulary()).isNull();
        assertThat(hooded.disposition().primaryTerm(io.casehub.eidos.api.DispositionAxis.SOCIAL_ORIENTATION))
                .isEqualTo("competitive");
    }

    @Test
    void belbin_has_slot_vocabulary() {
        var registrar = new ProfileAwareDescriptorRegistrar(ProfileMode.BELBIN);
        var descriptors = registrar.descriptors();
        var hooded = descriptors.stream()
                .filter(d -> d.agentId().equals("hooded-claw"))
                .findFirst().orElseThrow();
        assertThat(hooded.slotVocabulary()).isEqualTo("urn:casehub:vocab:belbin");
        assertThat(hooded.slot()).isEqualTo("shaper");
        assertThat(hooded.disposition().primaryTerm(io.casehub.eidos.api.DispositionAxis.SOCIAL_ORIENTATION))
                .isEqualTo("competitive");
    }
}
