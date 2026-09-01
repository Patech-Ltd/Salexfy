package com.patechltd.salexfypos.ui.stock;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.PurchaseWithItems;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.Supplier;
import com.patechltd.salexfypos.print.ReceiptPrinter;
import com.patechltd.salexfypos.util.DialogUtil;

/**
 * Thermal-paper style preview of a purchase invoice, with one-tap printing.
 */
public class PurchasePreviewActivity extends AppCompatActivity {

    private Repository repo;
    private String purchaseId;
    private PurchaseWithItems cached;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase_preview);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        repo = Repository.get(this);
        purchaseId = getIntent().getStringExtra("purchaseId");
        if (purchaseId == null) {
            finish();
            return;
        }

        findViewById(R.id.btn_print).setOnClickListener(v -> print());
        load();
    }

    private void load() {
        repo.run(() -> {
            PurchaseWithItems pw = repo.purchases.getPurchaseWithItems(purchaseId);
            cached = pw;
            String supplierName = supplierName(pw);
            runOnUiThread(() -> {
                if (pw == null || pw.purchase == null) {
                    finish();
                    return;
                }
                String text = ReceiptPrinter.buildPurchaseText(
                        this, pw.purchase, pw.items == null ? new java.util.ArrayList<>() : pw.items,
                        supplierName);
                ((TextView) findViewById(R.id.invoice)).setText(text);
            });
        });
    }

    private String supplierName(PurchaseWithItems pw) {
        if (pw == null || pw.purchase == null || pw.purchase.supplierId == null) return null;
        Supplier s = repo.suppliers.getSupplier(pw.purchase.supplierId);
        return s == null ? null : s.name;
    }

    private void print() {
        repo.run(() -> {
            PurchaseWithItems pw = cached != null ? cached : repo.purchases.getPurchaseWithItems(purchaseId);
            if (pw == null || pw.purchase == null) return;
            com.patechltd.salexfypos.print.PrinterManager.printPurchase(this, pw.purchase,
                    pw.items == null ? new java.util.ArrayList<>() : pw.items, supplierName(pw),
                    (ok, msg) -> runOnUiThread(() -> DialogUtil.toast(this, msg)));
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}