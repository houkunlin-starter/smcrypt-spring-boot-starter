package com.houkunlin.smcrypt.handler;

/**
 * SM4 国密对称加密处理器。
 *
 * <p>密文格式：{@code SM4ENC(...)}；密钥为 16 字节，支持 hex 或 Base64 编码。</p>
 *
 * @author HouKunLin
 */
public class Sm4Handler extends AbstractSymmetricCipherHandler {

    @Override
    public String algorithm() {
        return "SM4";
    }

    @Override
    protected String defaultTransformation() {
        return "SM4/ECB/PKCS5Padding";
    }
}
