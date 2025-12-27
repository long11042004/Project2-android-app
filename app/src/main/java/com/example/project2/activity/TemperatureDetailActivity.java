package com.example.project2.activity;

import android.content.Intent;
import android.os.Bundle;
import android.graphics.drawable.ColorDrawable;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.project2.R;
import com.google.android.material.progressindicator.CircularProgressIndicator;

public class TemperatureDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TEMPERATURE_VALUE = "EXTRA_TEMPERATURE_VALUE";

    private CircularProgressIndicator progressIndicator;
    private TextView textViewTemperatureValue;

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

        // Đặt khoảng giá trị cho đồng hồ nhiệt độ, ví dụ 0-50°C
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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
