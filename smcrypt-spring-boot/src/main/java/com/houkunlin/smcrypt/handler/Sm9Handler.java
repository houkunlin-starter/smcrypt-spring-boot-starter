package com.houkunlin.smcrypt.handler;

import com.houkunlin.smcrypt.codec.EncodingDetector;
import com.houkunlin.smcrypt.config.CipherConfig;
import org.bouncycastle.asn1.gm.SM9Cipher;
import org.bouncycastle.crypto.engines.SM9Engine;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.params.SM9EncMasterPublicKeyParameters;
import org.bouncycastle.crypto.params.SM9EncPrivateKeyParameters;
import org.bouncycastle.crypto.params.SM9EncPublicKeyParameters;
import org.bouncycastle.util.Arrays;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * SM9 标识加密（IBC）处理器。
 *
 * <p>密文格式：{@code SM9ENC(...)}。SM9 为标识密码（GB/T 38635、GM/T 0044），用户私钥由 KGC
 * 用主私钥按身份派生，配置方式与其它算法不同，需同时提供用户私钥、主公钥、身份与 hid：</p>
 * <ul>
 *     <li>{@code smcrypt.sm9.private-key}：用户私钥 {@code de}（G2 点，129 字节，hex/Base64）；</li>
 *     <li>{@code smcrypt.sm9.master-public-key}：主公钥 {@code Ppub-e}（G1 点，65 字节，hex/Base64）；</li>
 *     <li>{@code smcrypt.sm9.identity}：身份字符串；</li>
 *     <li>{@code smcrypt.sm9.hid}：私钥生成函数标识，KEM/加密默认 {@code 03}；</li>
 *     <li>{@code smcrypt.sm9.mode}：数据封装方式，{@code SM4}（SM4-ECB，默认）或 {@code STREAM}（KDF 流）；</li>
 *     <li>{@code smcrypt.sm9.cipher-format}：密文格式，{@code raw}（{@code C1||C3||C2}，默认）或
 *         {@code asn1}（GM/T 0080-2020）；</li>
 *     <li>{@code smcrypt.sm9.encoding}：加密输出编码，默认 Base64。</li>
 * </ul>
 *
 * <p>需要 BouncyCastle 1.86 及以上版本。</p>
 *
 * @author HouKunLin
 */
public class Sm9Handler extends AbstractCipherHandler {
    /**
     * 算法名称（同时作为 JCE 变换串占位）
     */
    private static final String ALGORITHM = "SM9";
    /**
     * SM9 配置项前缀
     */
    private static final String PROPERTY_PREFIX = "smcrypt.sm9.";
    /**
     * KEM/加密用途的私钥生成函数标识 hid
     */
    private static final byte HID_ENCRYPTION = 0x03;
    /**
     * 密文格式：GM/T 0080-2020 ASN.1 结构
     */
    private static final String CIPHER_FORMAT_ASN1 = "asn1";
    /**
     * 封装方式：KDF 流
     */
    private static final String MODE_STREAM = "STREAM";
    /**
     * ASN.1 密文中 C1 的未压缩点前缀
     */
    private static final byte UNCOMPRESSED_POINT_PREFIX = 0x04;
    /**
     * G1 点（C1）未压缩编码长度：0x04 || x || y
     */
    private static final int G1_ENCODED_LENGTH = 65;
    /**
     * G1 点坐标长度
     */
    private static final int G1_COORDINATE_LENGTH = 64;
    /**
     * C3（SM3 MAC）长度
     */
    private static final int C3_LENGTH = 32;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String algorithm() {
        return ALGORITHM;
    }

    @Override
    protected String defaultTransformation() {
        return ALGORITHM;
    }

    @Override
    protected byte[] doDecrypt(byte[] cipherBytes, CipherConfig config) throws Exception {
        SM9EncMasterPublicKeyParameters masterPublicKey = loadMasterPublicKey();
        SM9EncPrivateKeyParameters userKey = loadUserKey(masterPublicKey);

        byte[] rawCipherText;
        SM9Engine.Mode mode;
        if (isAsn1Format()) {
            SM9Cipher cipher = SM9Cipher.getInstance(cipherBytes);
            mode = toEngineMode(cipher.getEnType());
            rawCipherText = toRawCipherText(cipher.getC1(), cipher.getC3(), cipher.getC2());
        } else {
            mode = resolveMode(config);
            rawCipherText = cipherBytes;
        }

        SM9Engine engine = new SM9Engine(mode);
        engine.init(false, userKey);
        return engine.processBlock(rawCipherText, 0, rawCipherText.length);
    }

    @Override
    protected byte[] doEncrypt(byte[] plainBytes, CipherConfig config) throws Exception {
        SM9EncMasterPublicKeyParameters masterPublicKey = loadMasterPublicKey();
        SM9EncPublicKeyParameters recipient = masterPublicKey.getUserPublicKey(identity(), hid());

        SM9Engine.Mode mode = resolveMode(config);
        SM9Engine engine = new SM9Engine(mode);
        engine.init(true, new ParametersWithRandom(recipient, random));
        byte[] rawCipherText = engine.processBlock(plainBytes, 0, plainBytes.length);

        if (isAsn1Format()) {
            return toAsn1CipherText(mode, rawCipherText);
        }
        return rawCipherText;
    }

    /**
     * 加载主公钥 {@code Ppub-e}
     *
     * @return 主公钥参数
     */
    private SM9EncMasterPublicKeyParameters loadMasterPublicKey() {
        byte[] encoded = decodePoint(requireProperty("master-public-key"));
        return SM9EncMasterPublicKeyParameters.fromEncoded(encoded);
    }

    /**
     * 加载并重建用户私钥 {@code de}
     *
     * @param masterPublicKey 主公钥（私钥编码不含主公钥/身份/hid，需一并提供）
     * @return 用户私钥参数
     */
    private SM9EncPrivateKeyParameters loadUserKey(SM9EncMasterPublicKeyParameters masterPublicKey) {
        byte[] de = decodePoint(requireProperty("private-key"));
        return SM9EncPrivateKeyParameters.fromEncoded(de, masterPublicKey, identity(), hid());
    }

    /**
     * 读取身份字符串（UTF-8 字节）
     *
     * @return 身份字节
     */
    private byte[] identity() {
        return requireProperty("identity").getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 读取私钥生成函数标识 hid，默认 {@code 03}
     *
     * @return hid 字节
     */
    private byte hid() {
        String value = context().getProperty(PROPERTY_PREFIX + "hid");
        if (value == null || value.trim().isEmpty()) {
            return HID_ENCRYPTION;
        }
        String text = value.trim();
        if (text.regionMatches(true, 0, "0x", 0, 2)) {
            text = text.substring(2);
        }
        return (byte) Integer.parseInt(text, 16);
    }

    /**
     * 是否使用 GM/T 0080-2020 ASN.1 密文格式
     *
     * @return 使用 ASN.1 格式返回 true，默认 raw
     */
    private boolean isAsn1Format() {
        String value = context().getProperty(PROPERTY_PREFIX + "cipher-format");
        return value != null && CIPHER_FORMAT_ASN1.equalsIgnoreCase(value.trim());
    }

    /**
     * 解析数据封装方式
     *
     * @param config 算法配置
     * @return {@code STREAM} 返回流模式，否则返回 SM4 模式
     */
    private SM9Engine.Mode resolveMode(CipherConfig config) {
        String mode = config.mode();
        return mode != null && MODE_STREAM.equalsIgnoreCase(mode.trim())
                ? SM9Engine.Mode.STREAM
                : SM9Engine.Mode.SM4;
    }

    /**
     * 将 ASN.1 的 enType 转换为引擎模式
     *
     * @param enType GM/T 0080 的数据封装类型
     * @return 引擎模式
     */
    private SM9Engine.Mode toEngineMode(int enType) {
        return enType == SM9Cipher.EN_TYPE_STREAM ? SM9Engine.Mode.STREAM : SM9Engine.Mode.SM4;
    }

    /**
     * 将 ASN.1 的 C1/C3/C2 还原为引擎原生密文 {@code C1||C3||C2}
     *
     * @param c1 ASN.1 中的 C1（{@code 0x04||x||y}，65 字节）
     * @param c3 C3（SM3 MAC）
     * @param c2 C2（密文数据）
     * @return 引擎原生密文
     */
    private byte[] toRawCipherText(byte[] c1, byte[] c3, byte[] c2) {
        byte[] point = (c1.length == G1_ENCODED_LENGTH && c1[0] == UNCOMPRESSED_POINT_PREFIX)
                ? Arrays.copyOfRange(c1, 1, c1.length)
                : c1;
        return Arrays.concatenate(point, c3, c2);
    }

    /**
     * 将引擎原生密文封装为 GM/T 0080-2020 ASN.1 结构
     *
     * @param mode 封装方式
     * @param raw  {@code C1||C3||C2}
     * @return ASN.1 编码后的密文
     * @throws IOException ASN.1 编码失败时抛出
     */
    private byte[] toAsn1CipherText(SM9Engine.Mode mode, byte[] raw) throws IOException {
        byte[] c1 = new byte[G1_ENCODED_LENGTH];
        c1[0] = UNCOMPRESSED_POINT_PREFIX;
        System.arraycopy(raw, 0, c1, 1, G1_COORDINATE_LENGTH);
        byte[] c3 = Arrays.copyOfRange(raw, G1_COORDINATE_LENGTH, G1_COORDINATE_LENGTH + C3_LENGTH);
        byte[] c2 = Arrays.copyOfRange(raw, G1_COORDINATE_LENGTH + C3_LENGTH, raw.length);
        int enType = mode == SM9Engine.Mode.SM4 ? SM9Cipher.EN_TYPE_SM4 : SM9Cipher.EN_TYPE_STREAM;
        return new SM9Cipher(enType, c1, c3, c2).getEncoded();
    }

    /**
     * 将点数据（hex / Base64）解码为字节
     *
     * @param text 点编码文本
     * @return 点字节
     */
    private byte[] decodePoint(String text) {
        return EncodingDetector.detect(text).decode();
    }

    /**
     * 读取必填配置项
     *
     * @param name 配置项名（不含 {@code smcrypt.sm9.} 前缀）
     * @return 配置值（去除首尾空白）
     */
    private String requireProperty(String name) {
        String value = context().getProperty(PROPERTY_PREFIX + name);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("未找到 SM9 配置项 " + PROPERTY_PREFIX + name);
        }
        return value.trim();
    }
}
