# ChaCha20-Poly1305 AEAD

[返回 README](../../README.md) | [算法索引](README.md)

## 简介

ChaCha20-Poly1305 是基于 ChaCha20 流密码与 Poly1305 认证码的 AEAD（带关联数据的认证加密）算法，
自带完整性校验，适合移动端与软件实现场景。

## 密文格式与算法前缀

前缀为 `CHACHA20ENC(...)`，编码规则见 [密文格式](../cipher-format.md)：

```
CHACHA20ENC(base64,xxxxxxxx)
```

## 默认参数

| 项目      | 值                  |
|-----------|---------------------|
| 变换串    | `ChaCha20-Poly1305` |
| 密钥长度  | 32 字节（256 位）   |
| nonce     | 12 字节             |
| 模式/填充 | 不支持（AEAD 固定） |

## 配置项

```properties
smcrypt.chacha20.key=00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff
smcrypt.chacha20.iv=00112233445566778899aabb
smcrypt.chacha20.encoding=base64
```

> ChaCha20-Poly1305 为 AEAD，不支持 `mode` / `padding`，也不支持 `mac`。

## 示例

```properties
# 32 字节密钥；AEAD 需 12 字节 nonce（每次加密随机且不复用）
smcrypt.chacha20.key=00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff
smcrypt.chacha20.iv=00112233445566778899aabb
```

## 安全说明

- **推荐**（AEAD，自带完整性）；
- nonce 必须随机、每次加密不同且不得复用；
- 自带认证标签，无需再配置 `mac`。

## 相关文档

- [配置项参考](../configuration.md)
- [算法安全性与选型建议](../security.md)
