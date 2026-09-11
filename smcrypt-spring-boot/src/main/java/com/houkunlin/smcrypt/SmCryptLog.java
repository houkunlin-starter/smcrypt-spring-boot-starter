package com.houkunlin.smcrypt;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 核心内部日志门面。
 *
 * <p>为编解码、密钥解析、SPI 加载等工具类提供统一的日志输出入口，避免捕获异常后静默丢弃。
 * 输出目标按以下顺序选择：</p>
 * <ol>
 *     <li>解密引擎启动时通过 {@link #setActive(SmCryptLogback)} 注入的早期日志上下文；</li>
 *     <li>无活动上下文时（例如命令行工具场景）回退到 {@link System#out}/{@link System#err}。</li>
 * </ol>
 *
 * <p>日志格式支持 SLF4J 风格的 {@code {}} 占位符；若最后一个参数为 {@link Throwable}，
 * 则同时输出异常堆栈。</p>
 *
 * @author HouKunLin
 */
public final class SmCryptLog {
    /**
     * 当前活动的早期日志上下文；为 null 时回退到控制台输出
     */
    private static final AtomicReference<SmCryptLogback> ACTIVE = new AtomicReference<>();

    /**
     * 工具类，禁止实例化
     */
    private SmCryptLog() {
    }

    /**
     * 设置当前活动的早期日志上下文
     *
     * @param logback 早期日志上下文
     */
    public static void setActive(SmCryptLogback logback) {
        ACTIVE.set(logback);
    }

    /**
     * 清除当前活动的早期日志上下文
     */
    public static void clearActive() {
        ACTIVE.set(null);
    }

    /**
     * 输出调试级别日志
     *
     * @param format 日志格式（支持 {@code {}} 占位符）
     * @param args   占位符参数；若最后一个参数为 {@link Throwable} 则输出其堆栈
     */
    public static void debug(String format, Object... args) {
        log(LogLevel.DEBUG, format, args);
    }

    /**
     * 输出信息级别日志
     *
     * @param format 日志格式（支持 {@code {}} 占位符）
     * @param args   占位符参数；若最后一个参数为 {@link Throwable} 则输出其堆栈
     */
    public static void info(String format, Object... args) {
        log(LogLevel.INFO, format, args);
    }

    /**
     * 输出警告级别日志
     *
     * @param format 日志格式（支持 {@code {}} 占位符）
     * @param args   占位符参数；若最后一个参数为 {@link Throwable} 则输出其堆栈
     */
    public static void warn(String format, Object... args) {
        log(LogLevel.WARN, format, args);
    }

    /**
     * 输出错误级别日志
     *
     * @param format 日志格式（支持 {@code {}} 占位符）
     * @param args   占位符参数；若最后一个参数为 {@link Throwable} 则输出其堆栈
     */
    public static void error(String format, Object... args) {
        log(LogLevel.ERROR, format, args);
    }

    /**
     * 统一日志输出入口
     *
     * <p>存在活动日志上下文时委托给 {@link SmCryptLogback#logMessage}；否则回退到
     * {@link SmCryptLogFormatter#printFallback} 的控制台兜底输出（例如命令行工具场景）。</p>
     *
     * @param level  日志级别
     * @param format 日志格式
     * @param args   占位符参数
     */
    private static void log(LogLevel level, String format, Object... args) {
        SmCryptLogback logback = ACTIVE.get();
        if (logback != null) {
            logback.logMessage(level, format, args);
            return;
        }
        // 无活动日志上下文（例如命令行工具）：回退到控制台输出，确保异常不被静默丢弃
        SmCryptLogFormatter.printFallback(level, format, args);
    }
}
