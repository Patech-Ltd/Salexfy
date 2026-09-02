package com.patechltd.salexfypos.scanner;

/*
 * COMMENTED OUT with OpenCV (to shrink APK size). Restore by removing the
 * surrounding block comment and uncommenting `implementation libs.opencv` in
 * app/build.gradle, the opencv entries in gradle/libs.versions.toml, the
 * btn_engine_opencv button in activity_scanner_settings.xml and the OpenCV
 * branches in ScannerEngines.java / ScannerSettingsActivity.java.
 *
import android.media.Image;

import androidx.camera.core.ImageProxy;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.objdetect.QRCodeDetector;

public class OpenCVQREngine implements DecodeEngine {

    private QRCodeDetector detector;
    private boolean ready;

    public OpenCVQREngine() {
        try {
            ready = OpenCVLoader.initLocal();
            if (ready) detector = new QRCodeDetector();
        } catch (Throwable t) {
            ready = false;
            detector = null;
        }
    }

    @Override
    public DecodeResult decode(ImageProxy image) {
        if (!ready || detector == null) return null;
        Image mediaImage = image.getImage();
        if (mediaImage == null) return null;
        int rotation = image.getImageInfo().getRotationDegrees();
        YuvUtils.Nv21 nv21 = YuvUtils.convert(mediaImage, rotation);
        if (nv21.data == null || nv21.data.length == 0) return null;

        Mat gray = null;
        try {
            gray = new Mat(nv21.height, nv21.width, CvType.CV_8UC1);
            gray.put(0, 0, nv21.data, 0, nv21.width * nv21.height);
            String text = detector.detectAndDecode(gray);
            if (text != null && !text.isEmpty()) {
                return new DecodeResult(text, "QR_CODE");
            }
        } catch (Throwable ignored) {
        } finally {
            if (gray != null) gray.release();
        }
        return null;
    }
}
*/
