# 自定义算法 / 加密机接入

[返回 README](../README.md) | [文档索引](README.md)

当内置算法无法满足需求（例如需要调用加密机、KMS，或使用未内置的算法）时，业务系统可实现 SPI 接口接入。

## 1. 实现处理器接口

```java
package com.example.crypto;

import com.houkunlin.smcrypt.handler.DecryptHandler;

public class HsmDecryptHandler implements DecryptHandler {

    @Override
    public String algorithm() {
        return "HSM"; // 同时作为密文前缀：HSMENC(...)
    }

    @Override
    public boolean support(String value) {
        return value != null && value.length() > 7
                && value.regionMatches(true, 0, "HSMENC(", 0, 7)
                && value.endsWith(")");
    }

    @Override
    public String getCipherText(String value) {
        return value.substring(7, value.length() - 1);
    }

    @Override
    public String getDecryptText(String value) throws Exception {
        return HsmClient.decrypt(getCipherText(value)); // 调用加密机解密
    }

    @Override
    public String getEncryptText(String plainText) throws Exception {
        return "HSMENC(" + HsmClient.encrypt(plainText) + ")";
    }
}
```

## 2. 注册处理器

以下两种方式任选其一：

方式一：`META-INF/services/com.houkunlin.smcrypt.handler.DecryptHandler`

```
com.example.crypto.HsmDecryptHandler
```

方式二：`META-INF/spring.factories`

```properties
com.houkunlin.smcrypt.handler.DecryptHandler=com.example.crypto.HsmDecryptHandler
```

> 处理器必须提供无参构造方法（SPI 实例化要求）。

## 3. 访问 Spring 环境（可选）

若处理器需要读取 Spring 配置或使用内置密钥解析能力，额外实现 `DecryptHandlerAware`：

```java
public class HsmDecryptHandler implements DecryptHandler, DecryptHandlerAware {

    private SmCryptContext context;

    @Override
    public void setContext(SmCryptContext context) {
        this.context = context;
    }

    // 通过 context.getProperty("...") 读取配置，
    // 或 context.resolveKey("HSM") 复用密钥解析逻辑
}
```

## 4. 覆盖内置算法

处理器加载顺序为： **内置 → `META-INF/services` → `spring.factories`**，按算法名称去重，
后加载者覆盖先加载者。因此业务系统注册的同名算法（例如 `SM4`）会覆盖内置实现，便于统一替换为
加密机等外部实现。

## 相关文档

- [工作原理与模块结构](architecture.md)
- [加密工具](tools.md)
