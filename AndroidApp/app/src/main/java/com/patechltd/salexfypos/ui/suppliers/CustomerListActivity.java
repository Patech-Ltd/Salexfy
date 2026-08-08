package com.patechltd.salexfypos.ui.suppliers;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.adapter.PartnerAdapter;
import com.patechltd.salexfypos.db.DebtorBalanceRow;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.Prefs;

import java.util.ArrayList;
import java.util.List;

public class CustomerListActivity extends AppCompatActivity {

    private Repository repo;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private PartnerAdapter adapter;
    private List<DebtorBalanceRow> customers = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_directory_list);
        repo = Repository.get(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Customers & Debtors");
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView list = findViewById(R.id.list);
        adapter = new PartnerAdapter(position -> {
            if (position < 0 || position >= customers.size()) return;
            Intent intent = new Intent(this, CustomerDetailActivity.class);
            intent.putExtra("customerId", customers.get(position).customerId);
            startActivity(intent);
        });
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        MaterialButton add = findViewById(R.id.btn_add);
        add.setText("Add Customer");
        add.setOnClickListener(v -> startActivity(new Intent(this, CustomerEditActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        repo.run(() -> {
            customers = repo.suppliers.getDebtorBalances();
            String c = Prefs.currency(this);
            List<PartnerAdapter.Row> rows = new ArrayList<>();
            for (DebtorBalanceRow d : customers) {
                PartnerAdapter.Row row = new PartnerAdapter.Row();
                row.title = d.name;
                row.subtitle = "Balance " + c + " " + NumberUtil.money(d.outstanding);
                rows.add(row);
            }
            handler.post(() -> adapter.submit(rows));
        });
    }
}
