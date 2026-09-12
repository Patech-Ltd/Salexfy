package com.patechltd.salexfypos.util;

import android.graphics.Rect;
import android.view.View;

/**
 * Per-page soft-keyboard handling. Attach to a page's root view and, while the
 * keyboard is open, the root is padded by the keyboard height so the page
 * content stays visible above the keyboard. Only the view it is attached to is
 * affected; every other page is untouched.
 */
public final class KeyboardUtil {

    private KeyboardUtil() { }

    public static void makeAdjustResize(View root) {
        root.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect rect = new Rect();
            root.getWindowVisibleDisplayFrame(rect);
            int keyboard = root.getRootView().getHeight() - rect.bottom;
            int threshold = Math.round(root.getResources().getDisplayMetrics().density * 120);
            root.setPadding(0, 0, 0, keyboard > threshold ? keyboard : 0);
        });
    }
}