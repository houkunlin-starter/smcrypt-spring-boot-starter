package com.houkunlin.smcrypt.codec;

/**
 * 密文编解码器。
 *
 * <p>负责在字符串形式的密文与字节数组之间相互转换。不同实现对应不同的编码方式，
 * 参见 {@link CipherEncoding}。</p>
 *
 * @author HouKunLin
 */
public interface CipherTextCodec {

    /**
     * 获取该编解码器对应的编码类型
     *
     * @return 编码类型
     */
    CipherEncoding encoding();

    /**
     * 将字符串密文解码为字节数组
     *
     * @param text 字符串密文
     * @return 解码后的字节数组
     * @throws IllegalArgumentException 密文格式非法时抛出
     */
    byte[] decode(String text);

    /**
     * 将字节数组编码为字符串密文
     *
     * @param data 字节数组
     * @return 编码后的字符串密文
     */
    String encode(byte[] data);
}
