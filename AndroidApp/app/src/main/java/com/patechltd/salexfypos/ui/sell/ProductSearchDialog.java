package com.patechltd.salexfypos.ui.sell;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.entity.Category;
import com.patechltd.salexfypos.db.entity.Product;
import com.patechltd.salexfypos.util.ImageUtil;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.Prefs;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductSearchDialog {

    public interface OnPick {
        void onProduct(Product product);
    }

    private final Context context;
    private final List<Product> products;
    private final List<Category> categories;
    private final boolean wholesale;
    private final OnPick callback;

    private String query = "";
    private String categoryId = null;

    public ProductSearchDialog(Context context, List<Product> products, List<Category> categories,
                               boolean wholesale, OnPick callback) {
        this.context = context;
        this.products = products;
        this.categories = categories;
        this.wholesale = wholesale;
        this.callback = callback;
    }

    public void show() {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_product_search, null);
        AutoCompleteTextView input = view.findViewById(R.id.search_input);
        LinearLayout chips = view.findViewById(R.id.category_chips);
        RecyclerView list = view.findViewById(R.id.result_list);
        list.setLayoutManager(new LinearLayoutManager(context));

        List<String> names = new ArrayList<>();
        for (Product p : products) names.add(p.name);
        ArrayAdapter<String> suggest = new ArrayAdapter<>(context,
                android.R.layout.simple_dropdown_item_1line, names);
        input.setAdapter(suggest);
        input.setThreshold(1);

        SearchAdapter adapter = new SearchAdapter();
        list.setAdapter(adapter);

        addChip(chips, "All", null, true, adapter);
        if (categories != null) {
            for (Category c : categories) addChip(chips, c.name, c.uid, false, adapter);
        }

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                query = s.toString().trim().toLowerCase(Locale.ROOT);
                adapter.refresh();
            }
        });

        new MaterialAlertDialogBuilder(context)
                .setTitle("Search product")
                .setView(view)
                .setNegativeButton("Close", null)
                .show();
    }

    private void addChip(LinearLayout container, String text, String id, boolean selected, SearchAdapter adapter) {
        TextView chip = new TextView(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
        lp.setMargins(4, 0, 4, 0);
        chip.setLayoutParams(lp);
        chip.setGravity(Gravity.CENTER);
        chip.setText(text);
        chip.setPadding(dp(14), 0, dp(14), 0);
        chip.setTextColor(selected ? context.getResources().getColor(R.color.white)
                : context.getResources().getColor(R.color.text_secondary));
        chip.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
        chip.setOnClickListener(v -> {
            categoryId = id;
            refreshChips(container);
            adapter.refresh();
        });
        chip.setTag(id);
        container.addView(chip);
    }

    private void refreshChips(LinearLayout container) {
        for (int i = 0; i < container.getChildCount(); i++) {
            TextView chip = (TextView) container.getChildAt(i);
            boolean selected = categoryId == null
                    ? chip.getTag() == null
                    : chip.getTag() != null && chip.getTag().equals(categoryId);
            chip.setTextColor(selected ? context.getResources().getColor(R.color.white)
                    : context.getResources().getColor(R.color.text_secondary));
            chip.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
        }
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private List<Product> matches() {
        List<Product> out = new ArrayList<>();
        for (Product p : products) {
            if (!p.isActive) continue;
            if (categoryId != null && !categoryId.equals(p.categoryId)) continue;
            if (!query.isEmpty()) {
                boolean hit = p.name != null && p.name.toLowerCase(Locale.ROOT).contains(query);
                if (!hit && p.barcode != null) hit = p.barcode.toLowerCase(Locale.ROOT).contains(query);
                if (!hit && p.sku != null) hit = p.sku.toLowerCase(Locale.ROOT).contains(query);
                if (!hit) continue;
            }
            out.add(p);
        }
        return out;
    }

    private class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.VH> {

        private final List<Product> items = new ArrayList<>();

        SearchAdapter() {
            refresh();
        }

        void refresh() {
            items.clear();
            items.addAll(matches());
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
            final Product p = items.get(position);
            holder.name.setText(p.name);
            String meta = p.sku == null || p.sku.isEmpty() ? "" : p.sku;
            if (p.barcode != null && !p.barcode.isEmpty()) {
                meta = meta.isEmpty() ? p.barcode : meta + " • " + p.barcode;
            }
            holder.meta.setText(meta);
            holder.barcode.setVisibility(View.GONE);
            holder.stock.setVisibility(View.GONE);
            holder.image.setTag(p.imagePath);
            ImageUtil.load(holder.image, p.imagePath, 128);
            double price = wholesale ? p.wholesalePrice : p.retailPrice;
            if (price <= 0) price = wholesale ? p.retailPrice : p.wholesalePrice;
            holder.price.setText(NumberUtil.money(price, Prefs.currency(context)));
            holder.itemView.setOnClickListener(v -> {
                if (callback != null) callback.onProduct(p);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
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
