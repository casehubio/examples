package io.casehub.examples.manor.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DriveContextDeserializationTest {

    @Test
    void gallantryDeserializesWithSocialContext() {
        var configs = ManorSocialConfigLoader.loadForProfile("GENERIC");
        var pp = configs.get("peter-perfect");
        for (var d : pp.drives()) {
            System.out.println(d.type() + " context=" + d.context() + " social=" + d.requiresSocialContext());
        }
        var gallantry = pp.drives().stream()
                .filter(d -> d.type().equals("gallantry"))
                .findFirst().orElseThrow();
        assertThat(gallantry.requiresSocialContext()).isTrue();
        assertThat(gallantry.context()).isEqualTo("social");
    }

    @Test
    void drivesWithoutContextDefaultToAny() {
        var configs = ManorSocialConfigLoader.loadForProfile("GENERIC");
        var pp = configs.get("peter-perfect");
        var protection = pp.drives().stream()
                .filter(d -> d.type().equals("protection"))
                .findFirst().orElseThrow();
        assertThat(protection.requiresSocialContext()).isFalse();
        assertThat(protection.context()).isEqualTo("any");
    }
}
