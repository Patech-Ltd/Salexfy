package com.patechltd.salexfypos.ui.products;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.adapter.SimpleStringAdapter;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.util.AppLogger;
import com.patechltd.salexfypos.util.DialogUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class DirectoryListActivity<T> extends AppCompatActivity {

    protected Repository repo;
    protected final Handler handler = new Handler(Looper.getMainLooper());
    protected List<T> entities = new ArrayList<>();
    protected SimpleStringAdapter adapter;

    protected abstract List<T> loadAll();

    protected abstract String nameOf(T item);

    protected abstract void insert(T item);

    protected abstract void update(T item);

    protected abstract void delete(T item);

    protected abstract T create(String name);

    protected abstract void deleteBlocked();

    protected abstract int itemIcon();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_directory_list);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getTitleText());
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        repo = Repository.get(this);
        RecyclerView list = findViewById(R.id.list);
        adapter = new SimpleStringAdapter(new SimpleStringAdapter.Listener() {
            @Override
            public void onClick(int position) {
                rename(position);
            }

            @Override
            public void onEdit(int position) {
                rename(position);
            }

            @Override
            public void onDelete(int position) {
                confirmDelete(position);
            }
        }, true);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        MaterialButton add = findViewById(R.id.btn_add);
        add.setText("Add " + getTitleText().toLowerCase());
        add.setOnClickListener(v -> addNew());

        reload();
    }

    protected abstract String getTitleText();

    protected void reload() {
        repo.run(() -> {
            entities = loadAll();
            List<String> names = new ArrayList<>();
            for (T t : entities) names.add(nameOf(t));
            handler.post(() -> adapter.submit(names));
        });
    }

    private void addNew() {
        DialogUtil.inputText(this, "Add " + getTitleText().toLowerCase(), "Name", "", "Add", value -> {
            if (value.trim().isEmpty()) return;
            T item = create(value.trim());
            repo.run(() -> {
                insert(item);
                handler.post(this::reload);
                AppLogger.i("Added " + value.trim());
            });
        });
    }

    private void rename(int position) {
        if (position < 0 || position >= entities.size()) return;
        T item = entities.get(position);
        DialogUtil.inputText(this, "Rename", "Name", nameOf(item), "Save", value -> {
            if (value.trim().isEmpty()) return;
            setName(item, value.trim());
            repo.run(() -> {
                update(item);
                handler.post(this::reload);
            });
        });
    }

    protected abstract void setName(T item, String name);

    private void confirmDelete(int position) {
        if (position < 0 || position >= entities.size()) return;
        T item = entities.get(position);
        DialogUtil.confirm(this, "Delete " + nameOf(item) + "?",
                "This may affect products using it.", () -> {
                    repo.run(() -> {
                        try {
                            delete(item);
                        } catch (Exception e) {
                            handler.post(this::deleteBlocked);
                            return;
                        }
                        handler.post(this::reload);
                    });
                });
    }
}
