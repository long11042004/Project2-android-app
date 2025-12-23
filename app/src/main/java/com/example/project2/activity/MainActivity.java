package com.example.project2.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;

import com.example.project2.R;
import com.example.project2.mqtt.MqttCallbackListener;
import com.example.project2.model.MoistureDataRepository;
import com.example.project2.mqtt.MqttHandler;

public class MainActivity extends AppCompatActivity implements MqttCallbackListener {

    private MqttHandler mqttHandler;

    private TextView textViewTemperature;
    private TextView textViewAirHumidity;
    private TextView textViewSoilMoisture;
    private TextView textViewPumpStatus;
    private SwitchCompat switchPump;
    private boolean isProgrammaticChange = false; // Cờ để tránh vòng lặp khi cập nhật switch
    private String currentTemperatureValue = null;
    private String currentAirHumidityValue = null;
    private String currentSoilMoistureValue = null; // Lưu giá trị độ ẩm thô

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Đặt tiêu đề cho ActionBar, sử dụng chuỗi "Bảng điều khiển Tưới cây"
        setTitle(R.string.dashboard_title);

        // Ánh xạ các thành phần
        textViewTemperature = findViewById(R.id.textViewTemperature);
        textViewAirHumidity = findViewById(R.id.textViewHumidity);
        textViewSoilMoisture = findViewById(R.id.textViewSoilMoisture);
        textViewPumpStatus = findViewById(R.id.textViewPumpStatus);
        switchPump = findViewById(R.id.switchPump);
        CardView cardViewTemperature = findViewById(R.id.cardViewTemperature);
        CardView cardViewAirHumidity = findViewById(R.id.cardViewAirHumidity);
        CardView cardViewSoilMoisture = findViewById(R.id.cardViewSoilMoisture);

        // Vô hiệu hóa switch máy bơm khi khởi động cho đến khi nhận được trạng thái đầu tiên
        switchPump.setEnabled(false);

        // Khởi tạo và kết nối MQTT
        mqttHandler = new MqttHandler(getApplicationContext(), this);
        mqttHandler.connect();

        // Xử lý sự kiện bật/tắt máy bơm
        switchPump.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isProgrammaticChange) {
                return; // Bỏ qua nếu thay đổi đến từ tin nhắn MQTT
            }
            if (isChecked) {
                mqttHandler.publishMessage(MqttHandler.TOPIC_PUMP_COMMAND, "ON");
                Toast.makeText(this, getString(R.string.pump_turned_on), Toast.LENGTH_SHORT).show();
            } else {
                mqttHandler.publishMessage(MqttHandler.TOPIC_PUMP_COMMAND, "OFF");
                Toast.makeText(this, getString(R.string.pump_turned_off), Toast.LENGTH_SHORT).show();
            }
        });

        // Xử lý sự kiện nhấn vào ô nhiệt độ
        cardViewTemperature.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TemperatureDetailActivity.class);
            if (currentTemperatureValue != null) {
                intent.putExtra(TemperatureDetailActivity.EXTRA_TEMPERATURE_VALUE, currentTemperatureValue);
            }
            startActivity(intent);
        });

        // Xử lý sự kiện nhấn vào ô độ ẩm không khí
        cardViewAirHumidity.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AirHumidityDetailActivity.class);
            if (currentAirHumidityValue != null) {
                intent.putExtra(AirHumidityDetailActivity.EXTRA_AIR_HUMIDITY_VALUE, currentAirHumidityValue);
            }
            startActivity(intent);
        });

        // Xử lý sự kiện nhấn vào ô độ ẩm đất
        cardViewSoilMoisture.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SoilMoistureDetailActivity.class);
            if (currentSoilMoistureValue != null) {
                intent.putExtra(SoilMoistureDetailActivity.EXTRA_MOISTURE_VALUE, currentSoilMoistureValue);
            }
            startActivity(intent);
        });
    }

    @Override
    public void onMessageArrived(String topic, String message) {
        // Cập nhật giao diện trên luồng chính
        runOnUiThread(() -> {
            if (topic.equals(MqttHandler.TOPIC_TEMPERATURE)) {
                try {
                    currentTemperatureValue = message;
                    float tempValue = Float.parseFloat(message);
                    String formattedText = getString(R.string.temperature_format, tempValue);
                    textViewTemperature.setText(formattedText);
                } catch (NumberFormatException e) {
                    Log.e("MainActivity", "Invalid temperature value received: " + message, e);
                    currentTemperatureValue = null;
                    textViewTemperature.setText(R.string.temperature_default);
                }
            } else if (topic.equals(MqttHandler.TOPIC_AIR_HUMIDITY)) {
                try {
                    currentAirHumidityValue = message;
                    int humidityValue = Integer.parseInt(message);
                    String formattedText = getString(R.string.air_humidity_format, humidityValue);
                    textViewAirHumidity.setText(formattedText);
                } catch (NumberFormatException e) {
                    Log.e("MainActivity", "Invalid air humidity value received: " + message, e);
                    currentAirHumidityValue = null;
                    textViewAirHumidity.setText(R.string.air_humidity_default);
                }
            } else if (topic.equals(MqttHandler.TOPIC_SOIL_MOISTURE)) {
                try {
                    currentSoilMoistureValue = message; // Lưu giá trị thô
                    int moistureValue = Integer.parseInt(message);
                    String formattedText = getString(R.string.soil_moisture_format, moistureValue);
                    textViewSoilMoisture.setText(formattedText);

                    // Cập nhật dữ liệu trong Repository để các thành phần khác có thể lắng nghe
                    MoistureDataRepository.getInstance(getApplication()).updateSoilMoisture(currentSoilMoistureValue);
                } catch (NumberFormatException e) {
                    Log.e("MainActivity", "Invalid soil moisture value received: " + message, e);
                    currentSoilMoistureValue = null;
                    textViewSoilMoisture.setText(R.string.soil_moisture_default);
                }
            } else if (topic.equals(MqttHandler.TOPIC_PUMP_STATUS)) {
                // Dựa vào tin nhắn từ esp8266.cpp
                boolean isPumpOn = "BOM DANG CHAY".equalsIgnoreCase(message);

                // Cập nhật văn bản trạng thái
                if (isPumpOn) {
                    textViewPumpStatus.setText(R.string.pump_status_running);
                } else {
                    textViewPumpStatus.setText(R.string.pump_status_off);
                }

                // Kích hoạt switch khi đã nhận được trạng thái rõ ràng từ MQTT
                switchPump.setEnabled(true);

                // Đồng bộ trạng thái của Switch mà không kích hoạt lại listener
                isProgrammaticChange = true;
                switchPump.setChecked(isPumpOn);
                isProgrammaticChange = false;
            }
        });
    }

    @Override
    public void onConnectionStatusChanged(boolean connected, String statusMessage) {
        // Hiển thị trạng thái kết nối cho người dùng
        runOnUiThread(() -> {
            Toast.makeText(this, statusMessage, Toast.LENGTH_SHORT).show();
            if (!connected) {
                // Khi mất kết nối, vô hiệu hóa switch và đặt lại trạng thái là "chưa rõ"
                switchPump.setEnabled(false);
                textViewPumpStatus.setText(R.string.pump_status_unknown);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mqttHandler != null) {
            mqttHandler.disconnect();
        }
    }
}
