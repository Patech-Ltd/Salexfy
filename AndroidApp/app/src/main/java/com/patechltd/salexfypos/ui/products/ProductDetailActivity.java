package com.patechltd.salexfypos.ui.products;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

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
import com.patechltd.salexfypos.util.DateUtil;
import com.patechltd.salexfypos.util.DialogUtil;
import com.patechltd.salexfypos.util.ImageUtil;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.Prefs;
import com.patechltd.salexfypos.util.QrUtil;

import java.util.List;

/**
 * Read-only product page: every detail at a glance, product QR code, UID,
 * barcodes, stock and prices. Editing happens on ProductEditActivity.
 */
public class ProductDetailActivity extends AppCompatActivity {

    private Repository repo;
    private String productId;
    private final java.util.Map<String, String> unitNames = new java.util.HashMap<>();
    private final java.util.Map<String, String> categoryNames = new java.util.HashMap<>();
    private final java.util.Map<String, String> brandNames = new java.util.HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        repo = Repository.get(this);
        productId = getIntent().getStringExtra("id");
        if (productId == null) {
            finish();
            return;
        }

        findViewById(R.id.btn_copy_uid).setOnClickListener(v -> copyUid());
        findViewById(R.id.btn_edit).setOnClickListener(v -> {
            Intent i = new Intent(this, ProductEditActivity.class);
            i.putExtra("id", productId);
            startActivity(i);
        });
        findViewById(R.id.btn_delete).setOnClickListener(v -> deleteProduct());
        findViewById(R.id.btn_adjust_stock).setOnClickListener(v -> adjustStock());

        boolean canEdit = PermissionChecker.has(this, Authority.PRODUCT_EDIT);
        findViewById(R.id.btn_edit).setEnabled(canEdit);
        findViewById(R.id.btn_delete).setEnabled(canEdit);
        findViewById(R.id.btn_adjust_stock).setEnabled(canEdit
                || PermissionChecker.has(this, Authority.STOCK_TAKE));

        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (repo != null && productId != null) load();
    }

    private void load() {
        repo.run(() -> {
            Product p = repo.products.getById(productId);
            if (p == null) {
                runOnUiThread(this::finish);
                return;
            }
            List<ProductUnit> units = repo.products.getUnitsByProduct(productId);
            List<ProductBarcode> barcodes = repo.products.getBarcodesByProduct(productId);
            double qty = repo.products.getCurrentQty(productId);
            for (Unit u : repo.directory.getUnits()) unitNames.put(u.uid, u.name);
            for (Category c : repo.directory.getCategories()) categoryNames.put(c.uid, c.name);
            for (Brand b : repo.directory.getBrands()) brandNames.put(b.uid, b.name);
            runOnUiThread(() -> render(p, units, barcodes, qty));
        });
    }

    private void render(Product p, List<ProductUnit> units, List<ProductBarcode> barcodes, double qty) {
        TextView name = findViewById(R.id.name);
        TextView meta = findViewById(R.id.meta);
        name.setText(p.name);

        StringBuilder metaSb = new StringBuilder();
        String cat = p.categoryId == null ? null : categoryNames.get(p.categoryId);
        String brand = p.brandId == null ? null : brandNames.get(p.brandId);
        if (brand != null && !brand.isEmpty() && !"No Brand".equals(brand)) metaSb.append(brand);
        if (cat != null && !cat.isEmpty()) {
            if (metaSb.length() > 0) metaSb.append("  •  ");
            metaSb.append(cat);
        }
        meta.setText(metaSb.length() == 0 ? "Uncategorised" : metaSb.toString());

        TextView status = findViewById(R.id.status_chip);
        status.setText(p.isActive ? "Active" : "Inactive");
        status.setBackgroundResource(p.isActive ? R.drawable.bg_success_card : R.drawable.bg_danger_card);
        status.setTextColor(getResources().getColor(p.isActive ? R.color.success : R.color.error));

        ImageView image = findViewById(R.id.image);
        image.setTag(p.imagePath);
        ImageUtil.load(image, p.imagePath, 256);

        String baseUnit = resolveUnitName(p.retailUnitId, p.retailUnit);
        TextView price = findViewById(R.id.price);
        price.setText(NumberUtil.money(p.retailPrice, Prefs.currency(this)) + " / " + baseUnit);
        TextView cost = findViewById(R.id.cost);
        cost.setText("Cost: " + NumberUtil.money(p.costPrice, Prefs.currency(this)));
        cost.setVisibility(p.costPrice > 0 ? View.VISIBLE : View.GONE);

        renderUnits(units, baseUnit);

        TextView stock = findViewById(R.id.stock);
        stock.setText(NumberUtil.qty(qty) + " " + baseUnit);
        stock.setTextColor(getResources().getColor(
                p.reorderLevel > 0 && qty <= p.reorderLevel ? R.color.error : R.color.text_primary));
        TextView reorder = findViewById(R.id.reorder);
        reorder.setText("Reorder level: " + NumberUtil.qty(p.reorderLevel) + " " + baseUnit);

        LinearLayout barcodesList = findViewById(R.id.barcodes_list);
        barcodesList.removeAllViews();
        addBarcodeRow(barcodesList, p.barcode, true);
        if (barcodes != null) {
            for (ProductBarcode pb : barcodes) {
                if (pb.barcode != null && !pb.barcode.trim().isEmpty()
                        && !pb.barcode.trim().equals(p.barcode)) {
                    addBarcodeRow(barcodesList, pb.barcode.trim(), false);
                }
            }
        }
        if (p.sku != null && !p.sku.isEmpty()) {
            TextView row = simpleRow("SKU: " + p.sku);
            barcodesList.addView(row);
        }

        TextView uidView = findViewById(R.id.uid);
        uidView.setText(p.uid);

        TextView dates = findViewById(R.id.dates);
        dates.setText("Created " + DateUtil.format(p.createdAt)
                + "  •  Updated " + DateUtil.format(p.updatedAt));

        Bitmap qr = QrUtil.qr(QrUtil.productContent(p.uid, p.barcode), 640);
        ImageView qrView = findViewById(R.id.qr);
        if (qr != null) {
            qrView.setImageBitmap(qr);
        } else {
            qrView.setVisibility(View.GONE);
            findViewById(R.id.qr_caption).setVisibility(View.GONE);
        }
        TextView caption = findViewById(R.id.qr_caption);
        caption.setText("Scan to find this product"
                + (p.barcode == null || p.barcode.isEmpty() ? " (UID code)" : " (barcode)"));
    }

    private void renderUnits(List<ProductUnit> units, String baseUnit) {
        LinearLayout container = findViewById(R.id.units_list);
        container.removeAllViews();
        if (units == null || units.isEmpty()) return;
        String currency = Prefs.currency(this);
        for (ProductUnit u : units) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(6), 0, dp(6));
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView label = new TextView(this);
            String unitName = resolveUnitName(u.unitId, u.unitName);
            label.setText(unitName + (u.isBase ? "  (base)" : ""));
            label.setTextColor(getResources().getColor(R.color.text_primary));
            label.setTextSize(14);
            label.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(label);

            TextView factor = new TextView(this);
            if (!u.isBase && u.factor > 0 && Math.abs(u.factor - 1) > 0.001) {
                String desc;
                if (u.factor < 1) {
                    desc = "1 " + unitName + " = " + NumberUtil.qty(u.factor) + " " + baseUnit;
                } else {
                    double rounded = Math.round(u.factor * 100.0) / 100.0;
                    desc = "1 " + unitName + " = " + (rounded == (long) rounded ? String.valueOf((long) rounded) : NumberUtil.qty(u.factor)) + " " + baseUnit;
                }
                factor.setText(desc + "  \u00B7  ");
                factor.setTextColor(getResources().getColor(R.color.text_secondary));
                factor.setTextSize(12);
                row.addView(factor);
            }

            TextView priceView = new TextView(this);
            priceView.setText(NumberUtil.money(u.price, currency));
            priceView.setTextColor(getResources().getColor(R.color.brand_primary_dark));
            priceView.setTextSize(15);
            priceView.setTypeface(priceView.getTypeface(), android.graphics.Typeface.BOLD);
            row.addView(priceView);

            container.addView(row);
        }
    }

    private void addBarcodeRow(LinearLayout container, String barcode, boolean primary) {
        if (barcode == null || barcode.trim().isEmpty()) return;
        TextView row = simpleRow(barcode.trim() + (primary ? "  (main)" : ""));
        container.addView(row);
    }

    private TextView simpleRow(String text) {
        TextView row = new TextView(this);
        row.setText(text);
        row.setTextColor(getResources().getColor(R.color.text_primary));
        row.setTextSize(14);
        row.setPadding(0, dp(4), 0, dp(4));
        return row;
    }

    private String resolveUnitName(String unitId, String fallback) {
        if (unitId != null && unitNames.containsKey(unitId)) return unitNames.get(unitId);
        if (fallback != null && !fallback.isEmpty()) return fallback;
        return "Pcs";
    }

    private void adjustStock() {
        repo.run(() -> {
            Product p = repo.products.getById(productId);
            if (p == null) return;
            double qty = repo.products.getCurrentQty(productId);
            runOnUiThread(() -> {
                Intent i = new Intent(this, com.patechltd.salexfypos.ui.stock.QuickStockActivity.class);
                i.putExtra(com.patechltd.salexfypos.ui.stock.QuickStockActivity.EXTRA_IDS,
                        new String[]{p.uid});
                i.putExtra(com.patechltd.salexfypos.ui.stock.QuickStockActivity.EXTRA_NAMES,
                        new String[]{p.name});
                i.putExtra(com.patechltd.salexfypos.ui.stock.QuickStockActivity.EXTRA_UNITS,
                        new String[]{resolveUnitName(p.retailUnitId, p.retailUnit)});
                i.putExtra(com.patechltd.salexfypos.ui.stock.QuickStockActivity.EXTRA_CURRENT,
                        new double[]{qty});
                i.putExtra(com.patechltd.salexfypos.ui.stock.QuickStockActivity.EXTRA_NEEDED,
                        new double[]{qty});
                i.putExtra(com.patechltd.salexfypos.ui.stock.QuickStockActivity.EXTRA_HINT,
                        "Set the stock you actually have for \"" + p.name + "\".");
                startActivity(i);
            });
        });
    }

    private void copyUid() {
        repo.run(() -> {
            Product p = repo.products.getById(productId);
            if (p == null) return;
            runOnUiThread(() -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("Product UID", p.uid));
                Toast.makeText(this, "UID copied", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void deleteProduct() {
        DialogUtil.confirm(this, "Delete product",
                "This product, its barcodes and unit prices will be removed permanently.",
                () -> repo.run(() -> {
                    Product p = repo.products.getById(productId);
                    if (p != null) {
                        repo.products.deleteBarcodesForProduct(p.uid);
                        repo.products.deleteUnitsForProduct(p.uid);
                        repo.products.delete(p);
                    }
                    runOnUiThread(this::finish);
                }));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
