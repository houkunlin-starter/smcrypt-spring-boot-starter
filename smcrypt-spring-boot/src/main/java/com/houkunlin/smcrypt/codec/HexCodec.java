package com.houkunlin.smcrypt.codec;

import org.bouncycastle.util.encoders.DecoderException;
import org.bouncycastle.util.encoders.Hex;

/**
 * 十六进制编解码器。
 *
 * @author HouKunLin
 */
public final class HexCodec implements CipherTextCodec {
    /**
     * 单例实例
     */
    public static final HexCodec INSTANCE = new HexCodec();

    private HexCodec() {
    }

    @Override
    public CipherEncoding encoding() {
        return CipherEncoding.HEX;
    }

    @Override
    public byte[] decode(String text) {
        if (text == null) {
            throw new IllegalArgumentException("十六进制密文不能为 null");
        }
        String value = text.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("十六进制密文不能为空");
        }
        if (value.length() % 2 != 0) {
            throw new IllegalArgumentException("十六进制密文长度必须为偶数：" + value);
        }
        try {
            return Hex.decode(value);
        } catch (DecoderException e) {
            throw new IllegalArgumentException("非法的十六进制密文：" + value, e);
        }
    }

    @Override
    public String encode(byte[] data) {
        return Hex.toHexString(data);
    }
}
