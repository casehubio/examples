package io.casehub.examples.manor.voice;

import io.casehub.examples.manor.CharacterProfileLoader;
import io.casehub.examples.manor.ManorConstants;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DescriptorLoadTest {

    @Test
    void five_characters_loaded_from_yaml() {
        var profiles = CharacterProfileLoader.load();
        assertThat(profiles).hasSize(5);
        assertThat(profiles.keySet())
                .containsExactlyInAnyOrder(
                        "penelope-pitstop", "hooded-claw", "ant-hill-mob",
                        "dick-dastardly", "peter-perfect");
    }

    @Test
    void all_characters_have_tenancy_id() {
        var profiles = CharacterProfileLoader.load();
        profiles.values().forEach(p ->
                                          assertThat(p.tenancyId())
                                                  .as("tenancyId for %s", p.agentId())
                                                  .isEqualTo(ManorConstants.TENANCY_ID));
    }

    @Test
    void hooded_claw_has_villain_disposition() {
        var profiles = CharacterProfileLoader.load();
        var hc       = profiles.get("hooded-claw");
        assertThat(hc).isNotNull();
        assertThat(hc.disposition().riskAppetite()).isEqualTo("extreme");
        assertThat(hc.disposition().conflictMode()).isEqualTo("competing");
        assertThat(hc.briefing()).containsIgnoringCase("Nyah-ha-ha");
    }

    @Test
    void penelope_has_collaborative_disposition() {
        var profiles = CharacterProfileLoader.load();
        var penelope = profiles.get("penelope-pitstop");
        assertThat(penelope).isNotNull();
        assertThat(penelope.disposition().socialOrient()).isEqualTo("collaborative");
        assertThat(penelope.briefing()).containsIgnoringCase("Southern");
    }

    @Test
    void all_characters_have_briefing_with_soliloquy_instruction() {
        var profiles = CharacterProfileLoader.load();
        profiles.values().forEach(p ->
                                          assertThat(p.briefing())
                                                  .as("briefing for %s should include soliloquy instruction", p.agentId())
                                                  .containsIgnoringCase("narrate your situation aloud"));
    }

    @Test
    void system_prompt_includes_personality_and_briefing() {
        var profiles = CharacterProfileLoader.load();
        var prompt   = profiles.get("hooded-claw").buildSystemPrompt();
        assertThat(prompt)
                .contains("The Hooded Claw")
                .contains("Risk appetite: extreme")
                .contains("Nyah-ha-ha");
    }
}
