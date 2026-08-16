package com.patechltd.salexfypos.ui.sell;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.entity.Customer;
import com.patechltd.salexfypos.util.NumberUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CustomerSearchDialog {

    public interface OnPick {
        void onCustomer(Customer customer);
    }

    public interface OnAddNew {
        void addNew();
    }

    private final Context context;
    private final List<Customer> customers;
    private final Map<String, Double> debts;
    private final OnPick callback;
    private final OnAddNew onAddNew;

    private String query = "";

    public CustomerSearchDialog(Context context, List<Customer> customers,
                                Map<String, Double> debts, OnPick callback) {
        this(context, customers, debts, callback, null);
    }

    public CustomerSearchDialog(Context context, List<Customer> customers,
                                Map<String, Double> debts, OnPick callback, OnAddNew onAddNew) {
        this.context = context;
        this.customers = customers;
        this.debts = debts;
        this.callback = callback;
        this.onAddNew = onAddNew;
    }

    public void show() {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_customer_search, null);
        AutoCompleteTextView input = view.findViewById(R.id.search_input);
        RecyclerView list = view.findViewById(R.id.customer_list);
        list.setLayoutManager(new LinearLayoutManager(context));

        List<String> names = new ArrayList<>();
        for (Customer c : customers) names.add(c.name);
        ArrayAdapter<String> suggest = new ArrayAdapter<>(context,
                android.R.layout.simple_dropdown_item_1line, names);
        input.setAdapter(suggest);
        input.setThreshold(1);

        CustomerAdapter adapter = new CustomerAdapter();
        list.setAdapter(adapter);

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                query = s.toString().trim().toLowerCase(Locale.ROOT);
                adapter.refresh();
            }
        });

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setTitle("Select customer")
                .setView(view)
                .setNegativeButton("Close", null);
        if (onAddNew != null) {
            builder.setPositiveButton("New customer", (d, w) -> onAddNew.addNew());
        }
        builder.show();
    }

    private double debtOf(Customer c) {
        if (c == null || debts == null) return 0;
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

    private class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.VH> {

        private final List<Customer> items = new ArrayList<>();

        CustomerAdapter() {
            refresh();
        }

        void refresh() {
            items.clear();
            items.addAll(matches());
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
            final Customer c = items.get(position);
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
            holder.balance.setTextColor(context.getResources().getColor(
                    debt > 0.01 ? R.color.error : R.color.success));
            holder.itemView.setOnClickListener(v -> {
                if (callback != null) callback.onCustomer(c);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
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
