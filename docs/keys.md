# 密钥配置

[返回 README](../README.md) | [文档索引](README.md)

## 来源优先级

在 Spring Boot 应用中，密钥的最终取值由 Spring `Environment` 决定，真实优先级（从高到低）为：

1. 命令行参数 `--smcrypt.<算法>.key=...` / `--smcrypt.<算法>.file=...`；
2. JVM 系统属性 `-Dsmcrypt.<算法>.key=...` / `-Dsmcrypt.<算法>.file=...`；
3. 操作系统环境变量 `SMCRYPT_<算法>_KEY` / `SMCRYPT_<算法>_FILE`（算法名大写，如 `SMCRYPT_SM4_KEY`）；
4. 配置文件 `application.yml` / `application.properties` 中的 `smcrypt.<算法>.key` / `smcrypt.<算法>.file`；
5. 默认密钥文件：`smcrypt-<算法>.key` 或 `smcrypt-<算法>.properties`（先当前工作目录、后 classpath）。

其中 `<算法>` 使用小写名称：`sm4`、`sm2`、`aes`、`des`、`desede`、`rsa`、`ecc`。

> 说明：第 1~3 项由 Spring 的 `commandLineArgs` / `systemProperties` / `systemEnvironment` 属性源提供，
> 它们的优先级都高于配置文件，因此即使 `application.yml` 中已经配置了密钥，也可以通过
> `-Dsmcrypt.<算法>.key=...` 或环境变量进行覆盖。`SecretKeyResolver` 中显式的 `System.getProperty` /
> `System.getenv` 查找仅用于 `SmCryptCli` 等非 Spring 场景兜底。空字符串视为未配置，会继续向下查找。
> 另外，`smcrypt.<算法>.file` 指定的密钥文件支持 `file:` / `classpath:` 前缀。

> ⚠️ **安全提示（默认密钥文件的隐式加载）**：第 5 项会按固定文件名从 **当前工作目录**隐式加载密钥，
> 该来源不经过任何显式配置。若部署目录不可控（例如运行账号可写、或被上传/同步了同名文件），
> 可能加载到非预期的密钥，且因无显式配置而难以排查。生产环境建议：
> - 显式配置 `smcrypt.<算法>.key` / `smcrypt.<算法>.file`，或使用环境变量、JVM 参数、命令行参数；
> - 确保应用工作目录下不存在 `smcrypt-<算法>.key` / `smcrypt-<算法>.properties` 同名文件；
> - 若必须使用默认密钥文件，请限制文件权限（仅必要用户可读）并纳入变更审计。

## 密钥文件格式

- `.properties` 文件：读取 `key` 属性，兼容 `secret_key`；
- 其他文件：整体内容（去除首尾空白）作为密钥；
- 对称密钥：支持 hex、Base64，或直接使用原始口令（按 UTF-8 字节）；
- 可通过 `smcrypt.<算法>.key-encoding` 显式指定对称密钥编码（`hex` / `base64` / `plain`）；未配置时自动识别，
  全为十六进制字符的密钥会被优先按 hex 解码，含空白口令建议显式配置为 `plain`；
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

## 相关文档

- [配置项参考](configuration.md)
- [加密工具](tools.md)
- [常见问题](faq.md)
