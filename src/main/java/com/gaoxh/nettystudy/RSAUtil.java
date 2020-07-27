package com.gaoxh.nettystudy;


import java.security.Key;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;

import javax.crypto.Cipher;


public class RSAUtil {

    public static RSAPublicKey pk;

    static {

        try {
            pk = (RSAPublicKey) readPk();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static PublicKey readPk() throws Exception {
        return RSAKeyGenerator.getPublicKey("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgDlADmQScYyTZ9nRUaG9U0+po/F0JsFK\n" +
                "9GOUx5fidqUpCyeLTvGKmfHIJvc3nDAi5dKftQAoc38M+kQxuwB8LQIXpDqzi5U3lzN/7MhL07yZ\n" +
                "xZ1zQdiCrQwajtv+cHBJl5bv1bDA0GDzUre51IbmVSCKruXFGjXU7sM7vsCx9QzLdUCL0M52/0OP\n" +
                "tTvXLw81e+EM3ICYg5YPO7POfyfFLVPLiQp2Whzu0PP5hWMVpUQNVr55xfsDsLgjn+033G4bT9MT\n" +
                "wxy3PCoMx6F+alYqeD+2IfAxZnW1jXBm9FftwS7CUVsGG3Gn+gEvjBZ/moe+8qzAGEtEYBuFWGjg\n" +
                "ufHPFwIDAQAB");
    }

    private static byte[] RSAOperation(byte[] data, int mode, Key key, int bitLength) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(mode, key);
            return Utils.rsaSplitCodec(cipher, mode, data, bitLength);
        } catch (Exception e) {
            throw new RuntimeException("解密字符串[" + Arrays.toString(data) + "]时遇到异常", e);
        }
    }

    public static byte[] pkEncrypt(byte[] data, RSAPublicKey pk) {
        return RSAOperation(data, Cipher.ENCRYPT_MODE, pk, pk.getModulus().bitLength());
    }
}

