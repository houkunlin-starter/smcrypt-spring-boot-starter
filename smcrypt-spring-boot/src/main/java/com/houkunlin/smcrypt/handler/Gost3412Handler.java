package com.houkunlin.smcrypt.handler;

/**
 * GOST R 34.12-2015（Kuznyechik）对称加密处理器。
 *
 * <p>密文格式：{@code GOST3412ENC(...)}；密钥为 32 字节（256 位），支持 hex 或 Base64 编码；
 * 分组长度 128 位，CBC 等模式需 16 字节 IV。</p>
 *
 * <p>俄罗斯地区标准，仅建议用于相关合规或兼容场景。</p>
 *
 * @author HouKunLin
 */
public class Gost3412Handler extends AbstractSymmetricCipherHandler {

    @Override
    public String algorithm() {
        return "GOST3412";
    }

    @Override
    protected String keyAlgorithm() {
        return jceAlgorithm();
    }

    @Override
    protected String jceAlgorithm() {
        return "GOST3412-2015";
    }

    @Override
    protected String defaultTransformation() {
        return "GOST3412-2015/ECB/PKCS5Padding";
    }
}
