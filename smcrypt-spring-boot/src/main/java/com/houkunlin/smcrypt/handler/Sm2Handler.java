package com.houkunlin.smcrypt.handler;

import com.houkunlin.smcrypt.config.CipherConfig;
import com.houkunlin.smcrypt.key.PrivateKeyLoader;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;

/**
 * SM2 国密非对称加密处理器。
 *
 * <p>密文格式：{@code SM2ENC(...)}；密钥内容支持 PEM 与 Base64/DER 两种私钥格式。
 * 默认采用 C1C3C2 密文顺序，可通过 {@code smcrypt.sm2.mode=C1C2C3} 切换。</p>
 *
 * @author HouKunLin
 */
public class Sm2Handler extends AbstractCipherHandler {
    private final SecureRandom random = new SecureRandom();

    @Override
    public String algorithm() {
        return "SM2";
    }

    @Override
    protected String defaultTransformation() {
        return "SM2";
    }

    @Override
    protected byte[] doDecrypt(byte[] cipherBytes, CipherConfig config) throws Exception {
        PrivateKey privateKey = PrivateKeyLoader.load(requireKey(), algorithm());
        AsymmetricKeyParameter keyParameter = PrivateKeyFactory.createKey(privateKey.getEncoded());
        SM2Engine engine = new SM2Engine(resolveMode(config));
        engine.init(false, keyParameter);
        return engine.processBlock(cipherBytes, 0, cipherBytes.length);
    }

    @Override
    protected byte[] doEncrypt(byte[] plainBytes, CipherConfig config) throws Exception {
        PrivateKey privateKey = PrivateKeyLoader.load(requireKey(), algorithm());
        PublicKey publicKey = PrivateKeyLoader.derivePublicKey(privateKey, algorithm());
        AsymmetricKeyParameter keyParameter = PublicKeyFactory.createKey(publicKey.getEncoded());
        SM2Engine engine = new SM2Engine(resolveMode(config));
        engine.init(true, new ParametersWithRandom(keyParameter, random));
        return engine.processBlock(plainBytes, 0, plainBytes.length);
    }

    private SM2Engine.Mode resolveMode(CipherConfig config) {
        return "C1C2C3".equalsIgnoreCase(config.mode()) ? SM2Engine.Mode.C1C2C3 : SM2Engine.Mode.C1C3C2;
    }
}
