package com.houkunlin.smcrypt.spi;

import com.houkunlin.smcrypt.SmCryptContext;
import com.houkunlin.smcrypt.TestContexts;
import com.houkunlin.smcrypt.handler.DecryptHandler;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CipherHandlerLoaderTest {

    @Test
    void loadsBuiltinAndSpiHandlers() {
        List<DecryptHandler> handlers = load();
        Set<String> algorithms = handlers.stream().map(DecryptHandler::algorithm).collect(Collectors.toSet());
        assertTrue(algorithms.containsAll(Arrays.asList("SM4", "SM2", "SM9", "AES", "DES", "DESEDE",
                "CHACHA20", "GOST3412", "DSTU7624", "RC6", "CAMELLIA", "ARIA", "SEED", "RSA", "ECC",
                "PBE", "JASYPT")));
        assertTrue(algorithms.contains("DEMO"), "应加载 META-INF/services 注册的处理器");
        assertTrue(algorithms.contains("FACTORY"), "应加载 spring.factories 注册的处理器");
    }

    @Test
    void serviceHandlerRoundTrip() throws Exception {
        DecryptHandler handler = find(load(), "DEMO");
        assertNotNull(handler);
        assertEquals("custom", handler.getDecryptText(handler.getEncryptText("custom")));
    }

    @Test
    void springFactoriesHandlerRoundTrip() throws Exception {
        DecryptHandler handler = find(load(), "FACTORY");
        assertNotNull(handler);
        assertEquals("custom", handler.getDecryptText(handler.getEncryptText("custom")));
    }

    @Test
    void addHandlerIfPresentSkipsMissingClass() {
        List<DecryptHandler> handlers = new ArrayList<>();
        assertDoesNotThrow(() -> new CipherHandlerLoader()
                .addHandlerIfPresent(handlers, "com.example.NotExistHandler", "测试"));
        assertTrue(handlers.isEmpty(), "缺少依赖类时应跳过，不应加入处理器");
    }

    @Test
    void addHandlerIfPresentLoadsExistingClass() {
        List<DecryptHandler> handlers = new ArrayList<>();
        new CipherHandlerLoader().addHandlerIfPresent(handlers,
                "com.houkunlin.smcrypt.handler.Sm4Handler", "SM4");
        assertEquals(1, handlers.size());
        assertEquals("SM4", handlers.get(0).algorithm());
    }

    private List<DecryptHandler> load() {
        SmCryptContext context = TestContexts.context(new HashMap<String, String>());
        return new CipherHandlerLoader().load(context);
    }

    private DecryptHandler find(List<DecryptHandler> handlers, String algorithm) {
        for (DecryptHandler handler : handlers) {
            if (algorithm.equals(handler.algorithm())) {
                return handler;
            }
        }
        return null;
    }
}
