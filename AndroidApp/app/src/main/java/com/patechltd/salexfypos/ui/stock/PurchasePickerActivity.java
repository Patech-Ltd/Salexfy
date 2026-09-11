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
import com.patechltd.salexfypos.db.entity.ProductUnit;
import com.patechltd.salexfypos.util.DialogUtil;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.Prefs;

import java.util.ArrayList;
import java.util.List;

/**
 * Friendly multi-select product picker for purchase batch add. Users type a
 * quantity straight into the field (e.g. "500") or use the +/− steppers,
 * and can pick which unit they are buying in. Returns product ids, quantities,
 * unit labels and factors in the result intent.
 */
public class PurchasePickerActivity extends AppCompatActivity {

    public static final String EXTRA_IDS = "ids";
    public static final String EXTRA_QTYS = "qtys";
    public static final String EXTRA_UNITS = "units";
    public static final String EXTRA_FACTORS = "factors";
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

    private static class UnitOption {
        final String label;
        final String display;
        final double factor;

        UnitOption(String label, String display, double factor) {
            this.label = label;
            this.display = display;
            this.factor = factor;
        }
    }

    private static class Row {
        final Product product;
        final List<UnitOption> units = new ArrayList<>();
        double qty;
        String unitLabel;
        double factor;

        Row(Product product) {
            this.product = product;
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
        search.setHint("Search product name or barcode");
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

    private String defaultUnitLabel(Product p) {
        if (p != null && p.retailUnit != null && !p.retailUnit.trim().isEmpty()) return p.retailUnit.trim();
        return "Pcs";
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
            final List<Row> built = new ArrayList<>();
            for (Product p : pageData) {
                Row row = new Row(p);
                String base = defaultUnitLabel(p);
                row.factor = 1;
                row.unitLabel = base;
                row.units.add(new UnitOption(base, base, 1));
                List<ProductUnit> pUnits = repo.products.getUnitsByProduct(p.uid);
                for (ProductUnit pu : pUnits) {
                    String name = (pu.unitName == null || pu.unitName.isEmpty()) ? "Unit" : pu.unitName;
                    double f = Math.max(0.001, pu.factor);
                    if (!hasOption(row.units, name, f)) {
                        row.units.add(new UnitOption(name, name + (f > 1.001
                                ? "  (" + NumberUtil.qty(f) + " " + base + " each)" : ""), f));
                    }
                }
                if (p.wholesaleFactor > 1) {
                    String name = (p.wholesaleUnit == null || p.wholesaleUnit.isEmpty())
                            ? "Wholesale" : p.wholesaleUnit;
                    double f = Math.max(0.001, p.wholesaleFactor);
                    if (!hasOption(row.units, name, f)) {
                        row.units.add(new UnitOption(name, name + "  (" + NumberUtil.qty(f) + " " + base + " each)", f));
                    }
                }
                built.add(row);
            }
            handler.post(() -> {
                loading = false;
                if (pageData.isEmpty()) {
                    exhausted = true;
                } else {
                    page++;
                    rows.addAll(built);
                    adapter.notifyDataSetChanged();
                }
                emptyHint.setVisibility(rows.isEmpty() && exhausted ? View.VISIBLE : View.GONE);
            });
        });
    }

    private boolean hasOption(List<UnitOption> opts, String label, double factor) {
        for (UnitOption o : opts) {
            if (o.label.equals(label) && Math.abs(o.factor - factor) < 0.001) return true;
        }
        return false;
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
        List<String> units = new ArrayList<>();
        List<String> factors = new ArrayList<>();
        for (Row row : rows) {
            if (row.qty > 0) {
                ids.add(row.product.uid);
                qtys.add(String.valueOf(row.qty));
                units.add(row.unitLabel);
                factors.add(String.valueOf(row.factor));
            }
        }
        if (ids.isEmpty()) {
            setResult(RESULT_CANCELED);
        } else {
            Intent result = new Intent();
            result.putExtra(EXTRA_IDS, ids.toArray(new String[0]));
            result.putExtra(EXTRA_QTYS, qtys.toArray(new String[0]));
            result.putExtra(EXTRA_UNITS, units.toArray(new String[0]));
            result.putExtra(EXTRA_FACTORS, factors.toArray(new String[0]));
            setResult(RESULT_OK, result);
        }
        finish();
    }

    private void pickUnit(final Row row, final int position) {
        List<String> options = new ArrayList<>();
        String c = Prefs.currency(this);
        for (UnitOption uo : row.units) {
            double buy = NumberUtil.round2(row.product.costPrice * uo.factor);
            options.add(uo.display + "   ·   Buy " + c + " " + (buy > 0 ? NumberUtil.money(buy) : "—") + "/" + uo.label);
        }
        int current = -1;
        for (int i = 0; i < row.units.size(); i++) {
            if (row.units.get(i).label.equals(row.unitLabel)
                    && Math.abs(row.units.get(i).factor - row.factor) < 0.001) {
                current = i;
                break;
            }
        }
        DialogUtil.pick(this, "Unit to buy — " + row.product.name,
                options.toArray(new String[0]), current, which -> {
                    if (which < 0 || which >= row.units.size()) return;
                    UnitOption uo = row.units.get(which);
                    row.unitLabel = uo.label;
                    row.factor = uo.factor;
                    adapter.notifyItemChanged(position);
                });
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
            final VH vh = new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_pick_product, parent, false));

            vh.qty.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
                @Override public void afterTextChanged(Editable s) {
                    int pos = vh.getBindingAdapterPosition();
                    if (pos < 0 || pos >= rows.size()) return;
                    Row row = rows.get(pos);
                    double parsed = NumberUtil.parse(s == null ? "" : s.toString(), 0);
                    if (Math.abs(parsed - row.qty) > 0.0001) {
                        boolean wasZero = row.qty <= 0;
                        row.qty = Math.max(0, parsed);
                        boolean nowZero = row.qty <= 0;
                        vh.check.setChecked(!nowZero);
                        if (wasZero && !nowZero) selected++;
                        else if (!wasZero && nowZero && selected > 0) selected--;
                        updateFooter();
                    }
                }
            });

            vh.minus.setOnClickListener(v -> {
                int pos = vh.getBindingAdapterPosition();
                if (pos < 0 || pos >= rows.size()) return;
                Row row = rows.get(pos);
                boolean wasZero = row.qty <= 0;
                if (row.qty > 1) {
                    row.qty--;
                } else if (row.qty == 1) {
                    row.qty = 0;
                }
                boolean nowZero = row.qty <= 0;
                vh.check.setChecked(!nowZero);
                if (wasZero && !nowZero) selected++;
                else if (!wasZero && nowZero && selected > 0) selected--;
                vh.qty.setText(NumberUtil.qty(row.qty));
                updateFooter();
            });

            vh.plus.setOnClickListener(v -> {
                int pos = vh.getBindingAdapterPosition();
                if (pos < 0 || pos >= rows.size()) return;
                Row row = rows.get(pos);
                boolean wasZero = row.qty <= 0;
                row.qty++;
                vh.check.setChecked(true);
                if (wasZero) selected++;
                vh.qty.setText(NumberUtil.qty(row.qty));
                updateFooter();
            });

            vh.unit.setOnClickListener(v -> {
                int pos = vh.getBindingAdapterPosition();
                if (pos < 0 || pos >= rows.size()) return;
                pickUnit(rows.get(pos), pos);
            });

            vh.check.setOnClickListener(v -> {
                int pos = vh.getBindingAdapterPosition();
                if (pos < 0 || pos >= rows.size()) return;
                Row row = rows.get(pos);
                boolean now = vh.check.isChecked();
                boolean wasZero = row.qty <= 0;
                if (now && wasZero) {
                    row.qty = Math.max(1, row.qty);
                    selected++;
                } else if (!now && !wasZero) {
                    row.qty = 0;
                    if (selected > 0) selected--;
                }
                vh.qty.setText(NumberUtil.qty(row.qty));
                updateFooter();
            });

            return vh;
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            final Row row = rows.get(position);
            final Product p = row.product;
            holder.name.setText(p.name);
            holder.sub.setText(buildSub(row));
            holder.unit.setText(row.unitLabel + "  ▾");
            holder.check.setChecked(row.qty > 0);
            holder.qty.setText(NumberUtil.qty(row.qty));
        }

        private String buildSub(Row row) {
            Product p = row.product;
            String id = (p.sku != null && !p.sku.isEmpty()) ? p.sku
                    : ((p.barcode != null && !p.barcode.isEmpty()) ? p.barcode : "No barcode");
            double buy = NumberUtil.round2(p.costPrice * row.factor);
            String c = Prefs.currency(PurchasePickerActivity.this);
            return id + "  ·  Buy " + c + " " + (buy > 0 ? NumberUtil.money(buy) : "—") + "/" + row.unitLabel;
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final CheckBox check;
            final TextView name, sub, unit;
            final EditText qty;
            final View minus, plus;

            VH(@NonNull View itemView) {
                super(itemView);
                check = itemView.findViewById(R.id.pick_check);
                name = itemView.findViewById(R.id.pick_name);
                sub = itemView.findViewById(R.id.pick_sub);
                unit = itemView.findViewById(R.id.pick_unit);
                qty = itemView.findViewById(R.id.pick_qty);
                minus = itemView.findViewById(R.id.qty_minus);
                plus = itemView.findViewById(R.id.qty_plus);
            }
        }
    }
}