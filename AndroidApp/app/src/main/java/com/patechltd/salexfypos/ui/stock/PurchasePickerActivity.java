package com.patechltd.salexfypos.ui.stock;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.Product;
import com.patechltd.salexfypos.util.NumberUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Friendly multi-select product picker for purchase batch add. Users tick
 * products and set a quantity per row (no comma typing). Returns the selected
 * product ids and quantities in the result intent.
 */
public class PurchasePickerActivity extends AppCompatActivity {

    public static final String EXTRA_IDS = "ids";
    public static final String EXTRA_QTYS = "qtys";
    private static final int PAGE_SIZE = 50;

    private Repository repo;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Row> rows = new ArrayList<>();
    private PickAdapter adapter;
    private TextView selectedCount, emptyHint;
    private MaterialButton addButton;
    private String query = "";
    private int page;
    private boolean loading;
    private boolean exhausted;
    private int selected;

    private static class Row {
        final Product product;
        double qty;

        Row(Product product) {
            this.product = product;
            this.qty = 0;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase_picker);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        repo = Repository.get(this);

        RecyclerView list = findViewById(R.id.picker_list);
        adapter = new PickAdapter();
        LinearLayoutManager lm = new LinearLayoutManager(this);
        list.setLayoutManager(lm);
        list.setAdapter(adapter);
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0) return;
                int total = lm.getItemCount();
                int last = lm.findLastVisibleItemPosition();
                if (total > 0 && last >= total - 3) loadPage();
            }
        });

        selectedCount = findViewById(R.id.selected_count);
        emptyHint = findViewById(R.id.empty_hint);
        addButton = findViewById(R.id.btn_add_selected);
        addButton.setOnClickListener(v -> done());

        EditText search = findViewById(R.id.picker_search);
        search.addTextChangedListener(new TextWatcher() {
            private final Runnable debounce = () -> {
                query = search.getText() == null ? "" : search.getText().toString().trim();
                resetAndReload();
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

        loadPage();
        updateFooter();
    }

    private void resetAndReload() {
        rows.clear();
        adapter.notifyDataSetChanged();
        selected = 0;
        updateFooter();
        page = 0;
        exhausted = false;
        loadPage();
    }

    private void loadPage() {
        if (loading || exhausted) return;
        loading = true;
        final int pageNo = page;
        repo.run(() -> {
            List<Product> pageData = repo.products.searchActivePage(query, null, PAGE_SIZE, pageNo * PAGE_SIZE);
            handler.post(() -> {
                loading = false;
                if (pageData.isEmpty()) {
                    exhausted = true;
                } else {
                    page++;
                    for (Product p : pageData) rows.add(new Row(p));
                    adapter.notifyDataSetChanged();
                }
                emptyHint.setVisibility(rows.isEmpty() && exhausted ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void updateFooter() {
        selectedCount.setText(selected == 0
                ? "No items selected"
                : selected + " item" + (selected == 1 ? "" : "s") + " selected");
        addButton.setText(selected == 0 ? "Add selected" : "Add selected (" + selected + ")");
    }

    private void done() {
        List<String> ids = new ArrayList<>();
        List<String> qtys = new ArrayList<>();
        for (Row row : rows) {
            if (row.qty > 0) {
                ids.add(row.product.uid);
                qtys.add(String.valueOf(row.qty));
            }
        }
        if (ids.isEmpty()) {
            setResult(RESULT_CANCELED);
        } else {
            Intent result = new Intent();
            result.putExtra(EXTRA_IDS, ids.toArray(new String[0]));
            result.putExtra(EXTRA_QTYS, qtys.toArray(new String[0]));
            setResult(RESULT_OK, result);
        }
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private class PickAdapter extends RecyclerView.Adapter<PickAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_pick_product, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            final Row row = rows.get(position);
            final Product p = row.product;
            holder.name.setText(p.name);
            String sub = p.sku == null || p.sku.isEmpty() ? (p.barcode == null ? "" : p.barcode)
                    : p.sku;
            String unit = p.wholesalePrice > 0
                    ? (p.wholesaleUnit == null || p.wholesaleUnit.isEmpty() ? "ctn" : p.wholesaleUnit)
                    : (p.retailUnit == null || p.retailUnit.isEmpty() ? "Pcs" : p.retailUnit);
            double price = p.wholesalePrice > 0 ? p.wholesalePrice : p.retailPrice;
            sub = (sub == null || sub.isEmpty() ? "No barcode" : sub)
                    + "  ·  " + NumberUtil.money(price)
                    + (unit.isEmpty() ? "" : "/" + unit);
            holder.sub.setText(sub);
            holder.check.setChecked(row.qty > 0);
            holder.qtyValue.setText(String.valueOf((long) row.qty));
            holder.itemView.setOnClickListener(v -> {
                boolean now = !holder.check.isChecked();
                holder.check.setChecked(now);
                row.qty = now ? Math.max(1, row.qty) : 0;
                if (now) selected++;
                else selected--;
                adapter.notifyItemChanged(position);
                updateFooter();
            });
            holder.check.setOnClickListener(v -> {
                boolean now = holder.check.isChecked();
                row.qty = now ? Math.max(1, row.qty) : 0;
                if (now) selected++;
                else selected--;
                adapter.notifyItemChanged(position);
                updateFooter();
            });
            holder.minus.setOnClickListener(v -> {
                if (row.qty > 1) {
                    row.qty--;
                } else if (row.qty == 1) {
                    row.qty = 0;
                    holder.check.setChecked(false);
                    if (selected > 0) selected--;
                }
                holder.qtyValue.setText(String.valueOf((long) row.qty));
                updateFooter();
            });
            holder.plus.setOnClickListener(v -> {
                if (row.qty == 0) selected++;
                row.qty++;
                holder.check.setChecked(true);
                holder.qtyValue.setText(String.valueOf((long) row.qty));
                updateFooter();
            });
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final CheckBox check;
            final TextView name, sub, qtyValue;
            final View minus, plus;

            VH(@NonNull View itemView) {
                super(itemView);
                check = itemView.findViewById(R.id.pick_check);
                name = itemView.findViewById(R.id.pick_name);
                sub = itemView.findViewById(R.id.pick_sub);
                qtyValue = itemView.findViewById(R.id.qty_value);
                minus = itemView.findViewById(R.id.qty_minus);
                plus = itemView.findViewById(R.id.qty_plus);
            }
        }
    }
}