package com.houkunlin.smcrypt.handler;

/**
 * ChaCha20-Poly1305 对称加密处理器（AEAD）。
 *
 * <p>密文格式：{@code CHACHA20ENC(...)}；密钥为 32 字节（256 位），支持 hex 或 Base64 编码。</p>
 *
 * <p>ChaCha20-Poly1305 为 AEAD（自带完整性校验），不支持 {@code mode} / {@code padding}；
 * 必须配置 12 字节 nonce（通过 {@code smcrypt.chacha20.iv}），且每次加密不得复用。</p>
 *
 * @author HouKunLin
 */
public class ChaCha20Handler extends AbstractSymmetricCipherHandler {

    @Override
    public String algorithm() {
        return "CHACHA20";
    }

    @Override
    protected String keyAlgorithm() {
        return "ChaCha20";
    }

    @Override
    protected String defaultTransformation() {
        return "ChaCha20-Poly1305";
    }
}
