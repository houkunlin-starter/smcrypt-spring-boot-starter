package com.houkunlin.smcrypt.key;

import com.houkunlin.smcrypt.SmCryptLog;
import com.houkunlin.smcrypt.codec.CipherEncoding;
import com.houkunlin.smcrypt.codec.EncodingDetector;

import java.nio.charset.StandardCharsets;

/**
 * 对称密钥解码工具。
 *
 * <p>密钥内容优先按十六进制解码，否则按 Base64 解码；两者均失败时按 UTF-8 原始字节处理，
 * 以兼容直接使用口令的场景。解码前会移除所有空白字符，允许密钥文件中存在换行。</p>
 *
 * @author HouKunLin
 */
public final class KeyCodec {

    /**
     * 工具类，禁止实例化
     */
    private KeyCodec() {
    }

    /**
     * 将密钥文本解码为字节数组
     *
     * @param keyText 密钥文本（hex / Base64 / 原始口令）
     * @return 密钥字节数组
     */
    public static byte[] decodeKey(String keyText) {
        if (keyText == null) {
            throw new IllegalArgumentException("密钥内容不能为 null");
        }
        String value = keyText.replaceAll("\\s", "");
        if (value.isEmpty()) {
            throw new IllegalArgumentException("密钥内容不能为空");
        }
        CipherEncoding encoding = EncodingDetector.detectEncoding(value);
        try {
            return encoding.codec().decode(value);
        } catch (RuntimeException e) {
            SmCryptLog.debug("密钥内容无法按 {} 解码，将按 UTF-8 原始字节处理", encoding, e);
            return value.getBytes(StandardCharsets.UTF_8);
        }
    }
}
