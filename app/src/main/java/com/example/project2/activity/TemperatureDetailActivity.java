package com.example.project2.activity;

import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.project2.db.TemperatureHistoryEntry;
import com.example.project2.view.CustomMarkerView;
import com.example.project2.view.ThermometerView;
import com.example.project2.model.TemperatureDataRepository;
import com.example.project2.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Locale;

public class TemperatureDetailActivity extends AppCompatActivity implements OnChartValueSelectedListener {
    public static final String EXTRA_TEMPERATURE_VALUE = "EXTRA_TEMPERATURE_VALUE";
    private ThermometerView thermometerView;
    private TextView textViewTemperatureValue;
    private LineChart historyChart;
    private TextView tvMax, tvMin, tvAvg;
    private Spinner spinnerTimeFilter;
    private List<com.example.project2.db.TemperatureHistoryEntry> fullHistory = new ArrayList<>();
    private long filterDuration = 15 * 60 * 1000L; // Mặc định 15 phút

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

        thermometerView = findViewById(R.id.thermometerView);
        textViewTemperatureValue = findViewById(R.id.textViewCurrentTemperatureDetail);
        historyChart = findViewById(R.id.lineChartTemperatureHistory);
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
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        
        setupChart();
        thermometerView.setMaxTemperature(50);
        thermometerView.setLiquidColor(ContextCompat.getColor(this, R.color.temperature_red));

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
        long referenceTimestamp = threshold;
        
        float maxVal = -Float.MAX_VALUE;
        float minVal = Float.MAX_VALUE;
        float sumVal = 0;
        int count = 0;

        for (int i = 0; i < fullHistory.size(); i++) {
            TemperatureHistoryEntry dbEntry = fullHistory.get(i);
            if (dbEntry.timestamp >= threshold) {
                float val = dbEntry.temperatureValue;
                chartEntries.add(new Entry((float) (dbEntry.timestamp - referenceTimestamp), val)); // Sử dụng timestamp làm giá trị x
                
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
        
        // Cập nhật định dạng trục X dựa trên khoảng thời gian
        XAxis xAxis = historyChart.getXAxis();
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum((float) (now - referenceTimestamp));
        final String format;
        if (filterDuration <= 2 * 60 * 60 * 1000L) { // <= 2 giờ: hiển thị giờ:phút:giây
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

    private void setupChart() {
        historyChart.getDescription().setEnabled(false);
        historyChart.setTouchEnabled(true);
        historyChart.setDragEnabled(true);
        historyChart.setScaleXEnabled(true);
        historyChart.setScaleYEnabled(false);
        historyChart.setPinchZoom(true);
        historyChart.setDoubleTapToZoomEnabled(true);
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
        historyChart.setData(new LineData()); // Khởi tạo với dữ liệu trống

        // Thiết lập MarkerView (hiển thị thông tin khi nhấn vào điểm)
        CustomMarkerView mv = new CustomMarkerView(this, R.layout.marker_view);
        mv.setUnit("°C");
        mv.setMarkerBackgroundColor(ContextCompat.getColor(this, R.color.temperature_red));
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

    private void updateUI(Float tempValue) {
        if (tempValue != null) {
            thermometerView.setCurrentTemperature(tempValue);
            textViewTemperatureValue.setText(getString(R.string.temperature_format, tempValue));
        } else {
            thermometerView.setCurrentTemperature(0);
            textViewTemperatureValue.setText(R.string.temperature_default);
        }
    }

    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "Temperature History");
        set.setDrawValues(false);
        int redColor = ContextCompat.getColor(this, R.color.temperature_red);
        set.setColor(redColor);
        set.setLineWidth(1f);
        set.setDrawCircles(true);
        set.setCircleColor(redColor);
        set.setCircleRadius(2f);
        set.setDrawCircleHole(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawFilled(true);
        set.setFillColor(redColor);
        set.setFillAlpha(60);

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
