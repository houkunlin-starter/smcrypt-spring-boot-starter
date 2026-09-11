package com.houkunlin.smcrypt.codec;

import java.util.Base64;

/**
 * Base64 编解码器。
 *
 * @author HouKunLin
 */
public final class Base64Codec implements CipherTextCodec {
    /**
     * 单例实例
     */
    public static final Base64Codec INSTANCE = new Base64Codec();

    private Base64Codec() {
    }

    @Override
    public CipherEncoding encoding() {
        return CipherEncoding.BASE64;
    }

    @Override
    public byte[] decode(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Base64 密文不能为 null");
        }
        String value = text.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Base64 密文不能为空");
        }
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("非法的 Base64 密文：" + value, e);
        }
    }

    @Override
    public String encode(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }
}
