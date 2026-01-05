package com.example.project2.activity;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.graphics.Color;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.project2.view.CustomMarkerView;
import com.example.project2.model.HumidityDataRepository;
import com.example.project2.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Locale;

public class AirHumidityDetailActivity extends AppCompatActivity implements OnChartValueSelectedListener {

    public static final String EXTRA_AIR_HUMIDITY_VALUE = "EXTRA_AIR_HUMIDITY_VALUE";

    private CircularProgressIndicator progressIndicator;
    private TextView textViewHumidityValue;
    private LineChart historyChart;
    private TextView tvMax, tvMin, tvAvg;
    private Spinner spinnerTimeFilter;
    private List<com.example.project2.db.AirHumidityHistoryEntry> fullHistory = new ArrayList<>();
    private long filterDuration = 15 * 60 * 1000L; // Mặc định 15 phút

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_air_humidity_detail);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Chi tiết Độ ẩm không khí");
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.air_humidity_blue_dark)));
        }

        progressIndicator = findViewById(R.id.progressIndicatorAirHumidity);
        textViewHumidityValue = findViewById(R.id.textViewCurrentAirHumidityDetail);
        historyChart = findViewById(R.id.lineChartAirHumidityHistory);
        tvMax = findViewById(R.id.tvMaxVal);
        tvMin = findViewById(R.id.tvMinVal);
        tvAvg = findViewById(R.id.tvAvgVal);
        spinnerTimeFilter = findViewById(R.id.spinnerTimeFilter);
        
        ImageButton btnRefresh = findViewById(R.id.btnRefresh);
        btnRefresh.setOnClickListener(v -> {
            updateChartWithFilter();
        });

        // Đặt giá trị mặc định cho Spinner là "15 phút" (vị trí 1)
        spinnerTimeFilter.setSelection(1);

        spinnerTimeFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                String selectedTime = (String) parent.getItemAtPosition(position);
                switch (selectedTime) {
                    case "5 phút": filterDuration = 5 * 60 * 1000L; break;
                    case "15 phút": filterDuration = 15 * 60 * 1000L; break;
                    case "30 phút": filterDuration = 30 * 60 * 1000L; break;
                    case "1 giờ": filterDuration = 60 * 60 * 1000L; break;
                    case "12 giờ": filterDuration = 12 * 60 * 60 * 1000L; break;
                    case "1 ngày": filterDuration = 24 * 60 * 60 * 1000L; break;
                    case "7 ngày": filterDuration = 7 * 24 * 60 * 60 * 1000L; break;
                    default: filterDuration = 15 * 60 * 1000L; break;
                }
                updateChartWithFilter();
                historyChart.fitScreen(); // Reset zoom và scroll khi thay đổi bộ lọc
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Không làm gì cả nếu không có mục nào được chọn
            }
        });

        setupChart();

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_AIR_HUMIDITY_VALUE)) {
            try {
                String humidityString = intent.getStringExtra(EXTRA_AIR_HUMIDITY_VALUE);
                float humidityFloat = Float.parseFloat(humidityString);
                updateUI(humidityFloat);
            } catch (NumberFormatException e) {
                updateUI(null);
            }
        } else {
            updateUI(null);
        }

        // Lắng nghe dữ liệu thời gian thực từ HumidityDataRepository
        HumidityDataRepository.getInstance(getApplication()).getAirHumidity().observe(this, humidityString -> {
            if (humidityString != null) {
                try {
                    float humidityValue = Float.parseFloat(humidityString);
                    updateUI(humidityValue);
                } catch (NumberFormatException e) {
                    // Bỏ qua lỗi
                }
            }
        });

        // Lắng nghe lịch sử dữ liệu để vẽ biểu đồ từ HumidityDataRepository
        HumidityDataRepository.getInstance(getApplication()).getAirHumidityHistory().observe(this, historyEntries -> {
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
        long referenceTimestamp = threshold;
        
        float maxVal = -Float.MAX_VALUE;
        float minVal = Float.MAX_VALUE;
        float sumVal = 0;
        int count = 0;

        for (com.example.project2.db.AirHumidityHistoryEntry dbEntry : fullHistory) {
            if (dbEntry.timestamp >= threshold) {
                float val = dbEntry.humidityValue;
                chartEntries.add(new Entry((float) (dbEntry.timestamp - referenceTimestamp), val));
                
                if (val > maxVal) maxVal = val;
                if (val < minVal) minVal = val;
                sumVal += val;
                count++;
            }
        }
        
        if (count > 0) {
            float avgVal = sumVal / count;
            tvMax.setText(getString(R.string.stat_max, String.format(Locale.getDefault(), "%.1f%%", maxVal)));
            tvMin.setText(getString(R.string.stat_min, String.format(Locale.getDefault(), "%.1f%%", minVal)));
            tvAvg.setText(getString(R.string.stat_avg, String.format(Locale.getDefault(), "%.1f%%", avgVal)));
        } else {
            tvMax.setText(getString(R.string.stat_max, "--"));
            tvMin.setText(getString(R.string.stat_min, "--"));
            tvAvg.setText(getString(R.string.stat_avg, "--"));
        }
        
        // Cập nhật định dạng trục X dựa trên khoảng thời gian
        XAxis xAxis = historyChart.getXAxis();
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum((float) (now - referenceTimestamp));
        final String format;
        if (filterDuration <= 2 * 60 * 60 * 1000L) { // <= 2 giờ: hiển thị giờ:phút
            format = "HH:mm";
        } else if (filterDuration <= 24 * 60 * 60 * 1000L) { // <= 1 ngày: hiển thị giờ:phút
            format = "HH:mm";
        } else { // > 1 ngày: hiển thị ngày/tháng
            format = "dd/MM";
        }
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat mFormat = new SimpleDateFormat(format, Locale.getDefault());
            @Override
            public String getAxisLabel(float value, com.github.mikephil.charting.components.AxisBase axis) {
                return mFormat.format(new Date((long) value + referenceTimestamp));
            }
        });

        updateChart(chartEntries);
    }

    private void updateUI(Float humidityValue) {
        if (humidityValue != null) {
            progressIndicator.setProgress(Math.round(humidityValue), true);
            textViewHumidityValue.setText(getString(R.string.air_humidity_format, humidityValue));
        } else {
            progressIndicator.setProgress(0, true);
            textViewHumidityValue.setText(R.string.air_humidity_default);
        }
    }

    private void setupChart() {
        historyChart.getDescription().setEnabled(false);
        historyChart.setTouchEnabled(true);
        historyChart.setDragEnabled(true);
        historyChart.setScaleXEnabled(true);
        historyChart.setScaleYEnabled(false);
        historyChart.setPinchZoom(true);
        historyChart.setDoubleTapToZoomEnabled(true);
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
        leftAxis.setAxisMaximum(100f);

        historyChart.getAxisRight().setEnabled(false);
        historyChart.getLegend().setEnabled(false);
        historyChart.setData(new LineData());

        // Thiết lập MarkerView
        CustomMarkerView mv = new CustomMarkerView(this, R.layout.marker_view);
        mv.setUnit("%");
        mv.setMarkerBackgroundColor(ContextCompat.getColor(this, R.color.air_humidity_blue));
        mv.setChartView(historyChart);
        historyChart.setMarker(mv);
        historyChart.setOnChartValueSelectedListener(this);
    }

    private void updateChart(ArrayList<Entry> chartEntries) {
        LineData data = historyChart.getData();
        ILineDataSet set = data.getDataSetByIndex(0);

        if (set == null) {
            set = createSet();
            data.addDataSet(set);
        }

        set.clear();
        for (Entry e : chartEntries) {
            set.addEntry(e);
        }

        data.notifyDataChanged();
        historyChart.notifyDataSetChanged();
        historyChart.invalidate();

        if (!chartEntries.isEmpty()) {
            historyChart.moveViewToX(chartEntries.get(chartEntries.size() - 1).getX());
        }
        historyChart.getLegend().setEnabled(false);
    }

    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "Lịch sử độ ẩm không khí");
        set.setDrawValues(false);

        int blueColor = ContextCompat.getColor(this, R.color.air_humidity_blue);
        set.setColor(blueColor);
        set.setLineWidth(1f);
        set.setDrawCircles(true);
        set.setCircleColor(blueColor);
        set.setCircleRadius(2f);
        set.setDrawCircleHole(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawFilled(true);
        set.setFillColor(blueColor);
        set.setFillAlpha(60);

        return set;
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) { }

    @Override
    public void onNothingSelected() { }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
