package com.example.project2.mqtt;

import android.content.Context;
import android.util.Log;
import android.content.SharedPreferences;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5Connect;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscribe;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class MqttHandler {

    private static final String TAG = "MqttHandler";

    // --- CẤU HÌNH MQTT ---

    // --- Cấu hình tạm thời cho broker.hivemq.com (công khai, không mã hóa) ---
    /* private static final String MQTT_SERVER_HOST = "broker.hivemq.com";
    private static final int MQTT_SERVER_PORT = 1883;
    private static final String MQTT_USER = "";
    private static final String MQTT_PASS = ""; */

    // --- Cấu hình cho HiveMQ Cloud (để dùng sau) ---
    private static final String MQTT_SERVER_HOST = "1fbec7bb18444301835d103a20af6aea.s1.eu.hivemq.cloud";
    private static final int MQTT_SERVER_PORT = 8883;
    private static final String MQTT_USER = "Project2";
    private static final String MQTT_PASS = "LinhLongKhiemIT2";
    private static final String CLIENT_ID_PREFIX = "AndroidClient_";

    // --- CHỦ ĐỀ MQTT (TOPIC) ---
    public static final String TOPIC_TEMPERATURE = "vuon/nhiet_do";
    public static final String TOPIC_AIR_HUMIDITY = "vuon/do_am_kk";
    public static final String TOPIC_SOIL_MOISTURE = "vuon/do_am";
    public static final String TOPIC_PUMP_COMMAND = "vuon/may_bom/lenh";
    public static final String TOPIC_PUMP_STATUS = "vuon/may_bom"; // Nhận trạng thái từ ESP

    private Mqtt5AsyncClient client;
    private final MqttCallbackListener listener;
    private final Context context;

    public MqttHandler(Context context, MqttCallbackListener listener) {
        this.context = context;
        this.listener = listener;
        // Client được xây dựng và kết nối trong phương thức connect()
    }

    private String getOrCreateClientId(Context context) {
        // Sử dụng SharedPreferences để lưu và lấy lại client ID ổn định
        SharedPreferences prefs = context.getSharedPreferences("mqtt_client_prefs", Context.MODE_PRIVATE);
        String clientId = prefs.getString("client_id", null);
        if (clientId == null) {
            // Tạo client ID mới nếu chưa tồn tại và lưu lại
            clientId = CLIENT_ID_PREFIX + System.currentTimeMillis();
            prefs.edit().putString("client_id", clientId).apply();
        }
        return clientId;
    }

    public void connect() {
        connect(null);
    }

    public void connect(String customClientId) {
        if (client != null && client.getState().isConnectedOrReconnect()) {
            Log.d(TAG, "Client đã kết nối hoặc đang kết nối lại.");
            return;
        }

        String clientId = (customClientId != null) ? customClientId : getOrCreateClientId(context);

        client = MqttClient.builder()
                .useMqttVersion5()
                .identifier(clientId)
                .serverHost(MQTT_SERVER_HOST)
                .serverPort(MQTT_SERVER_PORT)
                .sslWithDefaultConfig() // Bỏ SSL khi dùng broker công khai trên port 1883
                .automaticReconnect()
                    .initialDelay(500, TimeUnit.MILLISECONDS)
                    .maxDelay(1, TimeUnit.MINUTES)
                    .applyAutomaticReconnect()
                .addConnectedListener(context -> {
                    Log.d(TAG, "MQTT Connected");
                    listener.onConnectionStatusChanged(true, "Đã kết nối MQTT thành công");
                    subscribeToTopics();
                })
                .addDisconnectedListener(context -> {
                    String message = "Mất kết nối MQTT";
                    Log.e(TAG, message, context.getCause());
                    listener.onConnectionStatusChanged(false, "Mất kết nối MQTT");
                })
                .buildAsync();

        Mqtt5Connect connectMessage = Mqtt5Connect.builder()
                .cleanStart(false)
                .sessionExpiryInterval(3600) // Giữ session trong 1 giờ
                .simpleAuth()
                .username(MQTT_USER)
                .password(MQTT_PASS.getBytes(StandardCharsets.UTF_8))
                .applySimpleAuth()
                .build();

        client.connect(connectMessage)
                .whenComplete((connAck, throwable) -> {
                    if (throwable != null) {
                        String message = "Kết nối MQTT thất bại";
                        Log.e(TAG, "Kết nối MQTT thất bại", throwable);
                        listener.onConnectionStatusChanged(false, "Kết nối MQTT thất bại");
                    } else {
                        // Thành công sẽ được xử lý bởi addConnectedListener
                        Log.d(TAG, "MQTT Connection Process Started Successfully");
                    }
                });
    }

    public void disconnect() {
        if (client != null && client.getState().isConnected()) {
            client.disconnect().whenComplete((v, throwable) -> {
                if (throwable != null) {
                    Log.e(TAG, "Lỗi khi ngắt kết nối MQTT", throwable);
                } else {
                    Log.d(TAG, "Đã ngắt kết nối MQTT");
                }
            });
        }
    }

    private void subscribeToTopics() {
        if (client == null) return;

        Mqtt5Subscribe subscribeMessage = Mqtt5Subscribe.builder()
                .addSubscription().topicFilter(TOPIC_SOIL_MOISTURE).qos(MqttQos.AT_LEAST_ONCE).applySubscription()
                .addSubscription().topicFilter(TOPIC_TEMPERATURE).qos(MqttQos.AT_LEAST_ONCE).applySubscription()
                .addSubscription().topicFilter(TOPIC_AIR_HUMIDITY).qos(MqttQos.AT_LEAST_ONCE).applySubscription()
                .addSubscription().topicFilter(TOPIC_PUMP_STATUS).qos(MqttQos.AT_LEAST_ONCE).applySubscription()
                .build();

        client.subscribe(subscribeMessage, publish -> {
            String topic = publish.getTopic().toString();
            String payload = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);
            Log.d(TAG, "Tin nhắn đến từ topic: " + topic + " | Payload: " + payload);
            listener.onMessageArrived(topic, payload);
        }).whenComplete((subAck, throwable) -> {
            if (throwable != null) {
                Log.e(TAG, "Không thể đăng ký các topic", throwable);
            } else {
                Log.d(TAG, "Subscribed to topics successfully");
            }
        });
    }

    public void publishMessage(String topic, String payload) {
        if (client == null || !client.getState().isConnected()) {
            Log.e(TAG, "Không thể gửi tin nhắn, client MQTT chưa kết nối");
            return;
        }

        Mqtt5Publish publishMessage = Mqtt5Publish.builder()
                .topic(topic)
                .payload(payload.getBytes(StandardCharsets.UTF_8))
                .qos(MqttQos.AT_LEAST_ONCE)
                .build();

        client.publish(publishMessage).whenComplete((publishResult, throwable) -> {
            if (throwable != null) { // Sửa lỗi chính tả từ "publis" thành "publish"
                Log.e(TAG, "Không thể gửi tin nhắn đến " + topic, throwable);
            } else {
                Log.d(TAG, "Đã gửi tin nhắn đến " + topic);
            }
        });
    }
}
