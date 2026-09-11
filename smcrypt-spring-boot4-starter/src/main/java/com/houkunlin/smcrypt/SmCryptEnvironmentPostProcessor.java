package com.houkunlin.smcrypt;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Spring Boot 4.x 配置文件密文解密适配器。
 *
 * <p>实现 Spring Boot 4.x 的 {@link EnvironmentPostProcessor}（已从 {@code org.springframework.boot.env}
 * 迁移至 {@code org.springframework.boot} 包），将解密逻辑委托给版本无关的 {@link SmCryptDecryptor}。</p>
 *
 * @author HouKunLin
 */
public class SmCryptEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    /**
     * 版本无关的解密引擎
     */
    private final SmCryptDecryptor delegate = new SmCryptDecryptor();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        delegate.postProcessEnvironment(environment, application);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 10;
    }
}
