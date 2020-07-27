package com.gaoxh.nettystudy;

import org.bouncycastle.util.encoders.Base64;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

/**
 * RSA算法，实现数据的加密解密。
 *
 * @author ShaoJiang
 */
public class RSAKeyGenerator {

    public static PublicKey getPublicKey(String key) throws Exception {
        byte[] keyBytes;
        keyBytes = Base64.decode(key);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }
}