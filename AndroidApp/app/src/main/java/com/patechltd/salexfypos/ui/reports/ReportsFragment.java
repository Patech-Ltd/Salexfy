package com.patechltd.salexfypos.ui.reports;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.PaymentMethodTotalRow;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.sync.SyncEvents;
import com.patechltd.salexfypos.util.DateUtil;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.Prefs;

import java.util.List;

public class ReportsFragment extends Fragment {

    private Repository repo;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable syncListener = () -> handler.post(this::load);
    private TextView salesView, profitView, countView, debtorsView;
    private android.widget.LinearLayout paymentsContainer;

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
        paymentsContainer = view.findViewById(R.id.payments_container);

        view.findViewById(R.id.btn_manual_sale).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), com.patechltd.salexfypos.ui.sell.ManualSaleActivity.class)));
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
        SyncEvents.addListener(syncListener);
        load();
    }

    @Override
    public void onPause() {
        super.onPause();
        SyncEvents.removeListener(syncListener);
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
            final List<com.patechltd.salexfypos.db.PaymentMethodTotalRow> pmts =
                    repo.sales.paymentTotalsBetween(from, to);
            handler.post(() -> {
                salesView.setText(c + " " + NumberUtil.money(sales));
                profitView.setText(c + " " + NumberUtil.money(sales - cost));
                countView.setText(String.valueOf(count));
                debtorsView.setText(c + " " + NumberUtil.money(credit - payments));
                renderPayments(pmts, c);
            });
        });
    }

    private void renderPayments(List<PaymentMethodTotalRow> pmts, String currency) {
        paymentsContainer.removeAllViews();
        if (pmts.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("No payments today");
            empty.setTextColor(requireContext().getResources().getColor(R.color.text_secondary));
            empty.setTextSize(13);
            paymentsContainer.addView(empty);
            return;
        }
        for (com.patechltd.salexfypos.db.PaymentMethodTotalRow pm : pmts) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setBackgroundResource(R.drawable.bg_card);
            int pad = (int) (requireContext().getResources().getDisplayMetrics().density * 14);
            row.setPadding(pad, pad, pad, pad);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(6);
            row.setLayoutParams(lp);

            TextView name = new TextView(requireContext());
            name.setText(com.patechltd.salexfypos.model.PaymentMethod.labelOf(pm.method));
            name.setTextColor(requireContext().getResources().getColor(R.color.text_primary));
            name.setTextSize(15);
            name.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            row.addView(name);

            TextView amount = new TextView(requireContext());
            amount.setText(currency + " " + NumberUtil.money(pm.total));
            amount.setTextColor(requireContext().getResources().getColor(R.color.brand_primary_dark));
            amount.setTextSize(16);
            amount.setTypeface(amount.getTypeface(), android.graphics.Typeface.BOLD);
            row.addView(amount);

            paymentsContainer.addView(row);
        }
    }

    private int dp(int v) {
        return Math.round(requireContext().getResources().getDisplayMetrics().density * v);
    }
}
