package io.casehub.examples.manor.agent;

import io.casehub.blocks.summarisation.observation.affordance.ObservationSection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PersonaActivationSectionTest {

    @Test
    void emitsWhenActivePersonaWhenNearbyAgentsPresent() {
        var mapping = new SocialConfig.PersonaConstraintMapping(
                "never-break-cover", "sneekly", "claw");
        var section = PersonaActivationSection.resolve(mapping, Set.of("penelope-pitstop"));
        assertThat(section).isNotNull();
        assertThat(section.header()).isEqualTo("Active Voice");
        var content = ((ObservationSection.TextBlock) section).content();
        assertThat(content).contains("Sneekly");
        assertThat(content).contains("Maintain this voice");
    }

    @Test
    void emitsWhenInactivePersonaWhenAlone() {
        var mapping = new SocialConfig.PersonaConstraintMapping(
                "never-break-cover", "sneekly", "claw");
        var section = PersonaActivationSection.resolve(mapping, Set.of());
        assertThat(section).isNotNull();
        var content = ((ObservationSection.TextBlock) section).content();
        assertThat(content).contains("Claw");
    }

    @Test
    void returnsNullForNullMapping() {
        var section = PersonaActivationSection.resolve(null, Set.of("someone"));
        assertThat(section).isNull();
    }

    @Test
    void personaConstraintLoadedFromYaml() {
        var configs = ManorSocialConfigLoader.load();
        var hoodedClaw = configs.get("hooded-claw");
        assertThat(hoodedClaw.personaConstraint()).isNotNull();
        assertThat(hoodedClaw.personaConstraint().constraintName()).isEqualTo("never-break-cover");
        assertThat(hoodedClaw.personaConstraint().whenActive()).isEqualTo("sneekly");
        assertThat(hoodedClaw.personaConstraint().whenInactive()).isEqualTo("claw");
    }

    @Test
    void personaConstraintNullForSinglePersonaCharacters() {
        var configs = ManorSocialConfigLoader.load();
        assertThat(configs.get("penelope-pitstop").personaConstraint()).isNull();
        assertThat(configs.get("dick-dastardly").personaConstraint()).isNull();
        assertThat(configs.get("ant-hill-mob").personaConstraint()).isNull();
    }

    @Test
    void personaActivationAppearsInCognitiveSections() {
        var configs = ManorSocialConfigLoader.load();
        var socialConfig = configs.get("hooded-claw");
        var cognition = new CharacterCognition("hooded-claw", null, null, socialConfig, List.of());
        var sections = cognition.renderCognitiveSections(
                new io.casehub.examples.manor.model.CharacterState(
                        "hooded-claw", "HC", "Room", 0.0, List.of()),
                List.of("penelope-pitstop"),
                Map.of("penelope-pitstop", "Penelope Pitstop"));
        assertThat(sections.stream().map(ObservationSection::header).toList())
                .contains("Active Voice");
        var voiceSection = sections.stream()
                .filter(s -> "Active Voice".equals(s.header()))
                .findFirst().orElseThrow();
        assertThat(((ObservationSection.TextBlock) voiceSection).content())
                .contains("Sneekly");
    }

    @Test
    void personaActivationAbsentForSinglePersonaCharacter() {
        var configs = ManorSocialConfigLoader.load();
        var socialConfig = configs.get("penelope-pitstop");
        var cognition = new CharacterCognition("penelope-pitstop", null, null, socialConfig, List.of());
        var sections = cognition.renderCognitiveSections(
                new io.casehub.examples.manor.model.CharacterState(
                        "penelope-pitstop", "Penelope", "Room", 0.0, List.of()),
                List.of("hooded-claw"),
                Map.of("hooded-claw", "The Hooded Claw"));
        assertThat(sections.stream().map(ObservationSection::header).toList())
                .doesNotContain("Active Voice");
    }

    @Test
    void hoodedClawGetsClawWhenAlone() {
        var configs = ManorSocialConfigLoader.load();
        var socialConfig = configs.get("hooded-claw");
        var cognition = new CharacterCognition("hooded-claw", null, null, socialConfig, List.of());
        var sections = cognition.renderCognitiveSections(
                new io.casehub.examples.manor.model.CharacterState(
                        "hooded-claw", "HC", "Room", 0.0, List.of()),
                List.of(),
                Map.of());
        var voiceSection = sections.stream()
                .filter(s -> "Active Voice".equals(s.header()))
                .findFirst().orElseThrow();
        assertThat(((ObservationSection.TextBlock) voiceSection).content())
                .contains("Claw");
    }
}
