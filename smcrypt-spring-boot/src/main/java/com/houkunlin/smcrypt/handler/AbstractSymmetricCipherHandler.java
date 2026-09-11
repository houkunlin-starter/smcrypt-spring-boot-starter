package com.houkunlin.smcrypt.handler;

import com.houkunlin.smcrypt.BouncyCastleSupport;
import com.houkunlin.smcrypt.config.CipherConfig;
import com.houkunlin.smcrypt.config.MacAlgorithm;
import org.bouncycastle.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;

/**
 * 对称加密处理器抽象基类。
 *
 * <p>基于 JCE {@link Cipher} 实现，密钥通过 {@link #resolveSymmetricKey()} 获取，
 * 支持通过配置项指定变换串、模式、填充与初始向量。</p>
 *
 * <p>可选启用 encrypt-then-MAC 完整性校验：配置 {@code smcrypt.<算法>.mac} 后，加密输出为
 * {@code 密文 || MAC}，解密前先校验 MAC，校验失败抛出异常。MAC 密钥默认复用加密密钥，
 * 可通过 {@code smcrypt.<算法>.mac-key} 单独配置。</p>
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
        byte[] actualCipherBytes = cipherBytes;
        if (config.hasMac()) {
            actualCipherBytes = verifyMac(cipherBytes, config);
        }
        Cipher cipher = Cipher.getInstance(config.transformation(), BouncyCastleSupport.provider());
        initCipher(cipher, Cipher.DECRYPT_MODE, config);
        return cipher.doFinal(actualCipherBytes);
    }

    @Override
    protected byte[] doEncrypt(byte[] plainBytes, CipherConfig config) throws Exception {
        Cipher cipher = Cipher.getInstance(config.transformation(), BouncyCastleSupport.provider());
        initCipher(cipher, Cipher.ENCRYPT_MODE, config);
        byte[] cipherBytes = cipher.doFinal(plainBytes);
        if (!config.hasMac()) {
            return cipherBytes;
        }
        byte[] mac = computeMac(cipherBytes, config);
        return Arrays.concatenate(cipherBytes, mac);
    }

    /**
     * 校验密文尾部的 MAC，并返回去除 MAC 后的实际密文
     *
     * @param payload 密文载荷（{@code 密文 || MAC}）
     * @param config  算法配置
     * @return 去除 MAC 后的实际密文
     * @throws Exception MAC 缺失或校验失败时抛出
     */
    private byte[] verifyMac(byte[] payload, CipherConfig config) throws Exception {
        MacAlgorithm macAlgorithm = config.macAlgorithm();
        int macLength = macAlgorithm.macLength();
        if (payload.length <= macLength) {
            throw new IllegalArgumentException("密文长度不足，无法包含 " + macAlgorithm.jceName() + " 校验值");
        }
        int cipherLength = payload.length - macLength;
        byte[] cipherBytes = Arrays.copyOfRange(payload, 0, cipherLength);
        byte[] actualMac = Arrays.copyOfRange(payload, cipherLength, payload.length);
        byte[] expectedMac = computeMac(cipherBytes, config);
        if (!MessageDigest.isEqual(actualMac, expectedMac)) {
            throw new SecurityException("密文完整性校验失败（" + macAlgorithm.jceName() + " 校验值不匹配），密文可能已被篡改");
        }
        return cipherBytes;
    }

    /**
     * 对密文计算 MAC
     *
     * @param cipherBytes 密文
     * @param config      算法配置
     * @return MAC 字节
     * @throws Exception 计算失败时抛出
     */
    private byte[] computeMac(byte[] cipherBytes, CipherConfig config) throws Exception {
        MacAlgorithm macAlgorithm = config.macAlgorithm();
        byte[] key = config.macKey() != null ? config.macKey() : resolveSymmetricKey();
        Mac mac = Mac.getInstance(macAlgorithm.jceName(), BouncyCastleSupport.provider());
        mac.init(new SecretKeySpec(key, macAlgorithm.jceName()));
        return mac.doFinal(cipherBytes);
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
