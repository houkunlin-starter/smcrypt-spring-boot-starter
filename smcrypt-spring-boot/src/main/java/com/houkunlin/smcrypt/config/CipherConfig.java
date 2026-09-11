package com.houkunlin.smcrypt.config;

import com.houkunlin.smcrypt.PropertyLookup;
import com.houkunlin.smcrypt.codec.CipherEncoding;
import com.houkunlin.smcrypt.codec.EncodingDetector;

/**
 * 单个算法的加解密配置。
 *
 * <p>从属性（Spring 环境或系统属性）中按 {@code smcrypt.<算法>.<项>} 的键名读取，
 * 未配置时使用算法内置默认值。支持的配置项：</p>
 * <ul>
 *     <li>{@code smcrypt.<alg>.transformation}：完整 JCE 变换串，优先级最高，例如 {@code AES/GCM/NoPadding}；</li>
 *     <li>{@code smcrypt.<alg>.mode}：模式，与 {@code padding} 组合生成变换串；</li>
 *     <li>{@code smcrypt.<alg>.padding}：填充方式，默认 {@code PKCS5Padding}；</li>
 *     <li>{@code smcrypt.<alg>.iv}：初始向量（hex 或 Base64，自动识别）；</li>
 *     <li>{@code smcrypt.<alg>.encoding}：加密输出所用编码，默认 Base64。</li>
 * </ul>
 *
 * @author HouKunLin
 */
public class CipherConfig {
    /**
     * JCE 变换串分隔符
     */
    private static final String TRANSFORMATION_SEPARATOR = "/";

    /**
     * 算法名称
     */
    private final String algorithm;
    /**
     * JCE 变换串
     */
    private final String transformation;
    /**
     * 加密模式
     */
    private final String mode;
    /**
     * 填充方式
     */
    private final String padding;
    /**
     * 初始向量
     */
    private final byte[] iv;
    /**
     * 加密输出编码
     */
    private final CipherEncoding encoding;

    /**
     * 构造算法配置
     *
     * @param algorithm      算法名称
     * @param transformation JCE 变换串
     * @param mode           加密模式
     * @param padding        填充方式
     * @param iv             初始向量
     * @param encoding       加密输出编码
     */
    public CipherConfig(String algorithm, String transformation, String mode, String padding,
                        byte[] iv, CipherEncoding encoding) {
        this.algorithm = algorithm;
        this.transformation = transformation;
        this.mode = mode;
        this.padding = padding;
        this.iv = iv;
        this.encoding = encoding;
    }

    /**
     * 根据属性解析算法配置
     *
     * @param algorithm             算法名称，如 {@code SM4}
     * @param properties            属性查询接口
     * @param defaultTransformation 未配置 {@code transformation}/{@code mode} 时使用的默认变换串
     * @param defaultEncoding       未配置 {@code encoding} 时加密输出使用的默认编码
     * @return 解析后的算法配置
     */
    public static CipherConfig resolve(String algorithm, PropertyLookup properties,
                                       String defaultTransformation, CipherEncoding defaultEncoding) {
        String prefix = "smcrypt." + algorithm.toLowerCase() + ".";
        String transformationValue = get(properties, prefix + "transformation");
        String modeValue = get(properties, prefix + "mode");
        String paddingValue = get(properties, prefix + "padding");
        String ivText = get(properties, prefix + "iv");
        String encodingText = get(properties, prefix + "encoding");

        CipherEncoding encodingValue = CipherEncoding.fromToken(encodingText);
        if (encodingValue == null) {
            encodingValue = defaultEncoding;
        }

        if (transformationValue == null && modeValue != null) {
            String actualPadding = paddingValue != null ? paddingValue : "PKCS5Padding";
            transformationValue = algorithm.toUpperCase() + TRANSFORMATION_SEPARATOR + modeValue
                    + TRANSFORMATION_SEPARATOR + actualPadding;
        }
        if (transformationValue == null) {
            transformationValue = defaultTransformation;
        }

        byte[] ivValue = null;
        if (ivText != null) {
            ivValue = EncodingDetector.detectEncoding(ivText).codec().decode(ivText);
        }

        return new CipherConfig(algorithm, transformationValue, modeValue, paddingValue, ivValue, encodingValue);
    }

    /**
     * 读取并去除首尾空白的属性值
     *
     * @param properties 属性查询接口
     * @param key        属性键
     * @return 去除空白后的属性值；不存在或为空时返回 null
     */
    private static String get(PropertyLookup properties, String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 获取算法名称
     *
     * @return 算法名称
     */
    public String algorithm() {
        return algorithm;
    }

    /**
     * 获取 JCE 变换串
     *
     * @return JCE 变换串
     */
    public String transformation() {
        return transformation;
    }

    /**
     * 获取加密模式
     *
     * @return 加密模式
     */
    public String mode() {
        return mode;
    }

    /**
     * 获取填充方式
     *
     * @return 填充方式
     */
    public String padding() {
        return padding;
    }

    /**
     * 获取初始向量
     *
     * @return 初始向量
     */
    public byte[] iv() {
        return iv;
    }

    /**
     * 获取加密输出编码
     *
     * @return 加密输出编码
     */
    public CipherEncoding encoding() {
        return encoding;
    }

    /**
     * 是否配置了初始向量
     *
     * @return 已配置返回 true
     */
    public boolean hasIv() {
        return iv != null && iv.length > 0;
    }
}
