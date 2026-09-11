# smcrypt-spring-boot-starter

解密 Spring Boot 配置文件中的密文属性值。

在应用启动早期（`EnvironmentPostProcessor` 阶段）自动扫描所有配置来源，将 `{算法}ENC(...)` 形式的密文
解密为明文并替换，业务代码无需任何改动即可按原有方式读取配置。

---

## 目录

- [一、项目背景与作用](#一项目背景与作用)
- [二、核心特性](#二核心特性)
- [三、工作原理](#三工作原理)
- [四、模块结构](#四模块结构)
- [五、环境要求](#五环境要求)
- [六、快速开始](#六快速开始)
- [七、密文格式](#七密文格式)
- [八、支持的算法](#八支持的算法)
- [九、密钥配置](#九密钥配置)
- [十、配置项参考](#十配置项参考)
- [十一、自定义算法 / 加密机接入](#十一自定义算法--加密机接入)
- [十二、加密工具](#十二加密工具)
- [十三、日志](#十三日志)
- [十四、注意事项与常见问题](#十四注意事项与常见问题)
- [十五、构建与测试](#十五构建与测试)
- [十六、许可证](#十六许可证)

---

## 一、项目背景与作用

实际项目中，数据库密码、Redis 密码、第三方密钥等敏感信息往往直接以明文写在
`application.yml` / `application.properties` 中，存在泄露风险。常见的做法是：

1. 用加密工具把敏感信息加密成密文，写入配置文件；
2. 应用启动时在读取配置之前把密文解密回明文。

本项目即为第 2 步的通用实现，通过 Spring Boot 的 `EnvironmentPostProcessor` 扩展点，在 **任何 Bean 创建之前**
完成配置解密，因此对业务代码完全透明。

它的核心价值：

- **零侵入**：业务代码照常使用 `@Value`、`Environment`、`@ConfigurationProperties`，无需感知密文；
- **多版本兼容**：同一套核心逻辑，分别适配 Spring Boot 2 / 3 / 4；
- **多算法**：内置国密 SM2 / SM4 及 AES / DES / RSA / ECC；
- **多编码**：支持 hex、Base64，可显式声明或自动识别；
- **可扩展**：业务系统可通过 SPI 接入自定义算法，或对接加密机（HSM）等外部解密能力。

## 二、核心特性

- 同时支持 Spring Boot 2 / 3 / 4，按版本选用对应 Starter；
- 内置算法：SM4、SM2、AES、DES、RSA、ECC（ECIES），基于 BouncyCastle；
- 密文编码支持 hex 与 Base64，可显式声明（`SM4ENC(hex,...)`）或自动识别；
- 支持 properties、yml/yaml、命令行参数、环境变量等所有 Spring Boot 配置来源；
- 解密时保留配置来源（Origin）信息，YAML 行号、错误溯源不受影响；
- 解密失败仅记录日志并跳过该属性，不会导致整个应用启动崩溃；
- 保留 SPI 扩展接口，业务方可接入自定义算法或加密机，并覆盖内置实现；
- 提供加密 API 与命令行工具，便于生成配置密文和联调测试；
- 启动早期使用独立日志上下文，不会干扰 Spring Boot 全局日志系统。

## 三、工作原理

以 Spring Boot 3 为例，启动流程如下：

```
SpringApplication.run()
        │
        ▼
prepareEnvironment()
        │
        ├── 加载 application.yml / application.properties / 命令行参数 / 环境变量 ...
        │
        ├── 触发 EnvironmentPostProcessor（本项目的 SmCryptEnvironmentPostProcessor）
        │        │
        │        ├── 加载全部密文处理器（内置 + 业务 SPI 注册）
        │        ├── 遍历所有 MapPropertySource / OriginTrackedMapPropertySource
        │        ├── 识别 {算法}ENC(...) 密文并解密
        │        └── 用明文替换原值（保留 Origin 信息）
        │
        ▼
创建 ApplicationContext、实例化 Bean
        │
        ▼
业务代码读取到的已是明文
```

关键点：

- 解密发生在配置加载完成之后、Bean 创建之前，因此对所有配置读取方式透明；
- 三个版本模块各自实现对应版本的 `EnvironmentPostProcessor`，仅做委托，核心逻辑统一在
  `smcrypt-spring-boot` 中，从而屏蔽 Spring Boot 4 的接口包名迁移（`org.springframework.boot.env`
  → `org.springframework.boot`）。

## 四、模块结构

| 模块                           | 职责                                                                              | 运行环境 |
|--------------------------------|-----------------------------------------------------------------------------------|----------|
| `smcrypt-spring-boot`          | 版本无关核心：编解码、算法处理器、密钥解析、解密引擎、SPI、加密工具               | Java 8+  |
| `smcrypt-spring-boot2-starter` | Spring Boot 2.x 适配器（`org.springframework.boot.env.EnvironmentPostProcessor`） | Java 8+  |
| `smcrypt-spring-boot3-starter` | Spring Boot 3.x 适配器（`org.springframework.boot.env.EnvironmentPostProcessor`） | Java 17+ |
| `smcrypt-spring-boot4-starter` | Spring Boot 4.x 适配器（`org.springframework.boot.EnvironmentPostProcessor`）     | Java 17+ |

核心模块的包结构：

```
com.houkunlin.smcrypt
├── SmCryptDecryptor            解密引擎：遍历/识别/解密/替换
├── SmCryptEncryptor            加密工具 API
├── SmCryptCli                  加密 / 解密命令行工具
├── SmCryptContext              加解密上下文（属性查询 + 密钥解析）
├── BouncyCastleSupport         BouncyCastle Provider 持有
├── SmCryptLogback              启动早期独立日志
├── codec/                      编码枚举、Hex/Base64 编解码、自动识别
├── config/                     算法配置 CipherConfig
├── handler/                    算法处理器 SPI 及内置实现
├── key/                        密钥解析与私钥加载
└── spi/                        处理器加载器
```

## 五、环境要求

| 使用场景        | JDK      | 对应依赖                       |
|-----------------|----------|--------------------------------|
| Spring Boot 2.x | Java 8+  | `smcrypt-spring-boot2-starter` |
| Spring Boot 3.x | Java 17+ | `smcrypt-spring-boot3-starter` |
| Spring Boot 4.x | Java 17+ | `smcrypt-spring-boot4-starter` |

核心模块以 Java 8 编译，BouncyCastle 使用 `jdk15to18` 变体以兼容低版本 JDK。

## 六、快速开始

### 1. 引入依赖

Gradle：

```groovy
// 按 Spring Boot 版本三选一
implementation 'com.houkunlin:smcrypt-spring-boot2-starter:1.0.0'
implementation 'com.houkunlin:smcrypt-spring-boot3-starter:1.0.0'
implementation 'com.houkunlin:smcrypt-spring-boot4-starter:1.0.0'
```

Maven：

```xml
<dependency>
    <groupId>com.houkunlin</groupId>
    <artifactId>smcrypt-spring-boot3-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 生成密文

使用命令行工具生成 SM4 密文（详见 [加密工具](#十二加密工具)）：

```bash
java -cp app.jar com.houkunlin.smcrypt.SmCryptCli \
    --algorithm SM4 \
    --key 0123456789abcdeffedcba9876543210 \
    --text my-db-password \
    --encoding base64
# 输出：SM4ENC(base64,xxxxxxxxxxxx)
```

### 3. 写入配置

`application.yml`：

```yaml
spring:
  datasource:
    password: SM4ENC(base64,xxxxxxxxxxxx)

smcrypt:
  sm4:
    key: 0123456789abcdeffedcba9876543210
```

或 `application.properties`：

```properties
spring.datasource.password=SM4ENC(base64,xxxxxxxxxxxx)
smcrypt.sm4.key=0123456789abcdeffedcba9876543210
```

### 4. 正常使用

```java

@Component
public class DemoService {
    @Value("${spring.datasource.password}")
    private String password; // 已自动解密为明文
}
```

## 七、密文格式

### 包裹结构

```
{算法}ENC(密文)
{算法}ENC(hex,密文)
{算法}ENC(base64,密文)
```

- `{算法}` 为算法名称，大小写不敏感：`SM4ENC(...)`、`sm4enc(...)` 均可；
- 括号内可选的编码前缀支持 `hex`、`base64`、`b64`（大小写不敏感）；
- 编码前缀与密文之间用英文逗号分隔。

### 编码自动识别

未显式声明编码时，按以下规则自动识别：

1. 内容全部为十六进制字符 **且长度为 2 的倍数** → 按 hex 解码；
2. 其余情况 → 按 Base64 解码。

> 提示：某些字符串（例如 `ABCD`）既是合法的十六进制又是合法的 Base64，存在歧义。
> 为保证确定性， **推荐使用加密工具生成密文时带上编码前缀**（本项目的加密工具默认就会输出
> `{算法}ENC(base64,...)` 或 `{算法}ENC(hex,...)`）。

### 算法前缀

| 算法 | 前缀          |
|------|---------------|
| SM4  | `SM4ENC(...)` |
| SM2  | `SM2ENC(...)` |
| AES  | `AESENC(...)` |
| DES  | `DESENC(...)` |
| RSA  | `RSAENC(...)` |
| ECC  | `ECCENC(...)` |

## 八、支持的算法

| 算法 | 前缀     | 默认变换               | 密钥要求             | 说明                         |
|------|----------|------------------------|----------------------|------------------------------|
| SM4  | `SM4ENC` | `SM4/ECB/PKCS5Padding` | 16 字节对称密钥      | 国密分组密码                 |
| SM2  | `SM2ENC` | `SM2`（C1C3C2）        | EC 私钥（sm2p256v1） | 国密非对称，默认 C1C3C2 顺序 |
| AES  | `AESENC` | `AES/ECB/PKCS5Padding` | 16 / 24 / 32 字节    | 国际通用对称加密             |
| DES  | `DESENC` | `DES/ECB/PKCS5Padding` | 8 字节               | 兼容遗留系统                 |
| RSA  | `RSAENC` | `RSA/ECB/PKCS1Padding` | RSA 私钥             | 非对称加密                   |
| ECC  | `ECCENC` | `ECIES`                | EC 私钥              | 基于 ECIES 的椭圆曲线加密    |

说明：

- 对称算法通过 JCE `Cipher` 实现，支持通过 `transformation` / `mode` / `padding` / `iv` 自定义；
- 非对称算法仅需提供 **私钥**：解密使用私钥，加密时自动由私钥推导公钥；
- SM2 密文顺序可通过 `smcrypt.sm2.mode=C1C2C3` 切换；
- SM2 / ECC 的椭圆曲线由密钥本身决定，无需额外配置；
- 内置算法均由 BouncyCastle 提供，启动时以显式 Provider 方式使用，不污染全局 JCE Provider。

## 九、密钥配置

### 来源优先级

按算法分别解析，优先级从高到低：

1. Spring 配置属性 `smcrypt.<算法>.key`（也包含命令行参数 `--smcrypt.<算法>.key=...`）；
2. JVM 参数 `-Dsmcrypt.<算法>.key=...`；
3. 环境变量 `SMCRYPT_<算法>_KEY`（算法名大写，如 `SMCRYPT_SM4_KEY`）；
4. 密钥文件 `smcrypt.<算法>.file`（可由属性、JVM 参数或环境变量 `SMCRYPT_<算法>_FILE` 指定，
   支持 `file:` / `classpath:` 前缀）；
5. 默认密钥文件：`smcrypt-<算法>.key` 或 `smcrypt-<算法>.properties`（先文件系统、后 classpath）。

其中 `<算法>` 使用小写名称：`sm4`、`sm2`、`aes`、`des`、`rsa`、`ecc`。

### 密钥文件格式

- `.properties` 文件：读取 `key` 属性，兼容 `secret_key`；
- 其他文件：整体内容（去除首尾空白）作为密钥；
- 对称密钥：支持 hex、Base64，或直接使用原始口令（按 UTF-8 字节）；
- 非对称私钥：支持 PEM 与 Base64/DER（PKCS#8）两种格式。

示例：

```properties
# 直接配置密钥（hex 格式）
smcrypt.sm4.key=0123456789abcdeffedcba9876543210
# 从文件加载对称密钥
smcrypt.aes.file=classpath:keys/aes.key
# 从文件加载 RSA 私钥（PEM）
smcrypt.rsa.file=file:/etc/app/keys/rsa-private.pem
```

## 十、配置项参考

所有配置项均以 `smcrypt.<算法>.<项>` 命名，均有默认值。

| 配置项                          | 说明                                 | 默认值         |
|---------------------------------|--------------------------------------|----------------|
| `smcrypt.<算法>.key`            | 密钥内容                             | 无             |
| `smcrypt.<算法>.file`           | 密钥文件（`file:` / `classpath:`）   | 无             |
| `smcrypt.<算法>.transformation` | 完整 JCE 变换串，优先级最高          | 见算法表       |
| `smcrypt.<算法>.mode`           | 加密模式，与 padding 组合生成变换串  | 见算法表       |
| `smcrypt.<算法>.padding`        | 填充方式                             | `PKCS5Padding` |
| `smcrypt.<算法>.iv`             | 初始向量（hex / Base64）             | 无             |
| `smcrypt.<算法>.encoding`       | 加密输出所用编码（`hex` / `base64`） | `base64`       |

示例：

```properties
# 使用 CBC 模式与自定义 IV
smcrypt.aes.mode=CBC
smcrypt.aes.iv=00112233445566778899aabbccddeeff
# 使用 GCM 模式（完整变换串优先）
smcrypt.aes.transformation=AES/GCM/NoPadding
# 加密输出使用 hex 编码
smcrypt.sm4.encoding=hex
```

> 注意：`transformation` 优先级最高，配置后 `mode` / `padding` 不再参与变换串拼接。

## 十一、自定义算法 / 加密机接入

当内置算法无法满足需求（例如需要调用加密机、KMS，或使用未内置的算法）时，业务系统可实现 SPI 接口接入。

### 1. 实现处理器接口

```java
package com.example.crypto;

import com.houkunlin.smcrypt.handler.DecryptHandler;

public class HsmDecryptHandler implements DecryptHandler {

    @Override
    public String algorithm() {
        return "HSM"; // 同时作为密文前缀：HSMENC(...)
    }

    @Override
    public boolean support(String value) {
        return value != null && value.length() > 7
                && value.regionMatches(true, 0, "HSMENC(", 0, 7)
                && value.endsWith(")");
    }

    @Override
    public String getCipherText(String value) {
        return value.substring(7, value.length() - 1);
    }

    @Override
    public String getDecryptText(String value) throws Exception {
        return HsmClient.decrypt(getCipherText(value)); // 调用加密机解密
    }

    @Override
    public String getEncryptText(String plainText) throws Exception {
        return "HSMENC(" + HsmClient.encrypt(plainText) + ")";
    }
}
```

### 2. 注册处理器

以下两种方式任选其一：

方式一：`META-INF/services/com.houkunlin.smcrypt.handler.DecryptHandler`

```
com.example.crypto.HsmDecryptHandler
```

方式二：`META-INF/spring.factories`

```properties
com.houkunlin.smcrypt.handler.DecryptHandler=com.example.crypto.HsmDecryptHandler
```

> 处理器必须提供无参构造方法（SPI 实例化要求）。

### 3. 访问 Spring 环境（可选）

若处理器需要读取 Spring 配置或使用内置密钥解析能力，额外实现 `DecryptHandlerAware`：

```java
public class HsmDecryptHandler implements DecryptHandler, DecryptHandlerAware {

    private SmCryptContext context;

    @Override
    public void setContext(SmCryptContext context) {
        this.context = context;
    }

    // 通过 context.getProperty("...") 读取配置，
    // 或 context.resolveKey("HSM") 复用密钥解析逻辑
}
```

### 4. 覆盖内置算法

处理器加载顺序为： **内置 → `META-INF/services` → `spring.factories`**，按算法名称去重，
后加载者覆盖先加载者。因此业务系统注册的同名算法（例如 `SM4`）会覆盖内置实现，便于统一替换为
加密机等外部实现。

## 十二、加密工具

### API 方式

```java
import com.houkunlin.smcrypt.SmCryptContext;
import com.houkunlin.smcrypt.SmCryptEncryptor;
import org.springframework.core.io.FileSystemResourceLoader;

// 通过系统属性提供密钥
System.setProperty("smcrypt.sm4.key", "0123456789abcdeffedcba9876543210");

SmCryptEncryptor encryptor = new SmCryptEncryptor(
        new SmCryptContext(System::getProperty, new FileSystemResourceLoader()));

// 加密：返回 SM4ENC(base64,xxxx)
String cipher = encryptor.encrypt("SM4", "hello");

// 解密：自动识别算法与编码
String plain = encryptor.decrypt(cipher);
```

### 命令行方式

```bash
java -cp app.jar com.houkunlin.smcrypt.SmCryptCli \
    --algorithm SM4 --key 0123456789abcdeffedcba9876543210 --text hello
```

参数说明：

| 参数               | 简写 | 说明                                                    |
|--------------------|------|---------------------------------------------------------|
| `--algorithm`      | `-a` | 算法名称：`SM4` / `SM2` / `AES` / `DES` / `RSA` / `ECC` |
| `--text`           | `-t` | 待加密明文；配合 `--decrypt` 时表示待解密密文           |
| `--key`            | `-k` | 密钥内容（hex / Base64 / PEM）                          |
| `--file`           | `-f` | 密钥文件路径（`file:` / `classpath:`）                  |
| `--encoding`       | `-e` | 加密输出编码：`hex` / `base64`（默认 `base64`）         |
| `--transformation` |      | 自定义 JCE 变换串，如 `AES/GCM/NoPadding`               |
| `--mode`           |      | 加密模式，如 `CBC`、`GCM`、`C1C3C2`                     |
| `--padding`        |      | 填充方式，默认 `PKCS5Padding`                           |
| `--iv`             |      | 初始向量（hex / Base64）                                |
| `--decrypt`        |      | 解密模式                                                |
| `--help`           | `-h` | 显示帮助                                                |

解密示例：

```bash
java -cp app.jar com.houkunlin.smcrypt.SmCryptCli \
    --algorithm SM4 --key 0123456789abcdeffedcba9876543210 \
    --decrypt --text "SM4ENC(base64,xxxxxxxx)"
```

## 十三、日志

- 解密流程日志统一带 `[SMCRYPT]` 前缀，正常解密会打印属性名、算法、来源；
- 解密发生在 Spring 全局日志系统初始化之前，因此使用 **独立 LoggerContext**，配置为
  `logback-smcrypt.xml`（位于核心模块 `smcrypt-spring-boot` 的 `src/main/resources`，由各 starter 共享）；
- 可在应用工作目录放置同名 `logback-smcrypt.xml` 覆盖默认早期日志配置；
- 早期日志文件默认为 `{spring.application.name}.smcrypt.log`，输出目录取 `logging.file.path`（默认 `logs`），
  日志上下文在解密结束后即关闭，不会在后台持续滚动；
- 独立日志初始化失败时自动回退到 `System.out` / `System.err`，保证关键日志不丢失。

## 十四、注意事项与常见问题

**1. 密钥本身不能是密文。** 密钥在解密阶段被读取，必须为明文（或来自加密机等外部通道）。

**2. 找不到密钥时会发生什么？** 若配置中存在可识别的密文但未找到对应密钥，会打印 WARN 提示并跳过解密，
密文将保持原样，相关配置可能因值不正确而导致启动或运行失败。

**3. 解密失败会影响启动吗？** 单个属性解密失败会记录 ERROR 日志并跳过该属性，不影响其他属性与其他流程。

**4. 默认模式为什么是 ECB？** 为兼容历史密文，对称算法默认使用 ECB。 **生产环境建议使用 CBC/GCM 等更安全的
模式并配置 IV**，例如：

```properties
smcrypt.aes.transformation=AES/GCM/NoPadding
smcrypt.aes.iv=00112233445566778899aabbccddeeff
```

**5. 加密与解密的参数必须一致。** 模式、填充、IV、编码（生成密文时）需与解密端一致，否则无法还原明文。

**6. 多算法可共存。** 不同属性可使用不同算法前缀，各自使用对应的 `smcrypt.<算法>.*` 配置。

**7. 编码歧义。** 对无编码前缀且内容恰好同时满足 hex 与 Base64 的密文，建议显式补充编码前缀。

## 十五、构建与测试

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

- 核心单元测试：编解码自动识别、六种算法加解密往返、密钥解析、SPI 加载（`META-INF/services` 与
  `spring.factories` 两条路径）、解密引擎对 PropertySource 的替换与来源保留；
- 各 starter 集成测试：真实启动 `SpringApplication`，验证配置中的密文被解成明文。

## 十六、许可证

本项目基于 [Mulan Permissive Software License, Version 2](https://license.coscl.org.cn/MulanPSL2) 发布。
