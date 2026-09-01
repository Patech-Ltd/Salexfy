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
        createReportUri(activity, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx", "sales_report_" + stamp);
    }

    public static void createReportUri(Activity activity, String mime, String extension, String stamp) {
        activity.startActivityForResult(createReportIntent(mime, extension, stamp), REQ_CREATE_REPORT);
    }

    public static Intent createReportIntent(String mime, String extension, String stamp) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType(mime);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_TITLE, "report_" + stamp + "." + extension);
        return intent;
    }
}
