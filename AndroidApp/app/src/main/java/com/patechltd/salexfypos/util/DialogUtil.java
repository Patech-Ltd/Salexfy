package com.patechltd.salexfypos.util;

import android.content.Context;
import android.text.InputType;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

public class DialogUtil {

    private DialogUtil() {
    }

    public static void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public interface OnPick {
        void onPick(int index);
    }

    public interface OnInput {
        void onInput(String value);
    }

    public static void pick(Context context, String title, String[] options, int selected, OnPick onPick) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setSingleChoiceItems(options, selected, (dialog, which) -> {
                    dialog.dismiss();
                    onPick.onPick(which);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public static void input(Context context, String title, String hint, String initial,
                             String positiveText, OnInput onInput) {
        TextInputEditText input = new TextInputEditText(context);
        input.setHint(hint);
        input.setText(initial == null ? "" : initial);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        int pad = (int) (20 * context.getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setView(input)
                .setPositiveButton(positiveText, (dialog, which) -> {
                    if (input.getText() != null) onInput.onInput(input.getText().toString());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public static void inputText(Context context, String title, String hint, String initial,
                                 String positiveText, OnInput onInput) {
        TextInputEditText input = new TextInputEditText(context);
        input.setHint(hint);
        input.setText(initial == null ? "" : initial);
        int pad = (int) (20 * context.getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setView(input)
                .setPositiveButton(positiveText, (dialog, which) -> {
                    if (input.getText() != null) onInput.onInput(input.getText().toString());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public static void confirm(Context context, String title, String message, Runnable onYes) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Yes", (dialog, which) -> onYes.run())
                .setNegativeButton("No", null)
                .show();
    }
}
