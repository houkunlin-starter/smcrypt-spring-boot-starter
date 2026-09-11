# 构建与测试

[返回 README](../README.md) | [文档索引](README.md)

```bash
./gradlew build                                   # 全量编译 + 测试 + javadoc
./gradlew test                                    # 全部模块测试
./gradlew :smcrypt-spring-boot:test               # 核心模块单元测试
./gradlew :smcrypt-spring-boot2-starter:test      # Boot 2 集成测试（Java 8 工具链）
./gradlew :smcrypt-spring-boot3-starter:test      # Boot 3 集成测试（Java 17 工具链）
./gradlew :smcrypt-spring-boot4-starter:test      # Boot 4 集成测试（Java 17 工具链）
./gradlew :smcrypt-spring-boot:compileJava        # 仅编译核心
./gradlew javadoc                                 # 生成 Javadoc
```

测试覆盖：

- 核心单元测试：编解码自动识别、各算法加解密往返（SM4 / SM2 / SM9 / AES / DES / DESEDE / CHACHA20 / GOST3412 / DSTU7624 /
  RC6 / CAMELLIA / ARIA / SEED / RSA / ECC）、口令派生（PBKDF2 / scrypt / Argon2 与 JCE PBE）与 Jasypt 兼容、
  完整性校验（对称算法 MAC 与非对称算法内置校验的篡改检测）、密钥生成与长度校验、密钥解析、SPI 加载（`META-INF/services` 与
  `spring.factories` 两条路径）、解密引擎对 PropertySource 的替换与来源保留；
- 各 starter 集成测试：真实启动 `SpringApplication`，验证配置中的密文被解成明文。

## 相关文档

- [工作原理与模块结构](architecture.md)
- [自定义算法 / 加密机接入](extension.md)
