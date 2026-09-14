# 注意事项与常见问题

[返回 README](../README.md) | [文档索引](README.md)

**1. 密钥本身不能是密文。** 密钥在解密阶段被读取，必须为明文（或来自加密机等外部通道）。

**2. 找不到密钥时会发生什么？** 若配置中存在可识别的密文但未找到对应密钥，会记录日志并跳过解密，
密文将保持原样，相关配置可能因值不正确而导致启动或运行失败。设置 `smcrypt.fail-fast=true` 可让此情况直接终止启动。

**3. 解密失败会影响启动吗？** 默认情况下单个属性解密失败会记录 ERROR 日志并跳过该属性，不影响其他属性与其他流程；
设置 `smcrypt.fail-fast=true` 后，任一属性解密失败（含未找到密钥）都会抛出异常并中断启动。

**4. 默认模式为什么是 ECB？** 为兼容历史密文，对称算法默认使用 ECB。 **生产环境建议使用 CBC/GCM 等更安全的
模式并配置 IV**，例如：

```properties
smcrypt.aes.transformation=AES/GCM/NoPadding
smcrypt.aes.iv=00112233445566778899aabbccddeeff
```

**5. 加密与解密的参数必须一致。** 模式、填充、IV、编码（生成密文时）需与解密端一致，否则无法还原明文。

**6. 多算法可共存。** 不同属性可使用不同算法前缀，各自使用对应的 `smcrypt.<算法>.*` 配置。

**7. 编码歧义。** 对无编码前缀且内容恰好同时满足 hex 与 Base64 的密文，建议显式补充编码前缀。

**8. BouncyCastle 版本与坐标？** 项目按 JDK 选择坐标：Boot 2 用 `bcprov-jdk15to18` / `bcpkix-jdk15to18`，
Boot 3/4 用 `bcprov-jdk18on` / `bcpkix-jdk18on`；核心模块本身不传递 BouncyCastle。
两个坐标包含相同的类， **不可同时引入**（classpath 重复类、module path 模块名冲突）。
若使用方已有自己的 BouncyCastle，可排除 starter 传递的坐标并保留自身坐标（版本需 ≥ 1.86）。
SM9 需要 BouncyCastle 1.86+；若运行时版本更低，启动时 SM9 处理器会因缺少相关类被自动跳过（记录 WARN），
其余算法与启动流程不受影响。如需使用 SM9，请确保运行时 BouncyCastle 版本不低于 1.86。

## 相关文档

- [密文格式](cipher-format.md)
- [算法安全性与选型建议](security.md)
- [日志](logging.md)
- [SM9 标识加密](algorithms/sm9.md)
