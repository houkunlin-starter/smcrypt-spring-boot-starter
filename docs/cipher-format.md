# 密文格式

[返回 README](../README.md) | [文档索引](README.md)

## 包裹结构

```
{算法}ENC(密文)
{算法}ENC(hex,密文)
{算法}ENC(base64,密文)
```

- `{算法}` 为算法名称，大小写不敏感：`SM4ENC(...)`、`sm4enc(...)` 均可；
- 括号内可选的编码前缀支持 `hex`、`base64`、`b64`（大小写不敏感）；
- 编码前缀与密文之间用英文逗号分隔。

## 编码自动识别

未显式声明编码时，按以下规则自动识别：

1. 内容全部为十六进制字符 **且长度为 2 的倍数** → 按 hex 解码；
2. 其余情况 → 按 Base64 解码。

> 提示：某些字符串（例如 `ABCD`）既是合法的十六进制又是合法的 Base64，存在歧义。
> 为保证确定性， **推荐使用加密工具生成密文时带上编码前缀**（本项目的加密工具默认就会输出
> `{算法}ENC(base64,...)` 或 `{算法}ENC(hex,...)`）。

## 算法前缀

| 算法     | 前缀               | 文档                                  |
|----------|--------------------|---------------------------------------|
| SM4      | `SM4ENC(...)`      | [sm4.md](algorithms/sm4.md)           |
| SM2      | `SM2ENC(...)`      | [sm2.md](algorithms/sm2.md)           |
| SM9      | `SM9ENC(...)`      | [sm9.md](algorithms/sm9.md)           |
| AES      | `AESENC(...)`      | [aes.md](algorithms/aes.md)           |
| DES      | `DESENC(...)`      | [des.md](algorithms/des.md)           |
| DESEDE   | `DESEDEENC(...)`   | [desede.md](algorithms/desede.md)     |
| CHACHA20 | `CHACHA20ENC(...)` | [chacha20.md](algorithms/chacha20.md) |
| GOST3412 | `GOST3412ENC(...)` | [gost3412.md](algorithms/gost3412.md) |
| DSTU7624 | `DSTU7624ENC(...)` | [dstu7624.md](algorithms/dstu7624.md) |
| RC6      | `RC6ENC(...)`      | [rc6.md](algorithms/rc6.md)           |
| CAMELLIA | `CAMELLIAENC(...)` | [camellia.md](algorithms/camellia.md) |
| ARIA     | `ARIAENC(...)`     | [aria.md](algorithms/aria.md)         |
| SEED     | `SEEDENC(...)`     | [seed.md](algorithms/seed.md)         |
| RSA      | `RSAENC(...)`      | [rsa.md](algorithms/rsa.md)           |
| ECC      | `ECCENC(...)`      | [ecc.md](algorithms/ecc.md)           |
| PBE      | `PBEENC(...)`      | [pbe.md](algorithms/pbe.md)           |
| JASYPT   | `JASYPTENC(...)`   | [jasypt.md](algorithms/jasypt.md)     |

> SM9 为标识加密（IBC），其配置方式与其它算法不同，见 [SM9 标识加密](algorithms/sm9.md)。
> 口令派生与 Jasypt 兼容的前缀为 `PBEENC(...)` / `JASYPTENC(...)`，见
> [PBE](algorithms/pbe.md) 与 [Jasypt](algorithms/jasypt.md)。

## 相关文档

- [算法索引](algorithms/README.md)
- [配置项参考](configuration.md)
