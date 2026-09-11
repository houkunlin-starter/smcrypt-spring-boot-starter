# 工作原理与模块结构

[返回 README](../README.md) | [文档索引](README.md)

## 工作原理

以 Spring Boot 3 为例，启动流程如下：

```
SpringApplication.run()
        │
        ▼
prepareEnvironment()
        │
        ├── 加载 application.yml / application.properties / 命令行参数 / 环境变量 ...
        │
        ├── 触发 EnvironmentPostProcessor（本项目的 SmCryptEnvironmentPostProcessor）
        │        │
        │        ├── 加载全部密文处理器（内置 + 业务 SPI 注册）
        │        ├── 遍历所有 MapPropertySource / OriginTrackedMapPropertySource
        │        ├── 识别 {算法}ENC(...) 密文并解密
        │        └── 用明文替换原值（保留 Origin 信息）
        │
        ▼
创建 ApplicationContext、实例化 Bean
        │
        ▼
业务代码读取到的已是明文
```

关键点：

- 解密发生在配置加载完成之后、Bean 创建之前，因此对所有配置读取方式透明；
- 三个版本模块各自实现对应版本的 `EnvironmentPostProcessor`，仅做委托，核心逻辑统一在
  `smcrypt-spring-boot` 中，从而屏蔽 Spring Boot 4 的接口包名迁移（`org.springframework.boot.env`
  → `org.springframework.boot`）。

## 模块结构

| 模块                           | 职责                                                                              | 运行环境 |
|--------------------------------|-----------------------------------------------------------------------------------|----------|
| `smcrypt-spring-boot`          | 版本无关核心：编解码、算法处理器、密钥解析、解密引擎、SPI、加密工具               | Java 8+  |
| `smcrypt-spring-boot2-starter` | Spring Boot 2.x 适配器（`org.springframework.boot.env.EnvironmentPostProcessor`） | Java 8+  |
| `smcrypt-spring-boot3-starter` | Spring Boot 3.x 适配器（`org.springframework.boot.env.EnvironmentPostProcessor`） | Java 17+ |
| `smcrypt-spring-boot4-starter` | Spring Boot 4.x 适配器（`org.springframework.boot.EnvironmentPostProcessor`）     | Java 17+ |

核心模块的包结构：

```
com.houkunlin.smcrypt
├── SmCryptDecryptor            解密引擎：遍历/识别/解密/替换
├── SmCryptEncryptor            加密工具 API
├── SmCryptCli                  加密 / 解密命令行工具
├── SmCryptContext              加解密上下文（属性查询 + 密钥解析）
├── BouncyCastleSupport         BouncyCastle Provider 持有
├── SmCryptLog                  核心日志门面
├── SmCryptLogback              启动早期独立日志
├── codec/                      编码枚举、Hex/Base64 编解码、自动识别
├── config/                     算法配置（CipherConfig、MacAlgorithm）
├── handler/                    算法处理器 SPI 及内置实现
├── key/                        密钥解析、私钥加载与密钥生成
└── spi/                        处理器加载器
```

## 相关文档

- [快速开始](getting-started.md)
- [密文格式](cipher-format.md)
- [自定义算法 / 加密机接入](extension.md)
