package com.houkunlin.smcrypt.handler;

import com.houkunlin.smcrypt.SmCryptContext;

/**
 * 处理器上下文感知接口。
 *
 * <p>处理器实现本接口后，加载器会在使用前注入 {@link SmCryptContext}，
 * 从而可以访问 Spring 环境属性与密钥解析能力。</p>
 *
 * @author HouKunLin
 */
public interface DecryptHandlerAware {

    /**
     * 注入加解密上下文
     *
     * @param context 加解密上下文
     */
    void setContext(SmCryptContext context);
}
