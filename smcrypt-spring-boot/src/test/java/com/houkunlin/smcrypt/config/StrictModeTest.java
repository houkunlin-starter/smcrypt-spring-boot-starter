package com.houkunlin.smcrypt.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StrictModeTest {

    @Test
    void parsesTokens() {
        assertEquals(StrictMode.OFF, StrictMode.fromToken(null));
        assertEquals(StrictMode.OFF, StrictMode.fromToken("  "));
        assertEquals(StrictMode.OFF, StrictMode.fromToken("off"));
        assertEquals(StrictMode.OFF, StrictMode.fromToken("false"));
        assertEquals(StrictMode.WARN, StrictMode.fromToken("WARN"));
        assertEquals(StrictMode.FAIL, StrictMode.fromToken("fail"));
        assertEquals(StrictMode.FAIL, StrictMode.fromToken("true"));
    }

    @Test
    void rejectsUnknownToken() {
        assertThrows(IllegalArgumentException.class, () -> StrictMode.fromToken("block"));
    }
}
