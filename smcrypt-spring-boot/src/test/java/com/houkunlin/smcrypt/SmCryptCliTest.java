package com.houkunlin.smcrypt;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

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
