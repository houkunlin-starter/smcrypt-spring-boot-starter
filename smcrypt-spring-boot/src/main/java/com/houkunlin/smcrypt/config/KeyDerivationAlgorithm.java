package com.houkunlin.smcrypt.config;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 口令派生函数（KDF）算法。
 *
 * <p>用于「口令 → 密钥」的派生，通过 {@code smcrypt.<前缀>.kdf} 配置。</p>
 *
 * @author HouKunLin
 */
public enum KeyDerivationAlgorithm {
    /**
     * PBKDF2（PKCS#5 v2），基于 HMAC 的迭代派生
     */
    PBKDF2,
    /**
     * scrypt（内存硬 KDF）
     */
    SCRYPT,
    /**
     * Argon2（Argon2id，内存硬 KDF）
     */
    ARGON2;

    /**
     * 非字母数字字符匹配（预编译），用于归一化配置值
     */
    private static final Pattern NON_ALNUM = Pattern.compile("[^A-Za-z0-9]");

    /**
     * 解析 KDF 标记
     *
     * <p>忽略大小写与分隔符，支持 {@code PBKDF2}、{@code scrypt}、{@code Argon2} / {@code Argon2id}。
     * 传入 null 或空白返回 null；无法识别的非空值抛出异常。</p>
     *
     * @param token 配置值
     * @return 对应的 KDF；未配置时返回 null
     * @throws IllegalArgumentException 配置了无法识别的 KDF 时抛出
     */
    public static KeyDerivationAlgorithm fromToken(String token) {
        if (token == null) {
            return null;
        }
        String normalized = NON_ALNUM.matcher(token).replaceAll("").toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }
        if ("PBKDF2".equals(normalized)) {
            return PBKDF2;
        }
        if ("SCRYPT".equals(normalized)) {
            return SCRYPT;
        }
        if ("ARGON2".equals(normalized) || "ARGON2ID".equals(normalized)) {
            return ARGON2;
        }
        throw new IllegalArgumentException("不支持的 KDF：" + token + "，仅支持 PBKDF2 / SCRYPT / ARGON2");
    }
}
