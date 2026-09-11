package com.houkunlin.smcrypt.codec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EncodingDetectorTest {

    @Test
    void explicitHexPrefix() {
        CipherPayload payload = EncodingDetector.detect("hex,0a0b");
        assertEquals(CipherEncoding.HEX, payload.encoding());
        assertEquals("0a0b", payload.text());
        assertArrayEquals(new byte[]{0x0a, 0x0b}, payload.decode());
    }

    @Test
    void explicitBase64Prefix() {
        CipherPayload payload = EncodingDetector.detect("base64,YWJjZA==");
        assertEquals(CipherEncoding.BASE64, payload.encoding());
        assertEquals("YWJjZA==", payload.text());
        assertArrayEquals("abcd".getBytes(), payload.decode());
    }

    @Test
    void explicitPrefixIsCaseInsensitive() {
        assertEquals(CipherEncoding.HEX, EncodingDetector.detect("HEX,0A0B").encoding());
        assertEquals(CipherEncoding.BASE64, EncodingDetector.detect("B64,YWJjZA==").encoding());
    }

    @Test
    void autoDetectHex() {
        CipherPayload payload = EncodingDetector.detect("0A0B1C2D");
        assertEquals(CipherEncoding.HEX, payload.encoding());
        assertArrayEquals(new byte[]{0x0A, 0x0B, 0x1C, 0x2D}, payload.decode());
    }

    @Test
    void autoDetectBase64() {
        CipherPayload payload = EncodingDetector.detect("YWJjZA==");
        assertEquals(CipherEncoding.BASE64, payload.encoding());
        assertArrayEquals("abcd".getBytes(), payload.decode());
    }

    @Test
    void oddLengthHexFallsBackToBase64() {
        assertEquals(CipherEncoding.BASE64, EncodingDetector.detectEncoding("abc"));
    }
}
