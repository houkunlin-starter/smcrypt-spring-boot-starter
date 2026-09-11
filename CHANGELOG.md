# 更新日志

本项目遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 与
[语义化版本](https://semver.org/lang/zh-CN/)。

## [Unreleased]

### 新增

- 全局配置 `smcrypt.fail-fast`：解密失败（含未找到密钥）时中断应用启动。
- 全局配置 `smcrypt.strict`：启动阶段执行加密配置安全体检（`off` / `warn` / `fail`），可拦截 ECB、DES / 3DES、
  RSA PKCS#1 / 弱密钥、Jasypt 旧算法、PBE 固定盐 / 弱派生参数等弱配置。
- 密钥编码配置 `smcrypt.<算法>.key-encoding` / `smcrypt.<算法>.mac-key-encoding`
  （`hex` / `base64` / `plain`），用于消除密钥自动识别的歧义。
- CLI 支持 `--key -` / `--password -` 从标准输入读取密钥 / 口令，避免进入命令历史。

### 变更

- 改进 DESEDE 的 JCE 变换串拼接：按 `mode` 生成规范的 `DESede` 名称。
- 缓存密钥解析结果与算法配置，避免逐属性重复读取配置、扫描密钥文件与解码密钥。
- 统一使用 `Locale.ROOT` 进行大小写转换，避免区域设置差异。
- 密钥长度不匹配时给出包含算法名与实际字节数的错误信息。
- 启用 MAC 但未单独配置 `mac-key` 时，提示复用加密密钥。
- 预编译正则表达式，减少重复编译开销。

### 修复

- 修复安全体检在配置非法时（`smcrypt.strict=warn`）意外中断启动的问题：解析异常改为记录为一条问题，不再直接抛出。
- 修复 SM2 的 `mode`（密文顺序）被错误用于拼接 JCE 变换串，产生无意义变换串的问题。
- 安全体检新增 PBE JCE 模式弱算法（`PBEWithMD5AndDES`）检测。

### 文档

- README 拆分为主题文档（`docs/`）与按算法的文档（`docs/algorithms/`）。
- 配置项表格标注使用阶段（加密 / 解密 / 加解密）。
- 新增生产环境加固清单。

## [1.0.0]

### 新增

- 在 `EnvironmentPostProcessor` 阶段解密配置文件中的 `{算法}ENC(...)` 密文，兼容 Spring Boot 2 / 3 / 4。
- 内置算法处理器：SM2、SM4、SM9（标识加密）、AES、DES、DESEDE、ChaCha20-Poly1305、GOST3412-2015、DSTU7624、
  RC6、Camellia、ARIA、SEED、RSA、ECC（ECIES）。
- 密文编码支持 hex / Base64，可显式声明或自动识别。
- 对称算法可选 encrypt-then-MAC（`smcrypt.<算法>.mac=HmacSM3|HmacSHA256`）。
- 口令派生（PBE：PBKDF2 / scrypt / Argon2，或 JCE PBE 变换）与 Jasypt 密文兼容。
- 密钥生成、加密 API 与命令行工具。
- SPI 扩展接口，支持接入自定义算法或加密机并覆盖内置实现。
- 启动早期独立日志上下文。
