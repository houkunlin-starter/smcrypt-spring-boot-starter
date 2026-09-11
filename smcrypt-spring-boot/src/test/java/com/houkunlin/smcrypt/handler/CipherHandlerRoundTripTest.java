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

import static org.junit.jupiter.api.Assertions.*;

class CipherHandlerRoundTripTest {
    private static final String SM4_KEY = "0123456789abcdeffedcba9876543210";
    private static final String AES_KEY = "00112233445566778899aabbccddeeff";
    private static final String DES_KEY = "0123456789abcdef";
    private static final String DESEDE_KEY_16 = "0123456789abcdeffedcba9876543210";
    private static final String DESEDE_KEY_24 = "0123456789abcdeffedcba98765432100123456789abcdef";
    private static final String KEY_256 = "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff";
    private static final String CHACHA20_IV = "00112233445566778899aabb";

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
    void sm4PlainKeyEncodingKeepsWhitespace() throws Exception {
        Map<String, String> properties = singleton("smcrypt.sm4.key", "my secret key 12");
        properties.put("smcrypt.sm4.key-encoding", "plain");
        Sm4Handler handler = new Sm4Handler();
        handler.setContext(TestContexts.context(properties));
        String cipher = handler.getEncryptText("plain-key");
        assertEquals("plain-key", handler.getDecryptText(cipher));
    }

    @Test
    void aesMacKeyExplicitHexEncoding() throws Exception {
        Map<String, String> properties = singleton("smcrypt.aes.key", AES_KEY);
        properties.put("smcrypt.aes.mac", "HmacSHA256");
        properties.put("smcrypt.aes.mac-key", "aa bb cc dd ee ff 00 11 22 33 44 55 66 77 88 99");
        properties.put("smcrypt.aes.mac-key-encoding", "hex");
        AesHandler handler = new AesHandler();
        handler.setContext(TestContexts.context(properties));
        String cipher = handler.getEncryptText("mac-hex-key");
        assertEquals("mac-hex-key", handler.getDecryptText(cipher));
    }

    @Test
    void invalidKeyLengthGivesClearMessage() {
        Sm4Handler handler = new Sm4Handler();
        handler.setContext(TestContexts.context(singleton("smcrypt.sm4.key", "00112233")));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> handler.getEncryptText("bad-key"));
        assertTrue(error.getMessage().contains("SM4"));
    }

    @Test
    void aesGcmRoundTrip() throws Exception {
        Map<String, String> properties = singleton("smcrypt.aes.key", AES_KEY);
        properties.put("smcrypt.aes.transformation", "AES/GCM/NoPadding");
        properties.put("smcrypt.aes.iv", "00112233445566778899aabb");
        AesHandler handler = new AesHandler();
        handler.setContext(TestContexts.context(properties));
        String cipher = handler.getEncryptText("aes-gcm-data");
        assertEquals("aes-gcm-data", handler.getDecryptText(cipher));
    }

    @Test
    void sm4MacRoundTrip() throws Exception {
        Map<String, String> properties = singleton("smcrypt.sm4.key", SM4_KEY);
        properties.put("smcrypt.sm4.mac", "HmacSM3");
        Sm4Handler handler = new Sm4Handler();
        handler.setContext(TestContexts.context(properties));
        String cipher = handler.getEncryptText("sm4-mac-data");
        assertEquals("sm4-mac-data", handler.getDecryptText(cipher));
    }

    @Test
    void aesMacWithSeparateMacKeyRoundTrip() throws Exception {
        Map<String, String> properties = singleton("smcrypt.aes.key", AES_KEY);
        properties.put("smcrypt.aes.mac", "HmacSHA256");
        properties.put("smcrypt.aes.mac-key", "aabbccddeeff00112233445566778899");
        AesHandler handler = new AesHandler();
        handler.setContext(TestContexts.context(properties));
        String cipher = handler.getEncryptText("aes-mac-data");
        assertEquals("aes-mac-data", handler.getDecryptText(cipher));
    }

    @Test
    void macDetectsTamperedCipherText() throws Exception {
        Map<String, String> properties = singleton("smcrypt.sm4.key", SM4_KEY);
        properties.put("smcrypt.sm4.mac", "HmacSM3");
        Sm4Handler handler = new Sm4Handler();
        handler.setContext(TestContexts.context(properties));
        String cipher = handler.getEncryptText("tamper-data");
        assertThrows(SecurityException.class, () -> handler.getDecryptText(tamperBase64Payload(cipher)));
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
    void chacha20RoundTrip() throws Exception {
        Map<String, String> properties = singleton("smcrypt.chacha20.key", KEY_256);
        properties.put("smcrypt.chacha20.iv", CHACHA20_IV);
        ChaCha20Handler handler = new ChaCha20Handler();
        handler.setContext(TestContexts.context(properties));
        String cipher = handler.getEncryptText("chacha20-data");
        assertEquals("chacha20-data", handler.getDecryptText(cipher));
    }

    @Test
    void chacha20DetectsTamperedCipherText() throws Exception {
        Map<String, String> properties = singleton("smcrypt.chacha20.key", KEY_256);
        properties.put("smcrypt.chacha20.iv", CHACHA20_IV);
        ChaCha20Handler handler = new ChaCha20Handler();
        handler.setContext(TestContexts.context(properties));
        String cipher = handler.getEncryptText("chacha20-tamper");
        assertThrows(Exception.class, () -> handler.getDecryptText(tamperBase64Payload(cipher)));
    }

    @Test
    void gost3412RoundTrip() throws Exception {
        Gost3412Handler handler = new Gost3412Handler();
        handler.setContext(TestContexts.context(singleton("smcrypt.gost3412.key", KEY_256)));
        String cipher = handler.getEncryptText("gost-data");
        assertEquals("gost-data", handler.getDecryptText(cipher));
    }

    @Test
    void dstu7624RoundTrip() throws Exception {
        Dstu7624Handler handler = new Dstu7624Handler();
        handler.setContext(TestContexts.context(singleton("smcrypt.dstu7624.key", KEY_256)));
        String cipher = handler.getEncryptText("dstu-data");
        assertEquals("dstu-data", handler.getDecryptText(cipher));
    }

    @Test
    void rc6RoundTrip() throws Exception {
        Rc6Handler handler = new Rc6Handler();
        handler.setContext(TestContexts.context(singleton("smcrypt.rc6.key", KEY_256)));
        String cipher = handler.getEncryptText("rc6-data");
        assertEquals("rc6-data", handler.getDecryptText(cipher));
    }

    @Test
    void camelliaRoundTrip() throws Exception {
        CamelliaHandler handler = new CamelliaHandler();
        handler.setContext(TestContexts.context(singleton("smcrypt.camellia.key", KEY_256)));
        String cipher = handler.getEncryptText("camellia-data");
        assertEquals("camellia-data", handler.getDecryptText(cipher));
    }

    @Test
    void ariaRoundTrip() throws Exception {
        AriaHandler handler = new AriaHandler();
        handler.setContext(TestContexts.context(singleton("smcrypt.aria.key", KEY_256)));
        String cipher = handler.getEncryptText("aria-data");
        assertEquals("aria-data", handler.getDecryptText(cipher));
    }

    @Test
    void seedRoundTrip() throws Exception {
        SeedHandler handler = new SeedHandler();
        handler.setContext(TestContexts.context(singleton("smcrypt.seed.key", AES_KEY)));
        String cipher = handler.getEncryptText("seed-data");
        assertEquals("seed-data", handler.getDecryptText(cipher));
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
    void sm2DetectsTamperedCipherText() throws Exception {
        KeyPair keyPair = ecKeyPair("sm2p256v1");
        Sm2Handler handler = new Sm2Handler();
        handler.setContext(TestContexts.context(singleton("smcrypt.sm2.key", privateKey(keyPair))));
        String cipher = handler.getEncryptText("sm2-tamper");
        assertThrows(Exception.class, () -> handler.getDecryptText(tamperBase64Payload(cipher)));
    }

    @Test
    void eccDetectsTamperedCipherText() throws Exception {
        KeyPair keyPair = ecKeyPair("secp256r1");
        EccHandler handler = new EccHandler();
        handler.setContext(TestContexts.context(singleton("smcrypt.ecc.key", privateKey(keyPair))));
        String cipher = handler.getEncryptText("ecc-tamper");
        assertThrows(Exception.class, () -> handler.getDecryptText(tamperBase64Payload(cipher)));
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

    /**
     * 翻转密文载荷末尾字节，模拟被篡改的密文（仅适用于 base64 编码的密文）。
     */
    private static String tamperBase64Payload(String cipherText) {
        int comma = cipherText.indexOf(',');
        String prefix = cipherText.substring(0, comma + 1);
        String body = cipherText.substring(comma + 1, cipherText.length() - 1);
        byte[] bytes = Base64.getDecoder().decode(body);
        bytes[bytes.length - 1] ^= 0x01;
        return prefix + Base64.getEncoder().encodeToString(bytes) + ")";
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
