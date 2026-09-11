# 日志

[返回 README](../README.md) | [文档索引](README.md)

- 解密引擎的关键日志带 `[SMCRYPT]` 前缀，正常解密会打印属性名、算法、来源；密钥解析、SPI 加载等辅助类的告警/调试日志不带该前缀；
- 解密发生在 Spring 全局日志系统初始化之前，因此使用 **独立 LoggerContext**，配置为
  `logback-smcrypt.xml`（位于核心模块 `smcrypt-spring-boot` 的 `src/main/resources`，由各 starter 共享）；
- 可在应用工作目录放置同名 `logback-smcrypt.xml` 覆盖默认早期日志配置；
- 早期日志文件默认为 `{spring.application.name}.smcrypt.log`，输出目录取 `logging.file.path`（默认 `logs`），
  日志上下文在解密结束后即关闭，不会在后台持续滚动；
- 独立日志初始化失败时自动回退到 `System.out` / `System.err`，保证关键日志不丢失。

## 相关文档

- [常见问题](faq.md)
