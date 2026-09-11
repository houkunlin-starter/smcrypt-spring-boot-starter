# 算法索引

[返回 README](../../README.md) | [文档索引](../README.md)

本 starter 内置以下算法处理器（基于 BouncyCastle），密文前缀统一为 `{算法}ENC(...)`。

## 对称算法

| 算法     | 前缀          | 默认变换                         | 密钥要求          | 说明                              | 文档                       |
|----------|---------------|----------------------------------|-------------------|-----------------------------------|----------------------------|
| SM4      | `SM4ENC`      | `SM4/ECB/PKCS5Padding`           | 16 字节对称密钥   | 国密分组密码                      | [sm4.md](sm4.md)           |
| AES      | `AESENC`      | `AES/ECB/PKCS5Padding`           | 16 / 24 / 32 字节 | 国际通用对称加密                  | [aes.md](aes.md)           |
| DES      | `DESENC`      | `DES/ECB/PKCS5Padding`           | 8 字节            | 兼容遗留系统                      | [des.md](des.md)           |
| DESEDE   | `DESEDEENC`   | `DESede/ECB/PKCS5Padding`        | 16 / 24 字节      | 3DES（Triple DES），兼容遗留系统  | [desede.md](desede.md)     |
| CHACHA20 | `CHACHA20ENC` | `ChaCha20-Poly1305`              | 32 字节           | AEAD（自带完整性），nonce 12 字节 | [chacha20.md](chacha20.md) |
| GOST3412 | `GOST3412ENC` | `GOST3412-2015/ECB/PKCS5Padding` | 32 字节           | 俄罗斯标准（Kuznyechik）          | [gost3412.md](gost3412.md) |
| DSTU7624 | `DSTU7624ENC` | `DSTU7624/ECB/PKCS5Padding`      | 16 / 32 / 64 字节 | 乌克兰标准（Kalyna）              | [dstu7624.md](dstu7624.md) |
| RC6      | `RC6ENC`      | `RC6/ECB/PKCS5Padding`           | 16 / 24 / 32 字节 | AES 候选算法，使用较少            | [rc6.md](rc6.md)           |
| CAMELLIA | `CAMELLIAENC` | `Camellia/ECB/PKCS5Padding`      | 16 / 24 / 32 字节 | 日本 / ISO 地区标准               | [camellia.md](camellia.md) |
| ARIA     | `ARIAENC`     | `ARIA/ECB/PKCS5Padding`          | 16 / 24 / 32 字节 | 韩国地区标准                      | [aria.md](aria.md)         |
| SEED     | `SEEDENC`     | `SEED/ECB/PKCS5Padding`          | 16 字节           | 韩国地区标准                      | [seed.md](seed.md)         |

## 非对称算法

| 算法 | 前缀     | 默认变换               | 密钥要求             | 说明                         | 文档             |
|------|----------|------------------------|----------------------|------------------------------|------------------|
| SM2  | `SM2ENC` | `SM2`（C1C3C2）        | EC 私钥（sm2p256v1） | 国密非对称，默认 C1C3C2 顺序 | [sm2.md](sm2.md) |
| RSA  | `RSAENC` | `RSA/ECB/PKCS1Padding` | RSA 私钥             | 非对称加密                   | [rsa.md](rsa.md) |
| ECC  | `ECCENC` | `ECIES`                | EC 私钥              | 基于 ECIES 的椭圆曲线加密    | [ecc.md](ecc.md) |

## 标识加密与口令派生

| 算法   | 前缀             | 说明                                        | 文档                   |
|--------|------------------|---------------------------------------------|------------------------|
| SM9    | `SM9ENC(...)`    | 标识加密（IBC），配置独立为 `smcrypt.sm9.*` | [sm9.md](sm9.md)       |
| PBE    | `PBEENC(...)`    | 口令派生（KDF + 对称算法）或 JCE PBE 变换   | [pbe.md](pbe.md)       |
| JASYPT | `JASYPTENC(...)` | 解密 Jasypt 生成的密文                      | [jasypt.md](jasypt.md) |

## 通用说明

- 对称算法通过 JCE `Cipher` 实现，支持通过 `transformation` / `mode` / `padding` / `iv` 自定义；
- 3DES（`DESEDE`）密钥支持 16 字节（2-key，K1=K3）或 24 字节（3-key），属过时算法，仅建议用于兼容遗留系统；
- 非对称算法（RSA / SM2 / ECC）仅需提供 **私钥**：解密使用私钥，加密时自动由私钥推导公钥（SM9 不同，见 [sm9.md](sm9.md)）；
- SM2 密文顺序可通过 `smcrypt.sm2.mode=C1C2C3` 切换；
- SM2 / ECC 的椭圆曲线由密钥本身决定，无需额外配置；
- 内置算法均由 BouncyCastle 提供，启动时以显式 Provider 方式使用，不污染全局 JCE Provider。

## 相关文档

- [配置项参考](../configuration.md)
- [算法安全性与选型建议](../security.md)
- [密文格式](../cipher-format.md)
