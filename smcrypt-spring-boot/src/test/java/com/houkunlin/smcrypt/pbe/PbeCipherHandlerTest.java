package com.houkunlin.smcrypt.pbe;

import com.houkunlin.smcrypt.TestContexts;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PbeCipherHandlerTest {
    private static final String PASSWORD = "my-secret-password";

    @Test
    void kdfAesGcmRoundTrip() throws Exception {
        PbeCipherHandler handler = new PbeCipherHandler();
        handler.setContext(TestContexts.context(kdfProperties("AES/GCM/NoPadding", "PBKDF2")));
        String cipher = handler.getEncryptText("pbe-data");
        assertTrue(cipher.startsWith("PBEENC("));
        assertEquals("pbe-data", handler.getDecryptText(cipher));
    }

    @Test
    void kdfSm4CbcRoundTrip() throws Exception {
        assertKdfRoundTrip("SM4/CBC/PKCS5Padding", "PBKDF2");
    }

    @Test
    void kdfScryptRoundTrip() throws Exception {
        assertKdfRoundTrip("AES/GCM/NoPadding", "SCRYPT");
    }

    @Test
    void kdfArgon2RoundTrip() throws Exception {
        assertKdfRoundTrip("AES/GCM/NoPadding", "ARGON2");
    }

    @Test
    void kdfFixedSaltRoundTrip() throws Exception {
        Map<String, String> properties = kdfProperties("AES/GCM/NoPadding", "PBKDF2");
        properties.put("smcrypt.pbe.kdf.salt", "00112233445566778899aabbccddeeff");
        PbeCipherHandler handler = new PbeCipherHandler();
        handler.setContext(TestContexts.context(properties));
        assertEquals("pbe-fixed", handler.getDecryptText(handler.getEncryptText("pbe-fixed")));
    }

    @Test
    void kdfWrongPasswordFails() throws Exception {
        Map<String, String> properties = kdfProperties("AES/GCM/NoPadding", "PBKDF2");
        PbeCipherHandler handler = new PbeCipherHandler();
        handler.setContext(TestContexts.context(properties));
        String cipher = handler.getEncryptText("pbe-data");

        Map<String, String> wrong = new HashMap<>(properties);
        wrong.put("smcrypt.pbe.password", "wrong-password");
        PbeCipherHandler wrongHandler = new PbeCipherHandler();
        wrongHandler.setContext(TestContexts.context(wrong));
        assertThrows(Exception.class, () -> wrongHandler.getDecryptText(cipher));
    }

    @Test
    void kdfCustomSaltSizeRoundTrip() throws Exception {
        Map<String, String> properties = kdfProperties("AES/GCM/NoPadding", "PBKDF2");
        properties.put("smcrypt.pbe.kdf.salt-size", "24");
        PbeCipherHandler handler = new PbeCipherHandler();
        handler.setContext(TestContexts.context(properties));
        assertEquals("pbe-salt24", handler.getDecryptText(handler.getEncryptText("pbe-salt24")));
    }

    @Test
    void jceModeRoundTrip() throws Exception {
        Map<String, String> properties = new HashMap<>();
        properties.put("smcrypt.pbe.password", PASSWORD);
        properties.put("smcrypt.pbe.mode", "JCE");
        properties.put("smcrypt.pbe.transformation", "PBEWITHHMACSHA512ANDAES_256");
        PbeCipherHandler handler = new PbeCipherHandler();
        handler.setContext(TestContexts.context(properties));
        assertEquals("jce-data", handler.getDecryptText(handler.getEncryptText("jce-data")));
    }

    @Test
    void jceCustomSaltAndIvSizeRoundTrip() throws Exception {
        Map<String, String> properties = new HashMap<>();
        properties.put("smcrypt.pbe.password", PASSWORD);
        properties.put("smcrypt.pbe.mode", "JCE");
        properties.put("smcrypt.pbe.transformation", "PBEWITHHMACSHA512ANDAES_256");
        properties.put("smcrypt.pbe.salt-size", "8");
        properties.put("smcrypt.pbe.iv-size", "16");
        PbeCipherHandler handler = new PbeCipherHandler();
        handler.setContext(TestContexts.context(properties));
        assertEquals("jce-sizes", handler.getDecryptText(handler.getEncryptText("jce-sizes")));
    }

    private void assertKdfRoundTrip(String transformation, String kdf) throws Exception {
        PbeCipherHandler handler = new PbeCipherHandler();
        handler.setContext(TestContexts.context(kdfProperties(transformation, kdf)));
        assertEquals("pbe-data", handler.getDecryptText(handler.getEncryptText("pbe-data")));
    }

    private static Map<String, String> kdfProperties(String transformation, String kdf) {
        Map<String, String> properties = new HashMap<>();
        properties.put("smcrypt.pbe.password", PASSWORD);
        properties.put("smcrypt.pbe.mode", "KDF");
        properties.put("smcrypt.pbe.transformation", transformation);
        properties.put("smcrypt.pbe.kdf", kdf);
        // 降低迭代/内存参数以加快测试
        properties.put("smcrypt.pbe.kdf.cost", "16384");
        properties.put("smcrypt.pbe.kdf.memory", "16384");
        if ("PBKDF2".equals(kdf)) {
            properties.put("smcrypt.pbe.kdf.iterations", "1000");
        }
        return properties;
    }
}
