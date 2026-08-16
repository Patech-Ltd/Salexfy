package com.patechltd.salexfypos.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

public class StorageUtil {

    public static final int REQ_CREATE_REPORT = 401;
    public static final int REQ_BACKUP = 402;
    public static final int REQ_DRIVE_FOLDER = 403;

    private StorageUtil() {
    }

    public static void createReportUri(Activity activity, String stamp) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("application/vnd.ms-excel");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_TITLE, "sales_report_" + stamp + ".xls");
        activity.startActivityForResult(intent, REQ_CREATE_REPORT);
    }
}
