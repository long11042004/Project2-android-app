package com.example.project2.activity;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.project2.model.HumidityDataRepository;
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
import java.util.Date;
import java.util.Locale;

public class AirHumidityDetailActivity extends AppCompatActivity {

    public static final String EXTRA_AIR_HUMIDITY_VALUE = "EXTRA_AIR_HUMIDITY_VALUE";

    private CircularProgressIndicator progressIndicator;
    private TextView textViewHumidityValue;
    private LineChart historyChart;

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
                ArrayList<Entry> chartEntries = new ArrayList<>();
                for (com.example.project2.db.AirHumidityHistoryEntry dbEntry : historyEntries) {
                    chartEntries.add(new Entry(dbEntry.timestamp, dbEntry.humidityValue));
                }
                updateChart(chartEntries);
            }
        });
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
        leftAxis.setAxisMaximum(100f);

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

    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "Lịch sử độ ẩm không khí");
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawValues(false);
        set.setDrawCircleHole(false);
        set.setLineWidth(2f);
        set.setDrawCircles(false);
        set.setDrawFilled(true);

        int blueColor = ContextCompat.getColor(this, R.color.air_humidity_blue);
        set.setColor(blueColor);
        set.setCircleColor(blueColor);

        // Tạo gradient màu xanh cho vùng dưới biểu đồ
        int[] gradientColors = {
                ContextCompat.getColor(this, R.color.air_humidity_blue),
                ContextCompat.getColor(this, R.color.air_humidity_blue_dark)
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
