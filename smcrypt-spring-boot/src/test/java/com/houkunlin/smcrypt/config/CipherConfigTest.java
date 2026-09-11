package com.houkunlin.smcrypt.config;

import com.houkunlin.smcrypt.codec.CipherEncoding;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link CipherConfig} 变换串解析测试。
 */
class CipherConfigTest {
    private static final Map<String, String> EMPTY_PROPERTIES = Collections.emptyMap();

    @Test
    void modeBuildsTransformationUsingJceAlgorithmNameCase() {
        Map<String, String> properties = new HashMap<>();
        properties.put("smcrypt.desede.mode", "CBC");
        properties.put("smcrypt.desede.iv", "0123456789abcdef");

        CipherConfig config = CipherConfig.resolve("DESEDE", "DESede", properties::get,
                "DESede/ECB/PKCS5Padding", CipherEncoding.BASE64);

        assertEquals("DESede/CBC/PKCS5Padding", config.transformation());
    }

    @Test
    void modeBuildsTransformationWithHyphenatedJceAlgorithm() {
        Map<String, String> properties = new HashMap<>();
        properties.put("smcrypt.gost3412.mode", "CBC");

        CipherConfig config = CipherConfig.resolve("GOST3412", "GOST3412-2015", properties::get,
                "GOST3412-2015/ECB/PKCS5Padding", CipherEncoding.BASE64);

        assertEquals("GOST3412-2015/CBC/PKCS5Padding", config.transformation());
    }

    @Test
    void modeUsesDefaultPaddingWhenPaddingNotConfigured() {
        Map<String, String> properties = new HashMap<>();
        properties.put("smcrypt.aes.mode", "CBC");

        CipherConfig config = CipherConfig.resolve("AES", "AES", properties::get,
                "AES/ECB/PKCS5Padding", CipherEncoding.BASE64);

        assertEquals("AES/CBC/PKCS5Padding", config.transformation());
    }

    @Test
    void explicitTransformationTakesPrecedenceOverMode() {
        Map<String, String> properties = new HashMap<>();
        properties.put("smcrypt.aes.transformation", "AES/GCM/NoPadding");
        properties.put("smcrypt.aes.mode", "CBC");

        CipherConfig config = CipherConfig.resolve("AES", "AES", properties::get,
                "AES/ECB/PKCS5Padding", CipherEncoding.BASE64);

        assertEquals("AES/GCM/NoPadding", config.transformation());
    }

    @Test
    void sm2ModeDoesNotBuildTransformation() {
        Map<String, String> properties = new HashMap<>();
        properties.put("smcrypt.sm2.mode", "C1C2C3");

        CipherConfig config = CipherConfig.resolve("SM2", "SM2", properties::get,
                "SM2", CipherEncoding.BASE64);

        assertEquals("SM2", config.transformation());
        assertEquals("C1C2C3", config.mode());
    }

    @Test
    void defaultTransformationUsedWhenModeNotConfigured() {
        CipherConfig config = CipherConfig.resolve("AES", "AES", EMPTY_PROPERTIES::get,
                "AES/ECB/PKCS5Padding", CipherEncoding.BASE64);

        assertEquals("AES/ECB/PKCS5Padding", config.transformation());
        assertEquals(CipherEncoding.BASE64, config.encoding());
    }
}
