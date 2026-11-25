package io.casehub.examples.helpdesk.yaml;

import io.casehub.pages.playbook.runtime.HeadlessPlaybookRunner;
import io.casehub.pages.playbook.runtime.HeadlessPlaybookRunner.HeadlessRunnerConfig;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@Disabled("Requires Playwright browsers installed — run with: mvn test -Dtest=PlaybookIntegrationTest")
class PlaybookIntegrationTest {

    @Test
    void helpdeskDemoPlaybookPasses() {
        InputStream playbook = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("playbooks/helpdesk-demo.yaml");
        assertThat(playbook).as("helpdesk-demo.yaml must be on classpath (from helpdesk-shared)")
                .isNotNull();

        var config = HeadlessRunnerConfig.builder()
                .baseUrl("http://127.0.0.1:8190")
                .headless(true)
                .timeout(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(500))
                .build();

        var runner = new HeadlessPlaybookRunner(config);
        var result = runner.run(playbook);

        assertThat(result.passed())
                .as("All playbook steps should pass: %s",
                        result.failedSteps().stream()
                                .map(s -> s.stepName() + ": " + s.error())
                                .toList())
                .isTrue();
        assertThat(result.failedSteps()).isEmpty();
    }
}
