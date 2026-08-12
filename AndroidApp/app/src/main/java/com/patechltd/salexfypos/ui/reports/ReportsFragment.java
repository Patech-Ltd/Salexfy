package com.patechltd.salexfypos.ui.reports;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.util.DateUtil;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.Prefs;

public class ReportsFragment extends Fragment {

    private Repository repo;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView salesView, profitView, countView, debtorsView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reports, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = Repository.get(requireContext());

        salesView = view.findViewById(R.id.today_sales);
        profitView = view.findViewById(R.id.today_profit);
        countView = view.findViewById(R.id.today_count);
        debtorsView = view.findViewById(R.id.today_debtors);

        view.findViewById(R.id.btn_sales_report).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ReportsActivity.class)));
        view.findViewById(R.id.btn_excel_quick).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ReportsActivity.class)
                        .putExtra("exportToday", true)));
        view.findViewById(R.id.btn_sales_history).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), com.patechltd.salexfypos.ui.sell.SalesHistoryActivity.class)));

        load();
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        long from = DateUtil.startOfDay(System.currentTimeMillis());
        long to = DateUtil.endOfDay(System.currentTimeMillis());
        String c = Prefs.currency(requireContext());
        repo.run(() -> {
            double sales = repo.sales.salesTotal(from, to);
            double cost = repo.sales.costOfSalesBetween(from, to);
            int count = repo.sales.saleCountBetween(from, to);
            double credit = repo.sales.creditSalesBetween(from, to);
            double payments = repo.suppliers.paymentsBetween(from, to);
            handler.post(() -> {
                salesView.setText(c + " " + NumberUtil.money(sales));
                profitView.setText(c + " " + NumberUtil.money(sales - cost));
                countView.setText(String.valueOf(count));
                debtorsView.setText(c + " " + NumberUtil.money(credit - payments));
            });
        });
    }
}
