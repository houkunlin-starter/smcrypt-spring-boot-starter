package com.houkunlin.smcrypt.key;

import com.houkunlin.smcrypt.BouncyCastleSupport;
import com.houkunlin.smcrypt.SmCryptLog;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.IOException;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Locale;

/**
 * 非对称私钥加载与公钥推导工具。
 *
 * <p>支持 PEM（PKCS#1、PKCS#8、EC）与 Base64/DER（PKCS#8）两种私钥格式。
 * 加密场景下若无独立公钥，可由私钥推导出公钥：RSA 通过模数与公开指数推导，
 * EC/SM2 通过私钥标量与基点相乘推导。</p>
 *
 * @author HouKunLin
 */
public final class PrivateKeyLoader {
    /**
     * EC 密钥工厂算法名称（SM2、ECC 均映射到 EC）
     */
    private static final String EC_ALGORITHM = "EC";

    /**
     * 工具类，禁止实例化
     */
    private PrivateKeyLoader() {
    }

    /**
     * 加载非对称私钥
     *
     * <p>支持两种格式：包含 {@code -----BEGIN} 标识的 PEM 文本，以及 Base64/DER 编码的 PKCS#8 私钥。</p>
     *
     * @param content   私钥内容（PEM 或 Base64/DER 编码的 PKCS#8）
     * @param algorithm 算法名称（SM2、RSA、ECC 等）
     * @return 私钥对象
     * @throws IllegalArgumentException 私钥内容为空，或解析失败（PEM 解析、Base64/DER 解码、
     *                                  KeyFactory 生成私钥失败等）时抛出，原始异常作为 cause 保留
     */
    public static PrivateKey load(String content, String algorithm) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException(algorithm + " 私钥内容不能为空");
        }
        String value = content.trim();
        try {
            if (value.contains("-----BEGIN")) {
                return loadPem(value);
            }
            byte[] der = decodeBase64OrRaw(value);
            KeyFactory keyFactory = KeyFactory.getInstance(jceAlgorithm(algorithm), BouncyCastleSupport.provider());
            return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalArgumentException("无法解析 " + algorithm + " 私钥：" + e.getMessage(), e);
        }
    }

    /**
     * 由私钥推导对应公钥
     *
     * @param privateKey 私钥
     * @param algorithm  算法名称
     * @return 公钥对象
     */
    public static PublicKey derivePublicKey(PrivateKey privateKey, String algorithm) {
        try {
            if (privateKey instanceof RSAPrivateCrtKey) {
                RSAPrivateCrtKey rsa = (RSAPrivateCrtKey) privateKey;
                return KeyFactory.getInstance("RSA", BouncyCastleSupport.provider())
                        .generatePublic(new RSAPublicKeySpec(rsa.getModulus(), rsa.getPublicExponent()));
            }
            AsymmetricKeyParameter parameter = PrivateKeyFactory.createKey(privateKey.getEncoded());
            if (parameter instanceof ECPrivateKeyParameters) {
                ECPrivateKeyParameters ecPrivate = (ECPrivateKeyParameters) parameter;
                ECPublicKeyParameters ecPublic = new ECPublicKeyParameters(
                        ecPrivate.getParameters().getG().multiply(ecPrivate.getD()).normalize(),
                        ecPrivate.getParameters());
                return new JcaPEMKeyConverter().setProvider(BouncyCastleSupport.provider())
                        .getPublicKey(SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(ecPublic));
            }
            throw new IllegalArgumentException("暂不支持从 " + privateKey.getAlgorithm() + " 私钥推导公钥");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("无法从 " + algorithm + " 私钥推导公钥：" + e.getMessage(), e);
        }
    }

    /**
     * 解析 PEM 格式私钥文本
     *
     * <p>通过 BouncyCastle 的 {@link PEMParser} 读取 PEM 对象，支持以下形式：</p>
     * <ul>
     *     <li>{@link PEMKeyPair}：PKCS#1 / SEC1 密钥对（如 {@code -----BEGIN RSA PRIVATE KEY-----}、
     *         {@code -----BEGIN EC PRIVATE KEY-----}），返回其中的私钥；</li>
     *     <li>{@link PrivateKeyInfo}：PKCS#8 私钥（如 {@code -----BEGIN PRIVATE KEY-----}）。</li>
     * </ul>
     *
     * <p>加密的 PEM 私钥（{@link PEMEncryptedKeyPair}）与无法识别的 PEM 内容（包括空内容）均不受支持。</p>
     *
     * @param value PEM 文本（已去除首尾空白，且包含 {@code -----BEGIN} 标识）
     * @return 解析出的私钥对象
     * @throws IOException              PEM 内容读取失败（{@link PEMParser#readObject()}）、密钥转换失败
     *                                  （BouncyCastle 的 {@code PEMException} 继承自 {@link IOException}），
     *                                  或关闭解析器失败时抛出
     * @throws IllegalArgumentException PEM 私钥已加密（暂不支持），或 PEM 内容无法识别时抛出
     */
    private static PrivateKey loadPem(String value) throws IOException {
        try (PEMParser parser = new PEMParser(new StringReader(value))) {
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleSupport.provider());
            Object object = parser.readObject();
            if (object instanceof PEMKeyPair) {
                return converter.getKeyPair((PEMKeyPair) object).getPrivate();
            }
            if (object instanceof PrivateKeyInfo) {
                return converter.getPrivateKey((PrivateKeyInfo) object);
            }
            if (object instanceof PEMEncryptedKeyPair) {
                throw new IllegalArgumentException("暂不支持加密的 PEM 私钥");
            }
            throw new IllegalArgumentException("无法识别的 PEM 私钥内容");
        }
    }

    /**
     * 将内容按 Base64 解码，解码失败时按 UTF-8 原始字节处理
     *
     * @param value 待解码内容
     * @return 解码后的字节数组
     */
    private static byte[] decodeBase64OrRaw(String value) {
        String compact = value.replaceAll("\\s", "");
        try {
            return Base64.getDecoder().decode(compact);
        } catch (IllegalArgumentException e) {
            SmCryptLog.debug("私钥内容无法按 Base64 解码，将按 UTF-8 原始字节处理", e);
            return value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    /**
     * 将算法名称映射为 JCE KeyFactory 使用的算法名称
     *
     * <p>SM2、ECC、EC 均使用 {@code EC} 密钥工厂。</p>
     *
     * @param algorithm 算法名称
     * @return JCE 算法名称
     */
    private static String jceAlgorithm(String algorithm) {
        String upper = algorithm.toUpperCase(Locale.ROOT);
        if ("SM2".equals(upper) || "ECC".equals(upper) || EC_ALGORITHM.equals(upper)) {
            return EC_ALGORITHM;
        }
        return upper;
    }
}
