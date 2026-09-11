package com.houkunlin.smcrypt.handler;

import com.houkunlin.smcrypt.BouncyCastleSupport;
import com.houkunlin.smcrypt.config.CipherConfig;
import com.houkunlin.smcrypt.key.PrivateKeyLoader;

import javax.crypto.Cipher;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * ECC 非对称加密处理器。
 *
 * <p>密文格式：{@code ECCENC(...)}；基于 ECIES 实现，密钥内容支持 PEM 与 Base64/DER
 * 两种私钥格式，加密时由私钥推导公钥。</p>
 *
 * @author HouKunLin
 */
public class EccHandler extends AbstractCipherHandler {

    @Override
    public String algorithm() {
        return "ECC";
    }

    @Override
    protected String defaultTransformation() {
        return "ECIES";
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
