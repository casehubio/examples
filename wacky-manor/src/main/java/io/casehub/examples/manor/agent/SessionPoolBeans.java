package io.casehub.examples.manor.agent;

import io.casehub.platform.agent.BackendInstanceRegistry;
import io.casehub.platform.agent.config.PoolDeclaration;
import io.casehub.platform.agent.session.SessionPoolRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import java.util.List;

@ApplicationScoped
public class SessionPoolBeans {

    @Produces
    @Singleton
    SessionPoolRegistry sessionPoolRegistry(BackendInstanceRegistry backendRegistry) {
        return new SessionPoolRegistry(
                List.of(new PoolDeclaration(
                        "character-pool", "character", "claude", 0, 6, null, null, null)),
                backendRegistry);
    }
}
