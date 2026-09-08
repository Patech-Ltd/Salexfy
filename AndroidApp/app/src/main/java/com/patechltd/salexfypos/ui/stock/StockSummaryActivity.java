package com.patechltd.salexfypos.ui.stock;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.StockValuationSummary;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.Prefs;

public class StockSummaryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_summary);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        Repository repo = Repository.get(this);
        repo.products.observeValuationSummary().observe(this, this::renderSummary);
    }

    private void renderSummary(StockValuationSummary s) {
        if (s == null) return;
        String cur = Prefs.currency(this);
        ((TextView) findViewById(R.id.summary_products)).setText(NumberUtil.qty(s.products));
        ((TextView) findViewById(R.id.summary_qty)).setText(NumberUtil.qty(s.totalQty));
        ((TextView) findViewById(R.id.summary_assets)).setText(NumberUtil.money(s.assets, cur));
        ((TextView) findViewById(R.id.summary_retail_sales)).setText(NumberUtil.money(s.retailSales, cur));
        ((TextView) findViewById(R.id.summary_wholesale_sales)).setText(NumberUtil.money(s.wholesaleSales, cur));
        renderProfit((TextView) findViewById(R.id.summary_retail_profit), s.retailSales - s.assets, cur);
        renderProfit((TextView) findViewById(R.id.summary_wholesale_profit), s.wholesaleSales - s.assets, cur);
    }

    private void renderProfit(TextView view, double value, String currency) {
        view.setText(NumberUtil.money(value, currency));
        view.setTextColor(getColor(value < 0 ? R.color.error : R.color.success));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}