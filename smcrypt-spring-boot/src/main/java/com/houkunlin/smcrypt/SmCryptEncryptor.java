package com.houkunlin.smcrypt;

import com.houkunlin.smcrypt.handler.DecryptHandler;
import com.houkunlin.smcrypt.spi.CipherHandlerLoader;

import java.util.Locale;
import java.util.Map;

/**
 * 密文加密工具。
 *
 * <p>用于在运维或测试阶段生成配置文件中所需的密文。加密能力复用各算法处理器，
 * 配置与密钥来源通过 {@link SmCryptContext} 提供。</p>
 *
 * <p>示例：</p>
 * <pre>{@code
 * Map<String, String> properties = new HashMap<>();
 * properties.put("smcrypt.sm4.key", "0123456789abcdeffedcba9876543210");
 * SmCryptEncryptor encryptor = new SmCryptEncryptor(
 *         new SmCryptContext(properties::get, new FileSystemResourceLoader()));
 * String cipherText = encryptor.encrypt("SM4", "hello");
 * // SM4ENC(base64,....)
 * }</pre>
 *
 * <p><b>线程安全：</b>本类内部处理器会缓存解析出的算法配置与密钥，因此**非线程安全**，
 * 请勿在多线程间共享同一实例；如需并发使用，请为每个线程创建独立实例。</p>
 *
 * @author HouKunLin
 */
public class SmCryptEncryptor {
    /**
     * 算法名称（大写）到密文处理器的映射
     */
    private final Map<String, DecryptHandler> handlers;

    /**
     * 构造加密工具
     *
     * @param context 加解密上下文
     */
    public SmCryptEncryptor(SmCryptContext context) {
        this.handlers = new CipherHandlerLoader().loadMap(context);
    }

    /**
     * 加密明文
     *
     * @param algorithm 算法名称，如 {@code SM4}
     * @param plainText 明文
     * @return 完整密文串，如 {@code SM4ENC(base64,xxxx)}
     * @throws Exception 加密失败时抛出
     */
    public String encrypt(String algorithm, String plainText) throws Exception {
        DecryptHandler handler = handlers.get(algorithm.toUpperCase(Locale.ROOT));
        if (handler == null) {
            throw new IllegalArgumentException("不支持的算法：" + algorithm);
        }
        return handler.getEncryptText(plainText);
    }

    /**
     * 解密完整密文串
     *
     * @param cipherText 完整密文串
     * @return 明文
     * @throws Exception 解密失败或无法识别密文时抛出
     */
    public String decrypt(String cipherText) throws Exception {
        for (DecryptHandler handler : handlers.values()) {
            if (handler.support(cipherText)) {
                return handler.getDecryptText(cipherText);
            }
        }
        throw new IllegalArgumentException("无法识别的密文：" + cipherText);
    }
}
