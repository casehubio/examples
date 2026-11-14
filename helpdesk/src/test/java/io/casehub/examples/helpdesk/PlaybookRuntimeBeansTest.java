package io.casehub.examples.helpdesk;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlaybookRuntimeBeansTest {

    @Test
    void endpointsFollowTheConfiguredApplicationUrl() {
        var beans = new PlaybookRuntimeBeans();
        var local = beans.playbookConfig("http://localhost:8090");
        assertThat(local.restBaseUrl()).isEqualTo("http://localhost:8090");
        assertThat(local.graphQLEndpoint("helpdesk")).isEqualTo("http://localhost:8090/graphql");
        assertThat(local.pushEndpoint("helpdesk")).isEqualTo("ws://localhost:8090/push");

        var secure = beans.playbookConfig("https://example.test:8443");
        assertThat(secure.restBaseUrl()).isEqualTo("https://example.test:8443");
        assertThat(secure.graphQLEndpoint("helpdesk")).isEqualTo("https://example.test:8443/graphql");
        assertThat(secure.pushEndpoint("helpdesk")).isEqualTo("wss://example.test:8443/push");
    }
}
