package com.example.project2.activity;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.project2.R;
import com.google.android.material.progressindicator.CircularProgressIndicator;

public class AirHumidityDetailActivity extends AppCompatActivity {

    public static final String EXTRA_AIR_HUMIDITY_VALUE = "EXTRA_AIR_HUMIDITY_VALUE";

    private CircularProgressIndicator progressIndicator;
    private TextView textViewHumidityValue;

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

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_AIR_HUMIDITY_VALUE)) {
            try {
                String humidityString = intent.getStringExtra(EXTRA_AIR_HUMIDITY_VALUE);
                updateUI(Integer.parseInt(humidityString));
            } catch (NumberFormatException e) {
                updateUI(null);
            }
        } else {
            updateUI(null);
        }
    }

    private void updateUI(Integer humidityValue) {
        if (humidityValue != null) {
            progressIndicator.setProgress(humidityValue, true);
            textViewHumidityValue.setText(getString(R.string.air_humidity_format, humidityValue));
        } else {
            progressIndicator.setProgress(0, true);
            textViewHumidityValue.setText(R.string.air_humidity_default);
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
