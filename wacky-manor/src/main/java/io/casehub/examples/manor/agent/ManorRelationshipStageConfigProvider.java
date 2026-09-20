package io.casehub.examples.manor.agent;

import io.casehub.blocks.agentic.social.RelationshipStageConfig;
import io.casehub.blocks.agentic.social.RelationshipStageConfigProvider;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

@ApplicationScoped
public class ManorRelationshipStageConfigProvider implements RelationshipStageConfigProvider {

    private final Map<String, RelationshipStageConfig> perAgent;
    private final RelationshipStageConfig defaultConfig = RelationshipStageConfig.defaults();

    ManorRelationshipStageConfigProvider() {
        var allConfigs = ManorSocialConfigLoader.load();
        var builder = new java.util.HashMap<String, RelationshipStageConfig>();
        for (var entry : allConfigs.entrySet()) {
            if (entry.getValue().stageConfig() != null) {
                builder.put(entry.getKey(), entry.getValue().stageConfig());
            }
        }
        this.perAgent = Map.copyOf(builder);
    }

    @Override
    public RelationshipStageConfig forAgent(String agentId) {
        return perAgent.getOrDefault(agentId, defaultConfig);
    }
}
