# AES 高级加密标准

[返回 README](../../README.md) | [算法索引](README.md)

## 简介

AES（Advanced Encryption Standard）是国际通用的对称分组密码，广泛用于各类系统，是国际生态下的首选对称算法。

## 密文格式与算法前缀

前缀为 `AESENC(...)`，编码规则见 [密文格式](../cipher-format.md)：

```
AESENC(base64,xxxxxxxx)
AESENC(hex,0123abcd)
```

## 默认参数

| 项目     | 值                        |
|----------|---------------------------|
| 变换串   | `AES/ECB/PKCS5Padding`    |
| 密钥长度 | 16 / 24 / 32 字节         |
| 分组长度 | 16 字节                   |
| IV       | 16 字节（CBC/GCM 等模式） |

## 配置项

```properties
smcrypt.aes.key=00112233445566778899aabbccddeeff
smcrypt.aes.transformation=AES/ECB/PKCS5Padding
smcrypt.aes.mode=CBC
smcrypt.aes.padding=PKCS5Padding
smcrypt.aes.iv=00112233445566778899aabbccddeeff
smcrypt.aes.encoding=base64
smcrypt.aes.mac=HmacSHA256
```

## 示例

ECB（默认）：

```properties
smcrypt.aes.key=00112233445566778899aabbccddeeff
```

CBC + 自定义 IV：

```properties
smcrypt.aes.mode=CBC
smcrypt.aes.padding=PKCS5Padding
# 16 字节 IV（32 位 hex）
smcrypt.aes.iv=00112233445566778899aabbccddeeff
```

GCM（AEAD，无需 padding）：

```properties
# GCM 必须使用 NoPadding，并显式提供 IV（推荐 12 字节）
smcrypt.aes.transformation=AES/GCM/NoPadding
smcrypt.aes.iv=00112233445566778899aabb
```

## 安全说明

- **推荐**；优先 GCM，避免 ECB；
- CBC 必须使用随机且不可复用的 IV；默认无完整性校验，可配置 `smcrypt.aes.mac` 启用 encrypt-then-MAC，或改用 GCM；
- 推荐密钥长度 256 位（32 字节）。

## 相关文档

- [配置项参考](../configuration.md)
- [算法安全性与选型建议](../security.md)
