package com.patechltd.salexfypos.ui.stock;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.adapter.PurchaseLineAdapter;
import com.patechltd.salexfypos.db.PurchaseWithItems;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.Purchase;
import com.patechltd.salexfypos.db.entity.PurchaseItem;
import com.patechltd.salexfypos.db.entity.Supplier;
import com.patechltd.salexfypos.model.Authority;
import com.patechltd.salexfypos.security.PermissionChecker;
import com.patechltd.salexfypos.util.DateUtil;
import com.patechltd.salexfypos.util.DialogUtil;
import com.patechltd.salexfypos.util.NumberUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only view of a purchase invoice. Editing is a separate action that
 * opens {@link PurchaseEditActivity}.
 */
public class PurchaseViewActivity extends AppCompatActivity {

    private Repository repo;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String purchaseId;
    private String existingSupplierName;
    private PurchaseLineAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase_view);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        repo = Repository.get(this);
        purchaseId = getIntent().getStringExtra("id");
        if (purchaseId == null) {
            finish();
            return;
        }

        RecyclerView list = findViewById(R.id.items_list);
        adapter = new PurchaseLineAdapter(new PurchaseLineAdapter.Listener() {
            @Override
            public void onClick(int position) {
            }

            @Override
            public void onEditCost(int position) {
            }

            @Override
            public void onRemove(int position) {
            }
        });
        adapter.setReadOnly(true);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        MaterialButton btnEdit = findViewById(R.id.btn_edit);
        MaterialButton btnDelete = findViewById(R.id.btn_delete);
        boolean canEdit = PermissionChecker.has(this, Authority.PURCHASE_EDIT);
        if (canEdit) {
            btnEdit.setOnClickListener(v ->
                    startActivity(new android.content.Intent(this, PurchaseEditActivity.class)
                            .putExtra("id", purchaseId)));
            btnDelete.setOnClickListener(v -> confirmDelete());
        } else {
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
        }

        findViewById(R.id.btn_preview).setOnClickListener(v ->
                startActivity(new android.content.Intent(this, PurchasePreviewActivity.class)
                        .putExtra("purchaseId", purchaseId)));
        findViewById(R.id.btn_print_invoice).setOnClickListener(v -> printInvoice());

        load();
    }

    private void load() {
        repo.run(() -> {
            PurchaseWithItems pw = repo.purchases.getPurchaseWithItems(purchaseId);
            String supplierName = null;
            if (pw != null && pw.purchase != null && pw.purchase.supplierId != null) {
                Supplier s = repo.suppliers.getSupplier(pw.purchase.supplierId);
                supplierName = s == null ? "—" : s.name;
            }
            final String fSupplier = supplierName;
            handler.post(() -> {
                if (pw == null || pw.purchase == null) {
                    finish();
                    return;
                }
                Purchase p = pw.purchase;
                existingSupplierName = fSupplier != null && !"—".equals(fSupplier) ? fSupplier : null;
                ((TextView) findViewById(R.id.supplier_row)).setText(
                        "Supplier: " + (fSupplier == null ? "—" : fSupplier));
                ((TextView) findViewById(R.id.invoice_no)).setText(p.invoiceNo);
                TextView dateField = findViewById(R.id.date_field);
                dateField.setText(p.purchaseDate > 0 ? DateUtil.formatDate(p.purchaseDate) : "—");
                TextView notes = findViewById(R.id.notes);
                if (p.notes != null && !p.notes.trim().isEmpty()) {
                    notes.setText(p.notes);
                    notes.setVisibility(View.VISIBLE);
                }
                List<PurchaseItem> items = pw.items == null ? new ArrayList<>() : pw.items;
                adapter.submit(items);
                double subtotal = 0;
                for (PurchaseItem item : items) subtotal += item.lineTotal;
                ((TextView) findViewById(R.id.subtotal)).setText(NumberUtil.money(subtotal));
                ((TextView) findViewById(R.id.total)).setText(NumberUtil.money(p.total));
                ((TextView) findViewById(R.id.paid_amount)).setText(NumberUtil.money(p.paidAmount));
                ((TextView) findViewById(R.id.balance_amount)).setText(
                        NumberUtil.money(Math.max(0, p.total - p.paidAmount)));
            });
        });
    }

    private void printInvoice() {
        repo.run(() -> {
            PurchaseWithItems pw = repo.purchases.getPurchaseWithItems(purchaseId);
            if (pw == null || pw.purchase == null) return;
            String supplierName = existingSupplierName;
            if (supplierName == null && pw.purchase.supplierId != null) {
                Supplier s = repo.suppliers.getSupplier(pw.purchase.supplierId);
                supplierName = s == null ? null : s.name;
            }
            final PurchaseWithItems fPw = pw;
            final String fName = supplierName;
            handler.post(() -> com.patechltd.salexfypos.print.PrinterManager.printPurchase(this,
                    fPw.purchase, fPw.items == null ? new ArrayList<>() : fPw.items, fName,
                    (ok, msg) -> runOnUiThread(() -> DialogUtil.toast(this, msg))));
        });
    }

    private void confirmDelete() {
        DialogUtil.confirm(this, "Delete purchase?",
                "This will remove the stock added by this purchase.", () -> repo.run(() -> {
                    Purchase p = repo.purchases.getPurchase(purchaseId);
                    if (p != null) repo.deletePurchase(p);
                    handler.post(this::finish);
                }));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}