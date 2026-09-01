package com.patechltd.salexfypos.ui.settings;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.adapter.PayMethodAdapter;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.PaymentMethod;
import com.patechltd.salexfypos.util.DialogUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PaymentMethodsActivity extends AppCompatActivity {

    private Repository repo;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private PayMethodAdapter adapter;
    private List<PaymentMethod> all = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_methods);
        repo = Repository.get(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView list = findViewById(R.id.list);
        adapter = new PayMethodAdapter(new PayMethodAdapter.Listener() {
            @Override
            public void onEdit(int position) {
                rename(position);
            }

            @Override
            public void onToggle(int position, boolean active) {
                if (position < 0 || position >= all.size()) return;
                PaymentMethod m = all.get(position);
                m.active = active;
                repo.run(() -> repo.updatePaymentMethod(m));
            }

            @Override
            public void onDelete(int position) {
                delete(position);
            }
        });
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        MaterialButton add = findViewById(R.id.btn_add);
        add.setOnClickListener(v -> addMethod());
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        repo.run(() -> {
            all = repo.paymentMethods.getAll();
            handler.post(() -> adapter.submit(new ArrayList<>(all)));
        });
    }

    private void addMethod() {
        DialogUtil.inputText(this, "New payment method", "Name, e.g. Equity Bank",
                "", "Add", value -> {
                    final String name = value == null ? "" : value.trim();
                    if (name.isEmpty()) return;
                    final List<PaymentMethod> existing = new ArrayList<>(all);
                    final String id = uniqueId(existing, name);
                    final PaymentMethod m = new PaymentMethod();
                    m.id = id;
                    m.name = name;
                    m.active = true;
                    m.sortOrder = existing.size();
                    repo.run(() -> {
                        repo.savePaymentMethod(m);
                        handler.post(() -> {
                            Toast.makeText(this, "Payment method added", Toast.LENGTH_SHORT).show();
                            load();
                        });
                    });
                });
    }

    private void rename(int position) {
        if (position < 0 || position >= all.size()) return;
        final PaymentMethod m = all.get(position);
        DialogUtil.inputText(this, "Rename payment method",
                "Name", m.name, "Save", value -> {
                    final String name = value == null ? "" : value.trim();
                    if (name.isEmpty()) return;
                    m.name = name;
                    repo.run(() -> {
                        repo.updatePaymentMethod(m);
                        handler.post(this::load);
                    });
                });
    }

    private void delete(int position) {
        if (position < 0 || position >= all.size()) return;
        final PaymentMethod m = all.get(position);
        if (m.isSystem) {
            DialogUtil.toast(this, "Cash and On Credit cannot be removed");
            return;
        }
        DialogUtil.confirm(this, "Remove payment method",
                "Remove \"" + m.name + "\"? Past transactions keep their records.",
                () -> repo.run(() -> {
                    repo.deletePaymentMethod(m);
                    handler.post(this::load);
                }));
    }

    private String uniqueId(List<PaymentMethod> existing, String name) {
        String base = sanitize(name);
        String id = base;
        int n = 2;
        while (hasId(existing, id)) {
            id = base + "_" + n;
            n++;
        }
        return id;
    }

    private boolean hasId(List<PaymentMethod> list, String id) {
        for (PaymentMethod m : list) {
            if (id.equals(m.id)) return true;
        }
        return false;
    }

    private String sanitize(String name) {
        String slug = name.toUpperCase(Locale.US).replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return slug.isEmpty() ? "PAY" : slug;
    }
}