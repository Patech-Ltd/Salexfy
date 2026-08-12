package com.patechltd.salexfypos.scanner;

import android.content.Context;

import com.patechltd.salexfypos.util.Prefs;

public final class ScannerEngines {

    public static final String ENGINE_MLKIT = "mlkit";
    public static final String ENGINE_ZBAR = "zbar";
    public static final String ENGINE_OPENCV_QR = "opencv_qr";
    public static final String ENGINE_ZXING = "zxing";

    private ScannerEngines() {
    }

    public static DecodeEngine create(Context context) {
        String id = Prefs.getString(context, Prefs.KEY_SCANNER_ENGINE, ENGINE_MLKIT);
        if (ENGINE_MLKIT.equals(id)) return new MLKitEngine();
        if (ENGINE_OPENCV_QR.equals(id)) return new OpenCVQREngine();
        if (ENGINE_ZXING.equals(id)) return ZXingEngine.create(context);
        return new ZBarEngine();
    }
}
