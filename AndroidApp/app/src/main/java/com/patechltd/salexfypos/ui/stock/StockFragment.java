package com.patechltd.salexfypos.ui.stock;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Transformations;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.adapter.PurchaseAdapter;
import com.patechltd.salexfypos.adapter.StockAdapter;
import com.patechltd.salexfypos.adapter.StockTakeAdapter;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.Purchase;
import com.patechltd.salexfypos.db.entity.StockTake;
import com.patechltd.salexfypos.db.entity.Supplier;
import com.patechltd.salexfypos.model.Authority;
import com.patechltd.salexfypos.security.PermissionChecker;
import com.patechltd.salexfypos.util.DialogUtil;

import java.util.List;

public class StockFragment extends Fragment {

    private static final int REQ_SCAN = 6001;
    private static final int REQ_EXPORT_PURCHASES = 6002;
    private Repository repo;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private StockAdapter stockAdapter;
    private PurchaseAdapter purchaseAdapter;
    private StockTakeAdapter takeAdapter;
    private View levelsPanel, purchasesPanel, takesPanel;
    private TextView tabLevels, tabPurchases, tabTakes, lowStockCount;
    private final androidx.lifecycle.MutableLiveData<String> stockQuery =
            new androidx.lifecycle.MutableLiveData<>("");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stock, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = Repository.get(requireContext());

        levelsPanel = view.findViewById(R.id.levels_panel);
        purchasesPanel = view.findViewById(R.id.purchases_panel);
        takesPanel = view.findViewById(R.id.takes_panel);
        tabLevels = view.findViewById(R.id.tab_levels);
        tabPurchases = view.findViewById(R.id.tab_purchases);
        tabTakes = view.findViewById(R.id.tab_takes);
        lowStockCount = view.findViewById(R.id.low_stock_count);
        lowStockCount.setOnClickListener(v -> startActivity(new Intent(requireContext(),
                com.patechltd.salexfypos.ui.stock.LowStockActivity.class)));
        view.findViewById(R.id.btn_stock_summary).setOnClickListener(v ->
                startActivity(new Intent(requireContext(),
                        com.patechltd.salexfypos.ui.stock.StockSummaryActivity.class)));
        EditText search = view.findViewById(R.id.stock_search);

        RecyclerView stockList = view.findViewById(R.id.stock_list);
        stockAdapter = new StockAdapter();
        stockList.setLayoutManager(new LinearLayoutManager(requireContext()));
        stockList.setAdapter(stockAdapter);

        RecyclerView purchasesList = view.findViewById(R.id.purchases_list);
        purchaseAdapter = new PurchaseAdapter(p -> {
            Intent i = new Intent(requireContext(), PurchaseViewActivity.class);
            i.putExtra("id", p.uid);
            startActivity(i);
        });
        purchasesList.setLayoutManager(new LinearLayoutManager(requireContext()));
        purchasesList.setAdapter(purchaseAdapter);

        RecyclerView takesList = view.findViewById(R.id.takes_list);
        takeAdapter = new StockTakeAdapter(t -> openTake(t));
        takesList.setLayoutManager(new LinearLayoutManager(requireContext()));
        takesList.setAdapter(takeAdapter);

        MaterialButton newPurchase = view.findViewById(R.id.btn_new_purchase);
        MaterialButton newTake = view.findViewById(R.id.btn_new_take);
        newPurchase.setOnClickListener(v -> startActivity(new Intent(requireContext(), PurchaseEditActivity.class)));
        newTake.setOnClickListener(v -> startActivity(new Intent(requireContext(), StockTakeActivity.class)));
        view.findViewById(R.id.btn_export_purchases).setOnClickListener(v ->
                startActivityForResult(com.patechltd.salexfypos.util.StorageUtil.createReportIntent(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx", "purchases_" + System.currentTimeMillis()),
                        REQ_EXPORT_PURCHASES));
        if (!PermissionChecker.has(requireContext(), Authority.PURCHASE_EDIT)) {
            newPurchase.setVisibility(View.GONE);
        }
        if (!PermissionChecker.has(requireContext(), Authority.STOCK_TAKE)) {
            newTake.setVisibility(View.GONE);
        }

        tabLevels.setOnClickListener(v -> selectTab(0));
        tabPurchases.setOnClickListener(v -> selectTab(1));
        tabTakes.setOnClickListener(v -> selectTab(2));

        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                stockQuery.setValue(s.toString().trim());
            }
        });

        view.findViewById(R.id.btn_scan_stock).setOnClickListener(v ->
                startActivityForResult(
                        new Intent(requireContext(), com.patechltd.salexfypos.ui.scan.ScanActivity.class),
                        REQ_SCAN));

        Transformations.switchMap(stockQuery, q -> repo.products.observeStockRows(q))
                .observe(getViewLifecycleOwner(), stockAdapter::submit);

        repo.products.observeLowStock().observe(getViewLifecycleOwner(), list ->
                onLowStockChanged(list == null ? 0 : list.size()));

        selectTab(0);
        reloadPurchases();
        reloadTakes();
    }

    private void selectTab(int tab) {        tabLevels.setBackgroundResource(tab == 0 ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
        tabPurchases.setBackgroundResource(tab == 1 ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
        tabTakes.setBackgroundResource(tab == 2 ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
        tabLevels.setTextColor(requireContext().getColor(tab == 0 ? R.color.white : R.color.text_secondary));
        tabPurchases.setTextColor(requireContext().getColor(tab == 1 ? R.color.white : R.color.text_secondary));
        tabTakes.setTextColor(requireContext().getColor(tab == 2 ? R.color.white : R.color.text_secondary));
        levelsPanel.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        purchasesPanel.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        takesPanel.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);
        if (tab == 1) reloadPurchases();
        if (tab == 2) reloadTakes();
    }

    private void reloadPurchases() {
        repo.run(() -> {
            java.util.Map<String, String> names = new java.util.HashMap<>();
            for (Supplier s : repo.suppliers.getSuppliers()) names.put(s.uid, s.name);
            handler.post(() -> purchaseAdapter.setSupplierNames(names));
        });
        repo.purchases.observePurchases().observe(getViewLifecycleOwner(), purchaseAdapter::submit);
    }

    private void reloadTakes() {
        repo.stock.observeStockTakes().observe(getViewLifecycleOwner(), takeAdapter::submit);
    }

    private void exportPurchases(final android.net.Uri uri) {
        repo.run(() -> {
            try {
                final java.util.List<com.patechltd.salexfypos.db.PurchaseWithItems> rows =
                        repo.purchases.getPurchasesWithItems();
                final java.util.Map<String, String> names = new java.util.HashMap<>();
                for (com.patechltd.salexfypos.db.entity.Supplier s : repo.suppliers.getSuppliers()) {
                    names.put(s.uid, s.name);
                }
                final boolean ok = com.patechltd.salexfypos.util.ExcelUtil.export(
                        requireContext(), uri, "Purchases", rows,
                        new com.patechltd.salexfypos.util.ExcelUtil.RowWriter() {
                            @Override public Object[] header() {
                                return new Object[]{"Invoice No", "Date", "Supplier", "Subtotal",
                                        "Total", "Paid", "Balance", "Items"};
                            }
                            @Override public Object[] row(Object item, int index) {
                                com.patechltd.salexfypos.db.PurchaseWithItems pw =
                                        (com.patechltd.salexfypos.db.PurchaseWithItems) item;
                                com.patechltd.salexfypos.db.entity.Purchase p = pw.purchase;
                                String supplier = p.supplierId == null ? "" : names.get(p.supplierId);
                                if (supplier == null) supplier = "";
                                String items = pw.items == null ? "0"
                                        : String.valueOf(pw.items.size());
                                return new Object[]{p.invoiceNo, com.patechltd.salexfypos.util.DateUtil.formatDate(p.purchaseDate),
                                        supplier, p.subtotal, p.total, p.paidAmount,
                                        com.patechltd.salexfypos.util.NumberUtil.round2(p.total - p.paidAmount),
                                        items};
                            }
                        });
                handler.post(() -> DialogUtil.toast(requireContext(),
                        ok ? "Purchases exported" : "Export failed"));
            } catch (Exception e) {
                handler.post(() -> DialogUtil.toast(requireContext(), "Export failed"));
            }
        });
    }

    private void openTake(StockTake t) {
        Intent i = new Intent(requireContext(), StockTakeActivity.class);
        i.putExtra("id", t.uid);
        startActivity(i);
    }

    private int lastAlertedLow = 0;
    private boolean notifiedPermRequested;

    private void onLowStockChanged(int count) {
        lowStockCount.setText(count + " low");
        if (count == 0) {
            lastAlertedLow = 0;
            return;
        }
        if (count > lastAlertedLow) {
            lastAlertedLow = count;
            com.patechltd.salexfypos.util.SoundUtil.beep();
            DialogUtil.toast(requireContext(),
                    count + " item" + (count == 1 ? " is" : "s are") + " running low on stock");
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
                    && androidx.core.content.ContextCompat.checkSelfPermission(
                    requireContext(), android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                if (!notifiedPermRequested) {
                    notifiedPermRequested = true;
                    requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 4001);
                }
            } else {
                com.patechltd.salexfypos.util.Notifier.lowStock(requireContext(), count);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_EXPORT_PURCHASES && resultCode == android.app.Activity.RESULT_OK
                && data != null && data.getData() != null) {
            exportPurchases(data.getData());
            return;
        }
        if (requestCode == REQ_SCAN && resultCode == android.app.Activity.RESULT_OK && data != null) {
            String code = data.getStringExtra(com.patechltd.salexfypos.ui.scan.ScanActivity.EXTRA_CODE);
            if (code != null) {
                EditText search = getView() == null ? null : getView().findViewById(R.id.stock_search);
                if (search != null) {
                    search.setText(code.trim());
                    stockQuery.setValue(code.trim());
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 4001 && lastAlertedLow > 0) {
            com.patechltd.salexfypos.util.Notifier.lowStock(requireContext(), lastAlertedLow);
        }
    }
}
