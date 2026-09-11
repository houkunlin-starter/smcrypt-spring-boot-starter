package com.houkunlin.smcrypt.handler;

/**
 * RC6 对称加密处理器。
 *
 * <p>密文格式：{@code RC6ENC(...)}；密钥支持 16 / 24 / 32 字节（128 / 192 / 256 位），
 * 支持 hex 或 Base64 编码；分组长度 128 位，CBC 等模式需 16 字节 IV。</p>
 *
 * <p>RC6 为 AES 候选算法，安全性良好但实际使用较少。</p>
 *
 * @author HouKunLin
 */
public class Rc6Handler extends AbstractSymmetricCipherHandler {

    @Override
    public String algorithm() {
        return "RC6";
    }

    @Override
    protected String defaultTransformation() {
        return "RC6/ECB/PKCS5Padding";
    }
}
