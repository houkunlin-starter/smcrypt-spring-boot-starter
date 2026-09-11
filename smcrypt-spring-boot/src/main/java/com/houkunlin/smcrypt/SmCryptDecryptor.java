package com.houkunlin.smcrypt;

import com.houkunlin.smcrypt.handler.DecryptHandler;
import com.houkunlin.smcrypt.spi.CipherHandlerLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置文件密文解密引擎（版本无关）。
 *
 * <p>在 Spring Boot 的 {@code EnvironmentPostProcessor} 阶段执行，遍历
 * {@link ConfigurableEnvironment} 中所有 {@link PropertySource}，将 {@code {算法}ENC(...)}
 * 形式的密文解密为明文并替换原值。</p>
 *
 * <p>工作流程：</p>
 * <ol>
 *     <li>加载全部密文处理器（内置 + 业务通过 SPI 注册的处理器）；</li>
 *     <li>快速判断是否存在可识别的密文，无则跳过；</li>
 *     <li>遍历 {@link MapPropertySource}（含 {@link OriginTrackedMapPropertySource}），
 *         对每个字符串值寻找匹配的处理器并解密；</li>
 *     <li>替换原 PropertySource，{@link OriginTrackedMapPropertySource} 会保留来源信息。</li>
 * </ol>
 *
 * <p>各版本 Starter 中的 {@code EnvironmentPostProcessor} 实现仅负责将本引擎委托调用，
 * 从而屏蔽 Spring Boot 2/3/4 之间的接口包名差异。</p>
 *
 * @author HouKunLin
 */
public class SmCryptDecryptor {

    /**
     * 执行配置文件密文解密
     *
     * @param environment 当前 Spring 环境
     * @param application 当前 Spring 应用
     */
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        SmCryptLogback logback = new SmCryptLogback(environment, application, getClass().getName());
        SmCryptLog.setActive(logback);
        try {
            logback.logMessage(LogLevel.INFO, "[SMCRYPT] 正在执行 {} ，即将对配置文件属性值的密文进行解密处理", getClass().getName());
            SmCryptContext context = SmCryptContext.fromEnvironment(environment);
            List<DecryptHandler> handlers = new CipherHandlerLoader().load(context);
            MutablePropertySources propertySources = environment.getPropertySources();
            if (!hasCipherText(propertySources, handlers)) {
                logback.logMessage(LogLevel.INFO, "[SMCRYPT] 未检测到配置文件中存在加密的配置属性值密文，跳过配置文件解密，可以忽略相关日志提示");
                return;
            }

            Map<String, PropertySource<?>> replacements = new HashMap<>();
            for (PropertySource<?> source : propertySources) {
                if (source instanceof OriginTrackedMapPropertySource) {
                    handler(replacements, (OriginTrackedMapPropertySource) source, handlers, logback);
                } else if (source instanceof MapPropertySource) {
                    handler(replacements, (MapPropertySource) source, handlers, logback);
                }
            }
            replacements.forEach(propertySources::replace);

            if (!replacements.isEmpty()) {
                logback.logMessage(LogLevel.INFO, "[SMCRYPT] 配置文件解密完成，共处理 {} 个 PropertySource", replacements.size());
            }
        } finally {
            logback.logMessage(LogLevel.INFO, "[SMCRYPT] 执行完毕，系统即将开始启动");
            // 解密流程结束后关闭本类独立的早期日志上下文，释放文件句柄
            SmCryptLog.clearActive();
            logback.closeLogging();
        }
    }

    /**
     * 处理 {@link OriginTrackedMapPropertySource} 中的加密属性
     *
     * <p>解密后使用 {@link OriginTrackedValue} 包装结果，保留属性来源信息（如 YAML 行号），
     * 确保 Spring Boot 错误报告和 actuator 能正确溯源。</p>
     *
     * @param replacements 待替换的 PropertySource 映射
     * @param source       当前遍历的 OriginTrackedMapPropertySource
     * @param handlers     密文处理器列表
     * @param logback      日志工具
     */
    private void handler(Map<String, PropertySource<?>> replacements, OriginTrackedMapPropertySource source,
                         List<DecryptHandler> handlers, SmCryptLogback logback) {
        String simpleName = source.getClass().getSimpleName();
        Map<String, Object> decrypted = new HashMap<>();
        boolean hasEncrypted = false;
        for (String name : source.getPropertyNames()) {
            Object raw = source.getProperty(name);
            if (!(raw instanceof String)) {
                continue;
            }
            String value = (String) raw;
            DecryptHandler handler = findHandler(handlers, value);
            if (handler == null) {
                continue;
            }
            try {
                String decryptedValue = handler.getDecryptText(value);
                Origin origin = source.getOrigin(name);
                if (origin == null) {
                    decrypted.put(name, OriginTrackedValue.of(decryptedValue));
                } else {
                    decrypted.put(name, OriginTrackedValue.of(decryptedValue, origin));
                }
                hasEncrypted = true;
                logback.logMessage(LogLevel.INFO, "[SMCRYPT] 配置类型：{}，解密配置属性：{}，算法：{}，配置来源：{}",
                        simpleName, name, handler.algorithm(), source.getName());
            } catch (Exception e) {
                logback.logMessage(LogLevel.ERROR, "[SMCRYPT] 配置类型：{}，无法解密配置属性: {}，算法：{}，原始值: {}，配置来源：{}",
                        simpleName, name, handler.algorithm(), value, source.getName(), e);
            }
        }
        if (hasEncrypted) {
            Map<String, Object> merged = new HashMap<>(source.getSource());
            merged.putAll(decrypted);
            replacements.put(source.getName(), new OriginTrackedMapPropertySource(source.getName(), merged));
        }
    }

    /**
     * 处理普通 {@link MapPropertySource} 中的加密属性
     *
     * @param replacements 待替换的 PropertySource 映射
     * @param source       当前遍历的 MapPropertySource
     * @param handlers     密文处理器列表
     * @param logback      日志工具
     */
    private void handler(Map<String, PropertySource<?>> replacements, MapPropertySource source,
                         List<DecryptHandler> handlers, SmCryptLogback logback) {
        String simpleName = source.getClass().getSimpleName();
        Map<String, Object> decrypted = new HashMap<>();
        boolean hasEncrypted = false;
        for (String name : source.getPropertyNames()) {
            Object raw = source.getProperty(name);
            if (!(raw instanceof String)) {
                continue;
            }
            String value = (String) raw;
            DecryptHandler handler = findHandler(handlers, value);
            if (handler == null) {
                continue;
            }
            try {
                String decryptedValue = handler.getDecryptText(value);
                decrypted.put(name, decryptedValue);
                hasEncrypted = true;
                logback.logMessage(LogLevel.INFO, "[SMCRYPT] 配置类型：{}，解密配置属性：{}，算法：{}，配置来源：{}",
                        simpleName, name, handler.algorithm(), source.getName());
            } catch (Exception e) {
                logback.logMessage(LogLevel.ERROR, "[SMCRYPT] 配置类型：{}，无法解密配置属性: {}，算法：{}，原始值: {}，配置来源：{}",
                        simpleName, name, handler.algorithm(), value, source.getName(), e);
            }
        }
        if (hasEncrypted) {
            Map<String, Object> merged = new HashMap<>(source.getSource());
            merged.putAll(decrypted);
            replacements.put(source.getName(), new MapPropertySource(source.getName(), merged));
        }
    }

    /**
     * 检查 PropertySource 集合中是否存在可识别的密文
     *
     * @param propertySources 待检查的 PropertySource 集合
     * @param handlers        密文处理器列表
     * @return 存在密文属性时返回 true
     */
    private boolean hasCipherText(MutablePropertySources propertySources, List<DecryptHandler> handlers) {
        for (PropertySource<?> propertySource : propertySources) {
            if (propertySource instanceof MapPropertySource) {
                MapPropertySource source = (MapPropertySource) propertySource;
                for (String name : source.getPropertyNames()) {
                    Object raw = propertySource.getProperty(name);
                    if (raw instanceof String && findHandler(handlers, (String) raw) != null) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 查找能够处理指定配置值的密文处理器
     *
     * @param handlers 密文处理器列表
     * @param value    配置值
     * @return 匹配的处理器；无匹配时返回 null
     */
    private DecryptHandler findHandler(List<DecryptHandler> handlers, String value) {
        for (DecryptHandler handler : handlers) {
            if (handler.support(value)) {
                return handler;
            }
        }
        return null;
    }
}
