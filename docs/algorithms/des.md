# DES 数据加密标准

[返回 README](../../README.md) | [算法索引](README.md)

## 简介

DES（Data Encryption Standard）是早期的对称分组密码，密钥长度仅 56 位，已被暴力破解。
本 starter 集成 DES 仅用于兼容遗留系统。

## 密文格式与算法前缀

前缀为 `DESENC(...)`，编码规则见 [密文格式](../cipher-format.md)：

```
DESENC(base64,xxxxxxxx)
```

## 默认参数

| 项目     | 值                     |
|----------|------------------------|
| 变换串   | `DES/ECB/PKCS5Padding` |
| 密钥长度 | 8 字节（56 位有效）    |
| 分组长度 | 8 字节                 |
| IV       | 8 字节（CBC 等模式）   |

## 配置项

```properties
smcrypt.des.key=0123456789abcdef
smcrypt.des.mode=CBC
smcrypt.des.iv=0123456789abcdef
```

## 示例

ECB（默认）：

```properties
smcrypt.des.key=0123456789abcdef
```

CBC（需 8 字节 IV）：

```properties
smcrypt.des.mode=CBC
smcrypt.des.iv=0123456789abcdef
```

## 安全说明

- **已破解**：禁止用于新系统，仅用于兼容历史密文；
- 新系统请改用 AES 或 SM4。

## 相关文档

- [配置项参考](../configuration.md)
- [算法安全性与选型建议](../security.md)
