package com.patechltd.salexfypos.scanner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public class ScanFrameDrawable extends Drawable {

    private final Paint paint;
    private final int cornerLength;
    private final int strokeWidth;

    public ScanFrameDrawable(Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        cornerLength = Math.round(28 * density);
        strokeWidth = Math.round(3 * density);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        int cx = bounds.centerX();
        int cy = bounds.centerY();
        int w = (int) (bounds.width() * 0.78f);
        int h = (int) (bounds.height() * 0.52f);
        int halfW = w / 2;
        int halfH = h / 2;
        int left = cx - halfW;
        int top = cy - halfH;
        int right = cx + halfW;
        int bottom = cy + halfH;

        canvas.drawLine(left, top, left + cornerLength, top, paint);
        canvas.drawLine(left, top, left, top + cornerLength, paint);
        canvas.drawLine(right - cornerLength, top, right, top, paint);
        canvas.drawLine(right, top, right, top + cornerLength, paint);
        canvas.drawLine(left, bottom - cornerLength, left, bottom, paint);
        canvas.drawLine(left, bottom, left + cornerLength, bottom, paint);
        canvas.drawLine(right - cornerLength, bottom, right, bottom, paint);
        canvas.drawLine(right, bottom - cornerLength, right, bottom, paint);
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(android.graphics.ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return android.graphics.PixelFormat.TRANSLUCENT;
    }
}
