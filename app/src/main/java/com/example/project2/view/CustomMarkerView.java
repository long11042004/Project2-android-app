package com.example.project2.view;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.widget.TextView;
import android.widget.ImageView;

import com.example.project2.R;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CustomMarkerView extends MarkerView {

    private final TextView tvContent;
    private final ImageView ivArrow;
    private final SimpleDateFormat mFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private String unit = "°C"; // Mặc định

    public CustomMarkerView(Context context, int layoutResource) {
        super(context, layoutResource);
        tvContent = findViewById(R.id.tvContent);
        ivArrow = findViewById(R.id.ivArrow);
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public void setMarkerBackgroundColor(int color) {
        // Tô màu cho nền bo tròn của TextView
        Drawable background = tvContent.getBackground();
        if (background != null) {
            background.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
        
        // Tô màu cho mũi tên
        if (ivArrow != null) {
            ivArrow.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        String time = mFormat.format(new Date((long) e.getX()));
        String val = String.format(Locale.getDefault(), "%.1f%s", e.getY(), unit);
        tvContent.setText(time + "\n" + val);
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        // Căn giữa theo chiều ngang và hiển thị phía trên điểm
        return new MPPointF(-(getWidth() / 2f), -getHeight() - 20); // Dịch lên thêm 20 đơn vị để không bị sát
    }
}