package com.houkunlin.smcrypt.key;

import com.houkunlin.smcrypt.BouncyCastleSupport;
import com.houkunlin.smcrypt.SmCryptLog;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.KeyGenerationParameters;
import org.bouncycastle.crypto.generators.SM9EncMasterKeyPairGenerator;
import org.bouncycastle.crypto.params.SM9EncMasterPrivateKeyParameters;
import org.bouncycastle.crypto.params.SM9EncMasterPublicKeyParameters;
import org.bouncycastle.crypto.params.SM9EncPrivateKeyParameters;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.util.encoders.Hex;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.StringWriter;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

/**
 * 密钥生成工具。
 *
 * <p>用于生成指定长度的加解密密钥，便于配置密文的生成与联调：</p>
 * <ul>
 *     <li>对称算法（SM4 / AES / DES / DESEDE）：生成随机密钥并返回 hex 字符串，可直接用于
 *         {@code smcrypt.<算法>.key}；</li>
 *     <li>非对称算法（RSA / ECC / SM2）：生成密钥对，私钥可导出为 PEM 或 Base64(DER)；</li>
 *     <li>SM9：生成 KGC 主密钥对与指定身份的用户私钥，可直接用于 {@code smcrypt.sm9.*} 配置。</li>
 * </ul>
 *
 * @author HouKunLin
 */
public final class SmCryptKeyGenerator {
    /**
     * 密钥生成用安全随机数
     */
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String SM4 = "SM4";
    private static final String AES = "AES";
    private static final String DES = "DES";
    private static final String DESEDE = "DESEDE";
    private static final String CHACHA20 = "CHACHA20";
    private static final String GOST3412 = "GOST3412";
    private static final String DSTU7624 = "DSTU7624";
    private static final String RC6 = "RC6";
    private static final String RSA = "RSA";
    private static final String SM2 = "SM2";
    private static final String ECC = "ECC";

    /**
     * 工具类，禁止实例化
     */
    private SmCryptKeyGenerator() {
    }

    /**
     * 判断是否为对称算法
     *
     * @param algorithm 算法名称
     * @return 对称算法返回 true
     */
    public static boolean isSymmetric(String algorithm) {
        String upper = algorithm.toUpperCase();
        return SM4.equals(upper) || AES.equals(upper) || DES.equals(upper) || DESEDE.equals(upper)
                || CHACHA20.equals(upper) || GOST3412.equals(upper) || DSTU7624.equals(upper) || RC6.equals(upper);
    }

    /**
     * 生成对称密钥
     *
     * @param algorithm   对称算法名称（SM4 / AES / DES / DESEDE）
     * @param keySizeBits 密钥长度（位）；&le; 0 时使用算法默认值
     *                    （SM4=128、AES=256、DES=56、DESEDE=168）
     * @return 十六进制密钥字符串
     */
    public static String generateSymmetricKey(String algorithm, int keySizeBits) {
        String upper = algorithm.toUpperCase();
        int jceKeySize = resolveSymmetricKeySize(upper, keySizeBits);
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(symmetricJceAlgorithm(upper), BouncyCastleSupport.provider());
            keyGenerator.init(jceKeySize, RANDOM);
            SecretKey key = keyGenerator.generateKey();
            return Hex.toHexString(key.getEncoded());
        } catch (Exception e) {
            throw new IllegalArgumentException("对称密钥生成失败（" + algorithm + "）：" + e.getMessage(), e);
        }
    }

    /**
     * 生成非对称密钥对
     *
     * @param algorithm   非对称算法名称（RSA / ECC / SM2）
     * @param keySizeBits 密钥长度（位）；&le; 0 时使用默认值（RSA=2048、ECC=256，SM2 固定 256）
     * @return 密钥对
     */
    public static KeyPair generateKeyPair(String algorithm, int keySizeBits) {
        String upper = algorithm.toUpperCase();
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(jceKeyPairAlgorithm(upper), BouncyCastleSupport.provider());
            if (RSA.equals(upper)) {
                generator.initialize(keySizeBits > 0 ? keySizeBits : 2048, RANDOM);
            } else if (SM2.equals(upper)) {
                generator.initialize(new ECGenParameterSpec("sm2p256v1"), RANDOM);
            } else if (ECC.equals(upper)) {
                generator.initialize(new ECGenParameterSpec(ecCurve(keySizeBits)), RANDOM);
            } else {
                throw new IllegalArgumentException("不支持的非对称算法：" + algorithm);
            }
            return generator.generateKeyPair();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("密钥对生成失败（" + algorithm + "）：" + e.getMessage(), e);
        }
    }

    /**
     * 将私钥导出为 PEM 字符串
     *
     * @param privateKey 私钥
     * @return PEM 文本（PKCS#1 / PKCS#8，可被 {@link PrivateKeyLoader} 解析）
     */
    public static String toPrivateKeyPem(PrivateKey privateKey) {
        return toPem(privateKey, "私钥");
    }

    /**
     * 将公钥导出为 PEM 字符串
     *
     * @param publicKey 公钥
     * @return PEM 文本
     */
    public static String toPublicKeyPem(PublicKey publicKey) {
        return toPem(publicKey, "公钥");
    }

    /**
     * 将私钥导出为 Base64（PKCS#8 DER）字符串
     *
     * @param privateKey 私钥
     * @return Base64 文本
     */
    public static String toPrivateKeyBase64(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }

    /**
     * 生成 SM9 密钥材料（KGC 主密钥对 + 指定身份的用户私钥）
     *
     * @param identity 身份字符串的字节（UTF-8）
     * @param hid      私钥生成函数标识，KEM / 加密用 {@code 0x03}
     * @return SM9 密钥材料
     */
    public static Sm9KeyMaterial generateSm9Key(byte[] identity, byte hid) {
        SM9EncMasterKeyPairGenerator generator = new SM9EncMasterKeyPairGenerator();
        generator.init(new KeyGenerationParameters(RANDOM, 256));
        AsymmetricCipherKeyPair keyPair = generator.generateKeyPair();
        SM9EncMasterPrivateKeyParameters masterPrivate = (SM9EncMasterPrivateKeyParameters) keyPair.getPrivate();
        SM9EncMasterPublicKeyParameters masterPublic = masterPrivate.getPublicKeyParameters();
        SM9EncPrivateKeyParameters userKey = masterPrivate.generateUserKey(identity, hid);
        return new Sm9KeyMaterial(
                Hex.toHexString(masterPrivate.getEncoded()),
                Base64.getEncoder().encodeToString(masterPublic.getEncoded()),
                Base64.getEncoder().encodeToString(userKey.getEncoded()),
                identity,
                hid);
    }

    private static String toPem(Object object, String type) {
        StringWriter stringWriter = new StringWriter();
        JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter);
        try {
            pemWriter.writeObject(object);
        } catch (Exception e) {
            throw new IllegalArgumentException("导出" + type + " PEM 失败：" + e.getMessage(), e);
        } finally {
            try {
                pemWriter.close();
            } catch (Exception e) {
                SmCryptLog.debug("关闭 PEM 写出器失败", e);
            }
        }
        return stringWriter.toString();
    }

    private static int resolveSymmetricKeySize(String algorithm, int requestedBits) {
        switch (algorithm) {
            case SM4:
                if (requestedBits <= 0) {
                    return 128;
                }
                if (requestedBits != 128) {
                    throw new IllegalArgumentException("SM4 密钥长度仅支持 128 位");
                }
                return 128;
            case AES:
                if (requestedBits <= 0) {
                    return 256;
                }
                if (requestedBits != 128 && requestedBits != 192 && requestedBits != 256) {
                    throw new IllegalArgumentException("AES 密钥长度仅支持 128 / 192 / 256 位");
                }
                return requestedBits;
            case DES:
                if (requestedBits <= 0) {
                    return 56;
                }
                if (requestedBits != 56 && requestedBits != 64) {
                    throw new IllegalArgumentException("DES 密钥长度仅支持 56 / 64 位");
                }
                return 56;
            case DESEDE:
                if (requestedBits <= 0) {
                    return 168;
                }
                if (requestedBits == 112 || requestedBits == 128) {
                    return 112;
                }
                if (requestedBits == 168 || requestedBits == 192) {
                    return 168;
                }
                throw new IllegalArgumentException("DESEDE 密钥长度仅支持 112 / 128（2-key）或 168 / 192（3-key）位");
            case CHACHA20:
                if (requestedBits <= 0 || requestedBits == 256) {
                    return 256;
                }
                throw new IllegalArgumentException("ChaCha20 密钥长度仅支持 256 位");
            case GOST3412:
                if (requestedBits <= 0 || requestedBits == 256) {
                    return 256;
                }
                throw new IllegalArgumentException("GOST3412 密钥长度仅支持 256 位");
            case DSTU7624:
                if (requestedBits <= 0) {
                    return 256;
                }
                if (requestedBits != 128 && requestedBits != 256 && requestedBits != 512) {
                    throw new IllegalArgumentException("DSTU7624 密钥长度仅支持 128 / 256 / 512 位");
                }
                return requestedBits;
            case RC6:
                if (requestedBits <= 0) {
                    return 256;
                }
                if (requestedBits != 128 && requestedBits != 192 && requestedBits != 256) {
                    throw new IllegalArgumentException("RC6 密钥长度仅支持 128 / 192 / 256 位");
                }
                return requestedBits;
            default:
                throw new IllegalArgumentException("不支持的对称算法：" + algorithm);
        }
    }

    private static String symmetricJceAlgorithm(String algorithm) {
        if (DESEDE.equals(algorithm)) {
            return "DESede";
        }
        if (CHACHA20.equals(algorithm)) {
            return "ChaCha20";
        }
        if (GOST3412.equals(algorithm)) {
            return "GOST3412-2015";
        }
        return algorithm;
    }

    private static String jceKeyPairAlgorithm(String algorithm) {
        return RSA.equals(algorithm) ? RSA : "EC";
    }

    private static String ecCurve(int keySizeBits) {
        switch (keySizeBits) {
            case 0:
            case 256:
                return "secp256r1";
            case 384:
                return "secp384r1";
            case 521:
                return "secp521r1";
            default:
                throw new IllegalArgumentException("ECC 曲线位数仅支持 256 / 384 / 521");
        }
    }
}
