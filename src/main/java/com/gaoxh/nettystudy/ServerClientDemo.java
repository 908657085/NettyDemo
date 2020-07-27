package com.gaoxh.nettystudy;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class ServerClientDemo {

    public static void main(String[] args) {
        //startNettyServer();
        startNormalServer();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        startNettyClient();
    }

    public static void startNettyServer() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap server = new ServerBootstrap();
        server.group(bossGroup, workerGroup)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .channel(NioServerSocketChannel.class)
                .handler(new ChannelHandler() {
                    @Override
                    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
                        System.out.println("handlerAdded");
                    }

                    @Override
                    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
                        System.out.println("handlerRemoved");
                    }

                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                        cause.printStackTrace();
                        System.out.println("exceptionCaught");
                    }
                })
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        //获取管道
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        //字符串解码器
                        pipeline.addLast(new StringDecoder());
                        //字符串编码器
                        pipeline.addLast(new StringEncoder());
                        //处理类
                        pipeline.addLast(new ServerHandler());
                    }
                });
        server.bind(8080).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess() && future.channel() != null && future.channel().isOpen() && future.channel().isActive()) {
                    System.out.println("server start success");
                } else {
                    System.out.println("server start error");
                    future.cause().printStackTrace();
                }
            }
        }).getNow();
    }

    public static void startNettyClient() {
        int keepAlive = 30;
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .remoteAddress("127.0.0.1", 8080)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline channelPipeline = ch.pipeline();
                        channelPipeline.addLast("decoder", new StringDecoder());
                        channelPipeline.addLast("encoder", new StringEncoder());
                        channelPipeline.addLast("idleStateHandler", new IdleStateHandler(keepAlive, keepAlive, 0));
                        channelPipeline.addLast("msgHandler", new ClientHandler());
                    }
                });
        bootstrap.connect().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess() && future.channel() != null && future.channel().isOpen() && future.channel().isActive()) {
                    System.out.println("client start success");

                    new Thread() {
                        @Override
                        public void run() {
                            while (true) {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                System.out.println("client send msg");
                                future.channel().writeAndFlush("time is " + System.currentTimeMillis());
                            }
                        }
                    }.start();
                } else {
                    future.cause().printStackTrace();
                }
            }
        }).getNow();
    }

    public static void startNormalServer() {
        new Thread() {
            @Override
            public void run() {
                try {
                    ServerSocket serverSocket = new ServerSocket();
                    serverSocket.bind(new InetSocketAddress("127.0.0.1", 8080));
                    while (true) {
                        Socket socket = serverSocket.accept();
                        new ClientHandleThread(socket).start();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public static class ServerHandler extends SimpleChannelInboundHandler<String> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
            System.out.println("read client msg: " + msg);
            ctx.channel().writeAndFlush("I am server ,hello! " + msg);
        }
    }

    public static class ClientHandler extends SimpleChannelInboundHandler<String> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
            System.out.println("read server replay: " + msg);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            ctx.channel().writeAndFlush(" i am client, who are you?");
            super.channelActive(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
        }
    }

    public static class ClientHandleThread extends Thread {
        Socket socket;

        public ClientHandleThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                byte[] buffer = new byte[1024];
                while (true) {
                    int i = inputStream.read(buffer);
                    if (i > 0) {
                        String data = new String(Arrays.copyOfRange(buffer,0,i));
                        System.out.println("read data : " + data);
                        outputStream.write(("I am server replay: " + data).getBytes());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    inputStream.close();
                    outputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
