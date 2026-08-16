package com.patechltd.salexfypos.ui.sell;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.Customer;
import com.patechltd.salexfypos.model.PaymentMethod;
import com.patechltd.salexfypos.util.DialogUtil;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.Prefs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PaymentActivity extends AppCompatActivity {

    public static final String EXTRA_TOTAL = "total";
    public static final String EXTRA_SUBTOTAL = "subtotal";
    public static final String EXTRA_TAX = "tax";
    public static final String EXTRA_METHODS = "methods";
    public static final String EXTRA_AMOUNTS = "amounts";
    public static final String EXTRA_CUSTOMER_IDS = "customerIds";
    public static final String EXTRA_CUSTOMER_NAMES = "customerNames";
    public static final String EXTRA_POINTS_USED = "pointsUsed";
    public static final String EXTRA_NOTES = "notes";
    public static final int RESULT_HOLD = 2;

    private static class PaymentLine {
        final PaymentMethod method;
        final double amount;
        final String customerId;
        final String customerName;

        PaymentLine(PaymentMethod method, double amount, String customerId, String customerName) {
            this.method = method;
            this.amount = amount;
            this.customerId = customerId;
            this.customerName = customerName;
        }
    }

    private Repository repo;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private double total;
    private final List<Customer> customers = new ArrayList<>();
    private final Map<String, Double> debts = new HashMap<>();

    private PaymentMethod method = PaymentMethod.CASH;
    private Customer selectedCustomer;
    private Customer creditAccount;

    private final List<PaymentLine> lines = new ArrayList<>();

    private MaterialButton customerBtn;
    private TextView debtRow;
    private LinearLayout paymentsList;
    private TextView paymentsEmpty;
    private TextInputEditText payAmount;
    private TextView remainingAmount;
    private View changeRow;
    private TextView changeAmount;
    private TextView pointsDiscountText;
    private View pointsRow;
    private SwitchMaterial usePoints;
    private TextInputEditText notes;

    private double pointsDiscount;
    private double pointValue;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        repo = Repository.get(this);
        pointValue = Prefs.getDouble(this, Prefs.KEY_LOYALTY_POINT_VALUE, 0.5);
        total = getIntent().getDoubleExtra(EXTRA_TOTAL, 0);

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        ((TextView) findViewById(R.id.pay_total)).setText(NumberUtil.money(total));

        customerBtn = findViewById(R.id.btn_customer);
        debtRow = findViewById(R.id.debt_row);
        paymentsList = findViewById(R.id.payments_list);
        paymentsEmpty = findViewById(R.id.payments_empty);
        payAmount = findViewById(R.id.pay_amount);
        remainingAmount = findViewById(R.id.remaining_amount);
        changeRow = findViewById(R.id.change_row);
        changeAmount = findViewById(R.id.change_amount);
        pointsRow = findViewById(R.id.points_row);
        pointsDiscountText = findViewById(R.id.points_discount);
        usePoints = findViewById(R.id.use_points);
        notes = findViewById(R.id.notes);

        customerBtn.setOnClickListener(v -> pickCustomer());

        ChipGroup chips = findViewById(R.id.payment_chips);
        for (PaymentMethod m : PaymentMethod.values()) {
            Chip chip = new Chip(this);
            chip.setText(m.getLabel());
            chip.setCheckable(true);
            chip.setId(View.generateViewId());
            chip.setTag(m);
            chips.addView(chip);
            if (m == PaymentMethod.CASH) chip.setChecked(true);
        }
        chips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            Chip chip = group.findViewById(checkedIds.isEmpty() ? -1 : checkedIds.get(0));
            if (chip == null || chip.getTag() == null) return;
            method = (PaymentMethod) chip.getTag();
            refreshCustomerButton();
            refreshDebtRow();
            updateSummary();
        });

        findViewById(R.id.btn_add).setOnClickListener(v -> addPayment());
        findViewById(R.id.quick_exact).setOnClickListener(v ->
                payAmount.setText(NumberUtil.money(remaining())));
        findViewById(R.id.quick_500).setOnClickListener(v -> payAmount.setText("500"));
        findViewById(R.id.quick_1000).setOnClickListener(v -> payAmount.setText("1000"));
        findViewById(R.id.quick_2000).setOnClickListener(v -> payAmount.setText("2000"));

        payAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateSummary();
                refreshDebtRow();
            }
        });

        usePoints.setOnCheckedChangeListener((buttonView, isChecked) -> {
            recomputePoints();
            updateSummary();
        });

        findViewById(R.id.btn_hold).setOnClickListener(v -> {
            setResult(RESULT_HOLD);
            finish();
        });
        findViewById(R.id.btn_complete).setOnClickListener(v -> complete());

        loadCustomers();
        refreshCustomerButton();
        refreshPointsUI();
        refreshDebtRow();
        updateSummary();
    }

    private void loadCustomers() {
        repo.run(() -> {
            final List<Customer> all = repo.suppliers.getCustomers();
            final Map<String, Double> d = new HashMap<>();
            for (com.patechltd.salexfypos.db.DebtorBalanceRow row : repo.suppliers.getDebtorBalances()) {
                d.put(row.customerId, row.outstanding);
            }
            handler.post(() -> {
                customers.clear();
                customers.addAll(all);
                debts.clear();
                debts.putAll(d);
            });
        });
    }

    private void pickCustomer() {
        if (customers.isEmpty()) {
            addNewCustomer();
            return;
        }
        new CustomerSearchDialog(this, customers, debts,
                picked -> {
                    if (method == PaymentMethod.CREDIT) {
                        creditAccount = picked;
                    }
                    selectedCustomer = picked;
                    refreshCustomerButton();
                    refreshPointsUI();
                    refreshDebtRow();
                },
                this::addNewCustomer).show();
    }

    private void addNewCustomer() {
        DialogUtil.inputText(this, "New customer", "Customer name", "",
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
                            customers.add(c);
                            if (method == PaymentMethod.CREDIT) creditAccount = c;
                            selectedCustomer = c;
                            refreshCustomerButton();
                            refreshPointsUI();
                            refreshDebtRow();
                        });
                    });
                });
    }

    private void refreshCustomerButton() {
        if (method == PaymentMethod.CREDIT) {
            customerBtn.setText(creditAccount == null
                    ? "Select credit account (customer)"
                    : "Credit account: " + creditAccount.name);
        } else {
            customerBtn.setText(selectedCustomer == null
                    ? "Cash Customer (Walk-in)"
                    : "Customer: " + selectedCustomer.name);
        }
    }

    private void addPayment() {
        double amount = NumberUtil.parse(payAmount.getText() == null ? "" : payAmount.getText().toString(), 0);
        if (amount <= 0) {
            DialogUtil.toast(this, "Enter a valid amount");
            return;
        }
        if (method == PaymentMethod.CREDIT) {
            if (creditAccount == null) {
                DialogUtil.toast(this, "Select a credit account for credit payments");
                pickCustomer();
                return;
            }
            lines.add(new PaymentLine(method, amount, creditAccount.uid, creditAccount.name));
        } else {
            lines.add(new PaymentLine(method, amount, null, null));
        }
        payAmount.setText("");
        renderLines();
        updateSummary();
    }

    private void renderLines() {
        paymentsList.removeAllViews();
        paymentsEmpty.setVisibility(lines.isEmpty() ? View.VISIBLE : View.GONE);
        for (int i = 0; i < lines.size(); i++) {
            final PaymentLine line = lines.get(i);
            final int index = i;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(0, 6, 0, 6);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            row.setLayoutParams(rowLp);

            LinearLayout texts = new LinearLayout(this);
            texts.setOrientation(LinearLayout.VERTICAL);
            texts.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

            TextView title = new TextView(this);
            title.setText(PaymentMethod.labelOf(line.method.name())
                    + (line.customerName != null ? "  •  " + line.customerName : ""));
            title.setTextColor(getResources().getColor(R.color.text_primary));
            title.setTextSize(15);
            title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
            texts.addView(title);

            TextView amount = new TextView(this);
            amount.setText(NumberUtil.money(line.amount));
            amount.setTextColor(getResources().getColor(R.color.brand_primary_dark));
            amount.setTextSize(16);
            amount.setGravity(android.view.Gravity.END);
            texts.addView(amount);

            TextView remove = new TextView(this);
            remove.setText("✕");
            remove.setTextSize(20);
            remove.setTextColor(getResources().getColor(R.color.error));
            remove.setPadding(20, 0, 4, 0);
            remove.setOnClickListener(v -> {
                lines.remove(index);
                renderLines();
                updateSummary();
                refreshDebtRow();
            });

            row.addView(texts);
            row.addView(remove);
            paymentsList.addView(row);
        }
    }

    private double allocated() {
        double sum = 0;
        for (PaymentLine line : lines) sum += line.amount;
        return sum;
    }

    private double dueAmount() {
        return Math.max(0, total - pointsDiscount);
    }

    private double remaining() {
        return Math.max(0, dueAmount() - allocated());
    }

    private void updateSummary() {
        double allocated = allocated();
        double due = dueAmount();
        double remaining = due - allocated;
        remainingAmount.setText(NumberUtil.money(Math.max(0, remaining)));
        remainingAmount.setTextColor(getResources().getColor(
                remaining > 0.001 ? R.color.error : R.color.success));
        if (remaining < -0.001) {
            changeRow.setVisibility(View.VISIBLE);
            changeAmount.setText(NumberUtil.money(-remaining));
        } else {
            changeRow.setVisibility(View.GONE);
        }
    }

    private void complete() {
        if (lines.isEmpty()) {
            DialogUtil.toast(this, "Add at least one payment");
            return;
        }
        double due = dueAmount();
        if (allocated() < due - 0.001) {
            DialogUtil.toast(this, "Payments do not cover the total ("
                    + NumberUtil.money(remaining()) + " remaining)");
            return;
        }
        double pointsUsed = (usePoints.isChecked() && pointsRow.getVisibility() == View.VISIBLE)
                ? usedPoints() : 0;
        String note = notes.getText() == null ? "" : notes.getText().toString().trim();

        int n = lines.size();
        String[] methods = new String[n];
        double[] amounts = new double[n];
        String[] customerIds = new String[n];
        String[] customerNames = new String[n];
        for (int i = 0; i < n; i++) {
            PaymentLine line = lines.get(i);
            methods[i] = line.method.name();
            amounts[i] = line.amount;
            customerIds[i] = line.customerId;
            customerNames[i] = line.customerName;
        }

        Intent result = new Intent();
        result.putExtra(EXTRA_METHODS, methods);
        result.putExtra(EXTRA_AMOUNTS, amounts);
        result.putExtra(EXTRA_CUSTOMER_IDS, customerIds);
        result.putExtra(EXTRA_CUSTOMER_NAMES, customerNames);
        result.putExtra(EXTRA_POINTS_USED, pointsUsed);
        result.putExtra(EXTRA_NOTES, note);
        setResult(RESULT_OK, result);
        finish();
    }

    private double customerDebt(Customer c) {
        if (c == null) return 0;
        Double d = debts.get(c.uid);
        return d == null ? 0 : Math.max(0, d);
    }

    private double availablePoints() {
        return selectedCustomer != null ? selectedCustomer.loyaltyPoints : 0;
    }

    private double usedPoints() {
        double avail = availablePoints();
        if (avail <= 0) return 0;
        return Math.min(avail, (long) Math.ceil(total / (pointValue <= 0 ? 1 : pointValue)));
    }

    private void recomputePoints() {
        if (usePoints.isChecked() && availablePoints() > 0) {
            double pts = usedPoints();
            pointsDiscount = Math.min(total, Math.round(pts * pointValue * 100.0) / 100.0);
        } else {
            pointsDiscount = 0;
        }
        pointsDiscountText.setText("Points discount: -" + NumberUtil.money(pointsDiscount));
    }

    private void refreshPointsUI() {
        if (selectedCustomer != null && selectedCustomer.loyaltyPoints > 0) {
            pointsRow.setVisibility(View.VISIBLE);
            recomputePoints();
        } else {
            pointsRow.setVisibility(View.GONE);
            usePoints.setChecked(false);
            pointsDiscount = 0;
        }
    }

    private void refreshDebtRow() {
        if (debtRow == null) return;
        if (method != PaymentMethod.CREDIT) {
            debtRow.setVisibility(View.GONE);
            return;
        }
        if (creditAccount == null) {
            debtRow.setVisibility(View.VISIBLE);
            debtRow.setText("Select a credit account (customer) to continue");
            return;
        }
        double debt = customerDebt(creditAccount);
        double already = 0;
        for (PaymentLine line : lines) {
            if (line.method == PaymentMethod.CREDIT
                    && creditAccount.uid.equals(line.customerId)) already += line.amount;
        }
        double newLoan = Math.max(0, dueAmount() - allocated() + already);
        StringBuilder sb = new StringBuilder();
        sb.append("Credit account: ").append(creditAccount.name);
        if (debt > 0.01) sb.append("  •  Existing balance ").append(NumberUtil.money(debt));
        if (newLoan > 0.01) sb.append("  •  New loan ").append(NumberUtil.money(newLoan));
        if (debt <= 0.01 && newLoan <= 0.01) sb.append("  •  No balance");
        debtRow.setText(sb.toString());
        debtRow.setVisibility(View.VISIBLE);
    }
}
