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
  - [8.1 算法安全性与选型建议](#81-算法安全性与选型建议)
- [九、密钥配置](#九密钥配置)
- [十、配置项参考](#十配置项参考)
- [十一、SM9 标识加密](#十一sm9-标识加密)
- [十二、自定义算法 / 加密机接入](#十二自定义算法--加密机接入)
- [十三、加密工具](#十三加密工具)
- [十四、日志](#十四日志)
- [十五、注意事项与常见问题](#十五注意事项与常见问题)
- [十六、构建与测试](#十六构建与测试)
- [十七、许可证](#十七许可证)

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
- **多算法**：内置国密 SM2 / SM4 / SM9 (标识加密) 及 AES / DES / DESEDE (3DES) / RSA / ECC；
- **多编码**：支持 hex、Base64，可显式声明或自动识别；
- **可扩展**：业务系统可通过 SPI 接入自定义算法，或对接加密机（HSM）等外部解密能力。

## 二、核心特性

- 同时支持 Spring Boot 2 / 3 / 4，按版本选用对应 Starter；
- 内置算法：SM4、SM2、SM9（标识加密）、AES、DES、DESEDE (3DES)、RSA、ECC（ECIES），基于 BouncyCastle；
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
SM9 需要 BouncyCastle **1.86 及以上**（本 starter 已使用 1.86）。

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

使用命令行工具生成 SM4 密文（详见 [加密工具](#十三加密工具)）：

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

| 算法   | 前缀             |
|--------|------------------|
| SM4    | `SM4ENC(...)`    |
| SM2    | `SM2ENC(...)`    |
| AES    | `AESENC(...)`    |
| DES    | `DESENC(...)`    |
| DESEDE | `DESEDEENC(...)` |
| RSA    | `RSAENC(...)`    |
| ECC    | `ECCENC(...)`    |

> SM9 为标识加密（IBC），前缀为 `SM9ENC(...)`，其配置方式与其它算法不同，单独说明见「十一、SM9 标识加密」。

## 八、支持的算法

| 算法   | 前缀        | 默认变换                  | 密钥要求             | 说明                             |
|--------|-------------|---------------------------|----------------------|----------------------------------|
| SM4    | `SM4ENC`    | `SM4/ECB/PKCS5Padding`    | 16 字节对称密钥      | 国密分组密码                     |
| SM2    | `SM2ENC`    | `SM2`（C1C3C2）           | EC 私钥（sm2p256v1） | 国密非对称，默认 C1C3C2 顺序     |
| AES    | `AESENC`    | `AES/ECB/PKCS5Padding`    | 16 / 24 / 32 字节    | 国际通用对称加密                 |
| DES    | `DESENC`    | `DES/ECB/PKCS5Padding`    | 8 字节               | 兼容遗留系统                     |
| DESEDE | `DESEDEENC` | `DESede/ECB/PKCS5Padding` | 16 / 24 字节         | 3DES（Triple DES），兼容遗留系统 |
| RSA    | `RSAENC`    | `RSA/ECB/PKCS1Padding`    | RSA 私钥             | 非对称加密                       |
| ECC    | `ECCENC`    | `ECIES`                   | EC 私钥              | 基于 ECIES 的椭圆曲线加密        |

说明：

- 对称算法通过 JCE `Cipher` 实现，支持通过 `transformation` / `mode` / `padding` / `iv` 自定义；
- 3DES（`DESEDE`）密钥支持 16 字节（2-key，K1=K3）或 24 字节（3-key），属过时算法，仅建议用于兼容遗留系统；
- 非对称算法仅需提供 **私钥**：解密使用私钥，加密时自动由私钥推导公钥；
- SM2 密文顺序可通过 `smcrypt.sm2.mode=C1C2C3` 切换；
- SM2 / ECC 的椭圆曲线由密钥本身决定，无需额外配置；
- 内置算法均由 BouncyCastle 提供，启动时以显式 Provider 方式使用，不污染全局 JCE Provider。

> SM9 为标识加密（IBC），密钥材料与配置方式与上表不同，单独说明见「十一、SM9 标识加密」。

### 8.1 算法安全性与选型建议

各算法的安全状态与选型建议如下（新系统请优先选择「推荐」的算法）：

| 算法           | 密钥 / 安全强度             | 安全状态      | 选型建议                                     |
|----------------|-----------------------------|---------------|----------------------------------------------|
| AES            | 128 / 192 / 256 位          | 安全          | **推荐**；优先 GCM，避免 ECB                 |
| SM4            | 128 位                      | 安全          | **推荐**（国密合规）；优先 CBC/GCM，避免 ECB |
| SM2            | 256 位曲线（约 128 位）     | 安全          | **推荐**（国密合规）；配合 SM3 使用          |
| RSA            | 依密钥长度                  | 2048 位起安全 | 可用；推荐 3072 位 + OAEP                    |
| ECC（ECIES）   | 256 位曲线（约 128 位）     | 安全          | 可用；曲线不低于 256 位                      |
| SM9            | 256 位 BN 曲线（约 128 位） | 安全          | 仅标识密码（IBC）场景；依赖 KGC              |
| 3DES（DESEDE） | 112 / 168 位                | **已过时**    | 仅兼容遗留系统；NIST 自 2024 年起禁用其加密  |
| DES            | 56 位                       | **已破解**    | 禁止用于新系统，仅兼容                       |

**推荐优先级**

- 首选（新系统）：
  - 对称：`AES-256-GCM`（国际通用）或 `SM4-GCM` / `SM4-CBC`（国密合规）；
  - 非对称：`RSA-3072 + OAEP`（国际通用）或 `SM2`（国密合规）。
- 可接受（需正确使用）：
  - `AES-128/192/256-CBC`、`SM4-CBC`（必须使用随机且不可复用的 IV；非 AEAD 模式无完整性校验，防篡改请改用 GCM 或自行增加
    MAC）；
  - `RSA-2048 + OAEP`、`ECC/ECIES`（P-256 及以上曲线）。
- 不推荐 / 仅兼容：
  - `DES`、`3DES(DESEDE)`：强度不足或已过时；
  - `RSA-1024`：已不安全；
  - `ECB` 模式：会泄露明文分组规律（结构化数据尤其危险）；
  - `RSA` PKCS#1 v1.5 填充：存在填充预言（Bleichenbacher）风险。

**使用注意**

- 本项目为兼容历史密文，对称算法 **默认使用 `ECB/PKCS5Padding`**；生产环境建议改用 CBC 或 GCM，并配置随机 IV：
  ```properties
  # AES-GCM（推荐，需随机 12 字节 IV）
  smcrypt.aes.transformation=AES/GCM/NoPadding
  smcrypt.aes.iv=<每次加密随机生成的 IV>
  ```
- `IV` 必须随机、每次加密不同且不得复用（GCM 复用 IV 会导致密钥流泄露）；
- CBC + PKCS5/PKCS7 存在填充预言攻击面，建议改用 AEAD（GCM）；
- **完整性校验说明**：GCM 等 AEAD 模式自带认证标签（JCE 解密时会校验，密文被篡改会抛 `AEADBadTagException`）；SM2 / SM9 /
  ECIES 算法本身含 C3 或 MAC 校验；CBC / ECB 等非 AEAD 模式可配置 `smcrypt.<算法>.mac=HmacSM3|HmacSHA256` 启用
  encrypt-then-MAC（密文载荷 `密文 || MAC`，解密前校验）， **未启用时本 starter 不提供额外完整性校验**；
- RSA 加密请使用 OAEP（`RSA/ECB/OAEPWithSHA-256AndMGF1Padding`），避免 PKCS#1 v1.5；
- 密钥长度下限：AES / SM4 至少 128 位，RSA 至少 2048 位（推荐 3072 位），ECC / SM2 至少 256 位。

> 合规提示：需满足国密合规时优先使用 `SM2` / `SM4` / `SM9`；面向国际或通用生态时使用 `AES` / `RSA` / `ECC`。

## 九、密钥配置

### 来源优先级

在 Spring Boot 应用中，密钥的最终取值由 Spring `Environment` 决定，真实优先级（从高到低）为：

1. 命令行参数 `--smcrypt.<算法>.key=...` / `--smcrypt.<算法>.file=...`；
2. JVM 系统属性 `-Dsmcrypt.<算法>.key=...` / `-Dsmcrypt.<算法>.file=...`；
3. 操作系统环境变量 `SMCRYPT_<算法>_KEY` / `SMCRYPT_<算法>_FILE`（算法名大写，如 `SMCRYPT_SM4_KEY`）；
4. 配置文件 `application.yml` / `application.properties` 中的 `smcrypt.<算法>.key` / `smcrypt.<算法>.file`；
5. 默认密钥文件：`smcrypt-<算法>.key` 或 `smcrypt-<算法>.properties`（先文件系统、后 classpath）。

其中 `<算法>` 使用小写名称：`sm4`、`sm2`、`aes`、`des`、`desede`、`rsa`、`ecc`。

> 说明：第 1~3 项由 Spring 的 `commandLineArgs` / `systemProperties` / `systemEnvironment` 属性源提供，
> 它们的优先级都高于配置文件，因此即使 `application.yml` 中已经配置了密钥，也可以通过
> `-Dsmcrypt.<算法>.key=...` 或环境变量进行覆盖。`SecretKeyResolver` 中显式的 `System.getProperty` /
> `System.getenv` 查找仅用于 `SmCryptCli` 等非 Spring 场景兜底。空字符串视为未配置，会继续向下查找。
> 另外，`smcrypt.<算法>.file` 指定的密钥文件支持 `file:` / `classpath:` 前缀。

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

所有配置项均以 `smcrypt.<算法>.<项>` 命名，其中 `<算法>` 使用小写名称
（`sm4`、`sm2`、`aes`、`des`、`desede`、`rsa`、`ecc`），未配置时使用默认值。

### 10.1 配置项总览

| 配置项                          | 说明                                                                      | 默认值             |
|---------------------------------|---------------------------------------------------------------------------|--------------------|
| `smcrypt.<算法>.key`            | 密钥内容（对称：hex / Base64 / 口令；非对称：PEM / Base64(DER)）          | 无                 |
| `smcrypt.<算法>.file`           | 密钥文件（`file:` / `classpath:`，`.properties` 读 `key` / `secret_key`） | 无                 |
| `smcrypt.<算法>.transformation` | 完整 JCE 变换串，优先级最高                                               | 见算法表           |
| `smcrypt.<算法>.mode`           | 加密模式，与 `padding` 组合生成变换串                                     | 见算法表           |
| `smcrypt.<算法>.padding`        | 填充方式                                                                  | `PKCS5Padding`     |
| `smcrypt.<算法>.iv`             | 初始向量（hex / Base64，自动识别）                                        | 无                 |
| `smcrypt.<算法>.encoding`       | 加密输出所用编码（`hex` / `base64`）                                      | `base64`           |
| `smcrypt.<算法>.mac`            | 完整性校验算法（`HmacSM3` / `HmacSHA256`），仅对称算法有效                | 无（不启用）       |
| `smcrypt.<算法>.mac-key`        | MAC 密钥（hex / Base64 / 口令），默认复用加密密钥                         | 无（复用加密密钥） |

> `transformation` 优先级最高：一旦配置，`mode` / `padding` 不再参与变换串拼接。
> `encoding` 只影响 **加密输出**，解密时编码会自动识别，无需配置。
> 配置 `mac` 后启用 encrypt-then-MAC：加密输出为 `密文 || MAC`，解密前先校验 MAC，校验失败会抛出异常；
> `mac` / `mac-key` 仅对对称算法（SM4 / AES / DES / DESEDE）有效。

### 10.2 算法与参数支持矩阵

| 配置项           | SM4 / AES / DES / DESEDE（对称） | RSA                                   | ECC（ECIES）           | SM2                    |
|------------------|----------------------------------|---------------------------------------|------------------------|------------------------|
| `transformation` | 支持                             | 支持                                  | 支持（默认 `ECIES`）   | 不生效                 |
| `mode`           | 支持                             | 参与拼接（建议改用 `transformation`） | 不可设置               | 仅 `C1C3C2` / `C1C2C3` |
| `padding`        | 支持                             | 参与拼接（建议改用 `transformation`） | 不可设置               | 不生效                 |
| `iv`             | 支持（非 ECB 模式）              | 不适用                                | 不适用                 | 不适用                 |
| `encoding`       | 支持                             | 支持                                  | 支持                   | 支持                   |
| `mac`            | 支持（`HmacSM3` / `HmacSHA256`） | 不适用                                | 不适用（算法自带 MAC） | 不适用（算法自带 C3）  |

说明：

- **SM4 / AES / DES / DESEDE**：`transformation` / `mode` / `padding` / `iv` 可自由组合；ECB 模式无需 `iv`，CBC/GCM 等模式需提供
  `iv`；
- **完整性校验（MAC）**：仅对称算法支持；配置 `smcrypt.<算法>.mac` 后启用 encrypt-then-MAC（密文载荷为 `密文 || MAC`），
  解密时先校验 MAC，防篡改；非对称算法（SM2 / SM9 / ECIES）已内置完整性校验，无需额外配置；
- **RSA**：建议直接用 `transformation` 指定填充，如 `RSA/ECB/PKCS1Padding`、`RSA/ECB/OAEPWithSHA-256AndMGF1Padding`；
- **ECC**：基于 ECIES，仅使用 `transformation`（默认 `ECIES`）， **不要**配置 `mode` / `padding`，否则会拼出非法变换串；
- **SM2**：`mode` 仅用于选择密文顺序（默认 `C1C3C2`），其余参数不生效；
- `mode` / `padding` 仅在 **未配置** `transformation` 且 **设置了** `mode` 时才参与变换串拼接；只配置 `padding`
  不会改变默认变换串；
- `iv` 长度必须与算法分组一致：AES / SM4 为 16 字节（32 位 hex），DES / DESEDE 为 8 字节（16 位 hex）；GCM 推荐 12 字节。

### 10.3 常用场景示例

#### 场景 1：默认零配置（ECB）

对称算法默认使用 `ECB/PKCS5Padding`，只需配置密钥即可：

```properties
# SM4
smcrypt.sm4.key=0123456789abcdeffedcba9876543210
# AES
smcrypt.aes.key=00112233445566778899aabbccddeeff
# DES
smcrypt.des.key=0123456789abcdef
# 3DES（16 字节 2-key 或 24 字节 3-key）
smcrypt.desede.key=0123456789abcdeffedcba98765432100123456789abcdef
```

#### 场景 2：CBC 模式 + 自定义 IV

```properties
smcrypt.aes.mode=CBC
smcrypt.aes.padding=PKCS5Padding
# 16 字节 IV（32 位 hex）
smcrypt.aes.iv=00112233445566778899aabbccddeeff
```

#### 场景 3：GCM（AEAD，无需 padding）

```properties
# GCM 必须使用 NoPadding，并显式提供 IV（推荐 12 字节）
smcrypt.aes.transformation=AES/GCM/NoPadding
smcrypt.aes.iv=00112233445566778899aabb
```

#### 场景 4：国密 SM4（ECB / CBC）

```properties
# 默认 ECB
smcrypt.sm4.key=0123456789abcdeffedcba9876543210
# 如需 CBC，需配置 16 字节 IV
smcrypt.sm4.mode=CBC
smcrypt.sm4.iv=0123456789abcdeffedcba9876543210
```

#### 场景 5：SM2 切换密文顺序

```properties
# 默认 C1C3C2
smcrypt.sm2.key=-----BEGIN PRIVATE KEY-----...-----END PRIVATE KEY-----
# 对方系统使用 C1C2C3 时切换
smcrypt.sm2.mode=C1C2C3
```

#### 场景 6：RSA 使用 OAEP 填充

```properties
smcrypt.rsa.transformation=RSA/ECB/OAEPWithSHA-256AndMGF1Padding
smcrypt.rsa.key=-----BEGIN PRIVATE KEY-----...-----END PRIVATE KEY-----
```

#### 场景 7：加密输出使用 hex 编码

```properties
smcrypt.sm4.encoding=hex
# 生成结果形如：SM4ENC(hex,0123abcd...)
```

#### 场景 8：密钥来源（文件 / 环境变量 / JVM 参数）

```properties
# 1) 直接配置密钥
smcrypt.aes.key=00112233445566778899aabbccddeeff
# 2) 从文件加载（file: 或 classpath:）
smcrypt.aes.file=classpath:keys/aes.key
# 3) 从非对称私钥文件加载（PEM）
smcrypt.rsa.file=file:/etc/app/keys/rsa-private.pem
```

```bash
# 4) 环境变量（算法名大写）
SMCRYPT_AES_KEY=00112233445566778899aabbccddeeff
# 5) JVM 参数（可覆盖 application.yml 中的同名配置）
java -Dsmcrypt.aes.key=00112233445566778899aabbccddeeff -jar app.jar
```

#### 场景 9：YAML 配置写法

```yaml
smcrypt:
  sm4:
    key: 0123456789abcdeffedcba9876543210
    mode: CBC
    iv: 0123456789abcdeffedcba9876543210
  aes:
    transformation: AES/GCM/NoPadding
    iv: 00112233445566778899aabb
    encoding: hex
```

#### 场景 10：3DES（兼容遗留系统）

```properties
# 16 字节 2-key（K1=K3）或 24 字节 3-key 均可，由密钥长度决定
smcrypt.desede.key=0123456789abcdeffedcba9876543210
# 如需 CBC，需配置 8 字节 IV（16 位 hex）
smcrypt.desede.mode=CBC
smcrypt.desede.iv=0123456789abcdef
```

> 3DES 属过时算法，仅建议用于兼容遗留系统密文；新系统请使用 AES 或 SM4。

#### 场景 11：启用完整性校验（MAC）

```properties
# HMAC-SM3，MAC 密钥默认复用加密密钥
smcrypt.sm4.mac=HmacSM3
# HMAC-SHA256，单独指定 MAC 密钥
smcrypt.aes.mac=HmacSHA256
smcrypt.aes.mac-key=aabbccddeeff00112233445566778899
```

启用后加密输出为 `SM4ENC(base64,<密文||MAC>)`（encrypt-then-MAC），解密时会先校验 MAC，
密文被篡改将抛出异常；`mac` / `mac-key` 仅对对称算法有效。

> 以上配置项适用于 SM4 / AES / DES / DESEDE / RSA / ECC 等算法；SM9 的配置项单独见下一节。

## 十一、SM9 标识加密

SM9 是国密 **标识密码（IBC，Identity-Based Cryptography） **算法（GB/T 38635、GM/T 0044），基于 BN 曲线
上的双线性对。其特点是**公钥即身份**（如邮箱、工号），用户私钥由 KGC（密钥生成中心）用主私钥按身份派生，
无需数字证书。本 starter 仅集成其中的 **公钥加密 / KEM** 能力，用于解密配置密文。

> 需要 BouncyCastle **1.86 及以上**版本（本 starter 已使用 1.86）。
> SM9 属小众且强依赖 KGC 的算法，新系统建议优先使用 SM2 或 SM4。

### 11.1 算法与密文格式

- 密文前缀：`SM9ENC(...)`，编码规则与其它算法一致（可显式 `hex,` / `base64,`，缺省自动识别）；
- 数据封装方式（`smcrypt.sm9.mode`）：
  - `SM4`（默认）：SM4-ECB / PKCS#7 封装；
  - `STREAM`：KDF 流（XOR）封装；
- 密文结构（`smcrypt.sm9.cipher-format`）：
  - `raw`（默认）：引擎原生 `C1(64) || C3(32) || C2`；
  - `asn1`：GM/T 0080-2020 的 `SM9Cipher` ASN.1 结构（`enType`：0=STREAM，1=SM4-ECB）。

> 密文格式与封装方式必须与生成密文的一方（GmSSL、铜锁、加密机等）保持一致，否则无法解密。

### 11.2 密钥材料

SM9 的密钥材料包含四项，均需提供：

| 材料            | 说明                                | 长度                          |
|-----------------|-------------------------------------|-------------------------------|
| 用户私钥 `de`   | 由 KGC 按身份派生，用于解密         | G2 点，129 字节（`0x04‖x‖y`） |
| 主公钥 `Ppub-e` | KGC 的主公钥，用于加密与重建私钥    | G1 点，65 字节（`0x04‖x‖y`）  |
| 身份 `identity` | 用户标识字符串（UTF-8）             | 任意                          |
| `hid`           | 私钥生成函数标识，KEM / 加密为 `03` | 1 字节                        |

- `de` 与 `Ppub-e` 以 hex 或 Base64 提供；`de` 的编码 **不含**主公钥 / 身份 / hid，因此后三者必须另行配置；
- 仅需解密时，`de` + `Ppub-e` + `identity` + `hid` 缺一不可（身份参与 KDF 输入，主公钥用于重建私钥）。

### 11.3 配置项

SM9 的配置项独立于其它算法，统一以 `smcrypt.sm9.` 为前缀：

| 配置项                          | 说明                                 | 默认值   |
|---------------------------------|--------------------------------------|----------|
| `smcrypt.sm9.private-key`       | 用户私钥 `de`（hex / Base64）        | 无       |
| `smcrypt.sm9.master-public-key` | 主公钥 `Ppub-e`（hex / Base64）      | 无       |
| `smcrypt.sm9.identity`          | 身份字符串（UTF-8）                  | 无       |
| `smcrypt.sm9.hid`               | 私钥生成函数标识（hex，KEM 用 `03`） | `03`     |
| `smcrypt.sm9.mode`              | 数据封装方式：`SM4` / `STREAM`       | `SM4`    |
| `smcrypt.sm9.cipher-format`     | 密文格式：`raw` / `asn1`             | `raw`    |
| `smcrypt.sm9.encoding`          | 加密输出编码：`hex` / `base64`       | `base64` |

> SM9 不使用 `transformation` / `padding` / `iv` 等参数。

### 11.4 配置示例

```properties
# 用户私钥 de（G2 点，Base64）
smcrypt.sm9.private-key=BASE64_OF_DE
# 主公钥 Ppub-e（G1 点，Base64）
smcrypt.sm9.master-public-key=BASE64_OF_PPUBE
# 身份
smcrypt.sm9.identity=alice@example.com
# 私钥生成函数标识（KEM 默认 03，可省略）
smcrypt.sm9.hid=03
# 数据封装方式：SM4（默认）或 STREAM
smcrypt.sm9.mode=SM4
# 密文格式：raw（默认）或 asn1
smcrypt.sm9.cipher-format=raw
```

```yaml
smcrypt:
  sm9:
    private-key: BASE64_OF_DE
    master-public-key: BASE64_OF_PPUBE
    identity: alice@example.com
    hid: "03"
    mode: SM4
    cipher-format: raw
```

配置完成后，业务代码照常读取被加密的配置项即可（解密在启动早期自动完成）：

```properties
spring.datasource.password=SM9ENC(base64,xxxxxxxx)
```

### 11.5 生成密文（API）

命令行工具未提供 SM9 专用参数，可通过 `SmCryptEncryptor` API 或系统属性使用：

```text
System.setProperty("smcrypt.sm9.private-key","BASE64_OF_DE");
System.setProperty("smcrypt.sm9.master-public-key","BASE64_OF_PPUBE");
System.setProperty("smcrypt.sm9.identity","alice@example.com");

SmCryptEncryptor encryptor = new SmCryptEncryptor(new SmCryptContext(System::getProperty, new FileSystemResourceLoader()));
String cipher = encryptor.encrypt("SM9", "my-secret");
// 输出形如：SM9ENC(base64,xxxx)
```

### 11.6 限制

- 需要 BouncyCastle 1.86+；
- 仅支持数据封装类型 `STREAM` 与 `SM4-ECB`（GM/T 0080 的 SM4-CBC / OFB / CFB 未实现）；
- 用户私钥无标准 Java / DER / PEM 编码，须按 G2 点裸编码（129 字节）提供；
- 密钥交换与数字签名不在本 starter 范围内。

## 十二、自定义算法 / 加密机接入

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

## 十三、加密工具

### API 方式

```text
import com.houkunlin.smcrypt.SmCryptContext;
import com.houkunlin.smcrypt.SmCryptEncryptor;
import org.springframework.core.io.FileSystemResourceLoader;

// 通过系统属性提供密钥
System.setProperty("smcrypt.sm4.key","0123456789abcdeffedcba9876543210");

SmCryptEncryptor encryptor = new SmCryptEncryptor(new SmCryptContext(System::getProperty, new FileSystemResourceLoader()));

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

| 参数               | 简写 | 说明                                                               |
|--------------------|------|--------------------------------------------------------------------|
| `--algorithm`      | `-a` | 算法名称：`SM4` / `SM2` / `AES` / `DES` / `DESEDE` / `RSA` / `ECC` |
| `--text`           | `-t` | 待加密明文；配合 `--decrypt` 时表示待解密密文                      |
| `--key`            | `-k` | 密钥内容（hex / Base64 / PEM）                                     |
| `--file`           | `-f` | 密钥文件路径（`file:` / `classpath:`）                             |
| `--encoding`       | `-e` | 加密输出编码：`hex` / `base64`（默认 `base64`）                    |
| `--transformation` |      | 自定义 JCE 变换串，如 `AES/GCM/NoPadding`                          |
| `--mode`           |      | 加密模式，如 `CBC`、`GCM`、`C1C3C2`                                |
| `--padding`        |      | 填充方式，默认 `PKCS5Padding`                                      |
| `--iv`             |      | 初始向量（hex / Base64）                                           |
| `--decrypt`        |      | 解密模式                                                           |
| `--help`           | `-h` | 显示帮助                                                           |

解密示例：

```bash
java -cp app.jar com.houkunlin.smcrypt.SmCryptCli \
    --algorithm SM4 --key 0123456789abcdeffedcba9876543210 \
    --decrypt --text "SM4ENC(base64,xxxxxxxx)"
```

## 十四、日志

- 解密流程日志统一带 `[SMCRYPT]` 前缀，正常解密会打印属性名、算法、来源；
- 解密发生在 Spring 全局日志系统初始化之前，因此使用 **独立 LoggerContext**，配置为
  `logback-smcrypt.xml`（位于核心模块 `smcrypt-spring-boot` 的 `src/main/resources`，由各 starter 共享）；
- 可在应用工作目录放置同名 `logback-smcrypt.xml` 覆盖默认早期日志配置；
- 早期日志文件默认为 `{spring.application.name}.smcrypt.log`，输出目录取 `logging.file.path`（默认 `logs`），
  日志上下文在解密结束后即关闭，不会在后台持续滚动；
- 独立日志初始化失败时自动回退到 `System.out` / `System.err`，保证关键日志不丢失。

## 十五、注意事项与常见问题

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

## 十六、构建与测试

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

## 十七、许可证

本项目基于 [Mulan Permissive Software License, Version 2](https://license.coscl.org.cn/MulanPSL2) 发布。
