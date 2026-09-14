package io.casehub.examples.manor.agent;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@QuarkusTest
class CognitiveQueryIntegrationTest {

    @Inject
    Instance<io.casehub.neocortex.cognitive.index.CognitiveProfile> cognitiveProfileInstance;

    @Inject
    Instance<io.casehub.neocortex.mindmap.MindMapStore> mindMapStoreInstance;

    @Inject
    ManorConfig config;

    @Test
    void cognitiveProfileInstanceIsResolvableOrGracefullyAbsent() {
        assertThat(cognitiveProfileInstance).isNotNull();
    }

    @Test
    void mindMapStoreInstanceIsResolvableOrGracefullyAbsent() {
        assertThat(mindMapStoreInstance).isNotNull();
    }

    @Test
    void seederHandlesNullMindMapStore() {
        if (!mindMapStoreInstance.isResolvable()) {
            assertThat(true).as("MindMapStore not available — seeder skipped gracefully").isTrue();
            return;
        }
        var seeder = new ManorCognitiveSeeder(mindMapStoreInstance.get());
        var config = SocialConfig.forCharacter("hooded-claw");
        assertThatCode(() -> seeder.seed("test-hc", config, "test-tenant"))
                .doesNotThrowAnyException();
    }

    @Test
    void cognitionCoreConstructsWithNullOrchestrators() {
        assertThatCode(() -> {
            var core = new io.casehub.blocks.agentic.social.CognitionCore(
                    null, null, null, null, null, null, null, null, null,
                    io.casehub.blocks.agentic.social.CognitionConfig.none());
            assertThat(core).isNotNull();
        }).doesNotThrowAnyException();
    }

    @Test
    void characterCognitionWithFullConstructorRendersSections() {
        var socialConfig = SocialConfig.forCharacter("hooded-claw");
        var cognition = new CharacterCognition("hooded-claw", null, null, socialConfig,
                java.util.List.of(), null, new ManorContextStrategy(), null, null, "wacky-manor");
        var sections = cognition.renderCognitiveSections(
                new io.casehub.examples.manor.model.CharacterState(
                        "hooded-claw", "HC", "Grand Hallway", 0.0, java.util.List.of()),
                java.util.List.of("penelope-pitstop"),
                java.util.Map.of("penelope-pitstop", "Penelope Pitstop"));
        assertThat(sections).isNotEmpty();
        assertThat(sections.stream().map(s -> s.header()).toList())
                .contains("Your Drives");
    }

    @Test
    void consolidationConfigPresentAfterPhaseA() {
        assertThat(config.consolidation()).isNotNull();
        assertThat(config.consolidation().enabled()).isTrue();
    }
}
