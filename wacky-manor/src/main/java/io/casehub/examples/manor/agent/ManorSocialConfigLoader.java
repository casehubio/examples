package io.casehub.examples.manor.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

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

    @SuppressWarnings("unchecked")
    public static Map<String, SocialConfig> load(String resourcePath) {
        InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IllegalStateException("Social config not found: " + resourcePath);
        }

        var mapper = new ObjectMapper(new YAMLFactory());

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

        return new SocialConfig(drives, norms, beliefs);
    }
}
