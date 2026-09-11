package com.houkunlin.smcrypt.codec;

/**
 * 密文解析结果。
 *
 * <p>封装从密文串中解析出的编码类型与去除编码前缀后的实际密文内容。</p>
 *
 * @author HouKunLin
 */
public class CipherPayload {
    /**
     * 密文编码类型
     */
    private final CipherEncoding encoding;
    /**
     * 去除编码前缀后的实际密文内容
     */
    private final String text;

    /**
     * 构造密文解析结果
     *
     * @param encoding 密文编码类型
     * @param text     去除编码前缀后的实际密文内容
     */
    public CipherPayload(CipherEncoding encoding, String text) {
        this.encoding = encoding;
        this.text = text;
    }

    /**
     * 获取密文编码类型
     *
     * @return 编码类型
     */
    public CipherEncoding encoding() {
        return encoding;
    }

    /**
     * 获取去除编码前缀后的实际密文内容
     *
     * @return 实际密文内容
     */
    public String text() {
        return text;
    }

    /**
     * 将密文内容按解析出的编码解码为字节数组
     *
     * @return 解码后的字节数组
     */
    public byte[] decode() {
        return encoding.codec().decode(text);
    }

    @Override
    public String toString() {
        return "CipherPayload{encoding=" + encoding + ", text='" + text + "'}";
    }
}
