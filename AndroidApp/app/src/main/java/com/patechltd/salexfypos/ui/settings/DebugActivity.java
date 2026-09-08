package com.patechltd.salexfypos.ui.settings;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.adapter.KeyValueAdapter;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.CrashLog;
import com.patechltd.salexfypos.util.AppLogger;
import com.patechltd.salexfypos.util.DateUtil;
import com.patechltd.salexfypos.util.DialogUtil;
import com.patechltd.salexfypos.util.Prefs;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DebugActivity extends AppCompatActivity {

    private static final int REQUEST_SAVE_CRASH = 1001;

    private Repository repo;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private KeyValueAdapter adapter;
    private TextView logView;
    private List<CrashLog> crashes = new ArrayList<>();
    private CrashLog pendingDownload;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);
        repo = Repository.get(this);

        ((com.google.android.material.appbar.MaterialToolbar) findViewById(R.id.toolbar))
                .setNavigationOnClickListener(v -> finish());

        adapter = new KeyValueAdapter();
        adapter.setListener(this::onCrashRowClicked);
        RecyclerView list = findViewById(R.id.crash_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        logView = findViewById(R.id.log_view);

        SwitchMaterial debug = findViewById(R.id.switch_debug);
        debug.setChecked(Prefs.getBoolean(this, Prefs.KEY_DEBUG_MODE, false));
        debug.setOnCheckedChangeListener((buttonView, isChecked) ->
                Prefs.putBoolean(this, Prefs.KEY_DEBUG_MODE, isChecked));

        findViewById(R.id.btn_clear_crashes).setOnClickListener(v -> {
            crashes.clear();
            loadCrashes();
        });
        findViewById(R.id.btn_clear_log).setOnClickListener(v -> {
            AppLogger.clear();
            refreshLog();
        });

        loadCrashes();
        refreshLog();
    }

    private void onCrashRowClicked(int position) {
        if (position >= 0 && position < crashes.size()) {
            showCrashDetail(crashes.get(position));
        }
    }

    private void showCrashDetail(CrashLog c) {
        String title = (c.isFatal ? "Crash - " : "Log - ")
                + DateUtil.formatDate(c.timestamp) + " " + DateUtil.formatTime(c.timestamp);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (400 * getResources().getDisplayMetrics().density)));

        TextView text = new TextView(this);
        text.setTextSize(12);
        text.setTextColor(getResources().getColor(R.color.text_primary));
        text.setTypeface(android.graphics.Typeface.MONOSPACE);
        text.setTextIsSelectable(true);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        text.setPadding(pad, pad, pad, pad);
        text.setText(buildDetailText(c));
        scroll.addView(text);

        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(scroll)
                .setPositiveButton("Copy", (dialog, which) -> copyCrash(c))
                .setNeutralButton("Share", (dialog, which) -> shareCrash(c))
                .setNegativeButton("Download", (dialog, which) -> downloadCrash(c))
                .show();
    }

    private String buildDetailText(CrashLog c) {
        StringBuilder sb = new StringBuilder();
        sb.append("Time: ").append(DateUtil.formatDate(c.timestamp))
                .append(" ").append(DateUtil.formatTime(c.timestamp)).append('\n');
        if (c.threadName != null) sb.append("Thread: ").append(c.threadName).append('\n');
        if (c.appVersion != null) sb.append("App version: ").append(c.appVersion).append('\n');
        sb.append("Type: ").append(c.isFatal ? "Fatal crash" : "Logged error").append('\n');
        sb.append("Message: ").append(c.message == null ? "" : c.message).append('\n');
        if (c.stackTrace != null) sb.append('\n').append(c.stackTrace);
        return sb.toString();
    }

    private void copyCrash(CrashLog c) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("Crash report", buildDetailText(c)));
        DialogUtil.toast(this, "Crash details copied");
    }

    private void shareCrash(CrashLog c) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, "Salexfy crash report");
        send.putExtra(Intent.EXTRA_TEXT, buildDetailText(c));
        startActivity(Intent.createChooser(send, "Share crash report"));
    }

    private void downloadCrash(CrashLog c) {
        pendingDownload = c;
        Intent create = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        create.addCategory(Intent.CATEGORY_OPENABLE);
        create.setType("text/plain");
        create.putExtra(Intent.EXTRA_TITLE, "crash_" + c.timestamp + ".txt");
        startActivityForResult(create, REQUEST_SAVE_CRASH);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SAVE_CRASH && resultCode == RESULT_OK && data != null) {
            CrashLog c = pendingDownload;
            if (c == null) return;
            Uri uri = data.getData();
            if (uri == null) return;
            new Thread(() -> {
                try {
                    OutputStream out = getContentResolver().openOutputStream(uri);
                    if (out != null) {
                        out.write(buildDetailText(c).getBytes(StandardCharsets.UTF_8));
                        out.close();
                    }
                    handler.post(() -> DialogUtil.toast(this, "Crash report saved"));
                } catch (Exception e) {
                    handler.post(() -> DialogUtil.toast(this, "Could not save crash report"));
                }
            }).start();
        }
    }

    private void loadCrashes() {
        repo.run(() -> {
            crashes = repo.crash.getCrashes(100);
            List<KeyValueAdapter.Row> rows = new ArrayList<>();
            for (CrashLog c : crashes) {
                String message = c.message == null ? "Unknown error" : c.message;
                if (message.length() > 90) message = message.substring(0, 90) + "...";
                rows.add(new KeyValueAdapter.Row(
                        DateUtil.formatDate(c.timestamp) + " " + DateUtil.formatTime(c.timestamp),
                        message,
                        c.isFatal ? "CRASH" : "LOG",
                        c.isFatal ? 0xFFDC2626 : 0xFF64748B));
            }
            if (rows.isEmpty()) rows.add(new KeyValueAdapter.Row("No crashes recorded", "Good - the app is stable", "", 0));
            handler.post(() -> adapter.submit(rows));
        });
    }

    private void refreshLog() {
        handler.post(() -> logView.setText(AppLogger.readLog()));
    }
}
