package com.patechltd.salexfypos.scanner;

import android.media.Image;

import androidx.annotation.OptIn;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;

import com.google.android.gms.tasks.Tasks;

import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;

public class MLKitEngine implements DecodeEngine {

    private final BarcodeScanner scanner;

    public MLKitEngine() {
        scanner = BarcodeScanning.getClient(new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E,
                        Barcode.FORMAT_CODE_128,
                        Barcode.FORMAT_CODE_39,
                        Barcode.FORMAT_CODE_93,
                        Barcode.FORMAT_ITF,
                        Barcode.FORMAT_CODABAR,
                        Barcode.FORMAT_QR_CODE,
                        Barcode.FORMAT_DATA_MATRIX,
                        Barcode.FORMAT_PDF417)
                .build());
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    @Override
    public DecodeResult decode(ImageProxy image) {
        Image mediaImage = image.getImage();
        if (mediaImage == null) return null;
        try {
            int rotation = image.getImageInfo().getRotationDegrees();
            InputImage input = InputImage.fromMediaImage(mediaImage, rotation);
            List<Barcode> barcodes = Tasks.await(scanner.process(input));
            for (Barcode b : barcodes) {
                if (b == null) continue;
                String text = b.getRawValue();
                if (text == null || text.isEmpty()) text = b.getDisplayValue();
                if (text == null || text.isEmpty()) continue;
                return new DecodeResult(text, formatName(b.getFormat()));
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String formatName(int format) {
        switch (format) {
            case Barcode.FORMAT_EAN_13: return "EAN_13";
            case Barcode.FORMAT_EAN_8: return "EAN_8";
            case Barcode.FORMAT_UPC_A: return "UPC_A";
            case Barcode.FORMAT_UPC_E: return "UPC_E";
            case Barcode.FORMAT_CODE_128: return "CODE_128";
            case Barcode.FORMAT_CODE_39: return "CODE_39";
            case Barcode.FORMAT_CODE_93: return "CODE_93";
            case Barcode.FORMAT_ITF: return "ITF";
            case Barcode.FORMAT_CODABAR: return "CODABAR";
            case Barcode.FORMAT_QR_CODE: return "QR_CODE";
            case Barcode.FORMAT_DATA_MATRIX: return "DATA_MATRIX";
            case Barcode.FORMAT_PDF417: return "PDF_417";
            case Barcode.FORMAT_AZTEC: return "AZTEC";
            default: return "UNKNOWN";
        }
    }
}
