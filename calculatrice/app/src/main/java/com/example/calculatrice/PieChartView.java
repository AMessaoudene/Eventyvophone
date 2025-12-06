package com.example.calculatrice;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class PieChartView extends View {

    private Paint paint;
    private RectF rectF;
    private float[] data = new float[0];
    private int[] colors = new int[0];

    public PieChartView(Context context) {
        super(context);
        init();
    }

    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        rectF = new RectF();
    }

    public void setData(float[] data, int[] colors) {
        this.data = data;
        this.colors = colors;
        invalidate(); // Redraw
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (data == null || data.length == 0) return;

        float total = 0;
        for (float value : data) {
            total += value;
        }

        if (total == 0) return;

        int width = getWidth();
        int height = getHeight();
        int size = Math.min(width, height);
        
        // Center the chart
        float left = (width - size) / 2f;
        float top = (height - size) / 2f;
        
        // Add some padding
        float padding = 40f;
        rectF.set(left + padding, top + padding, left + size - padding, top + size - padding);

        float startAngle = 0;
        for (int i = 0; i < data.length; i++) {
            float sweepAngle = (data[i] / total) * 360;
            paint.setColor(colors[i]);
            canvas.drawArc(rectF, startAngle, sweepAngle, true, paint);
            startAngle += sweepAngle;
        }
    }
}
