package com.patechltd.salexfypos.ui.products;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.Brand;
import com.patechltd.salexfypos.db.entity.Category;
import com.patechltd.salexfypos.db.entity.Product;
import com.patechltd.salexfypos.db.entity.ProductBarcode;
import com.patechltd.salexfypos.db.entity.Unit;
import com.patechltd.salexfypos.model.Authority;
import com.patechltd.salexfypos.security.PermissionChecker;
import com.patechltd.salexfypos.util.AppLogger;
import com.patechltd.salexfypos.util.DialogUtil;
import com.patechltd.salexfypos.util.NumberUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProductEditActivity extends AppCompatActivity {

    private Repository repo;
    private String productId;
    private boolean viewOnly;
    private List<Category> categories = new ArrayList<>();
    private List<Brand> brands = new ArrayList<>();
    private List<Unit> units = new ArrayList<>();
    private String categoryId, brandId, retailUnitId, wholesaleUnitId;
    private boolean isNewProduct = true;
    private final List<String> extraBarcodes = new ArrayList<>();
    private TextInputEditText nameInput, barcodeInput, skuInput, retailInput, wholesaleInput,
            costInput, reorderInput, taxInput, factorInput, notesInput, openingStockInput;
    private TextView categoryRow, brandRow, retailUnitRow, wholesaleUnitRow, currentStockRow;
    private SwitchMaterial activeSwitch;
    private LinearLayout extraBarcodesList;
    private MaterialButton btnAddBarcode;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_edit);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Product");
        }

        repo = Repository.get(this);
        productId = getIntent().getStringExtra("id");
        viewOnly = getIntent().getBooleanExtra("viewOnly", false);

        bind();
        loadReferences();
    }

    private void bind() {
        nameInput = findViewById(R.id.name);
        barcodeInput = findViewById(R.id.barcode);
        skuInput = findViewById(R.id.sku);
        retailInput = findViewById(R.id.retail_price);
        wholesaleInput = findViewById(R.id.wholesale_price);
        costInput = findViewById(R.id.cost_price);
        reorderInput = findViewById(R.id.reorder_level);
        taxInput = findViewById(R.id.tax);
        factorInput = findViewById(R.id.wholesale_factor);
        notesInput = findViewById(R.id.notes);
        openingStockInput = findViewById(R.id.opening_stock);
        categoryRow = findViewById(R.id.category_row);
        brandRow = findViewById(R.id.brand_row);
        retailUnitRow = findViewById(R.id.retail_unit_row);
        wholesaleUnitRow = findViewById(R.id.wholesale_unit_row);
        currentStockRow = findViewById(R.id.current_stock_row);
        activeSwitch = findViewById(R.id.active_switch);
        extraBarcodesList = findViewById(R.id.extra_barcodes_list);
        btnAddBarcode = findViewById(R.id.btn_add_barcode);
        btnAddBarcode.setOnClickListener(v -> addExtraBarcode());
        renderBarcodes();
        MaterialButton save = findViewById(R.id.btn_save);
        MaterialButton delete = findViewById(R.id.btn_delete);
        MaterialButton edit = findViewById(R.id.btn_edit);
        edit.setVisibility(View.GONE);
        edit.setOnClickListener(v -> {
            Intent i = new Intent(this, ProductEditActivity.class);
            i.putExtra("id", productId);
            startActivity(i);
            finish();
        });

        setupSection(R.id.header_pricing, R.id.body_pricing, R.id.chevron_pricing, false);
        setupSection(R.id.header_units, R.id.body_units, R.id.chevron_units, false);
        setupSection(R.id.header_stock, R.id.body_stock, R.id.chevron_stock, true);
        setupSection(R.id.header_notes, R.id.body_notes, R.id.chevron_notes, false);

        String barcode = getIntent().getStringExtra("barcode");
        if (barcode != null) barcodeInput.setText(barcode);

        categoryRow.setOnClickListener(v -> pickCategory());
        brandRow.setOnClickListener(v -> pickBrand());
        retailUnitRow.setOnClickListener(v -> pickRetailUnit());
        wholesaleUnitRow.setOnClickListener(v -> pickWholesaleUnit());

        barcodeInput.setOnClickListener(v -> openScanner());
        com.google.android.material.textfield.TextInputLayout barcodeTil = findTextInputLayout(barcodeInput);
        if (barcodeTil != null) barcodeTil.setEndIconOnClickListener(v -> openScanner());

        delete.setOnClickListener(v -> {
            if (productId == null) {
                finish();
                return;
            }
            DialogUtil.confirm(this, "Delete product",
                    "Delete this product permanently? This cannot be undone.", () -> {
                        repo.run(() -> {
                            Product p = repo.products.getById(productId);
                            if (p != null) repo.products.delete(p);
                            handler.post(this::finish);
                        });
                    });
        });
        delete.setVisibility(productId == null ? View.GONE : View.VISIBLE);
        if (viewOnly) enterViewMode();

        if (!PermissionChecker.has(this, Authority.PRODUCT_EDIT)) {
            save.setEnabled(false);
        }

        save.setOnClickListener(v -> saveProduct());
    }

    private static final int REQ_SCAN = 5001;
    private static final int REQ_SCAN_EXTRA = 5002;

    private void setupSection(int headerId, int bodyId, int chevronId, boolean defaultOpen) {
        View header = findViewById(headerId);
        View body = findViewById(bodyId);
        ImageView chevron = findViewById(chevronId);
        body.setVisibility(defaultOpen ? View.VISIBLE : View.GONE);
        chevron.setRotation(defaultOpen ? 90 : 0);
        header.setOnClickListener(v -> {
            boolean visible = body.getVisibility() == View.VISIBLE;
            body.setVisibility(visible ? View.GONE : View.VISIBLE);
            chevron.animate().rotation(visible ? 0 : 90).setDuration(150).start();
        });
    }

    private void enterViewMode() {
        nameInput.setEnabled(false);
        barcodeInput.setEnabled(false);
        skuInput.setEnabled(false);
        retailInput.setEnabled(false);
        wholesaleInput.setEnabled(false);
        costInput.setEnabled(false);
        reorderInput.setEnabled(false);
        taxInput.setEnabled(false);
        factorInput.setEnabled(false);
        notesInput.setEnabled(false);
        openingStockInput.setEnabled(false);
        activeSwitch.setEnabled(false);
        findViewById(R.id.btn_save).setVisibility(View.GONE);
        findViewById(R.id.btn_delete).setVisibility(View.GONE);
        findViewById(R.id.btn_edit).setVisibility(View.VISIBLE);
        btnAddBarcode.setVisibility(View.GONE);
        expandSection(R.id.body_pricing, R.id.chevron_pricing);
        expandSection(R.id.body_units, R.id.chevron_units);
        expandSection(R.id.body_stock, R.id.chevron_stock);
        expandSection(R.id.body_notes, R.id.chevron_notes);
    }

    private void expandSection(int bodyId, int chevronId) {
        findViewById(bodyId).setVisibility(View.VISIBLE);
        ((ImageView) findViewById(chevronId)).setRotation(90);
    }

    private void openScanner() {
        startActivityForResult(new Intent(this, com.patechltd.salexfypos.ui.scan.ScanActivity.class), REQ_SCAN);
    }

    private com.google.android.material.textfield.TextInputLayout findTextInputLayout(android.view.View child) {
        if (child instanceof com.google.android.material.textfield.TextInputLayout) {
            return (com.google.android.material.textfield.TextInputLayout) child;
        }
        if (child instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) child;
            for (int i = 0; i < group.getChildCount(); i++) {
                com.google.android.material.textfield.TextInputLayout found =
                        findTextInputLayout(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private void addExtraBarcode() {
        if (viewOnly) return;
        DialogUtil.pick(this, "Add another barcode", new String[]{"Scan barcode", "Type barcode"}, -1, which -> {
            if (which == 0) {
                startActivityForResult(new Intent(this, com.patechltd.salexfypos.ui.scan.ScanActivity.class),
                        REQ_SCAN_EXTRA);
            } else {
                DialogUtil.inputText(this, "Barcode", "Barcode", "", "Add",
                        value -> addBarcodeToList(value == null ? "" : value.trim()));
            }
        });
    }

    private void addBarcodeToList(String code) {
        if (code.isEmpty()) return;
        String primary = barcodeInput.getText() == null ? "" : barcodeInput.getText().toString().trim();
        if (code.equals(primary)) {
            Toast.makeText(this, "This is already the main barcode", Toast.LENGTH_SHORT).show();
            return;
        }
        for (String existing : extraBarcodes) {
            if (existing.equals(code)) {
                Toast.makeText(this, "Barcode already added", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        extraBarcodes.add(code);
        renderBarcodes();
    }

    private void renderBarcodes() {
        extraBarcodesList.removeAllViews();
        for (String code : extraBarcodes) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            int pad = (int) (14 * getResources().getDisplayMetrics().density);
            row.setPadding(pad, pad, pad, pad);
            row.setBackgroundResource(R.drawable.bg_search);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (extraBarcodes.indexOf(code) > 0) {
                params.topMargin = (int) (6 * getResources().getDisplayMetrics().density);
            }
            row.setLayoutParams(params);

            TextView label = new TextView(this);
            label.setText(code);
            label.setTextColor(0xFF0F172A);
            label.setTextSize(14);
            label.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            row.addView(label);

            if (!viewOnly) {
                TextView remove = new TextView(this);
                remove.setText("✕");
                remove.setTextColor(0xFFDC2626);
                remove.setTextSize(16);
                remove.setPadding((int) (8 * getResources().getDisplayMetrics().density), 0, 0, 0);
                remove.setOnClickListener(v -> {
                    extraBarcodes.remove(code);
                    renderBarcodes();
                });
                row.addView(remove);
            }
            extraBarcodesList.addView(row);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            String code = data.getStringExtra(com.patechltd.salexfypos.ui.scan.ScanActivity.EXTRA_CODE);
            if (code == null) return;
            if (requestCode == REQ_SCAN) {
                barcodeInput.setText(code.trim());
            } else if (requestCode == REQ_SCAN_EXTRA) {
                addBarcodeToList(code.trim());
            }
        }
    }

    private void loadReferences() {
        repo.run(() -> {
            categories = repo.directory.getCategories();
            brands = repo.directory.getBrands();
            units = repo.directory.getUnits();
            if (productId != null) {
                isNewProduct = false;
                Product p = repo.products.getById(productId);
                if (p == null) {
                    handler.post(this::finish);
                    return;
                }
                List<ProductBarcode> extras = repo.products.getBarcodesByProduct(productId);
                double currentQty = viewOnly ? repo.products.getCurrentQty(productId) : 0;
                handler.post(() -> {
                    findViewById(R.id.til_opening_stock).setVisibility(View.GONE);
                    fillProduct(p);
                    extraBarcodes.clear();
                    for (ProductBarcode pb : extras) {
                        if (pb.barcode != null && !pb.barcode.trim().isEmpty()) {
                            extraBarcodes.add(pb.barcode.trim());
                        }
                    }
                    renderBarcodes();
                    if (viewOnly) {
                        currentStockRow.setText("Current stock: " + NumberUtil.qty(currentQty));
                    }
                });
            } else {
                handler.post(() -> {
                    categoryId = categories.isEmpty() ? null : categories.get(0).id;
                    retailUnitId = units.isEmpty() ? null : units.get(0).id;
                    factorInput.setText("1");
                });
            }
            handler.post(this::updateRows);
        });
    }

    private void fillProduct(Product p) {
        nameInput.setText(p.name);
        barcodeInput.setText(p.barcode);
        skuInput.setText(p.sku);
        retailInput.setText(String.valueOf(p.retailPrice));
        wholesaleInput.setText(String.valueOf(p.wholesalePrice));
        costInput.setText(String.valueOf(p.costPrice));
        reorderInput.setText(String.valueOf(p.reorderLevel));
        taxInput.setText(String.valueOf(p.taxPercent));
        factorInput.setText(String.valueOf(p.wholesaleFactor));
        notesInput.setText(p.notes);
        activeSwitch.setChecked(p.isActive);
        categoryId = p.categoryId;
        brandId = p.brandId;
        retailUnitId = unitIdByName(p.retailUnit);
        wholesaleUnitId = unitIdByName(p.wholesaleUnit);
    }

    private String unitIdByName(String name) {
        if (name == null) return null;
        for (Unit u : units) {
            if (name.equals(u.name)) return u.id;
        }
        return null;
    }

    private void pickCategory() {
        if (viewOnly) return;
        List<String> names = new ArrayList<>();
        for (Category c : categories) names.add(c.name);
        names.add("+ New category");
        DialogUtil.pick(this, "Category", names.toArray(new String[0]), -1, which -> {
            if (which < categories.size()) {
                categoryId = categories.get(which).id;
                updateRows();
            } else {
                DialogUtil.inputText(this, "New category", "Category name", "", "Add", value -> {
                    if (value.trim().isEmpty()) return;
                    Category c = new Category();
                    c.id = UUID.randomUUID().toString();
                    c.name = value.trim();
                    c.sortOrder = categories.size() + 1;
                    c.createdAt = System.currentTimeMillis();
                    repo.run(() -> {
                        repo.directory.insertCategory(c);
                        handler.post(() -> loadReferences());
                    });
                });
            }
        });
    }

    private void pickBrand() {
        if (viewOnly) return;
        List<String> names = new ArrayList<>();
        for (Brand b : brands) names.add(b.name);
        names.add("+ New brand");
        DialogUtil.pick(this, "Brand", names.toArray(new String[0]), -1, which -> {
            if (which < brands.size()) {
                brandId = brands.get(which).id;
                updateRows();
            } else {
                DialogUtil.inputText(this, "New brand", "Brand name", "", "Add", value -> {
                    if (value.trim().isEmpty()) return;
                    Brand b = new Brand();
                    b.id = UUID.randomUUID().toString();
                    b.name = value.trim();
                    b.createdAt = System.currentTimeMillis();
                    repo.run(() -> {
                        repo.directory.insertBrand(b);
                        handler.post(() -> loadReferences());
                    });
                });
            }
        });
    }

    private void pickRetailUnit() {
        if (viewOnly) return;
        List<String> names = new ArrayList<>();
        for (Unit u : units) names.add(u.name + (u.isWholesale ? " (wholesale)" : ""));
        names.add("+ New unit");
        DialogUtil.pick(this, "Retail unit", names.toArray(new String[0]), -1, which -> {
            if (which < units.size()) {
                retailUnitId = units.get(which).id;
                updateRows();
            } else {
                DialogUtil.inputText(this, "New unit", "Unit name (e.g. Piece)", "", "Add", value -> {
                    if (value.trim().isEmpty()) return;
                    Unit u = new Unit();
                    u.id = UUID.randomUUID().toString();
                    u.name = value.trim();
                    u.isWholesale = false;
                    u.createdAt = System.currentTimeMillis();
                    repo.run(() -> {
                        repo.directory.insertUnit(u);
                        handler.post(() -> loadReferences());
                    });
                });
            }
        });
    }

    private void pickWholesaleUnit() {
        if (viewOnly) return;
        List<String> names = new ArrayList<>();
        for (Unit u : units) names.add(u.name);
        names.add("None");
        DialogUtil.pick(this, "Wholesale unit", names.toArray(new String[0]), -1, which -> {
            if (which < units.size()) {
                wholesaleUnitId = units.get(which).id;
            } else {
                wholesaleUnitId = null;
            }
            updateRows();
        });
    }

    private void updateRows() {
        for (Category c : categories) {
            if (c.id.equals(categoryId)) {
                categoryRow.setText("Category: " + c.name);
                break;
            }
        }
        for (Brand b : brands) {
            if (b.id.equals(brandId)) {
                brandRow.setText("Brand: " + b.name);
                break;
            }
        }
        for (Unit u : units) {
            if (u.id.equals(retailUnitId)) {
                retailUnitRow.setText("Retail: " + u.name);
            }
            if (u.id.equals(wholesaleUnitId)) {
                wholesaleUnitRow.setText("Wholesale: " + u.name);
            }
        }
        if (wholesaleUnitId == null) wholesaleUnitRow.setText("Wholesale: none");
    }

    private void saveProduct() {
        String name = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Product name is required", Toast.LENGTH_SHORT).show();
            return;
        }
        String barcode = barcodeInput.getText() == null ? "" : barcodeInput.getText().toString().trim();
        String sku = skuInput.getText() == null ? "" : skuInput.getText().toString().trim();
        double retail = NumberUtil.parse(edit(retailInput), 0);
        double wholesale = NumberUtil.parse(edit(wholesaleInput), retail);
        double cost = NumberUtil.parse(edit(costInput), 0);
        double reorder = NumberUtil.parse(edit(reorderInput), 0);
        double tax = NumberUtil.parse(edit(taxInput), 0);
        int factor = (int) NumberUtil.parse(edit(factorInput), 1);
        if (factor < 1) factor = 1;
        final int fFactor = factor;
        double openingStock = isNewProduct
                ? NumberUtil.parse(edit(openingStockInput), 0) : 0;
        String notes = notesInput.getText() == null ? "" : notesInput.getText().toString().trim();

        String retailUnitName = unitName(retailUnitId);
        String wholesaleUnitName = unitName(wholesaleUnitId);

        repo.run(() -> {
            if (barcode != null && !barcode.isEmpty()) {
                Product existing = repo.products.findByBarcode(barcode);
                if (existing != null && !existing.id.equals(productId)) {
                    handler.post(() -> Toast.makeText(this,
                            "Another product already uses this barcode", Toast.LENGTH_LONG).show());
                    return;
                }
            }
            for (String extraCode : extraBarcodes) {
                if (!extraCode.equals(barcode)) {
                    Product existing = repo.products.findByBarcode(extraCode);
                    if (existing != null && !existing.id.equals(productId)) {
                        handler.post(() -> Toast.makeText(this,
                                "Barcode " + extraCode + " is used by another product", Toast.LENGTH_LONG).show());
                        return;
                    }
                }
            }
            Product p;
            boolean isNew = productId == null;
            if (isNew) {
                p = new Product();
                p.id = UUID.randomUUID().toString();
                p.createdAt = System.currentTimeMillis();
            } else {
                p = repo.products.getById(productId);
                if (p == null) {
                    handler.post(this::finish);
                    return;
                }
            }
            p.name = name;
            p.barcode = barcode.isEmpty() ? null : barcode;
            p.sku = sku.isEmpty() ? null : sku;
            p.categoryId = categoryId;
            p.brandId = brandId;
            p.retailUnit = retailUnitName;
            p.wholesaleUnit = wholesaleUnitName;
            p.wholesaleFactor = fFactor;
            p.retailPrice = retail;
            p.wholesalePrice = wholesale;
            p.costPrice = cost;
            p.reorderLevel = reorder;
            p.taxPercent = tax;
            p.notes = notes;
            p.isActive = activeSwitch.isChecked();
            p.updatedAt = System.currentTimeMillis();
            if (isNew) {
                repo.products.insert(p);
                if (openingStock > 0) {
                    repo.recordOpeningStock(p.id, openingStock, retailUnitName);
                }
            } else {
                repo.products.update(p);
            }
            repo.products.deleteBarcodesForProduct(p.id);
            for (String extraCode : extraBarcodes) {
                if (extraCode.isEmpty() || extraCode.equals(barcode)) continue;
                ProductBarcode pb = new ProductBarcode();
                pb.id = UUID.randomUUID().toString();
                pb.productId = p.id;
                pb.barcode = extraCode;
                repo.products.insertBarcode(pb);
            }
            AppLogger.i("Saved product " + name);
            handler.post(this::finish);
        });
    }

    private String edit(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    private String unitName(String id) {
        for (Unit u : units) {
            if (u.id.equals(id)) return u.name;
        }
        return "Pcs";
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
