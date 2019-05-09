package com.gaoxh.nettystudy.mqtt;


import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.*;


public class MqttChannelHandler extends SimpleChannelInboundHandler<MqttMessage> {

    private MqttClient client;

    public MqttChannelHandler(MqttClient mqttClient) {
        super();
        this.client = mqttClient;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) throws Exception {
        switch (msg.fixedHeader().messageType()) {
            case CONNACK:
                handleConnack(ctx.channel(), (MqttConnAckMessage) msg);
                break;
            case SUBACK:
                handleSubAck((MqttSubAckMessage) msg);
                break;
            case PUBLISH:
                handlePublish(ctx.channel(), (MqttPublishMessage) msg);
                break;
            case UNSUBACK:
                break;
            case PUBACK:
                handlePuback((MqttPubAckMessage) msg);
                break;
            case PUBREC:
                handlePubrec(ctx.channel(), msg);
                break;
            case PUBREL:
                handlePubrel(ctx.channel(), msg);
                break;
            case PUBCOMP:
                handlePubcomp(msg);
                break;
        }
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        if (client != null) {
            client.sendConnectMsg();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        if(client!=null){
            client.close();
            client.changeState(MqttClient.ClientState.NONE);
            this.client = null;
        }
    }



    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        try {
            if (client != null) {
                client.setException(cause);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleConnack(Channel channel, MqttConnAckMessage message) {
        switch (message.variableHeader().connectReturnCode()) {
            case CONNECTION_ACCEPTED:
                try {
                    if (client != null) {
                        client.changeState(MqttClient.ClientState.MQTT_CONNECTED);
                        client.subTopics();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD:
            case CONNECTION_REFUSED_IDENTIFIER_REJECTED:
            case CONNECTION_REFUSED_NOT_AUTHORIZED:
            case CONNECTION_REFUSED_SERVER_UNAVAILABLE:
            case CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION:
            default:
                System.out.println("mqtt connect error: " + message.variableHeader().connectReturnCode());
                try {
                    if (client != null) {
                        client.setException(new Throwable("mqtt connect error: " + message.variableHeader().connectReturnCode()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    private void handleSubAck(MqttSubAckMessage message) {
    }

    private void handlePublish(Channel channel, MqttPublishMessage message) {
        switch (message.fixedHeader().qosLevel()) {
            case AT_MOST_ONCE:
                break;

            case AT_LEAST_ONCE:
                if (message.variableHeader().packetId() != -1) {
                    MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
                    MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(message.variableHeader().packetId());
                    channel.writeAndFlush(new MqttPubAckMessage(fixedHeader, variableHeader));
                }
                break;

            case EXACTLY_ONCE:
                if (message.variableHeader().packetId() != -1) {
                    MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 0);
                    MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(message.variableHeader().packetId());
                    MqttMessage pubrecMessage = new MqttMessage(fixedHeader, variableHeader);
                    channel.writeAndFlush(pubrecMessage);
                }
                break;
        }
    }


    private void handlePuback(MqttPubAckMessage message) {

    }

    private void handlePubrec(Channel channel, MqttMessage message) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREL, false, MqttQoS.AT_LEAST_ONCE, false, 0);
        MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) message.variableHeader();
        MqttMessage pubrelMessage = new MqttMessage(fixedHeader, variableHeader);
        channel.writeAndFlush(pubrelMessage);
    }

    private void handlePubrel(Channel channel, MqttMessage message) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(((MqttMessageIdVariableHeader) message.variableHeader()).messageId());
        channel.writeAndFlush(new MqttMessage(fixedHeader, variableHeader));
    }

    private void handlePubcomp(MqttMessage message) {
        MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) message.variableHeader();
        System.out.println("public finish " + variableHeader.messageId());
    }
}
