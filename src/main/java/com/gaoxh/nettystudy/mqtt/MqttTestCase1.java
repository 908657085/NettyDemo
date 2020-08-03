package com.gaoxh.nettystudy.mqtt;

import java.util.concurrent.TimeUnit;

public class MqttTestCase1 {
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
        System.out.println("=================================使用示例:\nnettyStudy 200 test J102D03D00103,L220190408002\n=================================");
        if (args.length > 0) {
            try {
                int reconnectCount = Integer.parseInt(args[0]);
                RECONNECT_COUNT = reconnectCount;
            } catch (Exception e) {
                System.out.println("测试循环次数参数异常:" + args[1]);
            }
        }
        String server = Config.SERVER_TEST;
        if (args.length > 1) {
            try {
                String environment = String.valueOf(args[1]);
                if ("test".equals(environment)) {
                    server = Config.SERVER_TEST;
                } else if ("uat".equals(environment)) {
                    server = Config.SERVER_UAT;
                } else if ("release".equals(environment)) {
                    server = Config.SERVER_RELEASE;
                }
            } catch (Exception e) {
                System.out.println("测试环境选项错误,正确示例:\ntest\nuat\nrelease");
            }
        }
        clientManager = new MqttClientManager(server, Config.PORT, 30);
        String[] devices = new String[]{"TestClient"};
        if (args.length > 2) {
            try {
                String snStrs = String.valueOf(args[2]);
                devices = snStrs.split(",");
            } catch (Exception e) {
                System.out.println("sn选项错误\n正确示例:设备sn以逗号分割\nJ102D03D00103,L220190408002");
            }
        }
        for (String sn : devices) {
            System.out.println("设备:"+sn);
            clientManager.addConnect(sn);
        }
        checkState();
    }

    public static void checkState() {
        clientManager.eventExecutors.schedule(new Runnable() {
            @Override
            public void run() {
                int count = clientManager.getConnectCount();
                if (count < CONNECTION_COUNT) {
                    System.out.println("等待客户端连接完成:" + count);
                    for (MqttClient mqttClient : clientManager.mqttClientList) {
                        if (mqttClient.state == null || mqttClient.state != MqttClient.ClientState.MQTT_CONNECTED)
                            mqttClient.connect();
                    }
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
