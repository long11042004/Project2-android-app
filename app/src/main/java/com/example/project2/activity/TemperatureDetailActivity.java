package com.example.project2.activity;

import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.project2.db.TemperatureHistoryEntry;
import com.example.project2.view.CustomMarkerView;
import com.example.project2.model.TemperatureDataRepository;
import com.example.project2.R;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Locale;

public class TemperatureDetailActivity extends AppCompatActivity implements OnChartValueSelectedListener {
    public static final String EXTRA_TEMPERATURE_VALUE = "EXTRA_TEMPERATURE_VALUE";
    private CircularProgressIndicator progressIndicator;
    private TextView textViewTemperatureValue;
    private ScatterChart historyChart;
    private TextView tvMax, tvMin, tvAvg;
    private List<com.example.project2.db.TemperatureHistoryEntry> fullHistory = new ArrayList<>();
    private long filterDuration = 24 * 60 * 60 * 1000L; // Mặc định 24 giờ

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temperature_detail);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Chi tiết Nhiệt độ");
            // Đặt màu nền cho ActionBar để đồng bộ với theme màu đỏ
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.temperature_red)));
        }

        progressIndicator = findViewById(R.id.progressIndicatorTemperature);
        textViewTemperatureValue = findViewById(R.id.textViewCurrentTemperatureDetail);
        historyChart = findViewById(R.id.barChartTemperatureHistory);
        tvMax = findViewById(R.id.tvMaxVal);
        tvMin = findViewById(R.id.tvMinVal);
        tvAvg = findViewById(R.id.tvAvgVal);

        Button btn1h = findViewById(R.id.btn1Hour);
        Button btn24h = findViewById(R.id.btn24Hours);

        btn1h.setOnClickListener(v -> {
            filterDuration = 1 * 60 * 60 * 1000L;
            updateChartWithFilter();
        });

        btn24h.setOnClickListener(v -> {
            filterDuration = 24 * 60 * 60 * 1000L;
            updateChartWithFilter();
        });

        setupChart();
        progressIndicator.setMax(50);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_TEMPERATURE_VALUE)) {
            try {
                String tempString = intent.getStringExtra(EXTRA_TEMPERATURE_VALUE);
                updateUI(Float.parseFloat(tempString));
            } catch (NumberFormatException e) {
                updateUI(null);
            }
        } else {
            updateUI(null);
        }

        // Lắng nghe dữ liệu thời gian thực từ TemperatureDataRepository
        TemperatureDataRepository.getInstance(getApplication()).getTemperature().observe(this, tempString -> {
            if (tempString != null) {
                try {
                    float tempValue = Float.parseFloat(tempString);
                    updateUI(tempValue);
                } catch (NumberFormatException e) {
                    // Bỏ qua lỗi
                }
            }
        });

        // Lắng nghe lịch sử dữ liệu để vẽ biểu đồ từ TemperatureDataRepository
        TemperatureDataRepository.getInstance(getApplication()).getTemperatureHistory().observe(this, historyEntries -> {
            if (historyEntries != null) {
                fullHistory = historyEntries;
                updateChartWithFilter();
            }
        });
    }

    private void updateChartWithFilter() {
        ArrayList<Entry> chartEntries = new ArrayList<>();
        long now = System.currentTimeMillis();
        long threshold = now - filterDuration;
        
        float maxVal = -Float.MAX_VALUE;
        float minVal = Float.MAX_VALUE;
        float sumVal = 0;
        int count = 0;

        for (int i = 0; i < fullHistory.size(); i++) {
            TemperatureHistoryEntry dbEntry = fullHistory.get(i);
            if (dbEntry.timestamp >= threshold) {
                float val = dbEntry.temperatureValue;
                chartEntries.add(new Entry(dbEntry.timestamp, val)); // Sử dụng timestamp làm giá trị x
                
                if (val > maxVal) maxVal = val;
                if (val < minVal) minVal = val;
                sumVal += val;
                count++;
            }
        }
        
        if (count > 0) {
            float avgVal = sumVal / count;
            tvMax.setText(getString(R.string.stat_max, String.format(Locale.getDefault(), "%.1f°C", maxVal)));
            tvMin.setText(getString(R.string.stat_min, String.format(Locale.getDefault(), "%.1f°C", minVal)));
            tvAvg.setText(getString(R.string.stat_avg, String.format(Locale.getDefault(), "%.1f°C", avgVal)));
        } else {
            tvMax.setText(getString(R.string.stat_max, "--"));
            tvMin.setText(getString(R.string.stat_min, "--"));
            tvAvg.setText(getString(R.string.stat_avg, "--"));
        }
        
        updateChart(chartEntries);
    }

    private void setupChart() {
        historyChart.getDescription().setEnabled(false);
        historyChart.setTouchEnabled(true);
        historyChart.setDragEnabled(true);
        historyChart.setScaleEnabled(true);
        historyChart.setPinchZoom(true);
        historyChart.setDrawGridBackground(false);

        // Cấu hình trục X
        XAxis xAxis = historyChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawLabels(true);
        xAxis.setGranularity(1000f * 30); // 30 giây
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
        leftAxis.setAxisMaximum(50f); // Nhiệt độ từ 0-50 độ C

        historyChart.getAxisRight().setEnabled(false);
        historyChart.getLegend().setEnabled(false);
        historyChart.setData(new ScatterData()); // Khởi tạo với dữ liệu trống

        // Thiết lập MarkerView (hiển thị thông tin khi nhấn vào điểm)
        CustomMarkerView mv = new CustomMarkerView(this, R.layout.marker_view);
        mv.setUnit("°C");
        mv.setMarkerBackgroundColor(ContextCompat.getColor(this, R.color.temperature_red));
        mv.setChartView(historyChart);
        historyChart.setMarker(mv);
        historyChart.setOnChartValueSelectedListener(this);
    }

    private void updateChart(ArrayList<Entry> chartEntries) {
        ScatterData data = historyChart.getData();
        IScatterDataSet set = data.getDataSetByIndex(0);

        if (set == null) {
            set = createSet(chartEntries);
            data.addDataSet(set);
        } else {
            set.clear();
            for (Entry e : chartEntries) {
                set.addEntry(e);
            }
        }

        data.notifyDataChanged();
        historyChart.notifyDataSetChanged();
        
        if (!chartEntries.isEmpty()) {
            historyChart.moveViewToX(chartEntries.get(chartEntries.size() - 1).getX());
        }
        historyChart.getLegend().setEnabled(false);
    }

    private void updateUI(Float tempValue) {
        if (tempValue != null) {
            progressIndicator.setProgress(tempValue.intValue(), true);
            textViewTemperatureValue.setText(getString(R.string.temperature_format, tempValue));
        } else {
            progressIndicator.setProgress(0, true);
            textViewTemperatureValue.setText(R.string.temperature_default);
        }
    }

    private ScatterDataSet createSet(ArrayList<Entry> chartEntries) {
        ScatterDataSet set = new ScatterDataSet(chartEntries, "Temperature History");
        set.setDrawValues(false);
        int redColor = ContextCompat.getColor(this, R.color.temperature_red);
        set.setColor(redColor);

        set.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        set.setScatterShapeSize(25f); // Tăng kích thước điểm để dễ nhìn hơn
        set.setScatterShapeHoleColor(Color.WHITE); // Tạo lỗ trắng ở giữa
        set.setScatterShapeHoleRadius(2f); // Kích thước lỗ

        return set;
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        // Không cần hiển thị Toast nữa vì đã có MarkerView
    }

    @Override
    public void onNothingSelected() {
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
