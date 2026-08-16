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

import java.util.List;

public class StockFragment extends Fragment {

    private static final int REQ_SCAN = 6001;
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
        EditText search = view.findViewById(R.id.stock_search);

        RecyclerView stockList = view.findViewById(R.id.stock_list);
        stockAdapter = new StockAdapter();
        stockList.setLayoutManager(new LinearLayoutManager(requireContext()));
        stockList.setAdapter(stockAdapter);

        RecyclerView purchasesList = view.findViewById(R.id.purchases_list);
        purchaseAdapter = new PurchaseAdapter(p -> { });
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
                lowStockCount.setText((list == null ? 0 : list.size()) + " low"));

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

    private void openTake(StockTake t) {
        Intent i = new Intent(requireContext(), StockTakeActivity.class);
        i.putExtra("id", t.uid);
        startActivity(i);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
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
}
