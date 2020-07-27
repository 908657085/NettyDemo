package com.gaoxh.nettystudy;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.encoders.Hex;

public class AESUtil {
    public static byte[] AESEncrypt(byte[] input, byte[] secretKey) {
        return AESEncrypt(input, new SecretKeySpec(secretKey, "AES"));
    }


    public static byte[] AESDecrypt(byte[] input, byte[] secretKey) {
        return AESDecrypt(input, new SecretKeySpec(secretKey, "AES"));
    }


    public static byte[] AESEncrypt(byte[] input, SecretKey secretKey) {
        return AESOperation(input, 1, secretKey);
    }


    public static byte[] AESDecrypt(byte[] input, SecretKey secretKey) {
        return AESOperation(input, 2, secretKey);
    }


    private static byte[] AESOperation(byte[] input, int mode, SecretKey secretKey) {
        try {
            if (mode == 1) {
                int plaintextLength = input.length + 4;
                int L = input.length + 4 | Integer.MIN_VALUE;
                byte[] tmp = new byte[plaintextLength];
                tmp[3] = (byte) (L >>> 0 & 0xFF);
                tmp[2] = (byte) (L >>> 8 & 0xFF);
                tmp[1] = (byte) (L >>> 16 & 0xFF);
                tmp[0] = (byte) (L >>> 24 & 0xFF);
                System.arraycopy(input, 0, tmp, 4, input.length);
                input = tmp;
                int blockSize = 16;
                if (plaintextLength % blockSize != 0) {
                    plaintextLength += blockSize - plaintextLength % blockSize;
                    tmp = new byte[plaintextLength];
                    System.arraycopy(input, 0, tmp, plaintextLength - input.length, input.length);
                    input = tmp;
                }
            }

            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(mode, secretKey);
            byte[] plain = cipher.doFinal(input);
            if (mode == 2) {
                int keep = findFirstNonZeroNumber(plain);
                int i = Utils.bytesToIntBigEndian(plain, keep);
                int len = (i & Integer.MAX_VALUE) - 4;
                return Arrays.copyOfRange(plain, keep + 4, keep + 4 + len);
            }
            return plain;
        } catch (Exception e) {
            throw new RuntimeException("aes operation for [" + Arrays.toString(input) + "]", e);
        }
    }


    public static int findFirstNonZeroNumber(byte[] bytes) {
        int keep;
        for (keep = 0; keep < bytes.length && bytes[keep] == 0; keep++) {
        }
        return keep;
    }


    public static void AESEncrypt(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset, SecretKey secretKey) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(1, secretKey);

            int n = evaluateIO(inputLen);
            int k = outputOffset + n - inputLen - 4;
            System.arraycopy(input, inputOffset, output, k + 4, inputLen);
            int L = inputLen + 4 | Integer.MIN_VALUE;
            output[k + 3] = (byte) (L >>> 0 & 0xFF);
            output[k + 2] = (byte) (L >>> 8 & 0xFF);
            output[k + 1] = (byte) (L >>> 16 & 0xFF);
            output[k + 0] = (byte) (L >>> 24 & 0xFF);
            Arrays.fill(output, outputOffset, k, (byte) 0);

            cipher.doFinal(output, outputOffset, n, output, outputOffset);
        } catch (Exception e) {
            throw new RuntimeException("aes operation for [" + Arrays.toString(input) + "]", e);
        }
    }


    private static void AESEncrypt(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset, byte[] secretKey) {
        AESEncrypt(input, inputOffset, inputLen, output, outputOffset, new SecretKeySpec(secretKey, "AES"));
    }


    public static int evaluateIO(int n) {
        n += 4;
        int result = (((0xF & n) > 0) ? 1 : 0) + (n >>> 4) << 4;
        return result;
    }


    public static SecretKey generateKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);
        return keyGenerator.generateKey();
    }


    public static SecretKey loadKey(byte[] encoded) {
        return new SecretKeySpec(encoded, "AES");
    }


    public static SecretKey sk = new SecretKeySpec(Hex.decode("1571ec8b49c18c52905c05a0caf7796c"), "AES");

    //int+type
    //int+byte（RSA(AES_k1,RSA_PK)）；int+byte（AES(SN+"_GH_"+int(salt),ASE_k1))
    // AES_k1：客户端随机生成秘钥 RSA_PK：客户端写死公钥
    public static byte[] register(RSAPublicKey pk, String sn, SecretKey secretKey) throws NoSuchAlgorithmException {


        byte[] bytes = RSAUtil.pkEncrypt(secretKey.getEncoded(), pk);
        byte[] bytes1 = sn.getBytes();
        int dataSize = AESUtil.evaluateIO(bytes1.length + 4);
        byte[] res = new byte[4 + 4 + 4 + bytes.length + 4 + dataSize];

        //bigendian传输
        res[3] = (byte) (res.length & 0xff);//total length
        res[2] = (byte) (res.length >> 8 & 0xff);
        res[1] = (byte) (res.length >> 16 & 0xff);
        res[0] = (byte) (res.length >> 24 & 0xff);

        res[7] = 1;//type

        res[11] = (byte) (bytes.length + 4 & 0xff);//total length
        res[10] = (byte) (bytes.length + 4 >> 8 & 0xff);
        res[9] = (byte) (bytes.length + 4 >> 16 & 0xff);
        res[8] = (byte) (bytes.length + 4 >> 24 & 0xff);

        System.arraycopy(bytes, 0, res, 12, bytes.length);

        res[15 + bytes.length] = (byte) (dataSize + 4 & 0xff);//total length
        res[14 + bytes.length] = (byte) (dataSize + 4 >> 8 & 0xff);
        res[13 + bytes.length] = (byte) (dataSize + 4 >> 16 & 0xff);
        res[12 + bytes.length] = (byte) (dataSize + 4 >> 24 & 0xff);

        System.arraycopy(bytes1, 0, res, 16 + bytes.length, bytes1.length);

        int salt = new SecureRandom().nextInt();//盐

        res[19 + bytes.length + bytes1.length] = (byte) (salt & 0xff);//total length
        res[18 + bytes.length + bytes1.length] = (byte) (salt >> 8 & 0xff);
        res[17 + bytes.length + bytes1.length] = (byte) (salt >> 16 & 0xff);
        res[16 + bytes.length + bytes1.length] = (byte) (salt >> 24 & 0xff);

        AESEncrypt(res, 16 + bytes.length, bytes1.length + 4,
                res, 16 + bytes.length, secretKey);
        return res;
    }


    private static void printlnBytes(byte[] data) {
        System.out.println("data.length: " + data.length);
        for (byte b : data) {
            System.out.print(b + " ");
        }
        System.out.println("");
        System.out.println("data_hex:" + Hex.toHexString(data));
        System.out.println("");
    }

    public static byte[] clientAESKey(byte[] clientKey, SecretKey encryptionKey) {
        int dataSize = AESUtil.evaluateIO(8 + clientKey.length);
        byte[] res = new byte[12 + dataSize];


        res[3] = (byte) (res.length & 0xFF);
        res[2] = (byte) (res.length >> 8 & 0xFF);
        res[1] = (byte) (res.length >> 16 & 0xFF);
        res[0] = (byte) (res.length >> 24 & 0xFF);

        res[7] = 4;

        res[11] = (byte) (dataSize + 4 & 0xFF);
        res[10] = (byte) (dataSize + 4 >> 8 & 0xFF);
        res[9] = (byte) (dataSize + 4 >> 16 & 0xFF);
        res[8] = (byte) (dataSize + 4 >> 24 & 0xFF);

        res[15] = 1;

        res[19] = (byte) (clientKey.length + 4 & 0xFF);
        res[18] = (byte) (clientKey.length + 4 >> 8 & 0xFF);
        res[17] = (byte) (clientKey.length + 4 >> 16 & 0xFF);
        res[16] = (byte) (clientKey.length + 4 >> 24 & 0xFF);

        System.arraycopy(clientKey, 0, res, 20, clientKey.length);
        AESEncrypt(res, 12, 8 + clientKey.length,
                res, 12, encryptionKey);
        return res;
    }


    public static byte[] clientHasConnected(SecretKey aesKey) {
        return message(4, 2, aesKey);
    }


    public static byte[] clientNotExist(SecretKey aesKey) {
        return message(4, 0, aesKey);
    }


    public static byte[] message(int msgType, int type, SecretKey aesKey) {
        int dataSize = AESUtil.evaluateIO(4);
        byte[] res = new byte[12 + dataSize];

        res[3] = (byte) (res.length & 0xFF);
        res[2] = (byte) (res.length >> 8 & 0xFF);
        res[1] = (byte) (res.length >> 16 & 0xFF);
        res[0] = (byte) (res.length >> 24 & 0xFF);


        res[7] = (byte) (msgType & 0xFF);
        res[6] = (byte) (msgType >> 8 & 0xFF);
        res[5] = (byte) (msgType >> 16 & 0xFF);
        res[4] = (byte) (msgType >> 24 & 0xFF);

        res[11] = (byte) (dataSize + 4 & 0xFF);
        res[10] = (byte) (dataSize + 4 >> 8 & 0xFF);
        res[9] = (byte) (dataSize + 4 >> 16 & 0xFF);
        res[8] = (byte) (dataSize + 4 >> 24 & 0xFF);

        res[15] = (byte) (type & 0xFF);
        res[14] = (byte) (type >> 8 & 0xFF);
        res[13] = (byte) (type >> 16 & 0xFF);
        res[12] = (byte) (type >> 24 & 0xFF);

        AESEncrypt(res, 12, 4, res, 12, aesKey);

        return res;
    }

    /**
     * 电脑端连接
     * @param aesKey
     * @param sn
     * @return
     */
    public static byte[] hostConnectReq(SecretKey aesKey, String sn) {
        byte[] in = sn.getBytes();
        int aesLen = evaluateIO(in.length);
        int count = 4 + 4 + 4 + aesLen;
        byte[] res = new byte[count];
        //bigendian传输
        res[3] = (byte) (count & 0xff);//total length
        res[2] = (byte) ((count >> 8) & 0xff);
        res[1] = (byte) ((count >> 16) & 0xff);
        res[0] = (byte) ((count >> 24) & 0xff);

        res[7] = 3;//type

        res[11] = (byte) ((aesLen + 4) & 0xff);//total length
        res[10] = (byte) ((aesLen + 4) >> 8 & 0xff);
        res[9] = (byte) ((aesLen + 4) >> 16 & 0xff);
        res[8] = (byte) ((aesLen + 4) >> 24 & 0xff);
        System.arraycopy(in, 0, res, 12, in.length);
        AESEncrypt(res, 12, in.length, res, 12, aesKey);
        return res;
    }


  public static final byte[] aes_getPlain(byte[] in,int L,byte[] out) {
        int keep = findFirstNonZeroNumber(in);
        int i = Utils.bytesToIntBigEndian(in, keep);
        int len = (i & ~0x80000000) - 4;
        out = new byte[keep + 4 + len];
        System.arraycopy(in, 0, out,keep + 4, in.length);
        return out;
    }

    public static void main(String[] args) {
//        System.out.println("设备已链接");
//        byte[] data = clientHasConnected(sk);
//        System.out.println("");
//        System.out.println("设备不存在");
//        data = clientNotExist(sk);
//        String sn = "1H8KUIGV";
//        System.out.println("sn:" + sn);
//        byte[] bytes = new byte[0];
//        try {
//            State.getInstance().generateKey();
//            System.out.println("aes_key:");
//            printlnBytes(State.getInstance().getSecretKey().getEncoded());
//            bytes = register(RSAUtil.pk, sn, State.getInstance().getSecretKey());
//            System.out.println("client_register_data:");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        printlnBytes(bytes);
//        System.out.println("电脑端连接");
//        bytes=hostConnectReq(sk,sn);
//        printlnBytes(bytes);
//        System.out.println("电脑端返回");
//        bytes = clientAESKey(State.getInstance().getSecretKey().getEncoded(),sk);
//        printlnBytes(bytes);
        byte[] newbyte= new byte[]{0,0,0,36};
       int size =  Utils.bytesToIntBigEndian(newbyte,0);
       System.out.println(size);
        System.out.println("test data");
        byte[] data = Hex.decode("0000002c0000000400000024bc58b9956fbc50fe7c14b0b914c030e3436b43901f2393124aa45b868f2e1354");
        printlnBytes(data);
        int length = Utils.bytesToIntBigEndian(data,0);
        System.out.println(length);


    }


}
