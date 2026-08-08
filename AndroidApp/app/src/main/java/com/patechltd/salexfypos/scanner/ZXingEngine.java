package com.patechltd.salexfypos.scanner;

import android.media.Image;

import androidx.camera.core.ImageProxy;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.patechltd.salexfypos.util.Prefs;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ZXingEngine implements DecodeEngine {

    private static final List<BarcodeFormat> FORMATS = new ArrayList<>();

    static {
        FORMATS.add(BarcodeFormat.EAN_13);
        FORMATS.add(BarcodeFormat.EAN_8);
        FORMATS.add(BarcodeFormat.UPC_A);
        FORMATS.add(BarcodeFormat.UPC_E);
        FORMATS.add(BarcodeFormat.CODE_128);
        FORMATS.add(BarcodeFormat.CODE_39);
        FORMATS.add(BarcodeFormat.CODE_93);
        FORMATS.add(BarcodeFormat.ITF);
        FORMATS.add(BarcodeFormat.CODABAR);
        FORMATS.add(BarcodeFormat.QR_CODE);
        FORMATS.add(BarcodeFormat.DATA_MATRIX);
        FORMATS.add(BarcodeFormat.PDF_417);
    }

    private final int sensitivity;
    private final boolean tryHarder;

    public ZXingEngine() {
        this(2, true);
    }

    public ZXingEngine(int sensitivity, boolean tryHarder) {
        this.sensitivity = sensitivity;
        this.tryHarder = tryHarder;
    }

    public static ZXingEngine create(android.content.Context context) {
        return new ZXingEngine(
                Prefs.getInt(context, Prefs.KEY_ZXING_SENSITIVITY, 2),
                Prefs.getBoolean(context, Prefs.KEY_ZXING_TRY_HARDER, true));
    }

    @Override
    public DecodeResult decode(ImageProxy image) {
        Image mediaImage = image.getImage();
        if (mediaImage == null) return null;
        int rotation = image.getImageInfo().getRotationDegrees();
        YuvUtils.Nv21 nv21 = YuvUtils.convert(mediaImage, rotation);
        if (nv21.data == null || nv21.data.length == 0) return null;

        DecodeResult first = decodeOnce(nv21.data, nv21.width, nv21.height);
        if (first != null) return first;

        if (sensitivity >= 3 && nv21.width >= 20 && nv21.height >= 20) {
            int sw = nv21.width / 2;
            int sh = nv21.height / 2;
            if (sw >= 20 && sh >= 20) {
                byte[] small = new byte[sw * sh];
                for (int y = 0; y < sh; y++) {
                    int srcY = y * 2 * nv21.width;
                    for (int x = 0; x < sw; x++) {
                        small[y * sw + x] = nv21.data[srcY + x * 2];
                    }
                }
                DecodeResult second = decodeOnce(small, sw, sh);
                if (second != null) return second;
            }
        }
        return null;
    }

    private DecodeResult decodeOnce(byte[] yuvData, int width, int height) {
        try {
            LuminanceSource source = new PlanarYUVLuminanceSource(
                    yuvData, width, height, 0, 0, width, height, false);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
            hints.put(DecodeHintType.POSSIBLE_FORMATS, FORMATS);
            if (tryHarder) hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

            MultiFormatReader reader = new MultiFormatReader();
            reader.setHints(hints);
            Result result = reader.decode(bitmap);
            if (result != null && result.getText() != null && !result.getText().isEmpty()) {
                return new DecodeResult(result.getText(), result.getBarcodeFormat().name());
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
