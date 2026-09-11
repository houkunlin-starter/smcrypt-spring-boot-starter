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
 * <p>本类为命令行工具，标准输出/错误流是其结果与用法提示的对外契约，并非日志输出，
 * 因此保留 {@code System.out}/{@code System.err} 并抑制 SonarQube 规则 {@code java:S106}。</p>
 *
 * @author HouKunLin
 */
@SuppressWarnings("java:S106")
public class SmCryptCli {
    /**
     * 配置属性键前缀
     */
    private static final String PROPERTY_PREFIX = "smcrypt.";
    /**
     * 短选项前缀
     */
    private static final String OPTION_PREFIX = "-";
    /**
     * 长选项前缀
     */
    private static final String LONG_OPTION_PREFIX = "--";

    /**
     * 命令行入口
     *
     * @param args 命令行参数
     */
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
        applyOption(options, "key", PROPERTY_PREFIX + lower + ".key");
        applyOption(options, "file", PROPERTY_PREFIX + lower + ".file");
        applyOption(options, "transformation", PROPERTY_PREFIX + lower + ".transformation");
        applyOption(options, "mode", PROPERTY_PREFIX + lower + ".mode");
        applyOption(options, "padding", PROPERTY_PREFIX + lower + ".padding");
        applyOption(options, "iv", PROPERTY_PREFIX + lower + ".iv");
        applyOption(options, "encoding", PROPERTY_PREFIX + lower + ".encoding");

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

    /**
     * 将命令行选项写入系统属性，供 {@link SmCryptContext} 解析
     *
     * @param options  命令行选项
     * @param option   选项名
     * @param property 对应的系统属性名
     */
    private static void applyOption(Map<String, String> options, String option, String property) {
        String value = options.get(option);
        if (value != null && !value.trim().isEmpty()) {
            System.setProperty(property, value.trim());
        }
    }

    /**
     * 解析命令行参数为选项映射
     *
     * <p>支持 {@code --name value}、{@code --name=value} 与 {@code -x value} 三种写法；
     * 无值的开关（如 {@code --decrypt}）映射为空串。</p>
     *
     * @param args 命令行参数
     * @return 选项映射
     */
    private static Map<String, String> parse(String[] args) {
        Map<String, String> options = new LinkedHashMap<>();
        int index = 0;
        while (index < args.length) {
            String arg = args[index];
            if (!arg.startsWith(OPTION_PREFIX)) {
                index++;
                continue;
            }
            int prefixLength = arg.startsWith(LONG_OPTION_PREFIX)
                    ? LONG_OPTION_PREFIX.length()
                    : OPTION_PREFIX.length();
            String name = arg.substring(prefixLength);
            int equals = name.indexOf('=');
            if (equals >= 0) {
                options.put(name.substring(0, equals), name.substring(equals + 1));
                index++;
            } else if (index + 1 < args.length && !args[index + 1].startsWith(OPTION_PREFIX)) {
                options.put(name, args[index + 1]);
                index += 2;
            } else {
                options.put(name, "");
                index++;
            }
        }
        return options;
    }

    /**
     * 打印命令行用法说明
     */
    private static void printUsage() {
        System.out.println("用法：SmCryptCli --algorithm <算法> --text <内容> [选项]");
        System.out.println("  --algorithm, -a   算法名称：SM4 / SM2 / AES / DES / DESEDE / RSA / ECC");
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
