package com.example.project2.activity;

import android.content.Intent;
import android.os.Bundle;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.project2.model.TemperatureDataRepository;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Locale;

public class TemperatureDetailActivity extends AppCompatActivity {
    public static final String EXTRA_TEMPERATURE_VALUE = "EXTRA_TEMPERATURE_VALUE";
    private CircularProgressIndicator progressIndicator;
    private TextView textViewTemperatureValue;
    private LineChart historyChart;
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
        historyChart = findViewById(R.id.lineChartTemperatureHistory);
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

        for (com.example.project2.db.TemperatureHistoryEntry dbEntry : fullHistory) {
            if (dbEntry.timestamp >= threshold) {
                float val = dbEntry.temperatureValue;
                chartEntries.add(new Entry(dbEntry.timestamp, val));
                
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
        historyChart.setData(new LineData());
    }

    private void updateChart(ArrayList<Entry> chartEntries) {
        LineData data = historyChart.getData();
        ILineDataSet set = data.getDataSetByIndex(0);

        if (set == null) {
            set = createSet();
            data.addDataSet(set);
        }
        ((LineDataSet) set).setValues(chartEntries);
        data.notifyDataChanged();
        historyChart.notifyDataSetChanged();
        historyChart.moveViewToX(data.getEntryCount());
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

    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "Lịch sử nhiệt độ");
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawValues(false);
        set.setDrawCircleHole(false);
        set.setLineWidth(2f);
        set.setDrawCircles(false);
        set.setDrawFilled(true);

        int redColor = ContextCompat.getColor(this, R.color.temperature_red);
        set.setColor(redColor);
        set.setCircleColor(redColor);

        // Tạo gradient màu đỏ cho vùng dưới biểu đồ
        int[] gradientColors = {
                ContextCompat.getColor(this, R.color.temperature_red),
                ContextCompat.getColor(this, android.R.color.white)
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
