package com.houkunlin.smcrypt.handler;

/**
 * Camellia 对称加密处理器。
 *
 * <p>密文格式：{@code CAMELLIAENC(...)}；密钥支持 16 / 24 / 32 字节（128 / 192 / 256 位），
 * 支持 hex 或 Base64 编码；分组长度 128 位，CBC 等模式需 16 字节 IV。</p>
 *
 * <p>日本 / ISO 地区标准，安全性良好。</p>
 *
 * @author HouKunLin
 */
public class CamelliaHandler extends AbstractSymmetricCipherHandler {

    @Override
    public String algorithm() {
        return "CAMELLIA";
    }

    @Override
    protected String defaultTransformation() {
        return "Camellia/ECB/PKCS5Padding";
    }
}
