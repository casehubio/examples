package io.casehub.examples.helpdesk.yaml;

import io.casehub.api.engine.YamlCaseHub;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class HelpdeskYamlCaseHub extends YamlCaseHub {

    public HelpdeskYamlCaseHub() {
        super("casehub/helpdesk-ticket.yaml");
    }
}
