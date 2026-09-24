package io.casehub.examples.manor.agent;

import io.casehub.platform.agent.BackendInstanceFactory;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class BackendFactoryDiscoveryTest {

    @Inject
    @Any
    Instance<BackendInstanceFactory> factories;

    @Test
    void claudeVertexFactoryIsDiscoverable() {
        var names = StreamSupport.stream(factories.spliterator(), false)
                                 .map(f -> f.getClass().getName())
                                 .collect(Collectors.toList());

        assertThat(names)
                .as("BackendInstanceFactory implementations discovered by CDI")
                .anyMatch(n -> n.contains("ClaudeVertexBackendFactory"));
    }

    @Test
    void classIsOnClasspath() {
        var cl = Thread.currentThread().getContextClassLoader();

        // Check where ClaudeBeans loads from
        try {
            var beansClass = cl.loadClass("io.casehub.platform.agent.claude.quarkus.ClaudeBeans");
            var source     = beansClass.getProtectionDomain().getCodeSource();
            System.out.println("ClaudeBeans loaded from: " + (source != null ? source.getLocation() : "null"));
        } catch (Exception e) {
            System.out.println("ClaudeBeans NOT loadable: " + e);
        }

        // Try to load the factory with full exception chain
        try {
            var clazz = cl.loadClass("io.casehub.platform.agent.claude.ClaudeVertexBackendFactory");
            System.out.println("ClaudeVertexBackendFactory loaded OK: " + clazz);
        } catch (Throwable t) {
            System.out.println("ClaudeVertexBackendFactory load FAILED:");
            Throwable cause = t;
            while (cause != null) {
                System.out.println("  " + cause.getClass().getName() + ": " + cause.getMessage());
                cause = cause.getCause();
            }
        }

        // Try to load dependent types
        for (String name : new String[]{
                "io.casehub.platform.agent.BackendInstanceFactory",
                "io.casehub.platform.agent.BackendInstance",
                "io.casehub.platform.agent.claude.DefaultClaudeAgentProperties",
                "io.casehub.platform.agent.claude.ClaudeAgentClient",
                "io.casehub.platform.agent.claude.ClaudeAgentProvider"}) {
            try {
                cl.loadClass(name);
                System.out.println("  OK: " + name);
            } catch (Throwable t2) {
                System.out.println("  FAIL: " + name + " -> " + t2.getClass().getSimpleName() + ": " + t2.getMessage());
            }
        }
    }

    @Test
    void claudeBeansIsDiscoverable() {
        var claudeBeans = jakarta.enterprise.inject.spi.CDI.current()
                                                           .select(io.casehub.platform.agent.claude.quarkus.ClaudeBeans.class);
        assertThat(claudeBeans.isResolvable())
                .as("ClaudeBeans from agent-claude JAR is discoverable")
                .isTrue();
    }
}
