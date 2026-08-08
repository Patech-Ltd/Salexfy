package com.patechltd.salexfypos.ui.settings;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.backup.BackupManager;
import com.patechltd.salexfypos.print.BtDeviceInfo;
import com.patechltd.salexfypos.print.PrinterManager;
import com.patechltd.salexfypos.print.UsbPrinter;
import com.patechltd.salexfypos.scanner.ScannerEngines;
import com.patechltd.salexfypos.util.DateUtil;
import com.patechltd.salexfypos.util.DialogUtil;
import com.patechltd.salexfypos.util.Prefs;
import com.patechltd.salexfypos.util.StorageUtil;

import java.io.File;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private TextInputEditText shopName, phone, currency, tax, footer, printerWidth, loyaltyPoints, loyaltyValue;
    private SwitchMaterial beep, torch, autoBackup, printReceipts;
    private TextView lastBackup, btSelected, usbSelected;
    private MaterialButtonToggleGroup printerTypeGroup;
    private MaterialButtonToggleGroup scannerEngineGroup;
    private MaterialButtonToggleGroup zxingSensitivityGroup;
    private View btRow, usbRow, zxingSection;
    private SwitchMaterial zxingTryHarder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.toolbar).setOnClickListener(v -> onBackPressed());

        shopName = findViewById(R.id.input_shop_name);
        phone = findViewById(R.id.input_phone);
        currency = findViewById(R.id.input_currency);
        tax = findViewById(R.id.input_tax);
        footer = findViewById(R.id.input_footer);
        beep = findViewById(R.id.switch_beep);
        torch = findViewById(R.id.switch_torch);
        autoBackup = findViewById(R.id.switch_auto_backup);
        printReceipts = findViewById(R.id.switch_print_receipts);
        printerTypeGroup = findViewById(R.id.printer_type_group);
        scannerEngineGroup = findViewById(R.id.scanner_engine_group);
        zxingSensitivityGroup = findViewById(R.id.zxing_sensitivity_group);
        zxingTryHarder = findViewById(R.id.switch_zxing_try_harder);
        zxingSection = findViewById(R.id.zxing_section);
        btSelected = findViewById(R.id.bt_selected);
        usbSelected = findViewById(R.id.usb_selected);
        btRow = findViewById(R.id.bt_row);
        usbRow = findViewById(R.id.usb_row);
        printerWidth = findViewById(R.id.input_printer_width);
        lastBackup = findViewById(R.id.last_backup);
        loyaltyPoints = findViewById(R.id.input_loyalty_points);
        loyaltyValue = findViewById(R.id.input_loyalty_value);

        btRow.setOnClickListener(v -> pickBluetoothPrinter());
        usbRow.setOnClickListener(v -> pickUsbPrinter());
        printerTypeGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            updatePrinterVisibility();
        });
        scannerEngineGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            updateZxingVisibility();
        });

        load();

        findViewById(R.id.btn_save).setOnClickListener(v -> save());
        findViewById(R.id.btn_backup_now).setOnClickListener(v -> backupNow());
        findViewById(R.id.btn_restore).setOnClickListener(v -> restore());

        if (getIntent().getBooleanExtra("openBackup", false)) {
            autoBackup.requestFocus();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPrinterRows();
    }

    private void load() {
        shopName.setText(Prefs.getString(this, Prefs.KEY_SHOP_NAME, "My Shop"));
        phone.setText(Prefs.getString(this, Prefs.KEY_SHOP_PHONE, ""));
        currency.setText(Prefs.currency(this));
        tax.setText(String.valueOf(Prefs.taxPercent(this)));
        footer.setText(Prefs.getString(this, Prefs.KEY_RECEIPT_FOOTER, "Thank you!"));
        beep.setChecked(Prefs.getBoolean(this, Prefs.KEY_BEEP, true));
        torch.setChecked(Prefs.getBoolean(this, Prefs.KEY_TORCH_ON_START, false));
        autoBackup.setChecked(Prefs.getBoolean(this, Prefs.KEY_BACKUP_ENABLED, true));
        printReceipts.setChecked(Prefs.getBoolean(this, Prefs.KEY_PRINT_RECEIPTS, false));
        printerWidth.setText(String.valueOf(Prefs.getInt(this, Prefs.KEY_PRINTER_WIDTH, 32)));
        loyaltyPoints.setText(String.valueOf(Prefs.getDouble(this, Prefs.KEY_LOYALTY_POINTS_PER_MONEY, 1.0)));
        loyaltyValue.setText(String.valueOf(Prefs.getDouble(this, Prefs.KEY_LOYALTY_POINT_VALUE, 0.5)));
        updateLastBackup();
        refreshPrinterRows();
        refreshScannerEngine();
    }

    private void refreshScannerEngine() {
        String engine = Prefs.getString(this, Prefs.KEY_SCANNER_ENGINE, ScannerEngines.ENGINE_ZBAR);
        if (ScannerEngines.ENGINE_OPENCV_QR.equals(engine)) {
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
        updateZxingVisibility();
    }

    private void updateZxingVisibility() {
        boolean zxing = scannerEngineGroup.getCheckedButtonId() == R.id.btn_engine_zxing;
        zxingSection.setVisibility(zxing ? View.VISIBLE : View.GONE);
    }

    private void refreshPrinterRows() {
        String type = Prefs.getString(this, Prefs.KEY_PRINTER_TYPE, PrinterManager.TYPE_NONE);
        int id;
        if (PrinterManager.TYPE_BLUETOOTH.equals(type)) id = R.id.btn_printer_bluetooth;
        else if (PrinterManager.TYPE_USB.equals(type)) id = R.id.btn_printer_usb;
        else id = R.id.btn_printer_none;
        printerTypeGroup.check(id);

        String btAddress = Prefs.getString(this, Prefs.KEY_PRINTER_BT_ADDRESS, "");
        btSelected.setText(btAddress.isEmpty() ? "Not selected"
                : PrinterManager.btDeviceName(this, btAddress));
        int vendor = Prefs.getInt(this, Prefs.KEY_PRINTER_USB_VENDOR, 0);
        int product = Prefs.getInt(this, Prefs.KEY_PRINTER_USB_PRODUCT, 0);
        if (vendor == 0) {
            usbSelected.setText("Not selected");
        } else {
            usbSelected.setText(String.format("USB %04x:%04x", vendor, product));
        }
        updatePrinterVisibility();
    }

    private void updatePrinterVisibility() {
        int checkedId = printerTypeGroup.getCheckedButtonId();
        boolean bt = checkedId == R.id.btn_printer_bluetooth;
        boolean usb = checkedId == R.id.btn_printer_usb;
        btRow.setVisibility(bt ? View.VISIBLE : View.GONE);
        usbRow.setVisibility(usb ? View.VISIBLE : View.GONE);
    }

    private void pickBluetoothPrinter() {
        if (Build.VERSION.SDK_INT >= 31 && ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 2001);
            return;
        }
        showBluetoothPicker();
    }

    private void showBluetoothPicker() {
        List<BtDeviceInfo> paired = BtDeviceInfo.paired(this);
        if (paired.isEmpty()) {
            DialogUtil.toast(this, "No paired Bluetooth devices. Pair the printer in Android Bluetooth settings first.");
            return;
        }
        String[] options = new String[paired.size()];
        String[] addresses = new String[paired.size()];
        for (int i = 0; i < paired.size(); i++) {
            options[i] = paired.get(i).name;
            addresses[i] = paired.get(i).address;
        }
        DialogUtil.pick(this, "Select Bluetooth printer", options, -1, which -> {
            Prefs.putString(this, Prefs.KEY_PRINTER_BT_ADDRESS, addresses[which]);
            refreshPrinterRows();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 2001 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showBluetoothPicker();
        }
    }

    private void pickUsbPrinter() {
        List<UsbDevice> printers = UsbPrinter.listPrinters(this);
        if (printers.isEmpty()) {
            DialogUtil.toast(this, "No USB printer detected. Plug it in and grant permission when prompted.");
            return;
        }
        String[] options = new String[printers.size()];
        for (int i = 0; i < printers.size(); i++) {
            UsbDevice d = printers.get(i);
            options[i] = String.format("%s (%04x:%04x)", d.getDeviceName(), d.getVendorId(), d.getProductId());
        }
        DialogUtil.pick(this, "Select USB printer", options, -1, which -> {
            UsbDevice d = printers.get(which);
            Prefs.putInt(this, Prefs.KEY_PRINTER_USB_VENDOR, d.getVendorId());
            Prefs.putInt(this, Prefs.KEY_PRINTER_USB_PRODUCT, d.getProductId());
            refreshPrinterRows();
        });
    }

    private void updateLastBackup() {
        long last = Prefs.getLong(this, Prefs.KEY_LAST_BACKUP, 0);
        lastBackup.setText("Last backup: " + (last == 0 ? "Never" : DateUtil.formatDate(last) + " " + DateUtil.formatTime(last)));
    }

    private void save() {
        Prefs.putString(this, Prefs.KEY_SHOP_NAME, shopName.getText() == null ? "" : shopName.getText().toString().trim());
        Prefs.putString(this, Prefs.KEY_SHOP_PHONE, phone.getText() == null ? "" : phone.getText().toString().trim());
        Prefs.putString(this, Prefs.KEY_CURRENCY, currency.getText() == null ? "KSh" : currency.getText().toString().trim());
        Prefs.putString(this, Prefs.KEY_RECEIPT_FOOTER, footer.getText() == null ? "" : footer.getText().toString().trim());
        Prefs.putBoolean(this, Prefs.KEY_BEEP, beep.isChecked());
        Prefs.putBoolean(this, Prefs.KEY_TORCH_ON_START, torch.isChecked());
        Prefs.putBoolean(this, Prefs.KEY_BACKUP_ENABLED, autoBackup.isChecked());
        Prefs.putBoolean(this, Prefs.KEY_PRINT_RECEIPTS, printReceipts.isChecked());
        int checkedId = printerTypeGroup.getCheckedButtonId();
        String type;
        if (checkedId == R.id.btn_printer_bluetooth) type = PrinterManager.TYPE_BLUETOOTH;
        else if (checkedId == R.id.btn_printer_usb) type = PrinterManager.TYPE_USB;
        else type = PrinterManager.TYPE_NONE;
        Prefs.putString(this, Prefs.KEY_PRINTER_TYPE, type);
        int width = (int) com.patechltd.salexfypos.util.NumberUtil.parse(
                printerWidth.getText() == null ? "" : printerWidth.getText().toString(), 32);
        if (width < 20) width = 32;
        Prefs.putInt(this, Prefs.KEY_PRINTER_WIDTH, width);
        int engineId = scannerEngineGroup.getCheckedButtonId();
        if (engineId == R.id.btn_engine_opencv) {
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
        double t = com.patechltd.salexfypos.util.NumberUtil.parse(tax.getText() == null ? "" : tax.getText().toString(), 0);
        Prefs.putDouble(this, Prefs.KEY_TAX_PERCENT, t);
        double pp = com.patechltd.salexfypos.util.NumberUtil.parse(
                loyaltyPoints.getText() == null ? "" : loyaltyPoints.getText().toString(), 1.0);
        Prefs.putDouble(this, Prefs.KEY_LOYALTY_POINTS_PER_MONEY, Math.max(0, pp));
        double pv = com.patechltd.salexfypos.util.NumberUtil.parse(
                loyaltyValue.getText() == null ? "" : loyaltyValue.getText().toString(), 0.5);
        Prefs.putDouble(this, Prefs.KEY_LOYALTY_POINT_VALUE, Math.max(0, pv));
        DialogUtil.toast(this, "Settings saved");
    }

    private void backupNow() {
        final SettingsActivity self = this;
        new Thread(() -> {
            File target = BackupManager.timestampedBackupFile(self);
            boolean ok = BackupManager.backupToFile(self, target);
            runOnUiThread(() -> {
                updateLastBackup();
                DialogUtil.toast(self, ok ? "Backup saved to internal storage" : "Backup failed");
            });
        }).start();
    }

    private void restore() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, StorageUtil.REQ_BACKUP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == StorageUtil.REQ_BACKUP && resultCode == RESULT_OK && data != null && data.getData() != null) {
            final android.net.Uri uri = data.getData();
            DialogUtil.confirm(this, "Restore database",
                    "This will REPLACE all current data with the backup. Continue?",
                    () -> new Thread(() -> {
                        boolean ok = BackupManager.restoreFromUri(SettingsActivity.this, uri);
                        runOnUiThread(() -> DialogUtil.toast(SettingsActivity.this,
                                ok ? "Database restored. Restart the app." : "Restore failed"));
                    }).start());
        }
    }
}
