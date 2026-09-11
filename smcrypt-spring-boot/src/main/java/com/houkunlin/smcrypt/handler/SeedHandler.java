package com.houkunlin.smcrypt.handler;

/**
 * SEED 对称加密处理器。
 *
 * <p>密文格式：{@code SEEDENC(...)}；密钥为 16 字节（128 位），支持 hex 或 Base64 编码；
 * 分组长度 128 位，CBC 等模式需 16 字节 IV。</p>
 *
 * <p>韩国地区标准，安全性良好。</p>
 *
 * @author HouKunLin
 */
public class SeedHandler extends AbstractSymmetricCipherHandler {

    @Override
    public String algorithm() {
        return "SEED";
    }

    @Override
    protected String defaultTransformation() {
        return "SEED/ECB/PKCS5Padding";
    }
}
