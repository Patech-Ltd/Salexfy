package com.patechltd.salexfypos.ui.sell;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.SaleWithItems;
import com.patechltd.salexfypos.db.entity.Sale;
import com.patechltd.salexfypos.db.entity.SaleItem;
import com.patechltd.salexfypos.db.entity.SalePayment;
import com.patechltd.salexfypos.model.PaymentMethod;
import com.patechltd.salexfypos.security.PermissionChecker;
import com.patechltd.salexfypos.model.Authority;
import com.patechltd.salexfypos.util.DateUtil;
import com.patechltd.salexfypos.util.DialogUtil;
import com.patechltd.salexfypos.util.NumberUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured read-only view of a completed transaction: header, items,
 * totals, payments and the transaction UID. Receipt preview and printing
 * live here (and on ReceiptPreviewActivity).
 */
public class SaleDetailActivity extends AppCompatActivity {

    private Repository repo;
    private String saleId;
    private SaleWithItems cached;
    private MaterialButton btnVoid;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sale_detail);
        repo = Repository.get(this);
        saleId = getIntent().getStringExtra("saleId");
        if (saleId == null) {
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        btnVoid = findViewById(R.id.btn_void);
        if (PermissionChecker.has(this, Authority.SALE_VOID)) {
            btnVoid.setVisibility(View.VISIBLE);
            btnVoid.setOnClickListener(v -> voidSale());
        }

        findViewById(R.id.btn_print).setOnClickListener(v -> printReceipt());
        findViewById(R.id.btn_preview).setOnClickListener(v -> openPreview());

        load();
    }

    private void openPreview() {
        Intent i = new Intent(this, ReceiptPreviewActivity.class);
        i.putExtra("saleId", saleId);
        startActivity(i);
    }

    private void printReceipt() {
        repo.run(() -> {
            SaleWithItems sw = cached != null ? cached : repo.sales.getSaleWithItems(saleId);
            if (sw == null || sw.sale == null) return;
            double balance = repo.outstandingDebt(sw.sale.customerId);
            com.patechltd.salexfypos.print.PrinterManager.print(this, sw.sale, sw.items, sw.payments,
                    balance, (ok, msg) -> runOnUiThread(() -> DialogUtil.toast(this, msg)));
        });
    }

    private void load() {
        repo.run(() -> {
            SaleWithItems sw = repo.sales.getSaleWithItems(saleId);
            cached = sw;
            runOnUiThread(() -> {
                if (sw == null || sw.sale == null) {
                    finish();
                    return;
                }
                render(sw);
            });
        });
    }

    private void render(SaleWithItems sw) {
        Sale sale = sw.sale;
        ((TextView) findViewById(R.id.sale_no)).setText("Sale #" + sale.saleNo);

        TextView status = findViewById(R.id.status_chip);
        boolean voided = "VOID".equals(sale.status);
        status.setText(voided ? "Voided" : "Complete");
        status.setBackgroundResource(voided ? R.drawable.bg_danger_card : R.drawable.bg_success_card);
        status.setTextColor(getResources().getColor(voided ? R.color.error : R.color.success));

        if (btnVoid != null && voided) {
            btnVoid.setEnabled(false);
            btnVoid.setText("Already Voided");
        }

        ((TextView) findViewById(R.id.date)).setText(DateUtil.format(sale.saleDate));
        ((TextView) findViewById(R.id.cashier)).setText("Cashier: " + safe(sale.cashierName));
        TextView customer = findViewById(R.id.customer);
        if (sale.customerName != null && !sale.customerName.isEmpty()) {
            customer.setVisibility(View.VISIBLE);
            customer.setText("Customer: " + sale.customerName);
        } else {
            customer.setVisibility(View.GONE);
        }
        ((TextView) findViewById(R.id.uid)).setText("UID: " + sale.uid);

        android.graphics.Bitmap qr = com.patechltd.salexfypos.util.QrUtil.qr(
                com.patechltd.salexfypos.util.QrUtil.saleContent(sale.saleNo, sale.uid), 384);
        android.widget.ImageView qrView = findViewById(R.id.sale_qr);
        if (qr != null) {
            qrView.setImageBitmap(qr);
        } else {
            qrView.setVisibility(View.GONE);
        }

        RecyclerView itemsList = findViewById(R.id.items_list);
        itemsList.setLayoutManager(new LinearLayoutManager(this));
        List<SaleItem> saleItems = sw.items == null ? new ArrayList<>() : sw.items;
        itemsList.setAdapter(new LineAdapter(saleItems));

        LinearLayout totals = findViewById(R.id.totals_container);
        totals.removeAllViews();
        addTotalRow(totals, "Subtotal", NumberUtil.money(sale.subtotal), false);
        if (sale.discount > 0) {
            addTotalRow(totals, "Discount", "-" + NumberUtil.money(sale.discount), false);
        }
        if (sale.taxAmount > 0) {
            addTotalRow(totals, "Tax", NumberUtil.money(sale.taxAmount), false);
        }
        addTotalRow(totals, "Total", NumberUtil.money(sale.total), true);
        double cost = 0;
        for (SaleItem item : saleItems) cost += item.stockQty * item.costPrice;
        double profit = sale.total - cost;
        addTotalRow(totals, "Profit", NumberUtil.money(profit), false);

        if (sw.payments != null && !sw.payments.isEmpty()) {
            for (SalePayment p : sw.payments) {
                String label = PaymentMethod.labelOf(p.method);
                if (p.customerName != null && !p.customerName.isEmpty()) {
                    label += " (" + p.customerName + ")";
                }
                addTotalRow(totals, label, NumberUtil.money(p.amount), false);
            }
            if (sale.paidAmount > 0) addTotalRow(totals, "Paid", NumberUtil.money(sale.paidAmount), false);
            if (sale.changeAmount > 0) addTotalRow(totals, "Change", NumberUtil.money(sale.changeAmount), false);
            double balance = sale.total - sale.paidAmount;
            if (balance > 0.001) addTotalRow(totals, "Balance", NumberUtil.money(balance), false);
        } else if ("CREDIT".equals(sale.paymentMethod)) {
            addTotalRow(totals, "Payment", "On credit", false);
            if (sale.paidAmount > 0) addTotalRow(totals, "Paid now", NumberUtil.money(sale.paidAmount), false);
            addTotalRow(totals, "Balance", NumberUtil.money(sale.total - sale.paidAmount), false);
        } else {
            addTotalRow(totals, "Payment", PaymentMethod.labelOf(sale.paymentMethod), false);
            addTotalRow(totals, "Paid", NumberUtil.money(sale.paidAmount), false);
            addTotalRow(totals, "Change", NumberUtil.money(sale.changeAmount), false);
        }
        if (sale.pointsEarned > 0) {
            addTotalRow(totals, "Points earned", NumberUtil.qty(sale.pointsEarned), false);
        }
        if (sale.notes != null && !sale.notes.isEmpty()) {
            addTotalRow(totals, "Note", sale.notes, false);
        }
    }

    private void addTotalRow(LinearLayout container, String label, String value, boolean emphasized) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setPadding(0, dp(5), 0, dp(5));

        TextView l = new TextView(this);
        l.setText(label);
        l.setTextColor(getResources().getColor(emphasized ? R.color.text_primary : R.color.text_secondary));
        l.setTextSize(emphasized ? 16 : 14);
        if (emphasized) l.setTypeface(l.getTypeface(), Typeface.BOLD);
        l.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(l);

        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(getResources().getColor(emphasized ? R.color.amount_text : R.color.text_primary));
        v.setTextSize(emphasized ? 16 : 14);
        if (emphasized) v.setTypeface(v.getTypeface(), Typeface.BOLD);
        v.setGravity(Gravity.END);
        row.addView(v);
        container.addView(row);

        if (emphasized) {
            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
            divider.setBackgroundColor(getResources().getColor(R.color.outline));
            container.addView(divider);
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void voidSale() {
        DialogUtil.confirm(this, "Void this sale?",
                "Stock will be returned to inventory.", () -> repo.run(() -> {
                    Sale sale = repo.sales.getSale(saleId);
                    if (sale == null) return;
                    try {
                        repo.voidSale(sale);
                        runOnUiThread(() -> {
                            DialogUtil.toast(this, "Sale voided");
                            load();
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> DialogUtil.toast(this, "Could not void sale"));
                    }
                }));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private static class LineAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<LineAdapter.VH> {

        private final List<SaleItem> items;

        LineAdapter(List<SaleItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_sale_line, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            SaleItem item = items.get(position);
            holder.name.setText(item.productName);
            String unit = item.unitLabel == null || item.unitLabel.isEmpty() ? "" : " " + item.unitLabel;
            holder.sub.setText(NumberUtil.qty(item.qty) + unit + "  ×  " + NumberUtil.money(item.unitPrice));
            holder.total.setText(NumberUtil.money(item.lineTotal));
            if (item.costPrice > 0) {
                double buy = item.stockQty * item.costPrice;
                double profit = item.lineTotal - buy;
                holder.profit.setText("Buy " + NumberUtil.money(buy) + "  •  Profit "
                        + NumberUtil.money(profit));
                holder.profit.setTextColor(holder.profit.getResources().getColor(
                        profit < 0 ? R.color.error : R.color.success));
                holder.profit.setVisibility(View.VISIBLE);
            } else {
                holder.profit.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final TextView name, sub, total, profit;

            VH(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.line_name);
                sub = itemView.findViewById(R.id.line_sub);
                total = itemView.findViewById(R.id.line_total);
                profit = itemView.findViewById(R.id.line_profit);
            }
        }
    }
}
