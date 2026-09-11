package com.houkunlin.smcrypt.key;

import com.houkunlin.smcrypt.SmCryptContext;
import com.houkunlin.smcrypt.TestContexts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SecretKeyResolverTest {

    @Test
    void resolveFromProperty() {
        SmCryptContext context = TestContexts.context(singleton("smcrypt.sm4.key", "deadbeef"));
        assertEquals("deadbeef", context.resolveKey("SM4"));
    }

    @Test
    void resolveFromPlainFile(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("sm4.key");
        Files.write(file, "deadbeef\n".getBytes(StandardCharsets.UTF_8));
        SmCryptContext context = TestContexts.context(singleton("smcrypt.sm4.file", file.toString()));
        assertEquals("deadbeef", context.resolveKey("SM4"));
    }

    @Test
    void resolveFromPropertiesFile(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("sm4.properties");
        Files.write(file, "key=deadbeef\n".getBytes(StandardCharsets.UTF_8));
        SmCryptContext context = TestContexts.context(singleton("smcrypt.sm4.file", file.toString()));
        assertEquals("deadbeef", context.resolveKey("SM4"));
    }

    @Test
    void resolveMissingReturnsNull() {
        SmCryptContext context = TestContexts.context(new HashMap<>());
        assertNull(context.resolveKey("SM4"));
    }

    private static Map<String, String> singleton(String key, String value) {
        Map<String, String> map = new HashMap<>();
        map.put(key, value);
        return map;
    }
}
