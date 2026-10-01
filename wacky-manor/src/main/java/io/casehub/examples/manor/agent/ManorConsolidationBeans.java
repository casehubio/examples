package io.casehub.examples.manor.agent;

import io.casehub.neocortex.cognition.relationship.RelationshipStageConfigProvider;
import io.casehub.neocortex.cognition.relationship.RelationshipStagePhase;
import io.casehub.neocortex.cognition.drive.DriveAdaptationConfig;
import io.casehub.neocortex.cognition.drive.DriveAdaptationPhase;
import io.casehub.neocortex.cognition.drive.DriveReinforcementEntry;
import io.casehub.neocortex.cognition.drive.ReinforcementDirection;
import io.casehub.neocortex.cognition.drive.RewardAxis;
import io.casehub.neocortex.cognition.need.NeedSatisfactionConfig;
import io.casehub.neocortex.cognition.need.NeedTierMappingProvider;
import io.casehub.neocortex.memory.CaseMemoryStore;
import io.casehub.neocortex.mindmap.MindMapStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ManorConsolidationBeans {

    record DefaultDriveAdaptationConfig(
            double learningRate, double arousalWeight,
            double minIntensity, double maxIntensity,
            int maxPerPass
    ) implements DriveAdaptationConfig {}

    record DefaultNeedSatisfactionDecay(
            double safety, double tasks, double social,
            double selfExpression, double understanding
    ) implements NeedSatisfactionConfig.DecayConfig {}

    record DefaultNeedSatisfactionResting(
            double safety, double tasks, double social,
            double selfExpression, double understanding
    ) implements NeedSatisfactionConfig.RestingConfig {}

    record DefaultNeedSatisfactionConfig(
            double satisfactionIncrement, double dissatisfactionIncrement,
            double initialSatisfaction,
            NeedSatisfactionConfig.DecayConfig decay,
            NeedSatisfactionConfig.RestingConfig resting
    ) implements NeedSatisfactionConfig {}

    @Produces
    @Singleton
    DriveAdaptationPhase driveAdaptationPhase(
            MindMapStore mindMapStore,
            NeedTierMappingProvider tierMappingProvider) {
        var config = new DefaultDriveAdaptationConfig(0.05, 0.3, 0.1, 1.0, 20);
        var needConfig = new DefaultNeedSatisfactionConfig(
                0.05, 0.05, 0.5,
                new DefaultNeedSatisfactionDecay(0.15, 0.10, 0.08, 0.05, 0.03),
                new DefaultNeedSatisfactionResting(0.6, 0.3, 0.4, 0.4, 0.4));
        return new DriveAdaptationPhase(
                mindMapStore,
                buildReinforcementMap(),
                config,
                tierMappingProvider,
                needConfig);
    }

    @Produces
    @Singleton
    RelationshipStagePhase relationshipStagePhase(
            MindMapStore mindMapStore,
            CaseMemoryStore memoryStore,
            RelationshipStageConfigProvider configProvider,
            @ConfigProperty(name = "casehub.consolidation.interval-minutes", defaultValue = "5")
            long intervalMinutes) {
        return new RelationshipStagePhase(
                mindMapStore,
                memoryStore,
                configProvider,
                ManorCognitiveSeeder.PEOPLE_SUBGRAPH,
                intervalMinutes * 60_000L);
    }

    @Produces
    @Singleton
    io.casehub.neocortex.cognition.belief.BeliefRevisionPhase beliefRevisionPhase(
            MindMapStore mindMapStore,
            io.casehub.platform.agent.AgentProvider agentProvider) {
        return new io.casehub.neocortex.cognition.belief.BeliefRevisionPhase(
                mindMapStore,
                agentProvider,
                io.casehub.neocortex.cognition.belief.BeliefRevisionConfig.defaults());
    }

    private static Map<String, Map<String, List<DriveReinforcementEntry>>> buildReinforcementMap() {
        var allConfigs = ManorSocialConfigLoader.load();
        var result     = new HashMap<String, Map<String, List<DriveReinforcementEntry>>>();

        for (var entry : allConfigs.entrySet()) {
            var agentId = entry.getKey();
            var config  = entry.getValue();
            if (config.reinforcement().isEmpty()) {continue;}

            var agentMap = new HashMap<String, List<DriveReinforcementEntry>>();
            for (var reinforcement : config.reinforcement().entrySet()) {
                agentMap.put(reinforcement.getKey(), reinforcement.getValue().stream()
                                                                  .map(m -> new DriveReinforcementEntry(
                                                                          m.drive(),
                                                                          ReinforcementDirection.valueOf(m.direction()),
                                                                          RewardAxis.valueOf(m.rewardAxis())))
                                                                  .toList());
            }
            result.put(agentId, Map.copyOf(agentMap));
        }
        return Map.copyOf(result);
    }
}
