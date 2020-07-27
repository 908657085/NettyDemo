package com.gaoxh.nettystudy.mqtt;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class MqttClientManager {

    private static final int MIN_MSG_ID = 1;        // Lowest possible MQTT message ID to use
    private static final int MAX_MSG_ID = 65535;    // Highest possible MQTT message ID to use
    EventLoopGroup eventExecutors = new NioEventLoopGroup();
    Bootstrap bootstrap;
    List<MqttClient> mqttClientList = new CopyOnWriteArrayList<>();
    Random random = new Random();
    private int nextMsgId = MIN_MSG_ID - 1;            // The next available message ID to use
    private Hashtable inUseMsgIds = new Hashtable();                    // Used to store a set of in-use message IDs
    private int keepAliveSeconds;

    public MqttClientManager(String remoteHost, int remoteTcpPort, int keepAliveSeconds) {
        this.keepAliveSeconds = keepAliveSeconds;
        bootstrap = new Bootstrap()
                .group(eventExecutors)
                //禁用nagle算法 Nagle算法就是为了尽可能发送大块数据，避免网络中充斥着许多小数据块。
                .option(ChannelOption.TCP_NODELAY, true)//屏蔽Nagle算法试图
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                //指定NIO方式   //指定是NioSocketChannel, 用NioSctpChannel会抛异常
                .channel(NioSocketChannel.class)
                .remoteAddress(remoteHost, remoteTcpPort)
                //指定编解码器，处理数据的Handler
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast("mqttDecoder", new MqttDecoder());
                        socketChannel.pipeline().addLast("mqttEncoder", MqttEncoder.INSTANCE);
                        socketChannel.pipeline().addLast("idleStateHandler", new IdleStateHandler(keepAliveSeconds, keepAliveSeconds, 0));
                        socketChannel.pipeline().addLast("mqttPingHandler", new MqttPingHandler(MqttClientManager.this, keepAliveSeconds));
                    }
                });
    }

    public synchronized int getNextMessageId() throws Exception {
        int startingMessageId = nextMsgId;
        // Allow two complete passes of the message ID range. This gives
        // any asynchronous releases a chance to occur
        int loopCount = 0;
        do {
            nextMsgId++;
            if (nextMsgId > MAX_MSG_ID) {
                nextMsgId = MIN_MSG_ID;
            }
            if (nextMsgId == startingMessageId) {
                loopCount++;
                if (loopCount == 2) {
                    throw new Exception("no msg id can use");
                }
            }
        } while (inUseMsgIds.containsKey(new Integer(nextMsgId)));
        return nextMsgId;
    }

    public void addConnect() {
        MqttClient mqttClient = new MqttClient(this, keepAliveSeconds);
        mqttClientList.add(mqttClient);
        mqttClient.connect();
    }

    public void addConnect(int count){
        for(int i= 0;i<count;i++){
            MqttClient mqttClient = new MqttClient(this, keepAliveSeconds,i);
            mqttClientList.add(mqttClient);
            mqttClient.connect();
        }
    }

    public int getConnectCount(){
        int count =0;
        for(MqttClient mqttClient:mqttClientList){
            if(mqttClient.state == MqttClient.ClientState.MQTT_CONNECTED){
                count++;
            }
        }
        return count;
    }

    public void dumpStates() {
        int noneConnCount = 0;
        int connectingCount = 0;
        int mqttConnCount = 0;
        int socketConnCount = 0;
        int exceptionCount = 0;
        System.out.println("connection size " + mqttClientList.size());
        for (int i = 0; i < mqttClientList.size(); i++) {
            MqttClient.ClientState clientState = mqttClientList.get(i).state;
            switch (clientState) {
                case NONE:
                    noneConnCount++;
                    break;
                case SOCKET_CONNECTED:
                    socketConnCount++;
                    break;
                case MQTT_CONNECTED:
                    mqttConnCount++;
                    break;
                case EXCEPTION:
                    exceptionCount++;
                    break;
                case CONNECTING:
                    connectingCount++;
                    break;
                default:
                    break;
            }
        }
        System.out.println("idle count: " + noneConnCount + " \r\n"
                + "connecting count: " + connectingCount + "\r\n"
                + "socket connect count: " + socketConnCount + "\r\n"
                + "mqtt connect count : " + mqttConnCount + "\r\n"
                + "exception count : " + exceptionCount + "\r\n"
        );
    }
}
