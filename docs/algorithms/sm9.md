# SM9 标识加密

[返回 README](../../README.md) | [算法索引](README.md)

SM9 是国密 **标识密码（IBC，Identity-Based Cryptography） **算法（GB/T 38635、GM/T 0044），基于 BN 曲线
上的双线性对。其特点是**公钥即身份**（如邮箱、工号），用户私钥由 KGC（密钥生成中心）用主私钥按身份派生，
无需数字证书。本 starter 仅集成其中的 **公钥加密 / KEM** 能力，用于解密配置密文。

> 需要 BouncyCastle **1.86 及以上**版本（本 starter 已使用 1.86）。
> SM9 属小众且强依赖 KGC 的算法，新系统建议优先使用 SM2 或 SM4。

## 算法与密文格式

- 密文前缀：`SM9ENC(...)`，编码规则与其它算法一致（可显式 `hex,` / `base64,`，缺省自动识别）；
- 数据封装方式（`smcrypt.sm9.mode`）：
    - `SM4`（默认）：SM4-ECB / PKCS#7 封装；
    - `STREAM`：KDF 流（XOR）封装；
- 密文结构（`smcrypt.sm9.cipher-format`）：
    - `raw`（默认）：引擎原生 `C1(64) || C3(32) || C2`；
    - `asn1`：GM/T 0080-2020 的 `SM9Cipher` ASN.1 结构（`enType`：0=STREAM，1=SM4-ECB）。

> 密文格式与封装方式必须与生成密文的一方（GmSSL、铜锁、加密机等）保持一致，否则无法解密。

## 密钥材料

SM9 的密钥材料包含四项，均需提供：

| 材料            | 说明                                | 长度                          |
|-----------------|-------------------------------------|-------------------------------|
| 用户私钥 `de`   | 由 KGC 按身份派生，用于解密         | G2 点，129 字节（`0x04‖x‖y`） |
| 主公钥 `Ppub-e` | KGC 的主公钥，用于加密与重建私钥    | G1 点，65 字节（`0x04‖x‖y`）  |
| 身份 `identity` | 用户标识字符串（UTF-8）             | 任意                          |
| `hid`           | 私钥生成函数标识，KEM / 加密为 `03` | 1 字节                        |

- `de` 与 `Ppub-e` 以 hex 或 Base64 提供；`de` 的编码 **不含**主公钥 / 身份 / hid，因此后三者必须另行配置；
- 仅需解密时，`de` + `Ppub-e` + `identity` 缺一不可（身份参与 KDF 输入，主公钥用于重建私钥；`hid` 默认 `03`）。

## 配置项

SM9 的配置项独立于其它算法，统一以 `smcrypt.sm9.` 为前缀：

| 配置项                          | 说明                                 | 默认值   |
|---------------------------------|--------------------------------------|----------|
| `smcrypt.sm9.private-key`       | 用户私钥 `de`（hex / Base64）        | 无       |
| `smcrypt.sm9.master-public-key` | 主公钥 `Ppub-e`（hex / Base64）      | 无       |
| `smcrypt.sm9.identity`          | 身份字符串（UTF-8）                  | 无       |
| `smcrypt.sm9.hid`               | 私钥生成函数标识（hex，KEM 用 `03`） | `03`     |
| `smcrypt.sm9.mode`              | 数据封装方式：`SM4` / `STREAM`       | `SM4`    |
| `smcrypt.sm9.cipher-format`     | 密文格式：`raw` / `asn1`             | `raw`    |
| `smcrypt.sm9.encoding`          | 加密输出编码：`hex` / `base64`       | `base64` |

> SM9 不使用 `transformation` / `padding` / `iv` 等参数。

## 配置示例

```properties
# 用户私钥 de（G2 点，Base64）
smcrypt.sm9.private-key=BASE64_OF_DE
# 主公钥 Ppub-e（G1 点，Base64）
smcrypt.sm9.master-public-key=BASE64_OF_PPUBE
# 身份
smcrypt.sm9.identity=alice@example.com
# 私钥生成函数标识（KEM 默认 03，可省略）
smcrypt.sm9.hid=03
# 数据封装方式：SM4（默认）或 STREAM
smcrypt.sm9.mode=SM4
# 密文格式：raw（默认）或 asn1
smcrypt.sm9.cipher-format=raw
```

```yaml
smcrypt:
  sm9:
    private-key: BASE64_OF_DE
    master-public-key: BASE64_OF_PPUBE
    identity: alice@example.com
    hid: "03"
    mode: SM4
    cipher-format: raw
```

配置完成后，业务代码照常读取被加密的配置项即可（解密在启动早期自动完成）：

```properties
spring.datasource.password=SM9ENC(base64,xxxxxxxx)
```

## 生成密文（API）

命令行工具未提供 SM9 专用参数，可通过 `SmCryptEncryptor` API 或系统属性使用：

```java
import com.houkunlin.smcrypt.SmCryptEncryptor;

public class Main {
    public void main(String[] args) {
        // 加密只需主公钥与身份，无需用户私钥
        System.setProperty("smcrypt.sm9.master-public-key", "BASE64_OF_PPUBE");
        System.setProperty("smcrypt.sm9.identity", "alice@example.com");

        SmCryptEncryptor encryptor = new SmCryptEncryptor(new SmCryptContext(System::getProperty, new FileSystemResourceLoader()));
        String cipher = encryptor.encrypt("SM9", "my-secret");
        // 输出形如：SM9ENC(base64,xxxx)
    }
}
```

## 限制

- 需要 BouncyCastle 1.86+；
- 仅支持数据封装类型 `STREAM` 与 `SM4-ECB`（GM/T 0080 的 SM4-CBC / OFB / CFB 未实现）；
- 用户私钥无标准 Java / DER / PEM 编码，须按 G2 点裸编码（129 字节）提供；
- 密钥交换与数字签名不在本 starter 范围内。

## 相关文档

- [加密工具](../tools.md)
- [算法安全性与选型建议](../security.md)
