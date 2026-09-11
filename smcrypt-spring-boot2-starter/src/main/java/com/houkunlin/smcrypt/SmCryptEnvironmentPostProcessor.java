package com.houkunlin.smcrypt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Spring Boot 2.x 配置文件密文解密适配器。
 *
 * <p>实现 Spring Boot 2.x 的 {@link EnvironmentPostProcessor}（{@code org.springframework.boot.env} 包），
 * 将解密逻辑委托给版本无关的 {@link SmCryptDecryptor}。</p>
 *
 * @author HouKunLin
 */
public class SmCryptEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
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
