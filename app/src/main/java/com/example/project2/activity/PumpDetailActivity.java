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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

public class PumpDetailActivity extends AppCompatActivity implements MqttCallbackListener {

    public static final String EXTRA_PUMP_STATUS = "EXTRA_PUMP_STATUS";
    private TextView textViewPumpMode;
    private TextView textViewPumpState;
    private TextView textViewLastUpdated;
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

        textViewPumpMode = findViewById(R.id.textViewPumpMode);
        textViewPumpState = findViewById(R.id.textViewPumpState);
        textViewLastUpdated = findViewById(R.id.textViewLastUpdated);
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
        } else {
            updatePumpStatusUI(null);
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
        if (status == null) {
            textViewPumpMode.setText("Chế độ: Đang cập nhật...");
            textViewPumpState.setText("Trạng thái: Đang cập nhật...");
            textViewPumpState.setTextColor(Color.GRAY);
            if (textViewLastUpdated != null) textViewLastUpdated.setText("");
            return;
        }

        String modeDisplay = "Chế độ: --";
        String stateDisplay = "Trạng thái: --";
        int stateColor = Color.BLACK; // Màu mặc định
        boolean isAutoMode = false;

        String upperStatus = status.toUpperCase();
        
        // Xử lý các trường hợp đặc biệt không theo định dạng MODE:STATE
        if (upperStatus.contains("DA BAT CHE DO TU DONG")) {
            modeDisplay = "Chế độ: Tự động";
            stateDisplay = "Trạng thái: Đã kích hoạt";
            stateColor = Color.parseColor("#2196F3"); // Xanh dương
            isAutoMode = true;
        } else if (upperStatus.contains("HE THONG ONLINE")) {
            stateDisplay = "Trạng thái: Hệ thống Online";
            stateColor = Color.parseColor("#2196F3");
            isAutoMode = true; // Mặc định ESP khởi động là Auto
        } else {
            // Xử lý định dạng chuẩn MODE:STATE
            String[] parts = upperStatus.split(":");
            String mode = "";
            String state = "";

            if (parts.length > 0) mode = parts[0].trim();
            if (parts.length > 1) state = parts[1].trim();
            else state = upperStatus;

            // 1. Xác định chuỗi hiển thị cho Chế độ
            if (mode.equals("AUTO")) {
                modeDisplay = "Chế độ: Tự động";
                isAutoMode = true;
            } else if (mode.equals("THU CONG") || mode.equals("MANUAL")) {
                modeDisplay = "Chế độ: Thủ công";
                isAutoMode = false;
            }

            // 2. Xác định chuỗi hiển thị và màu sắc cho Trạng thái
            if (state.contains("DANG TUOI")) {
                stateDisplay = "Trạng thái: Đang tưới";
                stateColor = Color.parseColor("#4CAF50"); // Xanh lá
            } else if (state.contains("DANG BAT")) {
                stateDisplay = "Trạng thái: Đang bật";
                stateColor = Color.parseColor("#4CAF50");
            } else if (state.contains("DA TAT")) {
                stateDisplay = "Trạng thái: Đã tắt";
                stateColor = Color.parseColor("#F44336"); // Đỏ
            } else if (state.contains("DAT DU AM")) {
                stateDisplay = "Trạng thái: Đất đủ ẩm (Tắt)";
                stateColor = Color.parseColor("#F44336");
            } else if (state.contains("MUA") || state.contains("KHONG TUOI")) {
                stateDisplay = "Trạng thái: Trời mưa (Tắt)";
                stateColor = Color.parseColor("#F44336");
            } else {
                stateDisplay = "Trạng thái: " + (parts.length > 1 ? parts[1].trim() : status);
            }
        }

        boolean isPumpOn = upperStatus.contains("DANG BAT") || upperStatus.contains("DANG TUOI");

        // 3. Cập nhật giao diện
        textViewPumpMode.setText(modeDisplay);
        textViewPumpState.setText(stateDisplay);
        textViewPumpState.setTextColor(stateColor);

        // Cập nhật thời gian
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault());
        textViewLastUpdated.setText("Cập nhật: " + sdf.format(new Date()));

        // 4. Cập nhật các công tắc (Switch)
        if (switchAuto != null && switchManual != null) {
            isProgrammaticChange = true;
            switchAuto.setChecked(isAutoMode);
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
