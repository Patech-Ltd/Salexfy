package com.patechltd.salexfypos.ui.sell;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.entity.Customer;
import com.patechltd.salexfypos.model.PaymentMethod;
import com.patechltd.salexfypos.util.DialogUtil;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.Prefs;

import java.util.List;

public class CheckoutDialog {

    public interface Callback {
        void onConfirm(PaymentMethod method, String customerName, String customerId,
                       double paid, double pointsUsed, String notes);
    }

    private final Context context;
    private final double total;
    private final double discount;
    private final List<Customer> customers;
    private final String initialCustomer;
    private final Callback callback;

    private PaymentMethod method = PaymentMethod.CASH;
    private Customer selectedCustomer;
    private TextInputEditText received;
    private TextView changeLabel;
    private TextView changeAmount;
    private View pointsRow;
    private TextView pointsDiscountText;
    private SwitchMaterial usePoints;
    private TextView dueText;
    private double pointsDiscount;
    private final double pointValue;

    public CheckoutDialog(Context context, double total, double discount,
                          List<Customer> customers, String initialCustomer, Callback callback) {
        this.context = context;
        this.total = total;
        this.discount = discount;
        this.customers = customers;
        this.initialCustomer = initialCustomer;
        this.callback = callback;
        this.pointValue = Prefs.getDouble(context, Prefs.KEY_LOYALTY_POINT_VALUE, 0.5);
        if (initialCustomer != null && !initialCustomer.isEmpty() && customers != null) {
            for (Customer c : customers) {
                if (initialCustomer.equals(c.name)) {
                    selectedCustomer = c;
                    break;
                }
            }
        }
    }

    public void show() {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_checkout, null);

        TextView totalText = view.findViewById(R.id.dlg_total);
        Button customerBtn = view.findViewById(R.id.btn_customer);
        ChipGroup chips = view.findViewById(R.id.payment_chips);
        View cashSection = view.findViewById(R.id.cash_section);
        received = view.findViewById(R.id.received);
        changeLabel = view.findViewById(R.id.change_label);
        changeAmount = view.findViewById(R.id.change_amount);
        TextInputEditText notes = view.findViewById(R.id.notes);
        pointsRow = view.findViewById(R.id.points_row);
        pointsDiscountText = view.findViewById(R.id.points_discount);
        usePoints = view.findViewById(R.id.use_points);
        dueText = view.findViewById(R.id.due_amount);

        totalText.setText(NumberUtil.money(total));

        if (selectedCustomer != null) {
            customerBtn.setText("Customer: " + selectedCustomer.name);
            refreshPointsUI();
        }

        customerBtn.setOnClickListener(v -> {
            if (customers == null || customers.isEmpty()) {
                DialogUtil.inputText(context, "New customer", "Customer name", "",
                        "Add", value -> {
                            String name = value.trim();
                            if (name.isEmpty()) return;
                            selectedCustomer = new Customer();
                            selectedCustomer.name = name;
                            selectedCustomer.loyaltyPoints = 0;
                            customerBtn.setText("Customer: " + name);
                            refreshPointsUI();
                        });
                return;
            }
            String[] options = new String[customers.size() + 1];
            options[0] = "Cash Customer (Walk-in)";
            for (int i = 0; i < customers.size(); i++) options[i + 1] = customers.get(i).name;
            int selected = 0;
            if (selectedCustomer != null) {
                for (int i = 0; i < customers.size(); i++) {
                    if (customers.get(i).name.equals(selectedCustomer.name)) {
                        selected = i + 1;
                        break;
                    }
                }
            }
            DialogUtil.pick(context, "Select customer", options, selected, which -> {
                if (which == 0) {
                    selectedCustomer = null;
                    customerBtn.setText("Cash Customer (Walk-in)");
                } else {
                    selectedCustomer = customers.get(which - 1);
                    customerBtn.setText("Customer: " + selectedCustomer.name);
                }
                refreshPointsUI();
            });
        });

        usePoints.setOnCheckedChangeListener((buttonView, isChecked) -> {
            recomputePoints();
            refreshPaymentUI();
        });

        chips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            Chip chip = group.findViewById(checkedIds.isEmpty() ? -1 : checkedIds.get(0));
            if (chip == null) return;
            int id = chip.getId();
            if (id == R.id.chip_cash) method = PaymentMethod.CASH;
            else if (id == R.id.chip_mpesa) method = PaymentMethod.MPESA;
            else if (id == R.id.chip_card) method = PaymentMethod.CARD;
            else if (id == R.id.chip_bank) method = PaymentMethod.BANK;
            else if (id == R.id.chip_credit) method = PaymentMethod.CREDIT;
            cashSection.setVisibility(View.VISIBLE);
            received.setText("");
            received.setHint(method == PaymentMethod.CREDIT
                    ? "Pay now (remainder on credit)"
                    : "Amount received");
            refreshPaymentUI();
        });
        ((Chip) view.findViewById(R.id.chip_cash)).setChecked(true);
        cashSection.setVisibility(View.VISIBLE);
        received.setHint("Amount received");

        Button exact = view.findViewById(R.id.quick_exact);
        Button q500 = view.findViewById(R.id.quick_500);
        Button q1000 = view.findViewById(R.id.quick_1000);
        exact.setOnClickListener(v -> received.setText(String.valueOf(dueAmount())));
        q500.setOnClickListener(v -> received.setText(String.valueOf(500)));
        q1000.setOnClickListener(v -> received.setText(String.valueOf(1000)));
        received.setText(String.valueOf(dueAmount()));
        received.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updatePaymentSummary();
            }
        });

        new MaterialAlertDialogBuilder(context)
                .setTitle("Checkout")
                .setView(view)
                .setPositiveButton("Complete Sale", (dialog, which) -> {
                    double paid = NumberUtil.parse(received.getText() == null ? "" : received.getText().toString(), 0);
                    if (method == PaymentMethod.CASH && paid <= 0) paid = dueAmount();
                    if (method == PaymentMethod.MPESA || method == PaymentMethod.CARD || method == PaymentMethod.BANK) {
                        paid = dueAmount();
                    }
                    String note = notes.getText() == null ? "" : notes.getText().toString().trim();
                    double pointsUsed = (usePoints.isChecked() && pointsRow.getVisibility() == View.VISIBLE)
                            ? usedPoints() : 0;
                    String custName = selectedCustomer != null ? selectedCustomer.name : null;
                    String custId = selectedCustomer != null ? selectedCustomer.id : null;
                    callback.onConfirm(method, custName, custId, paid, pointsUsed, note);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private double dueAmount() {
        return Math.max(0, total - discount - pointsDiscount);
    }

    private double availablePoints() {
        return selectedCustomer != null ? selectedCustomer.loyaltyPoints : 0;
    }

    private double usedPoints() {
        double avail = availablePoints();
        if (avail <= 0) return 0;
        return Math.min(avail, (long) Math.ceil((total - discount) / (pointValue <= 0 ? 1 : pointValue)));
    }

    private void recomputePoints() {
        if (usePoints.isChecked() && availablePoints() > 0) {
            double pts = usedPoints();
            pointsDiscount = Math.min(total, Math.round(pts * pointValue * 100.0) / 100.0);
        } else {
            pointsDiscount = 0;
        }
        pointsDiscountText.setText("Points discount: -" + NumberUtil.money(pointsDiscount));
        dueText.setText(NumberUtil.money(dueAmount()));
    }

    private void refreshPointsUI() {
        if (selectedCustomer != null && selectedCustomer.loyaltyPoints > 0) {
            pointsRow.setVisibility(View.VISIBLE);
            pointsDiscountText.setText("Points discount: " + NumberUtil.money(0));
            recomputePoints();
        } else {
            pointsRow.setVisibility(View.GONE);
            usePoints.setChecked(false);
            pointsDiscount = 0;
        }
    }

    private void refreshPaymentUI() {
        recomputePoints();
        received.setText(method == PaymentMethod.CREDIT ? "0" : String.valueOf(dueAmount()));
        updatePaymentSummary();
    }

    private void updatePaymentSummary() {
        double paid = NumberUtil.parse(received.getText() == null ? "" : received.getText().toString(), 0);
        double due = dueAmount();
        if (method == PaymentMethod.CREDIT) {
            changeLabel.setText("Balance to pay later");
            changeAmount.setText(NumberUtil.money(Math.max(0, due - paid)));
        } else {
            changeLabel.setText("Change");
            changeAmount.setText(NumberUtil.money(Math.max(0, paid - due)));
        }
    }
}
