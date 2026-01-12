package com.example.project2.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.project2.view.CustomMarkerView;
import com.example.project2.model.MoistureDataRepository;
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

import java.util.Calendar;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.List;

public class SoilMoistureDetailActivity extends AppCompatActivity implements OnChartValueSelectedListener {

    public static final String EXTRA_MOISTURE_VALUE = "EXTRA_MOISTURE_VALUE";

    private CircularProgressIndicator progressIndicator;
    private TextView textViewMoistureValue;
    private LineChart historyChart;
    private TextView tvMax, tvMin, tvAvg;
    private EditText etDateFilter;
    private Calendar selectedDate = Calendar.getInstance();

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
        etDateFilter = findViewById(R.id.etDateFilter);
        
        ImageButton btnRefresh = findViewById(R.id.btnRefresh);
        btnRefresh.setOnClickListener(v -> {
            updateChartWithFilter();
        });

        ImageButton btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomIn.setOnClickListener(v -> {
            historyChart.zoom(1.4f, 1f, historyChart.getCenterOfView().x, historyChart.getCenterOfView().y);
        });

        ImageButton btnZoomOut = findViewById(R.id.btnZoomOut);
        btnZoomOut.setOnClickListener(v -> {
            historyChart.zoom(0.7f, 1f, historyChart.getCenterOfView().x, historyChart.getCenterOfView().y);
        });

        updateDateEditText();
        etDateFilter.setOnClickListener(v -> showDatePicker());
        
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
        MoistureDataRepository.getInstance(getApplication()).getMoistureHistory().observe(this, historyEntries -> {
            populateChart(historyEntries);
        });
        updateChartWithFilter(); // Lấy dữ liệu ban đầu cho ngày hôm nay
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(Calendar.YEAR, year);
                    selectedDate.set(Calendar.MONTH, month);
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateEditText();
                    updateChartWithFilter();
                    historyChart.fitScreen();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void updateDateEditText() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        etDateFilter.setText(sdf.format(selectedDate.getTime()));
    }

    private void updateChartWithFilter() {
        // Tính thời gian bắt đầu và kết thúc của ngày được chọn
        Calendar startCal = (Calendar) selectedDate.clone();
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        long startTime = startCal.getTimeInMillis();

        Calendar endCal = (Calendar) selectedDate.clone();
        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MILLISECOND, 999);
        long endTime = endCal.getTimeInMillis();

        // Kích hoạt việc lấy dữ liệu từ repository
        MoistureDataRepository.getInstance(getApplication()).fetchHistoryInRange(startTime, endTime);
    }

    private void populateChart(List<com.example.project2.db.MoistureHistoryEntry> history) {
        if (history == null) return;

        ArrayList<Entry> chartEntries = new ArrayList<>();
        Calendar startCal = (Calendar) selectedDate.clone();
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        long startTime = startCal.getTimeInMillis();

        float maxVal = -Float.MAX_VALUE;
        float minVal = Float.MAX_VALUE;
        float sumVal = 0;
        int count = 0;

        for (com.example.project2.db.MoistureHistoryEntry dbEntry : history) {
            float val = dbEntry.moistureValue;
            chartEntries.add(new Entry((float) (dbEntry.timestamp - startTime), val));

            if (val > maxVal) maxVal = val;
            if (val < minVal) minVal = val;
            sumVal += val;
            count++;
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
        xAxis.setAxisMaximum(24 * 60 * 60 * 1000f); // 24 giờ
        
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat mFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            @Override
            public String getAxisLabel(float value, com.github.mikephil.charting.components.AxisBase axis) {
                return mFormat.format(new Date((long) value + startTime));
            }
        });

        updateChart(chartEntries);
        // Giới hạn hiển thị tối đa 4 giờ trên màn hình
        historyChart.setVisibleXRangeMaximum(4 * 60 * 60 * 1000f);
        if (!chartEntries.isEmpty()) {
            historyChart.moveViewToX(chartEntries.get(chartEntries.size() - 1).getX());
        }
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
        historyChart.setScaleXEnabled(true);
        historyChart.setScaleYEnabled(false);
        historyChart.setPinchZoom(true);
        historyChart.setDoubleTapToZoomEnabled(true);
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

        // Thiết lập MarkerView
        CustomMarkerView mv = new CustomMarkerView(this, R.layout.marker_view);
        mv.setUnit("%");
        mv.setMarkerBackgroundColor(ContextCompat.getColor(this, R.color.brown));
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

        // Thông báo cho biểu đồ về sự thay đổi
        data.notifyDataChanged();
        historyChart.notifyDataSetChanged();
        historyChart.invalidate();

        // Tự động cuộn đến điểm dữ liệu mới nhất
        if (!chartEntries.isEmpty()) {
            historyChart.moveViewToX(chartEntries.get(chartEntries.size() - 1).getX());
        }
        historyChart.getLegend().setEnabled(false);
    }

    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "Lịch sử độ ẩm");
        set.setDrawValues(false);
        
        int brownColor = ContextCompat.getColor(this, R.color.brown);
        set.setColor(brownColor);
        set.setLineWidth(1f);
        set.setDrawCircles(true);
        set.setCircleColor(brownColor);
        set.setCircleRadius(2f);
        set.setDrawCircleHole(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawFilled(true);
        set.setFillColor(brownColor);
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
