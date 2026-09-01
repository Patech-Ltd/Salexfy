package com.patechltd.salexfypos.ui.sell;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.ProductStock;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.Category;
import com.patechltd.salexfypos.db.entity.Product;
import com.patechltd.salexfypos.util.ImageUtil;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.Prefs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Full-page product finder for the sell screen. Tapping a product returns
 * its UID in the result intent.
 */
public class ProductSearchActivity extends AppCompatActivity {

    public static final String EXTRA_WHOLESALE = "wholesale";
    public static final String EXTRA_PRODUCT_ID = "productId";

    private Repository repo;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<ProductStock> items = new ArrayList<>();
    private final Map<String, String> unitNames = new HashMap<>();
    private SearchAdapter adapter;
    private boolean wholesale;
    private String query = "";
    private String categoryId = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_search);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        repo = Repository.get(this);
        wholesale = getIntent().getBooleanExtra(EXTRA_WHOLESALE, false);

        RecyclerView list = findViewById(R.id.result_list);
        adapter = new SearchAdapter();
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        EditText search = findViewById(R.id.search_input);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                query = s.toString().trim().toLowerCase(Locale.ROOT);
                refresh();
            }
        });

        load();
    }

    private void load() {
        repo.run(() -> {
            List<Product> all = repo.products.getAllActive();
            List<Category> cats = repo.directory.getCategories();
            for (com.patechltd.salexfypos.db.entity.Unit u : repo.directory.getUnits()) {
                unitNames.put(u.uid, u.name);
            }
            List<String> ids = new ArrayList<>();
            for (Product p : all) ids.add(p.uid);
            Map<String, Double> qtys = new HashMap<>();
            for (com.patechltd.salexfypos.db.ProductQty pq : repo.products.getQtys(ids)) {
                qtys.put(pq.productId, pq.qty);
            }
            List<ProductStock> rows = new ArrayList<>();
            for (Product p : all) {
                ProductStock ps = new ProductStock();
                ps.product = p;
                Double v = qtys.get(p.uid);
                ps.currentQty = v == null ? 0 : v;
                String label = unitNames.get(p.retailUnitId);
                if (label == null || label.isEmpty()) label = p.retailUnit;
                ps.unitLabel = label;
                rows.add(ps);
            }
            handler.post(() -> {
                items.clear();
                items.addAll(rows);
                buildChips(cats);
                refresh();
            });
        });
    }

    private void buildChips(List<Category> cats) {
        LinearLayout chips = findViewById(R.id.category_chips);
        chips.removeAllViews();
        addChip(chips, "All", null, true);
        for (Category c : cats) addChip(chips, c.name, c.uid, false);
    }

    private void addChip(LinearLayout container, String text, String id, boolean selected) {
        TextView chip = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
        lp.setMargins(4, 0, 4, 0);
        chip.setLayoutParams(lp);
        chip.setGravity(Gravity.CENTER);
        chip.setText(text);
        chip.setPadding(dp(14), 0, dp(14), 0);
        chip.setTextColor(selected ? getResources().getColor(R.color.white)
                : getResources().getColor(R.color.text_secondary));
        chip.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
        chip.setTag(id);
        chip.setOnClickListener(v -> {
            categoryId = id;
            for (int i = 0; i < container.getChildCount(); i++) {
                TextView c = (TextView) container.getChildAt(i);
                boolean sel = categoryId == null ? c.getTag() == null
                        : c.getTag() != null && c.getTag().equals(categoryId);
                c.setTextColor(sel ? getResources().getColor(R.color.white)
                        : getResources().getColor(R.color.text_secondary));
                c.setBackgroundResource(sel ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
            }
            refresh();
        });
        container.addView(chip);
    }

    private List<ProductStock> matches() {
        List<ProductStock> out = new ArrayList<>();
        for (ProductStock ps : items) {
            Product p = ps.product;
            if (!p.isActive) continue;
            if (categoryId != null && !categoryId.equals(p.categoryId)) continue;
            if (!query.isEmpty()) {
                boolean hit = p.name != null && p.name.toLowerCase(Locale.ROOT).contains(query);
                if (!hit && p.barcode != null) hit = p.barcode.toLowerCase(Locale.ROOT).contains(query);
                if (!hit && p.sku != null) hit = p.sku.toLowerCase(Locale.ROOT).contains(query);
                if (!hit) continue;
            }
            out.add(ps);
        }
        return out;
    }

    private void refresh() {
        List<ProductStock> rows = matches();
        adapter.submit(rows);
        findViewById(R.id.empty_hint).setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void pick(ProductStock ps) {
        Intent result = new Intent();
        result.putExtra(EXTRA_PRODUCT_ID, ps.product.uid);
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.VH> {

        private final List<ProductStock> rows = new ArrayList<>();

        void submit(List<ProductStock> list) {
            rows.clear();
            rows.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_product, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            final ProductStock ps = rows.get(position);
            Product p = ps.product;
            holder.name.setText(p.name);
            String meta = p.sku == null || p.sku.isEmpty() ? "" : p.sku;
            if (p.barcode != null && !p.barcode.isEmpty()) {
                meta = meta.isEmpty() ? p.barcode : meta + " • " + p.barcode;
            }
            holder.meta.setText(meta);
            holder.barcode.setVisibility(View.GONE);
            holder.image.setTag(p.imagePath);
            ImageUtil.load(holder.image, p.imagePath, 128);
            double price = wholesale ? p.wholesalePrice : p.retailPrice;
            if (price <= 0) price = wholesale ? p.retailPrice : p.wholesalePrice;
            String unit = ps.unitLabel == null ? "" : " / " + ps.unitLabel;
            holder.price.setText(NumberUtil.money(price, Prefs.currency(ProductSearchActivity.this)));
            holder.stock.setText("Stock: " + NumberUtil.qty(ps.currentQty) + unit);
            holder.stock.setVisibility(View.VISIBLE);
            holder.itemView.setOnClickListener(v -> pick(ps));
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final TextView name, meta, barcode, price, stock;
            final ImageView image;

            VH(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.product_name);
                meta = itemView.findViewById(R.id.product_meta);
                barcode = itemView.findViewById(R.id.product_barcode);
                price = itemView.findViewById(R.id.retail_price);
                stock = itemView.findViewById(R.id.stock_qty);
                image = itemView.findViewById(R.id.product_image);
            }
        }
    }
}
