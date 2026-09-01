package com.patechltd.salexfypos.ui.sell;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.Customer;
import com.patechltd.salexfypos.util.NumberUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Full-page customer picker used during payment. Returns the customer UID,
 * or creates a new customer inline via the "New" button.
 */
public class CustomerSearchActivity extends AppCompatActivity {

    public static final String EXTRA_CUSTOMER_ID = "customerId";

    private Repository repo;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Customer> customers = new ArrayList<>();
    private final Map<String, Double> debts = new HashMap<>();
    private CustomerAdapter adapter;
    private String query = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_search);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        repo = Repository.get(this);

        RecyclerView list = findViewById(R.id.customer_list);
        adapter = new CustomerAdapter();
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        EditText search = findViewById(R.id.search_input);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                query = s.toString().trim().toLowerCase(Locale.ROOT);
                refresh();
            }
        });

        findViewById(R.id.btn_new).setOnClickListener(v -> addNew());

        load();
    }

    private void load() {
        repo.run(() -> {
            List<Customer> all = repo.suppliers.getCustomers();
            Map<String, Double> d = new HashMap<>();
            for (com.patechltd.salexfypos.db.DebtorBalanceRow row : repo.suppliers.getDebtorBalances()) {
                d.put(row.customerId, row.outstanding);
            }
            handler.post(() -> {
                customers.clear();
                customers.addAll(all);
                debts.clear();
                debts.putAll(d);
                refresh();
            });
        });
    }

    private void addNew() {
        com.patechltd.salexfypos.util.DialogUtil.inputText(this, "New customer", "Customer name", "",
                "Add", value -> {
                    final String name = value == null ? "" : value.trim();
                    if (name.isEmpty()) return;
                    final Customer c = new Customer();
                    c.uid = UUID.randomUUID().toString();
                    c.name = name;
                    c.loyaltyPoints = 0;
                    c.createdAt = System.currentTimeMillis();
                    repo.run(() -> {
                        repo.suppliers.insertCustomer(c);
                        handler.post(() -> {
                            Intent result = new Intent();
                            result.putExtra(EXTRA_CUSTOMER_ID, c.uid);
                            setResult(RESULT_OK, result);
                            finish();
                        });
                    });
                });
    }

    private double debtOf(Customer c) {
        if (c == null) return 0;
        Double d = debts.get(c.uid);
        return d == null ? 0 : Math.max(0, d);
    }

    private List<Customer> matches() {
        List<Customer> out = new ArrayList<>();
        for (Customer c : customers) {
            if (!query.isEmpty()) {
                boolean hit = c.name != null && c.name.toLowerCase(Locale.ROOT).contains(query);
                if (!hit && c.phone != null) hit = c.phone.toLowerCase(Locale.ROOT).contains(query);
                if (!hit) continue;
            }
            out.add(c);
        }
        out.sort((a, b) -> Double.compare(debtOf(b), debtOf(a)));
        return out;
    }

    private void refresh() {
        List<Customer> rows = matches();
        adapter.submit(rows);
        findViewById(R.id.empty_hint).setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void pick(Customer c) {
        Intent result = new Intent();
        result.putExtra(EXTRA_CUSTOMER_ID, c.uid);
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.VH> {

        private final List<Customer> rows = new ArrayList<>();

        void submit(List<Customer> list) {
            rows.clear();
            rows.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_customer_pick, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            final Customer c = rows.get(position);
            holder.name.setText(c.name);
            double debt = debtOf(c);
            String meta = (c.phone == null || c.phone.isEmpty()) ? "" : c.phone;
            if (c.loyaltyPoints > 0) {
                meta = meta.isEmpty() ? NumberUtil.qty(c.loyaltyPoints) + " pts"
                        : meta + " • " + NumberUtil.qty(c.loyaltyPoints) + " pts";
            }
            if (meta.isEmpty()) meta = debt > 0.01 ? "Debtor" : "Customer";
            holder.meta.setText(meta);
            holder.balance.setText(debt > 0.01
                    ? "Owes " + NumberUtil.money(debt)
                    : "Clear balance");
            holder.balance.setTextColor(getResources().getColor(
                    debt > 0.01 ? R.color.error : R.color.success));
            holder.itemView.setOnClickListener(v -> pick(c));
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final TextView name, meta, balance;

            VH(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.cust_name);
                meta = itemView.findViewById(R.id.cust_meta);
                balance = itemView.findViewById(R.id.cust_balance);
            }
        }
    }
}
