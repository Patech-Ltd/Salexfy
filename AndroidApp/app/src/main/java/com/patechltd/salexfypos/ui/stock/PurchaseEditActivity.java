package com.patechltd.salexfypos.ui.stock;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
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
import com.patechltd.salexfypos.db.PurchaseWithItems;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.Product;
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
import java.util.List;

public class PurchaseEditActivity extends AppCompatActivity {

    private static final int REQ_BATCH = 7001;

    private Repository repo;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<PurchaseItem> items = new ArrayList<>();
    private String supplierId;
    private long purchaseDate = System.currentTimeMillis();
    private ScannerView scanner;
    private FrameLayout scannerContainer;
    private PurchaseLineAdapter adapter;
    private TextView supplierRow, subtotalView, totalView;
    private TextInputEditText invoiceNo, paid, notes, dateField;
    private MaterialButton save, delete, toggleScanner, batchAdd, btnEdit, btnPreview, btnPrint;
    private boolean editingExisting;
    private boolean editing;
    private boolean paidTouched;
    private String existingSupplierName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase_edit);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());
        repo = Repository.get(this);
        String editId = getIntent().getStringExtra("id");
        editingExisting = editId != null;

        supplierRow = findViewById(R.id.supplier_row);
        invoiceNo = findViewById(R.id.invoice_no);
        paid = findViewById(R.id.paid);
        notes = findViewById(R.id.notes);
        dateField = findViewById(R.id.date_field);
        subtotalView = findViewById(R.id.subtotal);
        totalView = findViewById(R.id.total);
        save = findViewById(R.id.btn_save);
        delete = findViewById(R.id.btn_delete);
        toggleScanner = findViewById(R.id.btn_toggle_scanner);
        batchAdd = findViewById(R.id.btn_batch_add);
        MaterialButton exact = findViewById(R.id.btn_exact);
        btnEdit = findViewById(R.id.btn_edit);
        btnPreview = findViewById(R.id.btn_preview);
        btnPrint = findViewById(R.id.btn_print_invoice);
        scannerContainer = findViewById(R.id.scanner_container);
        scanner = findViewById(R.id.scanner);
        RecyclerView list = findViewById(R.id.items_list);

        adapter = new PurchaseLineAdapter(new PurchaseLineAdapter.Listener() {
            @Override
            public void onClick(int position) {
                if (editingExisting && !editing) {
                    Toast.makeText(PurchaseEditActivity.this,
                            "Tap \"Edit Purchase\" to change lines", Toast.LENGTH_SHORT).show();
                    return;
                }
                editLine(position);
            }

            @Override
            public void onEditCost(int position) {
                if (editingExisting && !editing) {
                    Toast.makeText(PurchaseEditActivity.this,
                            "Tap \"Edit Purchase\" to change buying prices", Toast.LENGTH_SHORT).show();
                    return;
                }
                editCost(position);
            }

            @Override
            public void onRemove(int position) {
                if (editingExisting && !editing) return;
                items.remove(position);
                afterChange();
            }
        });
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        adapter.setShowProfit(editingExisting);

        repo.run(() -> {
            final java.util.Map<String, Product> map = new java.util.HashMap<>();
            for (Product p : repo.products.getAllActive()) map.put(p.uid, p);
            handler.post(() -> adapter.setProducts(map));
        });

        if (editingExisting) {
            toolbar.setTitle(editing ? "Edit Purchase" : "Purchase Details");
            btnPreview.setVisibility(View.VISIBLE);
            btnPrint.setVisibility(View.VISIBLE);
            btnEdit.setVisibility(View.VISIBLE);
            save.setVisibility(View.GONE);
            toggleScanner.setVisibility(View.GONE);
            batchAdd.setVisibility(View.GONE);
            delete.setVisibility(View.VISIBLE);
            loadExisting(editId);
        } else {
            toolbar.setTitle("New Purchase");
            delete.setVisibility(View.GONE);
            repo.run(() -> {
                String no = repo.nextInvoiceNo();
                handler.post(() -> invoiceNo.setText(no));
            });
            paid.setText("0");
        }
        dateField.setText(DateUtil.formatDate(purchaseDate));

        dateField.setOnClickListener(v -> {
            if (editingExisting && !editing) return;
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

        supplierRow.setOnClickListener(v -> {
            if (editingExisting && !editing) return;
            pickSupplier();
        });

        toggleScanner.setOnClickListener(v -> {
            boolean show = scannerContainer.getVisibility() == View.VISIBLE;
            scannerContainer.setVisibility(show ? View.GONE : View.VISIBLE);
            if (show) scanner.stop();
            else ensurePermissionAndStart();
        });

        batchAdd.setOnClickListener(v ->
                startActivityForResult(new Intent(this, PurchasePickerActivity.class), REQ_BATCH));

        save.setOnClickListener(v -> savePurchase());
        delete.setOnClickListener(v -> confirmDelete());
        btnEdit.setOnClickListener(v -> enterEditMode());
        btnPreview.setOnClickListener(v -> openPreview());
        btnPrint.setOnClickListener(v -> printInvoice());

        exact.setOnClickListener(v -> {
            paidTouched = true;
            paid.setText(String.valueOf(NumberUtil.round2(computeTotal())));
        });
        paid.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) paidTouched = true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (scannerContainer != null && scannerContainer.getVisibility() == View.VISIBLE) {
            ensurePermissionAndStart();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
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

    private void enterEditMode() {
        editing = true;
        ((MaterialToolbar) findViewById(R.id.toolbar)).setTitle("Edit Purchase");
        btnEdit.setVisibility(View.GONE);
        btnPreview.setVisibility(View.GONE);
        btnPrint.setVisibility(View.GONE);
        save.setVisibility(View.VISIBLE);
        toggleScanner.setVisibility(View.VISIBLE);
        batchAdd.setVisibility(View.VISIBLE);
        delete.setVisibility(View.VISIBLE);
        supplierRow.setText(supplierRow.getText());
    }

    private void handleScan(String code) {
        final String trimmed = code == null ? "" : code.trim();
        repo.run(() -> {
            Product p = repo.products.findActiveByBarcode(trimmed);
            handler.post(() -> {
                if (p == null) {
                    Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show();
                    return;
                }
                addPurchaseItem(p, 1);
            });
        });
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
        int factor = useWholesale ? Math.max(1, p.wholesaleFactor) : 1;
        item.isWholesale = useWholesale;
        item.unitLabel = useWholesale ? p.wholesaleUnit : p.retailUnit;
        double buyPrice = p.costPrice > 0
                ? (useWholesale ? p.costPrice * factor : p.costPrice)
                : (useWholesale ? p.wholesalePrice : p.retailPrice);
        item.unitPrice = buyPrice;
        item.qty = qty;
        item.stockQty = qty * factor;
        item.lineTotal = qty * item.unitPrice;
        items.add(item);
        afterChange();
        com.patechltd.salexfypos.util.SoundUtil.beep();
    }

    private void editLine(int position) {
        if (position < 0 || position >= items.size()) return;
        final PurchaseItem item = items.get(position);
        if (item.productId == null) {
            showEditLineDialog(item, null);
            return;
        }
        repo.run(() -> {
            Product p = repo.products.getById(item.productId);
            handler.post(() -> showEditLineDialog(item, p));
        });
    }

    private void editCost(int position) {
        if (position < 0 || position >= items.size()) return;
        final PurchaseItem item = items.get(position);
        if (item.productId == null) {
            showCostInput(item, null);
            return;
        }
        repo.run(() -> {
            Product p = repo.products.getById(item.productId);
            handler.post(() -> showCostInput(item, p));
        });
    }

    private void showCostInput(final PurchaseItem item, final Product p) {
        DialogUtil.input(this, "Buying price per " + (item.unitLabel == null ? "" : item.unitLabel),
                "New buying price for " + item.productName,
                String.valueOf(item.unitPrice), "Save", value -> {
                    double price = NumberUtil.parse(value, item.unitPrice);
                    applyCostChange(item, p, price);
                });
    }

    private void applyCostChange(final PurchaseItem item, final Product p, final double price) {
        item.unitPrice = NumberUtil.round2(Math.max(0, price));
        item.lineTotal = NumberUtil.round2(item.qty * item.unitPrice);
        afterChange();
        int factor = (p != null && item.isWholesale && p.wholesaleFactor > 0) ? p.wholesaleFactor : 1;
        final double unitCost = NumberUtil.round2(item.unitPrice / factor);
        if (p == null) return;
        repo.run(() -> {
            Product live = repo.products.getById(p.uid);
            if (live != null) {
                live.costPrice = unitCost;
                live.updatedAt = System.currentTimeMillis();
                repo.products.update(live);
            }
        });
    }

    private void showEditLineDialog(final PurchaseItem item, final Product p) {
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
                    item.qty = Math.max(0.001, NumberUtil.parse(
                            qtyInput.getText() == null ? "" : qtyInput.getText().toString(), item.qty));
                    double price = NumberUtil.parse(
                            priceInput.getText() == null ? "" : priceInput.getText().toString(), item.unitPrice);
                    int factor = item.isWholesale && p != null ? Math.max(1, p.wholesaleFactor) : 1;
                    item.stockQty = item.qty * factor;
                    item.unitLabel = p != null
                            ? (item.isWholesale ? p.wholesaleUnit : p.retailUnit) : item.unitLabel;
                    applyCostChange(item, p, price);
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
        if (!editingExisting && !paidTouched) {
            paid.setText(String.valueOf(NumberUtil.round2(subtotal)));
        }
    }

    private double computeTotal() {
        double total = 0;
        for (PurchaseItem item : items) total += item.lineTotal;
        return NumberUtil.round2(total);
    }

    private void pickSupplier() {
        repo.run(() -> {
            List<Supplier> suppliers = repo.suppliers.getSuppliers();
            List<String> names = new ArrayList<>();
            names.add("＋ Add new supplier");
            for (Supplier s : suppliers) names.add(s.name);
            handler.post(() -> DialogUtil.pick(this, "Select supplier",
                    names.toArray(new String[0]), -1, which -> {
                if (which == 0) {
                    addNewSupplier();
                } else {
                    int idx = which - 1;
                    if (idx >= 0 && idx < suppliers.size()) {
                        supplierId = suppliers.get(idx).uid;
                        supplierRow.setText("Supplier: " + suppliers.get(idx).name);
                    }
                }
            }));
        });
    }

    private void addNewSupplier() {
        DialogUtil.inputText(this, "New supplier", "Supplier name", "", "Add", value -> {
            if (value.trim().isEmpty()) return;
            final String name = value.trim();
            Supplier s = new Supplier();
            s.uid = java.util.UUID.randomUUID().toString();
            s.name = name;
            s.createdAt = System.currentTimeMillis();
            repo.run(() -> {
                repo.suppliers.insertSupplier(s);
                handler.post(() -> {
                    supplierId = s.uid;
                    supplierRow.setText("Supplier: " + name);
                });
            });
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
        if (editingExisting) {
            purchase.uid = getIntent().getStringExtra("id");
        }
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
        purchase.createdAt = editingExisting ? System.currentTimeMillis() : System.currentTimeMillis();
        List<PurchaseItem> copy = new ArrayList<>(items);

        repo.run(() -> {
            try {
                if (purchase.invoiceNo.isEmpty()) purchase.invoiceNo = repo.nextInvoiceNo();
                if (editingExisting) {
                    repo.updatePurchase(purchase, copy, false);
                } else {
                    repo.savePurchase(purchase, copy, false);
                }
                AppLogger.i("Purchase " + purchase.invoiceNo + (editingExisting ? " updated" : " saved"));
                handler.post(() -> {
                    Toast.makeText(this, editingExisting
                            ? "Purchase updated. Stock re-adjusted."
                            : "Purchase saved. Stock updated.", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                handler.post(() -> Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                AppLogger.e("Purchase save failed", e);
            }
        });
    }

    private void openPreview() {
        Intent i = new Intent(this, PurchasePreviewActivity.class);
        i.putExtra("purchaseId", getIntent().getStringExtra("id"));
        startActivity(i);
    }

    private void printInvoice() {
        repo.run(() -> {
            PurchaseWithItems pw = repo.purchases.getPurchaseWithItems(getIntent().getStringExtra("id"));
            if (pw == null || pw.purchase == null) return;
            String supplierName = existingSupplierName;
            if (supplierName == null && pw.purchase.supplierId != null) {
                Supplier s = repo.suppliers.getSupplier(pw.purchase.supplierId);
                supplierName = s == null ? null : s.name;
            }
            final String fName = supplierName;
            handlePrint(pw, fName);
        });
    }

    private void handlePrint(final PurchaseWithItems pw, final String supplierName) {
        handler.post(() -> com.patechltd.salexfypos.print.PrinterManager.printPurchase(this, pw.purchase,
                pw.items == null ? new ArrayList<>() : pw.items, supplierName,
                (ok, msg) -> runOnUiThread(() -> DialogUtil.toast(this, msg))));
    }

    private void loadExisting(String id) {
        repo.run(() -> {
            PurchaseWithItems pw = repo.purchases.getPurchaseWithItems(id);
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
                existingSupplierName = supplierName != null && !"—".equals(supplierName)
                        ? supplierName : null;
                if (supplierName != null) {
                    supplierRow.setText("Supplier: " + supplierName);
                    supplierId = p.supplierId;
                }
                invoiceNo.setText(p.invoiceNo);
                paid.setText(String.valueOf(p.paidAmount));
                notes.setText(p.notes);
                if (p.purchaseDate > 0) {
                    purchaseDate = p.purchaseDate;
                    dateField.setText(DateUtil.formatDate(p.purchaseDate));
                }
                items.clear();
                if (pw.items != null) items.addAll(pw.items);
                adapter.submit(items);
                afterChange();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_BATCH && resultCode == RESULT_OK && data != null) {
            String[] ids = data.getStringArrayExtra(PurchasePickerActivity.EXTRA_IDS);
            String[] qtys = data.getStringArrayExtra(PurchasePickerActivity.EXTRA_QTYS);
            if (ids == null || ids.length == 0) return;
            List<Product> products = new ArrayList<>();
            List<Double> quantities = new ArrayList<>();
            repo.run(() -> {
                for (int i = 0; i < ids.length; i++) {
                    Product prod = repo.products.getById(ids[i]);
                    if (prod != null) {
                        products.add(prod);
                        double q = qtys != null && i < qtys.length && qtys[i] != null
                                ? NumberUtil.parse(qtys[i], 1) : 1;
                        quantities.add(Math.max(0.001, q));
                    }
                }
                final List<Product> prods = new ArrayList<>(products);
                final List<Double> qtysList = new ArrayList<>(quantities);
                handler.post(() -> {
                    for (int i = 0; i < prods.size(); i++) {
                        addPurchaseItem(prods.get(i), qtysList.get(i));
                    }
                    Toast.makeText(this, prods.size() + " product" + (prods.size() == 1 ? "" : "s")
                            + " added", Toast.LENGTH_SHORT).show();
                });
            });
        }
    }
}