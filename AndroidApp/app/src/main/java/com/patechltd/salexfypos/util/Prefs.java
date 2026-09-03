package com.patechltd.salexfypos.util;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {

    public static final String KEY_SHOP_NAME = "shop_name";
    public static final String KEY_SHOP_ADDRESS = "shop_address";
    public static final String KEY_SHOP_PHONE = "shop_phone";
    public static final String KEY_CURRENCY = "currency";
    public static final String KEY_TAX_PERCENT = "tax_percent";
    public static final String KEY_BEEP = "beep_enabled";
    public static final String KEY_TORCH_ON_START = "torch_on_start";
    public static final String KEY_AUTO_SUSPEND_PREVIEW = "auto_suspend_preview";
    public static final String KEY_BACKUP_ENABLED = "auto_backup_enabled";
    public static final String KEY_BACKUP_INTERVAL_HOURS = "auto_backup_interval";
    public static final String KEY_BACKUP_FOLDER = "backup_folder";
    public static final String KEY_DEBUG_MODE = "debug_mode";
    public static final String KEY_APP_LOCK = "app_lock_enabled";
    public static final String KEY_APP_LOCK_USE_BIOMETRIC = "app_lock_use_biometric";
    public static final String KEY_LAST_BACKUP = "last_backup_time";
    public static final String KEY_SEEDED = "db_seeded";
    public static final String KEY_RECEIPT_FOOTER = "receipt_footer";
    public static final String KEY_DEFAULT_CUSTOMER = "default_customer";
    public static final String KEY_PRINT_RECEIPTS = "print_receipts";
    public static final String KEY_PRINTER_TYPE = "printer_type";
    public static final String KEY_PRINTER_BT_ADDRESS = "printer_bt_address";
    public static final String KEY_PRINTER_USB_VENDOR = "printer_usb_vendor";
    public static final String KEY_PRINTER_USB_PRODUCT = "printer_usb_product";
    public static final String KEY_PRINTER_WIDTH = "printer_width";
    public static final String KEY_SCANNER_ENGINE = "scanner_engine";
    public static final String KEY_SCAN_REPEAT_DELAY_MS = "scan_repeat_delay_ms";
    public static final String KEY_PENDING_CRASH = "pending_crash";
    public static final String KEY_ZXING_SENSITIVITY = "zxing_sensitivity";
    public static final String KEY_ZXING_TRY_HARDER = "zxing_try_harder";
    public static final String KEY_LOYALTY_POINTS_PER_MONEY = "loyalty_points_per_money";
    public static final String KEY_LOYALTY_POINT_VALUE = "loyalty_point_value";
    public static final String KEY_BACKUP_DRIVE_URI = "backup_drive_uri";
    public static final String KEY_BACKUP_DRIVE_ENABLED = "backup_drive_enabled";
    public static final String KEY_BACKUP_DRIVE_EMAIL = "backup_drive_email";
    public static final String KEY_LAST_DRIVE_BACKUP = "last_drive_backup_time";
    public static final String KEY_SYNC_ENABLED = "sync_enabled";
    public static final String KEY_SYNC_SERVER_URL = "sync_server_url";
    public static final String KEY_SYNC_USERNAME = "sync_username";
    public static final String KEY_SYNC_PASSWORD = "sync_password";
    public static final String KEY_SYNC_INTERVAL_MINUTES = "sync_interval_minutes";
    public static final String KEY_SYNC_LAST_SYNC = "last_sync_time";
    public static final String KEY_SYNC_PULL_CURSOR = "sync_pull_cursor";
    public static final String KEY_DEVICE_ID = "device_id";
    public static final String KEY_APP_LOCK_NEEDS_REAUTH = "app_lock_needs_reauth";
    public static final String KEY_APP_LOCK_LAST_UNLOCK = "app_lock_last_unlock";
    public static final String KEY_APP_LOCK_TIMEOUT_MS = "app_lock_timeout_ms";
    public static final String KEY_LICENSE_KEY = "license_key";
    public static final String KEY_LICENSE_EXPIRY = "license_expiry";
    public static final String KEY_SHOP_ID = "shop_id";

    private static final String PREFS = "salexfy_prefs";

    private Prefs() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String getString(Context context, String key, String def) {
        return prefs(context).getString(key, def);
    }

    public static void putString(Context context, String key, String value) {
        prefs(context).edit().putString(key, value).apply();
    }

    public static boolean getBoolean(Context context, String key, boolean def) {
        return prefs(context).getBoolean(key, def);
    }

    public static void putBoolean(Context context, String key, boolean value) {
        prefs(context).edit().putBoolean(key, value).apply();
    }

    public static long getLong(Context context, String key, long def) {
        return prefs(context).getLong(key, def);
    }

    public static void putLong(Context context, String key, long value) {
        prefs(context).edit().putLong(key, value).apply();
    }

    public static int getInt(Context context, String key, int def) {
        return prefs(context).getInt(key, def);
    }

    public static void putInt(Context context, String key, int value) {
        prefs(context).edit().putInt(key, value).apply();
    }

    public static double getDouble(Context context, String key, double def) {
        String v = getString(context, key, String.valueOf(def));
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static void putDouble(Context context, String key, double value) {
        putString(context, key, String.valueOf(value));
    }

    public static String currency(Context context) {
        return getString(context, KEY_CURRENCY, "KSh");
    }

    public static double taxPercent(Context context) {
        return getDouble(context, KEY_TAX_PERCENT, TaxUtil.TOT_RATE);
    }
}
