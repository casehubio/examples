package io.casehub.examples.manor.web;

import io.casehub.examples.manor.agent.PlaybookOrchestrator;
import io.casehub.examples.manor.engine.MansionLoader;
import io.casehub.examples.manor.engine.WorldState;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/manor")
public class ManorResource {

    @Inject
    PlaybookOrchestrator orchestrator;
    @Inject
    ManorEventBus        eventBus;
    @Inject
    io.casehub.eidos.api.AgentRegistry agentRegistry;
    @Inject
    io.casehub.eidos.api.VocabularyRegistry vocabRegistry;


    @org.eclipse.microprofile.config.inject.ConfigProperty(name = "manor.scenario.mode", defaultValue = "scripted")
    String scenarioModeConfig;

    volatile WorldState activeWorld;

    @POST
    @Path("/start")
    public Response startScenario() {
        if (activeWorld != null && !activeWorld.isScenarioComplete()) {
            return Response.status(Response.Status.CONFLICT)
                           .entity("{\"error\":\"Scenario already running\"}")
                           .build();
        }

        activeWorld = MansionLoader.loadWorld();
        eventBus.setActiveWorld(activeWorld);
        eventBus.broadcast(ManorWebSocketEvent.scenario("started"));
        eventBus.broadcast(eventBus.buildSnapshot(activeWorld));

        var mode = io.casehub.examples.manor.model.PlaybookMode.valueOf(scenarioModeConfig.toUpperCase());
        orchestrator.startScenario(activeWorld, mode);

        return Response.accepted()
                       .entity("{\"status\":\"started\",\"mode\":\"" + mode.name().toLowerCase() + "\"}")
                       .build();
    }

    @jakarta.ws.rs.GET
    @jakarta.ws.rs.Path("/events")
    @jakarta.ws.rs.Produces("application/json")
    public Response getEvents() {
        if (activeWorld == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"No scenario\"}").build();
        }
        var events = activeWorld.allEvents();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var root   = mapper.createObjectNode();
        root.put("complete", activeWorld.isScenarioComplete());
        root.put("reason", activeWorld.completionReason() != null ? activeWorld.completionReason().name() : "running");
        var eventsArray = mapper.createArrayNode();
        for (var e : events) {
            var node = mapper.createObjectNode();
            node.put("type", e.type());
            node.put("character", e.characterId() != null ? e.characterId() : "");
            node.put("room", e.room() != null ? e.room() : "");
            node.put("desc", e.description() != null ? e.description() : "");
            if (e.thinking() != null) {
                node.put("thinking", e.thinking());
            }
            eventsArray.add(node);
        }
        root.set("events", eventsArray);
        try {
            return Response.ok(mapper.writeValueAsString(root)).build();
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            return Response.serverError().entity("{\"error\":\"JSON serialization failed\"}").build();
        }
    }

    @POST
    @Path("/pause")
    public Response pauseScenario() {
        if (activeWorld == null || activeWorld.isScenarioComplete()) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("{\"error\":\"No active scenario\"}").build();
        }
        activeWorld.setPaused(true);
        eventBus.broadcast(ManorWebSocketEvent.control("paused", activeWorld.speedMultiplier()));
        return Response.ok("{\"status\":\"paused\"}").build();
    }

    @POST
    @Path("/resume")
    public Response resumeScenario() {
        if (activeWorld == null || activeWorld.isScenarioComplete()) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("{\"error\":\"No active scenario\"}").build();
        }
        activeWorld.setPaused(false);
        eventBus.broadcast(ManorWebSocketEvent.control("resumed", activeWorld.speedMultiplier()));
        return Response.ok("{\"status\":\"resumed\"}").build();
    }

    @POST
    @Path("/stop")
    public Response stopScenario() {
        if (activeWorld == null || activeWorld.isScenarioComplete()) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("{\"error\":\"No active scenario\"}").build();
        }
        activeWorld.setScenarioComplete(io.casehub.examples.manor.model.CompletionReason.DAWN);
        eventBus.broadcast(ManorWebSocketEvent.scenario("completed", "stopped"));
        return Response.ok("{\"status\":\"stopped\"}").build();
    }


    @POST
    @Path("/speed")
    public Response setSpeed(@jakarta.ws.rs.QueryParam("rate") double rate) {
        if (activeWorld == null || activeWorld.isScenarioComplete()) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("{\"error\":\"No active scenario\"}").build();
        }
        activeWorld.setSpeedMultiplier(rate);
        eventBus.broadcast(ManorWebSocketEvent.control("speed", activeWorld.speedMultiplier()));
        return Response.ok("{\"status\":\"speed\",\"rate\":" + activeWorld.speedMultiplier() + "}").build();
    }

    @jakarta.ws.rs.GET
    @jakarta.ws.rs.Path("/characters/{id}/profile")
    @jakarta.ws.rs.Produces("application/json")
    public Response getCharacterProfile(@jakarta.ws.rs.PathParam("id") String id) {
        var desc = agentRegistry.findById(id, io.casehub.examples.manor.ManorConstants.TENANCY_ID);
        if (desc.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("{\"error\":\"Character not found: " + id + "\"}").build();
        }
        var    descriptor = desc.get();
        String slotLabel  = null;
        if (descriptor.slotVocabulary() != null && descriptor.slot() != null) {
            slotLabel = vocabRegistry.resolve(descriptor.slotVocabulary(), descriptor.slot())
                                     .map(io.casehub.eidos.api.VocabularyTerm::label).orElse(descriptor.slot());
        }
        var dto = CharacterProfileDTO.from(descriptor, slotLabel, null);
        return Response.ok(dto).build();
    }


}
