package com.patechltd.salexfypos.ui.crash;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.patechltd.salexfypos.MainActivity;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.util.DialogUtil;
import com.patechltd.salexfypos.util.Prefs;

public class CrashRecoveryActivity extends AppCompatActivity {

    private String message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        message = Prefs.getString(this, Prefs.KEY_PENDING_CRASH, "");
        if (message == null || message.isEmpty()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_crash_recovery);

        TextView crashMessage = findViewById(R.id.crash_message);
        crashMessage.setText(message);

        Button btnCopy = findViewById(R.id.btn_copy_details);
        btnCopy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("Crash report", message));
            DialogUtil.toast(this, "Crash details copied");
        });

        Button btnTryAgain = findViewById(R.id.btn_try_again);
        btnTryAgain.setOnClickListener(v -> relaunch());
    }

    @Override
    public void onBackPressed() {
        relaunch();
    }

    private void relaunch() {
        Prefs.putString(this, Prefs.KEY_PENDING_CRASH, "");
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}
