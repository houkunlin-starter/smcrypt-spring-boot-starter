package com.houkunlin.smcrypt.spi;

import com.houkunlin.smcrypt.SmCryptContext;
import com.houkunlin.smcrypt.TestContexts;
import com.houkunlin.smcrypt.handler.DecryptHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 SPI 注册的处理器损坏时不会中断处理器加载。
 *
 * <p>通过临时目录与独立 {@link URLClassLoader} 注入一个无法实例化的 provider，避免污染
 * 其它测试共享的 {@code META-INF/services} 注册文件。</p>
 */
class BrokenSpiHandlerTest {

    /**
     * provider 类不存在时 {@link java.util.ServiceLoader} 会抛出 {@link java.util.ServiceConfigurationError}
     * （{@link Error} 子类），加载器应捕获并跳过该来源，而不是中断启动。
     *
     * @param tempDir JUnit 提供的临时目录
     * @throws IOException 写入临时 services 文件失败时抛出
     */
    @Test
    void brokenSpiProviderDoesNotInterruptLoading(@TempDir Path tempDir) throws IOException {
        Path services = tempDir.resolve("META-INF/services/com.houkunlin.smcrypt.handler.DecryptHandler");
        Files.createDirectories(services.getParent());
        Files.write(services, "com.houkunlin.smcrypt.spi.MissingHandler\n".getBytes(StandardCharsets.UTF_8));

        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader brokenLoader = new URLClassLoader(new URL[]{tempDir.toUri().toURL()}, original)) {
            Thread.currentThread().setContextClassLoader(brokenLoader);
            SmCryptContext context = TestContexts.context(new HashMap<String, String>());
            List<DecryptHandler> handlers = new CipherHandlerLoader().load(context);
            Set<String> algorithms = handlers.stream().map(DecryptHandler::algorithm).collect(Collectors.toSet());
            assertTrue(algorithms.contains("SM4"), "损坏的 SPI provider 不应导致内置处理器缺失");
            assertTrue(algorithms.contains("AES"), "损坏的 SPI provider 不应导致内置处理器缺失");
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }
}
