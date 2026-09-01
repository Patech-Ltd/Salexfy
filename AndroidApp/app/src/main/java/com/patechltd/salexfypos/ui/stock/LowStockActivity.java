package com.patechltd.salexfypos.ui.stock;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.adapter.LowStockAdapter;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.StockRow;

public class LowStockActivity extends AppCompatActivity {

    private LowStockAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_low_stock);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        Repository repo = Repository.get(this);

        RecyclerView list = findViewById(R.id.low_stock_list);
        adapter = new LowStockAdapter(this::openQuickStock);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        repo.products.observeLowStockRows().observe(this, rows -> {
            if (rows == null || rows.isEmpty()) {
                adapter.submit(new java.util.ArrayList<>());
                findViewById(R.id.empty_hint).setVisibility(View.VISIBLE);
                return;
            }
            findViewById(R.id.empty_hint).setVisibility(View.GONE);
            adapter.submit(rows);
        });
    }

    private void openQuickStock(StockRow row) {
        Intent i = new Intent(this, QuickStockActivity.class);
        i.putExtra(QuickStockActivity.EXTRA_IDS, new String[]{row.productId});
        i.putExtra(QuickStockActivity.EXTRA_NAMES, new String[]{row.name});
        i.putExtra(QuickStockActivity.EXTRA_UNITS, new String[]{row.unitLabel});
        i.putExtra(QuickStockActivity.EXTRA_CURRENT, new double[]{row.currentQty});
        i.putExtra(QuickStockActivity.EXTRA_NEEDED, new double[]{row.reorderLevel});
        startActivity(i);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}