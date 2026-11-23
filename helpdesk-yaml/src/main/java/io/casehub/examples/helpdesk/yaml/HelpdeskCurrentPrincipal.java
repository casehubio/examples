package io.casehub.examples.helpdesk.yaml;

import io.casehub.platform.api.identity.CurrentPrincipal;
import io.casehub.platform.api.identity.TenancyConstants;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import java.util.Set;

@ApplicationScoped
public class HelpdeskCurrentPrincipal implements CurrentPrincipal {

    @Override
    public String actorId() {
        return "hw-specialist";
    }

    @Override
    public Set<String> groups() {
        return Set.of("hw-specialist", "sw-specialist", "access-specialist", "general-specialist");
    }

    @Override
    public String tenancyId() {
        return TenancyConstants.DEFAULT_TENANT_ID;
    }

    @Override
    public boolean isCrossTenantAdmin() {
        return false;
    }
}
