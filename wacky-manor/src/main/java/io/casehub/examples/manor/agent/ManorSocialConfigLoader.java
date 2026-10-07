package io.casehub.examples.manor.agent;

import io.casehub.yaml.jackson.YamlMappers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ManorSocialConfigLoader {

    private static final String DEFAULT_RESOURCE = "META-INF/eidos/social-config.yaml";

    private ManorSocialConfigLoader() {}

    public static Map<String, SocialConfig> load() {
        return load(DEFAULT_RESOURCE);
    }

    public static Map<String, SocialConfig> loadForProfile(String profileName) {
        String profileResource = String.format("META-INF/eidos/social-config-%s.yaml", profileName.toLowerCase(java.util.Locale.ROOT));
        if (Thread.currentThread().getContextClassLoader().getResource(profileResource) != null) {
            return load(profileResource);
        }
        return load(DEFAULT_RESOURCE);
    }


    @SuppressWarnings("unchecked")
    public static Map<String, SocialConfig> load(String resourcePath) {
        InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IllegalStateException("Social config not found: " + resourcePath);
        }

        var mapper = YamlMappers.create();

        try (is) {
            Map<String, Map<String, Object>> raw = mapper.readValue(is, Map.class);

            var result = new HashMap<String, SocialConfig>();
            for (var entry : raw.entrySet()) {
                result.put(entry.getKey(), parseCharacterConfig(entry.getValue()));
            }
            return Collections.unmodifiableMap(result);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse social config: " + resourcePath, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static SocialConfig parseCharacterConfig(Map<String, Object> raw) {
        @SuppressWarnings("unchecked")
        var goals = raw.containsKey("goals")
                    ? ((List<Map<String, Object>>) raw.get("goals")).stream()
                                                                    .map(m -> new SocialConfig.GoalConfig(
                                                                            (String) m.get("name"),
                                                                            (String) m.get("description"),
                                                                            (String) m.get("axis"),
                                                                            ((Number) m.get("intensity")).doubleValue(),
                                                                            (String) m.get("formation-reason")))
                                                                    .toList()
                    : List.<SocialConfig.GoalConfig>of();

        var drives = raw.containsKey("drives")
                     ? ((List<Map<String, Object>>) raw.get("drives")).stream()
                                                                      .map(m -> new SocialConfig.Drive(
                                                                              (String) m.get("type"),
                                                                              ((Number) m.get("intensity")).doubleValue(),
                                                                              (String) m.get("description")))
                                                                      .toList()
                     : List.<SocialConfig.Drive>of();

        var norms = raw.containsKey("norms")
                    ? ((List<Map<String, Object>>) raw.get("norms")).stream()
                                                                    .map(m -> new SocialConfig.NormEntry(
                                                                            (String) m.get("rule"),
                                                                            ((Number) m.get("priority")).intValue()))
                                                                    .toList()
                    : List.<SocialConfig.NormEntry>of();

        var beliefs = raw.containsKey("initial-beliefs")
                      ? ((List<Map<String, Object>>) raw.get("initial-beliefs")).stream()
                                                                                .map(m -> new SocialConfig.InitialBelief(
                                                                                        (String) m.get("key"),
                                                                                        (String) m.get("value")))
                                                                                .toList()
                      : List.<SocialConfig.InitialBelief>of();

        var relationships = raw.containsKey("relationships")
                            ? ((List<Map<String, Object>>) raw.get("relationships")).stream()
                                                                                    .map(m -> new SocialConfig.Relationship(
                                                                                            (String) m.get("target"),
                                                                                            ((Number) m.get("pleasure")).doubleValue(),
                                                                                            ((Number) m.get("arousal")).doubleValue(),
                                                                                            ((Number) m.get("dominance")).doubleValue()))
                                                                                    .toList()
                            : List.<SocialConfig.Relationship>of();

        var reinforcement = new java.util.HashMap<String, List<SocialConfig.ReinforcementMapping>>();
        if (raw.containsKey("reinforcement")) {
            var reinforcementMap = (Map<String, List<Map<String, Object>>>) raw.get("reinforcement");
            for (var entry : reinforcementMap.entrySet()) {
                var mappings = entry.getValue().stream()
                                    .map(m -> new SocialConfig.ReinforcementMapping(
                                            (String) m.get("drive"),
                                            (String) m.get("direction"),
                                            (String) m.get("reward-axis")))
                                    .toList();
                reinforcement.put(entry.getKey(), mappings);
            }
        }

        io.casehub.neocortex.cognition.relationship.RelationshipStageConfig stageConfig = null;
        if (raw.containsKey("familiarity-thresholds")) {
            var ftRaw    = (Map<String, Object>) raw.get("familiarity-thresholds");
            var defaults = io.casehub.neocortex.cognition.relationship.RelationshipStageConfig.defaults();
            var tiers = ftRaw.containsKey("tiers")
                        ? ((List<Map<String, Object>>) ftRaw.get("tiers")).stream()
                                                                          .map(t -> new io.casehub.neocortex.cognition.relationship.StageTier(
                                                                                  (String) t.get("name"),
                                                                                  ((Number) t.get("threshold")).doubleValue()))
                                                                          .toList()
                        : defaults.tiers();
            double decayRate = ftRaw.containsKey("decay-rate")
                               ? ((Number) ftRaw.get("decay-rate")).doubleValue()
                               : defaults.decayRate();
            double positiveWeight = ftRaw.containsKey("positive-weight")
                                    ? ((Number) ftRaw.get("positive-weight")).doubleValue()
                                    : defaults.positiveWeight();
            double negativeWeight = ftRaw.containsKey("negative-weight")
                                    ? ((Number) ftRaw.get("negative-weight")).doubleValue()
                                    : defaults.negativeWeight();
            stageConfig = new io.casehub.neocortex.cognition.relationship.RelationshipStageConfig(
                    tiers, decayRate, positiveWeight, negativeWeight);
        }

        SocialConfig.PersonaConstraintMapping personaConstraint = null;
        if (raw.containsKey("persona-constraint")) {
            var pcRaw = (Map<String, Object>) raw.get("persona-constraint");
            personaConstraint = new SocialConfig.PersonaConstraintMapping(
                    (String) pcRaw.get("constraint"),
                    (String) pcRaw.get("when-active"),
                    (String) pcRaw.get("when-inactive"));
        }

        @SuppressWarnings("unchecked")
        var tendencies = raw.containsKey("tendencies")
                         ? ((List<String>) raw.get("tendencies"))
                         : List.<String>of();

        var personalityFacets = raw.containsKey("personality-facets")
                ? ((List<Map<String, Object>>) raw.get("personality-facets")).stream()
                        .map(m -> new SocialConfig.PersonalityFacet(
                                (String) m.get("facet"),
                                ((Number) m.get("score")).intValue(),
                                (String) m.get("origin")))
                        .toList()
                : List.<SocialConfig.PersonalityFacet>of();

        var relationalSchemas = raw.containsKey("relational-schemas")
                ? ((List<Map<String, Object>>) raw.get("relational-schemas")).stream()
                        .map(m -> new SocialConfig.RelationalSchema(
                                (String) m.get("target"),
                                (String) m.get("role"),
                                ((Number) m.getOrDefault("trust", 0.5)).doubleValue(),
                                ((Number) m.getOrDefault("intimacy", 0.0)).doubleValue(),
                                ((Number) m.getOrDefault("utility", 0.0)).doubleValue(),
                                (String) m.get("description")))
                        .toList()
                : List.<SocialConfig.RelationalSchema>of();

        SocialConfig.AttachmentStyle attachment = null;
        if (raw.containsKey("attachment")) {
            var atRaw = (Map<String, Object>) raw.get("attachment");
            attachment = new SocialConfig.AttachmentStyle(
                    (String) atRaw.get("style"),
                    ((Number) atRaw.getOrDefault("anxiety", 0.5)).doubleValue(),
                    ((Number) atRaw.getOrDefault("avoidance", 0.5)).doubleValue());
        }

        var formationMemories = raw.containsKey("formation-memories")
                ? ((List<Map<String, Object>>) raw.get("formation-memories")).stream()
                        .map(m -> new SocialConfig.FormationMemory(
                                ((Number) m.get("age")).intValue(),
                                (String) m.get("episode")))
                        .toList()
                : List.<SocialConfig.FormationMemory>of();

        return new SocialConfig(goals, drives, norms, beliefs, relationships, reinforcement, stageConfig, personaConstraint, tendencies, personalityFacets, relationalSchemas, attachment, formationMemories);
    }
}
