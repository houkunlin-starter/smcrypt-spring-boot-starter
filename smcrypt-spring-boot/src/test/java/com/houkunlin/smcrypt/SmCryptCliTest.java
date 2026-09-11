package com.houkunlin.smcrypt;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmCryptCliTest {

    @Test
    void generatesAesKey() {
        String output = run("--generate-key", "--algorithm", "AES", "--key-length", "256");
        assertEquals(64, output.trim().length());
    }

    @Test
    void generatesRsaPrivateKeyPem() {
        String output = run("--generate-key", "--algorithm", "RSA", "--key-length", "2048");
        assertTrue(output.contains("PRIVATE KEY"));
    }

    @Test
    void generatesSm9Configuration() {
        String output = run("--generate-key", "--algorithm", "SM9", "--identity", "alice@example.com");
        assertTrue(output.contains("smcrypt.sm9.private-key="));
        assertTrue(output.contains("smcrypt.sm9.master-public-key="));
        assertTrue(output.contains("smcrypt.sm9.identity=alice@example.com"));
    }

    @Test
    void pbeRoundTrip() {
        String cipher = run("--algorithm", "PBE", "--password", "pbe-pw", "--pbe-mode", "KDF",
                "--transformation", "AES/GCM/NoPadding", "--text", "pbe-hello");
        String plain = run("--algorithm", "PBE", "--password", "pbe-pw", "--pbe-mode", "KDF",
                "--transformation", "AES/GCM/NoPadding", "--decrypt", "--text", cipher.trim());
        assertEquals("pbe-hello", plain.trim());
    }

    @Test
    void pbeModeOptionIgnoresGenericMode() {
        String cipher = run("--algorithm", "PBE", "--password", "pbe-pw", "--mode", "JCE",
                "--transformation", "AES/GCM/NoPadding", "--text", "pbe-mode");
        String plain = run("--algorithm", "PBE", "--password", "pbe-pw", "--pbe-mode", "KDF",
                "--transformation", "AES/GCM/NoPadding", "--decrypt", "--text", cipher.trim());
        assertEquals("pbe-mode", plain.trim());
    }

    @Test
    void readsKeyFromStdin() {
        String key = "0123456789abcdeffedcba9876543210";
        String property = "smcrypt.sm4.key";
        String originalProperty = System.getProperty(property);
        InputStream originalIn = System.in;
        try {
            System.setIn(new ByteArrayInputStream((key + System.lineSeparator()).getBytes(StandardCharsets.UTF_8)));
            String cipher = run("--algorithm", "SM4", "--key", "-", "--text", "stdin-key");
            String plain = run("--algorithm", "SM4", "--key", key, "--decrypt", "--text", cipher.trim());
            assertEquals("stdin-key", plain.trim());
        } finally {
            System.setIn(originalIn);
            if (originalProperty == null) {
                System.clearProperty(property);
            } else {
                System.setProperty(property, originalProperty);
            }
        }
    }

    private static String run(String... args) {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer));
        try {
            SmCryptCli.main(args);
        } finally {
            System.setOut(original);
        }
        return buffer.toString();
    }
}
