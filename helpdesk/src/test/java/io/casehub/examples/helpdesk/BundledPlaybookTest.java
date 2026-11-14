package io.casehub.examples.helpdesk;

import io.casehub.pages.playbook.PlaybookContentParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class BundledPlaybookTest {

    @Test
    void serverAndBrowserUseTheSameCurrentPlaybook() throws Exception {
        try (var server = getClass().getResourceAsStream("/playbooks/help-desk-demo.yaml");
             var browser = getClass().getResourceAsStream(
                     "/META-INF/resources/playbooks/help-desk-demo.yaml")) {
            assertThat(server).isNotNull();
            assertThat(browser).isNotNull();
            var yaml = server.readAllBytes();
            assertThat(browser.readAllBytes()).isEqualTo(yaml);
            var playbook = PlaybookContentParser.parse(new String(yaml, StandardCharsets.UTF_8));
            assertThat(playbook.playbook()).isEqualTo("help-desk-demo");
            assertThat(playbook.allSteps()).hasSize(28)
                    .allSatisfy(step -> assertThat(step.action()).isNotEqualTo("commands"));
        }
    }
}
