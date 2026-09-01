package com.patechltd.salexfypos.util;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.util.HashMap;
import java.util.Map;

public final class QrUtil {

    private QrUtil() {
    }

    /** Renders a QR code bitmap for the given content. */
    public static Bitmap qr(String content, int sizePx) {
        if (content == null || content.isEmpty()) return null;
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE,
                    sizePx, sizePx, hints);
            int[] pixels = new int[sizePx * sizePx];
            for (int y = 0; y < sizePx; y++) {
                for (int x = 0; x < sizePx; x++) {
                    pixels[y * sizePx + x] = matrix.get(x, y) ? Color.BLACK : Color.WHITE;
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx);
            return bitmap;
        } catch (Exception e) {
            AppLogger.e("QR generation failed", e);
            return null;
        }
    }

    /** Standard content used for product QRs: scannable back to the product. */
    public static String productContent(String uid, String barcode) {
        if (barcode != null && !barcode.trim().isEmpty()) return barcode.trim();
        return "SALEXFY:" + (uid == null ? "" : uid);
    }

    /** Standard content used for sale QRs. */
    public static String saleContent(String saleNo, String uid) {
        return "SALEXFY:SALE:" + (saleNo == null ? "" : saleNo) + ":" + (uid == null ? "" : uid);
    }
}
