package com.gaoxh.nettystudy;

import java.io.ByteArrayOutputStream;
import javax.crypto.Cipher;


public class Utils
{
  public static int bytesToIntBigEndian(byte[] bytes, int offset) {
    int value = 0;
    value = (bytes[0 + offset] & 0xFF) << 24 | (bytes[1 + offset] & 0xFF) << 16 | (
      bytes[2 + offset] & 0xFF) << 8 | bytes[3 + offset] & 0xFF;
    return value;
  }

  
  public static byte[] int2Bytes(int value) {
    byte[] bytes = new byte[4];
    bytes[0] = (byte)(value & 0xFF);
    bytes[1] = (byte)(value >> 8 & 0xFF);
    bytes[2] = (byte)(value >> 16 & 0xFF);
    bytes[3] = (byte)(value >> 24 & 0xFF);
    return bytes;
  }
  
  public static byte[] rsaSplitCodec(Cipher cipher, int opMode, byte[] datas, int keySize) throws Exception {
    int maxBlock;
    if (opMode == 2) {
      maxBlock = keySize / 8;
    } else {
      maxBlock = keySize / 8 - 11;
    } 
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    int offSet = 0;
    
    int i = 0;
    try {
      while (datas.length > offSet) {
        byte[] buff; if (datas.length - offSet > maxBlock) {
          buff = cipher.doFinal(datas, offSet, maxBlock);
        } else {
          buff = cipher.doFinal(datas, offSet, datas.length - offSet);
        } 
        out.write(buff, 0, buff.length);
        i++;
        offSet = i * maxBlock;
      } 
    } finally {
      out.close();
    } 
    return out.toByteArray();
  }
}
