package io.casehub.examples.manor.web;

import io.casehub.examples.manor.ManorConstants;
import io.casehub.examples.manor.agent.CharacterCognition;
import io.casehub.examples.manor.agent.ManorContextStrategy;
import io.casehub.examples.manor.agent.ManorSocialConfigLoader;
import io.casehub.examples.manor.agent.SocialConfig;
import io.casehub.examples.manor.model.CharacterState;
import io.casehub.neocortex.mindmap.MindMapQuery;
import io.casehub.neocortex.mindmap.MindMapStore;
import io.casehub.neocortex.mindmap.SubgraphTypes;
import io.casehub.agency.perception.observation.affordance.ObservationSection;
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
public class CognitiveDumpResource {

    @Inject
    Instance<MindMapStore> mindMapStoreInstance;

    @GET
    @Path("/cognitive-dump/{characterId}")
    @Produces("application/json")
    public Response cognitiveDump(@PathParam("characterId") String characterId) {
        var store = mindMapStoreInstance.isResolvable() ? mindMapStoreInstance.get() : null;
        if (store == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                           .entity(Map.of("error", "MindMapStore not available")).build();
        }

        var tenantId = ManorConstants.TENANCY_ID;
        var allCognitive = store.search(
            MindMapQuery.of(tenantId, 2000).withType(SubgraphTypes.COGNITIVE));

        var result = new LinkedHashMap<String, Object>();
        result.put("characterId", characterId);
        result.put("totalCognitiveNodes", allCognitive.size());

        var charNodes = allCognitive.stream()
            .filter(n -> characterId.equals(n.properties().get("agent-id")))
            .toList();
        result.put("characterNodes", charNodes.size());

        var byKind = charNodes.stream()
            .collect(Collectors.groupingBy(
                n -> n.properties().getOrDefault("cognitiveKind", "none"),
                Collectors.counting()));
        result.put("nodeKinds", byKind);

        var byTrait = charNodes.stream()
            .flatMap(n -> n.traits().stream())
            .collect(Collectors.groupingBy(t -> t, Collectors.counting()));
        result.put("traitCounts", byTrait);

        var descriptions = charNodes.stream()
            .filter(n -> n.traits().contains("DriveDescriptionGenerated"))
            .map(n -> Map.of(
                "drive", n.properties().getOrDefault("drive-type", "?"),
                "description", n.properties().getOrDefault("generated-description", "(none)"),
                "intensity", n.properties().getOrDefault("drive-intensity", "?")))
            .toList();
        result.put("generatedDescriptions", descriptions);

        var profiles = charNodes.stream()
            .filter(n -> n.traits().contains("DriveProfileInferred"))
            .map(n -> Map.of(
                "drive", n.properties().getOrDefault("drive-type", "?"),
                "inferredInputs", n.properties().getOrDefault("inferred-inputs", "(none)")))
            .toList();
        result.put("driveProfiles", profiles);

        var competitions = charNodes.stream()
            .filter(n -> n.traits().contains("DriveCompetitionInferred"))
            .map(n -> Map.of(
                "driveA", n.properties().getOrDefault("drive-a", "?"),
                "driveB", n.properties().getOrDefault("drive-b", "?"),
                "differentiationText", n.properties().getOrDefault("differentiation-text", "(none)")))
            .toList();
        result.put("driveCompetition", competitions);

        var configs = ManorSocialConfigLoader.loadForProfile("generic");
        var socialConfig = configs.getOrDefault(characterId, SocialConfig.empty());
        var cognition = new CharacterCognition(
            characterId, null, null, socialConfig, List.of(),
            null, new ManorContextStrategy(), null, null,
            tenantId, store, null, null, true);

        var dummyCharacter = new CharacterState(characterId, characterId, "dump", 0.0, List.of());
        var sections = cognition.renderCognitiveSections(dummyCharacter, List.of(), Map.of());

        var sectionDump = new ArrayList<Map<String, String>>();
        for (var section : sections) {
            var s = new LinkedHashMap<String, String>();
            s.put("header", section.header());
            if (section instanceof ObservationSection.TextBlock tb) {
                s.put("content", tb.content());
            } else if (section instanceof ObservationSection.ItemList il) {
                s.put("content", String.join("\n", il.items()));
            }
            sectionDump.add(s);
        }
        result.put("cognitiveSections", sectionDump);

        var driveStatus = new ArrayList<Map<String, String>>();
        for (var drive : socialConfig.drives()) {
            var ds = new LinkedHashMap<String, String>();
            ds.put("type", drive.type());
            ds.put("intensity", String.valueOf(drive.intensity()));
            boolean hasGenerated = descriptions.stream()
                .anyMatch(d -> drive.type().equals(d.get("drive")));
            ds.put("source", hasGenerated ? "LLM-generated" : "hand-authored-fallback");
            driveStatus.add(ds);
        }
        result.put("driveStatus", driveStatus);

        return Response.ok(result).build();
    }
}
