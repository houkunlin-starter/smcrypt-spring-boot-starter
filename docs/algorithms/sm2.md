# SM2 国密椭圆曲线公钥密码

[返回 README](../../README.md) | [算法索引](README.md)

## 简介

SM2 是中国国家密码管理局发布的椭圆曲线公钥密码标准（GB/T 32918），基于 `sm2p256v1` 曲线，
用于非对称加密。本 starter 仅需提供 **私钥**，解密使用私钥，加密时自动由私钥推导公钥。

## 密文格式与算法前缀

前缀为 `SM2ENC(...)`，编码规则见 [密文格式](../cipher-format.md)：

```
SM2ENC(base64,xxxxxxxx)
```

## 默认参数

| 项目     | 值                                   |
|----------|--------------------------------------|
| 变换串   | `SM2`（密文顺序 C1C3C2）             |
| 密钥     | EC 私钥（`sm2p256v1` 曲线）          |
| 密文顺序 | `C1C3C2`（默认）/ `C1C2C3`（可切换） |
| 曲线     | 由密钥本身决定，无需额外配置         |

> `transformation` / `padding` / `iv` 对 SM2 不生效；`mode` 仅用于选择密文顺序。

## 配置项

```properties
smcrypt.sm2.key=-----BEGIN PRIVATE KEY-----...-----END PRIVATE KEY-----
smcrypt.sm2.mode=C1C3C2
smcrypt.sm2.encoding=base64
```

## 示例

默认 C1C3C2：

```properties
smcrypt.sm2.key=-----BEGIN PRIVATE KEY-----...-----END PRIVATE KEY-----
```

对方系统使用 C1C2C3 时切换：

```properties
smcrypt.sm2.mode=C1C2C3
```

## 安全说明

- **推荐**（国密合规）；算法内部使用 SM3 摘要，无需单独配置；
- SM2 算法本身含 C3 校验，具备完整性校验能力，无需额外配置 `mac`。

## 相关文档

- [配置项参考](../configuration.md)
- [算法安全性与选型建议](../security.md)
