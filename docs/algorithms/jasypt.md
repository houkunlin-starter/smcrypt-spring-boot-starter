# Jasypt 兼容

[返回 README](../../README.md) | [算法索引](README.md)

`JASYPTENC(...)` 用于解密 Jasypt 生成的密文，配置前缀为 `smcrypt.jasypt.*`。

统一载荷：`base64(salt ‖ iv ‖ cipher)`。

## 迁移方式

Jasypt 的密文为 `ENC(base64(salt ‖ iv ‖ cipher))`。迁移到本项目只需把前缀 `ENC(` 改为 `JASYPTENC(`，内层与口令保持不变即可解密：

```properties
# Jasypt 原密文：secret=ENC(nrmZtkF7T0kjG/VodDvBw93Ct8EgjCA+)
# 迁移后：
secret=JASYPTENC(nrmZtkF7T0kjG/VodDvBw93Ct8EgjCA+)
smcrypt.jasypt.password=my-jasypt-password
```

## 配置项

| 配置项                                  | 说明                    | 默认                          |
|-----------------------------------------|-------------------------|-------------------------------|
| `smcrypt.jasypt.password`               | Jasypt 口令             | 无                            |
| `smcrypt.jasypt.transformation`         | Jasypt 算法             | `PBEWITHHMACSHA512ANDAES_256` |
| `smcrypt.jasypt.iterations`             | 迭代次数                | `1000`                        |
| `smcrypt.jasypt.salt-size` / `.iv-size` | 盐 / IV 长度（0=无 IV） | `16` / `16`                   |
| `smcrypt.jasypt.provider`               | Provider 名             | JVM 默认（SunJCE）            |
| `smcrypt.jasypt.encoding`               | 内层编码                | `base64`                      |

## 安全建议

- Jasypt 旧默认算法 `PBEWithMD5AndDES` 不安全；若既有密文使用它，建议尽快重加密为
  `PBEWITHHMACSHA512ANDAES_256` 或本项目的 `PBEENC` + AES-GCM；
- 口令通过环境变量 / JVM 参数提供，勿写入配置文件；
- `JASYPTENC` 主要用于兼容既有 Jasypt 密文。

## 相关文档

- [PBE 口令派生](pbe.md)
- [算法安全性与选型建议](../security.md)
