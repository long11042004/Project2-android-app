package com.example.project2.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

public class ThermometerView extends View {

    private Paint glassPaint;
    private Paint liquidPaint;
    private Paint scalePaint;
    private float currentTemp = 0f;
    private float maxTemp = 50f;
    private float minTemp = 0f;

    public ThermometerView(Context context) {
        super(context);
        init();
    }

    public ThermometerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        glassPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glassPaint.setColor(Color.parseColor("#EEEEEE")); // Màu nền ống (xám nhạt)
        glassPaint.setStyle(Paint.Style.FILL);

        liquidPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        liquidPaint.setColor(Color.RED); // Màu mặc định, sẽ được set lại từ Activity
        liquidPaint.setStyle(Paint.Style.FILL);

        scalePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scalePaint.setColor(Color.BLACK);
        scalePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        scalePaint.setStrokeWidth(2f);
        scalePaint.setTextSize(25f);
    }

    public void setCurrentTemperature(float temp) {
        this.currentTemp = Math.max(minTemp, Math.min(temp, maxTemp));
        invalidate();
    }

    public void setMaxTemperature(float max) {
        this.maxTemp = max;
        invalidate();
    }
    
    public void setLiquidColor(int color) {
        liquidPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();
        float centerX = width / 2;
        float padding = 10f;

        // Kích thước bầu đựng (Bulb)
        float bulbRadius = Math.min(width, height) / 6;
        float bulbCenterY = height - bulbRadius - padding;

        // Kích thước ống (Tube)
        float tubeWidth = bulbRadius * 0.6f;
        float tubeTop = padding;
        float tubeBottom = bulbCenterY;

        // 1. Vẽ phần vỏ thủy tinh (Nền)
        RectF tubeRect = new RectF(centerX - tubeWidth/2, tubeTop, centerX + tubeWidth/2, tubeBottom);
        canvas.drawRoundRect(tubeRect, tubeWidth/2, tubeWidth/2, glassPaint);
        canvas.drawCircle(centerX, bulbCenterY, bulbRadius, glassPaint);

        // 2. Vẽ chất lỏng
        float tubeHeight = tubeBottom - tubeTop - tubeWidth/2; // Trừ đi phần bo tròn ở đỉnh
        float percent = (currentTemp - minTemp) / (maxTemp - minTemp);
        float liquidHeight = tubeHeight * percent;
        float liquidTop = tubeBottom - liquidHeight;

        // Bầu chất lỏng (nhỏ hơn vỏ một chút)
        canvas.drawCircle(centerX, bulbCenterY, bulbRadius - 4, liquidPaint); 
        
        // Ống chất lỏng
        if (percent > 0) {
            RectF liquidRect = new RectF(centerX - tubeWidth/2 + 4, liquidTop, centerX + tubeWidth/2 - 4, tubeBottom + 10);
            canvas.drawRect(liquidRect, liquidPaint);
        }

        // 3. Vẽ thang đo (Scale)
        float scaleStartX = centerX + tubeWidth / 2 + 10;
        float majorTickLength = 30f;
        float minorTickLength = 15f;

        scalePaint.setTextAlign(Paint.Align.LEFT);

        for (float temp = minTemp; temp <= maxTemp; temp += 5) {
            float tempPercent = (temp - minTemp) / (maxTemp - minTemp);
            float yPos = tubeBottom - (tubeHeight * tempPercent);

            if (temp % 10 == 0) { // Major tick
                canvas.drawLine(scaleStartX, yPos, scaleStartX + majorTickLength, yPos, scalePaint);
                canvas.drawText(String.valueOf((int)temp), scaleStartX + majorTickLength + 10, yPos + 8, scalePaint);
            } else { // Minor tick
                canvas.drawLine(scaleStartX, yPos, scaleStartX + minorTickLength, yPos, scalePaint);
            }
        }
    }
}