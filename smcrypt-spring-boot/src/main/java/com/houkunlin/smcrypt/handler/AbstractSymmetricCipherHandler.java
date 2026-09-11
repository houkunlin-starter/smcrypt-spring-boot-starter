package com.houkunlin.smcrypt.handler;

import com.houkunlin.smcrypt.BouncyCastleSupport;
import com.houkunlin.smcrypt.config.CipherConfig;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 对称加密处理器抽象基类。
 *
 * <p>基于 JCE {@link Cipher} 实现，密钥通过 {@link #resolveSymmetricKey()} 获取，
 * 支持通过配置项指定变换串、模式、填充与初始向量。</p>
 *
 * @author HouKunLin
 */
public abstract class AbstractSymmetricCipherHandler extends AbstractCipherHandler {

    /**
     * 密钥算法名称（JCE 规范名）
     *
     * <p>默认与 {@link #algorithm()} 相同；当密文前缀与 JCE 密钥算法名不一致时（例如 3DES 的前缀
     * 为 {@code DESEDE}、而密钥算法名为 {@code DESede}），由子类覆写。</p>
     *
     * @return 用于构造 {@link SecretKeySpec} 的密钥算法名
     */
    protected String keyAlgorithm() {
        return algorithm();
    }

    @Override
    protected byte[] doDecrypt(byte[] cipherBytes, CipherConfig config) throws Exception {
        Cipher cipher = Cipher.getInstance(config.transformation(), BouncyCastleSupport.provider());
        initCipher(cipher, Cipher.DECRYPT_MODE, config);
        return cipher.doFinal(cipherBytes);
    }

    @Override
    protected byte[] doEncrypt(byte[] plainBytes, CipherConfig config) throws Exception {
        Cipher cipher = Cipher.getInstance(config.transformation(), BouncyCastleSupport.provider());
        initCipher(cipher, Cipher.ENCRYPT_MODE, config);
        return cipher.doFinal(plainBytes);
    }

    /**
     * 初始化 JCE Cipher 的密钥与初始向量
     *
     * @param cipher JCE Cipher
     * @param mode   加解密模式（{@link Cipher#ENCRYPT_MODE} / {@link Cipher#DECRYPT_MODE}）
     * @param config 算法配置
     * @throws Exception 初始化失败时抛出
     */
    private void initCipher(Cipher cipher, int mode, CipherConfig config) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(resolveSymmetricKey(), keyAlgorithm());
        if (config.hasIv()) {
            cipher.init(mode, keySpec, new IvParameterSpec(config.iv()));
        } else {
            cipher.init(mode, keySpec);
        }
    }
}
