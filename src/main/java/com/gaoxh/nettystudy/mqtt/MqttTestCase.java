package com.gaoxh.nettystudy.mqtt;

import java.util.concurrent.TimeUnit;

public class MqttTestCase {
    /**
     * 测试连接数量 100
     */
    public static int CONNECTION_COUNT = 1;
    /**
     * 重连以及断开间隔 500毫秒
     */
    public static int DISCONNECT_DELAY_MILLISECONDS = 500;

    /**
     * 连接断连次数
     */
    public static int RECONNECT_COUNT = 1;

    public static MqttClientManager clientManager = null;
    public static int testCount = 0;

    public static void main(String[] args) {
        System.out.println("使用示例:\n./nettyStudy 10 1 500");
        System.out.println("参数1:连接数量 默认值 1");
        System.out.println("参数2:断开连接次数 默认值 1");
        System.out.println("参数3:断开以及连接操作间隔 默认值 500　非必填项");
        if (args == null) {
            System.out.println("使用默认参数测试:");
        } else {
            System.out.println("使用输入参数");
            if (args.length > 0) {
                try {
                    int connectCount = Integer.parseInt(args[0]);
                    CONNECTION_COUNT = connectCount;
                } catch (Exception e) {
                    System.out.println("测试连接数参数异常:" + args[0]);
                }
            }
            if(args.length>1){
                try {
                    int reconnectCount = Integer.parseInt(args[1]);
                    RECONNECT_COUNT = reconnectCount;
                } catch (Exception e) {
                    System.out.println("测试循环次数参数异常:" + args[1]);
                }
            }
            if(args.length>2){
                try {
                    int delay = Integer.parseInt(args[2]);
                    DISCONNECT_DELAY_MILLISECONDS = delay;
                } catch (Exception e) {
                    System.out.println("操作等待间隔参数异常:" + args[2]);
                }
            }
        }
        System.out.println("连接数量:" + CONNECTION_COUNT);
        System.out.println("连接断开次数:" + RECONNECT_COUNT);
        System.out.println("操作间隔:" + DISCONNECT_DELAY_MILLISECONDS + "ms");
        clientManager = new MqttClientManager(Config.SERVER, Config.PORT, 30);
        clientManager.addConnect(CONNECTION_COUNT);
        checkState();
    }

    public static void checkState() {
        clientManager.eventExecutors.schedule(new Runnable() {
            @Override
            public void run() {
                int count = clientManager.getConnectCount();
                if (count < CONNECTION_COUNT) {
                    System.out.println("等待客户端连接完成:" + count);
                    checkState();
                } else {
                    System.out.println("连接客户端数量:" + count);
                    System.out.println("客户端连接完成,开始测试流程");
                    disConnect();
                }
            }
        }, DISCONNECT_DELAY_MILLISECONDS, TimeUnit.MILLISECONDS);
    }

    public static void checkDisConnectState() {
        clientManager.eventExecutors.schedule(new Runnable() {
            @Override
            public void run() {
                int count = clientManager.getConnectCount();
                if (count != 0) {
                    System.out.println("等待客户端断连结束:" + count);
                    checkDisConnectState();
                } else {
                    System.out.println("客户端断开完成,开始连接:" + testCount);
                    connect();
                }
            }
        }, DISCONNECT_DELAY_MILLISECONDS, TimeUnit.MILLISECONDS);
    }

    public static void disConnect() {
        clientManager.eventExecutors.schedule(new Runnable() {
            @Override
            public void run() {
                System.out.println("断开连接");
                for (MqttClient mqttClient : clientManager.mqttClientList) {
                    mqttClient.disconnect();
                }
                testCount++;
                if (testCount > RECONNECT_COUNT) {
                    System.out.println("测试完成:退出");
                    System.exit(0);
                } else {
                    checkDisConnectState();
                }
            }
        }, DISCONNECT_DELAY_MILLISECONDS, TimeUnit.MILLISECONDS);
    }

    public static void connect() {
        clientManager.eventExecutors.schedule(new Runnable() {
            @Override
            public void run() {
                System.out.println("开启连接");
                for (MqttClient mqttClient : clientManager.mqttClientList) {
                    mqttClient.connect();
                }
                checkState();
            }
        }, DISCONNECT_DELAY_MILLISECONDS, TimeUnit.MILLISECONDS);
    }
}
