package com.houkunlin.smcrypt;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Provider;
import java.security.Security;

/**
 * BouncyCastle 提供者持有工具。
 *
 * <p>内部持有单例 {@link BouncyCastleProvider}，加解密时通过显式传入该 Provider 的方式使用，
 * 避免污染应用的全局 JCE Provider 列表（应用可能自行注册了其他版本的 BouncyCastle）。
 * 若需要全局注册（例如第三方组件按名称查找 {@code BC}），可调用 {@link #ensureRegistered()}。</p>
 *
 * @author HouKunLin
 */
public final class BouncyCastleSupport {
    private static final BouncyCastleProvider PROVIDER = new BouncyCastleProvider();

    private BouncyCastleSupport() {
    }

    /**
     * 获取 BouncyCastle Provider 单例
     *
     * @return Provider 实例
     */
    public static Provider provider() {
        return PROVIDER;
    }

    /**
     * 确保 BouncyCastle Provider 已注册到全局 JCE
     */
    public static void ensureRegistered() {
        if (Security.getProvider(PROVIDER.getName()) == null) {
            Security.addProvider(PROVIDER);
        }
    }
}
