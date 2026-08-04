package io.casehub.examples.manor.web;

import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.examples.manor.ManorConstants;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@QuarkusTest
class ManorResourceProfileTest {

    @Inject AgentRegistry agentRegistry;
    @Inject VocabularyRegistry vocabRegistry;

    @Test
    void profile_returns_dto_for_known_character() {
        var r = new ManorResource();
        r.agentRegistry = agentRegistry;
        r.vocabRegistry = vocabRegistry;
        r.eventBus = new ManorEventBus();

        var resp = r.getCharacterProfile("penelope-pitstop");
        assertThat(resp.getStatus()).isEqualTo(200);

        var dto = (CharacterProfileDTO) resp.getEntity();
        assertThat(dto.agentId()).isEqualTo("penelope-pitstop");
        assertThat(dto.name()).isEqualTo("Penelope Pitstop");
        assertThat(dto.slot()).isNotBlank();
        assertThat(dto.dispositionProfile()).isNotNull();
        assertThat(dto.briefing()).contains("Penelope Pitstop");
    }

    @Test
    void profile_returns_404_for_unknown_character() {
        var r = new ManorResource();
        r.agentRegistry = agentRegistry;
        r.vocabRegistry = vocabRegistry;
        r.eventBus = new ManorEventBus();

        var resp = r.getCharacterProfile("nonexistent");
        assertThat(resp.getStatus()).isEqualTo(404);
    }

    @Test
    void profile_filters_private_goals() {
        var r = new ManorResource();
        r.agentRegistry = agentRegistry;
        r.vocabRegistry = vocabRegistry;
        r.eventBus = new ManorEventBus();

        var resp = r.getCharacterProfile("hooded-claw");
        assertThat(resp.getStatus()).isEqualTo(200);

        var dto = (CharacterProfileDTO) resp.getEntity();
        assertThat(dto.goals().stream().map(CharacterProfileDTO.GoalDTO::name))
            .doesNotContain("eliminate-penelope");
    }
}
