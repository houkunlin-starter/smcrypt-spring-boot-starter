package com.houkunlin.smcrypt;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.boot.origin.TextResourceOrigin;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SmCryptDecryptorTest {
    private static final String SM4_KEY = "0123456789abcdeffedcba9876543210";
    private static final String CAMELLIA_KEY = "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff";

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

    @Test
    void failFastThrowsWhenKeyMissing() throws Exception {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("testSource", missingKeySource("true")));
        assertThrows(IllegalStateException.class,
                () -> new SmCryptDecryptor().postProcessEnvironment(environment, new SpringApplication()));
    }

    @Test
    void defaultSkipsWhenKeyMissing() throws Exception {
        StandardEnvironment environment = new StandardEnvironment();
        Map<String, Object> source = missingKeySource(null);
        environment.getPropertySources().addFirst(new MapPropertySource("testSource", source));
        assertDoesNotThrow(() -> new SmCryptDecryptor().postProcessEnvironment(environment, new SpringApplication()));
        assertEquals(source.get("demo.value"), environment.getProperty("demo.value"));
    }

    @Test
    void preservesOriginWhenDecryptingOriginTrackedSource() throws Exception {
        Map<String, Object> encrypted = encryptedSource();
        TextResourceOrigin origin = new TextResourceOrigin(new ClassPathResource("application.yml"), null);
        encrypted.put("demo.value", OriginTrackedValue.of(encrypted.get("demo.value"), origin));

        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new OriginTrackedMapPropertySource("testOrigin", encrypted));
        new SmCryptDecryptor().postProcessEnvironment(environment, new SpringApplication());

        PropertySource<?> replaced = environment.getPropertySources().get("testOrigin");
        assertNotNull(replaced);
        assertTrue(replaced instanceof OriginTrackedMapPropertySource);
        Origin resultOrigin = ((OriginTrackedMapPropertySource) replaced).getOrigin("demo.value");
        assertSame(origin, resultOrigin);
    }

    @Test
    void ignoresNonMapPropertySource() {
        StandardEnvironment environment = new StandardEnvironment();
        CustomPropertySource custom = new CustomPropertySource("custom", "SM4ENC(base64,xxxxxxxx)");
        environment.getPropertySources().addFirst(custom);
        assertDoesNotThrow(() -> new SmCryptDecryptor().postProcessEnvironment(environment, new SpringApplication()));
        assertSame(custom, environment.getPropertySources().get("custom"));
    }

    /**
     * 非 {@link MapPropertySource} 的自定义属性源，用于验证解密引擎会跳过非 Map 类型来源。
     */
    private static final class CustomPropertySource extends PropertySource<String> {
        private CustomPropertySource(String name, String value) {
            super(name, value);
        }

        @Override
        public Object getProperty(String name) {
            return null;
        }
    }

    @Test
    void strictFailThrowsOnWeakConfig() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("testSource", weakConfigSource("fail")));
        assertThrows(IllegalStateException.class,
                () -> new SmCryptDecryptor().postProcessEnvironment(environment, new SpringApplication()));
    }

    @Test
    void strictWarnDoesNotThrowOnWeakConfig() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("testSource", weakConfigSource("warn")));
        assertDoesNotThrow(() -> new SmCryptDecryptor().postProcessEnvironment(environment, new SpringApplication()));
    }

    @Test
    void strictWarnDoesNotThrowOnInvalidConfig() {
        StandardEnvironment environment = new StandardEnvironment();
        Map<String, Object> source = new HashMap<>();
        source.put("smcrypt.sm4.key", SM4_KEY);
        source.put("smcrypt.sm4.mac", "bogus");
        source.put("smcrypt.strict", "warn");
        environment.getPropertySources().addFirst(new MapPropertySource("testSource", source));
        assertDoesNotThrow(() -> new SmCryptDecryptor().postProcessEnvironment(environment, new SpringApplication()));
    }

    @Test
    void strictOffSkipsCheck() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("testSource", weakConfigSource("off")));
        assertDoesNotThrow(() -> new SmCryptDecryptor().postProcessEnvironment(environment, new SpringApplication()));
    }

    private Map<String, Object> weakConfigSource(String strict) {
        Map<String, Object> source = new HashMap<>();
        // SM4 未配置 transformation/mode，默认使用 ECB，属于弱配置
        source.put("smcrypt.sm4.key", SM4_KEY);
        source.put("smcrypt.strict", strict);
        return source;
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

    /**
     * 构造一个包含密文但不含对应密钥的属性源，用于验证密钥缺失时的行为。
     *
     * @param failFast {@code smcrypt.fail-fast} 的取值；为 null 时不写入该配置
     */
    private Map<String, Object> missingKeySource(String failFast) throws Exception {
        Map<String, String> keyProperties = new HashMap<>();
        keyProperties.put("smcrypt.camellia.key", CAMELLIA_KEY);
        String cipher = new SmCryptEncryptor(TestContexts.context(keyProperties)).encrypt("CAMELLIA", "secret");
        Map<String, Object> source = new HashMap<>();
        source.put("demo.value", cipher);
        if (failFast != null) {
            source.put("smcrypt.fail-fast", failFast);
        }
        return source;
    }
}
