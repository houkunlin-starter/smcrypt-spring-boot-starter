package com.houkunlin.smcrypt.pbe;

import com.houkunlin.smcrypt.TestContexts;
import org.bouncycastle.util.Arrays;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JasyptCipherHandlerTest {
    private static final String PASSWORD = "jasypt-password";
    private static final String TRANSFORMATION = "PBEWITHHMACSHA512ANDAES_256";
    private static final int ITERATIONS = 1000;

    @Test
    void roundTrip() throws Exception {
        JasyptCipherHandler handler = new JasyptCipherHandler();
        handler.setContext(TestContexts.context(properties()));
        String cipher = handler.getEncryptText("jasypt-data");
        assertTrue(cipher.startsWith("JASYPTENC("));
        assertEquals("jasypt-data", handler.getDecryptText(cipher));
    }

    /**
     * 按 Jasypt 的格式（{@code base64(salt ‖ iv ‖ cipher)}）手工生成密文，验证本处理器可解密。
     */
    @Test
    void decryptsJasyptFormatCipherText() throws Exception {
        byte[] salt = new byte[16];
        byte[] iv = new byte[16];
        SecretKeyFactory factory = SecretKeyFactory.getInstance(TRANSFORMATION);
        SecretKey key = factory.generateSecret(new PBEKeySpec(PASSWORD.toCharArray()));
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(salt, ITERATIONS, new IvParameterSpec(iv)));
        byte[] cipherBytes = cipher.doFinal("jasypt-data".getBytes(StandardCharsets.UTF_8));

        String value = "JASYPTENC("
                + Base64.getEncoder().encodeToString(Arrays.concatenate(salt, iv, cipherBytes))
                + ")";

        JasyptCipherHandler handler = new JasyptCipherHandler();
        handler.setContext(TestContexts.context(properties()));
        assertEquals("jasypt-data", handler.getDecryptText(value));
    }

    private static Map<String, String> properties() {
        Map<String, String> properties = new HashMap<>();
        properties.put("smcrypt.jasypt.password", PASSWORD);
        properties.put("smcrypt.jasypt.transformation", TRANSFORMATION);
        properties.put("smcrypt.jasypt.iterations", String.valueOf(ITERATIONS));
        return properties;
    }
}
