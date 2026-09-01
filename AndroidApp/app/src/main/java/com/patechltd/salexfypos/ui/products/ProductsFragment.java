package com.patechltd.salexfypos.ui.products;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.adapter.ProductAdapter;
import com.patechltd.salexfypos.db.ProductStock;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.Brand;
import com.patechltd.salexfypos.db.entity.Category;
import com.patechltd.salexfypos.db.entity.Product;
import com.patechltd.salexfypos.model.Authority;
import com.patechltd.salexfypos.security.PermissionChecker;
import com.patechltd.salexfypos.sync.SyncEvents;
import com.patechltd.salexfypos.util.AppLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductsFragment extends Fragment {

    private static final int REQ_SCAN = 4001;
    private static final int REQ_PICK_DIRECTORY = 4002;
    private static final int PAGE_SIZE = 50;
    private Repository repo;
    private ProductAdapter adapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable syncListener = () -> handler.post(this::reload);
    private String currentQuery = "";
    private String currentCategoryId = null;
    private Map<String, String> categoryNames = new HashMap<>();
    private Map<String, String> brandNames = new HashMap<>();
    private Map<String, String> unitNames = new HashMap<>();
    private List<Category> categories = new ArrayList<>();
    private final List<Product> suggestionProducts = new ArrayList<>();
    private SuggestionsAdapter suggestionAdapter;
    private final List<ProductStock> allRows = new ArrayList<>();
    private int page;
    private boolean loading;
    private boolean endReached;
    private int generation;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_products, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = Repository.get(requireContext());

        RecyclerView list = view.findViewById(R.id.product_list);
        AutoCompleteTextView search = view.findViewById(R.id.search_input);
        MaterialButton btnCategories = view.findViewById(R.id.btn_categories);
        MaterialButton btnAdd = view.findViewById(R.id.btn_add_product);

        suggestionAdapter = new SuggestionsAdapter(LayoutInflater.from(requireContext()));
        search.setAdapter(suggestionAdapter);
        search.setThreshold(1);
        search.setOnItemClickListener((parent, view1, position, id) -> {
            if (position < 0 || position >= suggestionProducts.size()) return;
            Product p = suggestionProducts.get(position);
            search.setText(p.name == null ? "" : p.name);
            search.setSelection(search.getText().length());
            openProductById(p.uid);
        });

        view.findViewById(R.id.btn_scan_products).setOnClickListener(v ->
                startActivityForResult(
                        new Intent(requireContext(), com.patechltd.salexfypos.ui.scan.ScanActivity.class),
                        REQ_SCAN));

        adapter = new ProductAdapter(this::openProduct);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        list.setLayoutManager(layoutManager);
        list.setAdapter(adapter);
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0) return;
                int total = layoutManager.getItemCount();
                int last = layoutManager.findLastVisibleItemPosition();
                if (total > 0 && last >= total - 5) loadNextPage();
            }
        });

        boolean canEdit = PermissionChecker.has(requireContext(), Authority.PRODUCT_EDIT);
        btnAdd.setVisibility(canEdit ? View.VISIBLE : View.GONE);
        btnAdd.setOnClickListener(v -> {
            Intent i = new Intent(requireContext(), ProductEditActivity.class);
            startActivity(i);
        });

        MaterialButton btnQuickAdd = view.findViewById(R.id.btn_quick_add);
        btnQuickAdd.setVisibility(canEdit ? View.VISIBLE : View.GONE);
        btnQuickAdd.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), QuickAddProductActivity.class)));

        MaterialButton btnBatchAdd = view.findViewById(R.id.btn_batch_add_products);
        btnBatchAdd.setVisibility(canEdit ? View.VISIBLE : View.GONE);
        btnBatchAdd.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), BatchAddProductsActivity.class)));

        btnCategories.setOnClickListener(v -> showCategories());

        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                currentQuery = s.toString().trim();
                reload();
                fetchSuggestions(currentQuery);
            }
        });

        loadReferences();
        reload();
    }

    @Override
    public void onResume() {
        super.onResume();
        SyncEvents.addListener(syncListener);
        loadReferences();
        reload();
    }

    @Override
    public void onPause() {
        super.onPause();
        SyncEvents.removeListener(syncListener);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_SCAN && resultCode == android.app.Activity.RESULT_OK && data != null) {
            String code = data.getStringExtra(com.patechltd.salexfypos.ui.scan.ScanActivity.EXTRA_CODE);
            if (code != null && !code.isEmpty()) {
                EditText search = getView() == null ? null : getView().findViewById(R.id.search_input);
                if (search != null) search.setText(code.trim());
            }
            return;
        }
        if (requestCode == REQ_PICK_DIRECTORY && resultCode == android.app.Activity.RESULT_OK && data != null) {
            int index = data.getIntExtra(PickerActivity.EXTRA_INDEX, -1);
            Class<?> cls = index == 0 ? CategoryActivity.class
                    : index == 1 ? BrandActivity.class : UnitActivity.class;
            startActivity(new Intent(requireContext(), cls));
        }
    }

    private void loadReferences() {
        repo.run(() -> {
            List<Category> cats = repo.directory.getCategories();
            List<Brand> brands = repo.directory.getBrands();
            List<com.patechltd.salexfypos.db.entity.Unit> units = repo.directory.getUnits();
            handler.post(() -> {
                categories = cats;
                categoryNames.clear();
                for (Category c : cats) categoryNames.put(c.uid, c.name);
                brandNames.clear();
                for (Brand b : brands) brandNames.put(b.uid, b.name);
                unitNames.clear();
                for (com.patechltd.salexfypos.db.entity.Unit u : units) unitNames.put(u.uid, u.name);
                buildCategoryChips();
            });
        });
    }

    private void buildCategoryChips() {
        View view = getView();
        if (view == null) return;
        LinearLayout chips = view.findViewById(R.id.category_chips);
        chips.removeAllViews();
        addChip(chips, "All", null, true);
        for (Category c : categories) {
            addChip(chips, c.name, c.uid, false);
        }
    }

    private void addChip(LinearLayout container, String text, String id, boolean selected) {
        TextView chip = new TextView(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
        lp.setMargins(4, 0, 4, 0);
        chip.setLayoutParams(lp);
        chip.setGravity(android.view.Gravity.CENTER);
        chip.setText(text);
        chip.setPadding(dp(14), 0, dp(14), 0);
        chip.setTextColor(selected ? getResources().getColor(R.color.white) : getResources().getColor(R.color.text_secondary));
        chip.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
        chip.setOnClickListener(v -> {
            currentCategoryId = id;
            reload();
            buildCategoryChips();
        });
        container.addView(chip);
    }

    private void reload() {
        generation++;
        page = 0;
        endReached = false;
        loading = false;
        allRows.clear();
        adapter.submit(new ArrayList<>());
        loadNextPage();
    }

    private void loadNextPage() {
        if (loading || endReached) return;
        loading = true;
        final int gen = generation;
        final int offset = page * PAGE_SIZE;
        final String q = currentQuery;
        final String cat = currentCategoryId;
        repo.thenOnMain(repo.io(() -> {
            List<Product> list = repo.products.searchPage(q, cat, PAGE_SIZE, offset);
            List<String> ids = new ArrayList<>();
            for (Product p : list) ids.add(p.uid);
            Map<String, Double> qtys = new HashMap<>();
            for (com.patechltd.salexfypos.db.ProductQty pq : repo.products.getQtys(ids)) {
                qtys.put(pq.productId, pq.qty);
            }
            List<ProductStock> rows = new ArrayList<>();
            for (Product p : list) {
                ProductStock ps = new ProductStock();
                ps.product = p;
                Double v = qtys.get(p.uid);
                ps.currentQty = v == null ? 0 : v;
                ps.categoryName = categoryNames.get(p.categoryId);
                ps.brandName = brandNames.get(p.brandId);
                String label = unitNames.get(p.retailUnitId);
                if (label == null || label.isEmpty()) label = p.retailUnit;
                ps.unitLabel = label;
                rows.add(ps);
            }
            return rows;
        }), rows -> {
            if (gen != generation) return;
            loading = false;
            if (rows.size() < PAGE_SIZE) endReached = true;
            if (page == 0) allRows.clear();
            allRows.addAll(rows);
            page++;
            adapter.submit(new ArrayList<>(allRows));
        });
    }

    private final Runnable suggestionRunnable = new Runnable() {
        @Override
        public void run() {
            AutoCompleteTextView search = getView() == null ? null : getView().findViewById(R.id.search_input);
            if (search == null) return;
            if (currentQuery.isEmpty()) {
                suggestionProducts.clear();
                suggestionAdapter.setItems(new ArrayList<>());
                return;
            }
        repo.io(() -> {
                List<Product> matches = repo.products.searchTop(currentQuery, currentCategoryId, 8);
                handler.post(() -> {
                    suggestionProducts.clear();
                    suggestionProducts.addAll(matches);
                    List<String> labels = new ArrayList<>();
                    for (Product p : matches) {
                        String label = p.name;
                        if (p.barcode != null && !p.barcode.isEmpty()) label += "  •  " + p.barcode;
                        labels.add(label);
                    }
                    suggestionAdapter.setItems(labels);
                    if (suggestionAdapter.getCount() > 0) {
                        search.showDropDown();
                    }
                });
                return null;
            });
        }
    };

    private void fetchSuggestions(String q) {
        handler.removeCallbacks(suggestionRunnable);
        handler.postDelayed(suggestionRunnable, 250);
    }

    private void openProduct(ProductStock ps) {
        openProductById(ps.product.uid);
    }

    private void openProductById(String id) {
        Intent i = new Intent(requireContext(), ProductDetailActivity.class);
        i.putExtra("id", id);
        startActivity(i);
    }

    private void showCategories() {
        Intent i = new Intent(requireContext(), PickerActivity.class);
        i.putExtra(PickerActivity.EXTRA_TITLE, "Manage directories");
        ArrayList<String> options = new ArrayList<>();
        options.add("Categories");
        options.add("Brands");
        options.add("Units of measure");
        i.putExtra(PickerActivity.EXTRA_ITEMS, options);
        startActivityForResult(i, REQ_PICK_DIRECTORY);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    static class SuggestionsAdapter extends BaseAdapter implements android.widget.Filterable {

        private final LayoutInflater inflater;
        private final List<String> items = new ArrayList<>();

        SuggestionsAdapter(LayoutInflater inflater) {
            this.inflater = inflater;
        }

        void setItems(List<String> labels) {
            items.clear();
            items.addAll(labels);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv = convertView instanceof TextView
                    ? (TextView) convertView
                    : (TextView) inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            tv.setText(items.get(position));
            return tv;
        }

        @Override
        public android.widget.Filter getFilter() {
            return new android.widget.Filter() {
                @Override
                protected android.widget.Filter.FilterResults performFiltering(CharSequence constraint) {
                    android.widget.Filter.FilterResults results = new android.widget.Filter.FilterResults();
                    results.values = items;
                    results.count = items.size();
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, android.widget.Filter.FilterResults results) {
                    notifyDataSetChanged();
                }
            };
        }
    }
}
