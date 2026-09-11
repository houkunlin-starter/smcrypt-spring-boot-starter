# RC6

[返回 README](../../README.md) | [算法索引](README.md)

## 简介

RC6 是 AES 竞赛的候选算法之一，支持 128 / 192 / 256 位密钥。安全性良好但实际使用较少。

## 密文格式与算法前缀

前缀为 `RC6ENC(...)`，编码规则见 [密文格式](../cipher-format.md)：

```
RC6ENC(base64,xxxxxxxx)
```

## 默认参数

| 项目     | 值                     |
|----------|------------------------|
| 变换串   | `RC6/ECB/PKCS5Padding` |
| 密钥长度 | 16 / 24 / 32 字节      |
| 分组长度 | 16 字节                |
| IV       | 16 字节（CBC 等模式）  |

## 配置项

```properties
smcrypt.rc6.key=<密钥的 hex / Base64>
smcrypt.rc6.mode=CBC
smcrypt.rc6.iv=<16 字节 IV>
```

## 示例

```properties
smcrypt.rc6.key=<密钥的 hex / Base64>
smcrypt.rc6.mode=CBC
smcrypt.rc6.iv=<16 字节 IV>
```

## 安全说明

- 安全但使用较少；若需通用性建议改用 AES；
- CBC 必须使用随机且不可复用的 IV；可配置 `smcrypt.rc6.mac` 启用 encrypt-then-MAC。

## 相关文档

- [配置项参考](../configuration.md)
- [算法安全性与选型建议](../security.md)
