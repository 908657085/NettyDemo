package com.gaoxh.nettystudy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;

public class ByteBufDemo {

    public static void main(String[] args) {
        byteBufDemo();
    }

    public static void byteBufDemo() {
        ByteBuf byteBuf = UnpooledByteBufAllocator.DEFAULT.buffer(100);
        try {
            System.out.println("arrayOffset" + byteBuf.arrayOffset());
        }catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("capacity" + byteBuf.capacity());
        byteBuf.writeByte(1);

        try {
            System.out.println("arrayOffset" + byteBuf.arrayOffset());
        }catch (Exception e){
            e.printStackTrace();
        }

        System.out.println(byteBuf.nioBufferCount());
    }

}
