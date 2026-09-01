package com.patechltd.salexfypos.ui.sell;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.SaleWithItems;
import com.patechltd.salexfypos.print.ReceiptPrinter;
import com.patechltd.salexfypos.util.DialogUtil;

/**
 * Thermal-paper style preview of a sale receipt, with one-tap printing.
 */
public class ReceiptPreviewActivity extends AppCompatActivity {

    private Repository repo;
    private String saleId;
    private SaleWithItems cached;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt_preview);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        repo = Repository.get(this);
        saleId = getIntent().getStringExtra("saleId");
        if (saleId == null) {
            finish();
            return;
        }

        findViewById(R.id.btn_print).setOnClickListener(v -> print());
        load();
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
                String text = ReceiptPrinter.buildReceiptText(this, sw.sale, sw.items, sw.payments);
                ((TextView) findViewById(R.id.receipt)).setText(text);
            });
        });
    }

    private void print() {
        repo.run(() -> {
            SaleWithItems sw = cached != null ? cached : repo.sales.getSaleWithItems(saleId);
            if (sw == null || sw.sale == null) return;
            com.patechltd.salexfypos.print.PrinterManager.print(this, sw.sale, sw.items, sw.payments,
                    (ok, msg) -> runOnUiThread(() -> DialogUtil.toast(this, msg)));
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
