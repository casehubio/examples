package io.casehub.examples.manor.web;

import io.casehub.examples.manor.agent.ScenarioOrchestrator;
import io.casehub.examples.manor.engine.MansionLoader;
import io.casehub.examples.manor.engine.WorldState;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/manor")
public class ManorResource {

    @Inject
    ScenarioOrchestrator orchestrator;
    @Inject
    ManorEventBus        eventBus;

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

        var mode = io.casehub.examples.manor.model.ScenarioMode.valueOf(scenarioModeConfig.toUpperCase());
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
        var sb     = new StringBuilder("[");
        for (int i = 0; i < events.size(); i++) {
            var e = events.get(i);
            if (i > 0) {sb.append(",");}
            sb.append("{\"type\":\"").append(e.type())
              .append("\",\"character\":\"").append(e.characterId() != null ? e.characterId() : "")
              .append("\",\"room\":\"").append(e.room() != null ? e.room() : "")
              .append("\",\"desc\":\"").append(e.description().replace("\"", "\\\"")).append("\"}");
        }
        sb.append("]");
        String reason   = activeWorld.completionReason() != null ? activeWorld.completionReason().name() : "running";
        String complete = activeWorld.isScenarioComplete() ? "true" : "false";
        return Response.ok("{\"complete\":" + complete + ",\"reason\":\"" + reason + "\",\"events\":" + sb + "}").build();
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

}
