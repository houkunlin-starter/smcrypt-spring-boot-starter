package com.houkunlin.smcrypt;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import org.slf4j.Logger;
import org.slf4j.spi.MDCAdapter;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Iterator;

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

    /**
     * 早期日志文件 appender 名称
     */
    private static final String FILE_APPENDER_NAME = "EARLY_FILE";
    /**
     * 应用名缺省值（用于日志文件名）
     */
    private static final String DEFAULT_APPLICATION_NAME = "spring";
    /**
     * 早期日志格式缺省值（logback 配置未定义 {@code EARLY_LOG_PATTERN} 时使用）
     */
    private static final String DEFAULT_LOG_PATTERN = "%d{yyyy-MM-dd HH:mm:ss.SSS} %5level %logger{36} : %msg%n";
    /**
     * 早期日志文件名后缀
     */
    private static final String LOG_FILE_SUFFIX = ".smcrypt.log";
    /**
     * 早期日志滚动文件名后缀
     */
    private static final String LOG_FILE_PATTERN_SUFFIX = ".smcrypt.%d{yyyy-MM-dd}.%i.log";

    /**
     * 资源加载器，用于查找早期日志配置文件
     */
    private final FileSystemResourceLoader resourceLoader = new FileSystemResourceLoader();
    /**
     * 独立日志上下文
     */
    private LoggerContext earlyContext;
    /**
     * 独立日志上下文是否初始化成功
     */
    private boolean loggingReady = false;
    /**
     * 是否已启用文件输出（避免重复挂载文件 appender）
     */
    private boolean fileLoggingEnabled = false;
    /**
     * 独立日志上下文中的 Logger
     */
    private Logger log;

    /**
     * 构造早期日志工具并立即初始化独立日志上下文
     *
     * @param environment 当前 Spring 环境
     * @param application 当前 Spring 应用（用于获取类加载器）
     * @param logName     日志名称
     */
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
        fileLoggingEnabled = false;
        log = null;
    }

    /**
     * 启用早期日志文件输出
     * <p>
     * 文件 appender 由代码按需创建并挂载，而非在 {@code logback-smcrypt.xml} 中静态声明，目的是
     * 避免应用未使用密文时仍在 {@code logging.file.path} 下生成 {@code <应用名>.smcrypt.log} 文件。
     * 因此本方法应在确认存在密文后再调用；重复调用只生效一次，独立日志上下文不可用时不做处理。
     * <p>
     * 文件路径、滚动策略沿用 {@code logback-smcrypt.xml} 与 Spring 标准配置解析出的上下文属性
     * （{@code BASE_LOG_PATH}、{@code APPLICATION_NAME}、{@code EARLY_LOG_PATTERN}、
     * {@code LOGBACK_ROLLINGPOLICY_*}、{@code FILE_LOG_CHARSET}），保持与原有行为一致。
     */
    public void enableFileLogging() {
        if (fileLoggingEnabled || !loggingReady || earlyContext == null) {
            return;
        }
        fileLoggingEnabled = true;
        try {
            if (hasAttachedFileAppender()) {
                // 外部覆盖的 logback-smcrypt.xml 已挂载同名文件 appender，避免重复创建
                return;
            }
            String basePath = resolveBaseLogPath();
            String applicationName = propertyOrDefault("APPLICATION_NAME", DEFAULT_APPLICATION_NAME);
            String pattern = propertyOrDefault("EARLY_LOG_PATTERN", DEFAULT_LOG_PATTERN);

            PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setContext(earlyContext);
            encoder.setPattern(pattern);
            encoder.setCharset(resolveFileCharset());
            encoder.start();

            RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
            appender.setContext(earlyContext);
            appender.setName(FILE_APPENDER_NAME);
            appender.setFile(basePath + "/" + applicationName + LOG_FILE_SUFFIX);
            appender.setAppend(true);
            appender.setEncoder(encoder);

            SizeAndTimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new SizeAndTimeBasedRollingPolicy<>();
            rollingPolicy.setContext(earlyContext);
            rollingPolicy.setParent(appender);
            rollingPolicy.setFileNamePattern(basePath + "/" + applicationName + LOG_FILE_PATTERN_SUFFIX);
            rollingPolicy.setCleanHistoryOnStart(
                    Boolean.parseBoolean(propertyOrDefault("LOGBACK_ROLLINGPOLICY_CLEAN_HISTORY_ON_START", "false")));
            rollingPolicy.setMaxFileSize(FileSize.valueOf(propertyOrDefault("LOGBACK_ROLLINGPOLICY_MAX_FILE_SIZE", "10MB")));
            rollingPolicy.setTotalSizeCap(FileSize.valueOf(propertyOrDefault("LOGBACK_ROLLINGPOLICY_TOTAL_SIZE_CAP", "0")));
            rollingPolicy.setMaxHistory(Integer.parseInt(propertyOrDefault("LOGBACK_ROLLINGPOLICY_MAX_HISTORY", "7")));
            rollingPolicy.start();

            appender.setRollingPolicy(rollingPolicy);
            appender.start();

            earlyContext.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(appender);
            logMessage(LogLevel.INFO, "[SMCRYPT] 检测到密文，已启用早期日志文件输出：{}", appender.getFile());
        } catch (Exception e) {
            // 文件输出为增强能力，失败时保留控制台输出即可，不影响解密流程
            logMessage(LogLevel.WARN, "[SMCRYPT] 启用早期日志文件输出失败，日志仅输出到控制台", e);
        }
    }

    /**
     * 判断根 logger 是否已挂载同名文件 appender
     *
     * <p>用于兼容部署时通过工作目录覆盖 {@code logback-smcrypt.xml}、并静态声明了文件 appender 的场景，
     * 避免再额外创建一个同名 appender 造成重复写入。</p>
     *
     * @return 已存在同名 appender 时返回 true
     */
    private boolean hasAttachedFileAppender() {
        Iterator<Appender<ILoggingEvent>> appenders =
                earlyContext.getLogger(Logger.ROOT_LOGGER_NAME).iteratorForAppenders();
        while (appenders.hasNext()) {
            if (FILE_APPENDER_NAME.equals(appenders.next().getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析日志文件字符集
     *
     * <p>优先级与全局日志一致：{@code logging.charset.file} → JVM {@code file.encoding} → UTF-8，
     * 避免非 ASCII 日志内容写入文件时乱码。</p>
     *
     * @return 文件日志字符集
     */
    private Charset resolveFileCharset() {
        String charset = earlyContext.getProperty("FILE_LOG_CHARSET");
        if (charset == null || charset.trim().isEmpty()) {
            charset = System.getProperty("file.encoding", "UTF-8");
        }
        try {
            return Charset.forName(charset.trim());
        } catch (RuntimeException e) {
            return Charset.forName("UTF-8");
        }
    }

    /**
     * 解析日志文件基础目录
     *
     * @return 日志目录；均未配置时回退到 {@code java.io.tmpdir}
     */
    private String resolveBaseLogPath() {
        String basePath = propertyOrDefault("BASE_LOG_PATH", null);
        if (basePath != null) {
            return basePath;
        }
        String logPath = propertyOrDefault("LOG_PATH", null);
        if (logPath != null) {
            return logPath;
        }
        String logTemp = propertyOrDefault("LOG_TEMP", null);
        if (logTemp != null) {
            return logTemp;
        }
        return System.getProperty("java.io.tmpdir", ".");
    }

    /**
     * 读取独立日志上下文属性，缺失或空白时返回默认值
     *
     * @param name         属性名
     * @param defaultValue 默认值
     * @return 去除首尾空白的属性值；缺失或空白时返回默认值
     */
    private String propertyOrDefault(String name, String defaultValue) {
        String value = earlyContext.getProperty(name);
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
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
     * 走独立 LoggerContext；否则回退到 {@link SmCryptLogFormatter#printFallback} 的控制台兜底输出，
     * 保证解密流程的关键日志不丢失。
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
        SmCryptLogFormatter.printFallback(level, format, args);
    }
}
