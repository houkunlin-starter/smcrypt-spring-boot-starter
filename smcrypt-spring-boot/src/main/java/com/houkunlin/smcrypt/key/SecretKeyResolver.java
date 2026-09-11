package com.houkunlin.smcrypt.key;

import com.houkunlin.smcrypt.PropertyLookup;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * 密钥解析器。
 *
 * <p>按算法从多种来源依次查找密钥内容，查找顺序：</p>
 * <ol>
 *     <li>属性 {@code smcrypt.<alg>.key}（{@link #properties}，Spring 场景下为 {@code Environment}）；</li>
 *     <li>JVM 参数 {@code -Dsmcrypt.<alg>.key=...}；</li>
 *     <li>环境变量 {@code SMCRYPT_<ALG>_KEY}；</li>
 *     <li>密钥文件 {@code smcrypt.<alg>.file}（属性、JVM 参数或环境变量指定，支持 {@code file:} / {@code classpath:}）；</li>
 *     <li>默认密钥文件 {@code smcrypt-<alg>.key} / {@code smcrypt-<alg>.properties}（文件系统与 classpath）。</li>
 * </ol>
 *
 * <p>注意第 1 步的顺序设计：Spring 场景下 {@link #properties} 即 {@code Environment::getProperty}，
 * 而 Spring 的 {@code commandLineArgs} / {@code systemProperties} / {@code systemEnvironment} 属性源
 * 优先级均高于配置文件，因此命令行参数、{@code -D} 参数、环境变量可以覆盖
 * {@code application.yml} / {@code application.properties} 中已配置的密钥。显式的
 * {@code System.getProperty} / {@code System.getenv} 查找（第 2、3 步）仅用于 {@code SmCryptCli}
 * 等非 Spring 场景兜底，不应调整到第 1 步之前，否则会破坏 Spring 的属性覆盖约定。</p>
 *
 * <p>密钥文件为 {@code .properties} 时读取 {@code key} 或 {@code secret_key} 属性；
 * 其余文件将整体内容（去除首尾空白）作为密钥。</p>
 *
 * @author HouKunLin
 */
public class SecretKeyResolver {
    private final PropertyLookup properties;
    private final ResourceLoader resourceLoader;

    public SecretKeyResolver(PropertyLookup properties, ResourceLoader resourceLoader) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }

    /**
     * 解析指定算法的密钥内容
     *
     * @param algorithm 算法名称，如 {@code SM4}
     * @return 密钥内容；未找到时返回 null
     */
    public String resolve(String algorithm) {
        String lower = algorithm.toLowerCase();
        String upper = algorithm.toUpperCase();

        String key = firstNonBlank(
                properties.getProperty("smcrypt." + lower + ".key"),
                System.getProperty("smcrypt." + lower + ".key"),
                System.getenv("SMCRYPT_" + upper + "_KEY"));
        if (key != null) {
            return key;
        }

        String file = firstNonBlank(
                properties.getProperty("smcrypt." + lower + ".file"),
                System.getProperty("smcrypt." + lower + ".file"),
                System.getenv("SMCRYPT_" + upper + "_FILE"));
        if (file != null) {
            String content = readResource(file);
            if (content != null) {
                return content;
            }
        }

        String[] defaults = {"smcrypt-" + lower + ".key", "smcrypt-" + lower + ".properties"};
        for (String name : defaults) {
            String content = readResource(name);
            if (content == null) {
                content = readResource("classpath:" + name);
            }
            if (content != null) {
                return content;
            }
        }
        return null;
    }

    private String readResource(String location) {
        try {
            Resource resource = resourceLoader.getResource(location);
            if (!resource.exists()) {
                return null;
            }
            try (InputStream inputStream = resource.getInputStream()) {
                if (location.toLowerCase().endsWith(".properties")) {
                    Properties properties = new Properties();
                    properties.load(inputStream);
                    String value = properties.getProperty("key");
                    if (value == null) {
                        value = properties.getProperty("secret_key");
                    }
                    return blankToNull(value);
                }
                String text = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
                return blankToNull(text);
            }
        } catch (IOException e) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            String result = blankToNull(value);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
