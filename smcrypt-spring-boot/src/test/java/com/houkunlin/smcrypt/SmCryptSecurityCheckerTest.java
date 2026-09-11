package com.houkunlin.smcrypt;

import com.houkunlin.smcrypt.handler.DecryptHandler;
import com.houkunlin.smcrypt.key.SmCryptKeyGenerator;
import com.houkunlin.smcrypt.spi.CipherHandlerLoader;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SmCryptSecurityCheckerTest {
    private static final String KEY_16 = "0123456789abcdeffedcba9876543210";

    @Test
    void detectsEcbForSymmetricAlgorithm() {
        assertIssue(singleton("smcrypt.sm4.key", KEY_16), "ECB");
    }

    @Test
    void detectsDes() {
        assertIssue(singleton("smcrypt.des.key", "0123456789abcdef"), "56 位");
    }

    @Test
    void detectsDesEde() {
        assertIssue(singleton("smcrypt.desede.key", "0123456789abcdeffedcba98765432100123456789abcdef"), "3DES");
    }

    @Test
    void detectsRsaPkcs1() {
        assertIssue(singleton("smcrypt.rsa.key", rsaKey(2048)), "PKCS#1");
    }

    @Test
    void detectsRsaShortKey() {
        Map<String, String> properties = singleton("smcrypt.rsa.key", rsaKey(1024));
        properties.put("smcrypt.rsa.transformation", "RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        assertIssue(properties, "低于 2048 位");
    }

    @Test
    void detectsJasyptWeakAlgorithm() {
        Map<String, String> properties = singleton("smcrypt.jasypt.password", "pw");
        properties.put("smcrypt.jasypt.transformation", "PBEWithMD5AndDES");
        assertIssue(properties, "Jasypt");
    }

    @Test
    void detectsPbeFixedSalt() {
        Map<String, String> properties = singleton("smcrypt.pbe.password", "pw");
        properties.put("smcrypt.pbe.kdf.salt", "00112233445566778899aabbccddeeff");
        assertIssue(properties, "固定盐");
    }

    @Test
    void detectsPbeWeakIterations() {
        Map<String, String> properties = singleton("smcrypt.pbe.password", "pw");
        properties.put("smcrypt.pbe.kdf", "PBKDF2");
        properties.put("smcrypt.pbe.kdf.iterations", "1000");
        assertIssue(properties, "PBKDF2");
    }

    @Test
    void noIssuesForSafeConfig() {
        Map<String, String> properties = singleton("smcrypt.aes.key", KEY_16);
        properties.put("smcrypt.aes.transformation", "AES/GCM/NoPadding");
        properties.put("smcrypt.aes.iv", "00112233445566778899aabb");
        assertTrue(check(properties).isEmpty());
    }

    @Test
    void skipsUnconfiguredAlgorithms() {
        assertTrue(check(new HashMap<>()).isEmpty());
    }

    @Test
    void skipsCustomHandlers() {
        List<String> issues = new SmCryptSecurityChecker()
                .check(TestContexts.context(new HashMap<>()), Collections.singletonList(new CustomHandler()));
        assertTrue(issues.isEmpty());
    }

    private static void assertIssue(Map<String, String> properties, String expected) {
        List<String> issues = check(properties);
        assertTrue(issues.stream().anyMatch(issue -> issue.contains(expected)),
                "期望包含「" + expected + "」的问题，实际：" + issues);
    }

    private static List<String> check(Map<String, String> properties) {
        SmCryptContext context = TestContexts.context(properties);
        List<DecryptHandler> handlers = new CipherHandlerLoader().load(context);
        return new SmCryptSecurityChecker().check(context, handlers);
    }

    private static String rsaKey(int bits) {
        return SmCryptKeyGenerator.toPrivateKeyBase64(SmCryptKeyGenerator.generateKeyPair("RSA", bits).getPrivate());
    }

    private static Map<String, String> singleton(String key, String value) {
        Map<String, String> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    /**
     * 非 {@link com.houkunlin.smcrypt.handler.AbstractCipherHandler} 的自定义处理器，应被安全体检跳过。
     */
    private static final class CustomHandler implements DecryptHandler {
        @Override
        public String algorithm() {
            return "CUSTOM";
        }

        @Override
        public boolean support(String propValue) {
            return false;
        }

        @Override
        public String getCipherText(String propValue) {
            return propValue;
        }

        @Override
        public String getDecryptText(String propValue) {
            return propValue;
        }

        @Override
        public String getEncryptText(String plainText) {
            return plainText;
        }
    }
}
