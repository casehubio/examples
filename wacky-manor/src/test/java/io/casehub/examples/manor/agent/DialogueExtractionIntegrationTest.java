package io.casehub.examples.manor.agent;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import io.casehub.neocortex.mindmap.intelligence.MindMapExtractor;
import io.casehub.neocortex.mindmap.MindMapStore;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class DialogueExtractionIntegrationTest {

    @Inject
    Instance<MindMapExtractor> mindMapExtractorInstance;

    @Inject
    Instance<MindMapStore> mindMapStoreInstance;

    @Test
    void mindMapExtractor_resolvableOrGracefullyAbsent() {
        if (mindMapExtractorInstance.isResolvable()) {
            assertThat(mindMapExtractorInstance.get()).isNotNull();
        }
    }

    @Test
    void mindMapStore_subgraphCreation_followsSeederConvention() {
        if (!mindMapStoreInstance.isResolvable()) return;
        var store = mindMapStoreInstance.get();
        var subgraphId = store.createSubgraph(
                new io.casehub.neocortex.mindmap.SubgraphInput(
                        ManorCognitiveSeeder.subgraphName("test-extraction-agent"),
                        "cognitive", null),
                "test-tenant");
        assertThat(subgraphId).isNotNull();
    }
}
