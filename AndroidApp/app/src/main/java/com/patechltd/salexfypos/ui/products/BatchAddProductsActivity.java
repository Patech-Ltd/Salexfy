package com.patechltd.salexfypos.ui.products;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.Category;
import com.patechltd.salexfypos.db.entity.PendingProduct;
import com.patechltd.salexfypos.db.entity.Product;
import com.patechltd.salexfypos.db.entity.ProductUnit;
import com.patechltd.salexfypos.db.entity.Unit;
import com.patechltd.salexfypos.model.Authority;
import com.patechltd.salexfypos.security.PermissionChecker;
import com.patechltd.salexfypos.util.AppLogger;
import com.patechltd.salexfypos.util.NumberUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Friendly, row-by-row batch entry. Each row is scanned/typed, saved to a
 * temporary pending list (survives leaving the screen), and the whole list
 * is committed to real products with one tap at the end.
 */
public class BatchAddProductsActivity extends AppCompatActivity {

    private static final int REQ_SCAN = 4401;
    private static final int REQ_PICK_CATEGORY = 4402;
    private static final int REQ_PICK_UNIT = 4403;

    private Repository repo;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private List<Category> categories = new ArrayList<>();
    private List<Unit> units = new ArrayList<>();
    private String defaultCategoryId;
    private String defaultUnitId;
    private String defaultUnitName = "Piece";

    private TextInputEditText nameInput, barcodeInput, costInput, priceInput, qtyInput;
    private TextView categoryRow, unitRow, editorTitle, rowCount, summary, emptyHint;
    private final List<PendingProduct> rows = new ArrayList<>();
    private RowAdapter adapter;
    private String editingUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch_add_products);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        repo = Repository.get(this);

        nameInput = findViewById(R.id.name);
        barcodeInput = findViewById(R.id.barcode);
        costInput = findViewById(R.id.cost);
        priceInput = findViewById(R.id.price);
        qtyInput = findViewById(R.id.qty);
        categoryRow = findViewById(R.id.category_row);
        unitRow = findViewById(R.id.unit_row);
        editorTitle = findViewById(R.id.editor_title);
        rowCount = findViewById(R.id.row_count);
        summary = findViewById(R.id.summary);
        emptyHint = findViewById(R.id.empty_hint);

        categoryRow.setOnClickListener(v -> pickCategory());
        unitRow.setOnClickListener(v -> pickUnit());
        barcodeInput.setOnClickListener(v -> openScanner());
        com.google.android.material.textfield.TextInputLayout barcodeTil = findTextInputLayout(barcodeInput);
        if (barcodeTil != null) barcodeTil.setEndIconOnClickListener(v -> openScanner());

        MaterialButton btnScan = findViewById(R.id.btn_scan);
        btnScan.setOnClickListener(v -> openScanner());

        MaterialButton btnSaveRow = findViewById(R.id.btn_save_row);
        btnSaveRow.setOnClickListener(v -> saveRow());
        if (!PermissionChecker.has(this, Authority.PRODUCT_EDIT)) {
            btnSaveRow.setEnabled(false);
            findViewById(R.id.btn_commit).setEnabled(false);
        }

        findViewById(R.id.btn_commit).setOnClickListener(v -> commit());

        RecyclerView list = findViewById(R.id.rows_list);
        adapter = new RowAdapter();
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        loadDefaults();
        loadRows();
    }

    private com.google.android.material.textfield.TextInputLayout findTextInputLayout(View child) {
        if (child instanceof com.google.android.material.textfield.TextInputLayout) {
            return (com.google.android.material.textfield.TextInputLayout) child;
        }
        if (child instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) child;
            for (int i = 0; i < group.getChildCount(); i++) {
                com.google.android.material.textfield.TextInputLayout found = findTextInputLayout(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private void loadDefaults() {
        repo.run(() -> {
            List<Category> cats = repo.directory.getCategories();
            List<Unit> us = repo.directory.getUnits();
            handler.post(() -> {
                categories.clear();
                categories.addAll(cats);
                units.clear();
                units.addAll(us);
                if (defaultCategoryId == null && !categories.isEmpty()) {
                    defaultCategoryId = categories.get(0).uid;
                }
                if (defaultUnitId == null && !units.isEmpty()) {
                    Unit piece = null;
                    for (Unit u : units) {
                        if (u.name != null && u.name.toLowerCase(java.util.Locale.ROOT)
                                .contains("piece")) {
                            piece = u;
                            break;
                        }
                    }
                    Unit chosen = piece != null ? piece : units.get(0);
                    defaultUnitId = chosen.uid;
                    defaultUnitName = chosen.name;
                }
                updateDefaultRows();
            });
        });
    }

    private void updateDefaultRows() {
        String cat = null;
        for (Category c : categories) {
            if (c.uid.equals(defaultCategoryId)) {
                cat = c.name;
                break;
            }
        }
        categoryRow.setText(cat == null ? "Category: None" : "Category: " + cat);
        unitRow.setText("Unit: " + (defaultUnitName == null ? "Piece" : defaultUnitName));
    }

    private void pickCategory() {
        ArrayList<String> names = new ArrayList<>();
        for (Category c : categories) names.add(c.name);
        names.add("None");
        Intent i = new Intent(this, PickerActivity.class);
        i.putExtra(PickerActivity.EXTRA_TITLE, "Category for these products");
        i.putExtra(PickerActivity.EXTRA_ITEMS, names);
        startActivityForResult(i, REQ_PICK_CATEGORY);
    }

    private void pickUnit() {
        ArrayList<String> names = new ArrayList<>();
        for (Unit u : units) names.add(u.name);
        Intent i = new Intent(this, PickerActivity.class);
        i.putExtra(PickerActivity.EXTRA_TITLE, "Unit for these products");
        i.putExtra(PickerActivity.EXTRA_ITEMS, names);
        startActivityForResult(i, REQ_PICK_UNIT);
    }

    private void openScanner() {
        startActivityForResult(new Intent(this,
                com.patechltd.salexfypos.ui.scan.ScanActivity.class), REQ_SCAN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        if (requestCode == REQ_SCAN) {
            String code = data.getStringExtra(com.patechltd.salexfypos.ui.scan.ScanActivity.EXTRA_CODE);
            if (code == null || code.trim().isEmpty()) return;
            barcodeInput.setText(code.trim());
            if (editingUid == null) {
                for (PendingProduct p : rows) {
                    if (code.trim().equals(p.barcode)) {
                        Toast.makeText(this, "Barcode already in the rows below", Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
            }
            if (nameInput.getText() != null && nameInput.getText().toString().trim().isEmpty()) {
                nameInput.requestFocus();
            }
            return;
        }
        if (requestCode == REQ_PICK_CATEGORY) {
            int index = data.getIntExtra(PickerActivity.EXTRA_INDEX, -1);
            if (index >= 0 && index < categories.size()) {
                defaultCategoryId = categories.get(index).uid;
            } else {
                defaultCategoryId = null;
            }
            updateDefaultRows();
        } else if (requestCode == REQ_PICK_UNIT) {
            int index = data.getIntExtra(PickerActivity.EXTRA_INDEX, -1);
            if (index >= 0 && index < units.size()) {
                defaultUnitId = units.get(index).uid;
                defaultUnitName = units.get(index).name;
                updateDefaultRows();
            }
        }
    }

    private void loadRows() {
        repo.run(() -> {
            List<PendingProduct> pending = repo.products.getPending();
            handler.post(() -> {
                rows.clear();
                rows.addAll(pending);
                refreshList();
            });
        });
    }

    private void saveRow() {
        String name = text(nameInput).trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Product name is required", Toast.LENGTH_SHORT).show();
            return;
        }
        String barcode = text(barcodeInput).trim();
        double cost = NumberUtil.parse(text(costInput), 0);
        double price = NumberUtil.parse(text(priceInput), 0);
        double qty = NumberUtil.parse(text(qtyInput), 1);
        if (qty <= 0) qty = 1;

        for (PendingProduct p : rows) {
            if (!p.uid.equals(editingUid)) {
                if (!barcode.isEmpty() && barcode.equals(p.barcode)) {
                    Toast.makeText(this, "This barcode is already in a row below", Toast.LENGTH_LONG).show();
                    return;
                }
                if (p.name != null && p.name.equalsIgnoreCase(name)) {
                    Toast.makeText(this, "A row with this name already exists", Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }

        PendingProduct pending = editingUid != null ? findRow(editingUid) : null;
        if (pending == null) {
            pending = new PendingProduct();
            pending.uid = UUID.randomUUID().toString();
            pending.createdAt = System.currentTimeMillis();
        }
        pending.name = name;
        pending.barcode = barcode.isEmpty() ? null : barcode;
        pending.cost = cost;
        pending.price = price;
        pending.qty = qty;
        pending.categoryId = defaultCategoryId;
        pending.unitId = defaultUnitId;
        pending.unitName = defaultUnitName;

        final PendingProduct saved = pending;
        final boolean wasEdit = editingUid != null;
        repo.run(() -> {
            if (!barcode.isEmpty()) {
                Product existing = repo.products.findByBarcode(barcode);
                if (existing != null) {
                    handler.post(() -> Toast.makeText(this,
                            "Barcode already belongs to \"" + existing.name + "\"",
                            Toast.LENGTH_LONG).show());
                    return;
                }
            }
            repo.products.insertPending(saved);
            handler.post(() -> {
                if (!wasEdit) {
                    rows.add(saved);
                }
                refreshList();
                resetEditor();
                Toast.makeText(this, wasEdit ? "Row updated" : "\"" + name + "\" saved",
                        Toast.LENGTH_SHORT).show();
            });
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        autoSaveIfNeeded();
    }

    /** Keeps whatever is typed in the editor so nothing is lost. */
    private void autoSaveIfNeeded() {
        String name = text(nameInput).trim();
        if (name.isEmpty()) return;
        String barcode = text(barcodeInput).trim();
        if (editingUid == null) {
            for (PendingProduct p : rows) {
                if (p.name != null && p.name.equalsIgnoreCase(name)
                        && ((p.barcode == null && barcode.isEmpty())
                        || (p.barcode != null && p.barcode.equals(barcode)))) {
                    return;
                }
            }
        }
        PendingProduct pending = editingUid != null ? findRow(editingUid) : null;
        if (pending == null) {
            pending = new PendingProduct();
            pending.uid = UUID.randomUUID().toString();
            pending.createdAt = System.currentTimeMillis();
        }
        pending.name = name;
        pending.barcode = barcode.isEmpty() ? null : barcode;
        pending.cost = NumberUtil.parse(text(costInput), 0);
        pending.price = NumberUtil.parse(text(priceInput), 0);
        pending.qty = Math.max(1, NumberUtil.parse(text(qtyInput), 1));
        pending.categoryId = defaultCategoryId;
        pending.unitId = defaultUnitId;
        pending.unitName = defaultUnitName;
        final PendingProduct saved = pending;
        final boolean wasEdit = editingUid != null;
        repo.run(() -> {
            repo.products.insertPending(saved);
            handler.post(() -> {
                if (!wasEdit) {
                    boolean exists = false;
                    for (PendingProduct p : rows) {
                        if (p.uid.equals(saved.uid)) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) rows.add(saved);
                }
                refreshList();
                resetEditor();
            });
        });
    }

    private PendingProduct findRow(String uid) {
        for (PendingProduct p : rows) {
            if (p.uid.equals(uid)) return p;
        }
        return null;
    }

    private void resetEditor() {
        editingUid = null;
        editorTitle.setText("New row");
        nameInput.getText().clear();
        barcodeInput.getText().clear();
        costInput.getText().clear();
        priceInput.getText().clear();
        qtyInput.getText().clear();
        qtyInput.setText("1");
        nameInput.requestFocus();
    }

    private void editRow(PendingProduct p) {
        editingUid = p.uid;
        editorTitle.setText("Editing \"" + p.name + "\"");
        nameInput.setText(p.name);
        barcodeInput.setText(p.barcode);
        costInput.setText(String.valueOf(p.cost));
        priceInput.setText(String.valueOf(p.price));
        qtyInput.setText(String.valueOf(p.qty));
    }

    private void deleteRow(PendingProduct p) {
        repo.run(() -> {
            repo.products.deletePending(p.uid);
            handler.post(() -> {
                rows.remove(p);
                if (p.uid.equals(editingUid)) resetEditor();
                refreshList();
            });
        });
    }

    private void refreshList() {
        adapter.submit(rows);
        rowCount.setText(rows.size() + " row" + (rows.size() == 1 ? "" : "s"));
        emptyHint.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
        MaterialButton commit = findViewById(R.id.btn_commit);
        commit.setText("Commit " + rows.size() + " product" + (rows.size() == 1 ? "" : "s")
                + " to stock");
        commit.setEnabled(!rows.isEmpty() && PermissionChecker.has(this, Authority.PRODUCT_EDIT));
    }

    private void commit() {
        if (rows.isEmpty()) return;
        findViewById(R.id.btn_commit).setEnabled(false);
        repo.run(() -> {
            int added = 0;
            int skipped = 0;
            StringBuilder skipInfo = new StringBuilder();
            Set<String> seen = new HashSet<>();
            for (PendingProduct p : new ArrayList<>(rows)) {
                String barcode = p.barcode == null ? "" : p.barcode.trim();
                if (!barcode.isEmpty()) {
                    if (seen.contains(barcode) || repo.products.findByBarcode(barcode) != null) {
                        skipped++;
                        if (skipInfo.length() > 0) skipInfo.append(", ");
                        skipInfo.append(p.name);
                        continue;
                    }
                    seen.add(barcode);
                }
                Product product = new Product();
                product.uid = UUID.randomUUID().toString();
                product.name = p.name;
                product.barcode = barcode.isEmpty() ? null : barcode;
                product.sku = null;
                product.categoryId = p.categoryId;
                product.brandId = null;
                product.retailUnit = p.unitName == null ? "Piece" : p.unitName;
                product.retailUnitId = p.unitId;
                product.wholesaleUnit = product.retailUnit;
                product.wholesaleUnitId = p.unitId;
                product.wholesaleFactor = 1;
                product.retailPrice = p.price;
                product.wholesalePrice = 0;
                product.costPrice = p.cost;
                product.reorderLevel = 0;
                product.taxPercent = 0;
                product.isActive = true;
                long now = System.currentTimeMillis();
                product.createdAt = now;
                product.updatedAt = now;
                repo.products.insert(product);

                ProductUnit base = new ProductUnit();
                base.uid = UUID.randomUUID().toString();
                base.productId = product.uid;
                base.unitId = p.unitId;
                base.unitName = product.retailUnit;
                base.factor = 1;
                base.price = p.price;
                base.isBase = true;
                base.sortOrder = 0;
                repo.products.insertProductUnit(base);

                if (p.qty > 0) {
                    repo.recordOpeningStock(product.uid, p.qty, product.retailUnit);
                }
                added++;
            }
            repo.products.clearPending();
            final int fAdded = added;
            final int fSkipped = skipped;
            final String fSkipInfo = skipInfo.toString();
            AppLogger.i("Batch committed " + added + " products");
            handler.post(() -> {
                rows.clear();
                refreshList();
                if (fAdded > 0) {
                    summary.setVisibility(View.VISIBLE);
                    summary.setText("Added " + fAdded + " product" + (fAdded == 1 ? "" : "s")
                            + (fSkipped > 0 ? " • " + fSkipped + " skipped (duplicate barcode): "
                            + fSkipInfo : ""));
                    findViewById(R.id.btn_commit).setEnabled(false);
                } else {
                    Toast.makeText(this, "Nothing new to add", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private class RowAdapter extends RecyclerView.Adapter<RowAdapter.VH> {

        private final List<PendingProduct> items = new ArrayList<>();

        void submit(List<PendingProduct> list) {
            items.clear();
            items.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_pending_row, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            final PendingProduct p = items.get(position);
            holder.name.setText(p.name);
            String sub = (p.barcode == null || p.barcode.isEmpty() ? "no barcode" : p.barcode);
            sub += " • " + NumberUtil.qty(p.qty) + " " + (p.unitName == null ? "Piece" : p.unitName);
            if (p.cost > 0) sub += " • cost " + NumberUtil.money(p.cost);
            holder.sub.setText(sub);
            holder.price.setText(NumberUtil.money(p.price));
            holder.itemView.setOnClickListener(v -> editRow(p));
            holder.delete.setOnClickListener(v -> deleteRow(p));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final TextView name, sub, price;
            final View delete;

            VH(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.pending_name);
                sub = itemView.findViewById(R.id.pending_sub);
                price = itemView.findViewById(R.id.pending_price);
                delete = itemView.findViewById(R.id.pending_delete);
            }
        }
    }
}
