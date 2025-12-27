package com.example.project2.activity;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.project2.R;

public class PumpDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PUMP_STATUS = "EXTRA_PUMP_STATUS";
    private TextView textViewPumpStatus;
    private static final String TOPIC_COMMAND = "vuon/may_bom/lenh";

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
            textViewPumpStatus.setText(status);
        }

        // 2. Xử lý sự kiện click nút
        btnAuto.setOnClickListener(v -> sendCommand("AUTO"));
        btnOn.setOnClickListener(v -> sendCommand("ON"));
        btnOff.setOnClickListener(v -> sendCommand("OFF"));
    }

    private void sendCommand(String command) {
        // TODO: Tích hợp MqttHandler của bạn ở đây để gửi lệnh
        // Ví dụ:
        // MqttHandler mqttHandler = new MqttHandler();
        // mqttHandler.connect( ... ); // Nếu cần kết nối lại
        // mqttHandler.publish(TOPIC_COMMAND, command);

        // Hiện tại chỉ hiển thị thông báo mô phỏng
        Toast.makeText(this, "Đang gửi lệnh: " + command, Toast.LENGTH_SHORT).show();

        // Cập nhật tạm thời giao diện (ESP8266 sẽ gửi xác nhận sau)
        if (command.equals("AUTO")) {
            textViewPumpStatus.setText("Đang chuyển sang TỰ ĐỘNG...");
        } else if (command.equals("ON")) {
            textViewPumpStatus.setText("Đang BẬT máy bơm...");
        } else {
            textViewPumpStatus.setText("Đang TẮT máy bơm...");
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
