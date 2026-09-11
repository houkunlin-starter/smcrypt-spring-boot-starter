package com.houkunlin.smcrypt.spi;

import com.houkunlin.smcrypt.SmCryptContext;
import com.houkunlin.smcrypt.TestContexts;
import com.houkunlin.smcrypt.handler.DecryptHandler;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
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
