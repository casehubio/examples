package io.casehub.examples.manor.web;

import io.casehub.examples.manor.ManorConstants;
import io.casehub.neocortex.memory.CaseMemoryStore;
import io.casehub.neocortex.memory.Memory;
import io.casehub.neocortex.memory.MemoryQuery;
import io.casehub.neocortex.memory.MemoryScanRequest;
import io.casehub.neocortex.memory.Subject;
import io.casehub.neocortex.memory.experience.ExperienceEvents;
import io.casehub.neocortex.memory.experience.SubThoughtAttributeKeys;
import io.casehub.neocortex.mindmap.MindMapNode;
import io.casehub.neocortex.mindmap.MindMapQuery;
import io.casehub.neocortex.mindmap.MindMapStore;
import io.casehub.neocortex.mindmap.SubgraphTypes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/manor")
public class MemorySnapshotResource {

    @Inject
    Instance<MindMapStore> mindMapStoreInstance;

    @Inject
    Instance<CaseMemoryStore> memoryStoreInstance;

    @GET
    @Path("/memory-snapshot/{characterId}")
    @Produces("application/json")
    public Response memorySnapshot(@PathParam("characterId") String characterId) {
        var result = new LinkedHashMap<String, Object>();
        result.put("characterId", characterId);
        result.put("tenantId", ManorConstants.TENANCY_ID);

        var mindMap = mindMapStoreInstance.isResolvable() ? mindMapStoreInstance.get() : null;
        var memStore = memoryStoreInstance.isResolvable() ? memoryStoreInstance.get() : null;

        result.put("mindMapStoreClass", mindMap != null ? mindMap.getClass().getName() : "null");
        result.put("memoryStoreClass", memStore != null ? memStore.getClass().getName() : "null");

        if (mindMap != null) {
            result.put("mindMap", snapshotMindMap(mindMap, characterId));
        }

        if (memStore != null) {
            result.put("memories", snapshotMemories(memStore, characterId));
        }

        result.put("contextResetAssessment", assessContextReset(mindMap, memStore, characterId));

        return Response.ok(result).build();
    }

    @GET
    @Path("/memory-snapshot-all")
    @Produces("application/json")
    public Response memorySnapshotAll() {
        var mindMap = mindMapStoreInstance.isResolvable() ? mindMapStoreInstance.get() : null;
        if (mindMap == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                           .entity(Map.of("error", "MindMapStore not available")).build();
        }

        var result = new LinkedHashMap<String, Object>();
        var tenantId = ManorConstants.TENANCY_ID;

        var subgraphs = mindMap.listSubgraphs(tenantId);
        result.put("totalSubgraphs", subgraphs.size());

        var subgraphSummaries = new ArrayList<Map<String, Object>>();
        int totalNodes = 0;
        for (var sg : subgraphs) {
            var nodes = mindMap.nodesIn(sg.id(), tenantId);
            totalNodes += nodes.size();
            var summary = new LinkedHashMap<String, Object>();
            summary.put("id", sg.id());
            summary.put("name", sg.name());
            summary.put("type", sg.type());
            summary.put("nodeCount", nodes.size());
            subgraphSummaries.add(summary);
        }
        result.put("totalNodes", totalNodes);
        result.put("subgraphs", subgraphSummaries);

        return Response.ok(result).build();
    }

    private Map<String, Object> snapshotMindMap(MindMapStore store, String characterId) {
        var tenantId = ManorConstants.TENANCY_ID;
        var result = new LinkedHashMap<String, Object>();

        var subgraphs = store.listSubgraphs(tenantId);
        result.put("totalSubgraphs", subgraphs.size());

        var allTypes = List.of(
            SubgraphTypes.COGNITIVE, SubgraphTypes.GOAL, SubgraphTypes.ACTIVITY,
            SubgraphTypes.BEHAVIORAL, SubgraphTypes.PERSON, SubgraphTypes.CONCEPT,
            SubgraphTypes.GENERAL, SubgraphTypes.TYPE_SYSTEM, SubgraphTypes.PLACE,
            SubgraphTypes.CULTURAL, "social");

        var typeDetails = new LinkedHashMap<String, Object>();
        int totalNodes = 0;

        for (String type : allTypes) {
            var nodes = store.search(MindMapQuery.of(tenantId, 5000).withType(type));

            var charNodes = nodes.stream()
                .filter(n -> characterId.equals(n.properties().get("agent-id"))
                          || characterId.equals(n.properties().get("agentId"))
                          || n.name().contains(characterId))
                .toList();

            if (!charNodes.isEmpty()) {
                var detail = new LinkedHashMap<String, Object>();
                detail.put("totalNodesThisType", nodes.size());
                detail.put("characterNodes", charNodes.size());
                detail.put("nodes", charNodes.stream().map(this::nodeToMap).toList());
                typeDetails.put(type, detail);
                totalNodes += charNodes.size();
            }
        }

        result.put("characterNodeCount", totalNodes);
        result.put("nodesByType", typeDetails);

        var charSubgraphName = "beliefs-" + characterId;
        var charSubgraph = subgraphs.stream()
            .filter(sg -> charSubgraphName.equals(sg.name()))
            .findFirst();

        if (charSubgraph.isPresent()) {
            var sgNodes = store.nodesIn(charSubgraph.get().id(), tenantId);
            var edgeSummary = new ArrayList<Map<String, String>>();
            for (var node : sgNodes) {
                var edges = store.neighbors(node.id(), tenantId);
                for (var edge : edges) {
                    var e = new LinkedHashMap<String, String>();
                    e.put("from", node.name());
                    e.put("edgeType", edge.edgeType());
                    e.put("to", edge.targetNodeId());
                    edgeSummary.add(e);
                }
            }
            result.put("edgesInCognitiveSubgraph", edgeSummary);
        }

        return result;
    }

    private Map<String, Object> snapshotMemories(CaseMemoryStore store, String characterId) {
        var tenantId = ManorConstants.TENANCY_ID;
        var result = new LinkedHashMap<String, Object>();

        try {
            var allMemories = store.scan(new MemoryScanRequest(tenantId, null, null, null, 500, null));
            result.put("totalMemoriesAllDomains", allMemories.size());

            var expMemories = store.scan(new MemoryScanRequest(tenantId, ExperienceEvents.DOMAIN.name(), null, null, 500, null));
            result.put("experienceMemories", expMemories.size());

            var charMemories = store.query(MemoryQuery.forSubject(Subject.of("agent", characterId), ExperienceEvents.DOMAIN, tenantId).withLimit(100));
            result.put("characterExperienceMemories", charMemories.size());

            if (!charMemories.isEmpty()) {
                result.put("memoryDetails", charMemories.stream().map(this::memoryToMap).toList());
                long withSubThoughts = charMemories.stream().filter(m -> m.attributes().containsKey(SubThoughtAttributeKeys.COUNT)).count();
                result.put("memoriesWithSubThoughts", withSubThoughts);
            }
        } catch (Exception e) {
            result.put("scanError", e.getClass().getName() + ": " + e.getMessage());
        }

        return result;
    }

    private Map<String, Object> assessContextReset(MindMapStore mindMap, CaseMemoryStore memStore, String characterId) {
        var assessment = new LinkedHashMap<String, Object>();
        var knows = new ArrayList<String>();
        var missing = new ArrayList<String>();

        if (mindMap != null) {
            var tenantId = ManorConstants.TENANCY_ID;
            var cogNodes = mindMap.search(MindMapQuery.of(tenantId, 1000).withType(SubgraphTypes.COGNITIVE));
            var charCog = cogNodes.stream().filter(n -> characterId.equals(n.properties().get("agent-id"))).toList();

            var kinds = charCog.stream().collect(Collectors.groupingBy(n -> n.properties().getOrDefault("cognitiveKind", "none"), Collectors.counting()));

            if (kinds.containsKey("drive-intensity")) knows.add("Drive intensities (" + kinds.get("drive-intensity") + " drives)");
            if (kinds.containsKey("personality-facet")) knows.add("Personality facets (" + kinds.get("personality-facet") + ")");
            if (kinds.containsKey("attachment-style")) knows.add("Attachment style");
            if (kinds.containsKey("need-satisfaction")) knows.add("Need satisfaction levels (" + kinds.get("need-satisfaction") + " tiers)");
            if (kinds.containsKey("relational-schema")) knows.add("Relational schemas (" + kinds.get("relational-schema") + " relationships)");

            long beliefs = charCog.stream().filter(n -> n.traits().contains("Belieflike")).count();
            if (beliefs > 0) knows.add("Initial beliefs (" + beliefs + ")");

            long generated = charCog.stream().filter(n -> n.traits().contains("DriveDescriptionGenerated")).count();
            if (generated > 0) knows.add("LLM-generated drive descriptions (" + generated + ")");

            long charGoals = mindMap.search(MindMapQuery.of(tenantId, 500).withType(SubgraphTypes.GOAL)).stream()
                .filter(n -> characterId.equals(n.properties().get("agent-id"))).count();
            if (charGoals > 0) knows.add("Goals (" + charGoals + ")");
        }

        missing.add("What happened this session (no experience memories persisted)");
        missing.add("Who said what (no dialogue transcript stored)");
        missing.add("What room they're in (no location tracking persisted)");
        missing.add("Who else is present (no co-presence tracking persisted)");
        missing.add("What they were doing (no action history persisted)");
        missing.add("Sub-thoughts about events (no sub-thought graduation)");
        missing.add("Emotional reactions to events (computed per-tick, discarded)");
        missing.add("Narrator summaries (broadcast via WebSocket, never stored)");
        missing.add("Formation memories (seeded to NoOp store, lost)");
        missing.add("Tiered observation summaries (computed every tick, discarded)");

        assessment.put("characterWouldKnow", knows);
        assessment.put("characterWouldNOTKnow", missing);
        assessment.put("verdict", "After context clear, character retains only static personality profile. All experiential knowledge (what happened, who said what, emotional reactions) is lost. The character would have amnesia about everything that occurred.");

        return assessment;
    }

    private Map<String, Object> nodeToMap(MindMapNode node) {
        var m = new LinkedHashMap<String, Object>();
        m.put("id", node.id());
        m.put("name", node.name());
        m.put("subgraphType", node.subgraphType());
        m.put("provenance", node.provenance());
        m.put("traits", node.traits());
        m.put("confidence", node.confidence() != null ? node.confidence().value() : null);
        m.put("properties", node.properties());
        m.put("createdAt", node.createdAt() != null ? node.createdAt().toString() : null);
        return m;
    }

    private Map<String, Object> memoryToMap(Memory memory) {
        var m = new LinkedHashMap<String, Object>();
        m.put("memoryId", memory.memoryId());
        m.put("subject", memory.subject().id());
        m.put("domain", memory.domain().name());
        m.put("text", memory.text());
        m.put("attributes", memory.attributes());
        m.put("createdAt", memory.createdAt() != null ? memory.createdAt().toString() : null);
        return m;
    }
}
