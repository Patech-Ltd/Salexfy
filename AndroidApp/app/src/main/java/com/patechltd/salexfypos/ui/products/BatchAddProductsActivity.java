package com.patechltd.salexfypos.ui.products;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.Category;
import com.patechltd.salexfypos.db.entity.Product;
import com.patechltd.salexfypos.db.entity.Unit;
import com.patechltd.salexfypos.model.Authority;
import com.patechltd.salexfypos.security.PermissionChecker;
import com.patechltd.salexfypos.util.AppLogger;
import com.patechltd.salexfypos.util.DialogUtil;
import com.patechltd.salexfypos.util.NumberUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BatchAddProductsActivity extends AppCompatActivity {

    private Repository repo;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextInputEditText batchInput;
    private TextView summary, categoryRow, unitRow;
    private List<Category> categories = new ArrayList<>();
    private List<Unit> units = new ArrayList<>();
    private String defaultCategoryId;
    private String defaultUnitName = "Pcs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch_add_products);

        repo = Repository.get(this);

        findViewById(R.id.toolbar).setOnClickListener(v -> onBackPressed());

        batchInput = findViewById(R.id.batch_input);
        summary = findViewById(R.id.summary);
        categoryRow = findViewById(R.id.category_row);
        unitRow = findViewById(R.id.unit_row);

        categoryRow.setOnClickListener(v -> pickCategory());
        unitRow.setOnClickListener(v -> pickUnit());

        findViewById(R.id.btn_add).setOnClickListener(v -> addProducts());

        loadDefaults();
    }

    private void loadDefaults() {
        repo.run(() -> {
            categories = repo.directory.getCategories();
            units = repo.directory.getUnits();
            handler.post(() -> {
                if (!categories.isEmpty()) {
                    defaultCategoryId = categories.get(0).id;
                    categoryRow.setText("Default category: " + categories.get(0).name);
                }
                if (!units.isEmpty()) {
                    defaultUnitName = units.get(0).name;
                    unitRow.setText("Retail unit: " + units.get(0).name);
                }
            });
        });
    }

    private void pickCategory() {
        List<String> names = new ArrayList<>();
        for (Category c : categories) names.add(c.name);
        names.add("None");
        DialogUtil.pick(this, "Default category", names.toArray(new String[0]), -1, which -> {
            if (which < categories.size()) {
                defaultCategoryId = categories.get(which).id;
                categoryRow.setText("Default category: " + categories.get(which).name);
            } else {
                defaultCategoryId = null;
                categoryRow.setText("Default category: None");
            }
        });
    }

    private void pickUnit() {
        List<String> names = new ArrayList<>();
        for (Unit u : units) names.add(u.name);
        DialogUtil.pick(this, "Retail unit", names.toArray(new String[0]), -1, which -> {
            if (which < units.size()) {
                defaultUnitName = units.get(which).name;
                unitRow.setText("Retail unit: " + units.get(which).name);
            }
        });
    }

    private void addProducts() {
        if (!PermissionChecker.has(this, Authority.PRODUCT_EDIT)) {
            Toast.makeText(this, "You do not have permission to add products", Toast.LENGTH_LONG).show();
            return;
        }
        String text = batchInput.getText() == null ? "" : batchInput.getText().toString();
        List<BatchLine> lines = parse(text);
        int valid = 0;
        for (BatchLine l : lines) if (l.name != null) valid++;
        if (valid == 0) {
            Toast.makeText(this, "No valid lines found. Check the format.", Toast.LENGTH_LONG).show();
            return;
        }
        final List<BatchLine> toInsert = lines;
        findViewById(R.id.btn_add).setEnabled(false);
        repo.run(() -> {
            int added = 0;
            int skipped = 0;
            Set<String> seenBarcodes = new HashSet<>();
            for (BatchLine l : toInsert) {
                if (l.name == null) {
                    skipped++;
                    continue;
                }
                if (l.barcode != null) {
                    String code = l.barcode.trim();
                    if (!code.isEmpty()) {
                        if (seenBarcodes.contains(code)) {
                            skipped++;
                            continue;
                        }
                        if (repo.products.findByBarcode(code) != null) {
                            skipped++;
                            continue;
                        }
                        seenBarcodes.add(code);
                    }
                }
                Product p = new Product();
                p.id = UUID.randomUUID().toString();
                p.name = l.name;
                p.barcode = l.barcode == null || l.barcode.trim().isEmpty() ? null : l.barcode.trim();
                p.sku = null;
                p.categoryId = defaultCategoryId;
                p.brandId = null;
                p.retailUnit = defaultUnitName;
                p.wholesaleUnit = defaultUnitName;
                p.wholesaleFactor = 1;
                p.retailPrice = l.retail;
                p.wholesalePrice = 0;
                p.costPrice = l.cost;
                p.reorderLevel = 0;
                p.taxPercent = 0;
                p.notes = null;
                p.isActive = true;
                long now = System.currentTimeMillis();
                p.createdAt = now;
                p.updatedAt = now;
                repo.products.insert(p);
                if (l.qty > 0) {
                    repo.recordOpeningStock(p.id, l.qty, defaultUnitName);
                }
                added++;
            }
            final int fAdded = added;
            final int fSkipped = skipped;
            AppLogger.i("Batch added " + added + " products");
            handler.post(() -> {
                findViewById(R.id.btn_add).setEnabled(true);
                summary.setVisibility(View.VISIBLE);
                summary.setText("Added " + fAdded + " products"
                        + (fSkipped > 0 ? " (" + fSkipped + " skipped: missing name or duplicate barcode)" : "")
                        + ".\nYou can review them from the Products list.");
                batchInput.getText().clear();
            });
        });
    }

    private static List<BatchLine> parse(String text) {
        List<BatchLine> result = new ArrayList<>();
        String[] lines = text.split("\n");
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                result.add(new BatchLine(null, null, 0, 0, 0));
                continue;
            }
            String sep = null;
            for (String s : new String[]{"\t", ",", ";", "|"}) {
                if (line.contains(s)) {
                    sep = s;
                    break;
                }
            }
            String[] parts = sep == null ? new String[]{line} : line.split(java.util.regex.Pattern.quote(sep));
            String name = parts.length > 0 ? parts[0].trim() : "";
            String barcode = parts.length > 1 ? parts[1].trim() : "";
            double cost = parts.length > 2 ? NumberUtil.parse(parts[2].trim(), 0) : 0;
            double retail = parts.length > 3 ? NumberUtil.parse(parts[3].trim(), cost) : cost;
            double qty = parts.length > 4 ? Math.max(0, NumberUtil.parse(parts[4].trim(), 0)) : 0;
            result.add(new BatchLine(name.isEmpty() ? null : name,
                    barcode.isEmpty() ? null : barcode, cost, retail, qty));
        }
        return result;
    }

    private static class BatchLine {
        final String name;
        final String barcode;
        final double cost;
        final double retail;
        final double qty;

        BatchLine(String name, String barcode, double cost, double retail, double qty) {
            this.name = name;
            this.barcode = barcode;
            this.cost = cost;
            this.retail = retail;
            this.qty = qty;
        }
    }
}
