# smcrypt-spring-boot-starter

解密 Spring Boot 配置文件中的密文属性值。

在应用启动早期（`EnvironmentPostProcessor` 阶段）自动扫描所有配置来源，将 `{算法}ENC(...)` 形式的密文
解密为明文并替换，业务代码无需任何改动即可按原有方式读取配置。

## 项目作用

实际项目中，数据库密码、Redis 密码、第三方密钥等敏感信息往往直接以明文写在
`application.yml` / `application.properties` 中，存在泄露风险。常见做法是：

1. 用加密工具把敏感信息加密成密文，写入配置文件；
2. 应用启动时在读取配置之前把密文解密回明文。

本项目即为第 2 步的通用实现，通过 Spring Boot 的 `EnvironmentPostProcessor` 扩展点，在 **任何 Bean 创建之前**
完成配置解密，因此对业务代码完全透明。

## 核心特性

- 同时支持 Spring Boot 2 / 3 / 4，按版本选用对应 Starter；
- 内置算法：SM4、SM2、SM9（标识加密）、AES、DES、DESEDE (3DES)
  、ChaCha20-Poly1305、GOST3412-2015、DSTU7624、RC6、Camellia、ARIA、SEED、RSA、ECC（ECIES），基于
  BouncyCastle；
- 密文编码支持 hex 与 Base64，可显式声明（`SM4ENC(hex,...)`）或自动识别；
- 支持 properties、yml/yaml、命令行参数、环境变量等所有 Spring Boot 配置来源；
- 解密时保留配置来源（Origin）信息，YAML 行号、错误溯源不受影响；
- 解密失败默认仅记录日志并跳过该属性，可通过 `smcrypt.fail-fast=true` 改为中断启动；
- 保留 SPI 扩展接口，业务方可接入自定义算法或加密机，并覆盖内置实现；
- 支持口令派生（PBE：PBKDF2 / scrypt / Argon2，或 JCE PBE 变换）与 Jasypt 密文兼容（`PBEENC` / `JASYPTENC`）；
- 提供密钥生成、加密 API 与命令行工具，便于生成密钥与配置密文；
- 启动早期使用独立日志上下文，不会干扰 Spring Boot 全局日志系统。

> 生产环境部署前，请先阅读 [生产环境加固建议](docs/security.md)。

## 快速开始

按 Spring Boot 版本引入依赖：

```groovy
// Gradle，三选一
implementation 'com.houkunlin:smcrypt-spring-boot2-starter:1.0.0'
implementation 'com.houkunlin:smcrypt-spring-boot3-starter:1.0.0'
implementation 'com.houkunlin:smcrypt-spring-boot4-starter:1.0.0'
```

生成密文并写入配置：

```bash
java -cp app.jar com.houkunlin.smcrypt.SmCryptCli \
    --algorithm SM4 \
    --key 0123456789abcdeffedcba9876543210 \
    --text my-db-password
# 输出：SM4ENC(base64,xxxxxxxxxxxx)
```

```properties
spring.datasource.password=SM4ENC(base64,xxxxxxxxxxxx)
smcrypt.sm4.key=0123456789abcdeffedcba9876543210
```

业务代码照常读取即可，值已被自动解密：

```java

@Value("${spring.datasource.password}")
private String password;
```

详见 [环境要求与快速开始](docs/getting-started.md)。

## 支持的算法

| 算法     | 算法类型   | 密钥长度                                      | 前缀          | 默认变换                         | 说明                      |
|----------|------------|-----------------------------------------------|---------------|----------------------------------|---------------------------|
| SM4      | 对称算法   | 128 位（16 字节）                             | `SM4ENC`      | `SM4/ECB/PKCS5Padding`           | 国密分组密码              |
| SM2      | 非对称算法 | 256 位（32 字节）                             | `SM2ENC`      | `SM2`（C1C3C2）                  | 国密非对称                |
| AES      | 对称算法   | 128 / 192 / 256 位（16 / 24 / 32 字节）       | `AESENC`      | `AES/ECB/PKCS5Padding`           | 国际通用对称加密          |
| DES      | 对称算法   | 56 位（8 字节）                               | `DESENC`      | `DES/ECB/PKCS5Padding`           | 兼容遗留系统              |
| DESEDE   | 对称算法   | 112 / 168 位（16 / 24 字节）                  | `DESEDEENC`   | `DESede/ECB/PKCS5Padding`        | 3DES，兼容遗留系统        |
| CHACHA20 | 对称算法   | 256 位（32 字节）                             | `CHACHA20ENC` | `ChaCha20-Poly1305`              | AEAD，自带完整性          |
| GOST3412 | 对称算法   | 256 位（32 字节）                             | `GOST3412ENC` | `GOST3412-2015/ECB/PKCS5Padding` | 俄罗斯标准（Kuznyechik）  |
| DSTU7624 | 对称算法   | 128 / 256 / 512 位（16 / 32 / 64 字节）       | `DSTU7624ENC` | `DSTU7624/ECB/PKCS5Padding`      | 乌克兰标准（Kalyna）      |
| RC6      | 对称算法   | 128 / 192 / 256 位（16 / 24 / 32 字节）       | `RC6ENC`      | `RC6/ECB/PKCS5Padding`           | AES 候选算法              |
| CAMELLIA | 对称算法   | 128 / 192 / 256 位（16 / 24 / 32 字节）       | `CAMELLIAENC` | `Camellia/ECB/PKCS5Padding`      | 日本 / ISO 地区标准       |
| ARIA     | 对称算法   | 128 / 192 / 256 位（16 / 24 / 32 字节）       | `ARIAENC`     | `ARIA/ECB/PKCS5Padding`          | 韩国地区标准              |
| SEED     | 对称算法   | 128 位（16 字节）                             | `SEEDENC`     | `SEED/ECB/PKCS5Padding`          | 韩国地区标准              |
| RSA      | 非对称算法 | 2048 / 3072 / 4096 位（256 / 384 / 512 字节） | `RSAENC`      | `RSA/ECB/PKCS1Padding`           | 非对称加密                |
| ECC      | 非对称算法 | 256 / 384 / 521 位（32 / 48 / 66 字节）       | `ECCENC`      | `ECIES`                          | 椭圆曲线加密              |
| SM9      | 非对称算法 | 256 位（BN 曲线，de 129 / Ppub-e 65 字节）    | `SM9ENC`      | —                                | 标识加密（IBC），配置独立 |
| PBE      | 对称算法   | 由 KDF / 算法决定                             | `PBEENC`      | —                                | 口令派生（KDF / JCE）     |
| JASYPT   | 对称算法   | 由算法决定                                    | `JASYPTENC`   | —                                | 解密 Jasypt 密文          |

完整参数与示例见 [算法索引](docs/algorithms/README.md)。

## 模块结构

| 模块                           | 职责                                                                | 运行环境 |
|--------------------------------|---------------------------------------------------------------------|----------|
| `smcrypt-spring-boot`          | 版本无关核心：编解码、算法处理器、密钥解析、解密引擎、SPI、加密工具 | Java 8+  |
| `smcrypt-spring-boot2-starter` | Spring Boot 2.x 适配器                                              | Java 8+  |
| `smcrypt-spring-boot3-starter` | Spring Boot 3.x 适配器                                              | Java 17+ |
| `smcrypt-spring-boot4-starter` | Spring Boot 4.x 适配器                                              | Java 17+ |

详见 [工作原理与模块结构](docs/architecture.md)。

## 文档导航

完整文档见 [文档索引](docs/README.md)。

| 主题                                          | 说明                                        |
|-----------------------------------------------|---------------------------------------------|
| [环境要求与快速开始](docs/getting-started.md) | 各 Spring Boot 版本的环境要求与最小可用示例 |
| [工作原理与模块结构](docs/architecture.md)    | 启动早期解密的流程与各模块职责              |
| [密文格式](docs/cipher-format.md)             | `{算法}ENC(...)` 包裹结构、编码自动识别     |
| [密钥配置](docs/keys.md)                      | 密钥来源优先级与密钥文件格式                |
| [配置项参考](docs/configuration.md)           | `smcrypt.<算法>.*` 总览、参数矩阵与通用示例 |
| [算法索引](docs/algorithms/README.md)         | 各算法的默认参数、配置与示例                |
| [算法安全性与选型建议](docs/security.md)      | 各算法安全状态与推荐优先级                  |
| [自定义算法 / 加密机接入](docs/extension.md)  | 通过 SPI 接入自定义算法或加密机             |
| [加密工具](docs/tools.md)                     | 加密 API、密钥生成与命令行工具              |
| [日志](docs/logging.md)                       | 启动早期独立日志上下文                      |
| [常见问题](docs/faq.md)                       | 注意事项与常见问题                          |
| [构建与测试](docs/build.md)                   | 构建、测试命令与测试覆盖说明                |

## 许可证

本项目基于 [Mulan Permissive Software License, Version 2](https://license.coscl.org.cn/MulanPSL2) 发布。
