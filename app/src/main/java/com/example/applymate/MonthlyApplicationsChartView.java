package com.example.applymate;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MonthlyApplicationsChartView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int textColor;
    private final int gridColor;
    private final int barColor;
    private final float density;
    private int[] values = new int[0];

    public MonthlyApplicationsChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        textColor = getResources().getColor(R.color.secondary_text);
        gridColor = getResources().getColor(R.color.outline);
        barColor = getResources().getColor(R.color.primary_blue);
        setContentDescription(getContext().getString(R.string.monthly_applications_chart));
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
        if (values.length == 0) {
            return;
        }
        float left = 32 * density;
        float right = getWidth() - 12 * density;
        float top = 14 * density;
        float bottom = getHeight() - 32 * density;
        if (right <= left || bottom <= top) {
            return;
        }
        int maximum = 0;
        for (int value : values) {
            maximum = Math.max(maximum, value);
        }
        float graphHeight = bottom - top;
        paint.setColor(gridColor);
        paint.setStrokeWidth(density);
        for (int line = 0; line < 4; line++) {
            float y = top + graphHeight * line / 3f;
            canvas.drawLine(left, y, right, y, paint);
        }
        float slotWidth = (right - left) / values.length;
        float maxBarHeight = graphHeight * 0.88f;
        Calendar month = Calendar.getInstance();
        month.set(Calendar.DAY_OF_MONTH, 1);
        month.add(Calendar.MONTH, -(values.length - 1));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(11 * density);
        Rect bounds = new Rect();
        SimpleDateFormat monthLabel = new SimpleDateFormat("MMM", Locale.getDefault());
        for (int index = 0; index < values.length; index++) {
            float centerX = left + slotWidth * (index + 0.5f);
            float barHeight = maximum == 0 ? 0
                    : maxBarHeight * values[index] / maximum;
            paint.setColor(barColor);
            canvas.drawRoundRect(centerX - slotWidth * 0.28f, bottom - barHeight,
                    centerX + slotWidth * 0.28f, bottom, 4 * density, 4 * density, paint);
            paint.setColor(textColor);
            String label = monthLabel.format(month.getTime());
            canvas.drawText(label, centerX, getHeight() - 8 * density, paint);
            if (values[index] > 0) {
                String count = Integer.toString(values[index]);
                paint.getTextBounds(count, 0, count.length(), bounds);
                canvas.drawText(count, centerX,
                        bottom - barHeight - 5 * density, paint);
            }
            month.add(Calendar.MONTH, 1);
        }
    }
}
