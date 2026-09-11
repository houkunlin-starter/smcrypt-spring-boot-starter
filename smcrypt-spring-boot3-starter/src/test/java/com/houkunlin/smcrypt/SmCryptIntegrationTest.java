package com.houkunlin.smcrypt;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.FileSystemResourceLoader;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @SpringBootApplication
    static class TestConfig {
    }
}
