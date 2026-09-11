# AGENTS.md

## 项目概览

`smcrypt-spring-boot-starter`：解密 Spring Boot 配置文件中的密文属性值。Gradle 多模块聚合工程。

## 构建与测试命令

```bash
./gradlew build                                   # 全量编译 + 测试 + javadoc
./gradlew test                                    # 全部模块测试
./gradlew :smcrypt-spring-boot:test               # 核心模块单元测试
./gradlew :smcrypt-spring-boot2-starter:test      # Boot 2 集成测试（Java 8 工具链）
./gradlew :smcrypt-spring-boot3-starter:test      # Boot 3 集成测试（Java 17 工具链）
./gradlew :smcrypt-spring-boot4-starter:test      # Boot 4 集成测试（Java 17 工具链）
./gradlew :smcrypt-spring-boot:compileJava        # 仅编译核心
```

## 模块与职责

- `smcrypt-spring-boot`：版本无关核心（Java 8）。包含 `codec`（编解码）、`handler`（算法处理器）、
  `key`（密钥解析）、`config`（算法配置）、`spi`（处理器加载）、`SmCryptDecryptor`（解密引擎）、
  `SmCryptEncryptor` / `SmCryptCli`（加密工具）。`compileOnly` 依赖 Spring Boot 2.7，不得引用 Boot 3/4 专有 API。
- `smcrypt-spring-boot2/3/4-starter`：各自仅有一个薄适配器 `SmCryptEnvironmentPostProcessor`，
  实现对应版本的 `EnvironmentPostProcessor` 并委托核心引擎。Boot 4 的接口位于 `org.springframework.boot`
  （2/3 位于 `org.springframework.boot.env`）。

## 关键约定

- 核心源码必须兼容 Java 8（禁止 `var`、`instanceof` 模式匹配、`List.of` 等）。
- 新增算法：继承 `AbstractCipherHandler`（对称用 `AbstractSymmetricCipherHandler`），并在
  `CipherHandlerLoader#builtinHandlers` 注册。
- 密文格式统一为 `{算法}ENC([hex|base64,]密文)`，编码缺省时自动识别；对称算法可选启用
  encrypt-then-MAC（`smcrypt.<算法>.mac=HmacSM3|HmacSHA256`），此时载荷为 `密文 || MAC`，解密前先校验。
- 核心测试运行时会加载 `src/test/resources/META-INF/services` 与 `spring.factories` 中注册的测试处理器，
  用于验证 SPI 扩展路径，改动时需同步更新 `CipherHandlerLoaderTest`。
- 早期日志配置文件名固定为 `logback-smcrypt.xml`（位于核心模块 `smcrypt-spring-boot` 的 `src/main/resources`，
  由各 starter 共享；部署时可通过工作目录同名文件覆盖）。
- 发布配置在 `publishing.gradle`，根项目不发布产物。
