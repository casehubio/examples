package io.casehub.examples.manor.agent;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.DispositionValue;
import io.casehub.neocortex.cognitive.index.ConflictInterpretation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ManorCognitiveSetupTest {

    @Test
    void gameWorldTypesAreRegistered() {
        var types = ManorCognitiveSetup.gameWorldTypes();
        assertThat(types).containsExactlyInAnyOrder("item", "location", "character");
    }

    @Test
    void descriptorViewBridgesAxes() {
        var descriptor = buildDescriptor("test-agent",
                "cooperative", "moderate", "calculated", "high", "competing");

        var view = ManorCognitiveSetup.toDescriptorView(descriptor);

        assertThat(view.agentId()).isEqualTo("test-agent");
        assertThat(view.disposition()).isNotNull();
        assertThat(view.disposition().socialOrient()).isEqualTo("cooperative");
        assertThat(view.disposition().ruleFollowing()).isEqualTo("moderate");
        assertThat(view.disposition().riskAppetite()).isEqualTo("calculated");
        assertThat(view.disposition().autonomy()).isEqualTo("high");
        assertThat(view.disposition().conflictMode()).isEqualTo("competing");
    }

    @Test
    void descriptorViewBridgesDispositionProfile() {
        var disposition = AgentDisposition.builder()
                .socialOrient("cooperative")
                .dispositionProfile(
                        new DispositionValue("ni", 0.4),
                        new DispositionValue("te", 0.3),
                        new DispositionValue("fi", 0.2),
                        new DispositionValue("se", 0.1))
                .build();
        var descriptor = AgentDescriptor.builder()
                .agentId("profile-agent")
                .name("Profile Agent")
                .slot("test")
                .tenancyId("test")
                .disposition(disposition)
                .build();

        var view = ManorCognitiveSetup.toDescriptorView(descriptor);

        assertThat(view.dispositionProfile()).hasSize(4);
        assertThat(view.dispositionProfile().get(0).term()).isEqualTo("ni");
        assertThat(view.dispositionProfile().get(0).weight()).isCloseTo(0.4, within(0.001));
    }

    @Test
    void descriptorViewBridgesGoals() {
        var descriptor = buildDescriptor("goal-agent",
                "cooperative", "moderate", "calculated", "moderate", "cooperative");

        var view = ManorCognitiveSetup.toDescriptorView(descriptor);

        assertThat(view.goals()).containsExactly("Test goal description");
    }

    @Test
    void deriveDefaultsProducesSocialCognition() {
        var descriptor = buildDescriptor("social-agent",
                "cooperative", "moderate", "calculated", "moderate", "cooperative");

        var defaults = ManorCognitiveSetup.deriveDefaults(descriptor);

        assertThat(defaults.agentId()).isEqualTo("social-agent");
        assertThat(defaults.socialCognition()).isNotNull();
        assertThat(defaults.socialCognition().trustFormationRate()).isCloseTo(0.7, within(0.01));
        assertThat(defaults.socialCognition().conflictInterpretation()).isEqualTo(ConflictInterpretation.REPAIR);
    }

    @Test
    void deriveDefaultsFromCompetitiveSocial() {
        var descriptor = buildDescriptor("competitive-agent",
                "competitive", "strict", "bold", "high", "competing");

        var defaults = ManorCognitiveSetup.deriveDefaults(descriptor);

        assertThat(defaults.socialCognition()).isNotNull();
        assertThat(defaults.socialCognition().trustFormationRate()).isCloseTo(0.4, within(0.01));
    }

    @Test
    void nullDispositionHandledGracefully() {
        var descriptor = AgentDescriptor.builder()
                .agentId("no-disposition")
                .name("No Disposition")
                .slot("test")
                .tenancyId("test")
                .build();

        var view = ManorCognitiveSetup.toDescriptorView(descriptor);

        assertThat(view.agentId()).isEqualTo("no-disposition");
        assertThat(view.disposition()).isNull();
        assertThat(view.dispositionProfile()).isEmpty();
    }

    private static AgentDescriptor buildDescriptor(String agentId,
            String socialOrient, String ruleFollowing, String riskAppetite,
            String autonomy, String conflictMode) {
        var disposition = AgentDisposition.builder()
                .socialOrient(socialOrient)
                .ruleFollowing(ruleFollowing)
                .riskAppetite(riskAppetite)
                .autonomy(autonomy)
                .conflictMode(conflictMode)
                .build();
        return AgentDescriptor.builder()
                .agentId(agentId)
                .name(agentId)
                .slot("test")
                .tenancyId("test")
                .disposition(disposition)
                .goals(List.of(new io.casehub.eidos.api.AgentGoal(
                        "test-goal", "Test goal description",
                        io.casehub.eidos.api.GoalPriority.PRIMARY,
                        io.casehub.eidos.api.Visibility.PUBLIC,
                        List.of(), null)))
                .build();
    }
}
