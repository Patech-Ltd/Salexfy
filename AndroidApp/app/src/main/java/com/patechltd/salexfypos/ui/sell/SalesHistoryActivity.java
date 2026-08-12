package com.patechltd.salexfypos.ui.sell;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.adapter.KeyValueAdapter;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.SaleWithItems;
import com.patechltd.salexfypos.util.DateUtil;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.Prefs;

import java.util.ArrayList;
import java.util.List;

public class SalesHistoryActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<SaleWithItems> sales = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales_history);

        findViewById(R.id.toolbar).setOnClickListener(v -> onBackPressed());

        KeyValueAdapter adapter = new KeyValueAdapter();
        adapter.setListener(position -> {
            if (position < 0 || position >= sales.size()) return;
            Intent i = new Intent(this, SaleDetailActivity.class);
            i.putExtra("saleId", sales.get(position).sale.id);
            startActivity(i);
        });

        RecyclerView list = findViewById(R.id.sales_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        TextView emptyText = findViewById(R.id.empty_text);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Repository repo = Repository.get(this);
        RecyclerView list = findViewById(R.id.sales_list);
        load(repo, (KeyValueAdapter) list.getAdapter(), findViewById(R.id.empty_text));
    }

    private void load(Repository repo, KeyValueAdapter adapter, TextView emptyText) {
        repo.run(() -> {
            List<SaleWithItems> rows = repo.sales.getCompleteWithItemsBetween(0L, Long.MAX_VALUE);
            String currency = Prefs.currency(this);
            List<KeyValueAdapter.Row> out = new ArrayList<>();
            for (SaleWithItems sw : rows) {
                if (sw == null || sw.sale == null) continue;
                int count = sw.items == null ? 0 : sw.items.size();
                out.add(new KeyValueAdapter.Row(
                        "#" + sw.sale.saleNo,
                        DateUtil.formatDate(sw.sale.saleDate) + " " + DateUtil.formatTime(sw.sale.saleDate)
                                + " · " + count + " item" + (count == 1 ? "" : "s")
                                + " · " + (sw.sale.paymentMethod == null ? "CASH" : sw.sale.paymentMethod),
                        currency + " " + NumberUtil.money(sw.sale.total),
                        0xFF1565C0));
            }
            handler.post(() -> {
                sales.clear();
                sales.addAll(rows);
                adapter.submit(out);
                emptyText.setVisibility(sales.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }
}
