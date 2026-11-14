package io.casehub.examples.helpdesk;

import io.casehub.pages.playbook.runtime.GraphQLDispatcher;
import io.casehub.pages.playbook.runtime.PlaybookConfig;
import io.casehub.pages.playbook.runtime.RestDispatcher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Map;

@ApplicationScoped
public class PlaybookRuntimeBeans {

    @Produces
    @Singleton
    PlaybookConfig playbookConfig(
            @ConfigProperty(name = "casehub.pages.playbook.rest-base-url") String baseUrl) {
        return new PlaybookConfig(baseUrl + "/graphql",
                baseUrl.replaceFirst("^http", "ws") + "/push", Map.of(), Map.of());
    }

    @Produces
    @Singleton
    RestDispatcher restDispatcher() {
        return new RestDispatcher();
    }

    @Produces
    @Singleton
    GraphQLDispatcher graphQLDispatcher() {
        return new GraphQLDispatcher();
    }
}
