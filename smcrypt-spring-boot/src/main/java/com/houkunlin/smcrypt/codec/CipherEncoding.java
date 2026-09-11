package com.houkunlin.smcrypt.codec;

import java.util.Locale;

/**
 * 密文编码类型。
 *
 * <p>用于描述密文（或密钥）在字符串与字节数组之间转换时所采用的编码方式：</p>
 * <ul>
 *     <li>{@link #HEX}：十六进制编码；</li>
 *     <li>{@link #BASE64}：Base64 编码。</li>
 * </ul>
 *
 * <p>在密文包裹中可通过显式前缀声明编码，例如 {@code SM4ENC(hex,abcdef)}、
 * {@code SM4ENC(base64,YWJjZA==)}；未声明时由
 * {@link EncodingDetector} 自动识别。</p>
 *
 * @author HouKunLin
 */
public enum CipherEncoding {
    /**
     * 十六进制编码
     */
    HEX,
    /**
     * Base64 编码
     */
    BASE64;

    /**
     * 获取该编码类型对应的编解码器
     *
     * @return 编解码器实例
     */
    public CipherTextCodec codec() {
        return this == HEX ? HexCodec.INSTANCE : Base64Codec.INSTANCE;
    }

    /**
     * 将密文中的编码标记解析为编码类型
     *
     * <p>支持 {@code hex}、{@code base64}、{@code b64} 三种标记（忽略大小写与首尾空白），
     * 无法识别时返回 {@code null}。</p>
     *
     * @param token 编码标记
     * @return 对应的编码类型；无法识别时返回 null
     */
    public static CipherEncoding fromToken(String token) {
        if (token == null) {
            return null;
        }
        String value = token.trim().toLowerCase(Locale.ROOT);
        if ("hex".equals(value)) {
            return HEX;
        }
        if ("base64".equals(value) || "b64".equals(value)) {
            return BASE64;
        }
        return null;
    }
}
