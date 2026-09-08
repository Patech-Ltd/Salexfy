package com.patechltd.salexfypos.ui.reports;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.adapter.KeyValueAdapter;
import com.patechltd.salexfypos.db.MonthReportRow;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.util.DateUtil;
import com.patechltd.salexfypos.util.DialogUtil;
import com.patechltd.salexfypos.util.ExcelUtil;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.Prefs;
import com.patechltd.salexfypos.util.StorageUtil;
import com.patechltd.salexfypos.util.TaxUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Monthly turnover tax (TOT) statement: summary plus one row per month.
 * Tap a month for the daily statement with Excel / PDF export.
 */
public class TOTActivity extends AppCompatActivity {

    private Repository repo;
    private final List<MonthReportRow> months = new ArrayList<>();
    private KeyValueAdapter adapter;
    private TextView summarySales, summaryProfit, summaryTot, summaryNet, summaryMonths;
    private TextView mtdSales, mtdProfit, mtdTot;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tot);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        repo = Repository.get(this);

        summarySales = findViewById(R.id.summary_sales);
        summaryProfit = findViewById(R.id.summary_profit);
        summaryTot = findViewById(R.id.summary_tot);
        summaryNet = findViewById(R.id.summary_net);
        summaryMonths = findViewById(R.id.summary_months);
        mtdSales = findViewById(R.id.mtd_sales);
        mtdProfit = findViewById(R.id.mtd_profit);
        mtdTot = findViewById(R.id.mtd_tot);

        adapter = new KeyValueAdapter();
        adapter.setListener(position -> {
            if (position < 0 || position >= months.size()) return;
            Intent i = new Intent(this, TotMonthDetailActivity.class);
            i.putExtra(TotMonthDetailActivity.EXTRA_MONTH_START, months.get(position).monthStart);
            startActivity(i);
        });

        RecyclerView list = findViewById(R.id.months_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        findViewById(R.id.btn_export_excel).setOnClickListener(v ->
                StorageUtil.createReportUri(this, "tot_months_" + DateUtil.formatDate(System.currentTimeMillis())));

        load();
    }

    private void load() {
        String currency = Prefs.currency(this);
        long now = System.currentTimeMillis();
        final long monthFrom = DateUtil.startOfMonth(now);
        final long monthTo = DateUtil.endOfDay(now);
        repo.run(() -> {
            List<MonthReportRow> rows = repo.sales.getMonthlySummary();
            double sales = 0, profit = 0, tot = 0;
            for (MonthReportRow m : rows) {
                sales += m.totalSales;
                profit += m.profit;
                tot += TaxUtil.tot(m.subtotal);
            }
            final double fSales = sales, fProfit = profit, fTot = tot;
            final List<MonthReportRow> copy = new ArrayList<>(rows);
            final double mtdSubtotal = repo.sales.subtotalTotal(monthFrom, monthTo);
            final double mtdSalesAmount = repo.sales.salesTotal(monthFrom, monthTo);
            final double mtdCost = repo.sales.costOfSalesBetween(monthFrom, monthTo);
            runOnUiThread(() -> {
                months.clear();
                months.addAll(copy);
                summarySales.setText(currency + " " + NumberUtil.money(fSales));
                summaryProfit.setText(currency + " " + NumberUtil.money(fProfit));
                summaryTot.setText(currency + " " + NumberUtil.money(fTot));
                summaryNet.setText(currency + " " + NumberUtil.money(fProfit - fTot));
                summaryMonths.setText(months.size() + (months.size() == 1 ? " month" : " months")
                        + " with sales");
                mtdSales.setText(currency + " " + NumberUtil.money(mtdSalesAmount));
                mtdProfit.setText(currency + " " + NumberUtil.money(mtdSalesAmount - mtdCost));
                mtdTot.setText(currency + " " + NumberUtil.money(TaxUtil.tot(mtdSubtotal)));
                findViewById(R.id.empty_hint).setVisibility(months.isEmpty() ? View.VISIBLE : View.GONE);

                List<KeyValueAdapter.Row> out = new ArrayList<>();
                int negColor = getResources().getColor(R.color.accent_negative);
                for (MonthReportRow m : months) {
                    out.add(new KeyValueAdapter.Row(
                            DateUtil.monthName(m.monthStart),
                            m.saleCount + (m.saleCount == 1 ? " sale" : " sales")
                                    + " · Profit " + currency + " " + NumberUtil.money(m.profit),
                            "TOT " + currency + " " + NumberUtil.money(TaxUtil.tot(m.subtotal)),
                            negColor));
                }
                if (out.isEmpty()) out.add(new KeyValueAdapter.Row("No sales", "recorded yet", "", 0));
                adapter.submit(out);
            });
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == StorageUtil.REQ_CREATE_REPORT && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            Uri uri = data.getData();
            repo.run(() -> {
                List<MonthReportRow> list = repo.sales.getMonthlySummary();
                List<ExcelUtil.Section> sections = new ArrayList<>();
                sections.add(new ExcelUtil.Section("Monthly TOT", list, new ExcelUtil.RowWriter() {
                    @Override public Object[] header() {
                        return new Object[]{"Month", "Transactions", "Total Sales", "Cost",
                                "Profit", "TOT (1.5%)", "Net After TOT"};
                    }
                    @Override public Object[] row(Object item, int index) {
                        MonthReportRow m = (MonthReportRow) item;
                        double tot = TaxUtil.tot(m.subtotal);
                        return new Object[]{DateUtil.monthName(m.monthStart), m.saleCount,
                                m.totalSales, m.totalCost, m.profit, tot, m.profit - tot};
                    }
                }));
                boolean ok = ExcelUtil.exportMulti(this, uri, sections);
                runOnUiThread(() -> DialogUtil.toast(this, ok ? "Report exported" : "Export failed"));
            });
        }
    }
}