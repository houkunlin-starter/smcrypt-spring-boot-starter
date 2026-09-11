# 加密工具

[返回 README](../README.md) | [文档索引](README.md)

## API 方式

```java
import com.houkunlin.smcrypt.SmCryptContext;
import com.houkunlin.smcrypt.SmCryptEncryptor;
import org.springframework.core.io.FileSystemResourceLoader;

public class Main {
    public void main(String[] args) {
        // 通过系统属性提供密钥
        System.setProperty("smcrypt.sm4.key", "0123456789abcdeffedcba9876543210");

        SmCryptEncryptor encryptor = new SmCryptEncryptor(new SmCryptContext(System::getProperty, new FileSystemResourceLoader()));

        // 加密：返回 SM4ENC(base64,xxxx)
        String cipher = encryptor.encrypt("SM4", "hello");

        // 解密：自动识别算法与编码
        String plain = encryptor.decrypt(cipher);
    }
}
```

## 生成密钥

API 方式（`com.houkunlin.smcrypt.key.SmCryptKeyGenerator`）：

```java
// 对称密钥（返回 hex 字符串）
String aesKey = SmCryptKeyGenerator.generateSymmetricKey("AES", 256);

// 非对称密钥对（RSA / ECC / SM2）
KeyPair keyPair = SmCryptKeyGenerator.generateKeyPair("RSA", 3072);
String privateKeyPem = SmCryptKeyGenerator.toPrivateKeyPem(keyPair.getPrivate());
String privateKeyBase64 = SmCryptKeyGenerator.toPrivateKeyBase64(keyPair.getPrivate());

// SM9：生成 KGC 主密钥对与指定身份的用户私钥
Sm9KeyMaterial material = SmCryptKeyGenerator.generateSm9Key(
        "alice@example.com".getBytes(StandardCharsets.UTF_8), (byte) 0x03);
```

命令行方式：

```bash
# 生成 AES-256 密钥（输出 hex）
java -cp app.jar com.houkunlin.smcrypt.SmCryptCli --generate-key --algorithm AES --key-length 256

# 生成 RSA-3072 私钥（输出 PEM）
java -cp app.jar com.houkunlin.smcrypt.SmCryptCli --generate-key --algorithm RSA --key-length 3072

# 生成 SM9 密钥（输出 smcrypt.sm9.* 配置项）
java -cp app.jar com.houkunlin.smcrypt.SmCryptCli --generate-key --algorithm SM9 --identity alice@example.com
```

`--key-length` 支持的密钥长度（单位：位；未指定时使用默认值）：

| 算法     | 可用长度                               | 默认 |
|----------|----------------------------------------|------|
| SM4      | 128                                    | 128  |
| AES      | 128 / 192 / 256                        | 256  |
| DES      | 56 / 64                                | 56   |
| DESEDE   | 112 / 128（2-key）、168 / 192（3-key） | 168  |
| CHACHA20 | 256（固定）                            | 256  |
| GOST3412 | 256（固定）                            | 256  |
| DSTU7624 | 128 / 256 / 512                        | 256  |
| RC6      | 128 / 192 / 256                        | 256  |
| CAMELLIA | 128 / 192 / 256                        | 256  |
| ARIA     | 128 / 192 / 256                        | 256  |
| SEED     | 128（固定）                            | 128  |
| RSA      | 2048 / 3072 / 4096 等                  | 2048 |
| ECC      | 256 / 384 / 521                        | 256  |
| SM2      | 256（固定）                            | 256  |

> 本工具只负责 **生成密钥**；密钥长度由算法与 `--key-length` 决定，与「加密生成密文」是两个独立步骤。
> 使用对称密钥加密时，AES/DES 的密钥长度即由所提供密钥的字节数决定（见
> [算法安全性与选型建议](security.md)）。

## 命令行方式

```bash
java -cp app.jar com.houkunlin.smcrypt.SmCryptCli \
    --algorithm SM4 --key 0123456789abcdeffedcba9876543210 --text hello
```

参数说明：

| 参数               | 简写 | 说明                                                                                                                                                                        |
|--------------------|------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `--algorithm`      | `-a` | 算法名称：`SM4` / `SM2` / `SM9` / `AES` / `DES` / `DESEDE` / `CHACHA20` / `GOST3412` / `DSTU7624` / `RC6` / `CAMELLIA` / `ARIA` / `SEED` / `RSA` / `ECC` / `PBE` / `JASYPT` |
| `--text`           | `-t` | 待加密明文；配合 `--decrypt` 时表示待解密密文                                                                                                                               |
| `--key`            | `-k` | 密钥内容（hex / Base64 / PEM）；值为 `-` 时从标准输入读取；也可用 `--file` 或环境变量 `SMCRYPT_<算法>_KEY`                                                                  |
| `--file`           | `-f` | 密钥文件路径（`file:` / `classpath:`）                                                                                                                                      |
| `--encoding`       | `-e` | 加密输出编码：`hex` / `base64`（默认 `base64`）                                                                                                                             |
| `--transformation` |      | 自定义 JCE 变换串，如 `AES/GCM/NoPadding`                                                                                                                                   |
| `--mode`           |      | 加密模式，如 `CBC`、`GCM`、`C1C3C2`                                                                                                                                         |
| `--padding`        |      | 填充方式，默认 `PKCS5Padding`                                                                                                                                               |
| `--iv`             |      | 初始向量（hex / Base64）                                                                                                                                                    |
| `--password`       |      | 口令派生 / Jasypt 兼容的口令；值为 `-` 时从标准输入读取                                                                                                                     |
| `--kdf`            |      | KDF 算法：`PBKDF2` / `SCRYPT` / `ARGON2`（PBE，默认 `PBKDF2`）                                                                                                              |
| `--kdf-iterations` |      | KDF 迭代次数（PBE）                                                                                                                                                         |
| `--salt`           |      | KDF 固定盐（hex / Base64，PBE；不配则随机内嵌）                                                                                                                             |
| `--pbe-mode`       |      | PBE 模式：`KDF`（默认）/ `JCE`                                                                                                                                              |
| `--iterations`     |      | JCE PBE 迭代次数（默认 `1000`）                                                                                                                                             |
| `--decrypt`        |      | 解密模式                                                                                                                                                                    |
| `--generate-key`   |      | 生成密钥（配合 `--algorithm`；对称输出 hex，RSA/ECC/SM2 输出私钥 PEM）                                                                                                      |
| `--key-length`     |      | 生成密钥的长度（位），见「生成密钥」                                                                                                                                        |
| `--identity`       |      | SM9 生成密钥时的身份                                                                                                                                                        |
| `--help`           | `-h` | 显示帮助                                                                                                                                                                    |

解密示例：

```bash
java -cp app.jar com.houkunlin.smcrypt.SmCryptCli \
    --algorithm SM4 --key 0123456789abcdeffedcba9876543210 \
    --decrypt --text "SM4ENC(base64,xxxxxxxx)"
```

> 安全提示：`--key` / `--password` 的值会出现在进程列表与命令历史中。敏感场景建议使用
> `--key -` / `--password -` 从标准输入读取，或改用 `--file` 与 `SMCRYPT_<算法>_KEY` 环境变量。

## 相关文档

- [快速开始](getting-started.md)
- [SM9 标识加密](algorithms/sm9.md)
- [PBE](algorithms/pbe.md) / [Jasypt](algorithms/jasypt.md)
