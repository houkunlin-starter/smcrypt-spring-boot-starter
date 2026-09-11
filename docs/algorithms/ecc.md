# ECC（ECIES）

[返回 README](../../README.md) | [算法索引](README.md)

## 简介

ECC 基于椭圆曲线集成加密方案（ECIES）实现非对称加密。本 starter 仅需提供 **私钥**，曲线由密钥本身决定。

## 密文格式与算法前缀

前缀为 `ECCENC(...)`，编码规则见 [密文格式](../cipher-format.md)：

```
ECCENC(base64,xxxxxxxx)
```

## 默认参数

| 项目   | 值                           |
|--------|------------------------------|
| 变换串 | `ECIES`                      |
| 密钥   | EC 私钥（PEM / Base64(DER)） |
| IV     | 不适用                       |

> ECC 仅使用 `transformation`（默认 `ECIES`）， **不要**配置 `mode` / `padding`，否则会拼出非法变换串。

## 配置项

```properties
smcrypt.ecc.key=-----BEGIN PRIVATE KEY-----...-----END PRIVATE KEY-----
smcrypt.ecc.transformation=ECIES
smcrypt.ecc.encoding=base64
```

## 示例

```properties
smcrypt.ecc.key=-----BEGIN PRIVATE KEY-----...-----END PRIVATE KEY-----
```

## 安全说明

- 曲线不低于 256 位；
- ECIES 算法自带 MAC，具备完整性校验能力，无需额外配置 `mac`。

## 相关文档

- [配置项参考](../configuration.md)
- [算法安全性与选型建议](../security.md)
