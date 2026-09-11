# RSA

[返回 README](../../README.md) | [算法索引](README.md)

## 简介

RSA 是最广泛使用的非对称加密算法。本 starter 仅需提供 **私钥**：解密使用私钥，加密时自动由私钥推导公钥。

## 密文格式与算法前缀

前缀为 `RSAENC(...)`，编码规则见 [密文格式](../cipher-format.md)：

```
RSAENC(base64,xxxxxxxx)
```

## 默认参数

| 项目   | 值                            |
|--------|-------------------------------|
| 变换串 | `RSA/ECB/PKCS1Padding`        |
| 密钥   | RSA 私钥（PEM / Base64(DER)） |
| IV     | 不适用                        |

> RSA 建议直接使用 `transformation` 指定填充；`mode` / `padding` 会参与拼接但推荐改用 `transformation`。

## 配置项

```properties
smcrypt.rsa.key=-----BEGIN PRIVATE KEY-----...-----END PRIVATE KEY-----
smcrypt.rsa.transformation=RSA/ECB/PKCS1Padding
smcrypt.rsa.encoding=base64
```

## 示例

使用 OAEP 填充（推荐）：

```properties
smcrypt.rsa.transformation=RSA/ECB/OAEPWithSHA-256AndMGF1Padding
smcrypt.rsa.key=-----BEGIN PRIVATE KEY-----...-----END PRIVATE KEY-----
```

## 安全说明

- 2048 位起安全； **推荐 3072 位 + OAEP**；
- 避免 PKCS#1 v1.5 填充（存在 Bleichenbacher 填充预言风险）；
- RSA-1024 已不安全。

## 相关文档

- [配置项参考](../configuration.md)
- [算法安全性与选型建议](../security.md)
