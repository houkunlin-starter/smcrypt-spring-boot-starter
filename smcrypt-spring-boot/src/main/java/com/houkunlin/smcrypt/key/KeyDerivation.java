package com.houkunlin.smcrypt.key;

import com.houkunlin.smcrypt.config.KeyDerivationConfig;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.generators.SCrypt;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.bouncycastle.crypto.params.KeyParameter;

import java.nio.charset.StandardCharsets;

/**
 * 口令派生（KDF）工具。
 *
 * <p>根据 {@link KeyDerivationConfig} 用口令与盐派生指定长度的密钥字节，支持 PBKDF2、scrypt、Argon2。</p>
 *
 * @author HouKunLin
 */
public final class KeyDerivation {
    private static final String PRF_SHA256 = "HmacSHA256";
    private static final String PRF_SHA512 = "HmacSHA512";
    private static final String PRF_SM3 = "HmacSM3";

    private KeyDerivation() {
    }

    /**
     * 派生密钥字节
     *
     * @param config        口令派生配置
     * @param salt          盐
     * @param keyLengthBits 派生密钥位数
     * @return 派生出的密钥字节
     */
    public static byte[] derive(KeyDerivationConfig config, byte[] salt, int keyLengthBits) {
        if (config.password() == null || config.password().isEmpty()) {
            throw new IllegalStateException("未配置口令，无法派生密钥");
        }
        if (keyLengthBits <= 0 || keyLengthBits % 8 != 0) {
            throw new IllegalArgumentException("派生密钥长度必须为 8 的倍数的正整数（位）：" + keyLengthBits);
        }
        byte[] password = config.password().getBytes(StandardCharsets.UTF_8);
        int keyLengthBytes = keyLengthBits / 8;
        switch (config.algorithm()) {
            case PBKDF2:
                return pbkdf2(password, salt, config.iterations(), keyLengthBytes, config.prf());
            case SCRYPT:
                return scrypt(password, salt, config.cost(), config.blockSize(), config.parallelism(), keyLengthBytes);
            case ARGON2:
                return argon2(password, salt, config.iterations(), config.memory(), config.parallelism(), keyLengthBytes);
            default:
                throw new IllegalArgumentException("不支持的 KDF：" + config.algorithm());
        }
    }

    private static byte[] pbkdf2(byte[] password, byte[] salt, int iterations, int keyLengthBytes, String prf) {
        PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator(prfDigest(prf));
        generator.init(password, salt, iterations);
        KeyParameter key = (KeyParameter) generator.generateDerivedParameters(keyLengthBytes * 8);
        return key.getKey();
    }

    private static byte[] scrypt(byte[] password, byte[] salt, int cost, int blockSize, int parallelism, int keyLengthBytes) {
        return SCrypt.generate(password, salt, cost, blockSize, parallelism, keyLengthBytes);
    }

    private static byte[] argon2(byte[] password, byte[] salt, int iterations, int memory, int parallelism, int keyLengthBytes) {
        Argon2Parameters parameters = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withSalt(salt)
                .withIterations(iterations)
                .withMemoryAsKB(memory)
                .withParallelism(parallelism)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .build();
        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(parameters);
        byte[] key = new byte[keyLengthBytes];
        generator.generateBytes(password, key);
        return key;
    }

    private static org.bouncycastle.crypto.Digest prfDigest(String prf) {
        if (PRF_SHA256.equalsIgnoreCase(prf)) {
            return new SHA256Digest();
        }
        if (PRF_SHA512.equalsIgnoreCase(prf)) {
            return new SHA512Digest();
        }
        if (PRF_SM3.equalsIgnoreCase(prf)) {
            return new SM3Digest();
        }
        throw new IllegalArgumentException("不支持的 PBKDF2 PRF：" + prf + "，仅支持 HmacSHA256 / HmacSHA512 / HmacSM3");
    }
}
