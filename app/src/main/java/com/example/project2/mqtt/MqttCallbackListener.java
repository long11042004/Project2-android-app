package com.example.project2.mqtt;

public interface MqttCallbackListener {
    void onMessageArrived(String topic, String message);
    void onConnectionStatusChanged(boolean connected, String statusMessage);
}
