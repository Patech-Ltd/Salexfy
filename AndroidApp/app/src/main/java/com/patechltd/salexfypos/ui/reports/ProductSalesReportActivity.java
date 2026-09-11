package com.patechltd.salexfypos.ui.reports;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.ProductSalesRow;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.ui.sell.CustomerSearchActivity;
import com.patechltd.salexfypos.ui.sell.ProductSearchActivity;
import com.patechltd.salexfypos.util.DateUtil;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.Prefs;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ProductSalesReportActivity extends AppCompatActivity {

    private static final int PAGE_SIZE = 50;
    private static final int REQ_PRODUCT = 7001;
    private static final int REQ_CUSTOMER = 7002;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<ProductSalesRow> lines = new ArrayList<>();
    private final List<View> rangeChips = new ArrayList<>();

    private Repository repo;
    private SalesAdapter adapter;
    private TextView emptyText;
    private TextView productsVal, revenueVal, profitVal;
    private TextView productVal, customerVal;
    private long from = 0L;
    private long to = Long.MAX_VALUE;
    private String productId;
    private String productName;
    private String customerId;
    private String customerName;
    private int page;
    private boolean loading;
    private boolean endReached;
    private String currency;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_sales_report);

        repo = Repository.get(this);
        currency = Prefs.currency(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        emptyText = findViewById(R.id.empty_text);
        productsVal = findViewById(R.id.summary_products_val);
        revenueVal = findViewById(R.id.summary_revenue_val);
        profitVal = findViewById(R.id.summary_profit_val);
        productVal = findViewById(R.id.filter_product_val);
        customerVal = findViewById(R.id.filter_customer_val);

        findViewById(R.id.filter_product).setOnClickListener(v -> onProductFilterTap());
        findViewById(R.id.filter_customer).setOnClickListener(v -> onCustomerFilterTap());

        adapter = new SalesAdapter();
        RecyclerView list = findViewById(R.id.sales_list);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        list.setLayoutManager(lm);
        list.setAdapter(adapter);
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0) return;
                int total = lm.getItemCount();
                int last = lm.findLastVisibleItemPosition();
                if (total > 0 && last >= total - 3) loadNextPage();
            }
        });

        buildRangeChips();
        resetAndLoad();
    }

    private void buildRangeChips() {
        String[] names = {"All", "Today", "Yesterday", "7 Days", "This Month", "Custom"};
        LinearLayout host = findViewById(R.id.range_chips);
        for (int i = 0; i < names.length; i++) {
            TextView chip = new TextView(this);
            chip.setText(names[i]);
            chip.setTextSize(13);
            chip.setTextColor(getResources().getColor(R.color.text_primary));
            chip.setBackgroundResource(R.drawable.bg_chip);
            chip.setPadding(dp(14), dp(6), dp(14), dp(6));
            final int idx = i;
            chip.setOnClickListener(v -> {
                selectChip(idx);
                applyRange(idx);
                resetAndLoad();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(8));
            host.addView(chip, lp);
            rangeChips.add(chip);
        }
        selectChip(0);
    }

    private void selectChip(int idx) {
        for (int i = 0; i < rangeChips.size(); i++) {
            rangeChips.get(i).setBackgroundResource(i == idx ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
            ((TextView) rangeChips.get(i)).setTextColor(i == idx ? 0xFFFFFFFF : getResources().getColor(R.color.text_primary));
        }
    }

    private void applyRange(int idx) {
        long now = System.currentTimeMillis();
        switch (idx) {
            case 0:
                from = 0L;
                to = Long.MAX_VALUE;
                break;
            case 1:
                from = DateUtil.startOfDay(now);
                to = DateUtil.endOfDay(now);
                break;
            case 2:
                from = DateUtil.startOfDay(now - 86400000L);
                to = DateUtil.endOfDay(now - 86400000L);
                break;
            case 3:
                from = DateUtil.startOfDay(now - 6 * 86400000L);
                to = DateUtil.endOfDay(now);
                break;
            case 4:
                from = DateUtil.startOfMonth(now);
                to = DateUtil.endOfDay(now);
                break;
            case 5:
                pickCustomRange();
                break;
        }
    }

    private void pickCustomRange() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dp = new DatePickerDialog(this, (v, y, m, d) -> {
            Calendar start = Calendar.getInstance();
            start.clear();
            start.set(y, m, d);
            from = start.getTimeInMillis();
            DatePickerDialog dp2 = new DatePickerDialog(this, (v2, y2, m2, d2) -> {
                Calendar end = Calendar.getInstance();
                end.clear();
                end.set(y2, m2, d2);
                end.set(Calendar.HOUR_OF_DAY, 23);
                end.set(Calendar.MINUTE, 59);
                end.set(Calendar.SECOND, 59);
                to = end.getTimeInMillis();
                resetAndLoad();
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            dp2.show();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dp.show();
    }

    private void onProductFilterTap() {
        if (productId != null) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Product filter")
                    .setItems(new String[]{"Change product", "All products"}, (dialog, which) -> {
                        if (which == 0) startActivityForResult(
                                new Intent(this, ProductSearchActivity.class)
                                        .putExtra(ProductSearchActivity.EXTRA_WHOLESALE, false),
                                REQ_PRODUCT);
                        else {
                            productId = null;
                            productName = null;
                            productVal.setText("All products");
                            resetAndLoad();
                        }
                    })
                    .show();
        } else {
            startActivityForResult(new Intent(this, ProductSearchActivity.class)
                    .putExtra(ProductSearchActivity.EXTRA_WHOLESALE, false), REQ_PRODUCT);
        }
    }

    private void onCustomerFilterTap() {
        if (customerId != null) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Customer filter")
                    .setItems(new String[]{"Change customer", "All customers"}, (dialog, which) -> {
                        if (which == 0) startActivityForResult(
                                new Intent(this, CustomerSearchActivity.class), REQ_CUSTOMER);
                        else {
                            customerId = null;
                            customerName = null;
                            customerVal.setText("All customers");
                            resetAndLoad();
                        }
                    })
                    .show();
        } else {
            startActivityForResult(new Intent(this, CustomerSearchActivity.class), REQ_CUSTOMER);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        if (requestCode == REQ_PRODUCT) {
            final String id = data.getStringExtra(ProductSearchActivity.EXTRA_PRODUCT_ID);
            if (id == null) return;
            repo.run(() -> {
                com.patechltd.salexfypos.db.entity.Product p = repo.products.getById(id);
                handler.post(() -> {
                    productId = id;
                    productName = p == null ? null : p.name;
                    productVal.setText(p == null ? "Selected product" : p.name);
                    resetAndLoad();
                });
            });
        } else if (requestCode == REQ_CUSTOMER) {
            final String id = data.getStringExtra(CustomerSearchActivity.EXTRA_CUSTOMER_ID);
            if (id == null) return;
            repo.run(() -> {
                com.patechltd.salexfypos.db.entity.Customer c = repo.suppliers.getCustomer(id);
                handler.post(() -> {
                    customerId = id;
                    customerName = c == null ? null : c.name;
                    customerVal.setText(c == null ? "Selected customer" : c.name);
                    resetAndLoad();
                });
            });
        }
    }

    private void resetAndLoad() {
        page = 0;
        endReached = false;
        lines.clear();
        adapter.notifyDataSetChanged();
        loadNextPage();
    }

    private void loadNextPage() {
        if (loading || endReached) return;
        loading = true;
        final int pageNo = page;
        repo.run(() -> {
            final List<ProductSalesRow> rows = repo.sales.getProductSales(from, to,
                    productId, customerId, PAGE_SIZE, pageNo * PAGE_SIZE);
            final int totalProducts = repo.sales.countProductSales(from, to, productId, customerId);
            final com.patechltd.salexfypos.db.ProductSalesSummaryRow summary =
                    repo.sales.getProductSalesSummary(from, to, productId, customerId);
            handler.post(() -> {
                if (rows.size() < PAGE_SIZE) endReached = true;
                lines.addAll(rows);
                page++;
                loading = false;
                render();
                if (pageNo == 0) {
                    double profit = summary.revenue - summary.cost;
                    productsVal.setText(totalProducts + " · " + summary.saleCount + " sales");
                    revenueVal.setText(currency + " " + NumberUtil.money(summary.revenue));
                    profitVal.setText(currency + " " + NumberUtil.money(profit));
                }
            });
        });
    }

    private void render() {
        adapter.notifyDataSetChanged();
        emptyText.setVisibility(lines.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }

    private class SalesAdapter extends RecyclerView.Adapter<SalesAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_product_sales, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            ProductSalesRow row = lines.get(position);

            String name = row.name == null || row.name.isEmpty() ? "(no product)" : row.name;
            holder.name.setText(name);

            String unit = row.unitLabel == null || row.unitLabel.isEmpty() ? "Pcs" : row.unitLabel;
            StringBuilder qty = new StringBuilder("Sold ")
                    .append(NumberUtil.qty(row.qty)).append(" ").append(unit);
            if (row.stockQty > 0 && Math.abs(row.stockQty - row.qty) > 0.0001) {
                qty.append("  (").append(NumberUtil.qty(row.stockQty)).append(" base units)");
            }
            qty.append("  ·  ").append(row.saleCount).append(row.saleCount == 1 ? " sale" : " sales");
            holder.qty.setText(qty.toString());

            holder.revenue.setText("Revenue " + currency + " " + NumberUtil.money(row.revenue));

            String profitStr = currency + " " + NumberUtil.money(row.profit);
            holder.profit.setText(profitStr);
            holder.profit.setTextColor(getResources().getColor(
                    row.profit < 0 ? R.color.error : R.color.success));

            if (customerId != null && customerName != null && !customerName.isEmpty()) {
                holder.extra.setText("Customer: " + customerName);
            } else {
                holder.extra.setText("");
            }
        }

        @Override
        public int getItemCount() {
            return lines.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final TextView name, qty, revenue, profit, extra;

            VH(View v) {
                super(v);
                name = v.findViewById(R.id.ps_name);
                qty = v.findViewById(R.id.ps_qty);
                revenue = v.findViewById(R.id.ps_revenue);
                profit = v.findViewById(R.id.ps_profit);
                extra = v.findViewById(R.id.ps_extra);
            }
        }
    }
}