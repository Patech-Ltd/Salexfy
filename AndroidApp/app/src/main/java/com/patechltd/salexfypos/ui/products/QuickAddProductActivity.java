package com.patechltd.salexfypos.ui.products;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.Category;
import com.patechltd.salexfypos.db.entity.Product;
import com.patechltd.salexfypos.db.entity.ProductUnit;
import com.patechltd.salexfypos.db.entity.Unit;
import com.patechltd.salexfypos.model.Authority;
import com.patechltd.salexfypos.security.PermissionChecker;
import com.patechltd.salexfypos.util.AppLogger;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.Prefs;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Relaxed quick-add: name, price, unit, category, barcode. Nothing else.
 * Stays open so a busy cashier can add many products back-to-back.
 */
public class QuickAddProductActivity extends AppCompatActivity {

    private static final int REQ_SCAN = 5101;
    private static final int REQ_PICK_UNIT = 5102;
    private static final int REQ_PICK_CATEGORY = 5103;

    private Repository repo;
    private TextInputEditText nameInput, priceInput, buyingPriceInput, barcodeInput;
    private TextView unitRow, categoryRow, feedback;
    private final List<Unit> units = new ArrayList<>();
    private final List<Category> categories = new ArrayList<>();
    private String unitId;
    private String unitName;
    private String categoryId;
    private boolean saving;
    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_add_product);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        repo = Repository.get(this);
        nameInput = findViewById(R.id.name);
        priceInput = findViewById(R.id.price);
        buyingPriceInput = findViewById(R.id.buying_price);
        barcodeInput = findViewById(R.id.barcode);
        unitRow = findViewById(R.id.unit_row);
        categoryRow = findViewById(R.id.category_row);
        feedback = findViewById(R.id.feedback);

        String barcode = getIntent().getStringExtra("barcode");
        if (barcode != null && !barcode.isEmpty()) barcodeInput.setText(barcode);

        unitRow.setOnClickListener(v -> pickUnit());
        categoryRow.setOnClickListener(v -> pickCategory());
        barcodeInput.setOnClickListener(v -> openScanner());
        com.google.android.material.textfield.TextInputLayout barcodeTil = findTextInputLayout(barcodeInput);
        if (barcodeTil != null) barcodeTil.setEndIconOnClickListener(v -> openScanner());

        MaterialButton saveMore = findViewById(R.id.btn_save_more);
        MaterialButton saveClose = findViewById(R.id.btn_save_close);
        saveMore.setOnClickListener(v -> save(false));
        saveClose.setOnClickListener(v -> save(true));

        if (!PermissionChecker.has(this, Authority.PRODUCT_EDIT)) {
            saveMore.setEnabled(false);
            saveClose.setEnabled(false);
        }

        loadReferences();
    }

    private com.google.android.material.textfield.TextInputLayout findTextInputLayout(View child) {
        if (child instanceof com.google.android.material.textfield.TextInputLayout) {
            return (com.google.android.material.textfield.TextInputLayout) child;
        }
        if (child instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) child;
            for (int i = 0; i < group.getChildCount(); i++) {
                com.google.android.material.textfield.TextInputLayout found = findTextInputLayout(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private static Unit preferPiece(List<Unit> list) {
        Unit first = null;
        for (Unit u : list) {
            if (first == null) first = u;
            String n = u.name == null ? "" : u.name.trim().toLowerCase(java.util.Locale.ROOT);
            if (n.equals("pc") || n.equals("pcs") || n.equals("piece") || n.contains("piece")) {
                return u;
            }
        }
        return first;
    }

    private void loadReferences() {
        repo.run(() -> {
            List<Unit> us = repo.directory.getUnits();
            List<Category> cs = repo.directory.getCategories();
            runOnUiThread(() -> {
                units.clear();
                units.addAll(us);
                categories.clear();
                categories.addAll(cs);
                if (unitId == null && !units.isEmpty()) {
                    Unit preferred = preferPiece(units);
                    unitId = preferred.uid;
                    unitName = preferred.name;
                }
                if (categoryId == null && !categories.isEmpty()) {
                    categoryId = categories.get(0).uid;
                }
                updateRows();
            });
        });
    }

    private void updateRows() {
        String unit = unitName == null ? "Piece" : unitName;
        unitRow.setText("Unit: " + unit);
        String cat = null;
        for (Category c : categories) {
            if (c.uid.equals(categoryId)) {
                cat = c.name;
                break;
            }
        }
        categoryRow.setText(cat == null ? "Category: Not set" : "Category: " + cat);
    }

    private void openScanner() {
        startActivityForResult(new Intent(this, com.patechltd.salexfypos.ui.scan.ScanActivity.class), REQ_SCAN);
    }

    private void pickUnit() {
        ArrayList<String> names = new ArrayList<>();
        for (Unit u : units) names.add(u.name);
        Intent i = new Intent(this, PickerActivity.class);
        i.putExtra(PickerActivity.EXTRA_TITLE, "Choose unit");
        i.putExtra(PickerActivity.EXTRA_ITEMS, names);
        i.putExtra(PickerActivity.EXTRA_ALLOW_NEW, true);
        i.putExtra(PickerActivity.EXTRA_NEW_HINT, "unit (e.g. Piece)");
        startActivityForResult(i, REQ_PICK_UNIT);
    }

    private void pickCategory() {
        ArrayList<String> names = new ArrayList<>();
        for (Category c : categories) names.add(c.name);
        names.add("None");
        Intent i = new Intent(this, PickerActivity.class);
        i.putExtra(PickerActivity.EXTRA_TITLE, "Choose category");
        i.putExtra(PickerActivity.EXTRA_ITEMS, names);
        i.putExtra(PickerActivity.EXTRA_ALLOW_NEW, true);
        i.putExtra(PickerActivity.EXTRA_NEW_HINT, "category");
        startActivityForResult(i, REQ_PICK_CATEGORY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        if (requestCode == REQ_SCAN) {
            String code = data.getStringExtra(com.patechltd.salexfypos.ui.scan.ScanActivity.EXTRA_CODE);
            if (code != null) barcodeInput.setText(code.trim());
            return;
        }
        if (requestCode == REQ_PICK_UNIT) {
            String newName = data.getStringExtra(PickerActivity.EXTRA_NEW_NAME);
            if (newName != null) {
                final Unit u = new Unit();
                u.uid = UUID.randomUUID().toString();
                u.name = newName.trim();
                u.isWholesale = false;
                u.createdAt = System.currentTimeMillis();
                repo.run(() -> {
                    repo.directory.insertUnit(u);
                    runOnUiThread(() -> {
                        unitId = u.uid;
                        unitName = u.name;
                        units.add(u);
                        updateRows();
                    });
                });
                return;
            }
            int index = data.getIntExtra(PickerActivity.EXTRA_INDEX, -1);
            if (index >= 0 && index < units.size()) {
                unitId = units.get(index).uid;
                unitName = units.get(index).name;
                updateRows();
            }
            return;
        }
        if (requestCode == REQ_PICK_CATEGORY) {
            String newName = data.getStringExtra(PickerActivity.EXTRA_NEW_NAME);
            if (newName != null) {
                final Category c = new Category();
                c.uid = UUID.randomUUID().toString();
                c.name = newName.trim();
                c.sortOrder = categories.size() + 1;
                c.createdAt = System.currentTimeMillis();
                repo.run(() -> {
                    repo.directory.insertCategory(c);
                    runOnUiThread(() -> {
                        categoryId = c.uid;
                        categories.add(c);
                        updateRows();
                    });
                });
                return;
            }
            int index = data.getIntExtra(PickerActivity.EXTRA_INDEX, -1);
            if (index >= categories.size()) {
                categoryId = null;
            } else if (index >= 0) {
                categoryId = categories.get(index).uid;
            }
            updateRows();
        }
    }

    private void save(boolean closeAfter) {
        if (saving) return;
        String name = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Product name is required", Toast.LENGTH_SHORT).show();
            return;
        }
        double price = NumberUtil.parse(priceInput.getText() == null ? ""
                : priceInput.getText().toString(), 0);
        double buyingPrice = NumberUtil.parse(buyingPriceInput.getText() == null ? ""
                : buyingPriceInput.getText().toString(), 0);
        if (price <= 0) {
            Toast.makeText(this, "Selling price is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (buyingPrice <= 0) {
            Toast.makeText(this, "Buying price is required", Toast.LENGTH_SHORT).show();
            return;
        }
        String barcode = barcodeInput.getText() == null ? "" : barcodeInput.getText().toString().trim();
        final String fUnitId = unitId;
        final String fUnitName = unitName == null ? "Pcs" : unitName;
        final String fCategory = categoryId;

        saving = true;
        repo.run(() -> {
            try {
                if (!barcode.isEmpty() && repo.products.findByBarcode(barcode) != null) {
                    runOnUiThread(() -> {
                        saving = false;
                        Toast.makeText(this, "Another product already uses this barcode",
                                Toast.LENGTH_LONG).show();
                    });
                    return;
                }
                Product p = new Product();
                p.uid = UUID.randomUUID().toString();
                p.name = name;
                p.barcode = barcode.isEmpty() ? null : barcode;
                p.sku = null;
                p.categoryId = fCategory;
                p.brandId = null;
                p.retailUnit = fUnitName;
                p.retailUnitId = fUnitId;
                p.wholesaleUnit = fUnitName;
                p.wholesaleUnitId = fUnitId;
                p.wholesaleFactor = 1;
                p.retailPrice = price;
                p.wholesalePrice = 0;
                p.costPrice = buyingPrice;
                p.reorderLevel = 0;
                p.taxPercent = 0;
                p.isActive = true;
                long now = System.currentTimeMillis();
                p.createdAt = now;
                p.updatedAt = now;
                repo.products.insert(p);
                ProductUnit base = new ProductUnit();
                base.uid = UUID.randomUUID().toString();
                base.productId = p.uid;
                base.unitId = fUnitId;
                base.unitName = fUnitName;
                base.factor = 1;
                base.price = price;
                base.isBase = true;
                base.sortOrder = 0;
                repo.products.insertProductUnit(base);
                AppLogger.i("Quick added product " + name);
                runOnUiThread(() -> {
                    saving = false;
                    feedback.setVisibility(View.VISIBLE);
                    feedback.setText("Saved \"" + name + "\" • " + NumberUtil.money(price, Prefs.currency(this)));
                    if (closeAfter) {
                        finish();
                    } else {
                        nameInput.getText().clear();
                        priceInput.getText().clear();
                        buyingPriceInput.getText().clear();
                        barcodeInput.getText().clear();
                        nameInput.requestFocus();
                        handler.postDelayed(() -> feedback.setVisibility(View.GONE), 2500);
                    }
                });
            } catch (Exception e) {
                AppLogger.e("Quick add failed", e);
                runOnUiThread(() -> {
                    saving = false;
                    Toast.makeText(this, "Could not save product", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
