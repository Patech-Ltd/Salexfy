package com.patechltd.salexfypos.ui.stock;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.adapter.PurchaseLineAdapter;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.Product;
import com.patechltd.salexfypos.db.entity.ProductBarcode;
import com.patechltd.salexfypos.db.entity.Purchase;
import com.patechltd.salexfypos.db.entity.PurchaseItem;
import com.patechltd.salexfypos.db.entity.Supplier;
import com.patechltd.salexfypos.scanner.ScannerView;
import com.patechltd.salexfypos.security.Session;
import com.patechltd.salexfypos.util.AppLogger;
import com.patechltd.salexfypos.util.DateUtil;
import com.patechltd.salexfypos.util.DialogUtil;
import com.patechltd.salexfypos.util.NumberUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PurchaseEditActivity extends AppCompatActivity {

    private Repository repo;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<PurchaseItem> items = new ArrayList<>();
    private final java.util.Map<String, Product> byBarcode = new java.util.HashMap<>();
    private final java.util.Map<String, Product> byId = new java.util.HashMap<>();
    private String supplierId;
    private long purchaseDate = System.currentTimeMillis();
    private ScannerView scanner;
    private FrameLayout scannerContainer;
    private PurchaseLineAdapter adapter;
    private TextView supplierRow, subtotalView, totalView;
    private TextInputEditText invoiceNo, paid, notes;
    private boolean editingExisting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase_edit);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        setSupportActionBar(toolbar);

        repo = Repository.get(this);
        String editId = getIntent().getStringExtra("id");
        editingExisting = editId != null;

        supplierRow = findViewById(R.id.supplier_row);
        invoiceNo = findViewById(R.id.invoice_no);
        paid = findViewById(R.id.paid);
        notes = findViewById(R.id.notes);
        subtotalView = findViewById(R.id.subtotal);
        totalView = findViewById(R.id.total);
        TextInputEditText dateField = findViewById(R.id.date_field);
        MaterialButton save = findViewById(R.id.btn_save);
        MaterialButton delete = findViewById(R.id.btn_delete);
        MaterialButton toggleScanner = findViewById(R.id.btn_toggle_scanner);
        MaterialButton batchAdd = findViewById(R.id.btn_batch_add);
        scannerContainer = findViewById(R.id.scanner_container);
        scanner = findViewById(R.id.scanner);
        RecyclerView list = findViewById(R.id.items_list);

        adapter = new PurchaseLineAdapter(new PurchaseLineAdapter.Listener() {
            @Override
            public void onClick(int position) {
                editLine(position);
            }

            @Override
            public void onRemove(int position) {
                items.remove(position);
                afterChange();
            }
        });
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        if (editingExisting) {
            toolbar.setTitle("Purchase Details");
            loadExisting(editId);
            save.setVisibility(View.GONE);
            toggleScanner.setVisibility(View.GONE);
            dateField.setText(DateUtil.formatDate(purchaseDate));
        } else {
            repo.run(() -> {
                String no = repo.nextInvoiceNo();
                handler.post(() -> invoiceNo.setText(no));
            });
            dateField.setText(DateUtil.formatDate(purchaseDate));
            paid.setText("0");
            toggleScanner.setOnClickListener(v -> {
                boolean show = scannerContainer.getVisibility() == View.VISIBLE;
                scannerContainer.setVisibility(show ? View.GONE : View.VISIBLE);
                if (!show) ensurePermissionAndStart();
            });
            batchAdd.setOnClickListener(v -> openBatchAdd());
        }

        dateField.setOnClickListener(v -> {
            if (editingExisting) return;
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Purchase date")
                    .setSelection(purchaseDate)
                    .build();
            picker.addOnPositiveButtonClickListener(sel -> {
                purchaseDate = sel;
                dateField.setText(DateUtil.formatDate(sel));
            });
            picker.show(getSupportFragmentManager(), "date");
        });

        supplierRow.setOnClickListener(v -> pickSupplier());

        save.setOnClickListener(v -> savePurchase());
        delete.setOnClickListener(v -> confirmDelete());

        loadProducts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (scannerContainer != null && scannerContainer.getVisibility() == View.VISIBLE) {
            ensurePermissionAndStart();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanner.stop();
    }

    private void ensurePermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1002);
            return;
        }
        scanner.setOnScanListener((text, format) -> handleScan(text));
        scanner.start(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1002 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            scanner.setOnScanListener((text, format) -> handleScan(text));
            scanner.start(this);
        }
    }

    private void loadProducts() {
        repo.run(() -> {
            List<Product> all = repo.products.getAllActive();
            List<ProductBarcode> extra = repo.products.getAllBarcodes();
            for (Product p : all) {
                byId.put(p.uid, p);
                if (p.barcode != null && !p.barcode.isEmpty()) {
                    byBarcode.put(p.barcode.trim(), p);
                }
            }
            for (ProductBarcode pb : extra) {
                Product p = pb.barcode == null ? null : byId.get(pb.productId);
                if (p != null && !pb.barcode.trim().isEmpty()) {
                    byBarcode.put(pb.barcode.trim(), p);
                }
            }
        });
    }

    private void openBatchAdd() {
        com.google.android.material.textfield.TextInputEditText input = new com.google.android.material.textfield.TextInputEditText(this);
        input.setHint("One barcode per line. Optional qty after a comma or space, e.g.\n8901234567890, 5");
        input.setGravity(android.view.Gravity.TOP);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);
        input.setMinHeight((int) (180 * getResources().getDisplayMetrics().density));

        new MaterialAlertDialogBuilder(this)
                .setTitle("Batch add products")
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    String text = input.getText() == null ? "" : input.getText().toString();
                    parseBatch(text);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void parseBatch(String text) {
        String[] lines = text.split("\n");
        int added = 0;
        int notFound = 0;
        StringBuilder missing = new StringBuilder();
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            String barcode = line;
            double qty = 1;
            int comma = line.indexOf(',');
            int space = line.indexOf(' ');
            int sep = -1;
            if (comma > 0) sep = comma;
            else if (space > 0) sep = space;
            if (sep > 0) {
                barcode = line.substring(0, sep).trim();
                qty = Math.max(0, NumberUtil.parse(line.substring(sep + 1).trim(), 1));
            }
            Product p = byBarcode.get(barcode);
            if (p == null) {
                notFound++;
                if (missing.length() < 200) {
                    if (missing.length() > 0) missing.append(", ");
                    missing.append(barcode);
                }
                continue;
            }
            addPurchaseItem(p, qty);
            added++;
        }
        String message = added + " item" + (added == 1 ? "" : "s") + " added";
        if (notFound > 0) message += "\nNot found: " + missing;
        DialogUtil.toast(this, message);
    }

    private void addPurchaseItem(Product p, double qty) {
        for (PurchaseItem item : items) {
            if (item.productId.equals(p.uid)) {
                item.qty += qty;
                int factor = item.isWholesale ? Math.max(1, p.wholesaleFactor) : 1;
                item.stockQty = item.stockQty + qty * factor;
                item.lineTotal = item.qty * item.unitPrice;
                afterChange();
                return;
            }
        }
        PurchaseItem item = new PurchaseItem();
        item.productId = p.uid;
        item.productName = p.name;
        item.barcode = p.barcode;
        boolean useWholesale = p.wholesalePrice > 0;
        item.isWholesale = useWholesale;
        item.unitLabel = useWholesale ? p.wholesaleUnit : p.retailUnit;
        item.unitPrice = useWholesale ? p.wholesalePrice : p.retailPrice;
        item.qty = qty;
        item.stockQty = qty * (useWholesale ? Math.max(1, p.wholesaleFactor) : 1);
        item.lineTotal = qty * item.unitPrice;
        items.add(item);
        afterChange();
        com.patechltd.salexfypos.util.SoundUtil.beep();
    }

    private void handleScan(String code) {
        Product p = byBarcode.get(code.trim());
        if (p == null) {
            Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show();
            return;
        }
        for (PurchaseItem item : items) {
            if (item.productId.equals(p.uid)) {
                item.qty += 1;
                item.stockQty = item.stockQty + (item.isWholesale ? Math.max(1, p.wholesaleFactor) : 1);
                item.lineTotal = item.qty * item.unitPrice;
                afterChange();
                return;
            }
        }
        PurchaseItem item = new PurchaseItem();
        item.productId = p.uid;
        item.productName = p.name;
        item.barcode = p.barcode;
        boolean useWholesale = p.wholesalePrice > 0;
        item.isWholesale = useWholesale;
        item.unitLabel = useWholesale ? p.wholesaleUnit : p.retailUnit;
        item.unitPrice = useWholesale ? p.wholesalePrice : p.retailPrice;
        item.qty = 1;
        item.stockQty = useWholesale ? Math.max(1, p.wholesaleFactor) : 1;
        item.lineTotal = item.unitPrice;
        items.add(item);
        afterChange();
        com.patechltd.salexfypos.util.SoundUtil.beep();
    }

    private void editLine(int position) {
        PurchaseItem item = items.get(position);
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_line, null);
        TextView title = view.findViewById(R.id.line_title);
        com.google.android.material.button.MaterialButtonToggleGroup toggle = view.findViewById(R.id.line_unit_toggle);
        com.google.android.material.button.MaterialButton retailBtn = view.findViewById(R.id.line_btn_retail);
        com.google.android.material.button.MaterialButton wholesaleBtn = view.findViewById(R.id.line_btn_wholesale);
        TextInputEditText qtyInput = view.findViewById(R.id.line_qty);
        TextInputEditText priceInput = view.findViewById(R.id.line_price);

        view.findViewById(R.id.line_unit_chips).setVisibility(View.GONE);
        view.findViewById(R.id.line_unit_label).setVisibility(View.GONE);
        view.findViewById(R.id.line_total).setVisibility(View.GONE);

        title.setText(item.productName);
        qtyInput.setText(String.valueOf(item.qty));
        priceInput.setText(String.valueOf(item.unitPrice));
        if (item.isWholesale) wholesaleBtn.setChecked(true);
        else retailBtn.setChecked(true);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit line")
                .setView(view)
                .setPositiveButton("Save", (dialog, which) -> {
                    item.isWholesale = wholesaleBtn.isChecked();
                    item.qty = Math.max(0.001, NumberUtil.parse(qtyInput.getText() == null ? "" : qtyInput.getText().toString(), item.qty));
                    item.unitPrice = NumberUtil.parse(priceInput.getText() == null ? "" : priceInput.getText().toString(), item.unitPrice);
                    Product p = byId.get(item.productId);
                    int factor = item.isWholesale && p != null ? Math.max(1, p.wholesaleFactor) : 1;
                    item.stockQty = item.qty * factor;
                    item.unitLabel = p != null ? (item.isWholesale ? p.wholesaleUnit : p.retailUnit) : item.unitLabel;
                    item.lineTotal = item.qty * item.unitPrice;
                    afterChange();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void afterChange() {
        adapter.submit(items);
        double subtotal = 0;
        for (PurchaseItem item : items) subtotal += item.lineTotal;
        subtotalView.setText(NumberUtil.money(subtotal));
        totalView.setText(NumberUtil.money(subtotal));
    }

    private void pickSupplier() {
        repo.run(() -> {
            List<Supplier> suppliers = repo.suppliers.getSuppliers();
            List<String> names = new ArrayList<>();
            for (Supplier s : suppliers) names.add(s.name);
            names.add("+ New supplier");
            handler.post(() -> DialogUtil.pick(this, "Supplier", names.toArray(new String[0]), -1, which -> {
                if (which < suppliers.size()) {
                    supplierId = suppliers.get(which).uid;
                    supplierRow.setText("Supplier: " + suppliers.get(which).name);
                } else {
                    DialogUtil.inputText(this, "New supplier", "Supplier name", "", "Add", value -> {
                        if (value.trim().isEmpty()) return;
                        Supplier s = new Supplier();
                        s.uid = java.util.UUID.randomUUID().toString();
                        s.name = value.trim();
                        s.createdAt = System.currentTimeMillis();
                        repo.run(() -> {
                            repo.suppliers.insertSupplier(s);
                            handler.post(() -> {
                                supplierId = s.uid;
                                supplierRow.setText("Supplier: " + s.name);
                            });
                        });
                    });
                }
            }));
        });
    }

    private void savePurchase() {
        if (supplierId == null) {
            Toast.makeText(this, "Select a supplier", Toast.LENGTH_SHORT).show();
            return;
        }
        if (items.isEmpty()) {
            Toast.makeText(this, "Add at least one product", Toast.LENGTH_SHORT).show();
            return;
        }
        Purchase purchase = new Purchase();
        purchase.invoiceNo = invoiceNo.getText() == null ? "" : invoiceNo.getText().toString().trim();
        purchase.supplierId = supplierId;
        purchase.purchaseDate = purchaseDate;
        double subtotal = 0;
        for (PurchaseItem item : items) subtotal += item.lineTotal;
        purchase.subtotal = NumberUtil.round2(subtotal);
        purchase.discount = 0;
        purchase.total = NumberUtil.round2(subtotal);
        purchase.paidAmount = NumberUtil.parse(paid.getText() == null ? "" : paid.getText().toString(), 0);
        purchase.status = "COMPLETE";
        purchase.notes = notes.getText() == null ? "" : notes.getText().toString().trim();
        purchase.createdBy = Session.userId(this);
        purchase.createdAt = System.currentTimeMillis();
        List<PurchaseItem> copy = new ArrayList<>(items);

        repo.run(() -> {
            try {
                if (purchase.invoiceNo.isEmpty()) purchase.invoiceNo = repo.nextInvoiceNo();
                repo.savePurchase(purchase, copy, true);
                AppLogger.i("Purchase " + purchase.invoiceNo + " saved");
                handler.post(() -> {
                    Toast.makeText(this, "Purchase saved. Stock updated.", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                handler.post(() -> Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                AppLogger.e("Purchase save failed", e);
            }
        });
    }

    private void loadExisting(String id) {
        repo.run(() -> {
            com.patechltd.salexfypos.db.PurchaseWithItems pw = repo.purchases.getPurchaseWithItems(id);
            final String supplierName;
            if (pw != null && pw.purchase != null && pw.purchase.supplierId != null) {
                Supplier s = repo.suppliers.getSupplier(pw.purchase.supplierId);
                supplierName = s == null ? "—" : s.name;
            } else {
                supplierName = null;
            }
            handler.post(() -> {
                if (pw == null || pw.purchase == null) {
                    finish();
                    return;
                }
                Purchase p = pw.purchase;
                if (supplierName != null) {
                    supplierRow.setText("Supplier: " + supplierName);
                }
                invoiceNo.setText(p.invoiceNo);
                paid.setText(String.valueOf(p.paidAmount));
                notes.setText(p.notes);
                if (p.purchaseDate > 0) {
                    purchaseDate = p.purchaseDate;
                    ((TextInputEditText) findViewById(R.id.date_field)).setText(DateUtil.formatDate(p.purchaseDate));
                }
                items.clear();
                if (pw.items != null) items.addAll(pw.items);
                adapter.submit(items);
                afterChange();
                findViewById(R.id.btn_delete).setVisibility(View.VISIBLE);
            });
        });
    }

    private void confirmDelete() {
        String id = getIntent().getStringExtra("id");
        DialogUtil.confirm(this, "Delete purchase?",
                "This will remove the stock added by this purchase.", () -> repo.run(() -> {
                    Purchase p = repo.purchases.getPurchase(id);
                    if (p != null) repo.deletePurchase(p);
                    handler.post(this::finish);
                }));
    }
}
