package com.example.project2.activity;

import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.project2.view.CustomMarkerView;
import com.example.project2.model.MoistureDataRepository;
import com.example.project2.R;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.List;

public class SoilMoistureDetailActivity extends AppCompatActivity implements OnChartValueSelectedListener {

    public static final String EXTRA_MOISTURE_VALUE = "EXTRA_MOISTURE_VALUE";

    private CircularProgressIndicator progressIndicator;
    private TextView textViewMoistureValue;
    private ScatterChart historyChart;
    private TextView tvMax, tvMin, tvAvg;
    private List<com.example.project2.db.MoistureHistoryEntry> fullHistory = new ArrayList<>();
    private long filterDuration = 24 * 60 * 60 * 1000L; // Mặc định 24 giờ

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

        for (com.example.project2.db.MoistureHistoryEntry dbEntry : fullHistory) {
            if (dbEntry.timestamp >= threshold) {
                float val = dbEntry.moistureValue;
                chartEntries.add(new Entry(dbEntry.timestamp, val));
                
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
        
        updateChart(chartEntries);
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
        historyChart.setData(new ScatterData()); // Khởi tạo với dữ liệu trống

        // Thiết lập MarkerView
        CustomMarkerView mv = new CustomMarkerView(this, R.layout.marker_view);
        mv.setUnit("%");
        mv.setMarkerBackgroundColor(ContextCompat.getColor(this, R.color.brown));
        mv.setChartView(historyChart);
        historyChart.setMarker(mv);
        historyChart.setOnChartValueSelectedListener(this);
    }

    private void updateChart(ArrayList<Entry> chartEntries) {
        ScatterData data = historyChart.getData();
        IScatterDataSet set = data.getDataSetByIndex(0);

        if (set == null) {
            set = createSet();
            data.addDataSet(set);
        } else {
            set.clear();
            for (Entry e : chartEntries) {
                set.addEntry(e);
            }
        }

        // Thông báo cho biểu đồ về sự thay đổi
        data.notifyDataChanged();
        historyChart.notifyDataSetChanged();

        // Tự động cuộn đến điểm dữ liệu mới nhất
        if (!chartEntries.isEmpty()) {
            historyChart.moveViewToX(chartEntries.get(chartEntries.size() - 1).getX());
        }
        historyChart.getLegend().setEnabled(false);
    }

    private ScatterDataSet createSet() {
        ScatterDataSet set = new ScatterDataSet(null, "Lịch sử độ ẩm");
        set.setDrawValues(false);
        
        int brownColor = ContextCompat.getColor(this, R.color.brown);
        set.setColor(brownColor);

        set.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        set.setScatterShapeSize(25f);
        set.setScatterShapeHoleColor(Color.WHITE);
        set.setScatterShapeHoleRadius(2f);

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
