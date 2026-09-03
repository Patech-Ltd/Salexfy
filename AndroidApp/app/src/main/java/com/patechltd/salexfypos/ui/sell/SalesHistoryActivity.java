package com.patechltd.salexfypos.ui.sell;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.adapter.KeyValueAdapter;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.SaleWithItems;
import com.patechltd.salexfypos.sync.SyncEvents;
import com.patechltd.salexfypos.util.DateUtil;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.Prefs;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class SalesHistoryActivity extends AppCompatActivity {

    private static final int PAGE_SIZE = 25;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable syncListener = () -> handler.post(this::resetAndLoad);
    private final List<SaleWithItems> sales = new ArrayList<>();
    private final List<View> rangeChips = new ArrayList<>();

    private Repository repo;
    private KeyValueAdapter adapter;
    private TextView emptyText;
    private String query = "";
    private long from = 0L;
    private long to = Long.MAX_VALUE;
    private int page;
    private boolean loading;
    private boolean endReached;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales_history);

        repo = Repository.get(this);

        ((com.google.android.material.appbar.MaterialToolbar) findViewById(R.id.toolbar))
                .setNavigationOnClickListener(v -> finish());

        findViewById(R.id.btn_manual_sale).setOnClickListener(v ->
                startActivity(new Intent(this, ManualSaleActivity.class)));

        findViewById(R.id.btn_export_sales).setOnClickListener(v ->
                com.patechltd.salexfypos.util.StorageUtil.createReportUri(this,
                        "sales_" + DateUtil.formatDate(System.currentTimeMillis())));

        adapter = new KeyValueAdapter();
        adapter.setListener(position -> {
            if (position < 0 || position >= sales.size()) return;
            Intent i = new Intent(this, SaleDetailActivity.class);
            i.putExtra("saleId", sales.get(position).sale.uid);
            startActivity(i);
        });

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

        emptyText = findViewById(R.id.empty_text);

        TextInputEditText searchInput = findViewById(R.id.search_input);
        searchInput.addTextChangedListener(new TextWatcher() {
            private final Runnable debounce = () -> {
                query = searchInput.getText() == null ? "" : searchInput.getText().toString().trim();
                resetAndLoad();
            };

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                handler.removeCallbacks(debounce);
                handler.postDelayed(debounce, 400);
            }
        });

        buildRangeChips();

        SyncEvents.addListener(syncListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SyncEvents.removeListener(syncListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        resetAndLoad();
    }

    private void buildRangeChips() {
        String[] names = {"All", "Today", "Yesterday", "7 Days", "This Month", "Custom"};
        LinearLayout host = findViewById(R.id.range_chips);
        for (int i = 0; i < names.length; i++) {
            TextView chip = new TextView(this);
            chip.setText(names[i]);
            chip.setTextSize(13);
            chip.setTextColor(0xFF2F3E46);
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
            ((TextView) rangeChips.get(i)).setTextColor(i == idx ? 0xFFFFFFFF : 0xFF2F3E46);
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

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == com.patechltd.salexfypos.util.StorageUtil.REQ_CREATE_REPORT
                && resultCode == RESULT_OK && data != null && data.getData() != null) {
            exportSales(data.getData());
        }
    }

    private void exportSales(android.net.Uri uri) {
        repo.run(() -> {
            final List<SaleWithItems> list =
                    repo.sales.exportComplete(query, from, to);
            boolean ok = com.patechltd.salexfypos.util.ExcelUtil.export(this, uri,
                    "Sales", list, new com.patechltd.salexfypos.util.ExcelUtil.RowWriter() {
                        @Override public Object[] header() {
                            return new Object[]{"Sale No", "Date", "Time", "Payment", "Cashier",
                                    "Customer", "Subtotal", "Tax", "Discount", "Total",
                                    "Paid", "Change", "Items"};
                        }
                        @Override public Object[] row(Object item, int index) {
                            SaleWithItems sw = (SaleWithItems) item;
                            if (sw == null || sw.sale == null) return null;
                            int items = sw.items == null ? 0 : sw.items.size();
                            return new Object[]{sw.sale.saleNo,
                                    DateUtil.formatDate(sw.sale.saleDate),
                                    DateUtil.formatTime(sw.sale.saleDate),
                                    com.patechltd.salexfypos.model.PaymentMethod.labelOf(sw.sale.paymentMethod),
                                    sw.sale.cashierName, sw.sale.customerName,
                                    sw.sale.subtotal, sw.sale.taxAmount, sw.sale.discount,
                                    sw.sale.total, sw.sale.paidAmount, sw.sale.changeAmount, items};
                        }
                    });
            handler.post(() -> android.widget.Toast.makeText(this,
                    ok ? "Sales exported" : "Export failed", android.widget.Toast.LENGTH_SHORT).show());
        });
    }

    private void resetAndLoad() {
        page = 0;
        endReached = false;
        sales.clear();
        render();
        loadNextPage();
    }

    private void loadNextPage() {
        if (loading || endReached) return;
        loading = true;
        final int pageNo = page;
        repo.run(() -> {
            final List<SaleWithItems> rows =
                    repo.sales.searchCompletePage(query, from, to, PAGE_SIZE, pageNo * PAGE_SIZE);
            handler.post(() -> {
                if (rows.size() < PAGE_SIZE) endReached = true;
                sales.addAll(rows);
                page++;
                loading = false;
                render();
            });
        });
    }

    private void render() {
        String currency = Prefs.currency(this);
        List<KeyValueAdapter.Row> out = new ArrayList<>();
        for (SaleWithItems sw : sales) {
            if (sw == null || sw.sale == null) continue;
            int count = sw.items == null ? 0 : sw.items.size();
            boolean voided = "VOID".equals(sw.sale.status);
            String sub = DateUtil.formatDate(sw.sale.saleDate) + " " + DateUtil.formatTime(sw.sale.saleDate)
                    + " · " + count + " item" + (count == 1 ? "" : "s")
                    + " · " + com.patechltd.salexfypos.model.PaymentMethod.labelOf(sw.sale.paymentMethod);
            if (voided) sub = "VOIDED — " + sub;
            out.add(new KeyValueAdapter.Row(
                    "#" + sw.sale.saleNo,
                    sub,
                    currency + " " + NumberUtil.money(sw.sale.total),
                    voided ? 0xFFB00020 : 0xFF1565C0));
        }
        adapter.submit(out);
        if (out.isEmpty()) {
            emptyText.setText(query.isEmpty()
                    ? "No completed sales in this range"
                    : "No sales match your search");
            emptyText.setVisibility(View.VISIBLE);
        } else {
            emptyText.setVisibility(View.GONE);
        }
    }
}
