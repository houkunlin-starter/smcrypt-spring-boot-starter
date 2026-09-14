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
- 初始向量（IV）长度不匹配时给出包含算法名与实际字节数的错误信息。
- 启用 MAC 但未单独配置 `mac-key` 时，提示复用加密密钥。
- 预编译正则表达式，减少重复编译开销。
- 合并解密前的密文探测与解密遍历为单次扫描，减少一次全量属性遍历。
- 处理器加载器新增按算法名称索引的 `loadMap`，`SmCryptEncryptor` 复用该映射，避免重复构建。
- BouncyCastle 依赖按 JDK 拆分：核心模块改为 `compileOnly`，Boot 2 starter 提供 `bcprov/bcpkix-jdk15to18`、
  Boot 3/4 starter 提供 `bcprov/bcpkix-jdk18on`，避免与使用方自身的 BouncyCastle 依赖冲突。
- 早期日志文件改为在检测到密文后才创建并挂载，避免未使用密文的应用仍生成 `<应用名>.smcrypt.log` 文件。

### 修复

- 修复安全体检在配置非法时（`smcrypt.strict=warn`）意外中断启动的问题：解析异常改为记录为一条问题，不再直接抛出。
- 修复 SM2 的 `mode`（密文顺序）被错误用于拼接 JCE 变换串，产生无意义变换串的问题。
- 安全体检新增 PBE JCE 模式弱算法（`PBEWithMD5AndDES`）检测。
- 修复 `.properties` 密钥文件按 ISO-8859-1 解码导致非 ASCII 口令乱码的问题，改为按 UTF-8 读取。
- 修复低版本 BouncyCastle（低于 1.86）环境下加载 SM9 处理器失败导致应用启动失败的问题：SM9 改为反射延迟加载，
  缺少相关类时记录 WARN 并跳过，不影响其它算法与启动流程。
- 修复通过 `META-INF/services` 加载处理器时，provider 实例化失败抛出的 `ServiceConfigurationError`（`Error` 子类）
  未被捕获、导致应用启动中断的问题。
- 核心模块编译依赖对齐 Spring Boot 2.7.0：`slf4j-api` 1.7.36 + `logback-classic` 1.2.13，修正此前
  `slf4j-api` 2.0.x 与 `logback-classic` 1.2.x 的版本错配。

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
