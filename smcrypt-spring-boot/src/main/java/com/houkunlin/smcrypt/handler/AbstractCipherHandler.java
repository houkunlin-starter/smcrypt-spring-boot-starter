package com.houkunlin.smcrypt.handler;

import com.houkunlin.smcrypt.SmCryptContext;
import com.houkunlin.smcrypt.codec.CipherEncoding;
import com.houkunlin.smcrypt.codec.CipherPayload;
import com.houkunlin.smcrypt.codec.EncodingDetector;
import com.houkunlin.smcrypt.config.CipherConfig;
import com.houkunlin.smcrypt.key.KeyCodec;

import java.nio.charset.StandardCharsets;

/**
 * 密文处理器抽象基类。
 *
 * <p>统一实现密文识别、编码解析、配置解析、密钥获取与结果编码等公共逻辑，
 * 子类只需关注具体算法的字节级加解密实现。</p>
 *
 * @author HouKunLin
 */
public abstract class AbstractCipherHandler implements DecryptHandler, DecryptHandlerAware {
    /**
     * 密文包裹后缀（算法名之后），形如 {@code SM4ENC(}
     */
    private static final String WRAPPER_SUFFIX = "ENC(";
    /**
     * 密文包裹结束符
     */
    private static final String WRAPPER_END = ")";

    /**
     * 加解密上下文，由加载器注入
     */
    private SmCryptContext context;

    /**
     * 默认 JCE 变换串
     *
     * @return 默认变换串
     */
    protected abstract String defaultTransformation();

    /**
     * 加密输出默认编码
     *
     * @return 默认编码，默认 Base64
     */
    protected CipherEncoding defaultEncoding() {
        return CipherEncoding.BASE64;
    }

    /**
     * 执行字节级解密
     *
     * @param cipherBytes 密文字节
     * @param config      算法配置
     * @return 明文字节
     * @throws Exception 解密失败时抛出
     */
    protected abstract byte[] doDecrypt(byte[] cipherBytes, CipherConfig config) throws Exception;

    /**
     * 执行字节级加密
     *
     * @param plainBytes 明文字节
     * @param config     算法配置
     * @return 密文字节
     * @throws Exception 加密失败时抛出
     */
    protected abstract byte[] doEncrypt(byte[] plainBytes, CipherConfig config) throws Exception;

    @Override
    public void setContext(SmCryptContext context) {
        this.context = context;
    }

    /**
     * 获取加解密上下文
     *
     * @return 上下文
     */
    protected SmCryptContext context() {
        if (context == null) {
            throw new IllegalStateException("处理器 " + algorithm() + " 尚未注入 SmCryptContext");
        }
        return context;
    }

    @Override
    public boolean support(String propValue) {
        if (propValue == null) {
            return false;
        }
        String prefix = algorithm() + WRAPPER_SUFFIX;
        return propValue.length() > prefix.length()
                && propValue.regionMatches(true, 0, prefix, 0, prefix.length())
                && propValue.endsWith(WRAPPER_END);
    }

    @Override
    public String getCipherText(String propValue) {
        String prefix = algorithm() + WRAPPER_SUFFIX;
        return propValue.substring(prefix.length(), propValue.length() - WRAPPER_END.length());
    }

    @Override
    public String getDecryptText(String propValue) throws Exception {
        CipherPayload payload = EncodingDetector.detect(getCipherText(propValue));
        CipherConfig config = resolveConfig();
        byte[] plainBytes = doDecrypt(payload.decode(), config);
        return new String(plainBytes, StandardCharsets.UTF_8);
    }

    @Override
    public String getEncryptText(String plainText) throws Exception {
        CipherConfig config = resolveConfig();
        byte[] cipherBytes = doEncrypt(plainText.getBytes(StandardCharsets.UTF_8), config);
        return algorithm() + WRAPPER_SUFFIX + config.encoding().name().toLowerCase() + ","
                + config.encoding().codec().encode(cipherBytes) + WRAPPER_END;
    }

    /**
     * JCE 变换串使用的基础算法名
     *
     * <p>默认与 {@link #algorithm()} 相同；当密文前缀与 JCE 变换基础名不一致时（例如 GOST3412 的前缀
     * 为 {@code GOST3412}、而 JCE 名为 {@code GOST3412-2015}；DESEDE 的前缀为 {@code DESEDE}、
     * 而 JCE 名为 {@code DESede}），由子类覆写。用于按 {@code mode} / {@code padding} 拼接变换串。</p>
     *
     * @return JCE 变换基础算法名
     */
    protected String jceAlgorithm() {
        return algorithm();
    }

    /**
     * 解析当前算法的配置
     *
     * @return 算法配置
     */
    protected CipherConfig resolveConfig() {
        return context().resolveConfig(algorithm(), jceAlgorithm(), defaultTransformation(), defaultEncoding());
    }

    /**
     * 获取当前算法的密钥内容，缺失时抛出异常
     *
     * @return 密钥内容
     */
    protected String requireKey() {
        String key = context().resolveKey(algorithm());
        if (key == null) {
            throw new IllegalStateException("未找到 " + algorithm() + " 算法所需密钥，请配置 smcrypt."
                    + algorithm().toLowerCase() + ".key 或 smcrypt." + algorithm().toLowerCase() + ".file");
        }
        return key;
    }

    /**
     * 获取并解码对称密钥
     *
     * @return 密钥字节
     */
    protected byte[] resolveSymmetricKey() {
        return KeyCodec.decodeKey(requireKey());
    }
}
