package com.patechltd.salexfypos.ui.reports;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.DayReportRow;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.util.DateUtil;
import com.patechltd.salexfypos.util.DialogUtil;
import com.patechltd.salexfypos.util.ExcelUtil;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.PdfUtil;
import com.patechltd.salexfypos.util.Prefs;
import com.patechltd.salexfypos.util.StorageUtil;
import com.patechltd.salexfypos.util.TaxUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Summary for a single month with total sales, profit, TOT (1.5%), a link
 * to the day-by-day breakdown and Excel / PDF export.
 */
public class TotMonthDetailActivity extends AppCompatActivity {

    public static final String EXTRA_MONTH_START = "monthStart";

    private Repository repo;
    private long from, to;
    private int pendingExport;
    private final List<DayReportRow> days = new ArrayList<>();
    private double sales, subtotal, cost, profit;
    private TextView salesTv, costTv, profitTv, totTv, netTv, countTv;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tot_month);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        repo = Repository.get(this);
        long monthStart = getIntent().getLongExtra(EXTRA_MONTH_START,
                DateUtil.startOfMonth(System.currentTimeMillis()));
        from = DateUtil.startOfMonth(monthStart);
        to = DateUtil.endOfMonth(from);

        ((TextView) findViewById(R.id.month_title)).setText(DateUtil.monthName(from));
        salesTv = findViewById(R.id.month_sales);
        costTv = findViewById(R.id.month_cost);
        profitTv = findViewById(R.id.month_profit);
        totTv = findViewById(R.id.month_tot);
        netTv = findViewById(R.id.month_net);
        countTv = findViewById(R.id.month_count);

        findViewById(R.id.btn_view_days).setOnClickListener(v -> {
            Intent i = new Intent(this, TotDaysActivity.class);
            i.putExtra(TotDaysActivity.EXTRA_MONTH_START, from);
            startActivity(i);
        });

        findViewById(R.id.btn_export_excel).setOnClickListener(v -> {
            pendingExport = 0;
            StorageUtil.createReportUri(this, "tot_" + DateUtil.formatDate(from));
        });
        findViewById(R.id.btn_export_pdf).setOnClickListener(v -> {
            pendingExport = 1;
            StorageUtil.createReportUri(this, "application/pdf", "pdf", "tot_" + DateUtil.formatDate(from));
        });

        load();
    }

    private void load() {
        String currency = Prefs.currency(this);
        repo.run(() -> {
            double salesVal = repo.sales.salesTotal(from, to);
            double subVal = repo.sales.subtotalTotal(from, to);
            double costVal = repo.sales.costOfSalesBetween(from, to);
            int countVal = repo.sales.saleCountBetween(from, to);
            List<DayReportRow> rows = repo.sales.getDailyReport(from, to);
            runOnUiThread(() -> {
                sales = salesVal;
                subtotal = subVal;
                cost = costVal;
                profit = sales - cost;
                days.clear();
                if (rows != null) days.addAll(rows);
                double tot = TaxUtil.tot(subtotal);
                salesTv.setText(currency + " " + NumberUtil.money(sales));
                costTv.setText(currency + " " + NumberUtil.money(cost));
                profitTv.setText(currency + " " + NumberUtil.money(profit));
                totTv.setText(currency + " " + NumberUtil.money(tot));
                netTv.setText(currency + " " + NumberUtil.money(profit - tot));
                countTv.setText(String.valueOf(countVal));
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
            final boolean isPdf = pendingExport == 1;
            repo.run(() -> {
                boolean ok = isPdf ? exportPdf(uri) : exportExcel(uri);
                runOnUiThread(() -> DialogUtil.toast(this, ok ? "Statement exported" : "Export failed"));
            });
        }
    }

    private boolean exportExcel(Uri uri) {
        List<ExcelUtil.Section> sections = new ArrayList<>();
        final Object[] summary = {DateUtil.monthName(from), countSales(), sales, cost, profit,
                TaxUtil.tot(subtotal), profit - TaxUtil.tot(subtotal)};
        sections.add(new ExcelUtil.Section("Summary", singleton(), new ExcelUtil.RowWriter() {
            @Override public Object[] header() {
                return new Object[]{"Month", "Transactions", "Total Sales", "Cost",
                        "Profit", "TOT (1.5%)", "Net After TOT"};
            }
            @Override public Object[] row(Object item, int index) {
                return summary;
            }
        }));
        sections.add(new ExcelUtil.Section("Daily Statement", rows(), new ExcelUtil.RowWriter() {
            @Override public Object[] header() {
                return new Object[]{"Date", "Sales", "Cost", "Profit", "TOT (1.5%)",
                        "Items", "Transactions"};
            }
            @Override public Object[] row(Object o, int index) {
                DayReportRow d = (DayReportRow) o;
                return new Object[]{DateUtil.formatDate(DateUtil.startOfDay(d.dayStart)), d.totalSales, d.totalCost,
                        d.profit, TaxUtil.tot(d.subtotal), d.itemCount, d.saleCount};
            }
        }));
        return ExcelUtil.exportMulti(this, uri, sections);
    }

    private boolean exportPdf(Uri uri) {
        String shop = Prefs.getString(this, Prefs.KEY_SHOP_NAME, "");
        String currency = Prefs.currency(this);
        List<String> lines = new ArrayList<>();
        lines.add(shop == null || shop.isEmpty() ? "Shop statement" : shop);
        lines.add("TOT statement · " + DateUtil.monthName(from));
        lines.add("Turnover tax at 1.5% of total sales");
        lines.add("");
        lines.add("// Monthly summary");
        lines.add("Total sales: ".concat(pad(currency + " " + NumberUtil.money(sales), 20)));
        lines.add("Total cost : ".concat(pad(currency + " " + NumberUtil.money(cost), 20)));
        lines.add("Profit     : ".concat(pad(currency + " " + NumberUtil.money(profit), 20)));
        lines.add("TOT due    : ".concat(pad(currency + " " + NumberUtil.money(TaxUtil.tot(subtotal)), 20)));
        lines.add("Net after TOT: ".concat(pad(currency + " " + NumberUtil.money(profit - TaxUtil.tot(subtotal)), 20)));
        lines.add("Transactions: ".concat(String.valueOf(countSales())));
        lines.add("");
        lines.add("// Daily statement");
        for (DayReportRow d : days) {
            StringBuilder sb = new StringBuilder();
            sb.append(pad(DateUtil.formatDate(DateUtil.startOfDay(d.dayStart)), 13));
            sb.append(pad(d.saleCount + " sales", 10));
            sb.append(pad(currency + " " + NumberUtil.money(d.totalSales), 16));
            sb.append("TOT ").append(NumberUtil.money(TaxUtil.tot(d.subtotal)));
            lines.add(sb.toString());
        }
        if (days.isEmpty()) lines.add("No sales in this month.");
        lines.add("");
        lines.add("// Month totals");
        lines.add("Total sales: ".concat(currency + " " + NumberUtil.money(sales)));
        lines.add("Profit     : ".concat(currency + " " + NumberUtil.money(profit)));
        lines.add("TOT due    : ".concat(currency + " " + NumberUtil.money(TaxUtil.tot(subtotal))));
        return PdfUtil.exportText(this, uri, "TOT Statement", lines);
    }

    private int countSales() {
        int n = 0;
        for (DayReportRow d : days) n += d.saleCount;
        return n;
    }

    private List<Object> singleton() {
        List<Object> one = new ArrayList<>();
        one.add(new Object());
        return one;
    }

    private List<DayReportRow> rows() {
        return Collections.unmodifiableList(days);
    }

    private static String pad(String s, int width) {
        if (s == null) s = "";
        if (s.length() > width) return s.substring(0, width);
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < width) sb.append(' ');
        return sb.toString();
    }
}