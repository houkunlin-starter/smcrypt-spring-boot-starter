# ARIA

[返回 README](../../README.md) | [算法索引](README.md)

## 简介

ARIA 是韩国地区标准的对称分组密码，由韩国信息安全局等机构设计，安全性等同于 AES。

## 密文格式与算法前缀

前缀为 `ARIAENC(...)`，编码规则见 [密文格式](../cipher-format.md)：

```
ARIAENC(base64,xxxxxxxx)
```

## 默认参数

| 项目     | 值                      |
|----------|-------------------------|
| 变换串   | `ARIA/ECB/PKCS5Padding` |
| 密钥长度 | 16 / 24 / 32 字节       |
| 分组长度 | 16 字节                 |
| IV       | 16 字节（CBC 等模式）   |

## 配置项

```properties
smcrypt.aria.key=<密钥的 hex / Base64>
smcrypt.aria.mode=CBC
smcrypt.aria.iv=<16 字节 IV>
```

## 示例

```properties
smcrypt.aria.key=<密钥的 hex / Base64>
smcrypt.aria.mode=CBC
smcrypt.aria.iv=<16 字节 IV>
```

## 安全说明

- 韩国地区标准，安全；
- CBC 必须使用随机且不可复用的 IV；可配置 `smcrypt.aria.mac` 启用 encrypt-then-MAC。

## 相关文档

- [配置项参考](../configuration.md)
- [算法安全性与选型建议](../security.md)
