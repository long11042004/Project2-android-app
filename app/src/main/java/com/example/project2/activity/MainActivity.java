package com.example.project2.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.graphics.Color;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.example.project2.R;
import com.example.project2.mqtt.MqttCallbackListener;
import com.example.project2.model.*;
import com.example.project2.mqtt.MqttHandler;

public class MainActivity extends AppCompatActivity implements MqttCallbackListener {

    private MqttHandler mqttHandler;

    private TextView textViewTemperature;
    private TextView textViewAirHumidity;
    private TextView textViewSoilMoisture;
    private TextView textViewPumpStatus;
    private Button btnDetailTemperature;
    private Button btnDetailHumidity;
    private Button btnDetailSoil;
    private Button btnDetailPump;
    private String currentTemperatureValue = null;
    private CardView cvPumpStatusDot;
    private String currentAirHumidityValue = null;
    private String currentSoilMoistureValue = null; // Lưu giá trị độ ẩm thô
    private String currentPumpStatus = null;

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
        btnDetailTemperature = findViewById(R.id.btnDetailTemperature);
        btnDetailHumidity = findViewById(R.id.btnDetailHumidity);
        btnDetailSoil = findViewById(R.id.btnDetailSoil);
        btnDetailPump = findViewById(R.id.btnDetailPump);
        cvPumpStatusDot = findViewById(R.id.cvPumpStatusDot);
        // Khởi tạo và kết nối MQTT
        mqttHandler = new MqttHandler(getApplicationContext(), this);
        mqttHandler.connect();
        // Xử lý sự kiện nhấn vào nút chi tiết nhiệt độ
        btnDetailTemperature.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TemperatureDetailActivity.class);
            if (currentTemperatureValue != null) {
                intent.putExtra(TemperatureDetailActivity.EXTRA_TEMPERATURE_VALUE, currentTemperatureValue);
            }
            startActivity(intent);
        });

        // Xử lý sự kiện nhấn vào nút chi tiết độ ẩm không khí
        btnDetailHumidity.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AirHumidityDetailActivity.class);
            if (currentAirHumidityValue != null) {
                intent.putExtra(AirHumidityDetailActivity.EXTRA_AIR_HUMIDITY_VALUE, currentAirHumidityValue);
            }
            startActivity(intent);
        });

        // Xử lý sự kiện nhấn vào nút chi tiết độ ẩm đất
        btnDetailSoil.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SoilMoistureDetailActivity.class);
            if (currentSoilMoistureValue != null) {
                intent.putExtra(SoilMoistureDetailActivity.EXTRA_MOISTURE_VALUE, currentSoilMoistureValue);
            }
            startActivity(intent);
        });

        // Xử lý sự kiện nhấn vào nút chi tiết máy bơm
        btnDetailPump.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PumpDetailActivity.class);
            if (currentPumpStatus != null) {
                intent.putExtra("pump_status", currentPumpStatus);
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
                    String formattedText = String.format("%.1f°C", tempValue);
                    textViewTemperature.setText(formattedText);

                    // Cập nhật dữ liệu vào Repository để vẽ biểu đồ
                    TemperatureDataRepository.getInstance(getApplication()).updateTemperature(currentTemperatureValue);
                } catch (NumberFormatException e) {
                    Log.e("MainActivity", "Invalid temperature value received: " + message, e);
                    currentTemperatureValue = null;
                    textViewTemperature.setText(R.string.temperature_default);
                }
            } else if (topic.equals(MqttHandler.TOPIC_AIR_HUMIDITY)) {
                try {
                    currentAirHumidityValue = message;
                    float humidityValue = Float.parseFloat(message);
                    String formattedText = String.format("%.1f%%", humidityValue);
                    textViewAirHumidity.setText(formattedText);

                    // Cập nhật dữ liệu vào Repository để vẽ biểu đồ
                    HumidityDataRepository.getInstance(getApplication()).updateAirHumidity(currentAirHumidityValue);
                } catch (NumberFormatException e) {
                    Log.e("MainActivity", "Invalid air humidity value received: " + message, e);
                    currentAirHumidityValue = null;
                    textViewAirHumidity.setText(R.string.air_humidity_default);
                }
            } else if (topic.equals(MqttHandler.TOPIC_SOIL_MOISTURE)) {
                try {
                    currentSoilMoistureValue = message; // Lưu giá trị thô
                    int moistureValue = Integer.parseInt(message);
                    String formattedText = String.format("%d%%", moistureValue);
                    textViewSoilMoisture.setText(formattedText);

                    // Cập nhật dữ liệu trong Repository để các thành phần khác có thể lắng nghe
                    MoistureDataRepository.getInstance(getApplication()).updateSoilMoisture(currentSoilMoistureValue);
                } catch (NumberFormatException e) {
                    Log.e("MainActivity", "Invalid soil moisture value received: " + message, e);
                    currentSoilMoistureValue = null;
                    textViewSoilMoisture.setText(R.string.soil_moisture_default);
                }
            } else if (topic.equals(MqttHandler.TOPIC_PUMP_STATUS)) {
                currentPumpStatus = message; // Lưu lại trạng thái mới nhất
                String upperStatus = message.toUpperCase();
                boolean isPumpOn = upperStatus.contains("DANG BAT") || upperStatus.contains("DANG TUOI");

                String stateDisplay;
                int dotColor;

                if (isPumpOn) {
                    stateDisplay = "Đang Bật";
                    dotColor = ContextCompat.getColor(this, R.color.mint);
                } else {
                    stateDisplay = "Đã Tắt";
                    dotColor = ContextCompat.getColor(this, R.color.temperature_red);
                }
                textViewPumpStatus.setText(stateDisplay);
                cvPumpStatusDot.setCardBackgroundColor(dotColor);

                // Lưu lịch sử trạng thái máy bơm
                PumpDataRepository.getInstance(getApplication()).updatePumpStatus(message);

            }
        });
    }

    @Override
    public void onConnectionStatusChanged(boolean connected, String statusMessage) {
        // Hiển thị trạng thái kết nối cho người dùng
        runOnUiThread(() -> {
            Toast.makeText(this, statusMessage, Toast.LENGTH_SHORT).show();
            if (!connected) {
                // Khi mất kết nối, đặt lại trạng thái là "chưa rõ" cho tất cả các cảm biến
                textViewTemperature.setText(R.string.temperature_default);
                textViewAirHumidity.setText(R.string.air_humidity_default);
                textViewSoilMoisture.setText(R.string.soil_moisture_default);
                textViewPumpStatus.setText(R.string.pump_status_unknown);
                if (cvPumpStatusDot != null) {
                    cvPumpStatusDot.setCardBackgroundColor(Color.parseColor("#9E9E9E")); // Màu xám
                }
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
