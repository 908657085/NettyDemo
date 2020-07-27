package com.gaoxh.nettystudy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * echo demo
 */
public class JavaNioDemo {


    public static void main(String args[]){
        try {
            startNioServer(8080);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void startNioServer(int port) throws IOException {
        // 打开服务端ServerSocketChannel
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        // 设置为非阻塞模式
        serverChannel.configureBlocking(false);
        // 绑定一个本地端口，这样客户端便可以通过这个端口连接到服务器
        serverChannel.bind(new InetSocketAddress(port));

        // 打开selector
        Selector selector = Selector.open();
        // 注意关心的事件是OP_ACCEPT，表示只关心接受事件即接受客户端到服务器的连接
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            // select()阻塞直到注册的某个事件就绪并会更新SelectionKey的状态
            int readyChannels = selector.select();
            if (readyChannels <= 0) {
                continue;
            }

            // 得到就绪的key集合，key中保存有就绪的事件以及对应的Channel通道
            Set<SelectionKey> SelectorKeySet = selector.selectedKeys();
            Iterator<SelectionKey> iterator = SelectorKeySet.iterator();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            // 遍历选择键
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    // 处理accept事件
                    SocketChannel socketChannel = serverChannel.accept();
                    socketChannel.configureBlocking(false);
                    // 注意此处新增关心事件OP_READ
                    socketChannel.register(selector, SelectionKey.OP_READ);
                } else if (key.isReadable()) {
                    // 处理read事件
                    SocketChannel socketChannel = (SocketChannel) key.channel();
                    buffer.clear();
                    socketChannel.read(buffer);
                    buffer.flip();
                    socketChannel.write(buffer);
                }
                iterator.remove();
            }
        }
    }
}
