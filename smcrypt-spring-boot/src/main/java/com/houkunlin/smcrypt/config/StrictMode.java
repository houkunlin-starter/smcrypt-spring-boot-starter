package com.houkunlin.smcrypt.config;

import java.util.Locale;

/**
 * 安全体检严格程度。
 *
 * <p>由全局配置 {@code smcrypt.strict} 指定：</p>
 * <ul>
 *     <li>{@link #OFF}（默认）：不执行安全体检；</li>
 *     <li>{@link #WARN}：执行体检，逐条打印告警，不阻断启动；</li>
 *     <li>{@link #FAIL}：执行体检，存在问题时抛出异常中断启动。</li>
 * </ul>
 *
 * @author HouKunLin
 */
public enum StrictMode {
    /**
     * 不执行安全体检
     */
    OFF,
    /**
     * 执行安全体检，仅告警
     */
    WARN,
    /**
     * 执行安全体检，存在问题时中断启动
     */
    FAIL;

    /**
     * 解析严格程度配置
     *
     * <p>支持 {@code off} / {@code false}、{@code warn}、{@code fail} / {@code true}（忽略大小写与首尾空白）。
     * 传入 null 或空白返回 {@link #OFF}；无法识别的非空值抛出异常。</p>
     *
     * @param token 配置值
     * @return 严格程度
     * @throws IllegalArgumentException 配置了无法识别的取值时抛出
     */
    public static StrictMode fromToken(String token) {
        if (token == null) {
            return OFF;
        }
        String value = token.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty() || "off".equals(value) || "false".equals(value)) {
            return OFF;
        }
        if ("warn".equals(value)) {
            return WARN;
        }
        if ("fail".equals(value) || "true".equals(value)) {
            return FAIL;
        }
        throw new IllegalArgumentException("不支持的 smcrypt.strict 取值：" + token + "，仅支持 off / warn / fail");
    }
}
