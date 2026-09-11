package com.houkunlin.smcrypt.handler;

/**
 * 密文处理器 SPI。
 *
 * <p>每种加密算法对应一个处理器，负责识别 {@code {算法}ENC(...)} 形式的密文并完成加解密。
 * 业务系统可自行实现本接口并注册（通过 {@code META-INF/services} 或 {@code spring.factories}），
 * 以接入自定义算法或加密机等外部解密能力；当算法名称与内置处理器冲突时，业务实现优先。</p>
 *
 * <p>自定义处理器若需要访问 Spring 环境或密钥配置，可额外实现 {@link DecryptHandlerAware}。</p>
 *
 * @author HouKunLin
 */
public interface DecryptHandler {

    /**
     * 算法名称，同时作为密文前缀（{@code 算法名ENC(...)}）
     *
     * @return 算法名称，如 {@code SM4}
     */
    String algorithm();

    /**
     * 判断给定配置值是否为本处理器可处理的密文
     *
     * @param propValue 配置值
     * @return 可处理返回 true
     */
    boolean support(String propValue);

    /**
     * 从完整密文串中提取括号内的密文内容
     *
     * @param propValue 完整密文串，如 {@code SM4ENC(base64,xxxx)}
     * @return 括号内的密文内容
     */
    String getCipherText(String propValue);

    /**
     * 解密配置值
     *
     * @param propValue 完整密文串
     * @return 解密后的明文
     * @throws Exception 解密失败时抛出
     */
    String getDecryptText(String propValue) throws Exception;

    /**
     * 加密明文，生成完整密文串
     *
     * @param plainText 明文
     * @return 完整密文串，如 {@code SM4ENC(base64,xxxx)}
     * @throws Exception 加密失败时抛出
     */
    String getEncryptText(String plainText) throws Exception;
}
