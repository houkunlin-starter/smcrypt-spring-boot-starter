package com.houkunlin.smcrypt.handler;

/**
 * 3DES（Triple DES / DESede）对称加密处理器。
 *
 * <p>密文格式：{@code DESEDEENC(...)}；密钥支持 16 字节（2-key，K1=K3）或 24 字节（3-key），
 * 支持 hex 或 Base64 编码。</p>
 *
 * <p>3DES 属于过时算法，仅建议用于兼容遗留系统密文；新系统请优先使用 AES 或 SM4。</p>
 *
 * @author HouKunLin
 */
public class DesEdeHandler extends AbstractSymmetricCipherHandler {

    @Override
    public String algorithm() {
        return "DESEDE";
    }

    @Override
    protected String keyAlgorithm() {
        return "DESede";
    }

    @Override
    protected String jceAlgorithm() {
        return "DESede";
    }

    @Override
    protected String defaultTransformation() {
        return "DESede/ECB/PKCS5Padding";
    }
}
