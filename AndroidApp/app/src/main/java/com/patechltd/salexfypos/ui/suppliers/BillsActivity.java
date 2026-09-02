package com.patechltd.salexfypos.ui.suppliers;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.adapter.BillAdapter;
import com.patechltd.salexfypos.db.BillRow;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.PaymentMethod;
import com.patechltd.salexfypos.db.entity.SupplierPayment;
import com.patechltd.salexfypos.security.Session;
import com.patechltd.salexfypos.ui.stock.PurchaseEditActivity;
import com.patechltd.salexfypos.util.DialogUtil;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.PaymentMethods;
import com.patechltd.salexfypos.util.Prefs;

public class BillsActivity extends AppCompatActivity {

    private Repository repo;
    private BillAdapter billAdapter;
    private RecyclerView recyclerView;
    private TextView summaryText;
    private MaterialButton btnAddBill;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bills);

        repo = Repository.get(this);

        recyclerView = findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        summaryText = findViewById(R.id.summary);
        btnAddBill = findViewById(R.id.btn_add);

        btnAddBill.setOnClickListener(v -> {
            Intent i = new Intent(this, PurchaseEditActivity.class);
            startActivity(i);
        });

        loadBills();
    }

    private void loadBills() {
        repo.run(() -> {
            final List<BillRow> bills = repo.purchases.getBills();
            final double outstanding = repo.purchases.totalOutstanding();
            handler.post(() -> {
                summaryText.setText("Total outstanding: " + NumberUtil.money(outstanding)
                        + "  (" + bills.size() + " bill" + (bills.size() == 1 ? "" : "s") + ")");
                if (billAdapter == null) {
                    billAdapter = new BillAdapter(new BillAdapter.Listener() {
                        @Override
                        public void onOpen(int position) {
                            // Handle bill open if needed
                        }

                        @Override
                        public void onPay(int position) {
                            showBillPayDialog(position);
                        }
                    }, Prefs.currency(BillsActivity.this));
                    billAdapter.submit(bills);
                    recyclerView.setAdapter(billAdapter);
                } else {
                    billAdapter.submit(bills);
                }
            });
        });
    }

    private void showBillPayDialog(final int position) {
        final BillRow bill = billAdapter.getItem(position);
        if (bill == null) return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_bill_pay, null);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);

        TextView billInfo = dialogView.findViewById(R.id.bill_info);
        ChipGroup payMethodChips = dialogView.findViewById(R.id.pay_method_chips);
        TextInputLayout payAmountLayout = dialogView.findViewById(R.id.pay_amount_layout);
        TextInputEditText payAmount = dialogView.findViewById(R.id.pay_amount);
        MaterialButton quickExact = dialogView.findViewById(R.id.quick_exact);
        MaterialButton quick500 = dialogView.findViewById(R.id.quick_500);
        MaterialButton quick1000 = dialogView.findViewById(R.id.quick_1000);
        MaterialButton quick2000 = dialogView.findViewById(R.id.quick_2000);
        TextView remainingHint = dialogView.findViewById(R.id.remaining_hint);

        billInfo.setText("Bill: " + (bill.supplierName == null || bill.supplierName.isEmpty() ? "General / Others" : bill.supplierName) + " - " + (bill.invoiceNo == null || bill.invoiceNo.isEmpty() ? "Invoice" : bill.invoiceNo));
        remainingHint.setText("Remaining: " + NumberUtil.money(bill.balance));

        // Set up payment method chips
        List<PaymentMethod> activeMethods = PaymentMethods.active();
        for (int i = 0; i < activeMethods.size(); i++) {
            PaymentMethod method = activeMethods.get(i);
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
            chip.setText(method.name == null ? method.id : method.name);
            chip.setCheckable(true);
            chip.setId(View.generateViewId());
            chip.setTag(method);
            if (method.id != null && method.id.equals(com.patechltd.salexfypos.model.PaymentMethod.CASH)) {
                chip.setChecked(true);
            }
            payMethodChips.addView(chip);
        }

        // Set up quick payment buttons
        double remaining = bill.balance;
        quickExact.setOnClickListener(v -> {
            payAmount.setText(NumberUtil.money(remaining));
        });
        quick500.setOnClickListener(v -> {
            payAmount.setText("500");
        });
        quick1000.setOnClickListener(v -> {
            payAmount.setText("1000");
        });
        quick2000.setOnClickListener(v -> {
            payAmount.setText("2000");
        });

        // Set up pay button
        builder.setView(dialogView)
                .setTitle("Pay Bill")
                .setPositiveButton("Pay", (dialog, which) -> {
                    String amountStr = payAmount.getText() == null ? "" : payAmount.getText().toString().trim();
                    if (amountStr.isEmpty()) {
                        DialogUtil.toast(BillsActivity.this, "Enter an amount");
                        return;
                    }
                    double amount = NumberUtil.parse(amountStr, 0);
                    if (amount <= 0 || amount > bill.balance) {
                        DialogUtil.toast(BillsActivity.this, "Invalid amount");
                        return;
                    }

                    String methodId = com.patechltd.salexfypos.model.PaymentMethod.CASH;
                    int checkedId = payMethodChips.getCheckedChipId();
                    if (checkedId != View.NO_ID) {
                        View chip = payMethodChips.findViewById(checkedId);
                        if (chip != null && chip.getTag() instanceof PaymentMethod) {
                            methodId = ((PaymentMethod) chip.getTag()).id;
                        }
                    }
                    final String payMethod = methodId;
                    final double payAmountValue = amount;

                    repo.run(() -> {
                        SupplierPayment payment = repo.recordSupplierPayment(
                                bill.purchaseId, payAmountValue, payMethod, "Bill payment",
                                Session.userId(BillsActivity.this));
                        handler.post(() -> {
                            if (payment != null) {
                                DialogUtil.toast(BillsActivity.this,
                                        "Payment of " + NumberUtil.money(payAmountValue) + " recorded");
                                loadBills();
                            } else {
                                DialogUtil.toast(BillsActivity.this, "Payment could not be recorded");
                            }
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}