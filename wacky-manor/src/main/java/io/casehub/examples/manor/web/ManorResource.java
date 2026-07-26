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

    @Inject ScenarioOrchestrator orchestrator;
    @Inject ManorEventBus eventBus;

    private volatile WorldState activeWorld;

    @POST
    @Path("/start")
    public Response startScenario() {
        if (activeWorld != null && !activeWorld.isScenarioComplete()) {
            return Response.status(Response.Status.CONFLICT)
                .entity("{\"error\":\"Scenario already running\"}")
                .build();
        }

        activeWorld = MansionLoader.loadWorld();
        eventBus.broadcast(ManorWebSocketEvent.scenario("started"));
        eventBus.broadcast(eventBus.buildSnapshot(activeWorld));

        orchestrator.startScenario(activeWorld);

        return Response.accepted()
            .entity("{\"status\":\"started\"}")
            .build();
    }
}
