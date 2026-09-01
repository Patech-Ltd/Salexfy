package com.patechltd.salexfypos.ui.products;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.patechltd.salexfypos.util.ImageUtil;

import androidx.annotation.NonNull;
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
import com.patechltd.salexfypos.db.entity.ProductUnit;
import com.patechltd.salexfypos.db.entity.Unit;
import com.patechltd.salexfypos.model.Authority;
import com.patechltd.salexfypos.security.PermissionChecker;
import com.patechltd.salexfypos.util.AppLogger;
import com.patechltd.salexfypos.util.DialogUtil;
import com.patechltd.salexfypos.util.NumberUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProductEditActivity extends AppCompatActivity {

    private static final int REQ_SCAN = 5001;
    private static final int REQ_SCAN_EXTRA = 5002;
    private static final int REQ_IMAGE = 5003;
    private static final int REQ_CAMERA_IMAGE = 5004;
    private static final int REQ_CAMERA_PERMISSION = 5005;
    private static final int REQ_PICK_CATEGORY = 5006;
    private static final int REQ_PICK_BRAND = 5007;
    private static final int REQ_PICK_BASE_UNIT = 5008;
    private static final int REQ_PICK_UNIT_ROW = 5009;

    private static class UnitRow {
        String unitId;
        String unitName;
        double factor;
        double price;
        boolean isBase;
        int sortOrder;
    }

    private Repository repo;
    private String productId;
    private boolean isNewProduct = true;
    private final List<Category> categories = new ArrayList<>();
    private final List<Brand> brands = new ArrayList<>();
    private final List<Unit> units = new ArrayList<>();
    private final List<UnitRow> unitRows = new ArrayList<>();
    private final List<String> extraBarcodes = new ArrayList<>();
    private String categoryId;
    private String brandId;
    private TextInputEditText nameInput, barcodeInput, skuInput, costInput, taxInput, notesInput,
            reorderInput, openingStockInput, retailInput;
    private TextView retailUnitRow, categoryRow, brandRow, currentStockRow;
    private SwitchMaterial activeSwitch;
    private LinearLayout unitsList;
    private LinearLayout extraBarcodesList;
    private ImageView imagePreview;
    private String imagePath;
    private java.util.concurrent.CompletableFuture<String> pendingImage;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int pickRowIndex = -1;
    private boolean syncingPrice;

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

        bind();
        loadReferences();
    }

    private void bind() {
        nameInput = findViewById(R.id.name);
        barcodeInput = findViewById(R.id.barcode);
        skuInput = findViewById(R.id.sku);
        costInput = findViewById(R.id.cost_price);
        taxInput = findViewById(R.id.tax);
        notesInput = findViewById(R.id.notes);
        reorderInput = findViewById(R.id.reorder_level);
        openingStockInput = findViewById(R.id.opening_stock);
        retailInput = findViewById(R.id.retail_price);
        retailUnitRow = findViewById(R.id.retail_unit_row);
        categoryRow = findViewById(R.id.category_row);
        brandRow = findViewById(R.id.brand_row);
        currentStockRow = findViewById(R.id.current_stock_row);
        activeSwitch = findViewById(R.id.active_switch);
        unitsList = findViewById(R.id.units_list);
        extraBarcodesList = findViewById(R.id.extra_barcodes_list);
        findViewById(R.id.btn_add_barcode).setOnClickListener(v -> addExtraBarcode());
        findViewById(R.id.btn_add_unit).setOnClickListener(v -> addUnitRow());
        imagePreview = findViewById(R.id.image_preview);
        findViewById(R.id.btn_pick_image).setOnClickListener(v -> pickImage());

        MaterialButton save = findViewById(R.id.btn_save);
        save.setOnClickListener(v -> saveProduct());
        if (!PermissionChecker.has(this, Authority.PRODUCT_EDIT)) {
            save.setEnabled(false);
        }

        setupSection(R.id.header_more, R.id.body_more, R.id.chevron_more);
        setupSection(R.id.header_units, R.id.body_units, R.id.chevron_units);

        String barcode = getIntent().getStringExtra("barcode");
        if (barcode != null) barcodeInput.setText(barcode);

        retailUnitRow.setOnClickListener(v -> pickBaseUnit());
        categoryRow.setOnClickListener(v -> pickCategory());
        brandRow.setOnClickListener(v -> pickBrand());

        retailInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            void onChanged() {
                if (syncingPrice) return;
                if (!unitRows.isEmpty()) {
                    unitRows.get(0).price = NumberUtil.parse(text(retailInput), 0);
                }
            }
        });

        barcodeInput.setOnClickListener(v -> openScanner());
        com.google.android.material.textfield.TextInputLayout barcodeTil = findTextInputLayout(barcodeInput);
        if (barcodeTil != null) barcodeTil.setEndIconOnClickListener(v -> openScanner());
    }

    private void setupSection(int headerId, int bodyId, int chevronId) {
        View header = findViewById(headerId);
        View body = findViewById(bodyId);
        ImageView chevron = findViewById(chevronId);
        body.setVisibility(View.GONE);
        chevron.setRotation(0);
        header.setOnClickListener(v -> {
            boolean visible = body.getVisibility() == View.VISIBLE;
            body.setVisibility(visible ? View.GONE : View.VISIBLE);
            chevron.animate().rotation(visible ? 0 : 90).setDuration(150).start();
        });
    }

    private void pickImage() {
        if (!PermissionChecker.has(this, Authority.PRODUCT_EDIT)) return;
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Product photo")
                .setItems(new String[]{"Take a photo", "Choose from gallery"}, (dialog, which) -> {
                    if (which == 0) captureImage();
                    else chooseFromGallery();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void chooseFromGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        try {
            startActivityForResult(Intent.createChooser(intent, "Select product image"), REQ_IMAGE);
        } catch (Exception e) {
            Toast.makeText(this, "No image picker available", Toast.LENGTH_SHORT).show();
        }
    }

    private void captureImage() {
        if (android.content.pm.PackageManager.PERMISSION_GRANTED
                != androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)) {
            androidx.core.app.ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CAMERA}, REQ_CAMERA_PERMISSION);
            return;
        }
        launchCamera();
    }

    private void launchCamera() {
        try {
            File out = new File(ImageUtil.productImageDir(this),
                    "cam_" + System.currentTimeMillis() + ".jpg");
            Uri uri = androidx.core.content.FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", out);
            Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(intent, REQ_CAMERA_IMAGE);
        } catch (Exception e) {
            Toast.makeText(this, "Camera unavailable: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openScanner() {
        startActivityForResult(new Intent(this, com.patechltd.salexfypos.ui.scan.ScanActivity.class), REQ_SCAN);
    }

    private com.google.android.material.textfield.TextInputLayout findTextInputLayout(View child) {
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
            row.setGravity(Gravity.CENTER_VERTICAL);
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
            extraBarcodesList.addView(row);
        }
    }

    // ---------- unit rows ----------

    private void addUnitRow() {
        UnitRow row = new UnitRow();
        row.isBase = unitRows.isEmpty();
        row.factor = 1;
        row.price = 0;
        row.sortOrder = unitRows.size();
        unitRows.add(row);
        renderUnitRows();
    }

    private void pickBaseUnit() {
        pickUnitForRow(0);
    }

    private void pickUnitForRow(int index) {
        pickRowIndex = index;
        ArrayList<String> names = new ArrayList<>();
        for (Unit u : units) names.add(u.name);
        Intent i = new Intent(this, PickerActivity.class);
        i.putExtra(PickerActivity.EXTRA_TITLE, "Choose unit");
        i.putExtra(PickerActivity.EXTRA_ITEMS, names);
        i.putExtra(PickerActivity.EXTRA_ALLOW_NEW, true);
        i.putExtra(PickerActivity.EXTRA_NEW_HINT, "unit (e.g. Piece)");
        startActivityForResult(i, REQ_PICK_UNIT_ROW);
    }

    private void renderUnitRows() {
        unitsList.removeAllViews();
        for (int i = 0; i < unitRows.size(); i++) {
            final int index = i;
            final UnitRow row = unitRows.get(i);

            LinearLayout container = new LinearLayout(this);
            container.setOrientation(LinearLayout.HORIZONTAL);
            container.setGravity(Gravity.CENTER_VERTICAL);
            int pad = (int) (10 * getResources().getDisplayMetrics().density);
            container.setPadding(pad, pad, pad, pad);
            container.setBackgroundResource(R.drawable.bg_search);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (i > 0) lp.topMargin = (int) (8 * getResources().getDisplayMetrics().density);
            container.setLayoutParams(lp);

            TextView name = new TextView(this);
            name.setText((row.isBase ? "Base: " : "") + (row.unitName == null ? "Pick unit" : row.unitName));
            name.setTextColor(getResources().getColor(R.color.text_primary));
            name.setTextSize(14);
            name.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f));
            name.setOnClickListener(v -> pickUnitForRow(index));
            container.addView(name);

            if (row.isBase) {
                TextView hint = new TextView(this);
                hint.setText("price above");
                hint.setTextColor(getResources().getColor(R.color.text_secondary));
                hint.setTextSize(12);
                hint.setPadding(dp(8), 0, 0, 0);
                container.addView(hint);
            } else {
                EditText priceInput = new EditText(this);
                priceInput.setHint("price");
                priceInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                        | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
                priceInput.setText(NumberUtil.qty(row.price));
                priceInput.setTextSize(13);
                priceInput.setMinEms(4);
                priceInput.setTag(row);
                priceInput.addTextChangedListener(new SimpleTextWatcher() {
                    @Override
                    public void onChanged() {
                        row.price = NumberUtil.parse(priceInput.getText().toString(), 0);
                    }
                });
                container.addView(priceInput);

                EditText factorInput = new EditText(this);
                factorInput.setHint("in 1");
                factorInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                        | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
                factorInput.setText(NumberUtil.qty(row.factor));
                factorInput.setTextSize(13);
                factorInput.setMinEms(4);
                factorInput.setTag(row);
                factorInput.addTextChangedListener(new SimpleTextWatcher() {
                    @Override
                    public void onChanged() {
                        row.factor = Math.max(1, NumberUtil.parse(factorInput.getText().toString(), 1));
                    }
                });
                container.addView(factorInput);

                TextView remove = new TextView(this);
                remove.setText("✕");
                remove.setTextColor(getResources().getColor(R.color.error));
                remove.setTextSize(16);
                remove.setPadding((int) (8 * getResources().getDisplayMetrics().density), 0, 0, 0);
                remove.setOnClickListener(v -> {
                    unitRows.remove(index);
                    for (int j = 0; j < unitRows.size(); j++) {
                        unitRows.get(j).isBase = j == 0;
                        unitRows.get(j).sortOrder = j;
                    }
                    renderUnitRows();
                });
                container.addView(remove);
            }
            unitsList.addView(container);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private abstract static class SimpleTextWatcher implements android.text.TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(android.text.Editable s) {
            onChanged();
        }

        abstract void onChanged();
    }

    // ---------- pickers ----------

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

    private void pickBrand() {
        ArrayList<String> names = new ArrayList<>();
        for (Brand b : brands) names.add(b.name);
        names.add("None");
        Intent i = new Intent(this, PickerActivity.class);
        i.putExtra(PickerActivity.EXTRA_TITLE, "Choose brand");
        i.putExtra(PickerActivity.EXTRA_ITEMS, names);
        i.putExtra(PickerActivity.EXTRA_ALLOW_NEW, true);
        i.putExtra(PickerActivity.EXTRA_NEW_HINT, "brand");
        startActivityForResult(i, REQ_PICK_BRAND);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQ_IMAGE) {
                Uri uri = data == null ? null : data.getData();
                if (uri != null) copyImageToStorage(uri);
                return;
            }
            if (requestCode == REQ_CAMERA_IMAGE) {
                if (data != null && data.getData() != null) {
                    copyImageToStorage(data.getData());
                }
                return;
            }
            if (data == null) return;
            if (requestCode == REQ_SCAN) {
                String code = data.getStringExtra(com.patechltd.salexfypos.ui.scan.ScanActivity.EXTRA_CODE);
                if (code != null) barcodeInput.setText(code.trim());
                return;
            }
            if (requestCode == REQ_SCAN_EXTRA) {
                String code = data.getStringExtra(com.patechltd.salexfypos.ui.scan.ScanActivity.EXTRA_CODE);
                if (code != null) addBarcodeToList(code.trim());
                return;
            }
            if (requestCode == REQ_PICK_CATEGORY) {
                String newName = data.getStringExtra(PickerActivity.EXTRA_NEW_NAME);
                if (newName != null) {
                    Category c = new Category();
                    c.uid = UUID.randomUUID().toString();
                    c.name = newName.trim();
                    c.sortOrder = categories.size() + 1;
                    c.createdAt = System.currentTimeMillis();
                    repo.run(() -> {
                        repo.directory.insertCategory(c);
                        handler.post(() -> {
                            categories.add(c);
                            categoryId = c.uid;
                            updateRows();
                        });
                    });
                    return;
                }
                int index = data.getIntExtra(PickerActivity.EXTRA_INDEX, -1);
                categoryId = index >= 0 && index < categories.size() ? categories.get(index).uid : null;
                updateRows();
                return;
            }
            if (requestCode == REQ_PICK_BRAND) {
                String newName = data.getStringExtra(PickerActivity.EXTRA_NEW_NAME);
                if (newName != null) {
                    Brand b = new Brand();
                    b.uid = UUID.randomUUID().toString();
                    b.name = newName.trim();
                    b.createdAt = System.currentTimeMillis();
                    repo.run(() -> {
                        repo.directory.insertBrand(b);
                        handler.post(() -> {
                            brands.add(b);
                            brandId = b.uid;
                            updateRows();
                        });
                    });
                    return;
                }
                int index = data.getIntExtra(PickerActivity.EXTRA_INDEX, -1);
                brandId = index >= 0 && index < brands.size() ? brands.get(index).uid : null;
                updateRows();
                return;
            }
            if (requestCode == REQ_PICK_UNIT_ROW || requestCode == REQ_PICK_BASE_UNIT) {
                int index = pickRowIndex;
                pickRowIndex = -1;
                if (index < 0 || index >= unitRows.size()) return;
                String newName = data.getStringExtra(PickerActivity.EXTRA_NEW_NAME);
                if (newName != null) {
                    Unit u = new Unit();
                    u.uid = UUID.randomUUID().toString();
                    u.name = newName.trim();
                    u.isWholesale = !unitRows.get(index).isBase;
                    u.createdAt = System.currentTimeMillis();
                    repo.run(() -> {
                        repo.directory.insertUnit(u);
                        handler.post(() -> {
                            units.add(u);
                            unitRows.get(index).unitId = u.uid;
                            unitRows.get(index).unitName = u.name;
                            renderUnitRows();
                            updateRows();
                        });
                    });
                    return;
                }
                int unitIndex = data.getIntExtra(PickerActivity.EXTRA_INDEX, -1);
                if (unitIndex >= 0 && unitIndex < units.size()) {
                    unitRows.get(index).unitId = units.get(unitIndex).uid;
                    unitRows.get(index).unitName = units.get(unitIndex).name;
                    renderUnitRows();
                    updateRows();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA_PERMISSION && grantResults.length > 0
                && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        }
    }

    private void copyImageToStorage(Uri uri) {
        final String stamp = UUID.randomUUID().toString();
        pendingImage = repo.io(() -> {
            String tmp = ImageUtil.copyImage(this, uri, stamp);
            if (tmp == null) {
                handler.post(() -> Toast.makeText(this, "Could not load image", Toast.LENGTH_SHORT).show());
                return null;
            }
            imagePath = tmp;
            final String path = tmp;
            handler.post(() -> {
                imagePreview.setTag(path);
                ImageUtil.load(imagePreview, path, 256);
            });
            return tmp;
        });
    }

    private void loadReferences() {
        repo.run(() -> {
            categories.clear();
            categories.addAll(repo.directory.getCategories());
            brands.clear();
            brands.addAll(repo.directory.getBrands());
            units.clear();
            units.addAll(repo.directory.getUnits());
            if (productId != null) {
                isNewProduct = false;
                Product p = repo.products.getById(productId);
                if (p == null) {
                    handler.post(this::finish);
                    return;
                }
                List<ProductBarcode> extras = repo.products.getBarcodesByProduct(productId);
                List<ProductUnit> existingUnits = repo.products.getUnitsByProduct(productId);
                double currentQty = repo.products.getCurrentQty(productId);
                handler.post(() -> {
                    findViewById(R.id.til_opening_stock).setVisibility(View.GONE);
                    fillProduct(p, existingUnits);
                    extraBarcodes.clear();
                    for (ProductBarcode pb : extras) {
                        if (pb.barcode != null && !pb.barcode.trim().isEmpty()) {
                            extraBarcodes.add(pb.barcode.trim());
                        }
                    }
                    renderBarcodes();
                    currentStockRow.setText("Current stock: " + NumberUtil.qty(currentQty));
                });
            } else {
                handler.post(() -> {
                    if (!units.isEmpty()) {
                        UnitRow base = new UnitRow();
                        base.unitId = units.get(0).uid;
                        base.unitName = units.get(0).name;
                        base.factor = 1;
                        base.price = 0;
                        base.isBase = true;
                        base.sortOrder = 0;
                        unitRows.add(base);
                    }
                    if (categories.isEmpty()) categoryId = null;
                    syncingPrice = true;
                    retailInput.setText("0");
                    syncingPrice = false;
                    renderUnitRows();
                    updateRows();
                });
            }
        });
    }

    private void fillProduct(Product p, List<ProductUnit> existingUnits) {
        nameInput.setText(p.name);
        barcodeInput.setText(p.barcode);
        skuInput.setText(p.sku);
        costInput.setText(String.valueOf(p.costPrice));
        taxInput.setText(String.valueOf(p.taxPercent));
        reorderInput.setText(String.valueOf(p.reorderLevel));
        notesInput.setText(p.notes);
        activeSwitch.setChecked(p.isActive);
        imagePath = p.imagePath;
        if (imagePath != null) {
            imagePreview.setTag(imagePath);
            ImageUtil.load(imagePreview, imagePath, 256);
        }
        categoryId = p.categoryId;
        brandId = p.brandId;

        unitRows.clear();
        if (existingUnits != null && !existingUnits.isEmpty()) {
            for (ProductUnit pu : existingUnits) {
                UnitRow row = new UnitRow();
                row.unitId = pu.unitId;
                row.unitName = resolveUnitName(pu.unitId, pu.unitName);
                row.factor = pu.factor <= 0 ? 1 : pu.factor;
                row.price = pu.price;
                row.isBase = pu.isBase;
                row.sortOrder = pu.sortOrder;
                unitRows.add(row);
            }
            boolean hasBase = false;
            for (UnitRow row : unitRows) {
                if (row.isBase) {
                    hasBase = true;
                    break;
                }
            }
            if (!hasBase && !unitRows.isEmpty()) {
                unitRows.get(0).isBase = true;
                unitRows.get(0).factor = 1;
                unitRows.get(0).sortOrder = 0;
            }
        } else {
            UnitRow base = new UnitRow();
            base.unitId = p.retailUnitId;
            base.unitName = resolveUnitName(p.retailUnitId, p.retailUnit);
            base.factor = 1;
            base.price = p.retailPrice;
            base.isBase = true;
            base.sortOrder = 0;
            unitRows.add(base);

            if (p.wholesaleUnit != null && !p.wholesaleUnit.isEmpty()
                    && (p.wholesalePrice > 0 || p.wholesaleFactor > 1
                    || !p.wholesaleUnit.equals(p.retailUnit))) {
                UnitRow bulk = new UnitRow();
                bulk.unitId = p.wholesaleUnitId;
                bulk.unitName = resolveUnitName(p.wholesaleUnitId, p.wholesaleUnit);
                bulk.factor = Math.max(1, p.wholesaleFactor);
                bulk.price = p.wholesalePrice;
                bulk.isBase = false;
                bulk.sortOrder = 1;
                unitRows.add(bulk);
            }
        }
        syncingPrice = true;
        retailInput.setText(unitRows.isEmpty() ? "" : String.valueOf(unitRows.get(0).price));
        syncingPrice = false;
        renderUnitRows();
    }

    private String resolveUnitName(String unitId, String fallback) {
        if (unitId != null) {
            for (Unit u : units) {
                if (unitId.equals(u.uid)) return u.name;
            }
        }
        if (fallback != null && !fallback.isEmpty()) return fallback;
        return units.isEmpty() ? "Pcs" : units.get(0).name;
    }

    private void updateRows() {
        UnitRow base = unitRows.isEmpty() ? null : unitRows.get(0);
        if (base != null && base.unitName != null) {
            retailUnitRow.setText("Unit: " + base.unitName);
        }
        for (Category c : categories) {
            if (c.uid.equals(categoryId)) {
                categoryRow.setText("Category: " + c.name);
                break;
            }
        }
        if (categoryId == null) categoryRow.setText("Category: Not set");
        boolean foundBrand = false;
        for (Brand b : brands) {
            if (b.uid.equals(brandId)) {
                brandRow.setText("Brand: " + b.name);
                foundBrand = true;
                break;
            }
        }
        if (!foundBrand) brandRow.setText("Brand: Not set");
    }

    private void saveProduct() {
        String name = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Product name is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (unitRows.isEmpty()) {
            Toast.makeText(this, "Choose a unit for the product", Toast.LENGTH_SHORT).show();
            return;
        }
        String barcode = barcodeInput.getText() == null ? "" : barcodeInput.getText().toString().trim();
        String sku = skuInput.getText() == null ? "" : skuInput.getText().toString().trim();
        double cost = NumberUtil.parse(text(costInput), 0);
        double tax = NumberUtil.parse(text(taxInput), 0);
        double reorder = NumberUtil.parse(text(reorderInput), 0);
        String notes = notesInput.getText() == null ? "" : notesInput.getText().toString().trim();
        double openingStock = isNewProduct ? NumberUtil.parse(text(openingStockInput), 0) : 0;

        for (UnitRow row : unitRows) {
            if (row.unitName == null || row.unitName.isEmpty()) {
                Toast.makeText(this, "Every unit needs a name - tap a unit row to pick one",
                        Toast.LENGTH_SHORT).show();
                return;
            }
        }

        UnitRow base = unitRows.get(0);
        UnitRow bulk = unitRows.size() > 1 ? unitRows.get(1) : null;
        base.price = NumberUtil.parse(text(retailInput), base.price);
        final String fRetailUnit = base.unitName;
        final String fRetailUnitId = base.unitId;
        final double fRetailPrice = base.price;
        final String fWholesaleUnit = bulk == null ? null : bulk.unitName;
        final String fWholesaleUnitId = bulk == null ? null : bulk.unitId;
        final int fWholesaleFactor = bulk == null ? 1 : (int) Math.max(1, bulk.factor);
        final double fWholesalePrice = bulk == null ? 0 : bulk.price;

        final List<UnitRow> rowsSnapshot = new ArrayList<>(unitRows);

        repo.run(() -> {
            if (pendingImage != null) {
                try {
                    String copied = pendingImage.get(10, java.util.concurrent.TimeUnit.SECONDS);
                    if (copied != null) imagePath = copied;
                } catch (Exception ignored) {
                }
            }
            if (!barcode.isEmpty()) {
                Product existing = repo.products.findByBarcode(barcode);
                if (existing != null && !existing.uid.equals(productId)) {
                    handler.post(() -> Toast.makeText(this,
                            "Another product already uses this barcode", Toast.LENGTH_LONG).show());
                    return;
                }
            }
            for (String extraCode : extraBarcodes) {
                if (!extraCode.equals(barcode)) {
                    Product existing = repo.products.findByBarcode(extraCode);
                    if (existing != null && !existing.uid.equals(productId)) {
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
                p.uid = UUID.randomUUID().toString();
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
            p.retailUnit = fRetailUnit;
            p.retailUnitId = fRetailUnitId;
            p.wholesaleUnit = fWholesaleUnit;
            p.wholesaleUnitId = fWholesaleUnitId;
            p.wholesaleFactor = fWholesaleFactor;
            p.retailPrice = fRetailPrice;
            p.wholesalePrice = fWholesalePrice;
            p.costPrice = cost;
            p.reorderLevel = reorder;
            p.taxPercent = tax;
            p.notes = notes;
            p.imagePath = imagePath;
            p.isActive = activeSwitch.isChecked();
            p.updatedAt = System.currentTimeMillis();
            if (isNew) {
                repo.products.insert(p);
            } else {
                repo.products.update(p);
            }

            repo.products.deleteUnitsForProduct(p.uid);
            int order = 0;
            for (UnitRow row : rowsSnapshot) {
                ProductUnit pu = new ProductUnit();
                pu.uid = UUID.randomUUID().toString();
                pu.productId = p.uid;
                pu.unitId = row.unitId;
                pu.unitName = row.unitName;
                pu.factor = row.isBase ? 1 : Math.max(1, row.factor);
                pu.price = row.price;
                pu.isBase = row.isBase;
                pu.sortOrder = order++;
                repo.products.insertProductUnit(pu);
            }

            repo.products.deleteBarcodesForProduct(p.uid);
            for (String extraCode : extraBarcodes) {
                if (extraCode.isEmpty() || extraCode.equals(barcode)) continue;
                ProductBarcode pb = new ProductBarcode();
                pb.uid = UUID.randomUUID().toString();
                pb.productId = p.uid;
                pb.barcode = extraCode;
                repo.products.insertBarcode(pb);
            }
            if (isNew && openingStock > 0) {
                repo.recordOpeningStock(p.uid, openingStock, fRetailUnit);
            }
            AppLogger.i("Saved product " + name);
            handler.post(this::finish);
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
}
