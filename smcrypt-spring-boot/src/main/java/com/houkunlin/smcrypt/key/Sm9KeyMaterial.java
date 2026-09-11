package com.houkunlin.smcrypt.key;

import java.util.Arrays;

/**
 * SM9 密钥材料。
 *
 * <p>由 {@link SmCryptKeyGenerator#generateSm9Key(byte[], byte)} 生成，包含 KGC 主密钥对与用户私钥，
 * 可直接用于 {@code smcrypt.sm9.*} 配置：</p>
 * <ul>
 *     <li>{@link #masterPrivateKey()} → 对应配置 {@code smcrypt.sm9.private-key}（用户私钥）；</li>
 *     <li>{@link #masterPublicKey()} → 对应配置 {@code smcrypt.sm9.master-public-key}；</li>
 *     <li>{@link #userPrivateKey()} → 对应配置 {@code smcrypt.sm9.private-key}。</li>
 * </ul>
 *
 * @author HouKunLin
 */
public class Sm9KeyMaterial {
    private final String masterPrivateKey;
    private final String masterPublicKey;
    private final String userPrivateKey;
    private final byte[] identity;
    private final byte hid;

    public Sm9KeyMaterial(String masterPrivateKey, String masterPublicKey, String userPrivateKey,
                          byte[] identity, byte hid) {
        this.masterPrivateKey = masterPrivateKey;
        this.masterPublicKey = masterPublicKey;
        this.userPrivateKey = userPrivateKey;
        this.identity = Arrays.copyOf(identity, identity.length);
        this.hid = hid;
    }

    /**
     * 获取 KGC 主私钥（hex，32 字节标量），仅供 KGC 保存，用于派生用户私钥
     *
     * @return 主私钥（hex）
     */
    public String masterPrivateKey() {
        return masterPrivateKey;
    }

    /**
     * 获取主公钥（Base64，G1 点），对应配置 {@code smcrypt.sm9.master-public-key}
     *
     * @return 主公钥（Base64）
     */
    public String masterPublicKey() {
        return masterPublicKey;
    }

    /**
     * 获取用户私钥（Base64，G2 点），对应配置 {@code smcrypt.sm9.private-key}
     *
     * @return 用户私钥（Base64）
     */
    public String userPrivateKey() {
        return userPrivateKey;
    }

    /**
     * 获取身份字符串的字节
     *
     * @return 身份字节
     */
    public byte[] identity() {
        return Arrays.copyOf(identity, identity.length);
    }

    /**
     * 获取私钥生成函数标识
     *
     * @return hid
     */
    public byte hid() {
        return hid;
    }
}
