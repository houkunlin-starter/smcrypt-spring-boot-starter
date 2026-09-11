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

    private List<DecryptHandler> builtinHandlers() {
        return Arrays.asList(
                new Sm4Handler(),
                new Sm2Handler(),
                new AesHandler(),
                new DesHandler(),
                new RsaHandler(),
                new EccHandler());
    }

    private void loadServiceHandlers(ClassLoader classLoader, Map<String, DecryptHandler> handlers) {
        try {
            for (DecryptHandler handler : ServiceLoader.load(DecryptHandler.class, classLoader)) {
                handlers.put(key(handler), handler);
            }
        } catch (Throwable e) {
            // 业务方未注册或注册内容异常时跳过该来源，但记录异常，避免静默失败
            SmCryptLog.warn("通过 META-INF/services 加载密文处理器失败，已跳过该来源，不影响内置处理器", e);
        }
    }

    private void loadSpringFactoriesHandlers(ClassLoader classLoader, Map<String, DecryptHandler> handlers) {
        try {
            for (DecryptHandler handler : SpringFactoriesLoader.loadFactories(DecryptHandler.class, classLoader)) {
                handlers.put(key(handler), handler);
            }
        } catch (Throwable e) {
            // spring.factories 不存在或加载异常时跳过该来源，但记录异常，避免静默失败
            SmCryptLog.warn("通过 spring.factories 加载密文处理器失败，已跳过该来源", e);
        }
    }

    private String key(DecryptHandler handler) {
        String algorithm = handler.algorithm();
        return algorithm == null ? "" : algorithm.toUpperCase();
    }

    private ClassLoader resolveClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }
        return classLoader;
    }
}
