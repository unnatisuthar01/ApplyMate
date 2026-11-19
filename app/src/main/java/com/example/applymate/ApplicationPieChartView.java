package com.example.applymate;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class ApplicationPieChartView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF chartBounds = new RectF();
    private final int[] colors;
    private int[] values = new int[0];
    private float density;

    public ApplicationPieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        colors = new int[]{
                getResources().getColor(R.color.secondary_text),
                getResources().getColor(R.color.primary_blue),
                getResources().getColor(R.color.warning),
                getResources().getColor(R.color.chart_interview),
                getResources().getColor(R.color.success),
                getResources().getColor(R.color.error)
        };
        setContentDescription(getContext().getString(R.string.application_status_distribution));
    }

    public void setValues(int[] values) {
        this.values = values.clone();
        invalidate();
    }

    public int[] getValues() {
        return values.clone();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int total = 0;
        for (int value : values) {
            total += Math.max(0, value);
        }
        float size = Math.min(getWidth(), getHeight()) - 24 * density;
        if (size <= 0) {
            return;
        }
        float left = (getWidth() - size) / 2f;
        float top = (getHeight() - size) / 2f;
        chartBounds.set(left, top, left + size, top + size);
        if (total == 0) {
            paint.setColor(getResources().getColor(R.color.outline));
            canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, size / 2f, paint);
            paint.setColor(getResources().getColor(R.color.secondary_text));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(14 * density);
            canvas.drawText(getContext().getString(R.string.no_chart_data),
                    getWidth() / 2f, getHeight() / 2f + 5 * density, paint);
            return;
        }

        float startAngle = -90f;
        for (int index = 0; index < values.length && index < colors.length; index++) {
            int value = Math.max(0, values[index]);
            if (value == 0) {
                continue;
            }
            float sweep = 360f * value / total;
            paint.setColor(colors[index]);
            canvas.drawArc(chartBounds, startAngle, sweep, true, paint);
            startAngle += sweep;
        }
        paint.setColor(getResources().getColor(R.color.card_white));
        canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, size * 0.26f, paint);
        paint.setColor(getResources().getColor(R.color.primary_text));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(18 * density);
        paint.setFakeBoldText(true);
        canvas.drawText(Integer.toString(total), getWidth() / 2f,
                getHeight() / 2f + 6 * density, paint);
        paint.setFakeBoldText(false);
    }
}
