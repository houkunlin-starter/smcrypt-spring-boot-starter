package com.houkunlin.smcrypt.handler;

import com.houkunlin.smcrypt.BouncyCastleSupport;
import com.houkunlin.smcrypt.TestContexts;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.KeyGenerationParameters;
import org.bouncycastle.crypto.generators.SM9EncMasterKeyPairGenerator;
import org.bouncycastle.crypto.params.SM9EncMasterPrivateKeyParameters;
import org.bouncycastle.crypto.params.SM9EncMasterPublicKeyParameters;
import org.bouncycastle.crypto.params.SM9EncPrivateKeyParameters;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
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
    private static final String DESEDE_KEY_16 = "0123456789abcdeffedcba9876543210";
    private static final String DESEDE_KEY_24 = "0123456789abcdeffedcba98765432100123456789abcdef";

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
    void desEdeRoundTripWithTwoKey() throws Exception {
        DesEdeHandler handler = new DesEdeHandler();
        handler.setContext(TestContexts.context(singleton("smcrypt.desede.key", DESEDE_KEY_16)));
        String cipher = handler.getEncryptText("desede-2key");
        assertTrue(cipher.startsWith("DESEDEENC("));
        assertEquals("desede-2key", handler.getDecryptText(cipher));
    }

    @Test
    void desEdeRoundTripWithThreeKey() throws Exception {
        DesEdeHandler handler = new DesEdeHandler();
        handler.setContext(TestContexts.context(singleton("smcrypt.desede.key", DESEDE_KEY_24)));
        String cipher = handler.getEncryptText("desede-3key");
        assertEquals("desede-3key", handler.getDecryptText(cipher));
    }

    @Test
    void desEdeCbcRoundTrip() throws Exception {
        Map<String, String> properties = singleton("smcrypt.desede.key", DESEDE_KEY_24);
        properties.put("smcrypt.desede.mode", "CBC");
        properties.put("smcrypt.desede.iv", "0123456789abcdef");
        DesEdeHandler handler = new DesEdeHandler();
        handler.setContext(TestContexts.context(properties));
        String cipher = handler.getEncryptText("desede-cbc");
        assertEquals("desede-cbc", handler.getDecryptText(cipher));
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

    @Test
    void sm9RawSm4RoundTrip() throws Exception {
        assertSm9RoundTrip(null, null);
    }

    @Test
    void sm9RawStreamRoundTrip() throws Exception {
        assertSm9RoundTrip("STREAM", null);
    }

    @Test
    void sm9Asn1Sm4RoundTrip() throws Exception {
        assertSm9RoundTrip(null, "asn1");
    }

    @Test
    void sm9Asn1StreamRoundTrip() throws Exception {
        assertSm9RoundTrip("STREAM", "asn1");
    }

    private void assertSm9RoundTrip(String mode, String cipherFormat) throws Exception {
        Sm9Handler handler = new Sm9Handler();
        handler.setContext(TestContexts.context(sm9Properties(mode, cipherFormat)));
        String cipher = handler.getEncryptText("sm9-data");
        assertTrue(cipher.startsWith("SM9ENC("));
        assertEquals("sm9-data", handler.getDecryptText(cipher));
    }

    /**
     * 模拟 KGC：生成主密钥对并派生用户私钥，构造 SM9 配置。
     */
    private static Map<String, String> sm9Properties(String mode, String cipherFormat) {
        SM9EncMasterKeyPairGenerator generator = new SM9EncMasterKeyPairGenerator();
        generator.init(new KeyGenerationParameters(new SecureRandom(), 256));
        AsymmetricCipherKeyPair keyPair = generator.generateKeyPair();
        SM9EncMasterPrivateKeyParameters masterPrivate = (SM9EncMasterPrivateKeyParameters) keyPair.getPrivate();
        SM9EncMasterPublicKeyParameters masterPublic = masterPrivate.getPublicKeyParameters();
        byte[] identity = "alice".getBytes(StandardCharsets.UTF_8);
        SM9EncPrivateKeyParameters userKey = masterPrivate.generateUserKey(identity, SM9EncMasterPrivateKeyParameters.HID);

        Map<String, String> properties = new HashMap<>();
        properties.put("smcrypt.sm9.private-key", Base64.getEncoder().encodeToString(userKey.getEncoded()));
        properties.put("smcrypt.sm9.master-public-key", Base64.getEncoder().encodeToString(masterPublic.getEncoded()));
        properties.put("smcrypt.sm9.identity", "alice");
        if (mode != null) {
            properties.put("smcrypt.sm9.mode", mode);
        }
        if (cipherFormat != null) {
            properties.put("smcrypt.sm9.cipher-format", cipherFormat);
        }
        return properties;
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
