package com.houkunlin.smcrypt.key;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class KeyEncodingTest {

    @Test
    void parsesTokens() {
        assertNull(KeyEncoding.fromToken(null));
        assertNull(KeyEncoding.fromToken("  "));
        assertEquals(KeyEncoding.HEX, KeyEncoding.fromToken("hex"));
        assertEquals(KeyEncoding.BASE64, KeyEncoding.fromToken("B64"));
        assertEquals(KeyEncoding.PLAIN, KeyEncoding.fromToken("utf-8"));
    }

    @Test
    void rejectsUnknownToken() {
        assertThrows(IllegalArgumentException.class, () -> KeyEncoding.fromToken("unknown"));
    }

    @Test
    void decodesKeyWithExplicitEncoding() {
        assertArrayEquals(new byte[]{0x0a, (byte) 0xff}, KeyCodec.decodeKey("0a ff", KeyEncoding.HEX));
        assertArrayEquals("a b".getBytes(StandardCharsets.UTF_8), KeyCodec.decodeKey("a b", KeyEncoding.PLAIN));
    }
}
