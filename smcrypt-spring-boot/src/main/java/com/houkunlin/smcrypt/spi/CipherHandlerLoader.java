package com.houkunlin.smcrypt.spi;

import com.houkunlin.smcrypt.SmCryptContext;
import com.houkunlin.smcrypt.SmCryptLog;
import com.houkunlin.smcrypt.handler.*;
import org.springframework.core.io.support.SpringFactoriesLoader;

import java.util.*;

/**
 * 密文处理器加载器。
 *
 * <p>按以下顺序加载处理器并注入上下文：</p>
 * <ol>
 *     <li>内置处理器（SM4、SM2、AES、DES、RSA、ECC）；</li>
 *     <li>通过 {@link ServiceLoader}（{@code META-INF/services}）注册的处理器；</li>
 *     <li>通过 {@link SpringFactoriesLoader}（{@code spring.factories}）注册的处理器。</li>
 * </ol>
 *
 * <p>处理器按算法名称去重，后加载者覆盖先加载者，因此业务系统注册的同名算法实现会
 * 覆盖内置实现，便于接入自定义算法或加密机。</p>
 *
 * @author HouKunLin
 */
public class CipherHandlerLoader {

    /**
     * 加载全部处理器并注入上下文
     *
     * @param context 加解密上下文
     * @return 处理器列表（按加载顺序，业务实现覆盖内置实现）
     */
    public List<DecryptHandler> load(SmCryptContext context) {
        ClassLoader classLoader = resolveClassLoader();
        Map<String, DecryptHandler> handlers = new LinkedHashMap<>();
        for (DecryptHandler handler : builtinHandlers()) {
            handlers.put(key(handler), handler);
        }
        loadServiceHandlers(classLoader, handlers);
        loadSpringFactoriesHandlers(classLoader, handlers);

        List<DecryptHandler> result = new ArrayList<>(handlers.values());
        for (DecryptHandler handler : result) {
            if (handler instanceof DecryptHandlerAware) {
                ((DecryptHandlerAware) handler).setContext(context);
            }
        }
        return result;
    }

    /**
     * 创建内置密文处理器列表
     *
     * @return 内置处理器列表
     */
    private List<DecryptHandler> builtinHandlers() {
        return Arrays.asList(
                new Sm4Handler(),
                new Sm2Handler(),
                new AesHandler(),
                new DesHandler(),
                new RsaHandler(),
                new EccHandler());
    }

    /**
     * 加载通过 {@code META-INF/services} 注册的密文处理器
     *
     * @param classLoader 类加载器
     * @param handlers    处理器映射（按算法名称去重）
     */
    private void loadServiceHandlers(ClassLoader classLoader, Map<String, DecryptHandler> handlers) {
        try {
            for (DecryptHandler handler : ServiceLoader.load(DecryptHandler.class, classLoader)) {
                handlers.put(key(handler), handler);
            }
        } catch (Exception e) {
            // 业务方未注册或注册内容异常时跳过该来源，但记录异常，避免静默失败
            SmCryptLog.warn("通过 META-INF/services 加载密文处理器失败，已跳过该来源，不影响内置处理器", e);
        }
    }

    /**
     * 加载通过 {@code spring.factories} 注册的密文处理器
     *
     * @param classLoader 类加载器
     * @param handlers    处理器映射（按算法名称去重）
     */
    private void loadSpringFactoriesHandlers(ClassLoader classLoader, Map<String, DecryptHandler> handlers) {
        try {
            for (DecryptHandler handler : SpringFactoriesLoader.loadFactories(DecryptHandler.class, classLoader)) {
                handlers.put(key(handler), handler);
            }
        } catch (Exception e) {
            // spring.factories 不存在或加载异常时跳过该来源，但记录异常，避免静默失败
            SmCryptLog.warn("通过 spring.factories 加载密文处理器失败，已跳过该来源", e);
        }
    }

    /**
     * 生成处理器去重使用的键（算法名称大写）
     *
     * @param handler 密文处理器
     * @return 大写算法名称；算法名为 null 时返回空串
     */
    private String key(DecryptHandler handler) {
        String algorithm = handler.algorithm();
        return algorithm == null ? "" : algorithm.toUpperCase();
    }

    /**
     * 解析用于 SPI 加载的类加载器
     *
     * @return 线程上下文类加载器；为空时返回本类的类加载器
     */
    private ClassLoader resolveClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }
        return classLoader;
    }
}
