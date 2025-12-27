package com.example.project2.activity;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.project2.R;
import com.example.project2.mqtt.MqttCallbackListener;
import com.example.project2.mqtt.MqttHandler;
import com.example.project2.model.PumpDataRepository;
import com.example.project2.db.PumpHistoryEntry;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.util.ArrayList;
import java.util.List;

public class PumpDetailActivity extends AppCompatActivity implements MqttCallbackListener {

    public static final String EXTRA_PUMP_STATUS = "EXTRA_PUMP_STATUS";
    private TextView textViewPumpStatus;
    private static final String TOPIC_COMMAND = "vuon/may_bom/lenh";
    private static final String TOPIC_STATUS = "vuon/may_bom";
    private MqttHandler mqttHandler;
    private CardView cvStatusDot;
    private TextView tvConnectionStatus;
    private SwitchCompat switchAuto;
    private SwitchCompat switchManual;
    private PieChart pieChart;
    private boolean isProgrammaticChange = false;

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
        cvStatusDot = findViewById(R.id.cvStatusDot);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        pieChart = findViewById(R.id.pieChartPump);
        switchAuto = findViewById(R.id.switchAuto);
        switchManual = findViewById(R.id.switchManual);

        // 1. Nhận dữ liệu trạng thái từ MainActivity gửi sang
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_PUMP_STATUS)) {
            String status = intent.getStringExtra(EXTRA_PUMP_STATUS);
            updatePumpStatusUI(status);
        }

        // 2. Xử lý sự kiện click nút
        switchAuto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isProgrammaticChange) return; // Bỏ qua nếu thay đổi do code cập nhật

            if (isChecked) {
                sendCommand("AUTO");
            } else {
                sendCommand("MANUAL"); // Gửi lệnh chuyển về thủ công
            }
        });

        switchManual.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isProgrammaticChange) return; // Bỏ qua nếu thay đổi do code cập nhật
            if (isChecked) {
                sendCommand("ON");
            } else {
                sendCommand("OFF");
            }
        });

        // Khởi tạo MQTT Handler riêng cho màn hình chi tiết
        mqttHandler = new MqttHandler(getApplicationContext(), this);
        // Sử dụng Client ID riêng để tránh xung đột với MainActivity
        mqttHandler.connect("PumpDetail_" + System.currentTimeMillis());

        // Lắng nghe dữ liệu lịch sử để vẽ biểu đồ
        PumpDataRepository.getInstance(getApplication()).getPumpHistory().observe(this, history -> {
            if (history != null) {
                updatePieChart(history);
            }
        });
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

        // Cập nhật trạng thái Switch dựa trên tin nhắn (VD: "AUTO: DANG TUOI" -> Bật switch)
        if (switchAuto != null && switchManual != null) {
            isProgrammaticChange = true;
            switchAuto.setChecked(status.toUpperCase().contains("AUTO"));
            // Cập nhật switch thủ công dựa trên trạng thái thực tế của bơm
            switchManual.setChecked(isPumpOn);
            isProgrammaticChange = false;
        }
    }

    private void updatePieChart(List<PumpHistoryEntry> history) {
        long totalOnTime = 0;
        long totalOffTime = 0;
        long now = System.currentTimeMillis();
        long startOfPeriod = now - 24 * 60 * 60 * 1000L; // 24 giờ qua

        // Tìm trạng thái ban đầu tại thời điểm startOfPeriod
        int currentState = 0; // Mặc định tắt
        long lastTime = startOfPeriod;

        // Lọc và tính toán
        for (PumpHistoryEntry entry : history) {
            if (entry.timestamp < startOfPeriod) {
                currentState = entry.status; // Cập nhật trạng thái trước mốc thời gian
            } else {
                // Tính thời gian cho trạng thái trước đó
                long duration = entry.timestamp - lastTime;
                if (currentState == 1) {
                    totalOnTime += duration;
                } else {
                    totalOffTime += duration;
                }
                // Cập nhật trạng thái mới
                currentState = entry.status;
                lastTime = entry.timestamp;
            }
        }

        // Tính đoạn thời gian cuối cùng từ entry cuối đến hiện tại
        long finalDuration = now - lastTime;
        if (currentState == 1) {
            totalOnTime += finalDuration;
        } else {
            totalOffTime += finalDuration;
        }

        // Tạo dữ liệu cho biểu đồ
        ArrayList<PieEntry> entries = new ArrayList<>();
        // Chuyển đổi sang phút hoặc giờ để dễ nhìn, ở đây dùng phút
        entries.add(new PieEntry((float) totalOnTime / (1000 * 60), "Bật (phút)"));
        entries.add(new PieEntry((float) totalOffTime / (1000 * 60), "Tắt (phút)"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(Color.parseColor("#4CAF50"), Color.parseColor("#E0E0E0")); // Xanh, Xám
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.BLACK);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText("24h Qua");
        pieChart.setCenterTextSize(16f);
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.invalidate(); // Vẽ lại biểu đồ
    }

    @Override
    public void onConnectionStatusChanged(boolean connected, String message) {
        runOnUiThread(() -> {
            if (connected) {
                cvStatusDot.setCardBackgroundColor(Color.parseColor("#4CAF50")); // Màu xanh (Online)
                tvConnectionStatus.setText(R.string.status_online);
            } else {
                cvStatusDot.setCardBackgroundColor(Color.parseColor("#9E9E9E")); // Màu xám (Offline)
                tvConnectionStatus.setText(R.string.status_offline);
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
