# SEED

[返回 README](../../README.md) | [算法索引](README.md)

## 简介

SEED 是韩国地区标准的对称分组密码，分组长度与密钥长度均为 128 位。

## 密文格式与算法前缀

前缀为 `SEEDENC(...)`，编码规则见 [密文格式](../cipher-format.md)：

```
SEEDENC(base64,xxxxxxxx)
```

## 默认参数

| 项目     | 值                      |
|----------|-------------------------|
| 变换串   | `SEED/ECB/PKCS5Padding` |
| 密钥长度 | 16 字节（128 位）       |
| 分组长度 | 16 字节                 |
| IV       | 16 字节（CBC 等模式）   |

## 配置项

```properties
smcrypt.seed.key=<16 字节密钥的 hex / Base64>
smcrypt.seed.mode=CBC
smcrypt.seed.iv=<16 字节 IV>
```

## 示例

```properties
smcrypt.seed.key=<16 字节密钥的 hex / Base64>
smcrypt.seed.mode=CBC
smcrypt.seed.iv=<16 字节 IV>
```

## 安全说明

- 韩国地区标准，安全；
- CBC 必须使用随机且不可复用的 IV；可配置 `smcrypt.seed.mac` 启用 encrypt-then-MAC。

## 相关文档

- [配置项参考](../configuration.md)
- [算法安全性与选型建议](../security.md)
