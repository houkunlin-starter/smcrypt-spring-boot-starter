package com.houkunlin.smcrypt;

import com.houkunlin.smcrypt.codec.CipherEncoding;
import com.houkunlin.smcrypt.config.CipherConfig;
import com.houkunlin.smcrypt.key.SecretKeyResolver;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.ResourceLoader;

/**
 * 加解密上下文。
 *
 * <p>聚合属性查询与密钥解析能力，供各算法处理器共享使用。核心逻辑通过
 * {@link PropertyLookup} 与 Spring 解耦：Spring 场景由 {@link Environment} 适配，
 * 命令行场景由系统属性适配。</p>
 *
 * @author HouKunLin
 */
public class SmCryptContext {
    /**
     * 属性查询接口
     */
    private final PropertyLookup properties;
    /**
     * 密钥解析器
     */
    private final SecretKeyResolver secretKeyResolver;

    /**
     * 构造加解密上下文
     *
     * @param properties     属性查询接口
     * @param resourceLoader 密钥文件资源加载器
     */
    public SmCryptContext(PropertyLookup properties, ResourceLoader resourceLoader) {
        this.properties = properties;
        this.secretKeyResolver = new SecretKeyResolver(properties, resourceLoader);
    }

    /**
     * 基于 Spring 环境构建上下文
     *
     * @param environment Spring 环境
     * @return 上下文实例
     */
    public static SmCryptContext fromEnvironment(Environment environment) {
        return new SmCryptContext(environment::getProperty, new FileSystemResourceLoader());
    }

    /**
     * 查询属性值
     *
     * @param key 属性键
     * @return 属性值；不存在时返回 null
     */
    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * 解析指定算法的密钥内容
     *
     * @param algorithm 算法名称
     * @return 密钥内容；未找到时返回 null
     */
    public String resolveKey(String algorithm) {
        return secretKeyResolver.resolve(algorithm);
    }

    /**
     * 解析指定算法的配置
     *
     * @param algorithm             算法名称
     * @param defaultTransformation 默认变换串
     * @param defaultEncoding       默认加密输出编码
     * @return 算法配置
     */
    public CipherConfig resolveConfig(String algorithm, String defaultTransformation, CipherEncoding defaultEncoding) {
        return CipherConfig.resolve(algorithm, properties, defaultTransformation, defaultEncoding);
    }
}
