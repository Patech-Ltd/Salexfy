package com.patechltd.salexfypos.print;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.os.Looper;

import com.patechltd.salexfypos.db.entity.Purchase;
import com.patechltd.salexfypos.db.entity.PurchaseItem;
import com.patechltd.salexfypos.db.entity.Sale;
import com.patechltd.salexfypos.db.entity.SaleItem;
import com.patechltd.salexfypos.db.entity.SalePayment;
import com.patechltd.salexfypos.util.AppLogger;
import com.patechltd.salexfypos.util.Prefs;

import java.util.List;

/**
 * Prints sale receipts according to the configured printer in settings.
 * All I/O happens on a background thread; result is posted to the main thread.
 */
public final class PrinterManager {

    public interface PrintCallback {
        void onResult(boolean success, String message);
    }

    public static final String TYPE_NONE = "none";
    public static final String TYPE_BLUETOOTH = "bluetooth";
    public static final String TYPE_USB = "usb";

    private PrinterManager() {
    }

    public static boolean isPrintingEnabled(Context context) {
        return Prefs.getBoolean(context, Prefs.KEY_PRINT_RECEIPTS, false)
                && !TYPE_NONE.equals(Prefs.getString(context, Prefs.KEY_PRINTER_TYPE, TYPE_NONE));
    }

    public static void print(Context context, Sale sale, List<SaleItem> items) {
        print(context, sale, items, null, null);
    }

    public static void print(Context context, Sale sale, List<SaleItem> items, PrintCallback callback) {
        print(context, sale, items, null, callback);
    }

    public static void print(Context context, Sale sale, List<SaleItem> items,
                             List<SalePayment> payments) {
        print(context, sale, items, payments, null);
    }

    public static void print(Context context, Sale sale, List<SaleItem> items,
                             List<SalePayment> payments, PrintCallback callback) {
        print(context, sale, items, payments, 0, callback);
    }

    public static void print(Context context, Sale sale, List<SaleItem> items,
                             List<SalePayment> payments, double customerBalance, PrintCallback callback) {
        final Context app = context.getApplicationContext();
        new Thread(() -> {
            String type = Prefs.getString(app, Prefs.KEY_PRINTER_TYPE, TYPE_NONE);
            String message;
            boolean ok = false;
            try {
                byte[] data = ReceiptPrinter.buildReceiptBytes(app, sale, items, payments, customerBalance);
                if (TYPE_BLUETOOTH.equals(type)) {
                    String address = Prefs.getString(app, Prefs.KEY_PRINTER_BT_ADDRESS, "");
                    if (address.isEmpty()) throw new Exception("No Bluetooth printer selected");
                    BluetoothPrinter.print(address, data);
                } else if (TYPE_USB.equals(type)) {
                    int vendor = Prefs.getInt(app, Prefs.KEY_PRINTER_USB_VENDOR, 0);
                    int product = Prefs.getInt(app, Prefs.KEY_PRINTER_USB_PRODUCT, 0);
                    if (vendor == 0) throw new Exception("No USB printer selected");
                    UsbPrinter.print(app, vendor, product, data);
                } else {
                    throw new Exception("Receipt printing is disabled");
                }
                ok = true;
                message = "Receipt printed";
            } catch (Exception e) {
                message = "Print failed: " + e.getMessage();
                AppLogger.e("Print failed", e);
            }
            final boolean fOk = ok;
            final String fMessage = message;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (callback != null) callback.onResult(fOk, fMessage);
            });
        }).start();
    }

    public static void printPurchase(Context context, Purchase purchase,
                                     List<PurchaseItem> items, String supplierName,
                                     PrintCallback callback) {
        final Context app = context.getApplicationContext();
        new Thread(() -> {
            String type = Prefs.getString(app, Prefs.KEY_PRINTER_TYPE, TYPE_NONE);
            String message;
            boolean ok = false;
            try {
                byte[] data = ReceiptPrinter.buildPurchaseBytes(app, purchase, items, supplierName);
                if (TYPE_BLUETOOTH.equals(type)) {
                    String address = Prefs.getString(app, Prefs.KEY_PRINTER_BT_ADDRESS, "");
                    if (address.isEmpty()) throw new Exception("No Bluetooth printer selected");
                    BluetoothPrinter.print(address, data);
                } else if (TYPE_USB.equals(type)) {
                    int vendor = Prefs.getInt(app, Prefs.KEY_PRINTER_USB_VENDOR, 0);
                    int product = Prefs.getInt(app, Prefs.KEY_PRINTER_USB_PRODUCT, 0);
                    if (vendor == 0) throw new Exception("No USB printer selected");
                    UsbPrinter.print(app, vendor, product, data);
                } else {
                    throw new Exception("Printing is disabled");
                }
                ok = true;
                message = "Invoice printed";
            } catch (Exception e) {
                message = "Print failed: " + e.getMessage();
                AppLogger.e("Print failed", e);
            }
            final boolean fOk = ok;
            final String fMessage = message;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (callback != null) callback.onResult(fOk, fMessage);
            });
        }).start();
    }

    public static String deviceLabel(Context context, String type) {
        if (TYPE_BLUETOOTH.equals(type)) {
            return Prefs.getString(context, Prefs.KEY_PRINTER_BT_ADDRESS, "").isEmpty()
                    ? "Not selected" : Prefs.getString(context, Prefs.KEY_PRINTER_BT_ADDRESS, "");
        }
        if (TYPE_USB.equals(type)) {
            int vendor = Prefs.getInt(context, Prefs.KEY_PRINTER_USB_VENDOR, 0);
            int product = Prefs.getInt(context, Prefs.KEY_PRINTER_USB_PRODUCT, 0);
            if (vendor == 0) return "Not selected";
            return "USB " + Integer.toHexString(vendor) + ":" + Integer.toHexString(product);
        }
        return "Not selected";
    }

    public static String btDeviceName(Context context, String address) {
        if (address == null || address.isEmpty()) return "Not selected";
        for (com.patechltd.salexfypos.print.BtDeviceInfo d : BtDeviceInfo.paired(context)) {
            if (address.equals(d.address)) return d.name;
        }
        return address;
    }

    public static String usbDeviceName(Context context, UsbDevice d) {
        return d.getDeviceName() + " (" + Integer.toHexString(d.getVendorId())
                + ":" + Integer.toHexString(d.getProductId()) + ")";
    }
}
