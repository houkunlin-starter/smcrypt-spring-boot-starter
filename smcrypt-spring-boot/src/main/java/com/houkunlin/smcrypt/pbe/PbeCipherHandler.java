package com.houkunlin.smcrypt.pbe;

import com.houkunlin.smcrypt.BouncyCastleSupport;
import com.houkunlin.smcrypt.config.CipherConfig;
import com.houkunlin.smcrypt.config.KeyDerivationConfig;
import com.houkunlin.smcrypt.key.KeyDerivation;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Locale;

/**
 * 口令派生（PBE）处理器，支持两种模式（{@code smcrypt.pbe.mode}）：
 *
 * <ul>
 *     <li>{@code KDF}（默认）：口令经 PBKDF2 / scrypt / Argon2 派生密钥，再用 {@code transformation}
 *         指定的对称算法加密（如 {@code AES/GCM/NoPadding}）；</li>
 *     <li>{@code JCE}：直接使用 JCE PBE 变换（如 {@code PBEWITHHMACSHA512ANDAES_256}），
 *         由 JVM 默认 Provider（SunJCE）执行。</li>
 * </ul>
 *
 * <p>密文格式：{@code PBEENC(...)}；载荷为 {@code salt ‖ iv ‖ cipher}。</p>
 *
 * @author HouKunLin
 */
public class PbeCipherHandler extends AbstractPbeCipherHandler {
    /**
     * 属性前缀
     */
    private static final String PREFIX = "smcrypt.pbe.";
    private static final String MODE_KDF = "KDF";
    private static final String MODE_JCE = "JCE";
    private static final String DEFAULT_KDF_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String DEFAULT_JCE_TRANSFORMATION = "PBEWITHHMACSHA512ANDAES_256";
    /**
     * KDF 模式默认 PBKDF2 迭代次数
     */
    private static final int DEFAULT_KDF_ITERATIONS = 600000;

    @Override
    public String algorithm() {
        return "PBE";
    }

    @Override
    protected String defaultTransformation() {
        return DEFAULT_KDF_TRANSFORMATION;
    }

    @Override
    protected String propertyPrefix() {
        return PREFIX;
    }

    @Override
    protected int defaultSaltSize() {
        return 16;
    }

    @Override
    protected int defaultIvSize() {
        return 16;
    }

    @Override
    protected String defaultPbeTransformation() {
        return DEFAULT_JCE_TRANSFORMATION;
    }

    @Override
    protected byte[] doDecrypt(byte[] payload, CipherConfig config) throws Exception {
        return isJceMode() ? jceDecrypt(payload, config) : kdfDecrypt(payload, config);
    }

    @Override
    protected byte[] doEncrypt(byte[] plainBytes, CipherConfig config) throws Exception {
        return isJceMode() ? jceEncrypt(plainBytes, config) : kdfEncrypt(plainBytes, config);
    }

    private boolean isJceMode() {
        return MODE_JCE.equalsIgnoreCase(property("mode", MODE_KDF));
    }

    private byte[] kdfEncrypt(byte[] plainBytes, CipherConfig config) throws Exception {
        KeyDerivationConfig kdf = KeyDerivationConfig.resolve(PREFIX, context()::getProperty, DEFAULT_KDF_ITERATIONS);
        String transformation = property("transformation", DEFAULT_KDF_TRANSFORMATION);
        String keyAlgorithm = keyAlgorithm(transformation);
        byte[] salt = kdf.hasFixedSalt() ? kdf.salt() : randomBytes(kdf.saltSize());
        int keyLengthBits = kdf.keyLengthBits() > 0 ? kdf.keyLengthBits() : defaultKeyLengthBits(keyAlgorithm);
        byte[] key = KeyDerivation.derive(kdf, salt, keyLengthBits);
        int ivLength = ivLength(transformation);
        byte[] iv = ivLength > 0 ? randomBytes(ivLength) : null;

        Cipher cipher = Cipher.getInstance(transformation, BouncyCastleSupport.provider());
        SecretKeySpec keySpec = new SecretKeySpec(key, keyAlgorithm);
        if (iv != null) {
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));
        } else {
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        }
        byte[] cipherBytes = cipher.doFinal(plainBytes);
        return frame(salt, iv, cipherBytes, !kdf.hasFixedSalt());
    }

    private byte[] kdfDecrypt(byte[] payload, CipherConfig config) throws Exception {
        KeyDerivationConfig kdf = KeyDerivationConfig.resolve(PREFIX, context()::getProperty, DEFAULT_KDF_ITERATIONS);
        String transformation = property("transformation", DEFAULT_KDF_TRANSFORMATION);
        String keyAlgorithm = keyAlgorithm(transformation);
        int ivLength = ivLength(transformation);
        byte[][] parts = split(payload, kdf.hasFixedSalt() ? kdf.salt() : null, kdf.saltSize(), ivLength);
        int keyLengthBits = kdf.keyLengthBits() > 0 ? kdf.keyLengthBits() : defaultKeyLengthBits(keyAlgorithm);
        byte[] key = KeyDerivation.derive(kdf, parts[0], keyLengthBits);

        Cipher cipher = Cipher.getInstance(transformation, BouncyCastleSupport.provider());
        SecretKeySpec keySpec = new SecretKeySpec(key, keyAlgorithm);
        if (parts[1] != null) {
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(parts[1]));
        } else {
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
        }
        return cipher.doFinal(parts[2]);
    }

    private static String keyAlgorithm(String transformation) {
        int slash = transformation.indexOf('/');
        return slash > 0 ? transformation.substring(0, slash) : transformation;
    }

    private static int ivLength(String transformation) {
        String upper = transformation.toUpperCase(Locale.ROOT);
        if (upper.contains("ECB")) {
            return 0;
        }
        if (upper.contains("GCM") || upper.contains("CHACHA20")) {
            return 12;
        }
        try {
            return Cipher.getInstance(transformation, BouncyCastleSupport.provider()).getBlockSize();
        } catch (Exception e) {
            throw new IllegalStateException("无法解析变换串：" + transformation, e);
        }
    }

    private static int defaultKeyLengthBits(String keyAlgorithm) {
        String upper = keyAlgorithm.toUpperCase(Locale.ROOT);
        switch (upper) {
            case "SM4":
            case "SEED":
                return 128;
            case "DES":
                return 64;
            case "DESEDE":
                return 192;
            default:
                return 256;
        }
    }
}
