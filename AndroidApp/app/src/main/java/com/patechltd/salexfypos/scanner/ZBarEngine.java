package com.patechltd.salexfypos.scanner;

import android.media.Image;

import androidx.camera.core.ImageProxy;

import com.yanzhenjie.zbar.Config;
import com.yanzhenjie.zbar.ImageScanner;
import com.yanzhenjie.zbar.Symbol;
import com.yanzhenjie.zbar.SymbolSet;

public class ZBarEngine implements DecodeEngine {

    private final ImageScanner scanner;

    public ZBarEngine() {
        scanner = new ImageScanner();
        scanner.setConfig(Symbol.NONE, Config.ENABLE, 0);
        enable(Symbol.EAN13);
        enable(Symbol.EAN8);
        enable(Symbol.UPCA);
        enable(Symbol.UPCE);
        enable(Symbol.CODE128);
        enable(Symbol.CODE39);
        enable(Symbol.CODE93);
        enable(Symbol.I25);
        enable(Symbol.CODABAR);
        enable(Symbol.QRCODE);
        enable(Symbol.PDF417);
        scanner.setConfig(Symbol.NONE, Config.X_DENSITY, 2);
        scanner.setConfig(Symbol.NONE, Config.Y_DENSITY, 2);
    }

    private void enable(int symbology) {
        scanner.setConfig(symbology, Config.ENABLE, 1);
    }

    @Override
    public DecodeResult decode(ImageProxy image) {
        Image mediaImage = image.getImage();
        if (mediaImage == null) return null;
        int rotation = image.getImageInfo().getRotationDegrees();
        YuvUtils.Nv21 nv21 = YuvUtils.convert(mediaImage, rotation);
        if (nv21.data == null || nv21.data.length == 0) return null;

        com.yanzhenjie.zbar.Image zbarImage =
                new com.yanzhenjie.zbar.Image(nv21.width, nv21.height, "NV21");
        zbarImage.setData(nv21.data);
        try {
            if (scanner.scanImage(zbarImage) > 0) {
                SymbolSet symbols = scanner.getResults();
                for (Symbol symbol : symbols) {
                    String data = symbol.getData();
                    if (data != null && !data.isEmpty()) {
                        return new DecodeResult(data, symbolName(symbol.getType()));
                    }
                }
            }
        } finally {
            zbarImage.destroy();
        }
        return null;
    }

    private static String symbolName(int type) {
        switch (type) {
            case Symbol.EAN13: return "EAN_13";
            case Symbol.EAN8: return "EAN_8";
            case Symbol.UPCA: return "UPC_A";
            case Symbol.UPCE: return "UPC_E";
            case Symbol.CODE128: return "CODE_128";
            case Symbol.CODE39: return "CODE_39";
            case Symbol.CODE93: return "CODE_93";
            case Symbol.I25: return "ITF";
            case Symbol.CODABAR: return "CODABAR";
            case Symbol.QRCODE: return "QR_CODE";
            case Symbol.PDF417: return "PDF_417";
            default: return "UNKNOWN";
        }
    }
}
