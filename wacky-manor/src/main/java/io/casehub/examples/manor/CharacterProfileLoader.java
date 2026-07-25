package io.casehub.examples.manor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class CharacterProfileLoader {

    private static final String DEFAULT_PATH = "/META-INF/eidos/descriptors.yaml";

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DescriptorFile(List<CharacterProfile> descriptors) {}

    public static Map<String, CharacterProfile> load() {
        return load(DEFAULT_PATH);
    }

    public static Map<String, CharacterProfile> load(String classpathResource) {
        var mapper = new ObjectMapper(new YAMLFactory());
        try (var stream = CharacterProfileLoader.class.getResourceAsStream(classpathResource)) {
            if (stream == null) {
                throw new IllegalStateException("Resource not found: " + classpathResource);
            }
            var file = mapper.readValue(stream, DescriptorFile.class);
            return file.descriptors().stream()
                    .collect(Collectors.toUnmodifiableMap(
                            CharacterProfile::agentId,
                            Function.identity()));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load descriptors from " + classpathResource, e);
        }
    }
}
