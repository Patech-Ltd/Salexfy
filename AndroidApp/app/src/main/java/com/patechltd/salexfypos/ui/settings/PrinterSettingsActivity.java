package com.patechltd.salexfypos.ui.settings;

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
import com.patechltd.salexfypos.print.BtDeviceInfo;
import com.patechltd.salexfypos.print.PrinterManager;
import com.patechltd.salexfypos.print.UsbPrinter;
import com.patechltd.salexfypos.util.DialogUtil;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.Prefs;

import java.util.List;

public class PrinterSettingsActivity extends AppCompatActivity {

    private SwitchMaterial printReceipts;
    private MaterialButtonToggleGroup printerTypeGroup;
    private TextView btSelected, usbSelected;
    private View btRow, usbRow;
    private TextInputEditText printerWidth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_printer_settings);

        findViewById(R.id.toolbar).setOnClickListener(v -> onBackPressed());

        printReceipts = findViewById(R.id.switch_print_receipts);
        printerTypeGroup = findViewById(R.id.printer_type_group);
        btSelected = findViewById(R.id.bt_selected);
        usbSelected = findViewById(R.id.usb_selected);
        btRow = findViewById(R.id.bt_row);
        usbRow = findViewById(R.id.usb_row);
        printerWidth = findViewById(R.id.input_printer_width);

        btRow.setOnClickListener(v -> pickBluetoothPrinter());
        usbRow.setOnClickListener(v -> pickUsbPrinter());
        printerTypeGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            updatePrinterVisibility();
        });

        load();

        ((MaterialButton) findViewById(R.id.btn_save)).setOnClickListener(v -> save());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPrinterRows();
    }

    private void load() {
        printReceipts.setChecked(Prefs.getBoolean(this, Prefs.KEY_PRINT_RECEIPTS, false));
        printerWidth.setText(String.valueOf(Prefs.getInt(this, Prefs.KEY_PRINTER_WIDTH, 32)));
        refreshPrinterRows();
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

    private void save() {
        Prefs.putBoolean(this, Prefs.KEY_PRINT_RECEIPTS, printReceipts.isChecked());
        int checkedId = printerTypeGroup.getCheckedButtonId();
        String type;
        if (checkedId == R.id.btn_printer_bluetooth) type = PrinterManager.TYPE_BLUETOOTH;
        else if (checkedId == R.id.btn_printer_usb) type = PrinterManager.TYPE_USB;
        else type = PrinterManager.TYPE_NONE;
        Prefs.putString(this, Prefs.KEY_PRINTER_TYPE, type);
        int width = (int) NumberUtil.parse(
                printerWidth.getText() == null ? "" : printerWidth.getText().toString(), 32);
        if (width < 20) width = 32;
        Prefs.putInt(this, Prefs.KEY_PRINTER_WIDTH, width);
        DialogUtil.toast(this, "Settings saved");
        finish();
    }
}
