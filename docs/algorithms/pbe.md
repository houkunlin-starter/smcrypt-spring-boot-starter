# PBE 口令派生

[返回 README](../../README.md) | [算法索引](README.md)

`PBEENC(...)` 支持「用口令派生密钥」的方式加密配置值，前缀为 `PBEENC(...)`，配置前缀为 `smcrypt.pbe.*`。

统一载荷：`base64(salt ‖ iv ‖ cipher)`（盐随机内嵌；配置固定盐时不内嵌盐）。

## 两种模式

`PBEENC(...)` 有两种模式（`smcrypt.pbe.mode`）：

- **KDF（默认）**：口令经 PBKDF2 / scrypt / Argon2 派生密钥，再用 `transformation` 指定的对称算法加密；
- **JCE**：直接使用 JCE PBE 变换（如 `PBEWITHHMACSHA512ANDAES_256`），由 JVM 默认 Provider（SunJCE）执行。

## 配置项

| 配置项                                                              | mode | 说明                                                | 默认                          |
|---------------------------------------------------------------------|------|-----------------------------------------------------|-------------------------------|
| `smcrypt.pbe.password`                                              | 全部 | 口令（必填）                                        | 无                            |
| `smcrypt.pbe.mode`                                                  | 全部 | `KDF` / `JCE`                                       | `KDF`                         |
| `smcrypt.pbe.encoding`                                              | 全部 | 载荷编码                                            | `base64`                      |
| `smcrypt.pbe.transformation`                                        | KDF  | 对称加密变换串                                      | `AES/GCM/NoPadding`           |
| `smcrypt.pbe.kdf`                                                   | KDF  | `PBKDF2` / `SCRYPT` / `ARGON2`                      | `PBKDF2`                      |
| `smcrypt.pbe.kdf.prf`                                               | KDF  | PBKDF2 PRF：`HmacSHA256` / `HmacSHA512` / `HmacSM3` | `HmacSHA256`                  |
| `smcrypt.pbe.kdf.iterations`                                        | KDF  | 迭代次数                                            | `600000`（Argon2 为 3）       |
| `smcrypt.pbe.kdf.key-length`                                        | KDF  | 派生密钥位数                                        | 按算法                        |
| `smcrypt.pbe.kdf.salt`                                              | KDF  | 固定盐（hex / Base64）；不配则随机内嵌              | 无（随机内嵌）                |
| `smcrypt.pbe.kdf.salt-size`                                         | KDF  | 随机盐长度                                          | `16`                          |
| `smcrypt.pbe.kdf.cost` / `.block-size` / `.parallelism` / `.memory` | KDF  | scrypt / Argon2 参数                                | `65536` / `8` / `1` / `65536` |
| `smcrypt.pbe.transformation`                                        | JCE  | JCE PBE 变换串                                      | `PBEWITHHMACSHA512ANDAES_256` |
| `smcrypt.pbe.iterations`                                            | JCE  | 迭代次数                                            | `1000`                        |
| `smcrypt.pbe.salt-size` / `.iv-size`                                | JCE  | 盐 / IV 长度（0=无 IV）                             | 分组大小                      |
| `smcrypt.pbe.provider`                                              | JCE  | Provider 名                                         | JVM 默认（SunJCE）            |

## 示例

KDF + AES-GCM（推荐）：

```properties
smcrypt.pbe.password=my-passphrase
smcrypt.pbe.mode=KDF
smcrypt.pbe.transformation=AES/GCM/NoPadding
smcrypt.pbe.kdf=PBKDF2
smcrypt.pbe.kdf.iterations=600000
# 业务配置
spring.datasource.password=PBEENC(base64,xxxxxxxx)
```

JCE PBE 变换：

```properties
smcrypt.pbe.password=my-passphrase
smcrypt.pbe.mode=JCE
smcrypt.pbe.transformation=PBEWITHHMACSHA512ANDAES_256
```

> `PBEENC(...)` 的盐默认随机内嵌，因此同一明文每次加密结果不同；解密端只需相同口令与参数即可还原。
> 若配置固定盐（`smcrypt.pbe.kdf.salt`），则密文不含盐，需保证加解密两端盐一致。

## 安全建议

- 优先使用 `PBEENC` + `mode=KDF` + `AES/GCM/NoPadding` + `Argon2id`（或 PBKDF2 ≥ 600000 次）；
- 盐保持随机内嵌，勿固定；
- 口令通过环境变量 / JVM 参数提供，勿写入配置文件；
- `mode=JCE` 主要用于互操作 / 兼容。

## 相关文档

- [Jasypt 兼容](jasypt.md)
- [算法安全性与选型建议](../security.md)
- [加密工具](../tools.md)
