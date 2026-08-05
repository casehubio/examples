package io.casehub.examples.manor.experiment;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.examples.manor.model.BriefingMode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BriefingTransformTest {

    private final AgentDescriptor base = AgentDescriptor.builder()
            .agentId("hooded-claw")
            .name("The Hooded Claw")
            .slot("shaper")
            .tenancyId("wacky-manor")
            .briefing("You are The Hooded Claw, a villain with elaborate schemes.")
            .build();

    @Test
    void empty_sets_null_briefing() {
        var result = BriefingTransform.withBriefing(base, BriefingMode.EMPTY);
        assertThat(result.briefing()).isNull();
        assertThat(result.agentId()).isEqualTo("hooded-claw");
    }

    @Test
    void name_only_uses_descriptor_name() {
        var result = BriefingTransform.withBriefing(base, BriefingMode.NAME_ONLY);
        assertThat(result.briefing())
                .isEqualTo("You are an agent named The Hooded Claw.");
    }

    @Test
    void name_role_uses_hardcoded_role() {
        var result = BriefingTransform.withBriefing(base, BriefingMode.NAME_ROLE);
        assertThat(result.briefing())
                .isEqualTo("You are The Hooded Claw, a villain and secret nemesis.");
    }

    @Test
    void rich_preserves_original() {
        var result = BriefingTransform.withBriefing(base, BriefingMode.RICH);
        assertThat(result.briefing()).isEqualTo(base.briefing());
    }

    @Test
    void name_role_throws_for_unmapped_agent() {
        var unknown = base.toBuilder().agentId("unknown-agent").build();
        assertThatThrownBy(() ->
                BriefingTransform.withBriefing(unknown, BriefingMode.NAME_ROLE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void all_five_experiment_characters_have_role_phrases() {
        for (String id : java.util.List.of(
                "hooded-claw", "penelope-pitstop", "ant-hill-mob",
                "dick-dastardly", "peter-perfect")) {
            var desc = base.toBuilder().agentId(id).build();
            var result = BriefingTransform.withBriefing(desc, BriefingMode.NAME_ROLE);
            assertThat(result.briefing())
                    .as("Missing role phrase for %s", id)
                    .startsWith("You are ");
        }
    }
}
