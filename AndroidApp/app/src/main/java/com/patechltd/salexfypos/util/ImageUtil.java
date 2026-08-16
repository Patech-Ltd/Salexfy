package com.patechltd.salexfypos.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ImageUtil {

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private ImageUtil() {
    }

    public static File productImageDir(Context context) {
        java.io.File base = context.getExternalFilesDir(null);
        if (base == null) base = context.getFilesDir();
        File dir = new File(base, "product_images");
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
    }

    public static String copyImage(Context context, Uri source, String fileName) {
        File dest = new File(productImageDir(context), fileName + ".jpg");
        try (InputStream in = context.getContentResolver().openInputStream(source);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buffer = new byte[16384];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return dest.getAbsolutePath();
        } catch (IOException e) {
            AppLogger.e("ImageUtil.copyImage failed", e);
            return null;
        }
    }

    public static Bitmap decode(String path, int targetPx) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, opts);
        int sample = 1;
        while (opts.outWidth / (sample * 2) >= targetPx
                || opts.outHeight / (sample * 2) >= targetPx) {
            sample *= 2;
        }
        opts.inJustDecodeBounds = false;
        opts.inSampleSize = sample;
        return BitmapFactory.decodeFile(path, opts);
    }

    public static void load(ImageView view, String path, int targetPx) {
        if (path == null || path.isEmpty()) {
            view.setTag(null);
            view.setImageResource(com.patechltd.salexfypos.R.drawable.ic_image);
            return;
        }
        if (view.getTag() == null || !path.equals(view.getTag())) {
            view.setImageResource(com.patechltd.salexfypos.R.drawable.ic_image);
        }
        IO.execute(() -> {
            Bitmap bmp = decode(path, targetPx);
            MAIN.post(() -> {
                if (view.getTag() == null || !path.equals(view.getTag())) {
                    return;
                }
                if (bmp != null) {
                    view.setImageBitmap(bmp);
                } else {
                    view.setImageResource(com.patechltd.salexfypos.R.drawable.ic_image);
                }
            });
        });
    }
}
