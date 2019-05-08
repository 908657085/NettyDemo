package com.gaoxh.nettystudy.mqtt;

import java.util.UUID;

public class MqttTestCase1 {
    public static void main(String[] args) {
        int connectionCount = 1000;
        long publishDelay = 200;
        try {
            connectionCount = Integer.parseInt(args[0]);
            publishDelay = Long.parseLong(args[1]);
        } catch (Exception e) {

        }
        MqttClientManager mqttClientManager = new MqttClientManager("101.132.38.194", 1883, 30);
        connectTest(mqttClientManager, connectionCount);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        publishTest(mqttClientManager, publishDelay);

    }

    public static void connectTest(MqttClientManager clientManager, int connectCount) {
        for (int i = 0; i < connectCount; i++) {
            clientManager.addConnect();
        }
    }

    public static void publishTest(MqttClientManager clientManager, long delay) {
        int count = 0;
        while (true) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            clientManager.dumpStates();
            int publishMsgCount = 0;
            int reconnectCount = 0;
            int waitCount = 0;
            System.out.println("publish msg start");
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < clientManager.mqttClientList.size(); i++) {
                MqttClient mqttClient = clientManager.mqttClientList.get(i);
                switch (mqttClient.state) {
                    case EXCEPTION:
                    case NONE:
                        mqttClient.close();
                        mqttClient.connect();
                        reconnectCount++;
                        break;
                    case MQTT_CONNECTED:
                        mqttClient.publish(UUID.randomUUID().toString());
                        publishMsgCount++;
                        break;
                    default:
                        //not ready
                        waitCount++;
                        break;
                }
            }
            System.out.println("publish msg finish " +(System.currentTimeMillis() - startTime) +  " ms  \r\n"
                    + "waitCount: " + waitCount + "\r\n"
                    + "reconnectCount: " + reconnectCount + "\r\n"
                    + "publishMsgCount: " + publishMsgCount + "\r\n"
            );

            count++;
        }
    }
}
