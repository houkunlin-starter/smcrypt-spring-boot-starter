package com.houkunlin.smcrypt.key;

import com.houkunlin.smcrypt.PropertyLookup;
import com.houkunlin.smcrypt.SmCryptLog;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
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
    /**
     * 属性键前缀
     */
    private static final String PROPERTY_PREFIX = "smcrypt.";
    /**
     * 环境变量前缀
     */
    private static final String ENV_PREFIX = "SMCRYPT_";
    /**
     * 默认密钥文件前缀
     */
    private static final String DEFAULT_FILE_PREFIX = "smcrypt-";
    /**
     * 密钥文件后缀
     */
    private static final String KEY_SUFFIX = ".key";
    /**
     * 密钥文件配置项后缀
     */
    private static final String FILE_SUFFIX = ".file";
    /**
     * properties 文件后缀
     */
    private static final String PROPERTIES_SUFFIX = ".properties";
    /**
     * classpath 资源前缀
     */
    private static final String CLASSPATH_PREFIX = "classpath:";

    /**
     * 属性查询接口（Spring 场景下为 Environment）
     */
    private final PropertyLookup properties;
    /**
     * 密钥文件资源加载器
     */
    private final ResourceLoader resourceLoader;

    /**
     * 构造密钥解析器
     *
     * @param properties     属性查询接口
     * @param resourceLoader 密钥文件资源加载器
     */
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
        String lower = algorithm.toLowerCase(Locale.ROOT);
        String upper = algorithm.toUpperCase(Locale.ROOT);

        String key = firstNonBlank(
                properties.getProperty(PROPERTY_PREFIX + lower + KEY_SUFFIX),
                System.getProperty(PROPERTY_PREFIX + lower + KEY_SUFFIX),
                System.getenv(ENV_PREFIX + upper + "_KEY"));
        if (key != null) {
            return key;
        }

        String file = firstNonBlank(
                properties.getProperty(PROPERTY_PREFIX + lower + FILE_SUFFIX),
                System.getProperty(PROPERTY_PREFIX + lower + FILE_SUFFIX),
                System.getenv(ENV_PREFIX + upper + "_FILE"));
        if (file != null) {
            String content = readResource(file);
            if (content != null) {
                return content;
            }
        }

        String[] defaults = {
                DEFAULT_FILE_PREFIX + lower + KEY_SUFFIX,
                DEFAULT_FILE_PREFIX + lower + PROPERTIES_SUFFIX};
        for (String name : defaults) {
            String content = readResource(name);
            if (content == null) {
                content = readResource(CLASSPATH_PREFIX + name);
            }
            if (content != null) {
                return content;
            }
        }
        return null;
    }

    /**
     * 从指定位置读取密钥内容
     *
     * <p>资源不存在时返回 null；{@code .properties} 文件读取 {@code key} 或 {@code secret_key} 属性，
     * 其余文件整体内容（去除首尾空白）作为密钥。读取失败时记录警告并返回 null。</p>
     *
     * @param location 资源位置（支持 {@code file:} / {@code classpath:} 前缀）
     * @return 密钥内容；资源不存在或读取失败时返回 null
     */
    private String readResource(String location) {
        try {
            Resource resource = resourceLoader.getResource(location);
            if (!resource.exists()) {
                return null;
            }
            try (InputStream inputStream = resource.getInputStream()) {
                if (location.toLowerCase(Locale.ROOT).endsWith(PROPERTIES_SUFFIX)) {
                    Properties keyProperties = new Properties();
                    // 显式按 UTF-8 读取，避免 Properties.load(InputStream) 的 ISO-8859-1 解码导致非 ASCII 口令乱码
                    keyProperties.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                    String value = keyProperties.getProperty("key");
                    if (value == null) {
                        value = keyProperties.getProperty("secret_key");
                    }
                    return blankToNull(value);
                }
                String text = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
                return blankToNull(text);
            }
        } catch (IOException e) {
            SmCryptLog.warn("读取密钥文件失败，将忽略该来源继续查找其它来源，文件：{}", location, e);
            return null;
        }
    }

    /**
     * 返回参数列表中第一个非空白值
     *
     * @param values 候选值
     * @return 第一个非空白值；全部为空时返回 null
     */
    private static String firstNonBlank(String... values) {
        for (String value : values) {
            String result = blankToNull(value);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    /**
     * 去除首尾空白，空值返回 null
     *
     * @param value 原始值
     * @return 去除空白后的值；为 null 或空白时返回 null
     */
    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
