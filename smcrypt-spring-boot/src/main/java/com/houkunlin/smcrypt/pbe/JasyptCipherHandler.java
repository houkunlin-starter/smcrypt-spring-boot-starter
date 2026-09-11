package com.houkunlin.smcrypt.pbe;

import com.houkunlin.smcrypt.config.CipherConfig;

/**
 * Jasypt 兼容处理器。
 *
 * <p>解密由 Jasypt 生成的密文：其 {@code ENC(xxx)} 内层为 {@code base64(salt ‖ iv ‖ cipher)}，
 * 迁移到本项目只需改为 {@code JASYPTENC(xxx)}（内层保持不变），使用同一口令即可解密。</p>
 *
 * <p>配置前缀 {@code smcrypt.jasypt.}：{@code password}、{@code transformation}（默认
 * {@code PBEWITHHMACSHA512ANDAES_256}）、{@code iterations}（默认 1000）、{@code salt-size}、
 * {@code iv-size}、{@code provider}、{@code encoding}。</p>
 *
 * <p>仅当配置了 {@code smcrypt.jasypt.password} 时才建议使用。</p>
 *
 * @author HouKunLin
 */
public class JasyptCipherHandler extends AbstractPbeCipherHandler {
    /**
     * 属性前缀
     */
    private static final String PREFIX = "smcrypt.jasypt.";
    /**
     * Jasypt Spring Boot 默认算法
     */
    private static final String DEFAULT_TRANSFORMATION = "PBEWITHHMACSHA512ANDAES_256";

    @Override
    public String algorithm() {
        return "JASYPT";
    }

    @Override
    protected String defaultTransformation() {
        return DEFAULT_TRANSFORMATION;
    }

    @Override
    protected String propertyPrefix() {
        return PREFIX;
    }

    @Override
    protected int defaultSaltSize() {
        return 16;
    }

    @Override
    protected int defaultIvSize() {
        return 16;
    }

    @Override
    protected String defaultPbeTransformation() {
        return DEFAULT_TRANSFORMATION;
    }

    @Override
    protected byte[] doDecrypt(byte[] payload, CipherConfig config) throws Exception {
        return jceDecrypt(payload);
    }

    @Override
    protected byte[] doEncrypt(byte[] plainBytes, CipherConfig config) throws Exception {
        return jceEncrypt(plainBytes);
    }
}
