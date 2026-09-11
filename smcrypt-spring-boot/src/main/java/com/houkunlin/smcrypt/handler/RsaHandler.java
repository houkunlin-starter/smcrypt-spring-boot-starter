package com.houkunlin.smcrypt.handler;

import com.houkunlin.smcrypt.BouncyCastleSupport;
import com.houkunlin.smcrypt.config.CipherConfig;
import com.houkunlin.smcrypt.key.PrivateKeyLoader;

import javax.crypto.Cipher;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * RSA 非对称加密处理器。
 *
 * <p>密文格式：{@code RSAENC(...)}；密钥内容支持 PEM 与 Base64/DER 两种私钥格式，
 * 加密时由私钥推导公钥。</p>
 *
 * @author HouKunLin
 */
public class RsaHandler extends AbstractCipherHandler {

    @Override
    public String algorithm() {
        return "RSA";
    }

    @Override
    protected String defaultTransformation() {
        return "RSA/ECB/PKCS1Padding";
    }

    @Override
    protected byte[] doDecrypt(byte[] cipherBytes, CipherConfig config) throws Exception {
        PrivateKey privateKey = PrivateKeyLoader.load(requireKey(), algorithm());
        Cipher cipher = Cipher.getInstance(config.transformation(), BouncyCastleSupport.provider());
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(cipherBytes);
    }

    @Override
    protected byte[] doEncrypt(byte[] plainBytes, CipherConfig config) throws Exception {
        PrivateKey privateKey = PrivateKeyLoader.load(requireKey(), algorithm());
        PublicKey publicKey = PrivateKeyLoader.derivePublicKey(privateKey, algorithm());
        Cipher cipher = Cipher.getInstance(config.transformation(), BouncyCastleSupport.provider());
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(plainBytes);
    }
}
