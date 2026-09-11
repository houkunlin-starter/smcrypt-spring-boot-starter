# 配置项参考

[返回 README](../README.md) | [文档索引](README.md)

所有配置项均以 `smcrypt.<算法>.<项>` 命名，其中 `<算法>` 使用小写名称
（`sm4`、`sm2`、`aes`、`des`、`desede`、`chacha20`、`gost3412`、`dstu7624`、`rc6`、`camellia`、`aria`、`seed`、`rsa`、`ecc`），
未配置时使用默认值。SM9 使用独立的 `smcrypt.sm9.*`，PBE / Jasypt 使用 `smcrypt.pbe.*` / `smcrypt.jasypt.*`。

## 配置项总览

| 配置项                          | 阶段   | 说明                                                                      | 默认值             |
|---------------------------------|--------|---------------------------------------------------------------------------|--------------------|
| `smcrypt.<算法>.key`            | 加解密 | 密钥内容（对称：hex / Base64 / 口令；非对称：PEM / Base64(DER)）          | 无                 |
| `smcrypt.<算法>.file`           | 加解密 | 密钥文件（`file:` / `classpath:`，`.properties` 读 `key` / `secret_key`） | 无                 |
| `smcrypt.<算法>.transformation` | 加解密 | 完整 JCE 变换串，优先级最高                                               | 见算法表           |
| `smcrypt.<算法>.mode`           | 加解密 | 加密模式，与 `padding` 组合生成变换串                                     | 见算法表           |
| `smcrypt.<算法>.padding`        | 加解密 | 填充方式                                                                  | `PKCS5Padding`     |
| `smcrypt.<算法>.iv`             | 加解密 | 初始向量（hex / Base64，自动识别）                                        | 无                 |
| `smcrypt.<算法>.encoding`       | 加密   | 加密输出所用编码（`hex` / `base64`）                                      | `base64`           |
| `smcrypt.<算法>.mac`            | 加解密 | 完整性校验算法（`HmacSM3` / `HmacSHA256`），仅对称算法有效                | 无（不启用）       |
| `smcrypt.<算法>.mac-key`        | 加解密 | MAC 密钥（hex / Base64 / 口令），默认复用加密密钥                         | 无（复用加密密钥） |

> `阶段` 列说明：`加密` 表示仅在生成密文时使用；`解密` 表示仅在解密时使用；`加解密` 表示加密与解密两端都需保持一致。
> `transformation` 优先级最高：一旦配置，`mode` / `padding` 不再参与变换串拼接。
> `encoding` 只影响 **加密输出**，解密时编码会自动识别，无需配置。
> 配置 `mac` 后启用 encrypt-then-MAC：加密输出为 `密文 || MAC`，解密前先校验 MAC，校验失败会抛出异常；
> `mac` / `mac-key` 仅对对称算法有效。

## 算法与参数支持矩阵

| 配置项           | 对称算法                         | RSA                                   | ECC（ECIES）           | SM2                    |
|------------------|----------------------------------|---------------------------------------|------------------------|------------------------|
| `transformation` | 支持                             | 支持                                  | 支持（默认 `ECIES`）   | 不生效                 |
| `mode`           | 支持                             | 参与拼接（建议改用 `transformation`） | 不可设置               | 仅 `C1C3C2` / `C1C2C3` |
| `padding`        | 支持                             | 参与拼接（建议改用 `transformation`） | 不可设置               | 不生效                 |
| `iv`             | 支持（非 ECB 模式）              | 不适用                                | 不适用                 | 不适用                 |
| `encoding`       | 支持                             | 支持                                  | 支持                   | 支持                   |
| `mac`            | 支持（`HmacSM3` / `HmacSHA256`） | 不适用                                | 不适用（算法自带 MAC） | 不适用（算法自带 C3）  |

说明：

- **对称算法**（SM4 / AES / DES / DESEDE / CHACHA20 / GOST3412 / DSTU7624 / RC6 / CAMELLIA / ARIA / SEED）：
  `transformation` / `mode` / `padding` /
  `iv` 可自由组合；ECB 模式无需 `iv`，CBC/GCM 等模式需提供
  `iv`；ChaCha20-Poly1305 为 AEAD，不支持 `mode` / `padding`，需 12 字节 nonce；
- **完整性校验（MAC）**：仅对称算法支持；配置 `smcrypt.<算法>.mac` 后启用 encrypt-then-MAC（密文载荷为 `密文 || MAC`），
  解密时先校验 MAC，防篡改；非对称算法（SM2 / SM9 / ECIES）已内置完整性校验，无需额外配置；
- **RSA**：建议直接用 `transformation` 指定填充，如 `RSA/ECB/PKCS1Padding`、`RSA/ECB/OAEPWithSHA-256AndMGF1Padding`；
- **ECC**：基于 ECIES，仅使用 `transformation`（默认 `ECIES`）， **不要**配置 `mode` / `padding`，否则会拼出非法变换串；
- **SM2**：`mode` 仅用于选择密文顺序（默认 `C1C3C2`），其余参数不生效；
- `mode` / `padding` 仅在 **未配置** `transformation` 且 **设置了** `mode` 时才参与变换串拼接；只配置 `padding`
  不会改变默认变换串；
- `iv` 长度必须与算法分组一致：AES / SM4 / GOST3412 / DSTU7624 / RC6 / CAMELLIA / ARIA / SEED 为 16 字节，DES / DESEDE 为
  8 字节；GCM 与 ChaCha20-Poly1305 的 nonce 为 12 字节。

## 常用场景示例

### 默认零配置（ECB）

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

### CBC 模式 + 自定义 IV

```properties
smcrypt.aes.mode=CBC
smcrypt.aes.padding=PKCS5Padding
# 16 字节 IV（32 位 hex）
smcrypt.aes.iv=00112233445566778899aabbccddeeff
```

### GCM（AEAD，无需 padding）

```properties
# GCM 必须使用 NoPadding，并显式提供 IV（推荐 12 字节）
smcrypt.aes.transformation=AES/GCM/NoPadding
smcrypt.aes.iv=00112233445566778899aabb
```

### 加密输出使用 hex 编码

```properties
smcrypt.sm4.encoding=hex
# 生成结果形如：SM4ENC(hex,0123abcd...)
```

### 启用完整性校验（MAC）

```properties
# HMAC-SM3，MAC 密钥默认复用加密密钥
smcrypt.sm4.mac=HmacSM3
# HMAC-SHA256，单独指定 MAC 密钥
smcrypt.aes.mac=HmacSHA256
smcrypt.aes.mac-key=aabbccddeeff00112233445566778899
```

启用后加密输出为 `SM4ENC(base64,<密文||MAC>)`（encrypt-then-MAC），解密时会先校验 MAC，
密文被篡改将抛出异常；`mac` / `mac-key` 仅对对称算法有效。

### 密钥来源（文件 / 环境变量 / JVM 参数）

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

### YAML 配置写法

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

## 相关文档

- [密钥配置](keys.md)
- [算法索引](algorithms/README.md)
- [算法安全性与选型建议](security.md)
- [SM9 标识加密](algorithms/sm9.md)
- [PBE](algorithms/pbe.md) / [Jasypt](algorithms/jasypt.md)
