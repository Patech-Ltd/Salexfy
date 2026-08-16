package com.patechltd.salexfypos.ui.suppliers;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.adapter.PartnerAdapter;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.SupplierPayableRow;
import com.patechltd.salexfypos.sync.SyncEvents;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.Prefs;

import java.util.ArrayList;
import java.util.List;

public class SupplierListActivity extends AppCompatActivity {

    private Repository repo;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable syncListener = () -> handler.post(this::load);
    private PartnerAdapter adapter;
    private List<SupplierPayableRow> allSuppliers = new ArrayList<>();
    private final List<SupplierPayableRow> visible = new ArrayList<>();
    private TextView summary;
    private String filter = "ALL";
    private String searchQuery = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supplier_list);
        repo = Repository.get(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        summary = findViewById(R.id.summary);

        android.widget.EditText search = findViewById(R.id.search_input);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                searchQuery = s.toString().trim().toLowerCase();
                render();
            }
        });

        RecyclerView list = findViewById(R.id.list);
        adapter = new PartnerAdapter(position -> {
            if (position < 0 || position >= visible.size()) return;
            Intent intent = new Intent(this, SupplierEditActivity.class);
            intent.putExtra("supplierId", visible.get(position).supplierId);
            startActivity(intent);
        });
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        MaterialButtonToggleGroup toggle = findViewById(R.id.filter_toggle);
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btn_filter_creditors) filter = "CREDITORS";
            else if (checkedId == R.id.btn_filter_suppliers) filter = "SUPPLIERS";
            else filter = "ALL";
            render();
        });
        toggle.check(R.id.btn_filter_all);

        MaterialButton add = findViewById(R.id.btn_add);
        add.setOnClickListener(v -> startActivity(new Intent(this, SupplierEditActivity.class)));

        SyncEvents.addListener(syncListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SyncEvents.removeListener(syncListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        repo.run(() -> {
            allSuppliers = repo.purchases.getSupplierPayables();
            handler.post(this::render);
        });
    }

    private void render() {
        String c = Prefs.currency(this);
        double totalPayable = 0;
        int creditors = 0;
        for (SupplierPayableRow s : allSuppliers) {
            if (s.payable > 0.01) {
                totalPayable += s.payable;
                creditors++;
            }
        }
        summary.setText("Total owed to suppliers: " + c + " " + NumberUtil.money(totalPayable)
                + "   •   " + creditors + " creditor" + (creditors == 1 ? "" : "s"));

        List<PartnerAdapter.Row> rows = new ArrayList<>();
        visible.clear();
        for (SupplierPayableRow s : allSuppliers) {
            boolean isCreditor = s.payable > 0.01;
            if ("CREDITORS".equals(filter) && !isCreditor) continue;
            if ("SUPPLIERS".equals(filter) && isCreditor) continue;
            if (!searchQuery.isEmpty()) {
                String hay = (s.name == null ? "" : s.name.toLowerCase())
                        + " " + (s.phone == null ? "" : s.phone.toLowerCase());
                if (!hay.contains(searchQuery)) continue;
            }
            visible.add(s);
            PartnerAdapter.Row row = new PartnerAdapter.Row();
            row.title = s.name;
            row.phone = s.phone;
            row.subtitle = isCreditor ? "Creditor — you owe them" : "Supplier — clear balance";
            if (isCreditor) {
                row.amount = c + " " + NumberUtil.money(s.payable);
                row.amountColor = 0xFFB45309;
            }
            rows.add(row);
        }
        adapter.submit(rows);
    }
}
