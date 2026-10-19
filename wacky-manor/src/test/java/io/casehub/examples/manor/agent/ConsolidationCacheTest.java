package io.casehub.examples.manor.agent;

import io.casehub.neocortex.cognitive.Confidence;
import io.casehub.neocortex.mindmap.NodeInput;
import io.casehub.neocortex.mindmap.SubgraphInput;
import io.casehub.neocortex.mindmap.SubgraphTypes;
import io.casehub.neocortex.mindmap.inmem.InMemoryMindMapStore;
import io.casehub.platform.api.identity.PrincipalId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ConsolidationCacheTest {

    @Test
    void dumpAndRestore_preservesSubgraphsAndNodes(@TempDir Path tempDir) throws Exception {
        var store = new InMemoryMindMapStore();
        var tenant = "test-tenant";

        var sgId = store.createSubgraph(new SubgraphInput("beliefs-hooded-claw", SubgraphTypes.COGNITIVE, null), tenant);
        store.addNode(NodeInput.of("scheming", sgId)
                .withTraits(Set.of("DriveProfileInferred"))
                .withProperties(Map.of("agent-id", "hooded-claw", "drive-type", "scheming", "inferred-inputs", "mastery:0.85"))
                .withProvenance("drive-profile-derivation")
                .withConfidence(Confidence.inferred(0.8, Instant.now()))
                .withPrincipalId(PrincipalId.agent("hooded-claw")), tenant);

        store.addNode(NodeInput.of("scheming-desc", sgId)
                .withTraits(Set.of("DriveDescriptionGenerated"))
                .withProperties(Map.of("agent-id", "hooded-claw", "drive-type", "scheming", "generated-description", "A cold settling..."))
                .withProvenance("drive-description-generation")
                .withPad(0.3, 0.7, 0.8), tenant);

        store.addNode(NodeInput.of("other-model-hc-pp", sgId)
                .withTraits(Set.of("OtherModelDerived"))
                .withProperties(Map.of("agent-id", "hooded-claw", "target-id", "penelope-pitstop",
                        "empathic-sensing", "A tightness gathers..."))
                .withProvenance("other-model-derivation"), tenant);

        ConsolidationCache.dump(store, tenant, "GENERIC", tempDir);

        assertThat(ConsolidationCache.exists("GENERIC", tempDir)).isTrue();
        assertThat(ConsolidationCache.cachePath("GENERIC", tempDir).toFile()).exists();

        var restored = new InMemoryMindMapStore();
        boolean success = ConsolidationCache.restore(restored, tenant, "GENERIC", tempDir);
        assertThat(success).isTrue();

        var subgraphs = restored.listSubgraphs(tenant);
        assertThat(subgraphs).hasSize(1);
        assertThat(subgraphs.get(0).name()).isEqualTo("beliefs-hooded-claw");
        assertThat(subgraphs.get(0).type()).isEqualTo(SubgraphTypes.COGNITIVE);

        var nodes = restored.nodesIn(subgraphs.get(0).id(), tenant);
        assertThat(nodes).hasSize(3);

        var profileNode = nodes.stream()
                .filter(n -> n.traits().contains("DriveProfileInferred"))
                .findFirst().orElseThrow();
        assertThat(profileNode.properties().get("drive-type")).isEqualTo("scheming");
        assertThat(profileNode.properties().get("inferred-inputs")).isEqualTo("mastery:0.85");
        assertThat(profileNode.provenance()).isEqualTo("drive-profile-derivation");
        assertThat(profileNode.principalId()).isNotNull();
        assertThat(profileNode.principalId().id()).isEqualTo("hooded-claw");

        var descNode = nodes.stream()
                .filter(n -> n.traits().contains("DriveDescriptionGenerated"))
                .findFirst().orElseThrow();
        assertThat(descNode.properties().get("generated-description")).isEqualTo("A cold settling...");
        assertThat(descNode.pleasure()).isEqualTo(0.3);
        assertThat(descNode.arousal()).isEqualTo(0.7);
        assertThat(descNode.dominance()).isEqualTo(0.8);

        var tomNode = nodes.stream()
                .filter(n -> n.traits().contains("OtherModelDerived"))
                .findFirst().orElseThrow();
        assertThat(tomNode.properties().get("target-id")).isEqualTo("penelope-pitstop");
        assertThat(tomNode.properties().get("empathic-sensing")).isEqualTo("A tightness gathers...");
    }

    @Test
    void restore_returnsFalseWhenNoCacheExists(@TempDir Path tempDir) {
        var store = new InMemoryMindMapStore();
        boolean result = ConsolidationCache.restore(store, "test", "GENERIC", tempDir);
        assertThat(result).isFalse();
    }
}
