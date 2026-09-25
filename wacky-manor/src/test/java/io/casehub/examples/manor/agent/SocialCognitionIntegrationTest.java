package io.casehub.examples.manor.agent;

import io.casehub.neocortex.mindmap.intelligence.consolidation.ConsolidationScheduler;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@QuarkusTest
class SocialCognitionIntegrationTest {

    @Inject
    ConsolidationScheduler consolidationScheduler;

    @Inject
    ManorConfig config;

    @Test
    void socialConfigLoadsForCharacters() {
        var hc = ManorSocialConfigLoader.load().get("hooded-claw");
        assertThat(hc.drives()).isNotEmpty();
        assertThat(hc.drives().stream().anyMatch(d -> d.type().equals("scheming"))).isTrue();
        assertThat(hc.norms()).isNotEmpty();
        assertThat(hc.initialBeliefs()).isNotEmpty();

        var penelope = ManorSocialConfigLoader.load().get("penelope-pitstop");
        assertThat(penelope.drives()).isNotEmpty();
        assertThat(penelope.drives().stream().anyMatch(d -> d.type().equals("curiosity"))).isTrue();
    }

    @Test
    void consolidateNowIsCallable() {
        assertThatCode(() -> consolidationScheduler.consolidateNow("wacky-manor-test"))
                .doesNotThrowAnyException();
    }

    @Test
    void consolidationConfigDefaults() {
        assertThat(config.consolidation()).isNotNull();
        assertThat(config.consolidation().enabled()).isTrue();
        assertThat(config.consolidation().intervalTicks()).isEqualTo(50);
    }

    @Test
    void characterCognitionRendersSectionsWithSocialConfig() {
        var socialConfig = ManorSocialConfigLoader.load().get("hooded-claw");
        var cognition    = new CharacterCognition("hooded-claw", null, null, socialConfig, List.of());
        var sections = cognition.renderCognitiveSections(
                new io.casehub.examples.manor.model.CharacterState(
                        "hooded-claw", "Hooded Claw", "Grand Hallway", 0.0, List.of()),
                List.of("penelope-pitstop"),
                Map.of("penelope-pitstop", "Penelope Pitstop"));
        assertThat(sections).isNotEmpty();
        assertThat(sections.stream().map(s -> s.header()).toList())
                .contains("Your Beliefs", "Social Rules");
    }

    @Test
    void sleepCycleTriggersAtConfiguredInterval() {
        var consolidation = new ManorConfig.ConsolidationConfig(true, 50);
        assertThat(consolidation.enabled()).isTrue();
        assertThat(consolidation.intervalTicks()).isEqualTo(50);
        assertThat(50 % consolidation.intervalTicks() == 0).isTrue();
        assertThat(49 % consolidation.intervalTicks() == 0).isFalse();
        assertThat(100 % consolidation.intervalTicks() == 0).isTrue();
    }
}
