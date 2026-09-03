package com.patechltd.salexfypos.ui.sell;

import android.app.DatePickerDialog;
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
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.ProfitLineRow;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.util.DateUtil;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.Prefs;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ProfitReportActivity extends AppCompatActivity {

    private static final int PAGE_SIZE = 50;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<ProfitLineRow> lines = new ArrayList<>();
    private final List<View> rangeChips = new ArrayList<>();

    private Repository repo;
    private LineAdapter adapter;
    private TextView emptyText;
    private SwitchMaterial switchVoided;
    private TextView revenueVal, costVal, profitVal;
    private long from = 0L;
    private long to = Long.MAX_VALUE;
    private int page;
    private boolean loading;
    private boolean endReached;
    private String currency;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profit_report);

        repo = Repository.get(this);
        currency = Prefs.currency(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        emptyText = findViewById(R.id.empty_text);
        revenueVal = findViewById(R.id.summary_revenue_val);
        costVal = findViewById(R.id.summary_cost_val);
        profitVal = findViewById(R.id.summary_profit_val);

        switchVoided = findViewById(R.id.switch_voided);
        switchVoided.setOnCheckedChangeListener((btn, checked) -> resetAndLoad());

        adapter = new LineAdapter();
        RecyclerView list = findViewById(R.id.profit_list);
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
        final boolean includeVoided = switchVoided.isChecked();
        repo.run(() -> {
            List<ProfitLineRow> rows = repo.sales.getProfitLines(from, to, includeVoided,
                    PAGE_SIZE, pageNo * PAGE_SIZE);
            final double totalRevenue = repo.sales.getSummaryRevenue(from, to, includeVoided);
            final double totalCost = repo.sales.getSummaryCost(from, to, includeVoided);
            final double totalProfit = totalRevenue - totalCost;
            handler.post(() -> {
                if (rows.size() < PAGE_SIZE) endReached = true;
                lines.addAll(rows);
                page++;
                loading = false;
                render();
                if (pageNo == 0) {
                    revenueVal.setText(currency + " " + NumberUtil.money(totalRevenue));
                    costVal.setText(currency + " " + NumberUtil.money(totalCost));
                    profitVal.setText(currency + " " + NumberUtil.money(totalProfit));
                }
            });
        });
    }

    private void render() {
        adapter.notifyDataSetChanged();
        if (lines.isEmpty()) {
            emptyText.setText("No sales in this range");
            emptyText.setVisibility(View.VISIBLE);
        } else {
            emptyText.setVisibility(View.GONE);
        }
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }

    private class LineAdapter extends RecyclerView.Adapter<LineAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_profit_line, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            ProfitLineRow row = lines.get(position);
            boolean voided = "VOID".equals(row.status);

            holder.product.setText(row.productName);
            if (row.unitLabel != null && !row.unitLabel.isEmpty()) {
                holder.product.setText(row.productName + " (" + row.unitLabel + ")");
            }

            holder.qty.setText("Qty " + NumberUtil.qty(row.qty)
                    + "  ·  #" + row.saleNo
                    + "  ·  " + DateUtil.formatDate(row.saleDate));

            String unitStr = currency + " " + NumberUtil.money(row.unitPrice) + " / unit";
            double perUnitCost = row.qty > 0
                    ? (row.stockQty * row.costPrice) / row.qty : 0;
            String costStr = perUnitCost > 0
                    ? "  Cost " + currency + " " + NumberUtil.money(perUnitCost)
                    : "";
            holder.priceInfo.setText(unitStr + costStr);

            String profitStr = currency + " " + NumberUtil.money(row.profit);
            holder.profit.setText(voided ? "VOID" : profitStr);

            if (voided) {
                holder.product.setPaintFlags(holder.product.getPaintFlags()
                        | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                holder.product.setTextColor(getResources().getColor(R.color.error));
                holder.profit.setTextColor(getResources().getColor(R.color.error));
            } else {
                holder.product.setPaintFlags(holder.product.getPaintFlags()
                        & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                holder.product.setTextColor(getResources().getColor(R.color.text_primary));
                holder.profit.setTextColor(getResources().getColor(R.color.success));
            }
        }

        @Override
        public int getItemCount() {
            return lines.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final TextView product, qty, priceInfo, profit;

            VH(View v) {
                super(v);
                product = v.findViewById(R.id.line_product);
                qty = v.findViewById(R.id.line_qty);
                priceInfo = v.findViewById(R.id.line_price_info);
                profit = v.findViewById(R.id.line_profit);
            }
        }
    }
}
