package io.casehub.examples.manor.agent;

import io.casehub.neocortex.cognition.drive.DriveAdaptationConfig;
import io.casehub.neocortex.cognition.drive.DriveAdaptationPhase;
import io.casehub.neocortex.cognition.drive.DriveReinforcementEntry;
import io.casehub.neocortex.cognition.drive.NeedSatisfactionConfig;
import io.casehub.neocortex.cognition.drive.NeedTierMappingProvider;
import io.casehub.neocortex.cognition.drive.ReinforcementDirection;
import io.casehub.neocortex.cognition.drive.RewardAxis;
import io.casehub.neocortex.cognition.relationship.RelationshipStageConfigProvider;
import io.casehub.neocortex.cognition.relationship.RelationshipStagePhase;
import io.casehub.neocortex.memory.CaseMemoryStore;
import io.casehub.neocortex.mindmap.MindMapStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ManorConsolidationBeans {

    @Produces
    @Singleton
    DriveAdaptationPhase driveAdaptationPhase(
            MindMapStore mindMapStore,
            NeedTierMappingProvider tierMappingProvider,
            DriveAdaptationConfig config,
            NeedSatisfactionConfig needConfig) {
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
            Instance<RelationshipStageConfigProvider> configProvider,
            Instance<io.casehub.neocortex.cognitive.index.CognitiveDefaultsRegistry> defaultsRegistry,
            @org.eclipse.microprofile.config.inject.ConfigProperty(
                    name = "casehub.cognition.relationship.interval-ms",
                    defaultValue = "300000") long relationshipIntervalMs) {
        return new RelationshipStagePhase(
                mindMapStore,
                memoryStore,
                configProvider,
                defaultsRegistry,
                ManorCognitiveSeeder.PEOPLE_SUBGRAPH,
                relationshipIntervalMs);
    }

    // BeliefRevisionPhase removed — direct agentProvider.invoke() bypasses elastic
    // scheduler, causing consolidation hangs. Re-enable after platform elastic fix.

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
