package com.houkunlin.smcrypt;

/**
 * 属性查询接口。
 *
 * <p>用于隔离配置来源：Spring 环境可通过 {@code environment::getProperty} 适配，
 * 命令行工具可通过系统属性或 {@link java.util.Map} 适配，从而使核心加解密逻辑
 * 不直接依赖 Spring 容器。</p>
 *
 * @author HouKunLin
 */
@FunctionalInterface
public interface PropertyLookup {

    /**
     * 查询指定键对应的属性值
     *
     * @param key 属性键
     * @return 属性值；不存在时返回 null
     */
    String getProperty(String key);
}
