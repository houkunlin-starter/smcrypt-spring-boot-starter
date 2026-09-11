package com.houkunlin.smcrypt.config;

/**
 * 完整性校验（MAC）算法。
 *
 * <p>用于对称算法的 encrypt-then-MAC：加密后对密文计算 MAC，解密前先校验 MAC，
 * 以检测密文是否被篡改。通过 {@code smcrypt.<算法>.mac} 配置启用。</p>
 *
 * @author HouKunLin
 */
public enum MacAlgorithm {
    /**
     * HMAC-SM3（国密），输出 32 字节
     */
    HMAC_SM3("HmacSM3", 32),
    /**
     * HMAC-SHA256，输出 32 字节
     */
    HMAC_SHA256("HmacSHA256", 32);

    private final String jceName;
    private final int macLength;

    MacAlgorithm(String jceName, int macLength) {
        this.jceName = jceName;
        this.macLength = macLength;
    }

    /**
     * JCE 算法名称
     *
     * @return JCE MAC 算法名
     */
    public String jceName() {
        return jceName;
    }

    /**
     * MAC 输出字节长度
     *
     * @return MAC 长度（字节）
     */
    public int macLength() {
        return macLength;
    }

    /**
     * 解析 MAC 算法标记
     *
     * <p>忽略大小写与分隔符，支持 {@code HmacSM3} / {@code SM3}、{@code HmacSHA256} / {@code SHA256}。
     * 传入 null 或空白表示未启用（返回 null）；无法识别的非空值抛出异常。</p>
     *
     * @param token 配置值
     * @return 对应的 MAC 算法；未配置时返回 null
     * @throws IllegalArgumentException 配置了无法识别的 MAC 算法时抛出
     */
    public static MacAlgorithm fromToken(String token) {
        if (token == null) {
            return null;
        }
        String normalized = token.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (normalized.isEmpty()) {
            return null;
        }
        if ("HMACSM3".equals(normalized) || "SM3".equals(normalized)) {
            return HMAC_SM3;
        }
        if ("HMACSHA256".equals(normalized) || "SHA256".equals(normalized)) {
            return HMAC_SHA256;
        }
        throw new IllegalArgumentException("不支持的 MAC 算法：" + token + "，仅支持 HmacSM3 / HmacSHA256");
    }
}
