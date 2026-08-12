package com.patechltd.salexfypos.ui.sell;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.SaleWithItems;
import com.patechltd.salexfypos.db.entity.Sale;
import com.patechltd.salexfypos.db.entity.SaleItem;
import com.patechltd.salexfypos.security.PermissionChecker;
import com.patechltd.salexfypos.model.Authority;
import com.patechltd.salexfypos.util.DateUtil;
import com.patechltd.salexfypos.util.DialogUtil;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.Prefs;

import java.util.List;

public class SaleDetailActivity extends AppCompatActivity {

    private Repository repo;
    private String saleId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sale_detail);
        repo = Repository.get(this);
        saleId = getIntent().getStringExtra("saleId");

        findViewById(R.id.toolbar).setOnClickListener(v -> onBackPressed());

        MaterialButton btnVoid = findViewById(R.id.btn_void);
        btnVoid.setVisibility(PermissionChecker.has(this, Authority.SALE_VOID)
                ? android.view.View.VISIBLE : android.view.View.GONE);
        btnVoid.setOnClickListener(v -> voidSale());

        MaterialButton btnPrint = findViewById(R.id.btn_print);
        btnPrint.setOnClickListener(v -> printReceipt());

        load();
    }

    private SaleWithItems cached;

    private void printReceipt() {
        repo.run(() -> {
            SaleWithItems sw = cached != null ? cached : repo.sales.getSaleWithItems(saleId);
            if (sw == null || sw.sale == null) return;
            com.patechltd.salexfypos.print.PrinterManager.print(this, sw.sale, sw.items,
                    (ok, msg) -> runOnUiThread(() -> DialogUtil.toast(this, msg)));
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
                ((TextView) findViewById(R.id.receipt)).setText(buildReceipt(sw.sale, sw.items));
            });
        });
    }

    private String buildReceipt(Sale sale, List<SaleItem> items) {
        StringBuilder sb = new StringBuilder();
        String shop = Prefs.getString(this, Prefs.KEY_SHOP_NAME, "My Shop");
        sb.append(shop).append('\n');
        sb.append("Sale No: ").append(sale.saleNo).append('\n');
        sb.append("Cashier: ").append(sale.cashierName).append('\n');
        sb.append("Date: ").append(DateUtil.format(sale.saleDate)).append('\n');
        sb.append("Status: ").append(sale.status).append('\n');
        sb.append("----------------------------\n");
        for (SaleItem item : items) {
            sb.append(item.productName).append('\n');
            sb.append("  ").append(NumberUtil.qty(item.qty)).append(" × ")
                    .append(NumberUtil.money(item.unitPrice)).append("  = ")
                    .append(NumberUtil.money(item.lineTotal)).append('\n');
        }
        sb.append("----------------------------\n");
        sb.append("Subtotal: ").append(NumberUtil.money(sale.subtotal)).append('\n');
        if (sale.taxAmount > 0) sb.append("Tax: ").append(NumberUtil.money(sale.taxAmount)).append('\n');
        sb.append("TOTAL: ").append(NumberUtil.money(sale.total)).append('\n');
        if ("CREDIT".equals(sale.paymentMethod)) {
            sb.append("Payment: ON CREDIT\n");
            if (sale.customerName != null) sb.append("Customer: ").append(sale.customerName).append('\n');
            if (sale.paidAmount > 0) sb.append("Paid now: ").append(NumberUtil.money(sale.paidAmount)).append('\n');
            sb.append("Balance: ").append(NumberUtil.money(sale.total - sale.paidAmount)).append('\n');
        } else {
            sb.append("Paid: ").append(NumberUtil.money(sale.paidAmount)).append('\n');
            sb.append("Change: ").append(NumberUtil.money(sale.changeAmount)).append('\n');
        }
        if (sale.pointsEarned > 0) {
            sb.append("Loyalty points earned: ").append(NumberUtil.qty(sale.pointsEarned)).append('\n');
        }
        if (sale.notes != null && !sale.notes.isEmpty()) {
            sb.append("Note: ").append(sale.notes).append('\n');
        }
        String footer = Prefs.getString(this, Prefs.KEY_RECEIPT_FOOTER, "");
        if (!footer.isEmpty()) sb.append(footer).append('\n');
        return sb.toString();
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
                            finish();
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> DialogUtil.toast(this, "Could not void sale"));
                    }
                }));
    }
}
