package com.patechltd.salexfypos.ui.sell;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.DeletedSaleItemRow;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.SaleWithItems;
import com.patechltd.salexfypos.db.entity.Sale;
import com.patechltd.salexfypos.model.Authority;
import com.patechltd.salexfypos.security.PermissionChecker;
import com.patechltd.salexfypos.sync.SyncEvents;
import com.patechltd.salexfypos.util.DateUtil;
import com.patechltd.salexfypos.util.DialogUtil;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.Prefs;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SalesHistoryActivity extends AppCompatActivity {

    private static final int PAGE_SIZE = 25;
    private static final String PREFS_NAME = "deleted_items_kept";
    private static final String KEY_KEPT_IDS = "kept_ids";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable syncListener = () -> handler.post(this::resetAndLoad);
    private final List<SaleWithItems> sales = new ArrayList<>();
    private final List<View> rangeChips = new ArrayList<>();
    private final Set<String> deletedItemSaleIds = new HashSet<>();

    private Repository repo;
    private SalesHistoryAdapter adapter;
    private DeletedItemsAdapter deletedItemsAdapter;
    private TextView emptyText;
    private TextView tabAllSales;
    private TextView tabDeletedItems;
    private RecyclerView salesList;
    private RecyclerView deletedItemsList;
    private LinearLayout rangeChipsHost;
    private LinearLayout wholesaleChipsHost;
    private String query = "";
    private long from = 0L;
    private long to = Long.MAX_VALUE;
    private int page;
    private boolean loading;
    private boolean endReached;
    private int wholesaleFilter;
    private boolean canVoid;
    private int currentTab = 0;
    private final List<View> wholesaleChips = new ArrayList<>();

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

        canVoid = PermissionChecker.has(this, Authority.SALE_VOID);

        tabAllSales = findViewById(R.id.tab_all_sales);
        tabDeletedItems = findViewById(R.id.tab_deleted_items);
        salesList = findViewById(R.id.sales_list);
        deletedItemsList = findViewById(R.id.deleted_items_list);
        rangeChipsHost = findViewById(R.id.range_chips);
        wholesaleChipsHost = findViewById(R.id.wholesale_chips);
        emptyText = findViewById(R.id.empty_text);

        adapter = new SalesHistoryAdapter(new SalesHistoryAdapter.Listener() {
            @Override
            public void onOpen(int position) {
                if (position < 0 || position >= sales.size()) return;
                Intent i = new Intent(SalesHistoryActivity.this, SaleDetailActivity.class);
                i.putExtra("saleId", sales.get(position).sale.uid);
                startActivity(i);
            }

            @Override
            public void onVoid(int position) {
                confirmVoid(position);
            }
        });

        LinearLayoutManager lm = new LinearLayoutManager(this);
        salesList.setLayoutManager(lm);
        salesList.setAdapter(adapter);
        salesList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0) return;
                int total = lm.getItemCount();
                int last = lm.findLastVisibleItemPosition();
                if (total > 0 && last >= total - 3) loadNextPage();
            }
        });

        deletedItemsAdapter = new DeletedItemsAdapter(new DeletedItemsAdapter.Listener() {
            @Override
            public void onKeep(int position) {
                DeletedSaleItemRow item = deletedItemsAdapter.getItem(position);
                if (item != null) {
                    markKept(item.saleItemId);
                    deletedItemsAdapter.removeAt(position);
                    if (deletedItemsAdapter.getItemCount() == 0) {
                        emptyText.setText("No deleted items to review");
                        emptyText.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onDelete(int position) {
                DeletedSaleItemRow item = deletedItemsAdapter.getItem(position);
                if (item != null) {
                    confirmDeleteItem(position, item);
                }
            }
        });
        deletedItemsList.setLayoutManager(new LinearLayoutManager(this));
        deletedItemsList.setAdapter(deletedItemsAdapter);

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

        tabAllSales.setOnClickListener(v -> switchTab(0));
        tabDeletedItems.setOnClickListener(v -> switchTab(1));

        buildRangeChips();
        buildWholesaleChips();

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

    private void switchTab(int tab) {
        if (currentTab == tab) return;
        currentTab = tab;
        boolean allSales = tab == 0;

        tabAllSales.setBackgroundResource(allSales ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
        tabAllSales.setTextColor(allSales ? 0xFFFFFFFF : getResources().getColor(R.color.text_primary));
        tabDeletedItems.setBackgroundResource(!allSales ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
        tabDeletedItems.setTextColor(!allSales ? 0xFFFFFFFF : getResources().getColor(R.color.text_primary));

        salesList.setVisibility(allSales ? View.VISIBLE : View.GONE);
        deletedItemsList.setVisibility(allSales ? View.GONE : View.VISIBLE);
        rangeChipsHost.setVisibility(allSales ? View.VISIBLE : View.GONE);
        wholesaleChipsHost.setVisibility(allSales ? View.VISIBLE : View.GONE);

        resetAndLoad();
    }

    private Set<String> getKeptIds() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_KEPT_IDS, "");
        Set<String> kept = new HashSet<>();
        if (!raw.isEmpty()) {
            for (String id : raw.split(",")) {
                if (!id.isEmpty()) kept.add(id);
            }
        }
        return kept;
    }

    private void markKept(String itemId) {
        Set<String> kept = getKeptIds();
        kept.add(itemId);
        StringBuilder sb = new StringBuilder();
        for (String id : kept) {
            if (sb.length() > 0) sb.append(",");
            sb.append(id);
        }
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_KEPT_IDS, sb.toString()).apply();
    }

    private void buildRangeChips() {
        String[] names = {"All", "Today", "Yesterday", "7 Days", "This Month", "Custom"};
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
            rangeChipsHost.addView(chip, lp);
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

    private static final String[] WHOLESALE_FILTERS = {"All sales", "Wholesale", "Retail"};

    private void buildWholesaleChips() {
        for (int i = 0; i < WHOLESALE_FILTERS.length; i++) {
            TextView chip = new TextView(this);
            chip.setText(WHOLESALE_FILTERS[i]);
            chip.setTextSize(13);
            chip.setTextColor(getResources().getColor(R.color.text_primary));
            chip.setBackgroundResource(R.drawable.bg_chip);
            chip.setPadding(dp(14), dp(6), dp(14), dp(6));
            final int idx = i;
            chip.setOnClickListener(v -> {
                wholesaleFilter = idx;
                selectWholesaleChip(idx);
                resetAndLoad();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(8));
            lp.bottomMargin = dp(6);
            wholesaleChipsHost.addView(chip, lp);
            wholesaleChips.add(chip);
        }
        selectWholesaleChip(0);
    }

    private void selectWholesaleChip(int idx) {
        for (int i = 0; i < wholesaleChips.size(); i++) {
            wholesaleChips.get(i).setBackgroundResource(i == idx ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
            ((TextView) wholesaleChips.get(i)).setTextColor(i == idx ? 0xFFFFFFFF : getResources().getColor(R.color.text_primary));
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
                    repo.sales.exportComplete(query, from, to, wholesaleFilter, 0);
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
        loading = false;
        sales.clear();
        render();

        if (currentTab == 0) {
            loadNextPage();
        } else {
            loadDeletedItems();
        }
    }

    private void loadNextPage() {
        if (loading || endReached) return;
        loading = true;
        final int pageNo = page;
        repo.run(() -> {
            final List<String> deletedIds = repo.sales.saleIdsWithDeletedItems();
            final List<SaleWithItems> rows =
                    repo.sales.searchCompletePage(query, from, to, wholesaleFilter, 0,
                            PAGE_SIZE, pageNo * PAGE_SIZE);
            handler.post(() -> {
                deletedItemSaleIds.clear();
                deletedItemSaleIds.addAll(deletedIds);
                if (rows.size() < PAGE_SIZE) endReached = true;
                sales.addAll(rows);
                page++;
                loading = false;
                render();
            });
        });
    }

    private void loadDeletedItems() {
        repo.run(() -> {
            final List<DeletedSaleItemRow> rows = repo.sales.getDeletedSaleItems();
            final Set<String> kept = getKeptIds();
            final List<DeletedSaleItemRow> filtered = new ArrayList<>();
            for (DeletedSaleItemRow row : rows) {
                if (!kept.contains(row.saleItemId)) {
                    filtered.add(row);
                }
            }
            handler.post(() -> {
                deletedItemsAdapter.submit(filtered);
                if (filtered.isEmpty()) {
                    emptyText.setText("No deleted items to review");
                    emptyText.setVisibility(View.VISIBLE);
                } else {
                    emptyText.setVisibility(View.GONE);
                }
            });
        });
    }

    private void render() {
        if (currentTab != 0) return;
        String currency = Prefs.currency(this);
        int posColor = getResources().getColor(R.color.accent_positive);
        int negColor = getResources().getColor(R.color.accent_negative);
        List<SalesHistoryAdapter.Row> out = new ArrayList<>();
        for (SaleWithItems sw : sales) {
            if (sw == null || sw.sale == null) continue;
            int count = sw.items == null ? 0 : sw.items.size();
            boolean voided = "VOID".equals(sw.sale.status);
            boolean hasWholesale = false;
            if (sw.items != null) {
                for (com.patechltd.salexfypos.db.entity.SaleItem it : sw.items) {
                    if (it.isWholesale) {
                        hasWholesale = true;
                        break;
                    }
                }
            }
            String sub = (hasWholesale ? "Wholesale · " : "")
                    + DateUtil.formatDate(sw.sale.saleDate) + " " + DateUtil.formatTime(sw.sale.saleDate)
                    + " · " + count + " item" + (count == 1 ? "" : "s")
                    + " · " + com.patechltd.salexfypos.model.PaymentMethod.labelOf(sw.sale.paymentMethod);
            if (voided) sub = "VOIDED — " + sub;
            if (deletedItemSaleIds.contains(sw.sale.uid)) sub = sub + " · ⚠ deleted item";
            out.add(new SalesHistoryAdapter.Row(
                    "#" + sw.sale.saleNo,
                    sub,
                    currency + " " + NumberUtil.money(sw.sale.total),
                    voided ? negColor : posColor,
                    canVoid && !voided));
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

    private void confirmVoid(int position) {
        if (position < 0 || position >= sales.size()) return;
        SaleWithItems sw = sales.get(position);
        if (sw == null || sw.sale == null) return;
        if (!canVoid) {
            DialogUtil.toast(this, "You don't have permission to void sales");
            return;
        }
        DialogUtil.confirm(this, "Void sale #" + sw.sale.saleNo + "?",
                "Stock will be returned to inventory.", () -> repo.run(() -> {
                    Sale sale = repo.sales.getSale(sw.sale.uid);
                    if (sale == null) return;
                    try {
                        repo.voidSale(sale);
                        runOnUiThread(() -> {
                            DialogUtil.toast(this, "Sale voided");
                            resetAndLoad();
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> DialogUtil.toast(this, "Could not void sale"));
                    }
                }));
    }

    private void confirmDeleteItem(int position, DeletedSaleItemRow item) {
        DialogUtil.confirm(this, "Delete item from sale?",
                "Remove '" + item.productName + "' from sale #" + item.saleNo
                        + "? Totals will be recalculated.",
                () -> repo.run(() -> {
                    try {
                        repo.removeDeletedSaleItem(item.saleItemId);
                        runOnUiThread(() -> {
                            markKept(item.saleItemId);
                            deletedItemsAdapter.removeAt(position);
                            DialogUtil.toast(this, "Item removed");
                            if (deletedItemsAdapter.getItemCount() == 0) {
                                emptyText.setText("No deleted items to review");
                                emptyText.setVisibility(View.VISIBLE);
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> DialogUtil.toast(this, "Could not delete item"));
                    }
                }));
    }

    private static class SalesHistoryAdapter extends RecyclerView.Adapter<SalesHistoryAdapter.VH> {

        interface Listener {
            void onOpen(int position);

            void onVoid(int position);
        }

        static class Row {
            final String title;
            final String subtitle;
            final String value;
            final int valueColor;
            final boolean showVoid;

            Row(String title, String subtitle, String value, int valueColor, boolean showVoid) {
                this.title = title;
                this.subtitle = subtitle;
                this.value = value;
                this.valueColor = valueColor;
                this.showVoid = showVoid;
            }
        }

        private final List<Row> items = new ArrayList<>();
        private final Listener listener;

        SalesHistoryAdapter(Listener listener) {
            this.listener = listener;
        }

        void submit(List<Row> list) {
            items.clear();
            items.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_sale_history_row, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Row row = items.get(position);
            final int pos = position;
            holder.title.setText(row.title);
            holder.subtitle.setText(row.subtitle);
            holder.value.setText(row.value);
            if (row.valueColor != 0) holder.value.setTextColor(row.valueColor);
            holder.itemView.setOnClickListener(v -> listener.onOpen(pos));
            holder.btnVoid.setVisibility(row.showVoid ? View.VISIBLE : View.GONE);
            holder.btnVoid.setOnClickListener(v -> listener.onVoid(pos));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final TextView title, subtitle, value;
            final ImageView btnVoid;

            VH(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.title);
                subtitle = itemView.findViewById(R.id.subtitle);
                value = itemView.findViewById(R.id.value);
                btnVoid = itemView.findViewById(R.id.btn_void_row);
            }
        }
    }

    private static class DeletedItemsAdapter extends RecyclerView.Adapter<DeletedItemsAdapter.VH> {

        interface Listener {
            void onKeep(int position);

            void onDelete(int position);
        }

        private final List<DeletedSaleItemRow> items = new ArrayList<>();
        private final Listener listener;

        DeletedItemsAdapter(Listener listener) {
            this.listener = listener;
        }

        void submit(List<DeletedSaleItemRow> list) {
            items.clear();
            items.addAll(list);
            notifyDataSetChanged();
        }

        DeletedSaleItemRow getItem(int position) {
            if (position >= 0 && position < items.size()) return items.get(position);
            return null;
        }

        void removeAt(int position) {
            if (position >= 0 && position < items.size()) {
                items.remove(position);
                notifyItemRemoved(position);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_deleted_sale_item, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            DeletedSaleItemRow row = items.get(position);
            String unit = row.unitLabel == null || row.unitLabel.isEmpty() ? "" : " " + row.unitLabel;
            holder.name.setText(row.productName + unit + " × " + NumberUtil.qty(row.qty));
            holder.saleInfo.setText("#" + row.saleNo + " · "
                    + DateUtil.formatDate(row.saleDate) + " " + DateUtil.formatTime(row.saleDate)
                    + " · " + (row.cashierName != null ? row.cashierName : ""));
            holder.total.setText(Prefs.currency(holder.itemView.getContext()) + " " + NumberUtil.money(row.lineTotal));
            final int pos = position;
            holder.btnKeep.setOnClickListener(v -> listener.onKeep(pos));
            holder.btnDelete.setOnClickListener(v -> listener.onDelete(pos));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final TextView name, saleInfo, total;
            final com.google.android.material.button.MaterialButton btnKeep, btnDelete;

            VH(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.deleted_item_name);
                saleInfo = itemView.findViewById(R.id.deleted_item_sale_info);
                total = itemView.findViewById(R.id.deleted_item_total);
                btnKeep = itemView.findViewById(R.id.btn_keep_item);
                btnDelete = itemView.findViewById(R.id.btn_delete_item);
            }
        }
    }
}
