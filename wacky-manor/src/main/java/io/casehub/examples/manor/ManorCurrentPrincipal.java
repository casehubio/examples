package io.casehub.examples.manor;

import io.casehub.platform.api.identity.CurrentPrincipal;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.util.Set;

@Alternative
@Priority(1000)
@ApplicationScoped
public class ManorCurrentPrincipal implements CurrentPrincipal {

    @Override
    public String actorId() { return "orchestrator"; }

    @Override
    public Set<String> groups() { return Set.of(); }

    @Override
    public String tenancyId() { return ManorConstants.TENANCY_ID; }

    @Override
    public boolean isCrossTenantAdmin() { return false; }
}
