package com.houkunlin.smcrypt;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 早期日志的占位符格式化与控制台兜底输出工具。
 *
 * <p>当独立日志上下文不可用（日志配置文件缺失、初始化失败，或命令行工具场景）时，
 * 由 {@link #printFallback(LogLevel, String, Object...)} 将日志输出到
 * {@code System.out}/{@code System.err}，作为最后兜底，保证关键日志与异常不被静默丢弃。</p>
 *
 * <p>约定：</p>
 * <ul>
 *     <li>{@code ERROR} 级别输出到 {@link System#err}，其余级别输出到 {@link System#out}；</li>
 *     <li>只要传入 {@link Throwable}，就打印其堆栈（与日志级别无关），并输出到与该条日志相同的流。</li>
 * </ul>
 *
 * @author HouKunLin
 */
final class SmCryptLogFormatter {
    /**
     * 回退输出时的时间格式
     */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * 工具类，禁止实例化
     */
    private SmCryptLogFormatter() {
    }

    /**
     * 解析 {@code {}} 占位符并拼接日志文本（不包含尾部异常参数）
     *
     * <p>尾部 {@link Throwable} 不会被当作占位符的值，避免异常文本与堆栈重复输出。</p>
     *
     * @param format 日志格式
     * @param args   占位符参数
     * @return 拼接后的日志文本
     */
    static String format(String format, Object... args) {
        StringBuilder message = new StringBuilder();
        int argIndex = 0;
        int index = 0;
        while (index < format.length()) {
            if (format.charAt(index) == '{' && index + 1 < format.length() && format.charAt(index + 1) == '}'
                    && argIndex < args.length && !(args[argIndex] instanceof Throwable)) {
                message.append(args[argIndex++]);
                index += 2;
            } else {
                message.append(format.charAt(index));
                index++;
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
    static Throwable lastThrowable(Object... args) {
        if (args.length > 0 && args[args.length - 1] instanceof Throwable) {
            return (Throwable) args[args.length - 1];
        }
        return null;
    }

    /**
     * 控制台兜底输出
     *
     * <p>{@code ERROR} 输出到 {@link System#err}，其余级别输出到 {@link System#out}；
     * 只要存在尾部 {@link Throwable}，就向其所属流打印堆栈。</p>
     *
     * @param level  日志级别
     * @param format 日志格式（支持 {@code {}} 占位符）
     * @param args   占位符参数；若最后一个参数为 {@link Throwable} 则追加堆栈输出
     */
    @SuppressWarnings("java:S106")
    static void printFallback(LogLevel level, String format, Object... args) {
        String message = format(format, args);
        Throwable throwable = lastThrowable(args);
        String line = TIME_FORMATTER.format(LocalDateTime.now()) + " [" + level.name() + "] " + message;
        if (level == LogLevel.ERROR) {
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
}
