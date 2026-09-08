package com.patechltd.salexfypos.ui.stock;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.StockValuationRow;
import com.patechltd.salexfypos.db.StockValuationSummary;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.Prefs;

import java.util.ArrayList;
import java.util.List;

public class StockSummaryActivity extends AppCompatActivity {

    private static final int PAGE = 300;

    private Repository repo;
    private BreakdownAdapter adapter;
    private final List<StockValuationRow> rows = new ArrayList<>();
    private String query = "";
    private boolean loading = false;
    private boolean exhausted = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_summary);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView list = findViewById(R.id.breakdown_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        list.setLayoutManager(layoutManager);
        adapter = new BreakdownAdapter();
        list.setAdapter(adapter);
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                int last = layoutManager.findLastVisibleItemPosition();
                int total = layoutManager.getItemCount();
                if (last >= total - 5) loadMore();
            }
        });

        EditText search = findViewById(R.id.breakdown_search);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                query = s == null ? "" : s.toString();
                rows.clear();
                exhausted = false;
                loadMore();
            }
        });

        repo = Repository.get(this);
        repo.products.observeValuationSummary().observe(this, this::renderSummary);
        loadMore();
    }

    private void loadMore() {
        if (loading || exhausted) return;
        loading = true;
        final String q = query;
        final int offset = rows.size();
        repo.run(() -> {
            List<StockValuationRow> fetched;
            try {
                fetched = repo.products.getValuationRows(q, PAGE, offset);
            } catch (Exception e) {
                fetched = new ArrayList<>();
            }
            final List<StockValuationRow> page = fetched;
            repo.onMain(() -> {
                loading = false;
                if (!q.equals(query)) {
                    loadMore();
                    return;
                }
                rows.addAll(page);
                if (page.size() < PAGE) exhausted = true;
                adapter.setLoading(!exhausted);
                adapter.notifyDataSetChanged();
            });
        });
    }

    private void renderSummary(StockValuationSummary s) {
        if (s == null) return;
        String cur = Prefs.currency(this);
        ((TextView) findViewById(R.id.summary_products)).setText(NumberUtil.qty(s.products));
        ((TextView) findViewById(R.id.summary_qty)).setText(NumberUtil.qty(s.totalQty));
        ((TextView) findViewById(R.id.summary_assets)).setText(NumberUtil.money(s.assets, cur));
        ((TextView) findViewById(R.id.summary_retail_sales)).setText(NumberUtil.money(s.retailSales, cur));
        ((TextView) findViewById(R.id.summary_wholesale_sales)).setText(NumberUtil.money(s.wholesaleSales, cur));
        renderProfit((TextView) findViewById(R.id.summary_retail_profit), s.retailSales - s.assets, cur);
        renderProfit((TextView) findViewById(R.id.summary_wholesale_profit), s.wholesaleSales - s.assets, cur);
    }

    private void renderProfit(TextView view, double value, String currency) {
        view.setText(NumberUtil.money(value, currency));
        view.setTextColor(getColor(value < 0 ? R.color.error : R.color.success));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private class BreakdownAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int TYPE_ROW = 0;
        private static final int TYPE_LOADING = 1;

        private boolean loading = false;

        void setLoading(boolean loading) {
            this.loading = loading;
        }

        @Override
        public int getItemViewType(int position) {
            return position < rows.size() ? TYPE_ROW : TYPE_LOADING;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_LOADING) {
                return new LoadingVH(inflater.inflate(R.layout.item_loading, parent, false));
            }
            return new VH(inflater.inflate(R.layout.item_stock_valuation, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof VH) {
                StockValuationRow r = rows.get(position);
                VH vh = (VH) holder;
                String cur = Prefs.currency(StockSummaryActivity.this);
                vh.name.setText(r.name);
                String unit = r.unitLabel == null ? "" : r.unitLabel;
                vh.qty.setText(NumberUtil.qty(r.qty) + " " + unit);
                vh.assets.setText(NumberUtil.money(r.assets, cur));
                vh.retailSales.setText(NumberUtil.money(r.retailSales, cur));
                vh.wholesaleSales.setText(NumberUtil.money(r.wholesaleSales, cur));
                vh.retailProfit.setText("Retail profit " + NumberUtil.money(r.retailSales - r.assets, cur));
                vh.retailProfit.setTextColor(getColor(r.retailSales - r.assets < 0 ? R.color.error : R.color.success));
                vh.wholesaleProfit.setText("Wholesale profit " + NumberUtil.money(r.wholesaleSales - r.assets, cur));
                vh.wholesaleProfit.setTextColor(getColor(r.wholesaleSales - r.assets < 0 ? R.color.error : R.color.success));
            }
        }

        @Override
        public int getItemCount() {
            return rows.size() + (loading ? 1 : 0);
        }

        class VH extends RecyclerView.ViewHolder {
            final TextView name, qty, assets, retailSales, wholesaleSales, retailProfit, wholesaleProfit;

            VH(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.bd_name);
                qty = itemView.findViewById(R.id.bd_qty);
                assets = itemView.findViewById(R.id.bd_assets);
                retailSales = itemView.findViewById(R.id.bd_retail_sales);
                wholesaleSales = itemView.findViewById(R.id.bd_wholesale_sales);
                retailProfit = itemView.findViewById(R.id.bd_retail_profit);
                wholesaleProfit = itemView.findViewById(R.id.bd_wholesale_profit);
            }
        }

        class LoadingVH extends RecyclerView.ViewHolder {
            LoadingVH(@NonNull View itemView) {
                super(itemView);
            }
        }
    }
}