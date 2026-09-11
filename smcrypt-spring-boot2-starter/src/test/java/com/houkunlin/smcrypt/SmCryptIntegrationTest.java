package com.houkunlin.smcrypt;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.FileSystemResourceLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SmCryptIntegrationTest {
    private static final String SM4_KEY = "0123456789abcdeffedcba9876543210";

    @Test
    void decryptsConfigurationProperty() throws Exception {
        Map<String, String> properties = new HashMap<>();
        properties.put("smcrypt.sm4.key", SM4_KEY);
        SmCryptContext cryptContext = new SmCryptContext(properties::get, new FileSystemResourceLoader());
        String cipher = new SmCryptEncryptor(cryptContext).encrypt("SM4", "hello");

        SpringApplication application = new SpringApplication(TestConfig.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("demo.value", cipher);
        defaults.put("logging.file.path", "build/test-logs");
        application.setDefaultProperties(defaults);

        try (ConfigurableApplicationContext context = application.run("--smcrypt.sm4.key=" + SM4_KEY)) {
            assertEquals("hello", context.getEnvironment().getProperty("demo.value"));
        }
    }

    @Test
    void writesEarlyLogFileWhenCipherTextPresent() throws Exception {
        Path logDir = Paths.get("build/test-logs");
        Path logFile = logDir.resolve("smcrypt-it.smcrypt.log");
        Files.deleteIfExists(logFile);

        Map<String, String> properties = new HashMap<>();
        properties.put("smcrypt.sm4.key", SM4_KEY);
        SmCryptContext cryptContext = new SmCryptContext(properties::get, new FileSystemResourceLoader());
        String cipher = new SmCryptEncryptor(cryptContext).encrypt("SM4", "hello");

        SpringApplication application = new SpringApplication(TestConfig.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("spring.application.name", "smcrypt-it");
        defaults.put("logging.file.path", logDir.toString());
        defaults.put("demo.value", cipher);
        application.setDefaultProperties(defaults);

        try (ConfigurableApplicationContext ignored = application.run("--smcrypt.sm4.key=" + SM4_KEY)) {
            // 启动即完成解密并写出早期日志
        }

        assertTrue(Files.exists(logFile), "存在密文时应生成早期日志文件：" + logFile.toAbsolutePath());
        String content = new String(Files.readAllBytes(logFile), StandardCharsets.UTF_8);
        assertTrue(content.contains("[SMCRYPT]"), "早期日志文件应包含 [SMCRYPT] 日志");
    }

    @Test
    void doesNotWriteEarlyLogFileWithoutCipherText() throws Exception {
        Path logDir = Paths.get("build/test-logs");
        Path logFile = logDir.resolve("smcrypt-no-cipher.smcrypt.log");
        Files.deleteIfExists(logFile);

        SpringApplication application = new SpringApplication(TestConfig.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("spring.application.name", "smcrypt-no-cipher");
        defaults.put("logging.file.path", logDir.toString());
        application.setDefaultProperties(defaults);

        try (ConfigurableApplicationContext ignored = application.run()) {
            // 无密文，不应生成早期日志文件
        }

        assertFalse(Files.exists(logFile), "无密文时不应生成早期日志文件：" + logFile.toAbsolutePath());
    }

    @SpringBootApplication
    static class TestConfig {
    }
}
