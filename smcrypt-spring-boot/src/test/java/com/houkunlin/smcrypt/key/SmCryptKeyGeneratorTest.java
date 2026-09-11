package com.houkunlin.smcrypt.key;

import com.houkunlin.smcrypt.TestContexts;
import com.houkunlin.smcrypt.handler.*;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SmCryptKeyGeneratorTest {

    @Test
    void generateSymmetricKeyLengths() {
        assertEquals(32, SmCryptKeyGenerator.generateSymmetricKey("SM4", 128).length());
        assertEquals(32, SmCryptKeyGenerator.generateSymmetricKey("AES", 128).length());
        assertEquals(48, SmCryptKeyGenerator.generateSymmetricKey("AES", 192).length());
        assertEquals(64, SmCryptKeyGenerator.generateSymmetricKey("AES", 256).length());
        assertEquals(16, SmCryptKeyGenerator.generateSymmetricKey("DES", 56).length());
        assertEquals(32, SmCryptKeyGenerator.generateSymmetricKey("DESEDE", 112).length());
        assertEquals(48, SmCryptKeyGenerator.generateSymmetricKey("DESEDE", 168).length());
    }

    @Test
    void generatedSymmetricKeysRoundTrip() throws Exception {
        assertSymmetricRoundTrip(new Sm4Handler(), "SM4", 128);
        assertSymmetricRoundTrip(new AesHandler(), "AES", 256);
        assertSymmetricRoundTrip(new DesHandler(), "DES", 56);
        assertSymmetricRoundTrip(new DesEdeHandler(), "DESEDE", 168);
    }

    @Test
    void generatedRsaKeyPairRoundTrip() throws Exception {
        KeyPair keyPair = SmCryptKeyGenerator.generateKeyPair("RSA", 2048);
        RsaHandler handler = new RsaHandler();
        handler.setContext(TestContexts.context(Collections.singletonMap("smcrypt.rsa.key",
                SmCryptKeyGenerator.toPrivateKeyPem(keyPair.getPrivate()))));
        String cipher = handler.getEncryptText("rsa-data");
        assertEquals("rsa-data", handler.getDecryptText(cipher));
    }

    @Test
    void generatedSm2KeyPairRoundTrip() throws Exception {
        KeyPair keyPair = SmCryptKeyGenerator.generateKeyPair("SM2", 0);
        Sm2Handler handler = new Sm2Handler();
        handler.setContext(TestContexts.context(Collections.singletonMap("smcrypt.sm2.key",
                SmCryptKeyGenerator.toPrivateKeyPem(keyPair.getPrivate()))));
        String cipher = handler.getEncryptText("sm2-data");
        assertEquals("sm2-data", handler.getDecryptText(cipher));
    }

    @Test
    void generatedEccKeyPairRoundTrip() throws Exception {
        KeyPair keyPair = SmCryptKeyGenerator.generateKeyPair("ECC", 384);
        EccHandler handler = new EccHandler();
        handler.setContext(TestContexts.context(Collections.singletonMap("smcrypt.ecc.key",
                SmCryptKeyGenerator.toPrivateKeyPem(keyPair.getPrivate()))));
        String cipher = handler.getEncryptText("ecc-data");
        assertEquals("ecc-data", handler.getDecryptText(cipher));
    }

    @Test
    void generatedSm9KeysRoundTrip() throws Exception {
        byte[] identity = "alice".getBytes(StandardCharsets.UTF_8);
        Sm9KeyMaterial material = SmCryptKeyGenerator.generateSm9Key(identity, (byte) 0x03);
        Map<String, String> properties = new HashMap<>();
        properties.put("smcrypt.sm9.private-key", material.userPrivateKey());
        properties.put("smcrypt.sm9.master-public-key", material.masterPublicKey());
        properties.put("smcrypt.sm9.identity", "alice");
        Sm9Handler handler = new Sm9Handler();
        handler.setContext(TestContexts.context(properties));
        String cipher = handler.getEncryptText("sm9-data");
        assertEquals("sm9-data", handler.getDecryptText(cipher));
    }

    private void assertSymmetricRoundTrip(com.houkunlin.smcrypt.handler.AbstractCipherHandler handler,
                                          String algorithm, int keySizeBits) throws Exception {
        String key = SmCryptKeyGenerator.generateSymmetricKey(algorithm, keySizeBits);
        Map<String, String> properties = Collections.singletonMap(
                "smcrypt." + algorithm.toLowerCase() + ".key", key);
        handler.setContext(TestContexts.context(properties));
        String cipher = handler.getEncryptText("symmetric-data");
        assertEquals("symmetric-data", handler.getDecryptText(cipher));
    }
}
