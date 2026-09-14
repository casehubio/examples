package io.casehub.examples.manor.agent;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ManorCognitiveSeederTest {

    @Test
    void seedResultTracksNodeTimestamps() {
        var result = new ManorCognitiveSeeder.SeedResult("sub-1",
                Map.of("penelope-awareness", Instant.now()));
        assertThat(result.subgraphId()).isEqualTo("sub-1");
        assertThat(result.seededNodeTimestamps()).containsKey("penelope-awareness");
    }

    @Test
    void emptyConfigProducesEmptyTimestamps() {
        var result = new ManorCognitiveSeeder.SeedResult("sub-1", Map.of());
        assertThat(result.seededNodeTimestamps()).isEmpty();
    }

    @Test
    void seedResultSubgraphIdFollowsNamingConvention() {
        var result = new ManorCognitiveSeeder.SeedResult(
                ManorCognitiveSeeder.subgraphName("hooded-claw"), Map.of());
        assertThat(result.subgraphId()).isEqualTo("beliefs-hooded-claw");
    }
}
