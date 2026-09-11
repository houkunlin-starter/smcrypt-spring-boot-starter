package com.houkunlin.smcrypt.spi;

import com.houkunlin.smcrypt.codec.CipherPayload;
import com.houkunlin.smcrypt.codec.EncodingDetector;
import com.houkunlin.smcrypt.handler.DecryptHandler;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 测试用自定义处理器，通过 {@code META-INF/services} 注册，模拟业务方接入自定义算法。
 */
public class DemoHandler implements DecryptHandler {
    private static final String PREFIX = "DEMOENC(";

    @Override
    public String algorithm() {
        return "DEMO";
    }

    @Override
    public boolean support(String propValue) {
        return propValue != null && propValue.length() > PREFIX.length()
                && propValue.regionMatches(true, 0, PREFIX, 0, PREFIX.length())
                && propValue.endsWith(")");
    }

    @Override
    public String getCipherText(String propValue) {
        return propValue.substring(PREFIX.length(), propValue.length() - 1);
    }

    @Override
    public String getDecryptText(String propValue) {
        CipherPayload payload = EncodingDetector.detect(getCipherText(propValue));
        return new String(payload.decode(), StandardCharsets.UTF_8);
    }

    @Override
    public String getEncryptText(String plainText) {
        return PREFIX + Base64.getEncoder().encodeToString(plainText.getBytes(StandardCharsets.UTF_8)) + ")";
    }
}
