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
import com.patechltd.salexfypos.db.DebtorBalanceRow;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.sync.SyncEvents;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.Prefs;

import java.util.ArrayList;
import java.util.List;

public class CustomerListActivity extends AppCompatActivity {

    private Repository repo;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable syncListener = () -> handler.post(this::load);
    private PartnerAdapter adapter;
    private List<DebtorBalanceRow> allCustomers = new ArrayList<>();
    private final List<DebtorBalanceRow> visible = new ArrayList<>();
    private TextView summary;
    private String filter = "ALL";
    private String searchQuery = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_list);
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
            Intent intent = new Intent(this, CustomerDetailActivity.class);
            intent.putExtra("customerId", visible.get(position).customerId);
            startActivity(intent);
        });
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        MaterialButtonToggleGroup toggle = findViewById(R.id.filter_toggle);
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btn_filter_debtors) filter = "DEBTORS";
            else if (checkedId == R.id.btn_filter_customers) filter = "CUSTOMERS";
            else filter = "ALL";
            render();
        });
        toggle.check(R.id.btn_filter_all);

        MaterialButton add = findViewById(R.id.btn_add);
        add.setOnClickListener(v -> startActivity(new Intent(this, CustomerEditActivity.class)));

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
            allCustomers = repo.suppliers.getDebtorBalances();
            handler.post(this::render);
        });
    }

    private void render() {
        String c = Prefs.currency(this);
        double totalOutstanding = 0;
        int debtors = 0;
        for (DebtorBalanceRow d : allCustomers) {
            if (d.outstanding > 0.01) {
                totalOutstanding += d.outstanding;
                debtors++;
            }
        }
        summary.setText("Total outstanding: " + c + " " + NumberUtil.money(totalOutstanding)
                + "   •   " + debtors + " debtor" + (debtors == 1 ? "" : "s"));

        List<PartnerAdapter.Row> rows = new ArrayList<>();
        visible.clear();
        for (DebtorBalanceRow d : allCustomers) {
            boolean isDebtor = d.outstanding > 0.01;
            if ("DEBTORS".equals(filter) && !isDebtor) continue;
            if ("CUSTOMERS".equals(filter) && isDebtor) continue;
            if (!searchQuery.isEmpty()) {
                String hay = (d.name == null ? "" : d.name.toLowerCase())
                        + " " + (d.phone == null ? "" : d.phone.toLowerCase());
                if (!hay.contains(searchQuery)) continue;
            }
            visible.add(d);
            PartnerAdapter.Row row = new PartnerAdapter.Row();
            row.title = d.name;
            row.phone = d.phone;
            row.subtitle = isDebtor ? "Debtor — paying back" : "Customer — clear balance";
            if (isDebtor) {
                row.amount = c + " " + NumberUtil.money(d.outstanding);
                row.amountColor = 0xFFDC2626;
            }
            rows.add(row);
        }
        adapter.submit(rows);
    }
}
