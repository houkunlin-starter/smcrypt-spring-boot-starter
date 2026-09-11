package com.houkunlin.smcrypt.codec;

import java.util.regex.Pattern;

/**
 * 密文编码自动识别器。
 *
 * <p>识别规则（按优先级）：</p>
 * <ol>
 *     <li>显式编码前缀：密文内容以 {@code hex,}、{@code base64,} 或 {@code b64,} 开头
 *         （忽略大小写）时，直接采用该编码并去除前缀；</li>
 *     <li>自动识别：内容全部为十六进制字符且长度为 2 的倍数时，判定为 {@link CipherEncoding#HEX}；</li>
 *     <li>其余情况判定为 {@link CipherEncoding#BASE64}。</li>
 * </ol>
 *
 * @author HouKunLin
 */
public final class EncodingDetector {
    /**
     * 十六进制字符串匹配（仅十六进制字符）
     */
    private static final Pattern HEX_PATTERN = Pattern.compile("^[0-9a-fA-F]+$");

    private EncodingDetector() {
    }

    /**
     * 解析密文串的编码类型并提取实际密文内容
     *
     * @param cipherText 密文内容（不含 {@code ALGENC(...)} 包裹）
     * @return 解析结果，包含编码类型与实际密文内容
     */
    public static CipherPayload detect(String cipherText) {
        if (cipherText == null) {
            throw new IllegalArgumentException("密文内容不能为 null");
        }
        int comma = cipherText.indexOf(',');
        if (comma > 0) {
            CipherEncoding explicit = CipherEncoding.fromToken(cipherText.substring(0, comma));
            if (explicit != null) {
                return new CipherPayload(explicit, cipherText.substring(comma + 1).trim());
            }
        }
        String value = cipherText.trim();
        return new CipherPayload(detectEncoding(value), value);
    }

    /**
     * 自动识别字符串内容的编码类型
     *
     * <p>内容全部为十六进制字符且长度为 2 的倍数时判定为十六进制，否则判定为 Base64。</p>
     *
     * @param text 待识别的内容
     * @return 识别出的编码类型
     */
    public static CipherEncoding detectEncoding(String text) {
        if (text == null || text.isEmpty()) {
            return CipherEncoding.BASE64;
        }
        if (text.length() % 2 == 0 && HEX_PATTERN.matcher(text).matches()) {
            return CipherEncoding.HEX;
        }
        return CipherEncoding.BASE64;
    }
}
