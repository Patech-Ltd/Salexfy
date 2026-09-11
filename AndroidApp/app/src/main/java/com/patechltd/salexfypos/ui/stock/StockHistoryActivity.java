package com.patechltd.salexfypos.ui.stock;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.adapter.StockMovementAdapter;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.Product;
import com.patechltd.salexfypos.db.entity.StockMovement;
import com.patechltd.salexfypos.util.NumberUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-product stock ledger. Loaded page-by-page off the UI thread using keyset
 * pagination, so it stays fast and memory-safe even with very large histories.
 * Each row shows previous balance, added/removed quantity and the new balance,
 * with type, note, user and time.
 */
public class StockHistoryActivity extends AppCompatActivity {

    public static final String EXTRA_PRODUCT_ID = "productId";

    private static final int PAGE_SIZE = 60;

    private Repository repo;
    private String productId;
    private StockMovementAdapter adapter;
    private TextView countView;

    private boolean loading = false;
    private boolean exhausted = false;
    private long lastCreatedAt;
    private String lastId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_history);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        repo = Repository.get(this);
        productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        if (productId == null) {
            finish();
            return;
        }

        countView = findViewById(R.id.h_count);

        RecyclerView list = findViewById(R.id.h_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        list.setLayoutManager(layoutManager);
        adapter = new StockMovementAdapter();
        list.setAdapter(adapter);
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                int last = layoutManager.findLastVisibleItemPosition();
                int total = layoutManager.getItemCount();
                if (last >= total - 5) loadMore();
            }
        });

        loadHeader();
        loadMore();
    }

    private void loadHeader() {
        repo.run(() -> {
            Product p = repo.products.getById(productId);
            int total = p == null ? 0 : repo.stock.countMovementsForProduct(productId);
            final String name = p == null ? "" : p.name;
            final String unit = p == null || p.retailUnit == null ? "" : p.retailUnit;
            final double qty = p == null ? 0 : repo.stock.currentQty(productId);
            final double reorder = p == null ? 0 : p.reorderLevel;
            final int fTotal = total;
            repo.onMain(() -> renderHeader(name, qty, reorder, unit, fTotal));
        });
    }

    private synchronized void loadMore() {
        if (loading || exhausted || isFinishing() || isDestroyed()) return;
        loading = true;
        adapter.setLoading(true);
        repo.run(() -> {
            List<StockMovement> fetched = null;
            try {
                fetched = repo.stock.getMovementsPage(productId, lastCreatedAt, lastId, PAGE_SIZE);
            } catch (Exception e) {
                fetched = null;
            }
            final List<StockMovement> page = fetched == null ? new ArrayList<>() : fetched;
            repo.onMain(() -> {
                loading = false;
                adapter.setLoading(false);
                if (page == null || page.isEmpty()) {
                    exhausted = true;
                    if (adapter.getItemCount() == 0) {
                        countView.setText("No stock changes yet");
                    }
                    return;
                }
                lastCreatedAt = page.get(page.size() - 1).createdAt;
                lastId = page.get(page.size() - 1).uid;
                adapter.append(page);
                if (page.size() < PAGE_SIZE) exhausted = true;
            });
        });
    }

    private void renderHeader(String name, double qty, double reorder, String unit, int total) {
        String unitSuffix = unit.isEmpty() ? "" : " " + unit;
        ((TextView) findViewById(R.id.h_name)).setText(name);
        TextView current = findViewById(R.id.h_current);
        current.setText(NumberUtil.qty(qty) + unitSuffix + " in stock");
        current.setTextColor(getColor(qty <= reorder ? R.color.error : R.color.amount_text));
        TextView reorderView = findViewById(R.id.h_reorder);
        reorderView.setText("Reorder at " + NumberUtil.qty(reorder) + unitSuffix);
        reorderView.setVisibility(View.VISIBLE);
        countView.setText(total > 0
                ? total + " stock change" + (total == 1 ? "" : "s") + "  •  loading pages"
                : "No stock changes yet");
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}