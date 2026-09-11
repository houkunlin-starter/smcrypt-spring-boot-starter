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

    private void initCipher(Cipher cipher, int mode, CipherConfig config) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(resolveSymmetricKey(), algorithm());
        if (config.hasIv()) {
            cipher.init(mode, keySpec, new IvParameterSpec(config.iv()));
        } else {
            cipher.init(mode, keySpec);
        }
    }
}
