package io.casehub.examples.manor.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.neocortex.caps.CapsTopology;
import io.casehub.neocortex.caps.NodeType;
import io.casehub.neocortex.caps.SituationActivation;
import io.casehub.neocortex.caps.SituationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PipelineSituationClassifier {

    private static final double CONFIDENCE_THRESHOLD = 0.3;

    private final String systemPrompt;
    private final Set<String> validNodeIds;
    private final ObjectMapper mapper = new ObjectMapper();

    public PipelineSituationClassifier(CapsTopology topology) {
        this.validNodeIds = topology.nodes().entrySet().stream()
                .filter(e -> e.getValue().type() == NodeType.INPUT)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        this.systemPrompt = buildSystemPrompt(topology);
    }

    public String systemPrompt() {
        return systemPrompt;
    }

    public String buildUserPrompt(String description, Map<String, String> metadata) {
        if (description == null || description.isBlank()) return null;
        var sb = new StringBuilder("Description: ").append(description);
        if (metadata != null && !metadata.isEmpty()) {
            sb.append("\n\nMetadata:");
            metadata.forEach((k, v) -> sb.append("\n  ").append(k).append(": ").append(v));
        }
        return sb.toString();
    }

    public SituationContext parseResponse(String raw) {
        if (raw == null || raw.isBlank()) return SituationContext.EMPTY;
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) return SituationContext.EMPTY;
        String json = raw.substring(start, end + 1);
        try {
            JsonNode root = mapper.readTree(json);
            JsonNode activationsNode = root.path("activations");
            if (!activationsNode.isArray()) return SituationContext.EMPTY;
            List<SituationActivation> result = new ArrayList<>();
            for (JsonNode entry : activationsNode) {
                String nodeId = entry.path("node").asText(null);
                double confidence = entry.path("confidence").asDouble(0.0);
                if (nodeId == null || !validNodeIds.contains(nodeId)) continue;
                if (confidence < CONFIDENCE_THRESHOLD) continue;
                result.add(new SituationActivation(nodeId, Math.min(1.0, confidence)));
            }
            return SituationContext.from(result);
        } catch (Exception e) {
            return SituationContext.EMPTY;
        }
    }

    private static String buildSystemPrompt(CapsTopology topology) {
        var sb = new StringBuilder("""
                You are a psychological situation classifier. Given a description of \
                an experience, classify it into one or more CAPS input node activations.

                Respond with a JSON object:
                {"activations": [{"node": "node_id", "confidence": 0.0-1.0}]}

                Rules:
                - Only use node IDs from the list below
                - confidence reflects how strongly the description matches the node
                - Multiple nodes can activate for the same description
                - Minimum confidence threshold is 0.3
                - Respond with valid JSON only

                Available input nodes:
                """);
        topology.nodes().forEach((id, node) -> {
            if (node.type() == NodeType.INPUT) {
                sb.append("- ").append(id);
                if (!node.description().isEmpty()) {
                    sb.append(" (").append(node.category())
                      .append(": ").append(node.description()).append(")");
                } else {
                    sb.append(" (").append(node.category()).append(")");
                }
                if (!node.vocabulary().isEmpty()) {
                    sb.append(" — vocabulary: ").append(String.join(", ", node.vocabulary().keySet()));
                }
                sb.append("\n");
            }
        });
        return sb.toString();
    }
}
