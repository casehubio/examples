package io.casehub.examples.manor.agent;

import io.casehub.blocks.agentic.social.CognitionConfig;
import io.casehub.blocks.agentic.social.CognitionCore;
import io.casehub.blocks.agentic.social.InnerLifeConfig;
import io.casehub.blocks.agentic.social.InnerLifeOrchestrator;
import io.casehub.blocks.agentic.social.MentalModelConfig;
import io.casehub.blocks.agentic.social.MentalModelOrchestrator;
import io.casehub.blocks.agentic.social.MoodConfig;
import io.casehub.blocks.agentic.social.MoodOrchestrator;
import io.casehub.blocks.agentic.social.StrategyLearningConfig;
import io.casehub.blocks.agentic.social.StrategyLearningOrchestrator;
import io.casehub.blocks.agentic.social.UserModelConfig;
import io.casehub.blocks.agentic.social.UserModelOrchestrator;
import io.casehub.blocks.agentic.social.drive.AffiliationDrive;
import io.casehub.blocks.agentic.social.drive.AutonomyDrive;
import io.casehub.blocks.agentic.social.drive.CompetenceDrive;
import io.casehub.blocks.agentic.social.drive.CuriosityDrive;
import io.casehub.blocks.agentic.social.drive.DriveComposer;
import io.casehub.blocks.agentic.social.drive.DriveConfig;
import io.casehub.blocks.agentic.social.drive.DriveOrchestrator;
import io.casehub.blocks.agentic.social.goal.GoalEscalationConfig;
import io.casehub.blocks.agentic.social.goal.GoalProposalConfig;
import io.casehub.blocks.agentic.social.goal.GoalProposalOrchestrator;
import io.casehub.blocks.agentic.social.narrative.NarrativeOrchestrator;
import io.casehub.blocks.agentic.social.prompt.DrivePromptSection;
import io.casehub.blocks.agentic.social.prompt.GoalPromptSection;
import io.casehub.blocks.agentic.social.prompt.MentalModelPromptSection;
import io.casehub.blocks.agentic.social.prompt.MoodPromptSection;
import io.casehub.blocks.agentic.social.prompt.NarrativePromptSection;
import io.casehub.blocks.agentic.social.prompt.StrategyPromptSection;
import io.casehub.blocks.agentic.social.prompt.UserModelPromptSection;
import io.casehub.blocks.memory.ArousalScorer;
import io.casehub.blocks.memory.CompositeConfidenceScorer;
import io.casehub.blocks.memory.MemoryHygieneOrchestrator;
import io.casehub.blocks.memory.RetentionConfig;
import io.casehub.blocks.memory.SurpriseScorer;
import io.casehub.blocks.memory.WeightedScorer;
import io.casehub.neocortex.memory.cbr.ScopeDecay;
import io.casehub.neocortex.memory.cbr.TemporalDecay;
import io.casehub.neocortex.memory.cbr.inmem.InMemoryCbrCaseMemoryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class CognitiveActivationTest {

    public static CognitionCore buildCore(CognitionConfig config) {
        return buildCore(config, null);
    }

    public static CognitionCore buildCore(CognitionConfig config, io.casehub.platform.agent.AgentProvider agentProvider) {
        var mood = new MoodOrchestrator(MoodConfig.defaults());
        var narrativeOrch = new NarrativeOrchestrator(new InMemoryNarrativeStore());
        var cbrStore = new InMemoryCbrCaseMemoryStore();
        var memoryHygiene = new MemoryHygieneOrchestrator(
                cbrStore,
                new CompositeConfidenceScorer(List.of(
                        new WeightedScorer(new ArousalScorer(), 0.5),
                        new WeightedScorer(new SurpriseScorer(), 0.5))),
                new TemporalDecay.HalfLife(Duration.ofDays(365)),
                new ScopeDecay.Step(1.0), null,
                StrategyLearningConfig.defaults().memoryDomain(),
                List.of(StrategyLearningConfig.defaults().engagementCaseType()),
                RetentionConfig.DEFAULT, 10, 0.7, event -> {});

        io.casehub.neocortex.memory.reflection.ReflectionOrchestrator noOpReflection =
                (agentId, tenantId, since, maxEntries) -> List.of();
        var userModel = new UserModelOrchestrator(
                new InMemoryUserProfileStore(), agentProvider, UserModelConfig.defaults());
        var mentalModel = new MentalModelOrchestrator(
                new InMemoryMentalModelStore(), agentProvider, MentalModelConfig.defaults());
        var strategy = new StrategyLearningOrchestrator(
                new InMemoryStrategyStore(), cbrStore, noOpReflection,
                agentProvider, StrategyLearningConfig.defaults());

        var drives = new DriveOrchestrator(
                new CuriosityDrive(memoryHygiene), new CompetenceDrive(strategy),
                new AffiliationDrive(userModel, 0.3, Duration.ofHours(1)),
                new AutonomyDrive(mentalModel, 0.5),
                mood, new DriveComposer(), DriveConfig.defaults());

        var innerLife = new InnerLifeOrchestrator(
                noOpReflection, agentProvider, List.of(), InnerLifeConfig.defaults(), drives);

        var goals = new GoalProposalOrchestrator(
                drives, List.of(), null, Optional.empty(),
                null, null, null,
                GoalProposalConfig.defaults(), GoalEscalationConfig.defaults(),
                java.time.Clock.systemUTC());

        return new CognitionCore(mood, drives, userModel, mentalModel, strategy,
                narrativeOrch, goals, memoryHygiene, innerLife, agentProvider, config, null, null);
    }

    @Test void tickRunsWithoutErrorOnFullConfig() {
        var core = buildCore(CognitionConfig.all());
        core.tick("test-agent", "test-tenant", null, (a, t) -> Set.of("other"));
    }

    @Test void allSectionsPresentWithFullConfig() {
        var core = buildCore(CognitionConfig.all());
        core.tick("test-agent", "test-tenant", null, (a, t) -> Set.of("other"));
        var sections = core.promptSections();
        assertThat(sections).anyMatch(s -> s instanceof MoodPromptSection);
        assertThat(sections).anyMatch(s -> s instanceof DrivePromptSection);
        assertThat(sections).anyMatch(s -> s instanceof NarrativePromptSection);
        assertThat(sections).anyMatch(s -> s instanceof UserModelPromptSection);
        assertThat(sections).anyMatch(s -> s instanceof MentalModelPromptSection);
        assertThat(sections).anyMatch(s -> s instanceof StrategyPromptSection);
        assertThat(sections).anyMatch(s -> s instanceof GoalPromptSection);
    }

    @Test void sectionAbsentWhenSubsystemDisabled() {
        var config = CognitionConfig.none().with("goals", true);
        var core = buildCore(config);
        core.tick("test-agent", "test-tenant", null, (a, t) -> Set.of());
        var sections = core.promptSections();
        assertThat(sections).anyMatch(s -> s instanceof GoalPromptSection);
        assertThat(sections).noneMatch(s -> s instanceof MoodPromptSection);
        assertThat(sections).noneMatch(s -> s instanceof NarrativePromptSection);
    }

    record ConfigLevel(String label, CognitionConfig config, int minSections) {}

    static Stream<ConfigLevel> configProgression() {
        var base = CognitionConfig.none()
                .with("goals", true).with("characterDrives", true).with("needsPyramid", true);
        return Stream.of(
                new ConfigLevel("baseline", base, 1),
                new ConfigLevel("+mood", base.with("mood", true), 2),
                new ConfigLevel("+narrative", base.with("mood", true).with("narrative", true), 3),
                new ConfigLevel("+models", base.with("mood", true).with("narrative", true)
                        .with("userModel", true).with("mentalModel", true), 5),
                new ConfigLevel("+strategy", base.with("mood", true).with("narrative", true)
                        .with("userModel", true).with("mentalModel", true).with("strategy", true), 6),
                new ConfigLevel("+drives", CognitionConfig.all().without("memoryHygiene", "innerLife"), 7),
                new ConfigLevel("full", CognitionConfig.all(), 7)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("configProgression")
    void configLevelProducesSections(ConfigLevel level) {
        var core = buildCore(level.config());
        core.tick("test-agent", "test-tenant", null, (a, t) -> Set.of("other"));
        var sections = core.promptSections();
        assertThat(sections).hasSizeGreaterThanOrEqualTo(level.minSections());
    }
}
