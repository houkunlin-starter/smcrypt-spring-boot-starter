package com.houkunlin.smcrypt.handler;

import com.houkunlin.smcrypt.BouncyCastleSupport;
import com.houkunlin.smcrypt.TestContexts;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CipherHandlerRoundTripTest {
    private static final String SM4_KEY = "0123456789abcdeffedcba9876543210";
    private static final String AES_KEY = "00112233445566778899aabbccddeeff";
    private static final String DES_KEY = "0123456789abcdef";

    @Test
    void sm4RoundTripWithDefaultBase64() throws Exception {
        Sm4Handler handler = new Sm4Handler();
        handler.setContext(TestContexts.context(singleton("smcrypt.sm4.key", SM4_KEY)));
        String cipher = handler.getEncryptText("hello 世界");
        assertTrue(cipher.startsWith("SM4ENC(base64,"));
        assertEquals("hello 世界", handler.getDecryptText(cipher));
    }

    @Test
    void sm4RoundTripWithHexEncoding() throws Exception {
        Map<String, String> properties = singleton("smcrypt.sm4.key", SM4_KEY);
        properties.put("smcrypt.sm4.encoding", "hex");
        Sm4Handler handler = new Sm4Handler();
        handler.setContext(TestContexts.context(properties));
        String cipher = handler.getEncryptText("hello");
        assertTrue(cipher.startsWith("SM4ENC(hex,"));
        assertEquals("hello", handler.getDecryptText(cipher));
    }

    @Test
    void sm4DecryptAutoDetectWithoutEncodingPrefix() throws Exception {
        Sm4Handler handler = new Sm4Handler();
        handler.setContext(TestContexts.context(singleton("smcrypt.sm4.key", SM4_KEY)));
        Map<String, String> properties = singleton("smcrypt.sm4.key", SM4_KEY);
        properties.put("smcrypt.sm4.encoding", "hex");
        Sm4Handler hexHandler = new Sm4Handler();
        hexHandler.setContext(TestContexts.context(properties));
        String hexCipher = hexHandler.getEncryptText("abc").replace("SM4ENC(hex,", "SM4ENC(");
        assertEquals("abc", handler.getDecryptText(hexCipher));
    }

    @Test
    void aesRoundTrip() throws Exception {
        AesHandler handler = new AesHandler();
        handler.setContext(TestContexts.context(singleton("smcrypt.aes.key", AES_KEY)));
        String cipher = handler.getEncryptText("aes-data");
        assertEquals("aes-data", handler.getDecryptText(cipher));
    }

    @Test
    void desRoundTrip() throws Exception {
        DesHandler handler = new DesHandler();
        handler.setContext(TestContexts.context(singleton("smcrypt.des.key", DES_KEY)));
        String cipher = handler.getEncryptText("des-data");
        assertEquals("des-data", handler.getDecryptText(cipher));
    }

    @Test
    void rsaRoundTrip() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA", BouncyCastleSupport.provider()).generateKeyPair();
        RsaHandler handler = new RsaHandler();
        handler.setContext(TestContexts.context(singleton("smcrypt.rsa.key", privateKey(keyPair))));
        String cipher = handler.getEncryptText("rsa-data");
        assertEquals("rsa-data", handler.getDecryptText(cipher));
    }

    @Test
    void sm2RoundTrip() throws Exception {
        KeyPair keyPair = ecKeyPair("sm2p256v1");
        Sm2Handler handler = new Sm2Handler();
        handler.setContext(TestContexts.context(singleton("smcrypt.sm2.key", privateKey(keyPair))));
        String cipher = handler.getEncryptText("sm2-data");
        assertEquals("sm2-data", handler.getDecryptText(cipher));
    }

    @Test
    void eccRoundTrip() throws Exception {
        KeyPair keyPair = ecKeyPair("secp256r1");
        EccHandler handler = new EccHandler();
        handler.setContext(TestContexts.context(singleton("smcrypt.ecc.key", privateKey(keyPair))));
        String cipher = handler.getEncryptText("ecc-data");
        assertEquals("ecc-data", handler.getDecryptText(cipher));
    }

    private static KeyPair ecKeyPair(String curve) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC", BouncyCastleSupport.provider());
        generator.initialize(new ECGenParameterSpec(curve));
        return generator.generateKeyPair();
    }

    private static String privateKey(KeyPair keyPair) {
        return Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
    }

    private static Map<String, String> singleton(String key, String value) {
        Map<String, String> map = new HashMap<>();
        map.put(key, value);
        return map;
    }
}
