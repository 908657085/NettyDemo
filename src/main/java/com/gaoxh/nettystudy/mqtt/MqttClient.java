package com.gaoxh.nettystudy.mqtt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.mqtt.*;

import java.util.UUID;

public class MqttClient {

    Channel channel;
    Throwable cause;
    ClientState state = ClientState.NONE;
    private MqttClientManager clientManager;
    private Object stateLock = new Object();
    private String id;
    private int keepAliveSeconds;

    public MqttClient(MqttClientManager clientManager, int keepAliveSeconds) {
        this.clientManager = clientManager;
        this.id = UUID.randomUUID().toString();
        this.keepAliveSeconds = keepAliveSeconds;
    }

    public void connect() {
        synchronized (stateLock) {
            if (state == ClientState.NONE) {
                clientManager.bootstrap.connect().addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future != null && future.channel() != null && future.channel().isActive()) {
                            channel = future.channel();
                            changeState(ClientState.SOCKET_CONNECTED);
                            channel.pipeline().addLast("mqttHandler", new MqttChannelHandler(MqttClient.this));
                        } else {
                            state = ClientState.NONE;
                        }
                    }
                }).getNow();
            }
            state = ClientState.CONNECTING;
        }
    }

    public void sendConnectMsg() {
        try {
            channel.writeAndFlush(MqttMessageBuilders
                    .connect()
                    .cleanSession(true)
                    .clientId(id)
                    .keepAlive(keepAliveSeconds)
                    .build());
        } catch (Exception e) {
            e.printStackTrace();
            setException(e.getCause());
        }
    }

    public void changeState(ClientState clientState) {
        //System.out.println("change client state: old : " + state + " new: " + clientState);
        synchronized (stateLock) {
            this.state = clientState;
        }
    }

    public void subTopics() {
        try {
            channel.writeAndFlush(MqttMessageBuilders.subscribe()
                    .messageId(clientManager.getNextMessageId())
                    .addSubscription(MqttQoS.AT_MOST_ONCE, "/netty/test/" + id)
                    .addSubscription(MqttQoS.AT_MOST_ONCE, "/netty").build()
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setException(Throwable cause) {
        synchronized (stateLock) {
            state = ClientState.EXCEPTION;
            this.cause = cause;
            close();
        }
    }

    public void close() {
        try {
            if (channel != null) {
                channel.closeFuture().getNow();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            channel = null;
        }
    }

    public void publish(String content) {
        synchronized (stateLock) {
            if (state != ClientState.MQTT_CONNECTED) {
                System.out.println("client not connected");
                return;
            }
        }
        ByteBuf buf = null;
        try {
            byte[] data = UUID.randomUUID().toString().getBytes();
            UnpooledByteBufAllocator allocator1 = new UnpooledByteBufAllocator(true);
            buf = allocator1.buffer();
            buf.writeBytes(content.getBytes());
            MqttMessage mqttMessage = MqttMessageBuilders.publish().
                    messageId(clientManager.getNextMessageId())
                    .qos(MqttQoS.valueOf(clientManager.random.nextInt(2)))
                    .topicName("/netty/test/" + UUID.randomUUID().toString())
                    .retained(false)
                    .payload(buf)
                    .build();
            channel.writeAndFlush(mqttMessage);
        } catch (Exception e) {
            e.printStackTrace();
            cause = e.getCause();
            disconnect();
        } finally {
            try {
                buf.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void disconnect() {
        try {
            MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.DISCONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0);
            MqttMessageIdVariableHeader variableHeader = null;
            variableHeader = MqttMessageIdVariableHeader.from(clientManager.getNextMessageId());
            MqttMessage pubrecMessage = new MqttMessage(fixedHeader, variableHeader);
            channel.writeAndFlush(pubrecMessage);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            synchronized (stateLock) {
                state = ClientState.NONE;
            }
            close();
        }
    }

    public enum ClientState {
        NONE,
        CONNECTING,
        SOCKET_CONNECTED,
        MQTT_CONNECTED,
        EXCEPTION,
    }
}
