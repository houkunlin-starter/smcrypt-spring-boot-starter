package com.houkunlin.smcrypt.handler;

/**
 * DSTU 7624:2014（Kalyna）对称加密处理器。
 *
 * <p>密文格式：{@code DSTU7624ENC(...)}；密钥支持 16 / 32 / 64 字节（128 / 256 / 512 位），
 * 支持 hex 或 Base64 编码；分组长度 128 位，CBC 等模式需 16 字节 IV。</p>
 *
 * <p>乌克兰地区标准，仅建议用于相关合规或兼容场景。</p>
 *
 * @author HouKunLin
 */
public class Dstu7624Handler extends AbstractSymmetricCipherHandler {

    @Override
    public String algorithm() {
        return "DSTU7624";
    }

    @Override
    protected String defaultTransformation() {
        return "DSTU7624/ECB/PKCS5Padding";
    }
}
