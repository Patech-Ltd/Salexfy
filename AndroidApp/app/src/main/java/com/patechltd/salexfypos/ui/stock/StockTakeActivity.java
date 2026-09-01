package com.patechltd.salexfypos.ui.stock;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;
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
import com.google.android.material.textfield.TextInputEditText;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.adapter.StockTakeLineAdapter;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.Product;
import com.patechltd.salexfypos.db.entity.StockTake;
import com.patechltd.salexfypos.db.entity.StockTakeItem;
import com.patechltd.salexfypos.db.entity.User;
import com.patechltd.salexfypos.scanner.ScannerView;
import com.patechltd.salexfypos.security.Session;
import com.patechltd.salexfypos.ui.sell.ProductSearchActivity;
import com.patechltd.salexfypos.util.AppLogger;
import com.patechltd.salexfypos.util.DateUtil;
import com.patechltd.salexfypos.util.DialogUtil;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.SoundUtil;

import java.util.ArrayList;
import java.util.List;

public class StockTakeActivity extends AppCompatActivity {

    private Repository repo;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<StockTakeLineAdapter.Line> lines = new ArrayList<>();
    private long takeDate = System.currentTimeMillis();
    private ScannerView scanner;
    private FrameLayout scannerContainer;
    private StockTakeLineAdapter adapter;
    private TextInputEditText nameInput;
    private String existingId;
    private static final int REQ_SEARCH = 41;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_take);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        repo = Repository.get(this);
        existingId = getIntent().getStringExtra("id");

        nameInput = findViewById(R.id.name);
        TextInputEditText dateField = findViewById(R.id.date_field);
        scannerContainer = findViewById(R.id.scanner_container);
        scanner = findViewById(R.id.scanner);
        MaterialButton toggleScanner = findViewById(R.id.btn_toggle_scanner);
        MaterialButton search = findViewById(R.id.btn_search);
        MaterialButton complete = findViewById(R.id.btn_complete);
        RecyclerView list = findViewById(R.id.items_list);

        adapter = new StockTakeLineAdapter(this::editLine);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        dateField.setText(DateUtil.formatDate(takeDate));
        dateField.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Stock take date")
                    .setSelection(takeDate)
                    .build();
            picker.addOnPositiveButtonClickListener(sel -> {
                takeDate = sel;
                dateField.setText(DateUtil.formatDate(sel));
            });
            picker.show(getSupportFragmentManager(), "date2");
        });

        toggleScanner.setOnClickListener(v -> {
            boolean show = scannerContainer.getVisibility() == View.VISIBLE;
            scannerContainer.setVisibility(show ? View.GONE : View.VISIBLE);
            if (show) scanner.stop();
            else ensurePermissionAndStart();
        });

        search.setOnClickListener(v -> {
            Intent i = new Intent(this, ProductSearchActivity.class);
            i.putExtra(ProductSearchActivity.EXTRA_WHOLESALE, false);
            startActivityForResult(i, REQ_SEARCH);
        });

        complete.setOnClickListener(v -> completeTake());

        if (existingId != null) {
            toolbar.setTitle("Stock Take Details");
            complete.setVisibility(View.GONE);
            toggleScanner.setVisibility(View.GONE);
            search.setVisibility(View.GONE);
            nameInput.setEnabled(false);
            dateField.setEnabled(false);
            adapter.setReadOnly(true);
            loadExisting(existingId);
        }
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
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1003);
            return;
        }
        scanner.setOnScanListener((text, format) -> handleScan(text));
        scanner.start(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1003 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            scanner.setOnScanListener((text, format) -> handleScan(text));
            scanner.start(this);
        }
    }

    private void handleScan(String code) {
        final String trimmed = code == null ? "" : code.trim();
        repo.run(() -> {
            Product p = repo.products.findActiveByBarcode(trimmed);
            final double systemQty = p == null ? 0 : repo.stock.currentQty(p.uid);
            handler.post(() -> {
                if (p == null) {
                    Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show();
                    return;
                }
                incrementProduct(p, systemQty);
            });
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_SEARCH && resultCode == RESULT_OK && data != null) {
            String productId = data.getStringExtra(ProductSearchActivity.EXTRA_PRODUCT_ID);
            if (productId == null) return;
            repo.run(() -> {
                Product p = repo.products.getById(productId);
                final double systemQty = p == null ? 0 : repo.stock.currentQty(p.uid);
                handler.post(() -> {
                    if (p == null) {
                        Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    for (StockTakeLineAdapter.Line line : lines) {
                        if (line.productId.equals(p.uid)) {
                            Toast.makeText(this, "Already on the list — tap it to set the count", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    addLine(p, systemQty, systemQty);
                    SoundUtil.beep();
                    Toast.makeText(this, "Added " + p.name + " — tap it to set the counted quantity", Toast.LENGTH_SHORT).show();
                });
            });
        }
    }

    private void incrementProduct(Product p, double systemQty) {
        for (StockTakeLineAdapter.Line line : lines) {
            if (line.productId.equals(p.uid)) {
                line.countedQty += 1;
                adapter.submit(lines);
                SoundUtil.beep();
                return;
            }
        }
        addLine(p, systemQty, systemQty + 1);
        SoundUtil.beep();
    }

    private void addLine(Product p, double systemQty, double counted) {
        StockTakeLineAdapter.Line line = new StockTakeLineAdapter.Line();
        line.productId = p.uid;
        line.name = p.name;
        line.unit = p.retailUnit;
        line.systemQty = systemQty;
        line.countedQty = counted;
        lines.add(line);
        adapter.submit(lines);
    }

    private void editLine(int position) {
        StockTakeLineAdapter.Line line = lines.get(position);
        DialogUtil.input(this, "Counted quantity",
                "Counted quantity for " + line.name,
                String.valueOf(line.countedQty), "Save", value -> {
                    double q = NumberUtil.parse(value, line.countedQty);
                    line.countedQty = Math.max(0, q);
                    adapter.submit(lines);
                });
    }

    private void completeTake() {
        String name = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
        if (name.isEmpty()) name = "Stock take " + DateUtil.formatDate(takeDate);
        final String fName = name;
        if (lines.isEmpty()) {
            Toast.makeText(this, "No items counted", Toast.LENGTH_SHORT).show();
            return;
        }
        StockTake take = new StockTake();
        take.name = fName;
        take.stockTakeDate = takeDate;
        take.status = "COMPLETE";
        take.createdBy = Session.userId(this);
        take.createdAt = System.currentTimeMillis();
        List<StockTakeItem> items = new ArrayList<>();
        for (StockTakeLineAdapter.Line line : lines) {
            StockTakeItem item = new StockTakeItem();
            item.productId = line.productId;
            item.systemQty = line.systemQty;
            item.countedQty = line.countedQty;
            item.diffQty = line.countedQty - line.systemQty;
            items.add(item);
        }
        repo.run(() -> {
            try {
                repo.applyStockTake(take, items);
                AppLogger.i("Stock take completed: " + fName);
                handler.post(() -> {
                    Toast.makeText(this, "Stock take completed. Stock adjusted.", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                handler.post(() -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                AppLogger.e("Stock take failed", e);
            }
        });
    }

    private void loadExisting(String id) {
        repo.run(() -> {
            StockTake take = repo.stock.getStockTake(id);
            List<StockTakeItem> items = repo.stock.getStockTakeItems(id);
            List<StockTakeLineAdapter.Line> loaded = new ArrayList<>();
            if (take != null) {
                for (StockTakeItem item : items) {
                    Product p = repo.products.getById(item.productId);
                    StockTakeLineAdapter.Line line = new StockTakeLineAdapter.Line();
                    line.productId = item.productId;
                    line.name = p != null ? p.name : "Unknown";
                    line.unit = p != null ? p.retailUnit : "";
                    line.systemQty = item.systemQty;
                    line.countedQty = item.countedQty;
                    loaded.add(line);
                }
            }
            final StockTake finalTake = take;
            handler.post(() -> {
                if (finalTake == null) {
                    finish();
                    return;
                }
                nameInput.setText(finalTake.name);
                ((TextInputEditText) findViewById(R.id.date_field)).setText(DateUtil.formatDate(finalTake.stockTakeDate));
                lines.clear();
                lines.addAll(loaded);
                adapter.submit(lines);
            });
        });
    }
}
