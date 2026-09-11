package com.houkunlin.smcrypt.key;

import com.houkunlin.smcrypt.SmCryptContext;
import com.houkunlin.smcrypt.TestContexts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.FileSystemResourceLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
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
    void systemPropertyOverridesConfigData() {
        String property = "smcrypt.sm4.key";
        String original = System.getProperty(property);
        try {
            // 模拟 -Dsmcrypt.sm4.key=deadbeef
            System.setProperty(property, "deadbeef");
            // 模拟 application.yml 中配置了另一个密钥（低优先级属性源）
            StandardEnvironment environment = new StandardEnvironment();
            environment.getPropertySources().addLast(new MapPropertySource("applicationConfig",
                    Collections.singletonMap(property, "cafebabe")));
            SmCryptContext context = new SmCryptContext(environment::getProperty, new FileSystemResourceLoader());
            // Spring 的 systemProperties 优先级高于 config data，应取到 -D 的值
            assertEquals("deadbeef", context.resolveKey("SM4"));
        } finally {
            if (original == null) {
                System.clearProperty(property);
            } else {
                System.setProperty(property, original);
            }
        }
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
    void resolveFromPropertiesFileWithUtf8Value(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("sm4.properties");
        Files.write(file, "key=口令密码\n".getBytes(StandardCharsets.UTF_8));
        SmCryptContext context = TestContexts.context(singleton("smcrypt.sm4.file", file.toString()));
        assertEquals("口令密码", context.resolveKey("SM4"));
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
