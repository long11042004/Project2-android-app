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

import java.util.Calendar;
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
    private EditText etDateFilter;
    private Calendar selectedDate = Calendar.getInstance();

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
            populateChart(historyEntries);
        });
        updateChartWithFilter(); // Fetch initial data for today
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

        // Kích hoạt việc lấy dữ liệu từ repository cho khoảng thời gian đã chọn
        TemperatureDataRepository.getInstance(getApplication()).fetchHistoryInRange(startTime, endTime);
    }

    private void populateChart(List<TemperatureHistoryEntry> history) {
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

        for (TemperatureHistoryEntry dbEntry : history) {
            float val = dbEntry.temperatureValue;
            chartEntries.add(new Entry((float) (dbEntry.timestamp - startTime), val)); // X là khoảng thời gian từ đầu ngày

            if (val > maxVal) maxVal = val;
            if (val < minVal) minVal = val;
            sumVal += val;
            count++;
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
        xAxis.setAxisMaximum(24 * 60 * 60 * 1000f); // 24 giờ

        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat mFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            @Override
            public String getAxisLabel(float value, com.github.mikephil.charting.components.AxisBase axis) {
                return mFormat.format(new Date((long) value + startTime));
            }
        });

        updateChart(chartEntries);
        historyChart.setVisibleXRangeMaximum(4 * 60 * 60 * 1000f);
        if (!chartEntries.isEmpty()) {
            historyChart.moveViewToX(chartEntries.get(chartEntries.size() - 1).getX());
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
