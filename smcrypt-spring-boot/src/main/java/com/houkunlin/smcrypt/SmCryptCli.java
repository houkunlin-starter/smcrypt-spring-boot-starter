package com.houkunlin.smcrypt;

import org.springframework.core.io.FileSystemResourceLoader;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 密文生成 / 解密命令行工具。
 *
 * <p>用法示例：</p>
 * <pre>{@code
 * # 使用 SM4 密钥加密
 * java -cp app.jar com.houkunlin.smcrypt.SmCryptCli \
 *     --algorithm SM4 --key 0123456789abcdeffedcba9876543210 --text hello
 *
 * # 指定 Base64 输出编码
 * java -cp app.jar com.houkunlin.smcrypt.SmCryptCli \
 *     --algorithm SM4 --key 0123... --text hello --encoding base64
 *
 * # 解密
 * java -cp app.jar com.houkunlin.smcrypt.SmCryptCli \
 *     --algorithm SM4 --key 0123... --decrypt --text SM4ENC(hex,xxxx)
 * }</pre>
 *
 * @author HouKunLin
 */
public class SmCryptCli {

    public static void main(String[] args) {
        Map<String, String> options = parse(args);
        if (options.containsKey("help") || options.containsKey("h")) {
            printUsage();
            return;
        }

        String algorithm = options.get("algorithm");
        if (algorithm == null || algorithm.trim().isEmpty()) {
            System.err.println("缺少参数：--algorithm");
            printUsage();
            System.exit(1);
            return;
        }
        String text = options.get("text");
        if (text == null) {
            System.err.println("缺少参数：--text");
            printUsage();
            System.exit(1);
            return;
        }

        String lower = algorithm.toLowerCase();
        applyOption(options, "key", "smcrypt." + lower + ".key");
        applyOption(options, "file", "smcrypt." + lower + ".file");
        applyOption(options, "transformation", "smcrypt." + lower + ".transformation");
        applyOption(options, "mode", "smcrypt." + lower + ".mode");
        applyOption(options, "padding", "smcrypt." + lower + ".padding");
        applyOption(options, "iv", "smcrypt." + lower + ".iv");
        applyOption(options, "encoding", "smcrypt." + lower + ".encoding");

        SmCryptContext context = new SmCryptContext(System::getProperty, new FileSystemResourceLoader());
        SmCryptEncryptor encryptor = new SmCryptEncryptor(context);
        try {
            if (options.containsKey("decrypt")) {
                System.out.println(encryptor.decrypt(text));
            } else {
                System.out.println(encryptor.encrypt(algorithm, text));
            }
        } catch (Exception e) {
            SmCryptLog.error("执行失败", e);
            System.exit(1);
        }
    }

    private static void applyOption(Map<String, String> options, String option, String property) {
        String value = options.get(option);
        if (value != null && !value.trim().isEmpty()) {
            System.setProperty(property, value.trim());
        }
    }

    private static Map<String, String> parse(String[] args) {
        Map<String, String> options = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("-")) {
                continue;
            }
            String name = arg.replaceFirst("^--?", "");
            int equals = name.indexOf('=');
            if (equals >= 0) {
                options.put(name.substring(0, equals), name.substring(equals + 1));
            } else if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                options.put(name, args[++i]);
            } else {
                options.put(name, "");
            }
        }
        return options;
    }

    private static void printUsage() {
        System.out.println("用法：SmCryptCli --algorithm <算法> --text <内容> [选项]");
        System.out.println("  --algorithm, -a   算法名称：SM4 / SM2 / AES / DES / RSA / ECC");
        System.out.println("  --text, -t        待加密明文；配合 --decrypt 时表示待解密密文");
        System.out.println("  --key, -k         密钥内容（hex / Base64 / PEM）");
        System.out.println("  --file, -f        密钥文件路径（file: 或 classpath:）");
        System.out.println("  --encoding, -e    加密输出编码：hex / base64（默认 base64）");
        System.out.println("  --transformation  自定义 JCE 变换串，如 AES/GCM/NoPadding");
        System.out.println("  --mode            加密模式，如 CBC、GCM、C1C3C2");
        System.out.println("  --padding         填充方式，默认 PKCS5Padding");
        System.out.println("  --iv              初始向量（hex / Base64）");
        System.out.println("  --decrypt         解密模式");
        System.out.println("  --help, -h        显示帮助");
    }
}
