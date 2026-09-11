package com.patechltd.salexfypos.ui.stock;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Transformations;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.adapter.AddStockAdapter;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.StockRow;

/**
 * Choose a product, then land on the Add/Remove stock page. Every change is a
 * logged stock movement, never a blind overwrite.
 */
public class AddStockActivity extends AppCompatActivity {

    private final androidx.lifecycle.MutableLiveData<String> query =
            new androidx.lifecycle.MutableLiveData<>("");

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_stock);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        Repository repo = Repository.get(this);

        EditText search = findViewById(R.id.stock_search);
        RecyclerView list = findViewById(R.id.product_list);
        AddStockAdapter adapter = new AddStockAdapter(row -> {
            Intent i = new Intent(this, AddStockAdjustActivity.class);
            i.putExtra(AddStockAdjustActivity.EXTRA_PRODUCT_ID, row.productId);
            startActivity(i);
        });
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(android.text.Editable s) {
                query.setValue(s == null ? "" : s.toString().trim());
            }
        });

        Transformations.switchMap(query, q -> repo.products.observeStockRows(q))
                .observe(this, adapter::submit);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}