package com.example.project2.activity;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.project2.R;
import com.example.project2.mqtt.MqttCallbackListener;
import com.example.project2.mqtt.MqttHandler;

public class PumpDetailActivity extends AppCompatActivity implements MqttCallbackListener {

    public static final String EXTRA_PUMP_STATUS = "EXTRA_PUMP_STATUS";
    private TextView textViewPumpStatus;
    private static final String TOPIC_COMMAND = "vuon/may_bom/lenh";
    private static final String TOPIC_STATUS = "vuon/may_bom";
    private MqttHandler mqttHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pump_detail);

        // Cấu hình ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Điều khiển Máy bơm");
            // Màu xanh Teal cho máy bơm
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFF009688));
        }

        textViewPumpStatus = findViewById(R.id.textViewPumpStatusDetail);
        Button btnAuto = findViewById(R.id.buttonAuto);
        Button btnOn = findViewById(R.id.buttonOn);
        Button btnOff = findViewById(R.id.buttonOff);

        // 1. Nhận dữ liệu trạng thái từ MainActivity gửi sang
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_PUMP_STATUS)) {
            String status = intent.getStringExtra(EXTRA_PUMP_STATUS);
            updatePumpStatusUI(status);
        }

        // 2. Xử lý sự kiện click nút
        btnAuto.setOnClickListener(v -> sendCommand("AUTO"));
        btnOn.setOnClickListener(v -> sendCommand("ON"));
        btnOff.setOnClickListener(v -> sendCommand("OFF"));

        // Khởi tạo MQTT Handler riêng cho màn hình chi tiết
        mqttHandler = new MqttHandler(getApplicationContext(), this);
        mqttHandler.connect();
    }

    private void sendCommand(String command) {
        if (mqttHandler != null) {
            mqttHandler.publishMessage(TOPIC_COMMAND, command);
        }

        // Hiện tại chỉ hiển thị thông báo mô phỏng
        Toast.makeText(this, "Đang gửi lệnh: " + command, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMessageArrived(String topic, String message) {
        // Cập nhật giao diện khi nhận được phản hồi từ ESP8266
        runOnUiThread(() -> {
            if (topic.equals(TOPIC_STATUS)) {
                updatePumpStatusUI(message);
            }
        });
    }

    private void updatePumpStatusUI(String status) {
        if (status == null) return;
        textViewPumpStatus.setText(status);

        // Kiểm tra trạng thái để đổi màu chữ (Dựa trên logic esp8266.cpp: "DANG BAT" hoặc "DANG TUOI")
        boolean isPumpOn = status.contains("DANG BAT") || status.contains("DANG TUOI");
        if (isPumpOn) {
            textViewPumpStatus.setTextColor(Color.parseColor("#4CAF50")); // Màu xanh (Green)
        } else {
            textViewPumpStatus.setTextColor(Color.parseColor("#F44336")); // Màu đỏ (Red)
        }
    }

    @Override
    public void onConnectionStatusChanged(boolean connected, String message) {
        runOnUiThread(() -> {
            if (!connected) {
                Toast.makeText(this, "Mất kết nối MQTT", Toast.LENGTH_SHORT).show();
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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Xử lý nút Back trên ActionBar
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
