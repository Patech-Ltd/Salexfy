package com.patechltd.salexfypos.ui.settings;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.scanner.ScannerEngines;
import com.patechltd.salexfypos.util.DialogUtil;
import com.patechltd.salexfypos.util.Prefs;

public class ScannerSettingsActivity extends AppCompatActivity {

    private MaterialButtonToggleGroup scannerEngineGroup;
    private MaterialButtonToggleGroup zxingSensitivityGroup;
    private SwitchMaterial zxingTryHarder;
    private SwitchMaterial beep, torch, autoSuspend;
    private TextInputEditText repeatDelayInput;
    private View zxingSection;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner_settings);

        ((com.google.android.material.appbar.MaterialToolbar) findViewById(R.id.toolbar))
                .setNavigationOnClickListener(v -> finish());

        scannerEngineGroup = findViewById(R.id.scanner_engine_group);
        zxingSensitivityGroup = findViewById(R.id.zxing_sensitivity_group);
        zxingTryHarder = findViewById(R.id.switch_zxing_try_harder);
        zxingSection = findViewById(R.id.zxing_section);
        beep = findViewById(R.id.switch_beep);
        torch = findViewById(R.id.switch_torch);
        autoSuspend = findViewById(R.id.switch_auto_suspend);
        repeatDelayInput = findViewById(R.id.repeat_delay);

        scannerEngineGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            updateZxingVisibility();
        });

        load();

        ((MaterialButton) findViewById(R.id.btn_save)).setOnClickListener(v -> save());
    }

    private void load() {
        beep.setChecked(Prefs.getBoolean(this, Prefs.KEY_BEEP, true));
        torch.setChecked(Prefs.getBoolean(this, Prefs.KEY_TORCH_ON_START, false));
        autoSuspend.setChecked(Prefs.getBoolean(this, Prefs.KEY_AUTO_SUSPEND_PREVIEW, true));
        String engine = Prefs.getString(this, Prefs.KEY_SCANNER_ENGINE, ScannerEngines.ENGINE_MLKIT);
        if (ScannerEngines.ENGINE_MLKIT.equals(engine)) {
            scannerEngineGroup.check(R.id.btn_engine_mlkit);
        } else if (ScannerEngines.ENGINE_OPENCV_QR.equals(engine)) {
            scannerEngineGroup.check(R.id.btn_engine_opencv);
        } else if (ScannerEngines.ENGINE_ZXING.equals(engine)) {
            scannerEngineGroup.check(R.id.btn_engine_zxing);
        } else {
            scannerEngineGroup.check(R.id.btn_engine_zbar);
        }
        int sensitivity = Prefs.getInt(this, Prefs.KEY_ZXING_SENSITIVITY, 2);
        zxingSensitivityGroup.check(sensitivity <= 1 ? R.id.btn_zxing_low
                : sensitivity >= 3 ? R.id.btn_zxing_high : R.id.btn_zxing_medium);
        zxingTryHarder.setChecked(Prefs.getBoolean(this, Prefs.KEY_ZXING_TRY_HARDER, true));
        repeatDelayInput.setText(String.valueOf(Prefs.getLong(this, Prefs.KEY_SCAN_REPEAT_DELAY_MS, 1000)));
        updateZxingVisibility();
    }

    private void updateZxingVisibility() {
        boolean zxing = scannerEngineGroup.getCheckedButtonId() == R.id.btn_engine_zxing;
        zxingSection.setVisibility(zxing ? View.VISIBLE : View.GONE);
    }

    private void save() {
        Prefs.putBoolean(this, Prefs.KEY_BEEP, beep.isChecked());
        Prefs.putBoolean(this, Prefs.KEY_TORCH_ON_START, torch.isChecked());
        Prefs.putBoolean(this, Prefs.KEY_AUTO_SUSPEND_PREVIEW, autoSuspend.isChecked());
        int engineId = scannerEngineGroup.getCheckedButtonId();
        if (engineId == R.id.btn_engine_mlkit) {
            Prefs.putString(this, Prefs.KEY_SCANNER_ENGINE, ScannerEngines.ENGINE_MLKIT);
        } else if (engineId == R.id.btn_engine_opencv) {
            Prefs.putString(this, Prefs.KEY_SCANNER_ENGINE, ScannerEngines.ENGINE_OPENCV_QR);
        } else if (engineId == R.id.btn_engine_zxing) {
            Prefs.putString(this, Prefs.KEY_SCANNER_ENGINE, ScannerEngines.ENGINE_ZXING);
        } else {
            Prefs.putString(this, Prefs.KEY_SCANNER_ENGINE, ScannerEngines.ENGINE_ZBAR);
        }
        int sensId = zxingSensitivityGroup.getCheckedButtonId();
        Prefs.putInt(this, Prefs.KEY_ZXING_SENSITIVITY,
                sensId == R.id.btn_zxing_low ? 1 : sensId == R.id.btn_zxing_high ? 3 : 2);
        Prefs.putBoolean(this, Prefs.KEY_ZXING_TRY_HARDER, zxingTryHarder.isChecked());
        long delay = 1000;
        try {
            delay = Long.parseLong(repeatDelayInput.getText() == null ? ""
                    : repeatDelayInput.getText().toString().trim());
        } catch (NumberFormatException ignored) {
        }
        if (delay < 0) delay = 0;
        Prefs.putLong(this, Prefs.KEY_SCAN_REPEAT_DELAY_MS, delay);
        DialogUtil.toast(this, "Settings saved");
        finish();
    }
}
