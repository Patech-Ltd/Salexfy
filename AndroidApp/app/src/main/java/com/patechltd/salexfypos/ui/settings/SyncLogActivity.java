package com.patechltd.salexfypos.ui.settings;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.adapter.KeyValueAdapter;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.SyncLog;
import com.patechltd.salexfypos.util.DateUtil;

import java.util.ArrayList;
import java.util.List;

public class SyncLogActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private KeyValueAdapter adapter;
    private TextView emptyText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync_log);

        ((com.google.android.material.appbar.MaterialToolbar) findViewById(R.id.toolbar))
                .setNavigationOnClickListener(v -> finish());

        emptyText = findViewById(R.id.empty_text);
        RecyclerView list = findViewById(R.id.log_list);
        adapter = new KeyValueAdapter();
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        Repository.get(this).run(() -> {
            final List<SyncLog> logs = Repository.get(this).sync.getLogs(200);
            handler.post(() -> render(logs));
        });
    }

    private void render(List<SyncLog> logs) {
        List<KeyValueAdapter.Row> rows = new ArrayList<>();
        for (SyncLog log : logs) {
            int statusColor = "OK".equals(log.status) ? 0xFF16A34A : 0xFFDC2626;
            String subtitle = DateUtil.formatDate(log.timestamp) + " " + DateUtil.formatTime(log.timestamp);
            if (log.details != null && !log.details.isEmpty()) subtitle += "\n" + log.details;
            String value = log.message == null ? log.status : log.message;
            rows.add(new KeyValueAdapter.Row(log.type + " · " + log.status, subtitle, value, statusColor));
        }
        adapter.submit(rows);
        emptyText.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
