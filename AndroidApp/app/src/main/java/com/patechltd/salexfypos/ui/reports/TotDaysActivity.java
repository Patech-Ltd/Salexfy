package com.patechltd.salexfypos.ui.reports;

import android.content.Intent;
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
import com.patechltd.salexfypos.db.DayReportRow;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.util.DateUtil;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.Prefs;
import com.patechltd.salexfypos.util.TaxUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Day-by-day TOT breakdown for a single month.
 */
public class TotDaysActivity extends AppCompatActivity {

    public static final String EXTRA_MONTH_START = "monthStart";
    public static final String EXTRA_FROM = "from";
    public static final String EXTRA_TO = "to";
    public static final String EXTRA_TITLE = "title";

    private final List<DayReportRow> days = new ArrayList<>();
    private KeyValueAdapter adapter;
    private long from, to;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tot_days);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        Intent intent = getIntent();
        long monthStart = intent.getLongExtra(EXTRA_MONTH_START, 0);
        if (intent.hasExtra(EXTRA_FROM) && intent.hasExtra(EXTRA_TO)) {
            from = intent.getLongExtra(EXTRA_FROM, DateUtil.startOfMonth(System.currentTimeMillis()));
            to = intent.getLongExtra(EXTRA_TO, DateUtil.endOfMonth(from));
        } else {
            if (monthStart == 0) monthStart = DateUtil.startOfMonth(System.currentTimeMillis());
            from = DateUtil.startOfMonth(monthStart);
            to = DateUtil.endOfMonth(from);
        }
        String title = intent.getStringExtra(EXTRA_TITLE);
        if (title == null || title.isEmpty()) {
            title = DateUtil.monthName(from) + " · TOT (1.5% of sales)";
        }
        ((TextView) findViewById(R.id.days_subtitle)).setText(title);

        adapter = new KeyValueAdapter();
        RecyclerView list = findViewById(R.id.days_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        TextView emptyHint = findViewById(R.id.empty_hint);
        String currency = Prefs.currency(this);
        Repository repo = Repository.get(this);
        repo.run(() -> {
            List<DayReportRow> rows = repo.sales.getDailyReport(from, to);
            final List<DayReportRow> days = fillAllDays(rows);
            runOnUiThread(() -> {
                this.days.clear();
                this.days.addAll(days);
                emptyHint.setVisibility(this.days.isEmpty() ? View.VISIBLE : View.GONE);

                List<KeyValueAdapter.Row> out = new ArrayList<>();
                for (DayReportRow d : this.days) {
                    out.add(new KeyValueAdapter.Row(
                            DateUtil.formatDate(DateUtil.startOfDay(d.dayStart)),
                            d.saleCount + (d.saleCount == 1 ? " sale" : " sales")
                                    + " · Profit " + currency + " " + NumberUtil.money(d.profit),
                            "TOT " + currency + " " + NumberUtil.money(TaxUtil.tot(d.subtotal)),
                            0xFFB00020));
                }
                adapter.submit(out);
            });
        });
    }

    /**
     * Expands the (sparse) daily report into one row per calendar day in the
     * covered range, so every day of the month shows up even when there were
     * no sales.
     */
    private List<DayReportRow> fillAllDays(List<DayReportRow> rows) {
        Map<Long, DayReportRow> byDay = new HashMap<>();
        if (rows != null) {
            for (DayReportRow r : rows) byDay.put(DateUtil.startOfDay(r.dayStart), r);
        }
        List<DayReportRow> all = new ArrayList<>();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(from);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long dayStart = DateUtil.startOfDay(from);
        long lastStart = DateUtil.startOfDay(to);
        int guard = 0;
        while (dayStart <= lastStart && guard < 366) {
            DayReportRow row = byDay.get(dayStart);
            if (row == null) {
                row = new DayReportRow();
                row.dayStart = dayStart;
            }
            all.add(row);
            c.add(Calendar.DAY_OF_MONTH, 1);
            dayStart = c.getTimeInMillis();
            guard++;
        }
        return all;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}