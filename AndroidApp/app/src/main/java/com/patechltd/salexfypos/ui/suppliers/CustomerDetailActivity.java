package com.patechltd.salexfypos.ui.suppliers;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.adapter.KeyValueAdapter;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.Customer;
import com.patechltd.salexfypos.db.entity.DebtPayment;
import com.patechltd.salexfypos.db.entity.Sale;
import com.patechltd.salexfypos.util.DateUtil;
import com.patechltd.salexfypos.util.DialogUtil;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.Prefs;
import com.patechltd.salexfypos.security.Session;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CustomerDetailActivity extends AppCompatActivity {

    private Repository repo;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String customerId;
    private TextView name, phone, outstanding, creditLimit, loyaltyPoints, totalSpent;
    private KeyValueAdapter salesAdapter, paymentsAdapter;
    private Customer customer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_detail);
        repo = Repository.get(this);
        customerId = getIntent().getStringExtra("customerId");

        findViewById(R.id.toolbar).setOnClickListener(v -> onBackPressed());

        name = findViewById(R.id.customer_name);
        phone = findViewById(R.id.customer_phone);
        outstanding = findViewById(R.id.outstanding);
        creditLimit = findViewById(R.id.credit_limit);
        loyaltyPoints = findViewById(R.id.loyalty_points);
        totalSpent = findViewById(R.id.total_spent);

        salesAdapter = new KeyValueAdapter();
        paymentsAdapter = new KeyValueAdapter();

        RecyclerView salesList = findViewById(R.id.sales_list);
        salesList.setLayoutManager(new LinearLayoutManager(this));
        salesList.setAdapter(salesAdapter);

        RecyclerView paymentsList = findViewById(R.id.payments_list);
        paymentsList.setLayoutManager(new LinearLayoutManager(this));
        paymentsList.setAdapter(paymentsAdapter);

        MaterialButton btnPayment = findViewById(R.id.btn_payment);
        btnPayment.setOnClickListener(v -> recordPayment());
        findViewById(R.id.btn_edit).setOnClickListener(v -> {
            Intent intent = new Intent(this, CustomerEditActivity.class);
            intent.putExtra("customerId", customerId);
            startActivity(intent);
        });

        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        repo.run(() -> {
            customer = repo.suppliers.getCustomer(customerId);
            if (customer == null) {
                runOnUiThread(this::finish);
                return;
            }
            double debt = repo.suppliers.customerDebt(customerId);
            double paid = repo.suppliers.customerPaid(customerId);
            List<Sale> sales = repo.sales.getSalesForCustomer(customerId);
            List<DebtPayment> payments = repo.suppliers.getPayments(customerId);
            String currency = Prefs.currency(this);
            handler.post(() -> {
                name.setText(customer.name);
                phone.setText(customer.phone == null || customer.phone.isEmpty() ? "" : customer.phone);
                double outstandingVal = debt - paid;
                outstanding.setText(currency + " " + NumberUtil.money(outstandingVal));
                creditLimit.setText(customer.creditLimit <= 0 ? "Unlimited" : currency + " " + NumberUtil.money(customer.creditLimit));
                loyaltyPoints.setText(NumberUtil.qty(customer.loyaltyPoints));
                totalSpent.setText(currency + " " + NumberUtil.money(customer.totalSpent));

                List<KeyValueAdapter.Row> sRows = new ArrayList<>();
                for (Sale s : sales) {
                    sRows.add(new KeyValueAdapter.Row(
                            "#" + s.saleNo,
                            DateUtil.formatDate(s.saleDate) + " · " + s.paymentMethod,
                            currency + " " + NumberUtil.money(s.total),
                            0xFF1565C0));
                }
                if (sRows.isEmpty()) sRows.add(new KeyValueAdapter.Row("No sales yet", "", "", 0));
                salesAdapter.submit(sRows);

                List<KeyValueAdapter.Row> pRows = new ArrayList<>();
                for (DebtPayment p : payments) {
                    pRows.add(new KeyValueAdapter.Row(
                            DateUtil.formatDate(p.paymentDate),
                            p.method == null ? "" : p.method,
                            "+ " + currency + " " + NumberUtil.money(p.amount),
                            0xFF16A34A));
                }
                if (pRows.isEmpty()) pRows.add(new KeyValueAdapter.Row("No payments yet", "", "", 0));
                paymentsAdapter.submit(pRows);
            });
        });
    }

    private void recordPayment() {
        DialogUtil.input(this, "Record payment", "Amount", "", "Save", value -> {
            double amount = NumberUtil.parse(value, -1);
            if (amount <= 0) {
                DialogUtil.toast(this, "Enter a valid amount");
                return;
            }
            repo.run(() -> {
                DebtPayment p = new DebtPayment();
                p.id = UUID.randomUUID().toString();
                p.customerId = customerId;
                p.amount = amount;
                p.paymentDate = System.currentTimeMillis();
                p.method = "Cash";
                p.createdBy = Session.userId(this);
                p.createdAt = System.currentTimeMillis();
                repo.recordPayment(p);
                handler.post(this::load);
            });
        });
    }
}
