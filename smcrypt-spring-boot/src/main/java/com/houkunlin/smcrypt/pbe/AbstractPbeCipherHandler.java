package com.houkunlin.smcrypt.pbe;

import com.houkunlin.smcrypt.handler.AbstractCipherHandler;
import org.bouncycastle.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.security.SecureRandom;

/**
 * 口令派生（PBE）处理器抽象基类。
 *
 * <p>统一密文载荷格式：{@code salt ‖ iv ‖ cipher}（盐/IV 随机生成并内嵌；固定盐时不内嵌盐）。
 * 提供 JCE PBE 变换的加解密与配置读取。</p>
 *
 * @author HouKunLin
 */
public abstract class AbstractPbeCipherHandler extends AbstractCipherHandler {
    /**
     * 随机数生成器
     */
    private static final SecureRandom RANDOM = new SecureRandom();
    /**
     * JCE PBE 默认迭代次数（与 Jasypt 一致）
     */
    protected static final int DEFAULT_JCE_ITERATIONS = 1000;

    /**
     * 属性前缀（含结尾点）
     *
     * @return 属性前缀，如 {@code smcrypt.pbe.}
     */
    protected abstract String propertyPrefix();

    /**
     * 默认盐长度（字节）
     *
     * @return 盐长度
     */
    protected abstract int defaultSaltSize();

    /**
     * 默认 IV 长度（字节）；0 表示无 IV
     *
     * @return IV 长度
     */
    protected abstract int defaultIvSize();

    /**
     * 默认 JCE PBE 变换串
     *
     * @return 默认变换串
     */
    protected abstract String defaultPbeTransformation();

    /**
     * 读取配置项
     *
     * @param name 配置项名（不含前缀）
     * @return 去除首尾空白的配置值；不存在或为空时返回 null
     */
    protected String property(String name) {
        String value = context().getProperty(propertyPrefix() + name);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 读取配置项，缺省返回默认值
     *
     * @param name         配置项名
     * @param defaultValue 默认值
     * @return 配置值或默认值
     */
    protected String property(String name, String defaultValue) {
        String value = property(name);
        return value == null ? defaultValue : value;
    }

    /**
     * 读取整数配置项
     *
     * @param name         配置项名
     * @param defaultValue 默认值
     * @return 配置值或默认值
     */
    protected int intProperty(String name, int defaultValue) {
        String value = property(name);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("非法的数值配置：" + propertyPrefix() + name + "=" + value, e);
        }
    }

    /**
     * 读取口令，缺失时抛出异常
     *
     * @return 口令
     */
    protected String requirePassword() {
        String password = property("password");
        if (password == null) {
            throw new IllegalStateException("未配置口令：" + propertyPrefix() + "password");
        }
        return password;
    }

    /**
     * 使用 JCE PBE 变换加密
     *
     * @param plainBytes 明文
     * @return 载荷 {@code salt ‖ iv ‖ cipher}
     * @throws Exception 加密失败时抛出
     */
    protected byte[] jceEncrypt(byte[] plainBytes) throws Exception {
        String transformation = property("transformation", defaultPbeTransformation());
        int iterations = intProperty("iterations", DEFAULT_JCE_ITERATIONS);
        String provider = property("provider");
        int blockSize = cipherBlockSize(transformation, provider);
        int saltSize = intProperty("salt-size", blockSize > 0 ? blockSize : defaultSaltSize());
        int ivSize = intProperty("iv-size", blockSize > 0 ? blockSize : defaultIvSize());
        byte[] salt = randomBytes(saltSize);
        byte[] iv = ivSize > 0 ? randomBytes(ivSize) : null;

        SecretKey key = pbeKey(transformation, provider, requirePassword());
        Cipher cipher = getCipher(transformation, provider);
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec(salt, iterations, iv));
        return frame(salt, iv, cipher.doFinal(plainBytes), true);
    }

    /**
     * 使用 JCE PBE 变换解密
     *
     * @param payload 载荷 {@code salt ‖ iv ‖ cipher}
     * @return 明文
     * @throws Exception 解密失败时抛出
     */
    protected byte[] jceDecrypt(byte[] payload) throws Exception {
        String transformation = property("transformation", defaultPbeTransformation());
        int iterations = intProperty("iterations", DEFAULT_JCE_ITERATIONS);
        String provider = property("provider");
        int blockSize = cipherBlockSize(transformation, provider);
        int saltSize = intProperty("salt-size", blockSize > 0 ? blockSize : defaultSaltSize());
        int ivSize = intProperty("iv-size", blockSize > 0 ? blockSize : defaultIvSize());

        byte[][] parts = split(payload, null, saltSize, ivSize);
        SecretKey key = pbeKey(transformation, provider, requirePassword());
        Cipher cipher = getCipher(transformation, provider);
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec(parts[0], iterations, parts[1]));
        return cipher.doFinal(parts[2]);
    }

    private SecretKey pbeKey(String transformation, String provider, String password) throws Exception {
        SecretKeyFactory factory = provider == null
                ? SecretKeyFactory.getInstance(transformation)
                : SecretKeyFactory.getInstance(transformation, provider);
        return factory.generateSecret(new PBEKeySpec(password.toCharArray()));
    }

    private Cipher getCipher(String transformation, String provider) throws Exception {
        return provider == null ? Cipher.getInstance(transformation) : Cipher.getInstance(transformation, provider);
    }

    private PBEParameterSpec parameterSpec(byte[] salt, int iterations, byte[] iv) {
        if (iv == null || iv.length == 0) {
            return new PBEParameterSpec(salt, iterations);
        }
        return new PBEParameterSpec(salt, iterations, new IvParameterSpec(iv));
    }

    private int cipherBlockSize(String transformation, String provider) {
        try {
            return getCipher(transformation, provider).getBlockSize();
        } catch (Exception e) {
            throw new IllegalStateException("无法解析变换串：" + transformation, e);
        }
    }

    /**
     * 生成随机字节
     *
     * @param length 长度
     * @return 随机字节
     */
    protected static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    /**
     * 组装载荷 {@code salt ‖ iv ‖ cipher}
     *
     * @param salt      盐（可为 null 或空，表示不内嵌）
     * @param iv        IV（可为 null 或空）
     * @param cipher    密文
     * @param embedSalt 是否内嵌盐
     * @return 载荷字节
     */
    protected static byte[] frame(byte[] salt, byte[] iv, byte[] cipher, boolean embedSalt) {
        byte[] result = cipher;
        if (iv != null && iv.length > 0) {
            result = Arrays.concatenate(iv, result);
        }
        if (embedSalt && salt != null && salt.length > 0) {
            result = Arrays.concatenate(salt, result);
        }
        return result;
    }

    /**
     * 拆分载荷为 {@code [salt, iv, cipher]}
     *
     * @param payload   载荷
     * @param fixedSalt 固定盐；非空时表示载荷不含盐
     * @param saltSize  盐长度
     * @param ivSize    IV 长度（0 表示无 IV）
     * @return 三元数组 {@code [salt, iv, cipher]}
     */
    protected static byte[][] split(byte[] payload, byte[] fixedSalt, int saltSize, int ivSize) {
        byte[] salt;
        int offset;
        if (fixedSalt != null && fixedSalt.length > 0) {
            salt = fixedSalt;
            offset = 0;
        } else {
            if (payload.length < saltSize) {
                throw new IllegalArgumentException("密文长度不足，无法包含盐（需要 " + saltSize + " 字节）");
            }
            salt = Arrays.copyOfRange(payload, 0, saltSize);
            offset = saltSize;
        }
        byte[] iv = null;
        if (ivSize > 0) {
            if (payload.length < offset + ivSize) {
                throw new IllegalArgumentException("密文长度不足，无法包含 IV（需要 " + ivSize + " 字节）");
            }
            iv = Arrays.copyOfRange(payload, offset, offset + ivSize);
            offset += ivSize;
        }
        if (payload.length <= offset) {
            throw new IllegalArgumentException("密文长度不足，缺少密文内容");
        }
        byte[] cipher = Arrays.copyOfRange(payload, offset, payload.length);
        return new byte[][]{salt, iv, cipher};
    }
}
