package com.patechltd.salexfypos.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;

import java.io.OutputStream;
import java.util.List;

/**
 * Minimal PDF export for text-based reports (TOT statements). Lines starting
 * with "//" are rendered as section headers.
 */
public final class PdfUtil {

    private PdfUtil() {
    }

    public static boolean exportText(Context context, Uri target, String title, List<String> lines) {
        final int pageW = 595;
        final int pageH = 842;
        final int margin = 40;
        final int lineH = 13;
        final int yMax = pageH - 50;

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(0xFF1A1A1A);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setTextSize(16);

        Paint headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerPaint.setColor(0xFF09535A);
        headerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        headerPaint.setTextSize(11);

        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(0xFF222222);
        linePaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));
        linePaint.setTextSize(9.5f);

        PdfDocument document = new PdfDocument();
        OutputStream os = null;
        try {
            PdfDocument.Page page = document.startPage(
                    new PdfDocument.PageInfo.Builder(pageW, pageH, 1).create());
            Canvas canvas = page.getCanvas();
            int y = margin + 12;

            canvas.drawText(title == null ? "" : title, margin, y, titlePaint);
            y += 24;

            for (String raw : lines) {
                String s = raw == null ? "" : raw;
                if (y > yMax) {
                    document.finishPage(page);
                    int n = document.getPages().size() + 1;
                    page = document.startPage(new PdfDocument.PageInfo.Builder(pageW, pageH, n).create());
                    canvas = page.getCanvas();
                    y = margin;
                }
                Paint p = s.startsWith("//") ? headerPaint : linePaint;
                canvas.drawText(s.startsWith("//") ? s.substring(2) : s, margin, y, p);
                y += lineH;
            }
            document.finishPage(page);

            os = context.getContentResolver().openOutputStream(target);
            if (os == null) return false;
            document.writeTo(os);
            return true;
        } catch (Exception e) {
            AppLogger.e("PDF export failed", e);
            return false;
        } finally {
            try {
                if (os != null) os.close();
            } catch (Exception ignored) {
            }
            document.close();
        }
    }
}