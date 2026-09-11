# SM4 国密分组密码

[返回 README](../../README.md) | [算法索引](README.md)

## 简介

SM4 是中国国家密码管理局发布的分组密码标准（GB/T 32907），分组长度与密钥长度均为 128 位。
作为国密合规的对称算法，适用于对合规性有要求的场景。

## 密文格式与算法前缀

前缀为 `SM4ENC(...)`，编码规则见 [密文格式](../cipher-format.md)：

```
SM4ENC(base64,xxxxxxxx)
SM4ENC(hex,0123abcd)
```

## 默认参数

| 项目     | 值                        |
|----------|---------------------------|
| 变换串   | `SM4/ECB/PKCS5Padding`    |
| 密钥长度 | 16 字节（128 位）         |
| 分组长度 | 16 字节                   |
| IV       | 16 字节（CBC/GCM 等模式） |

## 配置项

```properties
smcrypt.sm4.key=0123456789abcdeffedcba9876543210
smcrypt.sm4.transformation=SM4/ECB/PKCS5Padding
smcrypt.sm4.mode=CBC
smcrypt.sm4.padding=PKCS5Padding
smcrypt.sm4.iv=0123456789abcdeffedcba9876543210
smcrypt.sm4.encoding=base64
smcrypt.sm4.mac=HmacSM3
```

## 示例

ECB（默认）：

```properties
smcrypt.sm4.key=0123456789abcdeffedcba9876543210
```

CBC（需 16 字节 IV）：

```properties
smcrypt.sm4.mode=CBC
smcrypt.sm4.iv=0123456789abcdeffedcba9876543210
```

启用完整性校验：

```properties
smcrypt.sm4.mac=HmacSM3
```

## 安全说明

- **推荐**（国密合规）；优先使用 CBC / GCM，避免 ECB；
- CBC 必须使用随机且不可复用的 IV；默认无完整性校验，可配置 `smcrypt.sm4.mac` 启用 encrypt-then-MAC，或改用 GCM。

## 相关文档

- [配置项参考](../configuration.md)
- [算法安全性与选型建议](../security.md)
