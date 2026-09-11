package com.houkunlin.smcrypt;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import org.slf4j.Logger;
import org.slf4j.spi.MDCAdapter;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 启动早期独立日志工具。
 *
 * <p>本类在 Spring Boot 全局日志系统初始化之前执行（{@code EnvironmentPostProcessor} 阶段），
 * 为避免手动改动全局日志系统影响 Spring 后续初始化，这里创建一个与全局无关的独立
 * {@link LoggerContext}，仅加载简单的早期日志配置，本类日志走该独立上下文，打印简单格式
 * 并写入独立日志文件；全局日志系统不受任何影响，后续仍由 Spring 正常初始化。</p>
 *
 * <p>独立上下文初始化失败或找不到配置文件时，日志回退到 {@link System#out}/{@link System#err}。</p>
 *
 * @author HouKunLin
 */
public class SmCryptLogback {
    /**
     * 日志输出时间格式
     */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * 启动早期日志配置文件（候选位置）
     * <p>
     * 独立 LoggerContext 加载的日志配置文件，仅用于本类在 Spring 全局日志系统初始化前的
     * 早期日志输出。按优先级依次查找 {@code file:}（当前工作目录）与 {@code classpath:}
     * 位置的 {@code logback-smcrypt.xml}：当前目录配置优先于 classpath 内置配置，便于部署时
     * 通过外部文件覆盖默认日志行为。找到第一个存在的位置即使用；全部不存在则回退为 null，
     * 此时本类日志回退到 {@link System#out}/{@link System#err} 输出。
     */
    private static final String[] EARLY_LOG_CONFIG_LOCATIONS = {
            "file:logback-smcrypt.xml",
            "classpath:logback-smcrypt.xml",
    };

    private final FileSystemResourceLoader resourceLoader = new FileSystemResourceLoader();
    private LoggerContext earlyContext;
    private boolean loggingReady = false;
    private Logger log;

    public SmCryptLogback(ConfigurableEnvironment environment, SpringApplication application, String logName) {
        this.initLogging(environment, application.getClassLoader(), logName);
    }

    /**
     * 初始化本类的独立日志上下文
     * <p>
     * {@link org.springframework.boot.env.EnvironmentPostProcessor} 在 Spring Boot 的
     * {@code LoggingApplicationListener} 之前执行，此时全局日志系统尚未加载 {@code logback-spring.xml}。
     * 这里创建一个与全局无关的独立 {@link LoggerContext}，仅加载简单的早期日志配置，本类日志走该
     * 独立上下文，打印简单格式并写入独立日志文件；全局日志系统不受任何影响。
     * <p>
     * 独立上下文初始化时需注意：
     * <ul>
     *     <li>显式设置 {@link LogbackMDCAdapter}，否则日志事件访问 MDC 时抛出 NPE，导致所有
     *         appender 输出失败；</li>
     *     <li>从 Environment 注入 {@code APPLICATION_NAME}、{@code LOG_PATH} 及滚动策略等属性，
     *         供 {@code logback-smcrypt.xml} 的占位符解析（独立上下文不经过 Spring Boot 的
     *         {@code LoggingSystemProperties}）；</li>
     *     <li>配置解析后需显式 {@code start()}，否则 appender 未激活，日志事件被直接丢弃。</li>
     * </ul>
     * <p>
     * 独立上下文初始化失败或找不到配置文件时，{@link #loggingReady} 保持 false，
     * 后续日志回退到 {@link System#out}/{@link System#err}。
     *
     * @param environment 当前 Spring 环境
     * @param classLoader 用于加载 classpath 资源的类加载器
     * @param logName     日志名称
     */
    private void initLogging(ConfigurableEnvironment environment, ClassLoader classLoader, String logName) {
        try {
            Resource configResource = resolveEarlyLogConfig(classLoader);
            if (configResource == null) {
                // 未找到早期日志配置文件：回退到 System.out/err
                return;
            }
            earlyContext = new LoggerContext();
            earlyContext.setName(getClass().getName() + "-early");
            // 注入 Environment 中的日志相关属性，供 logback-smcrypt.xml 的 ${...} 占位符解析。
            applyLoggingProperties(earlyContext, environment);
            // 手动创建的 LoggerContext 在 logback 1.3+ 中需显式设置 MDC 适配器，否则日志事件在
            // prepareForDeferredProcessing 访问 MDC 时会抛 NullPointerException，导致所有 appender
            // 输出失败。logback 1.2.x（Spring Boot 2.x）无此方法且由 SLF4J 的 StaticMDCBinder 提供
            // 适配器，故通过反射调用以兼容 1.2/1.4/1.5 三个版本。
            initMdcAdapter(earlyContext);
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(earlyContext);
            try (InputStream inputStream = configResource.getInputStream()) {
                configurator.doConfigure(inputStream);
            }
            // JoranConfigurator 只解析配置，必须显式 start 才会激活 context 及全部 appender，
            // 否则日志事件会被未启动的 appender 直接丢弃（控制台、日志文件均无输出）
            earlyContext.start();
            log = earlyContext.getLogger(logName);
            loggingReady = true;
            logMessage(LogLevel.INFO, "[SMCRYPT] 日志初始化完毕，日志配置文件：{}", configResource.getURI());
        } catch (Exception e) {
            loggingReady = false;
            // 初始化失败：保留 loggingReady=false，使后续日志回退到 System.out/err，
            // 避免日志系统自身故障导致解密流程的所有日志静默丢失
            logMessage(LogLevel.ERROR, "[SMCRYPT] 初始化本类独立日志上下文失败，后续日志将回退到控制台输出: {}", e.getMessage(), e);
        }
    }

    /**
     * 为独立 LoggerContext 设置 MDC 适配器（兼容 logback 1.2/1.4/1.5）
     * <p>
     * logback 1.3+ 的手动 {@link LoggerContext} 默认 MDC 适配器为 null，需显式设置；
     * logback 1.2.x（Spring Boot 2.x）无 {@code setMDCAdapter} 方法，由 SLF4J 提供适配器。
     * 因此通过反射调用：方法存在时设置，不存在时记录调试日志后跳过。
     *
     * @param context 独立 LoggerContext
     */
    private void initMdcAdapter(LoggerContext context) {
        try {
            Method method = LoggerContext.class.getMethod("setMDCAdapter", MDCAdapter.class);
            method.invoke(context, new LogbackMDCAdapter());
        } catch (NoSuchMethodException e) {
            // logback 1.2.x 无需显式设置，记录调试日志便于排查
            logMessage(LogLevel.DEBUG, "[SMCRYPT] 当前 logback 版本不存在 setMDCAdapter 方法，跳过 MDC 适配器设置：{}", e.getMessage());
        } catch (Exception e) {
            // 设置失败时不影响正常日志输出，仅当后续访问 MDC 时可能受影响
            logMessage(LogLevel.WARN, "[SMCRYPT] 设置 logback MDC 适配器失败，后续日志访问 MDC 时可能异常", e);
        }
    }

    /**
     * 关闭本类独立的早期日志上下文
     */
    public void closeLogging() {
        if (earlyContext != null) {
            try {
                earlyContext.stop();
            } catch (Exception e) {
                loggingReady = false;
                // 停止失败不影响解密流程结果
                logMessage(LogLevel.ERROR, "停止 SMCRYPT 日志上下文失败", e);
            }
            earlyContext = null;
        }
        loggingReady = false;
        log = null;
    }

    /**
     * 将 Environment 中的日志相关属性注入独立 LoggerContext
     * <p>
     * 独立 LoggerContext 不经过 Spring Boot 的 {@code LoggingSystemProperties}，因此
     * {@code logback-smcrypt.xml} 中引用的 {@code ${APPLICATION_NAME}}、{@code ${LOG_PATH}}、
     * {@code ${LOG_FILE}}、{@code ${LOG_TEMP}}、{@code ${LOGBACK_ROLLINGPOLICY_*}}、
     * {@code ${CONSOLE_LOG_CHARSET}}、{@code ${FILE_LOG_CHARSET}} 等占位符无法解析。
     * 这里从 {@code environment} 读取对应配置并写入 context 属性，使 logback 能够正确拼接
     * 日志文件路径并应用滚动策略。
     *
     * @param context     独立 LoggerContext
     * @param environment 当前 Spring 环境
     */
    private void applyLoggingProperties(LoggerContext context, ConfigurableEnvironment environment) {
        putIfAbsent(context, "APPLICATION_NAME", environment.resolvePlaceholders("${spring.application.name:spring}"));
        putIfAbsent(context, "LOG_PATH", environment.resolvePlaceholders("${logging.file.path:logs}"));
        putIfAbsent(context, "LOG_FILE", environment.resolvePlaceholders("${logging.file.name:spring.log}"));
        putIfAbsent(context, "LOG_TEMP", System.getProperty("java.io.tmpdir", ""));
        putIfAbsent(context, "CONSOLE_LOG_CHARSET", environment.resolvePlaceholders("${logging.charset.console:}"));
        putIfAbsent(context, "FILE_LOG_CHARSET", environment.resolvePlaceholders("${logging.charset.file:}"));
        putIfAbsent(context, "LOGBACK_ROLLINGPOLICY_FILE_NAME_PATTERN",
                environment.resolvePlaceholders("${logging.logback.rollingpolicy.file-name-pattern:}"));
        putIfAbsent(context, "LOGBACK_ROLLINGPOLICY_CLEAN_HISTORY_ON_START",
                environment.resolvePlaceholders("${logging.logback.rollingpolicy.clean-history-on-start:false}"));
        putIfAbsent(context, "LOGBACK_ROLLINGPOLICY_MAX_FILE_SIZE",
                environment.resolvePlaceholders("${logging.logback.rollingpolicy.max-file-size:10MB}"));
        putIfAbsent(context, "LOGBACK_ROLLINGPOLICY_TOTAL_SIZE_CAP",
                environment.resolvePlaceholders("${logging.logback.rollingpolicy.total-size-cap:0}"));
        putIfAbsent(context, "LOGBACK_ROLLINGPOLICY_MAX_HISTORY",
                environment.resolvePlaceholders("${logging.logback.rollingpolicy.max-history:7}"));
    }

    /**
     * 仅当 context 中不存在指定属性时写入，避免覆盖已有值
     *
     * @param context 独立 LoggerContext
     * @param name    属性名
     * @param value   属性值
     */
    private void putIfAbsent(LoggerContext context, String name, String value) {
        if (value != null && !value.isEmpty() && context.getProperty(name) == null) {
            context.putProperty(name, value);
        }
    }

    /**
     * 解析早期日志配置文件
     * <p>
     * 按 {@link #EARLY_LOG_CONFIG_LOCATIONS} 的优先级，依次检查当前工作目录（{@code file:}）
     * 与 classpath 下的 {@code logback-smcrypt.xml}，返回第一个已确认存在的 {@link Resource}；
     * 所有位置都不存在时返回 {@code null}。
     *
     * @param classLoader 用于加载 classpath 资源的类加载器
     * @return 存在的配置文件资源；找不到时返回 null
     */
    private Resource resolveEarlyLogConfig(ClassLoader classLoader) {
        resourceLoader.setClassLoader(classLoader);
        for (String location : EARLY_LOG_CONFIG_LOCATIONS) {
            Resource resource = resourceLoader.getResource(location);
            if (resource.exists()) {
                return resource;
            }
        }
        return null;
    }

    /**
     * 日志输出统一入口
     * <p>
     * 本类独立日志上下文初始化成功（{@link #loggingReady} 为 true 且 {@link #log} 非空）时，
     * 走独立 LoggerContext；否则回退到 {@link System#out}/{@link System#err}，输出带时间戳与
     * {@code [级别]} 前缀的文本，保证解密流程的关键日志不丢失。
     *
     * @param level  日志级别
     * @param format 日志格式（支持 {@code {}} 占位符）
     * @param args   占位符参数；若最后一个参数为 {@link Throwable} 则追加堆栈输出
     */
    public void logMessage(LogLevel level, String format, Object... args) {
        if (loggingReady && log != null) {
            switch (level) {
                case DEBUG: {
                    log.debug(format, args);
                    break;
                }
                case INFO: {
                    log.info(format, args);
                    break;
                }
                case WARN: {
                    log.warn(format, args);
                    break;
                }
                case ERROR: {
                    log.error(format, args);
                    break;
                }
                default: {
                    break;
                }
            }
            return;
        }
        // 回退输出：解析 {} 占位符并打印到控制台
        StringBuilder message = new StringBuilder();
        int argIndex = 0;
        for (int i = 0; i < format.length(); i++) {
            if (format.charAt(i) == '{' && i + 1 < format.length() && format.charAt(i + 1) == '}' && argIndex < args.length) {
                message.append(args[argIndex++]);
                i++;
            } else {
                message.append(format.charAt(i));
            }
        }
        Throwable throwable = getThrowable(args);
        String line = TIME_FORMATTER.format(LocalDateTime.now()) + " [" + level.name() + "] " + message;
        if (level == LogLevel.ERROR) {
            System.err.println(line);
            if (throwable != null) {
                throwable.printStackTrace(System.err);
            }
        } else {
            System.out.println(line);
        }
    }

    private Throwable getThrowable(Object... args) {
        if (args.length > 0) {
            Object last = args[args.length - 1];
            if (last instanceof Throwable) {
                return (Throwable) last;
            }
        }
        return null;
    }
}
