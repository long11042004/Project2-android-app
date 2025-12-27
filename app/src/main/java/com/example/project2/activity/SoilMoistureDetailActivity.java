package com.example.project2.activity;

import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.project2.model.MoistureDataRepository;
import com.example.project2.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.List;

public class SoilMoistureDetailActivity extends AppCompatActivity {

    public static final String EXTRA_MOISTURE_VALUE = "EXTRA_MOISTURE_VALUE";

    private CircularProgressIndicator progressIndicator;
    private TextView textViewMoistureValue;
    private LineChart historyChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_soil_moisture_detail);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Chi tiết Độ ẩm Đất");
            // Đặt màu nền cho ActionBar để đồng bộ với theme màu nâu
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.brown)));
        }

        progressIndicator = findViewById(R.id.progressIndicatorSoilMoisture);
        textViewMoistureValue = findViewById(R.id.textViewCurrentMoistureDetail);
        historyChart = findViewById(R.id.lineChartSoilMoistureHistory);

        setupChart();

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_MOISTURE_VALUE)) {
            try {
                String moistureString = intent.getStringExtra(EXTRA_MOISTURE_VALUE);
                updateGauge(Float.parseFloat(moistureString));
            } catch (NumberFormatException e) {
                // Xử lý trường hợp giá trị không hợp lệ
                updateGauge(null);
            }
        } else {
            updateGauge(null);
        }

        // Lắng nghe các thay đổi dữ liệu độ ẩm từ Repository
        MoistureDataRepository.getInstance(getApplication()).getSoilMoisture().observe(this, moistureString -> {
            if (moistureString != null) {
                try {
                    float moistureValue = Float.parseFloat(moistureString);
                    updateGauge(moistureValue);
                } catch (NumberFormatException e) {
                    // Bỏ qua nếu giá trị không hợp lệ, hoặc có thể log lỗi
                }
            }
        });

        // Lắng nghe toàn bộ lịch sử (bao gồm cả cập nhật mới) để vẽ biểu đồ
        MoistureDataRepository.getInstance(getApplication()).getFullHistory().observe(this, historyEntries -> {
            if (historyEntries != null) {
                ArrayList<Entry> chartEntries = new ArrayList<>();
                for (com.example.project2.db.MoistureHistoryEntry dbEntry : historyEntries) {
                    chartEntries.add(new Entry(dbEntry.timestamp, dbEntry.moistureValue));
                }
                updateChart(chartEntries);
            }
        });
    }

    private void updateGauge(Float moistureValue) {
        if (moistureValue != null) {
            int moistureInt = moistureValue.intValue();
            progressIndicator.setProgress(moistureInt, true);
            textViewMoistureValue.setText(getString(R.string.soil_moisture_format, moistureInt));
        } else {
            progressIndicator.setProgress(0, true);
            textViewMoistureValue.setText(R.string.soil_moisture_default);
        }
    }

    private void setupChart() {
        historyChart.getDescription().setEnabled(false);
        historyChart.setTouchEnabled(true);
        historyChart.setDragEnabled(true);
        historyChart.setScaleEnabled(true);
        historyChart.setPinchZoom(true);
        historyChart.setDrawGridBackground(false);

        XAxis xAxis = historyChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawLabels(true); // Bật hiển thị nhãn cho trục X
        xAxis.setGranularity(1000f * 30); // Khoảng cách tối thiểu giữa các nhãn (30 giây)

        // Định dạng giá trị trục X từ timestamp (float) thành chuỗi giờ:phút:giây
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat mFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            @Override
            public String getAxisLabel(float value, com.github.mikephil.charting.components.AxisBase axis) {
                return mFormat.format(new Date((long) value));
            }
        });

        YAxis leftAxis = historyChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(100f);

        historyChart.getAxisRight().setEnabled(false);
        historyChart.getLegend().setEnabled(false);
        historyChart.setData(new LineData()); // Khởi tạo với dữ liệu trống
    }

    private void updateChart(ArrayList<Entry> chartEntries) {
        LineData data = historyChart.getData();
        ILineDataSet set = data.getDataSetByIndex(0);

        if (set == null) {
            set = createSet();
            data.addDataSet(set);
        }

        // Cập nhật dữ liệu cho DataSet
        ((LineDataSet) set).setValues(chartEntries);

        // Thông báo cho biểu đồ về sự thay đổi
        data.notifyDataChanged();
        historyChart.notifyDataSetChanged();

        // Tự động cuộn đến điểm dữ liệu mới nhất
        historyChart.moveViewToX(data.getEntryCount());
    }

    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "Lịch sử độ ẩm");
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawValues(false);
        set.setDrawCircleHole(false);
        set.setLineWidth(2f);
        set.setDrawCircles(false); // Tắt vẽ điểm tròn để biểu đồ mượt hơn khi có nhiều dữ liệu
        
        // --- TÙY CHỈNH MÀU SẮC BIỂU ĐỒ ---
        int brownColor = ContextCompat.getColor(this, R.color.brown);
        set.setColor(brownColor);
        set.setCircleColor(brownColor);
        set.setDrawFilled(true);
        int[] gradientColors = {
                ContextCompat.getColor(this, R.color.brown_light), // Màu nâu nhạt (Tan) cho vùng ẩm (giá trị cao, ở trên)
                ContextCompat.getColor(this, R.color.brown)    // Màu nâu sẫm (SaddleBrown) cho vùng khô (giá trị thấp, ở dưới)
        };
        Drawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, gradientColors);
        set.setFillDrawable(gradient);
        return set;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
