package com.houkunlin.smcrypt;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
     * 回退输出时的时间格式
     */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * 当前活动的早期日志上下文；为 null 时回退到控制台输出
     */
    private static volatile SmCryptLogback active;

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
        active = logback;
    }

    /**
     * 清除当前活动的早期日志上下文
     */
    public static void clearActive() {
        active = null;
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
     * @param level  日志级别
     * @param format 日志格式
     * @param args   占位符参数
     */
    private static void log(LogLevel level, String format, Object... args) {
        SmCryptLogback logback = active;
        if (logback != null) {
            logback.logMessage(level, format, args);
            return;
        }
        // 无活动日志上下文（例如命令行工具）：回退到控制台输出，确保异常不被静默丢弃
        String message = formatMessage(format, args);
        Throwable throwable = lastThrowable(args);
        String line = TIME_FORMATTER.format(LocalDateTime.now()) + " [" + level.name() + "] " + message;
        if (level == LogLevel.ERROR || level == LogLevel.WARN) {
            System.err.println(line);
            if (throwable != null) {
                throwable.printStackTrace(System.err);
            }
        } else {
            System.out.println(line);
            if (throwable != null) {
                throwable.printStackTrace(System.out);
            }
        }
    }

    /**
     * 解析 {@code {}} 占位符并拼接日志文本（不包含尾部异常参数）
     *
     * @param format 日志格式
     * @param args   占位符参数
     * @return 拼接后的日志文本
     */
    private static String formatMessage(String format, Object... args) {
        StringBuilder message = new StringBuilder();
        int argIndex = 0;
        for (int i = 0; i < format.length(); i++) {
            if (format.charAt(i) == '{' && i + 1 < format.length() && format.charAt(i + 1) == '}'
                    && argIndex < args.length && !(args[argIndex] instanceof Throwable)) {
                message.append(args[argIndex++]);
                i++;
            } else {
                message.append(format.charAt(i));
            }
        }
        return message.toString();
    }

    /**
     * 获取参数列表末尾的异常对象（SLF4J 风格的尾部异常）
     *
     * @param args 日志参数
     * @return 末尾的异常对象；不存在时返回 null
     */
    private static Throwable lastThrowable(Object... args) {
        if (args.length > 0 && args[args.length - 1] instanceof Throwable) {
            return (Throwable) args[args.length - 1];
        }
        return null;
    }
}
