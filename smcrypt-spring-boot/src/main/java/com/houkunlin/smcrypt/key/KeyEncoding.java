package com.houkunlin.smcrypt.key;

import com.houkunlin.smcrypt.codec.Base64Codec;
import com.houkunlin.smcrypt.codec.CipherTextCodec;
import com.houkunlin.smcrypt.codec.HexCodec;

import java.util.Locale;

/**
 * 密钥编码类型。
 *
 * <p>用于显式指定 {@code smcrypt.<算法>.key} / {@code smcrypt.<算法>.mac-key} 的编码方式，
 * 避免自动识别带来的歧义（例如全部由十六进制字符组成的口令）。通过
 * {@code smcrypt.<算法>.key-encoding} / {@code smcrypt.<算法>.mac-key-encoding} 配置：</p>
 * <ul>
 *     <li>{@link #HEX}：十六进制；</li>
 *     <li>{@link #BASE64}：Base64；</li>
 *     <li>{@link #PLAIN}：原始文本（UTF-8 字节，保留空白，适用于口令）。</li>
 * </ul>
 *
 * @author HouKunLin
 */
public enum KeyEncoding {
    /**
     * 十六进制
     */
    HEX,
    /**
     * Base64
     */
    BASE64,
    /**
     * 原始文本（UTF-8 字节，保留空白）
     */
    PLAIN;

    /**
     * 获取该编码对应的编解码器
     *
     * @return HEX / BASE64 的编解码器
     * @throws IllegalStateException {@link #PLAIN} 无编解码器时抛出
     */
    public CipherTextCodec codec() {
        switch (this) {
            case HEX:
                return HexCodec.INSTANCE;
            case BASE64:
                return Base64Codec.INSTANCE;
            default:
                throw new IllegalStateException("PLAIN 编码不使用编解码器");
        }
    }

    /**
     * 解析密钥编码标记
     *
     * <p>支持 {@code hex}、{@code base64} / {@code b64}、{@code plain} / {@code raw} / {@code utf8} / {@code text}
     * （忽略大小写与首尾空白）。传入 null 或空白返回 null；无法识别的非空值抛出异常。</p>
     *
     * @param token 配置值
     * @return 对应的编码；未配置时返回 null
     * @throws IllegalArgumentException 配置了无法识别的编码时抛出
     */
    public static KeyEncoding fromToken(String token) {
        if (token == null) {
            return null;
        }
        String value = token.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            return null;
        }
        switch (value) {
            case "hex":
                return HEX;
            case "base64":
            case "b64":
                return BASE64;
            case "plain":
            case "raw":
            case "utf8":
            case "utf-8":
            case "text":
                return PLAIN;
            default:
                throw new IllegalArgumentException("不支持的密钥编码：" + token + "，仅支持 hex / base64 / plain");
        }
    }
}
