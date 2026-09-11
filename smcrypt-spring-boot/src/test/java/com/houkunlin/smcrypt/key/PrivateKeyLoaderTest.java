package com.houkunlin.smcrypt.key;

import com.houkunlin.smcrypt.BouncyCastleSupport;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcePEMEncryptorBuilder;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class PrivateKeyLoaderTest {

    @Test
    void loadsPkcs8PemRsa() throws Exception {
        KeyPair keyPair = rsaKeyPair();
        String pem = toPkcs8Pem(keyPair.getPrivate());
        assertTrue(pem.contains("BEGIN PRIVATE KEY"));
        PrivateKey loaded = PrivateKeyLoader.load(pem, "RSA");
        PublicKey publicKey = PrivateKeyLoader.derivePublicKey(loaded, "RSA");
        assertNotNull(publicKey);
    }

    @Test
    void loadsPkcs1PemRsa() throws Exception {
        KeyPair keyPair = rsaKeyPair();
        String pem = toPem(keyPair.getPrivate());
        assertTrue(pem.contains("BEGIN RSA PRIVATE KEY"));
        PrivateKey loaded = PrivateKeyLoader.load(pem, "RSA");
        assertNotNull(loaded);
        assertNotNull(PrivateKeyLoader.derivePublicKey(loaded, "RSA"));
    }

    @Test
    void loadsPkcs8PemEc() throws Exception {
        KeyPair keyPair = ecKeyPair("secp256r1");
        PrivateKey loaded = PrivateKeyLoader.load(toPkcs8Pem(keyPair.getPrivate()), "ECC");
        assertNotNull(loaded);
        assertNotNull(PrivateKeyLoader.derivePublicKey(loaded, "ECC"));
    }

    @Test
    void loadsSec1PemEc() throws Exception {
        KeyPair keyPair = ecKeyPair("secp256r1");
        String pem = toPem(keyPair.getPrivate());
        assertTrue(pem.contains("BEGIN EC PRIVATE KEY"));
        PrivateKey loaded = PrivateKeyLoader.load(pem, "ECC");
        assertNotNull(loaded);
        assertNotNull(PrivateKeyLoader.derivePublicKey(loaded, "ECC"));
    }

    @Test
    void loadsPkcs8DerBase64Rsa() throws Exception {
        KeyPair keyPair = rsaKeyPair();
        String base64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        PrivateKey loaded = PrivateKeyLoader.load(base64, "RSA");
        assertNotNull(loaded);
        assertNotNull(PrivateKeyLoader.derivePublicKey(loaded, "RSA"));
    }

    @Test
    void rejectsEncryptedPem() throws Exception {
        KeyPair keyPair = rsaKeyPair();
        StringWriter writer = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(writer)) {
            pemWriter.writeObject(keyPair.getPrivate(), new JcePEMEncryptorBuilder("AES-256-CBC")
                    .setProvider(BouncyCastleSupport.provider())
                    .build("password".toCharArray()));
        }
        String pem = writer.toString();
        assertTrue(pem.contains("ENCRYPTED"));
        assertThrows(IllegalArgumentException.class, () -> PrivateKeyLoader.load(pem, "RSA"));
    }

    @Test
    void rejectsEmptyContent() {
        assertThrows(IllegalArgumentException.class, () -> PrivateKeyLoader.load("  ", "RSA"));
    }

    @Test
    void rejectsUnrecognizedPem() {
        assertThrows(IllegalArgumentException.class,
                () -> PrivateKeyLoader.load("-----BEGIN PRIVATE KEY-----\nnot-base64\n-----END PRIVATE KEY-----", "RSA"));
    }

    private static KeyPair rsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", BouncyCastleSupport.provider());
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static KeyPair ecKeyPair(String curve) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC", BouncyCastleSupport.provider());
        generator.initialize(new ECGenParameterSpec(curve));
        return generator.generateKeyPair();
    }

    /**
     * 由 JCA 私钥写出 PEM（RSA 为 PKCS#1、EC 为 SEC1）
     */
    private static String toPem(PrivateKey privateKey) throws Exception {
        StringWriter writer = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(writer)) {
            pemWriter.writeObject(privateKey);
        }
        return writer.toString();
    }

    /**
     * 由私钥的 PKCS#8 编码构造 PEM
     */
    private static String toPkcs8Pem(PrivateKey privateKey) {
        String base64 = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(privateKey.getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + base64 + "\n-----END PRIVATE KEY-----\n";
    }
}
