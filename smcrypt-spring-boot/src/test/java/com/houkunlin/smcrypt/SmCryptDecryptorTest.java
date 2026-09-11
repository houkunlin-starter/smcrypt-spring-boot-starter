package com.houkunlin.smcrypt;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SmCryptDecryptorTest {
    private static final String SM4_KEY = "0123456789abcdeffedcba9876543210";

    @Test
    void decryptsMapPropertySource() throws Exception {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("testSource", encryptedSource()));
        new SmCryptDecryptor().postProcessEnvironment(environment, new SpringApplication());
        assertEquals("hello", environment.getProperty("demo.value"));
    }

    @Test
    void decryptsOriginTrackedPropertySource() throws Exception {
        Map<String, Object> source = encryptedSource();
        source.put("demo.value", OriginTrackedValue.of(source.get("demo.value")));
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new OriginTrackedMapPropertySource("testOrigin", source));
        new SmCryptDecryptor().postProcessEnvironment(environment, new SpringApplication());
        assertEquals("hello", environment.getProperty("demo.value"));
    }

    private Map<String, Object> encryptedSource() throws Exception {
        Map<String, String> properties = new HashMap<>();
        properties.put("smcrypt.sm4.key", SM4_KEY);
        String cipher = new SmCryptEncryptor(TestContexts.context(properties)).encrypt("SM4", "hello");
        Map<String, Object> source = new HashMap<>();
        source.put("smcrypt.sm4.key", SM4_KEY);
        source.put("demo.value", cipher);
        return source;
    }
}
