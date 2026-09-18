package io.casehub.examples.manor.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.casehub.engine.trust.TrustEvolutionConfig;
import io.casehub.engine.trust.TrustEvolutionConfig.ConsolidationConfig;
import io.casehub.engine.trust.TrustEvolutionConfig.LevelConfig;
import io.casehub.engine.trust.TrustEvolutionConfig.ScoringConfig;
import io.casehub.engine.trust.TrustEvolutionConfig.TrustEventMapping;
import io.casehub.ledger.api.model.AttestationVerdict;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public final class ManorTrustEvolutionConfigLoader {

    private static final String DEFAULT_RESOURCE = "META-INF/eidos/trust-evolution.yaml";

    private ManorTrustEvolutionConfigLoader() {}

    public static TrustEvolutionConfig load() {
        return load(DEFAULT_RESOURCE);
    }

    @SuppressWarnings("unchecked")
    public static TrustEvolutionConfig load(String resourcePath) {
        InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IllegalStateException("Trust evolution config not found: " + resourcePath);
        }

        var mapper = new ObjectMapper(new YAMLFactory());

        try (is) {
            Map<String, Object> raw = mapper.readValue(is, Map.class);
            return parseConfig(raw);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse trust evolution config: " + resourcePath, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static TrustEvolutionConfig parseConfig(Map<String, Object> raw) {
        var scoringRaw = (Map<String, Object>) raw.get("scoring");
        var scoring = new ScoringConfig(
            ((Number) scoringRaw.get("decay-half-life-days")).intValue(),
            ((Number) scoringRaw.get("negative-decay-multiplier")).doubleValue());

        var consolidationRaw = (Map<String, Object>) raw.get("consolidation");
        var consolidation = new ConsolidationConfig(
            (String) consolidationRaw.get("overlay-property"),
            ((Number) consolidationRaw.get("significant-change-threshold")).doubleValue());

        var levelsRaw = (Map<String, Object>) raw.get("levels");
        var levels = new LevelConfig(
            ((Number) levelsRaw.get("high")).doubleValue(),
            ((Number) levelsRaw.get("moderate")).doubleValue(),
            ((Number) levelsRaw.get("low")).doubleValue());

        var eventsRaw = (List<Map<String, Object>>) raw.get("events");
        var events = eventsRaw.stream()
            .map(m -> new TrustEventMapping(
                (String) m.get("type"),
                AttestationVerdict.valueOf((String) m.get("verdict")),
                ((Number) m.get("confidence")).doubleValue(),
                ((Number) m.get("witness-confidence")).doubleValue()))
            .toList();

        return new TrustEvolutionConfig(events, scoring, consolidation, levels);
    }
}
