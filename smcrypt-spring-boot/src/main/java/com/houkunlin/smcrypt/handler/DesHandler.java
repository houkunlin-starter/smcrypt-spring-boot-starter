package com.houkunlin.smcrypt.handler;

/**
 * DES 对称加密处理器。
 *
 * <p>密文格式：{@code DESENC(...)}；密钥为 8 字节，支持 hex 或 Base64 编码。</p>
 *
 * @author HouKunLin
 */
public class DesHandler extends AbstractSymmetricCipherHandler {

    @Override
    public String algorithm() {
        return "DES";
    }

    @Override
    protected String defaultTransformation() {
        return "DES/ECB/PKCS5Padding";
    }
}
