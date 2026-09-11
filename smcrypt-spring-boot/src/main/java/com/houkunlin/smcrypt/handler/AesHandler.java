package com.houkunlin.smcrypt.handler;

/**
 * AES 对称加密处理器。
 *
 * <p>密文格式：{@code AESENC(...)}；密钥支持 16/24/32 字节，支持 hex 或 Base64 编码。</p>
 *
 * @author HouKunLin
 */
public class AesHandler extends AbstractSymmetricCipherHandler {

    @Override
    public String algorithm() {
        return "AES";
    }

    @Override
    protected String defaultTransformation() {
        return "AES/ECB/PKCS5Padding";
    }
}
