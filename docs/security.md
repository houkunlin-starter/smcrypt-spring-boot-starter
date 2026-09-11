# 算法安全性与选型建议

[返回 README](../README.md) | [文档索引](README.md)

各算法的安全状态与选型建议如下（新系统请优先选择「推荐」的算法）：

| 算法              | 密钥 / 安全强度             | 安全状态      | 选型建议                                                  |
|-------------------|-----------------------------|---------------|-----------------------------------------------------------|
| AES               | 128 / 192 / 256 位          | 安全          | **推荐**；优先 GCM，避免 ECB                              |
| SM4               | 128 位                      | 安全          | **推荐**（国密合规）；优先 CBC/GCM，避免 ECB              |
| SM2               | 256 位曲线（约 128 位）     | 安全          | **推荐**（国密合规）；算法内部使用 SM3 摘要，无需单独配置 |
| RSA               | 依密钥长度                  | 2048 位起安全 | 可用；推荐 3072 位 + OAEP                                 |
| ECC（ECIES）      | 256 位曲线（约 128 位）     | 安全          | 可用；曲线不低于 256 位                                   |
| SM9               | 256 位 BN 曲线（约 128 位） | 安全          | 仅标识密码（IBC）场景；依赖 KGC                           |
| ChaCha20-Poly1305 | 256 位                      | 安全          | **推荐**（AEAD，自带完整性）                              |
| GOST3412-2015     | 256 位                      | 安全          | 俄罗斯地区标准（Kuznyechik）                              |
| DSTU7624          | 128 / 256 / 512 位          | 安全          | 乌克兰地区标准（Kalyna）                                  |
| RC6               | 128 / 192 / 256 位          | 安全          | 可用但使用较少                                            |
| Camellia          | 128 / 192 / 256 位          | 安全          | 日本 / ISO 地区标准                                       |
| ARIA              | 128 / 192 / 256 位          | 安全          | 韩国地区标准                                              |
| SEED              | 128 位                      | 安全          | 韩国地区标准                                              |
| 3DES（DESEDE）    | 112 / 168 位                | **已过时**    | 仅兼容遗留系统；NIST 自 2024 年起禁用其加密               |
| DES               | 56 位                       | **已破解**    | 禁止用于新系统，仅兼容                                    |

## 推荐优先级

- 首选（新系统）：
    - 对称：`AES-256-GCM`（国际通用）或 `SM4-GCM` / `SM4-CBC`（国密合规）；
    - 非对称：`RSA-3072 + OAEP`（国际通用）或 `SM2`（国密合规）。
- 可接受（需正确使用）：
    - `AES-128/192/256-CBC`、`SM4-CBC`（必须使用随机且不可复用的 IV；默认无完整性校验，可配置
      `smcrypt.<算法>.mac` 启用 encrypt-then-MAC，或改用 GCM）；
    - `RSA-2048 + OAEP`、`ECC/ECIES`（P-256 及以上曲线）。
- 不推荐 / 仅兼容：
    - `DES`、`3DES(DESEDE)`：强度不足或已过时；
    - `RSA-1024`：已不安全；
    - `ECB` 模式：会泄露明文分组规律（结构化数据尤其危险）；
    - `RSA` PKCS#1 v1.5 填充：存在填充预言（Bleichenbacher）风险。

## 使用注意

- 本项目为兼容历史密文，对称算法 **默认使用 `ECB/PKCS5Padding`**；生产环境建议改用 CBC 或 GCM，并配置随机 IV：
  ```properties
  # AES-GCM（推荐，需随机 12 字节 IV）
  smcrypt.aes.transformation=AES/GCM/NoPadding
  smcrypt.aes.iv=<每次加密随机生成的 IV>
  ```
- `IV` 必须随机、每次加密不同且不得复用（GCM 复用 IV 会导致密钥流泄露）；
- CBC + PKCS5/PKCS7 存在填充预言攻击面，建议改用 AEAD（GCM）；
- **完整性校验说明**：GCM 等 AEAD 模式自带认证标签（JCE 解密时会校验，密文被篡改会抛 `AEADBadTagException`）；SM2 / SM9 /
  ECIES 算法本身含 C3 或 MAC 校验；CBC / ECB 等非 AEAD 模式可配置 `smcrypt.<算法>.mac=HmacSM3|HmacSHA256` 启用
  encrypt-then-MAC（密文载荷 `密文 || MAC`，解密前校验）， **未启用时本 starter 不提供额外完整性校验**；
- RSA 加密请使用 OAEP（`RSA/ECB/OAEPWithSHA-256AndMGF1Padding`），避免 PKCS#1 v1.5；
- 密钥长度下限：AES / SM4 至少 128 位，RSA 至少 2048 位（推荐 3072 位），ECC / SM2 至少 256 位。

> 合规提示：需满足国密合规时优先使用 `SM2` / `SM4` / `SM9`；面向国际或通用生态时使用 `AES` / `RSA` / `ECC`。

## 生产环境加固清单

部署前建议逐项检查：

- **启用 fail-fast**：设置 `smcrypt.fail-fast=true`，让密钥缺失或解密失败在启动阶段直接暴露，而不是带着未解密的密文继续启动；
- **启用严格体检**：设置 `smcrypt.strict=warn`（灰度观察）或 `smcrypt.strict=fail`（直接阻断），在启动阶段检查并拦截
  ECB 模式、DES / 3DES、RSA PKCS#1 v1.5 与密钥长度不足、Jasypt 旧算法、PBE 固定盐 / 弱派生参数等弱配置；
- **选择安全算法与模式**：对称优先 `AES-256-GCM` / `SM4-GCM`；非对称优先 `RSA-3072 + OAEP` / `SM2`；避免 ECB、DES / 3DES、RSA
  PKCS#1 v1.5；
- **配置随机且不复用的 IV / nonce**：GCM 复用 IV 会导致密钥流泄露；
- **启用完整性校验**：非 AEAD 模式配置 `smcrypt.<算法>.mac=HmacSM3|HmacSHA256` 启用 encrypt-then-MAC，并单独配置
  `smcrypt.<算法>.mac-key`，避免与加密密钥复用；
- **显式声明密钥编码**：密钥为含空白口令或全十六进制字符时，配置 `smcrypt.<算法>.key-encoding=plain|hex`，消除自动识别歧义；
- **密钥来源安全**：密钥 / 口令通过环境变量、JVM 参数或密钥文件提供，避免硬编码；CLI 使用 `--key -` / `--password -`
  从标准输入读取，避免进入命令历史；
- **避免隐式默认密钥文件**：`SecretKeyResolver` 会在所有显式来源均未命中时，从 **当前工作目录**按固定名
  `smcrypt-<算法>.key` / `smcrypt-<算法>.properties` 隐式加载密钥。请显式配置密钥来源（属性 / 环境变量 / `-D` / 命令行 /
  `smcrypt.<算法>.file`），并确保工作目录下不存在上述同名文件，避免加载到非预期密钥；
- **限制密钥文件权限**：确保 `smcrypt-<算法>.key` / `.properties` 仅对必要用户可读；
- **定期轮换密钥**：密钥泄露后应尽快更换并重新加密密文。

> 各配置项含义见 [配置项参考](configuration.md)。

## 口令派生（PBE）安全建议

- 优先使用 `PBEENC` + `mode=KDF` + `AES/GCM/NoPadding` + `Argon2id`（或 PBKDF2 ≥ 600000 次）；
- 盐保持随机内嵌，勿固定；
- 口令通过环境变量 / JVM 参数提供，勿写入配置文件；
- `mode=JCE` 与 `JASYPTENC` 主要用于互操作 / 兼容。

## 相关文档

- [算法索引](algorithms/README.md)
- [配置项参考](configuration.md)
- [PBE](algorithms/pbe.md) / [Jasypt](algorithms/jasypt.md)
