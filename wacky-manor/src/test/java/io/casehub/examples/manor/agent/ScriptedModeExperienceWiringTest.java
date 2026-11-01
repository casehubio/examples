package io.casehub.examples.manor.agent;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ScriptedModeExperienceWiringTest {

    @Test
    void runScriptedAcceptsExperienceService() {
        var methods = Arrays.stream(PlaybookOrchestrator.class.getDeclaredMethods())
            .filter(m -> m.getName().equals("runScripted"))
            .toList();

        assertThat(methods).isNotEmpty();

        Method runScripted = methods.getFirst();
        var paramTypes = Arrays.asList(runScripted.getParameterTypes());

        assertThat(paramTypes)
            .as("runScripted must accept AgentExperienceService to wire experience recording in scripted mode")
            .anyMatch(p -> p.equals(AgentExperienceService.class));
    }
}
