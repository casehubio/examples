package io.casehub.examples.manor.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SessionConfigTest {

    @Test
    void parsePlainNumber() {
        assertEquals(80_000, ManorConfig.SessionConfig.parseClearAtTokens("80000", 200_000));
    }

    @Test
    void parseKSuffix() {
        assertEquals(80_000, ManorConfig.SessionConfig.parseClearAtTokens("80k", 200_000));
    }

    @Test
    void parseUppercaseKSuffix() {
        assertEquals(120_000, ManorConfig.SessionConfig.parseClearAtTokens("120K", 200_000));
    }

    @Test
    void parsePercentage() {
        assertEquals(80_000, ManorConfig.SessionConfig.parseClearAtTokens("40%", 200_000));
    }

    @Test
    void parsePercentageWithDifferentContext() {
        assertEquals(50_000, ManorConfig.SessionConfig.parseClearAtTokens("50%", 100_000));
    }

    @Test
    void parseWithWhitespace() {
        assertEquals(80_000, ManorConfig.SessionConfig.parseClearAtTokens("  80k  ", 200_000));
    }

    @Test
    void invalidFormatThrows() {
        assertThrows(NumberFormatException.class,
                () -> ManorConfig.SessionConfig.parseClearAtTokens("abc", 200_000));
    }
}
