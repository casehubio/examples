package io.casehub.examples.manor.agent;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.casehub.neocortex.cognitive.Confidence;
import io.casehub.neocortex.cognitive.ConfidenceOrigin;
import io.casehub.neocortex.mindmap.MindMapNode;
import io.casehub.neocortex.mindmap.MindMapStore;
import io.casehub.neocortex.mindmap.NodeInput;
import io.casehub.neocortex.mindmap.NodeRef;
import io.casehub.neocortex.mindmap.SubgraphInput;
import io.casehub.platform.api.identity.PrincipalId;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class ConsolidationCache {

    private static final Logger log = Logger.getLogger(ConsolidationCache.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public static Path cachePath(String profile, Path cacheDir) {
        return cacheDir.resolve("consolidation-cache-" + profile.toLowerCase() + ".json");
    }

    public static boolean exists(String profile, Path cacheDir) {
        return Files.exists(cachePath(profile, cacheDir));
    }

    public static void dump(MindMapStore store, String tenantId, String profile, Path cacheDir) throws IOException {
        var subgraphs = store.listSubgraphs(tenantId);
        var cached = new ArrayList<Map<String, Object>>();

        for (var sg : subgraphs) {
            var nodes = store.nodesIn(sg.id(), tenantId);
            var cachedNodes = new ArrayList<Map<String, Object>>();
            for (var node : nodes) {
                cachedNodes.add(serializeNode(node));
            }
            var sgMap = new LinkedHashMap<String, Object>();
            sgMap.put("name", sg.name());
            sgMap.put("type", sg.type());
            sgMap.put("rootNodeId", sg.rootNodeId());
            sgMap.put("nodeCount", cachedNodes.size());
            sgMap.put("nodes", cachedNodes);
            cached.add(sgMap);
        }

        var root = new LinkedHashMap<String, Object>();
        root.put("profile", profile);
        root.put("timestamp", Instant.now().toString());
        root.put("subgraphCount", cached.size());
        int totalNodes = cached.stream().mapToInt(s -> ((List<?>) s.get("nodes")).size()).sum();
        root.put("totalNodes", totalNodes);
        root.put("subgraphs", cached);

        Files.createDirectories(cacheDir);
        MAPPER.writeValue(cachePath(profile, cacheDir).toFile(), root);
        log.infof("Consolidation cache saved: %d subgraphs, %d nodes → %s",
                cached.size(), totalNodes, cachePath(profile, cacheDir));
    }

    @SuppressWarnings("unchecked")
    public static boolean restore(MindMapStore store, String tenantId, String profile, Path cacheDir) {
        var path = cachePath(profile, cacheDir);
        if (!Files.exists(path)) {
            return false;
        }

        try {
            var root = MAPPER.readValue(path.toFile(), Map.class);
            var subgraphs = (List<Map<String, Object>>) root.get("subgraphs");

            int restoredNodes = 0;
            for (var sg : subgraphs) {
                String sgName = (String) sg.get("name");
                String sgType = (String) sg.get("type");
                String rootNodeId = (String) sg.get("rootNodeId");
                var nodes = (List<Map<String, Object>>) sg.get("nodes");

                String sgId = store.createSubgraph(new SubgraphInput(sgName, sgType, rootNodeId), tenantId);

                for (var nodeData : nodes) {
                    var input = deserializeNode(nodeData, sgId);
                    store.addNode(input, tenantId);
                    restoredNodes++;
                }
            }

            log.infof("Consolidation cache restored: %d subgraphs, %d nodes from %s",
                    subgraphs.size(), restoredNodes, path);
            return true;
        } catch (Exception e) {
            log.warnf(e, "Failed to restore consolidation cache from %s — will re-consolidate", path);
            return false;
        }
    }

    private static Map<String, Object> serializeNode(MindMapNode node) {
        var map = new LinkedHashMap<String, Object>();
        map.put("name", node.name());
        map.put("provenance", node.provenance());
        map.put("traits", node.traits() != null ? new ArrayList<>(node.traits()) : List.of());
        map.put("properties", node.properties() != null ? new LinkedHashMap<>(node.properties()) : Map.of());
        map.put("pleasure", node.pleasure());
        map.put("arousal", node.arousal());
        map.put("dominance", node.dominance());

        if (node.confidence() != null) {
            var conf = new LinkedHashMap<String, Object>();
            conf.put("origin", node.confidence().origin().name());
            conf.put("value", node.confidence().value());
            if (node.confidence().decayReference() != null) {
                conf.put("decayReference", node.confidence().decayReference().toString());
            }
            map.put("confidence", conf);
        }

        if (node.principalId() != null) {
            var pid = new LinkedHashMap<String, String>();
            pid.put("type", node.principalId().type().name());
            pid.put("id", node.principalId().id());
            map.put("principalId", pid);
        }

        if (node.refs() != null && !node.refs().isEmpty()) {
            var refs = node.refs().stream().map(r -> {
                var rm = new LinkedHashMap<String, String>();
                rm.put("scheme", r.scheme());
                rm.put("id", r.id());
                rm.put("qualifier", r.qualifier());
                return (Map<String, String>) rm;
            }).toList();
            map.put("refs", refs);
        }

        if (node.sharedWith() != null && !node.sharedWith().isEmpty()) {
            map.put("sharedWith", new ArrayList<>(node.sharedWith()));
        }

        if (node.validFrom() != null) {
            map.put("validFrom", node.validFrom().toString());
        }
        if (node.validUntil() != null) {
            map.put("validUntil", node.validUntil().toString());
        }

        return map;
    }

    @SuppressWarnings("unchecked")
    private static NodeInput deserializeNode(Map<String, Object> data, String subgraphId) {
        String name = (String) data.get("name");
        var input = NodeInput.of(name, subgraphId);

        var traits = (List<String>) data.get("traits");
        if (traits != null && !traits.isEmpty()) {
            input = input.withTraits(Set.copyOf(traits));
        }

        var properties = (Map<String, String>) data.get("properties");
        if (properties != null && !properties.isEmpty()) {
            input = input.withProperties(properties);
        }

        String provenance = (String) data.get("provenance");
        if (provenance != null) {
            input = input.withProvenance(provenance);
        }

        var confMap = (Map<String, Object>) data.get("confidence");
        if (confMap != null) {
            var origin = ConfidenceOrigin.valueOf((String) confMap.get("origin"));
            double value = ((Number) confMap.get("value")).doubleValue();
            String decayRef = (String) confMap.get("decayReference");
            Instant decay = decayRef != null ? Instant.parse(decayRef) : Instant.now();
            input = input.withConfidence(new Confidence(origin, value, decay));
        }

        var pidMap = (Map<String, String>) data.get("principalId");
        if (pidMap != null) {
            var actorType = io.casehub.platform.api.identity.ActorType.valueOf(pidMap.get("type"));
            input = input.withPrincipalId(new PrincipalId(actorType, pidMap.get("id")));
        }

        var refs = (List<Map<String, String>>) data.get("refs");
        if (refs != null && !refs.isEmpty()) {
            var nodeRefs = refs.stream()
                    .map(r -> new NodeRef(r.get("scheme"), r.get("id"), r.get("qualifier")))
                    .collect(Collectors.toSet());
            input = input.withRefs(nodeRefs);
        }

        var sharedWith = (List<String>) data.get("sharedWith");
        if (sharedWith != null && !sharedWith.isEmpty()) {
            input = input.withSharedWith(Set.copyOf(sharedWith));
        }

        Double pleasure = data.get("pleasure") != null ? ((Number) data.get("pleasure")).doubleValue() : null;
        Double arousal = data.get("arousal") != null ? ((Number) data.get("arousal")).doubleValue() : null;
        Double dominance = data.get("dominance") != null ? ((Number) data.get("dominance")).doubleValue() : null;
        if (pleasure != null || arousal != null || dominance != null) {
            input = input.withPad(pleasure, arousal, dominance);
        }

        String validFrom = (String) data.get("validFrom");
        if (validFrom != null) {
            input = input.withValidFrom(Instant.parse(validFrom));
        }
        String validUntil = (String) data.get("validUntil");
        if (validUntil != null) {
            input = input.withValidUntil(Instant.parse(validUntil));
        }

        return input;
    }
}
