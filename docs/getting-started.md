# 环境要求与快速开始

[返回 README](../README.md) | [文档索引](README.md)

## 环境要求

| 使用场景        | JDK      | 对应依赖                       |
|-----------------|----------|--------------------------------|
| Spring Boot 2.x | Java 8+  | `smcrypt-spring-boot2-starter` |
| Spring Boot 3.x | Java 17+ | `smcrypt-spring-boot3-starter` |
| Spring Boot 4.x | Java 17+ | `smcrypt-spring-boot4-starter` |

核心模块以 Java 8 编译，BouncyCastle 使用 `jdk15to18` 变体以兼容低版本 JDK。
SM9 需要 BouncyCastle **1.86 及以上**（本 starter 已使用 1.86）。

## 快速开始

### 1. 引入依赖

Gradle：

```groovy
// 按 Spring Boot 版本三选一
implementation 'com.houkunlin:smcrypt-spring-boot2-starter:1.0.0'
implementation 'com.houkunlin:smcrypt-spring-boot3-starter:1.0.0'
implementation 'com.houkunlin:smcrypt-spring-boot4-starter:1.0.0'
```

Maven：

```xml
<dependency>
    <groupId>com.houkunlin</groupId>
    <artifactId>smcrypt-spring-boot3-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 生成密文

使用命令行工具生成 SM4 密文（详见 [加密工具](tools.md)）：

```bash
java -cp app.jar com.houkunlin.smcrypt.SmCryptCli \
    --algorithm SM4 \
    --key 0123456789abcdeffedcba9876543210 \
    --text my-db-password \
    --encoding base64
# 输出：SM4ENC(base64,xxxxxxxxxxxx)
```

### 3. 写入配置

`application.yml`：

```yaml
spring:
  datasource:
    password: SM4ENC(base64,xxxxxxxxxxxx)

smcrypt:
  sm4:
    key: 0123456789abcdeffedcba9876543210
```

或 `application.properties`：

```properties
spring.datasource.password=SM4ENC(base64,xxxxxxxxxxxx)
smcrypt.sm4.key=0123456789abcdeffedcba9876543210
```

### 4. 正常使用

```java
@Component
public class DemoService {
    @Value("${spring.datasource.password}")
    private String password; // 已自动解密为明文
}
```

## 相关文档

- [密文格式](cipher-format.md)
- [密钥配置](keys.md)
- [配置项参考](configuration.md)
- [加密工具](tools.md)
