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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.adapter.CartAdapter;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.Category;
import com.patechltd.salexfypos.db.entity.Customer;
import com.patechltd.salexfypos.db.entity.Product;
import com.patechltd.salexfypos.db.entity.ProductBarcode;
import com.patechltd.salexfypos.db.entity.Sale;
import com.patechltd.salexfypos.db.entity.SaleItem;
import com.patechltd.salexfypos.db.entity.User;
import com.patechltd.salexfypos.model.PaymentMethod;
import com.patechltd.salexfypos.scanner.ScannerView;
import com.patechltd.salexfypos.security.PermissionChecker;
import com.patechltd.salexfypos.security.Session;
import com.patechltd.salexfypos.ui.products.ProductEditActivity;
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
    private final Map<String, Product> byBarcode = new HashMap<>();
    private final List<Product> products = new ArrayList<>();
    private boolean wholesaleMode;
    private Sale currentSale;
    private TextView subtotalView, taxView, totalView, itemCountView, feedbackView, transNoView;
    private MaterialButton btnHold, btnCheckout, btnPause, btnTogglePreview, btnSearch;
    private View previewContainer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean previewVisible = true;
    private boolean scanningPaused = false;
    private boolean productsLoaded;

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
        view.findViewById(R.id.btn_held).setOnClickListener(v -> showHeldList());

        loadProducts();
    }

    @Override
    public void onResume() {
        super.onResume();
        ensurePermissionAndStart();
        loadProducts();
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

    private void loadProducts() {
        repo.run(() -> {
            List<Product> all = repo.products.getAllActive();
            List<ProductBarcode> extra = repo.products.getAllBarcodes();
            handler.post(() -> {
                products.clear();
                products.addAll(all);
                byBarcode.clear();
                Map<String, Product> byId = new HashMap<>();
                for (Product p : all) {
                    byId.put(p.id, p);
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
                productsLoaded = true;
            });
        });
    }

    private void handleScan(String code) {
        if (!productsLoaded) {
            loadProducts();
            handler.postDelayed(() -> handleScan(code), 150);
            return;
        }
        Product product = byBarcode.get(code.trim());
        if (product == null) {
            SoundUtil.errorBeep();
            showFeedback("Product not found", false);
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Unknown barcode")
                    .setMessage("No product has barcode " + code + ".\nAdd it as a new product?")
                    .setPositiveButton("Add Product", (d, w) -> {
                        Intent i = new Intent(requireContext(), ProductEditActivity.class);
                        i.putExtra("barcode", code);
                        startActivity(i);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }
        addToCart(product);
    }

    private void addToCart(Product product) {
        double qty = 1;
        double unitPrice = wholesaleMode ? product.wholesalePrice : product.retailPrice;
        String unitLabel = wholesaleMode ? product.wholesaleUnit : product.retailUnit;
        int factor = wholesaleMode ? Math.max(1, product.wholesaleFactor) : 1;
        double stockQty = qty * factor;

        for (SaleItem item : cart) {
            if (item.productId.equals(product.id) && item.isWholesale == wholesaleMode) {
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
        item.productId = product.id;
        item.productName = product.name;
        item.barcode = product.barcode;
        item.qty = qty;
        item.stockQty = stockQty;
        item.unitLabel = unitLabel;
        item.isWholesale = wholesaleMode;
        item.unitPrice = unitPrice;
        item.costPrice = product.costPrice;
        item.lineTotal = unitPrice;
        cart.add(item);
        SoundUtil.beep();
        showFeedback(product.name + " added", true);
        afterCartChange();
    }

    private void adjustQty(int position, int delta) {
        SaleItem item = cart.get(position);
        item.qty += delta;
        int factor = item.isWholesale ? factorFor(item.productId) : 1;
        if (item.qty <= 0) {
            cart.remove(position);
        } else {
            item.stockQty = item.qty * factor;
            item.lineTotal = item.qty * item.unitPrice;
        }
        afterCartChange();
    }

    private int factorFor(String productId) {
        for (Product p : products) {
            if (p.id.equals(productId)) return Math.max(1, p.wholesaleFactor);
        }
        return 1;
    }

    private void convertCartPrices() {
        if (cart.isEmpty()) return;
        for (SaleItem item : cart) {
            Product p = findProduct(item.productId);
            if (p == null) continue;
            item.isWholesale = wholesaleMode;
            item.unitLabel = wholesaleMode ? p.wholesaleUnit : p.retailUnit;
            if (item.unitLabel == null || item.unitLabel.isEmpty()) {
                item.unitLabel = p.retailUnit;
            }
            double price = wholesaleMode ? p.wholesalePrice : p.retailPrice;
            if (price <= 0) price = wholesaleMode ? p.retailPrice : p.wholesalePrice;
            item.unitPrice = price;
            int factor = wholesaleMode ? Math.max(1, p.wholesaleFactor) : 1;
            item.stockQty = item.qty * factor;
            item.lineTotal = item.qty * item.unitPrice;
        }
        afterCartChange();
        showFeedback(wholesaleMode ? "Prices switched to wholesale" : "Prices switched to retail", false);
    }

    private void editLine(int position) {
        SaleItem item = cart.get(position);
        Product product = findProduct(item.productId);
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_line, null);

        TextView title = view.findViewById(R.id.line_title);
        MaterialButtonToggleGroup toggle = view.findViewById(R.id.line_unit_toggle);
        MaterialButton retailBtn = view.findViewById(R.id.line_btn_retail);
        MaterialButton wholesaleBtn = view.findViewById(R.id.line_btn_wholesale);
        TextInputEditText qtyInput = view.findViewById(R.id.line_qty);
        TextView unitLabel = view.findViewById(R.id.line_unit_label);
        TextInputEditText priceInput = view.findViewById(R.id.line_price);
        TextView totalView = view.findViewById(R.id.line_total);

        title.setText(item.productName);
        qtyInput.setText(String.valueOf(item.qty));
        if (item.isWholesale) {
            wholesaleBtn.setChecked(true);
        } else {
            retailBtn.setChecked(true);
        }

        priceInput.setFocusable(false);
        priceInput.setClickable(false);
        priceInput.setCursorVisible(false);

        Runnable refreshPrice = () -> {
            boolean wholesale = wholesaleBtn.isChecked();
            double price = 0;
            String unit = "";
            if (product != null) {
                price = wholesale ? product.wholesalePrice : product.retailPrice;
                if (price <= 0) price = wholesale ? product.retailPrice : product.wholesalePrice;
                unit = wholesale ? product.wholesaleUnit : product.retailUnit;
                if (unit == null) unit = "";
            } else {
                price = item.unitPrice;
            }
            double qty = Math.max(0.001, NumberUtil.parse(qtyInput.getText() == null ? "" : qtyInput.getText().toString(), item.qty));
            priceInput.setText(NumberUtil.money(price));
            unitLabel.setText("Price (" + (wholesale ? "wholesale" : "retail") + (unit.isEmpty() ? "" : " — " + unit) + ")");
            String c = Prefs.currency(requireContext());
            totalView.setText("Line total: " + c + " " + NumberUtil.money(qty * price));
        };

        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
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

        refreshPrice.run();

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Edit line")
                .setView(view)
                .setPositiveButton("Save", (dialog, which) -> {
                    item.isWholesale = wholesaleBtn.isChecked();
                    item.qty = Math.max(0.001, NumberUtil.parse(qtyInput.getText() == null ? "" : qtyInput.getText().toString(), item.qty));
                    if (product != null) {
                        double price = item.isWholesale ? product.wholesalePrice : product.retailPrice;
                        if (price <= 0) price = item.isWholesale ? product.retailPrice : product.wholesalePrice;
                        item.unitPrice = price;
                        item.unitLabel = item.isWholesale ? product.wholesaleUnit : product.retailUnit;
                    }
                    int factor = item.isWholesale && product != null ? Math.max(1, product.wholesaleFactor) : 1;
                    item.stockQty = item.qty * factor;
                    item.lineTotal = item.qty * item.unitPrice;
                    afterCartChange();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private Product findProduct(String id) {
        for (Product p : products) {
            if (p.id.equals(id)) return p;
        }
        return null;
    }

    private void afterCartChange() {
        cartAdapter.submit(cart);
        itemCountView.setText(cart.size() + " item" + (cart.size() == 1 ? "" : "s"));
        updateTransNo();
        updateTotals();
        scheduleDraftSave();
        autoManagePreview();
    }

    private void updateTransNo() {
        if (transNoView == null) return;
        String no = currentSale != null && currentSale.saleNo != null ? currentSale.saleNo : "";
        transNoView.setText(no.isEmpty() ? "" : "Trans #" + no);
    }

    private void updateTotals() {
        double subtotal = 0;
        for (SaleItem item : cart) subtotal += item.lineTotal;
        double taxPercent = Prefs.taxPercent(requireContext());
        double tax = subtotal * taxPercent / 100.0;
        double total = subtotal + tax;
        String c = Prefs.currency(requireContext());
        subtotalView.setText(c + " " + NumberUtil.money(subtotal));
        taxView.setText(c + " " + NumberUtil.money(tax));
        totalView.setText(c + " " + NumberUtil.money(total));
    }

    private void autoManagePreview() {
        int size = cart.size();
        if (size >= 8 && previewVisible) {
            setPreviewVisible(false);
        } else if (size < 6 && !previewVisible) {
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
        sale.id = UUID.randomUUID().toString();
        sale.saleNo = repo.nextSaleNo();
        sale.saleDate = System.currentTimeMillis();
        User user = repo.admin.getUser(Session.userId(requireContext()));
        sale.cashierId = user != null ? user.id : null;
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
        double subtotal = 0;
        for (SaleItem item : cart) subtotal += item.lineTotal;
        double taxPercent = Prefs.taxPercent(requireContext());
        double tax = subtotal * taxPercent / 100.0;
        double total = subtotal + tax;
        final double fSubtotal = subtotal;
        final double fTotal = total;
        final List<SaleItem> snapshot = new ArrayList<>(cart);

        repo.run(() -> {
            final String blocked = checkStock(snapshot);
            final List<Customer> customers = repo.suppliers.getCustomers();
            final Map<String, Double> debts = new HashMap<>();
            for (com.patechltd.salexfypos.db.DebtorBalanceRow d : repo.suppliers.getDebtorBalances()) {
                debts.put(d.customerId, d.outstanding);
            }
            handler.post(() -> {
                if (blocked != null) {
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Not enough stock")
                            .setMessage("There is not enough stock for:\n" + blocked)
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }
                new CheckoutDialog(requireContext(), fTotal, 0, customers, debts, "",
                        (method, customer, customerId, paid, pointsUsed, notes) ->
                                completeSale(method, customer, customerId, paid, pointsUsed, notes,
                                        fSubtotal, tax))
                        .show();
            });
        });
    }

    private String checkStock(List<SaleItem> snapshot) {
        StringBuilder sb = new StringBuilder();
        for (SaleItem item : snapshot) {
            double stock = repo.products.getCurrentQty(item.productId);
            if (stock < item.stockQty - 0.001) {
                sb.append("• ").append(item.productName).append(" (need ")
                        .append(NumberUtil.qty(item.stockQty)).append(", have ")
                        .append(NumberUtil.qty(stock)).append(")\n");
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private void completeSale(PaymentMethod method, String customer, String customerId, double paid,
                              double pointsUsed, String notes, double subtotal, double tax) {
        boolean onCredit = method == PaymentMethod.CREDIT;
        if (onCredit && (customer == null || customer.isEmpty())) {
            Toast.makeText(requireContext(), "Select a customer for credit sales", Toast.LENGTH_SHORT).show();
            return;
        }
        final boolean fOnCredit = onCredit;
        final String fCustomer = customer;
        final String fCustomerId = customerId;
        final double fPaid = paid;
        final double fPointsUsed = pointsUsed;
        final double fSubtotal = subtotal;
        final double fTax = tax;
        final double fPointsValue = fPointsUsed
                * Prefs.getDouble(requireContext(), Prefs.KEY_LOYALTY_POINT_VALUE, 0.5);
        final Sale existing = currentSale;
        final List<SaleItem> items = new ArrayList<>(cart);
        repo.run(() -> {
            final Sale sale = existing != null ? existing : buildSale("COMPLETE");
            sale.status = "COMPLETE";
            sale.subtotal = NumberUtil.round2(fSubtotal);
            sale.discount = NumberUtil.round2(fPointsValue);
            sale.taxAmount = NumberUtil.round2(fTax);
            sale.total = NumberUtil.round2(fSubtotal + fTax - fPointsValue);
            sale.paymentMethod = method.name();
            sale.pointsRedeemed = fPointsUsed;
            sale.notes = notes;
            sale.saleDate = System.currentTimeMillis();

            String resolvedCustomerId = fCustomerId;
            if (fCustomer != null && !fCustomer.isEmpty()) {
                com.patechltd.salexfypos.db.entity.Customer existingCust = repo.suppliers.findCustomerByName(fCustomer);
                if (existingCust == null) {
                    com.patechltd.salexfypos.db.entity.Customer c = new com.patechltd.salexfypos.db.entity.Customer();
                    c.id = UUID.randomUUID().toString();
                    c.name = fCustomer;
                    c.createdAt = System.currentTimeMillis();
                    repo.suppliers.insertCustomer(c);
                    sale.customerId = c.id;
                    sale.customerName = fCustomer;
                    resolvedCustomerId = c.id;
                } else {
                    sale.customerId = existingCust.id;
                    sale.customerName = existingCust.name;
                    resolvedCustomerId = existingCust.id;
                }
            }
            if (fOnCredit) {
                sale.paidAmount = NumberUtil.round2(fPaid);
                sale.changeAmount = 0;
            } else {
                sale.paidAmount = NumberUtil.round2(fPaid);
                sale.changeAmount = Math.max(0, fPaid - sale.total);
            }
            final double fEarned = NumberUtil.round2(sale.total
                    * Prefs.getDouble(requireContext(), Prefs.KEY_LOYALTY_POINTS_PER_MONEY, 1.0));
            sale.pointsEarned = fEarned;

            try {
                repo.completeSale(sale, items);
                applyLoyalty(resolvedCustomerId, fEarned, fPointsUsed);
                if (existing == null) handler.post(() -> currentSale = sale);
                handler.post(() -> {
                    clearCart();
                    AppLogger.i("Sale " + sale.saleNo + " completed, total " + sale.total);
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Sale Complete")
                            .setMessage(buildReceipt(sale, items))
                            .setPositiveButton("Done", null)
                            .show();
                    if (com.patechltd.salexfypos.print.PrinterManager.isPrintingEnabled(requireContext())) {
                        com.patechltd.salexfypos.print.PrinterManager.print(requireContext(), sale, items,
                                (ok, msg) -> {
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

    private String buildReceipt(Sale sale, List<SaleItem> items) {
        StringBuilder sb = new StringBuilder();
        String shop = Prefs.getString(requireContext(), Prefs.KEY_SHOP_NAME, "My Shop");
        sb.append(shop).append('\n');
        sb.append("Sale No: ").append(sale.saleNo).append('\n');
        sb.append("Cashier: ").append(sale.cashierName).append('\n');
        sb.append("Date: ").append(com.patechltd.salexfypos.util.DateUtil.format(sale.saleDate)).append('\n');
        sb.append("----------------------------\n");
        for (SaleItem item : items) {
            sb.append(item.productName).append('\n');
            sb.append("  ").append(NumberUtil.qty(item.qty)).append(" × ")
                    .append(NumberUtil.money(item.unitPrice)).append("  = ")
                    .append(NumberUtil.money(item.lineTotal)).append('\n');
        }
        sb.append("----------------------------\n");
        sb.append("Subtotal: ").append(NumberUtil.money(sale.subtotal)).append('\n');
        if (sale.taxAmount > 0) sb.append("Tax: ").append(NumberUtil.money(sale.taxAmount)).append('\n');
        if (sale.discount > 0) sb.append("Points discount: -").append(NumberUtil.money(sale.discount)).append('\n');
        sb.append("TOTAL: ").append(NumberUtil.money(sale.total)).append('\n');
        if ("CREDIT".equals(sale.paymentMethod)) {
            sb.append("Payment: ON CREDIT\n");
            if (sale.customerName != null) sb.append("Customer: ").append(sale.customerName).append('\n');
            if (sale.paidAmount > 0) sb.append("Paid now: ").append(NumberUtil.money(sale.paidAmount)).append('\n');
            sb.append("Balance: ").append(NumberUtil.money(sale.total - sale.paidAmount)).append('\n');
        } else {
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

    private void showHeldList() {
        repo.run(() -> {
            List<Sale> held = repo.sales.getHeld();
            if (held.isEmpty()) {
                handler.post(() -> Toast.makeText(requireContext(), "No held transactions", Toast.LENGTH_SHORT).show());
                return;
            }
            List<com.patechltd.salexfypos.db.SaleWithItems> rows = new ArrayList<>();
            for (Sale s : held) {
                com.patechltd.salexfypos.db.SaleWithItems sw = repo.sales.getSaleWithItems(s.id);
                if (sw != null) rows.add(sw);
            }
            handler.post(() -> new HeldListDialog(requireContext(), rows,
                    new HeldListDialog.Callback() {
                        @Override
                        public void onRecall(Sale sale) {
                            recallHeld(sale);
                        }

                        @Override
                        public void onDelete(Sale sale) {
                            deleteHeld(sale);
                        }
                    }).show());
        });
    }

    private void deleteHeld(Sale sale) {
        repo.run(() -> {
            repo.sales.deleteItemsForSale(sale.id);
            repo.sales.deleteSale(sale.id);
            handler.post(() -> {
                showHeldList();
                Toast.makeText(requireContext(), "Held transaction deleted", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void recallHeld(Sale held) {
        repo.run(() -> {
            com.patechltd.salexfypos.db.SaleWithItems sw = repo.sales.getSaleWithItems(held.id);
            handler.post(() -> {
                if (sw == null || sw.items == null) return;
                cart.clear();
                cart.addAll(sw.items);
                currentSale = held;
                held.status = "DRAFT";
                cartAdapter.submit(cart);
                itemCountView.setText(cart.size() + " items");
                updateTransNo();
                updateTotals();
                autoManagePreview();
                Toast.makeText(requireContext(), "Transaction " + held.saleNo + " recalled",
                        Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void openProductSearch() {
        repo.run(() -> {
            final List<Category> cats = repo.directory.getCategories();
            final List<Product> snapshot = new ArrayList<>(products);
            handler.post(() -> new ProductSearchDialog(requireContext(), snapshot, cats, wholesaleMode,
                    product -> addToCart(product)).show());
        });
    }
}
