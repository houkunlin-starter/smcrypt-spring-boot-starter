package com.houkunlin.smcrypt;

import com.houkunlin.smcrypt.config.KeyDerivationAlgorithm;
import com.houkunlin.smcrypt.handler.AbstractCipherHandler;
import com.houkunlin.smcrypt.handler.DecryptHandler;
import com.houkunlin.smcrypt.key.PrivateKeyLoader;

import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 加密配置安全体检。
 *
 * <p>在启动早期（{@code EnvironmentPostProcessor} 阶段）对**已配置密钥 / 口令**的内置算法做静态检查，
 * 发现弱算法、弱模式、弱派生参数等风险并返回问题描述，由 {@link SmCryptDecryptor} 按
 * {@code smcrypt.strict} 决定仅告警还是中断启动。</p>
 *
 * <p>检查规则：</p>
 * <ul>
 *     <li>对称算法使用 ECB 模式；</li>
 *     <li>使用 DES / 3DES；</li>
 *     <li>RSA 使用 PKCS#1 v1.5 填充或密钥长度小于 2048 位；</li>
 *     <li>Jasypt 使用 {@code PBEWithMD5AndDES}；</li>
 *     <li>PBE 使用固定盐或派生参数过弱。</li>
 * </ul>
 *
 * <p>SPI 注册的自定义处理器不在检查范围内。</p>
 *
 * @author HouKunLin
 */
public class SmCryptSecurityChecker {
    /**
     * RSA 密钥长度下限（位）
     */
    private static final int MIN_RSA_KEY_BITS = 2048;
    /**
     * PBKDF2 迭代次数建议下限
     */
    private static final int MIN_PBKDF2_ITERATIONS = 600000;
    /**
     * Argon2 迭代次数建议下限
     */
    private static final int MIN_ARGON2_ITERATIONS = 3;
    /**
     * scrypt cost 建议下限
     */
    private static final int MIN_SCRYPT_COST = 16384;
    /**
     * JCE PBE 迭代次数建议下限
     */
    private static final int MIN_JCE_ITERATIONS = 1000;
    /**
     * 非字母数字字符匹配（预编译），用于归一化算法名
     */
    private static final Pattern NON_ALNUM = Pattern.compile("[^A-Za-z0-9]");

    /**
     * 参与 ECB 检查的对称算法（ChaCha20-Poly1305 为 AEAD，不含在内）
     */
    private static final Set<String> ECB_CHECK_ALGORITHMS = new HashSet<>(Arrays.asList(
            "SM4", "AES", "DES", "DESEDE", "GOST3412", "DSTU7624", "RC6", "CAMELLIA", "ARIA", "SEED"));

    /**
     * 执行安全体检
     *
     * @param context  加解密上下文
     * @param handlers 已加载的密文处理器（内置 + SPI）
     * @return 问题描述列表；无问题时返回空列表
     */
    public List<String> check(SmCryptContext context, List<DecryptHandler> handlers) {
        List<String> issues = new ArrayList<>();
        for (DecryptHandler handler : handlers) {
            String algorithm = handler.algorithm();
            if (algorithm == null || !(handler instanceof AbstractCipherHandler)) {
                continue;
            }
            String upper = algorithm.toUpperCase(Locale.ROOT);
            AbstractCipherHandler cipherHandler = (AbstractCipherHandler) handler;
            if ("PBE".equals(upper)) {
                checkPbe(context, issues);
            } else if ("JASYPT".equals(upper)) {
                checkJasypt(context, issues);
            } else if ("RSA".equals(upper)) {
                checkRsa(context, cipherHandler, issues);
            } else if (ECB_CHECK_ALGORITHMS.contains(upper)) {
                checkSymmetric(context, cipherHandler, upper, issues);
            }
        }
        return issues;
    }

    /**
     * 检查对称算法的模式与弱算法
     */
    private void checkSymmetric(SmCryptContext context, AbstractCipherHandler handler, String algorithm, List<String> issues) {
        if (context.resolveKey(algorithm) == null) {
            return;
        }
        String lower = algorithm.toLowerCase(Locale.ROOT);
        String transformation = handler.resolveConfig().transformation();
        if (containsIgnoreCase(transformation, "ECB")) {
            issues.add("算法 " + algorithm + " 使用 ECB 模式（smcrypt." + lower + ".transformation=" + transformation
                    + "），会泄露明文分组规律；建议改用 GCM 或 CBC（CBC 需配置随机且不复用的 IV）");
        }
        if ("DES".equals(algorithm)) {
            issues.add("算法 DES 使用 56 位密钥，已被破解；建议改用 AES 或 SM4");
        }
        if ("DESEDE".equals(algorithm)) {
            issues.add("算法 DESEDE（3DES）已过时，NIST 自 2024 年起禁用其加密；建议改用 AES 或 SM4");
        }
    }

    /**
     * 检查 RSA 填充方式与密钥长度
     */
    private void checkRsa(SmCryptContext context, AbstractCipherHandler handler, List<String> issues) {
        String key = context.resolveKey("RSA");
        if (key == null) {
            return;
        }
        String transformation = handler.resolveConfig().transformation();
        if (containsIgnoreCase(transformation, "PKCS1")) {
            issues.add("算法 RSA 使用 PKCS#1 v1.5 填充（transformation=" + transformation
                    + "），存在填充预言风险；建议改用 OAEP，例如 "
                    + "smcrypt.rsa.transformation=RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        }
        checkRsaKeyLength(key, issues);
    }

    /**
     * 检查 RSA 密钥长度（尽力而为，解析失败时跳过）
     */
    private void checkRsaKeyLength(String key, List<String> issues) {
        try {
            PrivateKey privateKey = PrivateKeyLoader.load(key, "RSA");
            if (privateKey instanceof RSAPrivateKey) {
                int bits = ((RSAPrivateKey) privateKey).getModulus().bitLength();
                if (bits < MIN_RSA_KEY_BITS) {
                    issues.add("RSA 私钥仅 " + bits + " 位，低于 " + MIN_RSA_KEY_BITS + " 位安全下限；建议使用 3072 位");
                }
            }
        } catch (Exception e) {
            SmCryptLog.debug("安全体检无法解析 RSA 私钥，跳过密钥长度检查", e);
        }
    }

    /**
     * 检查 PBE 固定盐与派生参数
     */
    private void checkPbe(SmCryptContext context, List<String> issues) {
        if (property(context, "smcrypt.pbe.password") == null) {
            return;
        }
        if ("JCE".equalsIgnoreCase(property(context, "smcrypt.pbe.mode"))) {
            int iterations = intProperty(context, "smcrypt.pbe.iterations", MIN_JCE_ITERATIONS);
            if (iterations < MIN_JCE_ITERATIONS) {
                issues.add("PBE（JCE 模式）迭代次数 " + iterations + " 低于建议值 " + MIN_JCE_ITERATIONS);
            }
            return;
        }
        if (property(context, "smcrypt.pbe.kdf.salt") != null) {
            issues.add("PBE（KDF 模式）配置了固定盐 smcrypt.pbe.kdf.salt；盐应随机生成并内嵌到密文，建议移除该配置");
        }
        KeyDerivationAlgorithm kdf = KeyDerivationAlgorithm.fromToken(property(context, "smcrypt.pbe.kdf"));
        if (kdf == null) {
            kdf = KeyDerivationAlgorithm.PBKDF2;
        }
        switch (kdf) {
            case PBKDF2:
                checkThreshold(context, "smcrypt.pbe.kdf.iterations", MIN_PBKDF2_ITERATIONS, "PBKDF2 迭代次数", issues);
                break;
            case ARGON2:
                checkThreshold(context, "smcrypt.pbe.kdf.iterations", MIN_ARGON2_ITERATIONS, "Argon2 迭代次数", issues);
                break;
            case SCRYPT:
                checkThreshold(context, "smcrypt.pbe.kdf.cost", MIN_SCRYPT_COST, "scrypt cost", issues);
                break;
            default:
                break;
        }
    }

    /**
     * 检查 PBE 数值参数是否低于建议下限
     */
    private void checkThreshold(SmCryptContext context, String property, int minimum, String label, List<String> issues) {
        int value = intProperty(context, property, minimum);
        if (value < minimum) {
            issues.add("PBE（" + label + "）当前值 " + value + " 低于建议值 " + minimum);
        }
    }

    /**
     * 检查 Jasypt 是否使用不安全的旧算法
     */
    private void checkJasypt(SmCryptContext context, List<String> issues) {
        if (property(context, "smcrypt.jasypt.password") == null) {
            return;
        }
        String transformation = property(context, "smcrypt.jasypt.transformation");
        if (transformation != null && "PBEWITHMD5ANDDES".equals(normalize(transformation))) {
            issues.add("Jasypt 使用不安全的 PBEWithMD5AndDES；建议重加密为 PBEWITHHMACSHA512ANDAES_256，"
                    + "或改用 PBEENC + AES-GCM");
        }
    }

    private static boolean containsIgnoreCase(String text, String token) {
        return text != null && text.toUpperCase(Locale.ROOT).contains(token);
    }

    private static String normalize(String value) {
        return NON_ALNUM.matcher(value).replaceAll("").toUpperCase(Locale.ROOT);
    }

    private static String property(SmCryptContext context, String key) {
        String value = context.getProperty(key);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static int intProperty(SmCryptContext context, String key, int defaultValue) {
        String value = property(context, key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            SmCryptLog.debug("安全体检忽略非法数值配置 {}={}", key, value, e);
            return defaultValue;
        }
    }
}
