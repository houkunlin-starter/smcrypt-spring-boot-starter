package com.houkunlin.smcrypt.config;

import com.houkunlin.smcrypt.PropertyLookup;
import com.houkunlin.smcrypt.codec.EncodingDetector;

/**
 * 口令派生（PBE / KDF）配置。
 *
 * <p>从属性中按 {@code <前缀>password}、{@code <前缀>kdf}、{@code <前缀>kdf.*} 读取。支持的配置项：</p>
 * <ul>
 *     <li>{@code <前缀>password}：口令；</li>
 *     <li>{@code <前缀>kdf}：{@code PBKDF2}（默认）/ {@code SCRYPT} / {@code ARGON2}；</li>
 *     <li>{@code <前缀>kdf.prf}：PBKDF2 的 PRF，默认 {@code HmacSHA256}（可选 {@code HmacSM3}）；</li>
 *     <li>{@code <前缀>kdf.salt}：固定盐（hex / Base64）；不配置时随机生成并内嵌密文；</li>
 *     <li>{@code <前缀>kdf.salt-size}：随机盐长度，默认 16 字节；</li>
 *     <li>{@code <前缀>kdf.iterations}：迭代次数（PBKDF2 / Argon2）；</li>
 *     <li>{@code <前缀>kdf.key-length}：派生密钥位数；不配置时按加密算法；</li>
 *     <li>{@code <前缀>kdf.cost}：scrypt 的 N，默认 65536；</li>
 *     <li>{@code <前缀>kdf.block-size}：scrypt 的 r，默认 8；</li>
 *     <li>{@code <前缀>kdf.parallelism}：scrypt 的 p / Argon2 并行度，默认 1；</li>
 *     <li>{@code <前缀>kdf.memory}：Argon2 内存（KB），默认 65536。</li>
 * </ul>
 *
 * @author HouKunLin
 */
public class KeyDerivationConfig {
    private final String password;
    private final KeyDerivationAlgorithm algorithm;
    private final String prf;
    private final byte[] salt;
    private final int saltSize;
    private final int iterations;
    private final int keyLengthBits;
    private final int cost;
    private final int blockSize;
    private final int parallelism;
    private final int memory;

    public KeyDerivationConfig(String password, KeyDerivationAlgorithm algorithm, String prf, byte[] salt,
                               int saltSize, int iterations, int keyLengthBits, int cost, int blockSize,
                               int parallelism, int memory) {
        this.password = password;
        this.algorithm = algorithm;
        this.prf = prf;
        this.salt = salt;
        this.saltSize = saltSize;
        this.iterations = iterations;
        this.keyLengthBits = keyLengthBits;
        this.cost = cost;
        this.blockSize = blockSize;
        this.parallelism = parallelism;
        this.memory = memory;
    }

    /**
     * 根据属性解析口令派生配置
     *
     * @param prefix            属性前缀（含结尾点），如 {@code smcrypt.pbe.}
     * @param properties        属性查询接口
     * @param defaultIterations 未配置迭代次数时的默认值
     * @return 口令派生配置
     */
    public static KeyDerivationConfig resolve(String prefix, PropertyLookup properties, int defaultIterations) {
        String password = get(properties, prefix + "password");
        KeyDerivationAlgorithm algorithm = KeyDerivationAlgorithm.fromToken(get(properties, prefix + "kdf"));
        if (algorithm == null) {
            algorithm = KeyDerivationAlgorithm.PBKDF2;
        }
        String prf = get(properties, prefix + "kdf.prf");
        if (prf == null) {
            prf = "HmacSHA256";
        }
        String saltText = get(properties, prefix + "kdf.salt");
        byte[] salt = saltText == null ? null : EncodingDetector.detectEncoding(saltText).codec().decode(saltText);
        int saltSize = intValue(get(properties, prefix + "kdf.salt-size"), 16);
        int iterationsDefault = algorithm == KeyDerivationAlgorithm.ARGON2 ? 3 : defaultIterations;
        int iterations = intValue(get(properties, prefix + "kdf.iterations"), iterationsDefault);
        int keyLengthBits = intValue(get(properties, prefix + "kdf.key-length"), 0);
        int cost = intValue(get(properties, prefix + "kdf.cost"), 65536);
        int blockSize = intValue(get(properties, prefix + "kdf.block-size"), 8);
        int parallelism = intValue(get(properties, prefix + "kdf.parallelism"), 1);
        int memory = intValue(get(properties, prefix + "kdf.memory"), 65536);
        return new KeyDerivationConfig(password, algorithm, prf, salt, saltSize, iterations, keyLengthBits,
                cost, blockSize, parallelism, memory);
    }

    private static String get(PropertyLookup properties, String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static int intValue(String value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("非法的数值配置：" + value, e);
        }
    }

    public String password() {
        return password;
    }

    public KeyDerivationAlgorithm algorithm() {
        return algorithm;
    }

    public String prf() {
        return prf;
    }

    public byte[] salt() {
        return salt;
    }

    public int saltSize() {
        return saltSize;
    }

    public int iterations() {
        return iterations;
    }

    public int keyLengthBits() {
        return keyLengthBits;
    }

    public int cost() {
        return cost;
    }

    public int blockSize() {
        return blockSize;
    }

    public int parallelism() {
        return parallelism;
    }

    public int memory() {
        return memory;
    }

    /**
     * 是否配置了固定盐
     *
     * @return 已配置固定盐返回 true
     */
    public boolean hasFixedSalt() {
        return salt != null && salt.length > 0;
    }
}
