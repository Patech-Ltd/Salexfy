package com.patechltd.salexfypos.ui.scan;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.scanner.ScannerView;
import com.patechltd.salexfypos.util.Prefs;

/**
 * Full-screen barcode scanner. Returns the decoded value via RESULT_OK and
 * the {@link #EXTRA_CODE} extra. Launch with startActivityForResult from any
 * screen that needs a scan (search fields, barcode inputs, purchase lines...).
 */
public class ScanActivity extends AppCompatActivity {

    public static final String EXTRA_CODE = "scan_code";
    private ScannerView scanner;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        scanner = findViewById(R.id.scanner);
        TextView status = findViewById(R.id.scan_status);
        status.setText("Point the camera at a barcode or QR code");

        findViewById(R.id.btn_close_scan).setOnClickListener(v -> finish());

        scanner.setOnScanListener((text, format) -> {
            scanner.stop();
            Intent result = new Intent();
            result.putExtra(EXTRA_CODE, text);
            setResult(RESULT_OK, result);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1005);
            return;
        }
        scanner.start(this);
        if (Prefs.getBoolean(this, Prefs.KEY_TORCH_ON_START, false)) {
            handler.postDelayed(() -> scanner.setTorch(true), 800);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanner.stop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1005 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            scanner.start(this);
        }
    }
}
