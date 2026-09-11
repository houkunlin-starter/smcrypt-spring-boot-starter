package com.houkunlin.smcrypt;

import com.houkunlin.smcrypt.handler.DecryptHandler;
import com.houkunlin.smcrypt.spi.CipherHandlerLoader;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 密文加密工具。
 *
 * <p>用于在运维或测试阶段生成配置文件中所需的密文。加密能力复用各算法处理器，
 * 配置与密钥来源通过 {@link SmCryptContext} 提供。</p>
 *
 * <p>示例：</p>
 * <pre>{@code
 * System.setProperty("smcrypt.sm4.key", "0123456789abcdeffedcba9876543210");
 * SmCryptEncryptor encryptor = new SmCryptEncryptor(
 *         new SmCryptContext(System::getProperty, new FileSystemResourceLoader()));
 * String cipherText = encryptor.encrypt("SM4", "hello");
 * // SM4ENC(base64,....)
 * }</pre>
 *
 * @author HouKunLin
 */
public class SmCryptEncryptor {
    private final Map<String, DecryptHandler> handlers;

    public SmCryptEncryptor(SmCryptContext context) {
        List<DecryptHandler> loaded = new CipherHandlerLoader().load(context);
        this.handlers = new LinkedHashMap<>();
        for (DecryptHandler handler : loaded) {
            handlers.put(handler.algorithm().toUpperCase(), handler);
        }
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
        DecryptHandler handler = handlers.get(algorithm.toUpperCase());
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
