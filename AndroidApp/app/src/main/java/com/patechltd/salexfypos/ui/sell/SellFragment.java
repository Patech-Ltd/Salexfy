package com.patechltd.salexfypos.ui.sell;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.adapter.CartAdapter;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.Product;
import com.patechltd.salexfypos.db.entity.ProductUnit;
import com.patechltd.salexfypos.db.entity.Sale;
import com.patechltd.salexfypos.db.entity.SaleItem;
import com.patechltd.salexfypos.db.entity.SalePayment;
import com.patechltd.salexfypos.db.entity.User;
import com.patechltd.salexfypos.model.PaymentMethod;
import com.patechltd.salexfypos.scanner.ScannerView;
import com.patechltd.salexfypos.security.PermissionChecker;
import com.patechltd.salexfypos.security.Session;
import com.patechltd.salexfypos.ui.products.QuickAddProductActivity;
import com.patechltd.salexfypos.util.AppLogger;
import com.patechltd.salexfypos.util.DialogUtil;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.Prefs;
import com.patechltd.salexfypos.util.SoundUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SellFragment extends Fragment {

    private Repository repo;
    private ScannerView scanner;
    private CartAdapter cartAdapter;
    private final List<SaleItem> cart = new ArrayList<>();
    private boolean wholesaleMode;
    private Sale currentSale;
    private TextView subtotalView, taxView, totalView, itemCountView, feedbackView, transNoView;
    private MaterialButton btnHold, btnCheckout, btnPause, btnTogglePreview, btnSearch;
    private View previewContainer, emptyCartView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean previewVisible = true;
    private boolean scanningPaused = false;
    private static final int REQ_PAYMENT = 4101;
    private static final int REQ_PRODUCT_SEARCH = 4102;
    private static final int REQ_HELD = 4103;
    private static final int REQ_QUICK_STOCK = 4104;
    private double checkoutSubtotal;
    private double checkoutTax;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sell, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = Repository.get(requireContext());

        scanner = view.findViewById(R.id.scanner);
        previewContainer = view.findViewById(R.id.preview_container);
        emptyCartView = view.findViewById(R.id.empty_cart);
        RecyclerView cartList = view.findViewById(R.id.cart_list);
        subtotalView = view.findViewById(R.id.subtotal);
        taxView = view.findViewById(R.id.tax_amount);
        totalView = view.findViewById(R.id.total);
        itemCountView = view.findViewById(R.id.item_count);
        transNoView = view.findViewById(R.id.trans_no);
        feedbackView = view.findViewById(R.id.scan_feedback);
        btnHold = view.findViewById(R.id.btn_hold);
        btnCheckout = view.findViewById(R.id.btn_checkout);
        btnPause = view.findViewById(R.id.btn_pause);
        btnTogglePreview = view.findViewById(R.id.btn_toggle_preview);
        btnSearch = view.findViewById(R.id.btn_search);
        view.findViewById(R.id.btn_sales).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SalesHistoryActivity.class)));
        view.findViewById(R.id.btn_recent_sales).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SalesHistoryActivity.class)));
        view.findViewById(R.id.btn_manual_sale).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ManualSaleActivity.class)));

        MaterialButtonToggleGroup toggle = view.findViewById(R.id.unit_toggle);
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            boolean wholesale = checkedId == R.id.btn_wholesale;
            if (wholesale == wholesaleMode) return;
            wholesaleMode = wholesale;
            convertCartPrices();
        });
        toggle.check(R.id.btn_retail);

        cartAdapter = new CartAdapter(new CartAdapter.Listener() {
            @Override
            public void onPlus(int position) {
                adjustQty(position, 1);
            }

            @Override
            public void onMinus(int position) {
                adjustQty(position, -1);
            }

            @Override
            public void onRemove(int position) {
                cart.remove(position);
                afterCartChange();
            }

            @Override
            public void onClick(int position) {
                editLine(position);
            }
        });
        cartList.setLayoutManager(new LinearLayoutManager(requireContext()));
        cartList.setAdapter(cartAdapter);

        scanner.setOnScanListener((text, format) -> handleScan(text));

        btnHold.setOnClickListener(v -> holdCurrent());
        btnCheckout.setOnClickListener(v -> openCheckout());
        btnPause.setOnClickListener(v -> toggleScanning());
        btnTogglePreview.setOnClickListener(v -> togglePreview());
        btnSearch.setOnClickListener(v -> openProductSearch());
        view.findViewById(R.id.btn_held).setOnClickListener(v -> openHeldList());

        updateHeldBadge();
    }

    @Override
    public void onResume() {
        super.onResume();
        ensurePermissionAndStart();
        updateHeldBadge();
    }

    @Override
    public void onPause() {
        super.onPause();
        scanner.stop();
    }

    private void ensurePermissionAndStart() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.CAMERA}, 1001);
            return;
        }
        scanner.start(getViewLifecycleOwner());
        if (Prefs.getBoolean(requireContext(), Prefs.KEY_TORCH_ON_START, false)) {
            handler.postDelayed(() -> scanner.setTorch(true), 800);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            scanner.start(getViewLifecycleOwner());
        } else {
            scanner.setStatus("Camera permission needed to scan");
        }
    }

    private void updateHeldBadge() {
        repo.run(() -> {
            int count = repo.sales.getHeld().size();
            handler.post(() -> {
                if (getActivity() instanceof com.patechltd.salexfypos.MainActivity) {
                    ((com.patechltd.salexfypos.MainActivity) getActivity()).updateHeldBadge(count);
                }
            });
        });
    }

    private void handleScan(String code) {
        final String trimmed = code == null ? "" : code.trim();
        if (trimmed.isEmpty()) return;
        repo.run(() -> {
            Product product = repo.products.findActiveByBarcode(trimmed);
            handler.post(() -> {
                if (product == null) {
                    SoundUtil.errorBeep();
                    showFeedback("Product not found", false);
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Unknown barcode")
                            .setMessage("No product has barcode " + trimmed + ".\nAdd it quickly?")
                            .setPositiveButton("Quick add", (d, w) -> {
                                Intent i = new Intent(requireContext(), QuickAddProductActivity.class);
                                i.putExtra("barcode", trimmed);
                                startActivity(i);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                } else {
                    addToCart(product);
                }
            });
        });
    }

    private void addToCart(Product product) {
        double qty = 1;
        double unitPrice = wholesaleMode ? product.wholesalePrice : product.retailPrice;
        if (unitPrice <= 0) unitPrice = wholesaleMode ? product.retailPrice : product.wholesalePrice;
        String unitLabel = resolveUnitLabel(product, wholesaleMode);
        int factor = wholesaleMode ? Math.max(1, product.wholesaleFactor) : 1;
        double stockQty = qty * factor;

        for (SaleItem item : cart) {
            if (item.productId.equals(product.uid)
                    && java.util.Objects.equals(item.unitLabel, unitLabel)) {
                item.qty += qty;
                item.stockQty += stockQty;
                item.lineTotal = item.qty * item.unitPrice;
                SoundUtil.beep();
                showFeedback(product.name + " x" + NumberUtil.qty(item.qty), true);
                afterCartChange();
                return;
            }
        }

        SaleItem item = new SaleItem();
        item.uid = UUID.randomUUID().toString();
        item.productId = product.uid;
        item.productName = product.name;
        item.barcode = product.barcode;
        item.qty = qty;
        item.stockQty = stockQty;
        item.unitLabel = unitLabel;
        item.factor = factor;
        item.isWholesale = wholesaleMode;
        item.unitPrice = unitPrice;
        item.costPrice = product.costPrice;
        item.lineTotal = unitPrice;
        cart.add(item);
        SoundUtil.beep();
        showFeedback(product.name + " added", true);
        afterCartChange();
    }

    private String resolveUnitLabel(Product p, boolean wholesale) {
        if (wholesale) {
            if (p.wholesaleUnit != null && !p.wholesaleUnit.isEmpty()) return p.wholesaleUnit;
            return resolveUnitLabel(p, false);
        }
        if (p.retailUnit != null && !p.retailUnit.isEmpty()) return p.retailUnit;
        return "Pcs";
    }

    private void adjustQty(int position, int delta) {
        SaleItem item = cart.get(position);
        item.qty += delta;
        double factor = item.factor > 0 ? item.factor : 1;
        if (item.qty <= 0) {
            cart.remove(position);
        } else {
            item.stockQty = item.qty * factor;
            item.lineTotal = item.qty * item.unitPrice;
        }
        afterCartChange();
    }

    private void convertCartPrices() {
        if (cart.isEmpty()) return;
        repo.run(() -> {
            Map<String, Product> byId = new HashMap<>();
            for (SaleItem item : cart) {
                if (!byId.containsKey(item.productId)) {
                    Product p = repo.products.getById(item.productId);
                    if (p != null) byId.put(item.productId, p);
                }
            }
            handler.post(() -> {
                boolean changed = false;
                for (SaleItem item : cart) {
                    Product p = byId.get(item.productId);
                    if (p == null) continue;
                    changed = true;
                    item.isWholesale = wholesaleMode;
                    item.unitLabel = resolveUnitLabel(p, wholesaleMode);
                    double price = wholesaleMode ? p.wholesalePrice : p.retailPrice;
                    if (price <= 0) price = wholesaleMode ? p.retailPrice : p.wholesalePrice;
                    item.unitPrice = price;
                    int factor = wholesaleMode ? Math.max(1, p.wholesaleFactor) : 1;
                    item.factor = factor;
                    item.costPrice = p.costPrice;
                    item.stockQty = item.qty * factor;
                    item.lineTotal = item.qty * item.unitPrice;
                }
                if (changed) {
                    afterCartChange();
                    showFeedback(wholesaleMode ? "Prices switched to wholesale" : "Prices switched to retail", false);
                }
            });
        });
    }

    private void editLine(int position) {
        SaleItem item = cart.get(position);
        repo.run(() -> {
            Product loaded = repo.products.getById(item.productId);
            List<ProductUnit> loadedUnits = loaded == null ? null
                    : repo.products.getUnitsByProduct(loaded.uid);
            handler.post(() -> buildEditLineDialog(item, loaded, loadedUnits));
        });
    }

    private void buildEditLineDialog(SaleItem item, Product product, List<ProductUnit> productUnits) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_line, null);

        TextView title = view.findViewById(R.id.line_title);
        ChipGroup unitChips = view.findViewById(R.id.line_unit_chips);
        TextInputEditText qtyInput = view.findViewById(R.id.line_qty);
        TextView unitLabel = view.findViewById(R.id.line_unit_label);
        TextInputEditText priceInput = view.findViewById(R.id.line_price);
        TextView totalView = view.findViewById(R.id.line_total);

        view.findViewById(R.id.line_unit_toggle).setVisibility(View.GONE);

        title.setText(item.productName);
        qtyInput.setText(String.valueOf(item.qty));
        priceInput.setText(String.valueOf(item.unitPrice));
        priceInput.setEnabled(false);
        priceInput.setFocusable(false);
        TextInputLayout priceLayout = view.findViewById(R.id.line_price_layout);
        if (priceLayout != null) {
            priceLayout.setHelperText("Price is fixed at the saved product price");
        }

        if (productUnits != null && !productUnits.isEmpty()) {
            for (ProductUnit pu : productUnits) {
                Chip chip = new Chip(requireContext());
                chip.setText(pu.unitName == null || pu.unitName.isEmpty() ? "Unit" : pu.unitName);
                chip.setTag(pu);
                chip.setCheckable(true);
                chip.setChecked(pu.unitName != null && pu.unitName.equals(item.unitLabel));
                unitChips.addView(chip);
            }
        } else {
            for (boolean wholesale : new boolean[]{false, true}) {
                Chip chip = new Chip(requireContext());
                chip.setText(wholesale ? "Wholesale" : "Retail");
                chip.setTag(wholesale);
                chip.setCheckable(true);
                chip.setChecked(item.isWholesale == wholesale);
                unitChips.addView(chip);
            }
        }

        final String[] selectedUnitName = {item.unitLabel};
        final double[] selectedPrice = {item.unitPrice};
        final double[] selectedFactor = {1};

        Runnable refreshPrice = () -> {
            double qty = Math.max(0.001, NumberUtil.parse(
                    qtyInput.getText() == null ? "" : qtyInput.getText().toString(), item.qty));
            double price = NumberUtil.parse(
                    priceInput.getText() == null ? "" : priceInput.getText().toString(), selectedPrice[0]);
            String unit = selectedUnitName[0] == null ? "" : selectedUnitName[0];
            unitLabel.setText("Unit: " + (unit.isEmpty() ? "—" : unit)
                    + (Math.abs(selectedFactor[0] - 1) > 0.001 ? "  (" + NumberUtil.qty(selectedFactor[0]) + " base units each)" : ""));
            String c = Prefs.currency(requireContext());
            totalView.setText("Line total: " + c + " " + NumberUtil.money(qty * price));
        };

        unitChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            Chip chip = group.findViewById(checkedIds.get(0));
            if (chip == null) return;
            Object tag = chip.getTag();
            if (tag instanceof ProductUnit) {
                ProductUnit pu = (ProductUnit) tag;
                selectedUnitName[0] = pu.unitName == null || pu.unitName.isEmpty() ? "Unit" : pu.unitName;
                selectedPrice[0] = pu.price;
                selectedFactor[0] = Math.max(0.001, pu.factor);
                priceInput.setText(String.valueOf(pu.price));
            } else if (tag instanceof Boolean) {
                boolean wholesale = (Boolean) tag;
                if (product != null) {
                    selectedUnitName[0] = resolveUnitLabel(product, wholesale);
                    selectedPrice[0] = wholesale ? product.wholesalePrice : product.retailPrice;
                    if (selectedPrice[0] <= 0) {
                        selectedPrice[0] = wholesale ? product.retailPrice : product.wholesalePrice;
                    }
                    selectedFactor[0] = wholesale ? Math.max(1, product.wholesaleFactor) : 1;
                    priceInput.setText(String.valueOf(selectedPrice[0]));
                }
            }
            refreshPrice.run();
        });

        qtyInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                refreshPrice.run();
            }
        });
        priceInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                refreshPrice.run();
            }
        });

        refreshPrice.run();

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Edit line")
                .setView(view)
                .setPositiveButton("Save", (dialog, which) -> {
                    item.unitLabel = selectedUnitName[0];
                    item.qty = Math.max(0.001, NumberUtil.parse(
                            qtyInput.getText() == null ? "" : qtyInput.getText().toString(), item.qty));
                    item.unitPrice = NumberUtil.parse(
                            priceInput.getText() == null ? "" : priceInput.getText().toString(),
                            selectedPrice[0]);
                    item.isWholesale = selectedFactor[0] > 1;
                    item.factor = selectedFactor[0];
                    item.costPrice = product.costPrice;
                    item.stockQty = item.qty * selectedFactor[0];
                    item.lineTotal = item.qty * item.unitPrice;
                    afterCartChange();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void afterCartChange() {
        cartAdapter.submit(cart);
        itemCountView.setText(cart.size() + " item" + (cart.size() == 1 ? "" : "s"));
        updateTransNo();
        updateTotals();
        scheduleDraftSave();
        autoManagePreview();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (emptyCartView == null) return;
        emptyCartView.setVisibility(cart.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updateTransNo() {
        if (transNoView == null) return;
        String no = currentSale != null && currentSale.saleNo != null ? currentSale.saleNo : "";
        transNoView.setText(no.isEmpty() ? "" : "Trans #" + no);
    }

    private void updateTotals() {
        double subtotal = 0;
        for (SaleItem item : cart) subtotal += item.lineTotal;
        double taxPercent = 0;//Prefs.taxPercent(requireContext());
        double tax = subtotal * taxPercent / 100.0;
        double total = subtotal + tax;
        String c = Prefs.currency(requireContext());
        subtotalView.setText(c + " " + NumberUtil.money(subtotal));
        taxView.setText(c + " " + NumberUtil.money(tax));
        totalView.setText(c + " " + NumberUtil.money(total));
    }

    private void autoManagePreview() {
        int size = cart.size();
        if (size >= 10 && previewVisible) {
            setPreviewVisible(false);
        } else if (size < 8 && !previewVisible) {
            setPreviewVisible(true);
        }
    }

    private void setPreviewVisible(boolean visible) {
        previewVisible = visible;
        scanner.setPreviewVisible(visible);
        previewContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
        btnTogglePreview.setIconResource(visible ? R.drawable.ic_visibility_off : R.drawable.ic_visibility);
    }

    private void togglePreview() {
        setPreviewVisible(!previewVisible);
    }

    private void toggleScanning() {
        scanningPaused = !scanningPaused;
        scanner.setSuspend(scanningPaused);
        btnPause.setIconResource(scanningPaused ? R.drawable.ic_play : R.drawable.ic_pause);
        showFeedback(scanningPaused ? "Scanning paused" : "Scanning live", false);
    }

    private void showFeedback(String message, boolean good) {
        feedbackView.setText(message);
        feedbackView.setBackgroundResource(good ? R.drawable.bg_scan_feedback : R.drawable.bg_torch_button);
        feedbackView.setVisibility(View.VISIBLE);
        handler.removeCallbacks(feedbackHide);
        handler.postDelayed(feedbackHide, 1100);
    }

    private final Runnable feedbackHide = () -> feedbackView.setVisibility(View.GONE);

    private void scheduleDraftSave() {
        handler.removeCallbacks(draftSave);
        handler.postDelayed(draftSave, 900);
    }

    private final Runnable draftSave = () -> {
        if (cart.isEmpty()) return;
        final Sale existing = currentSale;
        if (existing != null && "COMPLETE".equals(existing.status)) return;
        final List<SaleItem> items = new ArrayList<>(cart);
        repo.run(() -> {
            final Sale sale = existing != null ? existing : buildSale("DRAFT");
            repo.saveDraft(sale, items);
            if (existing == null) handler.post(() -> {
                currentSale = sale;
                updateTransNo();
            });
        });
    };

    private Sale buildSale(String status) {
        Sale sale = new Sale();
        sale.uid = UUID.randomUUID().toString();
        sale.saleNo = repo.nextSaleNo();
        sale.saleDate = System.currentTimeMillis();
        User user = repo.admin.getUser(Session.userId(requireContext()));
        sale.cashierId = user != null ? user.uid : null;
        sale.cashierName = user != null ? user.fullName : "";
        sale.status = status;
        sale.createdBy = sale.cashierId;
        sale.createdAt = System.currentTimeMillis();
        return sale;
    }

    private void holdCurrent() {
        if (cart.isEmpty()) {
            Toast.makeText(requireContext(), "Cart is empty", Toast.LENGTH_SHORT).show();
            return;
        }
        DialogUtil.inputText(requireContext(), "Hold transaction", "Add a note (optional)", "",
                "Hold", note -> {
                    final Sale existing = currentSale;
                    final List<SaleItem> items = new ArrayList<>(cart);
                    repo.run(() -> {
                        final Sale sale = existing != null ? existing : buildSale("HELD");
                        sale.status = "HELD";
                        sale.notes = note.trim();
                        repo.holdSale(sale, items);
                        if (existing == null) handler.post(() -> currentSale = sale);
                        handler.post(() -> {
                            clearCart();
                            Toast.makeText(requireContext(), "Sale held. You can recall it anytime.",
                                    Toast.LENGTH_SHORT).show();
                        });
                    });
                });
    }

    private void clearCart() {
        cart.clear();
        currentSale = null;
        cartAdapter.submit(cart);
        itemCountView.setText("0 items");
        updateTransNo();
        updateTotals();
        autoManagePreview();
        updateEmptyState();
    }

    private void openCheckout() {
        if (cart.isEmpty()) {
            Toast.makeText(requireContext(), "Cart is empty", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!PermissionChecker.has(requireContext(), com.patechltd.salexfypos.model.Authority.SELL_VIEW)) {
            Toast.makeText(requireContext(), "You do not have permission to sell", Toast.LENGTH_SHORT).show();
            return;
        }
        double subtotal = cart.stream().mapToDouble(item -> item.lineTotal).sum();
        double taxPercent = 0;//Prefs.taxPercent(requireContext());
        double tax = subtotal * taxPercent / 100.0;
        double total = subtotal + tax;
        checkoutSubtotal = subtotal;
        checkoutTax = tax;
        final List<SaleItem> snapshot = new ArrayList<>(cart);

        repo.run(() -> {
            List<SaleItem> low = lowStockItems(snapshot);
            // Resolve current stock for low items here, in the background, so the
            // UI-thread dialog code below never has to touch the repo directly.
            Map<String, Double> stockByProductId = new HashMap<>();
            for (SaleItem item : low) {
                stockByProductId.put(item.productId, repo.products.getCurrentQty(item.productId));
            }
            handler.post(() -> {
                if (low.isEmpty()) {
                    launchPayment(total);
                } else {
                    showLowStockDialog(low, total, stockByProductId);
                }
            });
        });
    }

    private List<SaleItem> lowStockItems(List<SaleItem> snapshot) {
        List<SaleItem> low = new ArrayList<>();
        for (SaleItem item : snapshot) {
            double stock = repo.products.getCurrentQty(item.productId);
            if (stock + 0.001 < item.stockQty) low.add(item);
        }
        return low;
    }

    private void showLowStockDialog(List<SaleItem> low, double total, Map<String, Double> stockByProductId) {
        StringBuilder sb = new StringBuilder();
        for (SaleItem item : low) {
            double stock = stockByProductId.getOrDefault(item.productId, 0.0);
            String unit = item.unitLabel == null || item.unitLabel.isEmpty() ? "" : " " + item.unitLabel;
            sb.append("• ").append(item.productName)
                    .append("  (have ").append(NumberUtil.qty(stock))
                    .append(", need ").append(NumberUtil.qty(item.stockQty)).append(unit).append(")\n");
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Low stock")
                .setMessage("You are selling more than you have:\n\n" + sb
                        + "\nIt's fine to sell anyway - stock will go negative, or top it up first.")
                .setNeutralButton("Cancel", null)
                .setNegativeButton("Quick add stock", (d, w) -> openQuickStock(low, stockByProductId))
                .setPositiveButton("Sell anyway", (d, w) -> launchPayment(total))
                .show();
    }

    private void openQuickStock(List<SaleItem> low, Map<String, Double> stockByProductId) {
        int n = low.size();
        String[] ids = new String[n];
        String[] names = new String[n];
        String[] units = new String[n];
        double[] current = new double[n];
        double[] needed = new double[n];
        for (int i = 0; i < n; i++) {
            SaleItem item = low.get(i);
            ids[i] = item.productId;
            names[i] = item.productName;
            units[i] = item.unitLabel == null ? "" : item.unitLabel;
            current[i] = stockByProductId.getOrDefault(item.productId, 0.0);
            needed[i] = item.stockQty;
        }
        Intent i = new Intent(requireContext(), com.patechltd.salexfypos.ui.stock.QuickStockActivity.class);
        i.putExtra(com.patechltd.salexfypos.ui.stock.QuickStockActivity.EXTRA_IDS, ids);
        i.putExtra(com.patechltd.salexfypos.ui.stock.QuickStockActivity.EXTRA_NAMES, names);
        i.putExtra(com.patechltd.salexfypos.ui.stock.QuickStockActivity.EXTRA_UNITS, units);
        i.putExtra(com.patechltd.salexfypos.ui.stock.QuickStockActivity.EXTRA_CURRENT, current);
        i.putExtra(com.patechltd.salexfypos.ui.stock.QuickStockActivity.EXTRA_NEEDED, needed);
        i.putExtra(com.patechltd.salexfypos.ui.stock.QuickStockActivity.EXTRA_HINT,
                "Top up the stock below, then continue to payment.");
        i.putExtra("fromCheckout", true);
        startActivityForResult(i, REQ_QUICK_STOCK);
    }

    private void launchPayment(double total) {
        Intent i = new Intent(requireContext(), PaymentActivity.class);
        i.putExtra(PaymentActivity.EXTRA_TOTAL, total);
        i.putExtra(PaymentActivity.EXTRA_SUBTOTAL, checkoutSubtotal);
        i.putExtra(PaymentActivity.EXTRA_TAX, checkoutTax);
        startActivityForResult(i, REQ_PAYMENT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PRODUCT_SEARCH && resultCode == android.app.Activity.RESULT_OK && data != null) {
            String productId = data.getStringExtra(ProductSearchActivity.EXTRA_PRODUCT_ID);
            if (productId != null) {
                repo.run(() -> {
                    Product loaded = repo.products.getById(productId);
                    if (loaded != null && loaded.isActive) {
                        handler.post(() -> addToCart(loaded));
                    }
                });
            }
            return;
        }
        if (requestCode == REQ_HELD && resultCode == android.app.Activity.RESULT_OK && data != null) {
            handleHeldResult(data);
            return;
        }
        if (requestCode == REQ_QUICK_STOCK && resultCode == android.app.Activity.RESULT_OK) {
            launchPayment(checkoutSubtotal + checkoutTax);
            return;
        }
        if (requestCode != REQ_PAYMENT) return;
        if (resultCode == PaymentActivity.RESULT_HOLD) {
            holdCurrent();
            return;
        }
        if (resultCode != android.app.Activity.RESULT_OK || data == null) return;
        String[] methods = data.getStringArrayExtra(PaymentActivity.EXTRA_METHODS);
        double[] amounts = data.getDoubleArrayExtra(PaymentActivity.EXTRA_AMOUNTS);
        String[] customerIds = data.getStringArrayExtra(PaymentActivity.EXTRA_CUSTOMER_IDS);
        String[] customerNames = data.getStringArrayExtra(PaymentActivity.EXTRA_CUSTOMER_NAMES);
        double pointsUsed = data.getDoubleExtra(PaymentActivity.EXTRA_POINTS_USED, 0);
        String note = data.getStringExtra(PaymentActivity.EXTRA_NOTES);
        completeSale(methods, amounts, customerIds, customerNames, pointsUsed, note,
                checkoutSubtotal, checkoutTax);
    }

    private void completeSale(String[] methods, double[] amounts, String[] customerIds,
                              String[] customerNames, double pointsUsed, String notes,
                              double subtotal, double tax) {
        if (methods == null || methods.length == 0) {
            Toast.makeText(requireContext(), "No payment method selected", Toast.LENGTH_SHORT).show();
            return;
        }
        double paidTotal = 0;
        double allocated = 0;
        String custId = null;
        String custName = null;
        for (int i = 0; i < methods.length; i++) {
            double amount = amounts[i];
            allocated += amount;
            if ("CREDIT".equals(methods[i])) {
                if (custId == null) {
                    custId = customerIds[i];
                    custName = customerNames[i];
                }
            } else {
                paidTotal += amount;
            }
        }
        final boolean fOnCredit = custId != null;
        final String fCustomer = custName;
        final String fCustomerId = custId;
        final double fPaid = paidTotal;
        final double fAllocated = allocated;
        final double fPointsUsed = pointsUsed;
        final double fSubtotal = subtotal;
        final double fTax = tax;
        final double fPointsValue = fPointsUsed
                * Prefs.getDouble(requireContext(), Prefs.KEY_LOYALTY_POINT_VALUE, 0.5);
        final Sale existing = currentSale;
        final List<SaleItem> items = new ArrayList<>(cart);
        final List<SalePayment> payments = new ArrayList<>();
        for (int i = 0; i < methods.length; i++) {
            SalePayment p = new SalePayment();
            p.method = methods[i];
            p.amount = amounts[i];
            p.customerId = customerIds[i];
            p.customerName = customerNames[i];
            payments.add(p);
        }
        repo.run(() -> {
            final Sale sale = existing != null ? existing : buildSale("COMPLETE");
            sale.status = "COMPLETE";
            sale.subtotal = NumberUtil.round2(fSubtotal);
            sale.discount = NumberUtil.round2(fPointsValue);
            sale.taxAmount = NumberUtil.round2(fTax);
            sale.total = NumberUtil.round2(fSubtotal + fTax - fPointsValue);
            sale.paymentMethod = methods[0];
            sale.pointsRedeemed = fPointsUsed;
            sale.notes = notes;
            sale.saleDate = System.currentTimeMillis();

            String resolvedCustomerId = fCustomerId;
            if (fCustomer != null && !fCustomer.isEmpty()) {
                com.patechltd.salexfypos.db.entity.Customer existingCust =
                        repo.suppliers.findCustomerByName(fCustomer);
                if (existingCust == null && fCustomerId == null) {
                    com.patechltd.salexfypos.db.entity.Customer c = new com.patechltd.salexfypos.db.entity.Customer();
                    c.uid = UUID.randomUUID().toString();
                    c.name = fCustomer;
                    c.createdAt = System.currentTimeMillis();
                    repo.suppliers.insertCustomer(c);
                    sale.customerId = c.uid;
                    sale.customerName = fCustomer;
                    resolvedCustomerId = c.uid;
                } else if (existingCust != null) {
                    sale.customerId = existingCust.uid;
                    sale.customerName = existingCust.name;
                    resolvedCustomerId = existingCust.uid;
                } else {
                    sale.customerId = fCustomerId;
                    sale.customerName = fCustomer;
                }
            }
            sale.paidAmount = NumberUtil.round2(fPaid);
            sale.changeAmount = fOnCredit
                    ? 0
                    : NumberUtil.round2(Math.max(0, fAllocated - sale.total));
            final double fEarned = NumberUtil.round2(sale.total
                    * Prefs.getDouble(requireContext(), Prefs.KEY_LOYALTY_POINTS_PER_MONEY, 1.0));
            sale.pointsEarned = fEarned;
            final double fCustomerBalance = repo.outstandingDebt(resolvedCustomerId);

            try {
                repo.completeSale(sale, items, payments);
                applyLoyalty(resolvedCustomerId, fEarned, fPointsUsed);
                if (existing == null) handler.post(() -> currentSale = sale);
                handler.post(() -> {
                    clearCart();
                    AppLogger.i("Sale " + sale.saleNo + " completed, total " + sale.total);
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Sale Complete")
                            .setMessage(buildReceipt(sale, items, payments))
                            .setNeutralButton("Preview receipt", (d, w) -> {
                                Intent i = new Intent(requireContext(), ReceiptPreviewActivity.class);
                                i.putExtra("saleId", sale.uid);
                                startActivity(i);
                            })
                            .setPositiveButton("Done", null)
                            .show();
                    if (com.patechltd.salexfypos.print.PrinterManager.isPrintingEnabled(requireContext())) {
                        com.patechltd.salexfypos.print.PrinterManager.print(requireContext(), sale, items,
                                payments, fCustomerBalance, (ok, msg) -> {
                                    if (!ok) DialogUtil.toast(requireContext(), msg);
                                });
                    }
                });
            } catch (Exception e) {
                handler.post(() -> Toast.makeText(requireContext(), "Checkout failed: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
                AppLogger.e("Checkout failed", e);
            }
        });
    }

    private void applyLoyalty(String customerId, double earned, double pointsUsed) {
        if (customerId == null || customerId.isEmpty()) return;
        com.patechltd.salexfypos.db.entity.Customer c = repo.suppliers.getCustomer(customerId);
        if (c == null) return;
        if (pointsUsed > 0) {
            c.loyaltyPoints = Math.max(0, c.loyaltyPoints - pointsUsed);
        }
        c.loyaltyPoints = NumberUtil.round2(c.loyaltyPoints + earned);
        c.totalSpent = NumberUtil.round2(c.totalSpent + earned
                / Math.max(0.0001, Prefs.getDouble(requireContext(), Prefs.KEY_LOYALTY_POINTS_PER_MONEY, 1.0)));
        repo.suppliers.insertCustomer(c);
    }

    private String buildReceipt(Sale sale, List<SaleItem> items, List<SalePayment> payments) {
        StringBuilder sb = new StringBuilder();
        String shop = Prefs.getString(requireContext(), Prefs.KEY_SHOP_NAME, "My Shop");
        sb.append(shop).append('\n');
        sb.append("Sale No: ").append(sale.saleNo).append('\n');
        sb.append("Cashier: ").append(sale.cashierName).append('\n');
        sb.append("Date: ").append(com.patechltd.salexfypos.util.DateUtil.format(sale.saleDate)).append('\n');
        sb.append("----------------------------\n");
        for (SaleItem item : items) {
            sb.append(item.productName).append('\n');
            String unit = item.unitLabel == null || item.unitLabel.isEmpty() ? "" : " " + item.unitLabel;
            sb.append("  ").append(NumberUtil.qty(item.qty)).append(unit).append(" × ")
                    .append(NumberUtil.money(item.unitPrice)).append("  = ")
                    .append(NumberUtil.money(item.lineTotal)).append('\n');
        }
        sb.append("----------------------------\n");
        sb.append("Subtotal: ").append(NumberUtil.money(sale.subtotal)).append('\n');
        if (sale.discount > 0) sb.append("Points discount: -").append(NumberUtil.money(sale.discount)).append('\n');
        sb.append("TOTAL: ").append(NumberUtil.money(sale.total)).append('\n');
        if (payments != null && !payments.isEmpty()) {
            sb.append("----------------------------\n");
            for (SalePayment p : payments) {
                sb.append(PaymentMethod.labelOf(p.method));
                if (p.customerName != null) sb.append(" (").append(p.customerName).append(")");
                sb.append(": ").append(NumberUtil.money(p.amount)).append('\n');
            }
            if (sale.paidAmount > 0) sb.append("Paid: ").append(NumberUtil.money(sale.paidAmount)).append('\n');
            if (sale.changeAmount > 0) sb.append("Change: ").append(NumberUtil.money(sale.changeAmount)).append('\n');
            double balance = sale.total - sale.paidAmount;
            if (balance > 0.001) sb.append("Balance: ").append(NumberUtil.money(balance)).append('\n');
        } else if ("CREDIT".equals(sale.paymentMethod)) {
            sb.append("Payment: ON CREDIT\n");
            if (sale.customerName != null) sb.append("Customer: ").append(sale.customerName).append('\n');
            if (sale.paidAmount > 0) sb.append("Paid now: ").append(NumberUtil.money(sale.paidAmount)).append('\n');
            sb.append("Balance: ").append(NumberUtil.money(sale.total - sale.paidAmount)).append('\n');
        } else {
            sb.append("Payment: ").append(PaymentMethod.labelOf(sale.paymentMethod)).append('\n');
            sb.append("Paid: ").append(NumberUtil.money(sale.paidAmount)).append('\n');
            sb.append("Change: ").append(NumberUtil.money(sale.changeAmount)).append('\n');
        }
        if (sale.pointsEarned > 0) {
            sb.append("Loyalty points earned: ").append(NumberUtil.qty(sale.pointsEarned)).append('\n');
        }
        String footer = Prefs.getString(requireContext(), Prefs.KEY_RECEIPT_FOOTER, "");
        if (!footer.isEmpty()) sb.append(footer).append('\n');
        return sb.toString();
    }

    private void openProductSearch() {
        Intent i = new Intent(requireContext(), ProductSearchActivity.class);
        i.putExtra(ProductSearchActivity.EXTRA_WHOLESALE, wholesaleMode);
        startActivityForResult(i, REQ_PRODUCT_SEARCH);
    }

    private void openHeldList() {
        startActivityForResult(new Intent(requireContext(), HeldSalesActivity.class), REQ_HELD);
    }

    private void handleHeldResult(Intent data) {
        String saleId = data.getStringExtra(HeldSalesActivity.EXTRA_SALE_ID);
        if (saleId == null) return;
        repo.run(() -> {
            com.patechltd.salexfypos.db.SaleWithItems sw = repo.sales.getSaleWithItems(saleId);
            if (sw == null) return;
            handler.post(() -> {
                if (sw.items == null) return;
                cart.clear();
                cart.addAll(sw.items);
                currentSale = sw.sale;
                currentSale.status = "DRAFT";
                cartAdapter.submit(cart);
                itemCountView.setText(cart.size() + " items");
                updateTransNo();
                updateTotals();
                autoManagePreview();
                updateEmptyState();
                Toast.makeText(requireContext(), "Transaction " + sw.sale.saleNo + " recalled",
                        Toast.LENGTH_SHORT).show();
            });
        });
    }
}