# DESEDE 三重 DES（3DES）

[返回 README](../../README.md) | [算法索引](README.md)

## 简介

DESEDE（Triple DES / 3DES）是对 DES 的三次迭代，用于弥补 DES 密钥过短的问题。
NIST 自 2024 年起已禁用其加密用途，本 starter 集成 3DES 仅用于兼容遗留系统。

## 密文格式与算法前缀

前缀为 `DESEDEENC(...)`，编码规则见 [密文格式](../cipher-format.md)：

```
DESEDEENC(base64,xxxxxxxx)
```

## 默认参数

| 项目     | 值                                         |
|----------|--------------------------------------------|
| 变换串   | `DESede/ECB/PKCS5Padding`                  |
| 密钥长度 | 16 字节（2-key，K1=K3）或 24 字节（3-key） |
| 分组长度 | 8 字节                                     |
| IV       | 8 字节（CBC 等模式）                       |

## 配置项

```properties
smcrypt.desede.key=0123456789abcdeffedcba9876543210
smcrypt.desede.mode=CBC
smcrypt.desede.iv=0123456789abcdef
```

## 示例

16 字节 2-key（K1=K3）或 24 字节 3-key 均可，由密钥长度决定：

```properties
smcrypt.desede.key=0123456789abcdeffedcba9876543210
# 如需 CBC，需配置 8 字节 IV（16 位 hex）
smcrypt.desede.mode=CBC
smcrypt.desede.iv=0123456789abcdef
```

## 安全说明

- **已过时**：仅建议用于兼容遗留系统密文；NIST 自 2024 年起禁用其加密；
- 新系统请使用 AES 或 SM4。

## 相关文档

- [配置项参考](../configuration.md)
- [算法安全性与选型建议](../security.md)
