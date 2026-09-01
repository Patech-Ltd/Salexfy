package com.patechltd.salexfypos.ui.sell;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.SaleWithItems;
import com.patechltd.salexfypos.db.entity.Sale;
import com.patechltd.salexfypos.db.entity.SaleItem;
import com.patechltd.salexfypos.util.DateUtil;
import com.patechltd.salexfypos.util.DialogUtil;
import com.patechltd.salexfypos.util.NumberUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Full-page list of held (paused) transactions. Tap to recall, trash icon
 * to discard. Rows are loaded in pages straight from the database so the
 * whole held list is never held in memory.
 */
public class HeldSalesActivity extends AppCompatActivity {

    public static final String EXTRA_SALE_ID = "saleId";
    private static final int PAGE_SIZE = 30;

    private Repository repo;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<SaleWithItems> all = new ArrayList<>();
    private final List<SaleWithItems> visible = new ArrayList<>();
    private HeldAdapter adapter;
    private String query = "";
    private int page;
    private boolean loading;
    private boolean endReached;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_held_sales);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        repo = Repository.get(this);

        RecyclerView list = findViewById(R.id.held_list);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        adapter = new HeldAdapter();
        list.setLayoutManager(lm);
        list.setAdapter(adapter);
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0) return;
                int total = lm.getItemCount();
                int last = lm.findLastVisibleItemPosition();
                if (total > 0 && last >= total - 3) loadNextPage();
            }
        });

        EditText search = findViewById(R.id.held_search);
        search.addTextChangedListener(new TextWatcher() {
            private final Runnable debounce = () -> {
                query = search.getText() == null ? "" : search.getText().toString().trim();
                resetAndReload();
            };

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                handler.removeCallbacks(debounce);
                handler.postDelayed(debounce, 400);
            }
        });

        resetAndReload();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resetAndReload();
    }

    private void resetAndReload() {
        page = 0;
        endReached = false;
        all.clear();
        render();
        loadNextPage();
    }

    private void loadNextPage() {
        if (loading || endReached) return;
        loading = true;
        final int pageNo = page;
        repo.run(() -> {
            List<SaleWithItems> rows = repo.sales.getHeldPage(query, PAGE_SIZE, pageNo * PAGE_SIZE);
            handler.post(() -> {
                if (rows.size() < PAGE_SIZE) endReached = true;
                all.addAll(rows);
                page++;
                loading = false;
                render();
            });
        });
    }

    private void render() {
        visible.clear();
        visible.addAll(all);
        adapter.submit(visible);
        findViewById(R.id.empty_hint).setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void recall(Sale sale) {
        Intent result = new Intent();
        result.putExtra(EXTRA_SALE_ID, sale.uid);
        setResult(RESULT_OK, result);
        finish();
    }

    private void deleteHeld(Sale sale) {
        DialogUtil.confirm(this, "Discard transaction " + sale.saleNo + "?",
                "This cannot be undone.", () -> repo.run(() -> {
                    repo.sales.deleteItemsForSale(sale.uid);
                    repo.sales.deleteSale(sale.uid);
                    handler.post(() -> {
                        Toast.makeText(this, "Held transaction deleted", Toast.LENGTH_SHORT).show();
                        resetAndReload();
                    });
                }));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private class HeldAdapter extends RecyclerView.Adapter<HeldAdapter.VH> {

        private final List<SaleWithItems> rows = new ArrayList<>();

        void submit(List<SaleWithItems> list) {
            rows.clear();
            rows.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_held, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            final SaleWithItems sw = rows.get(position);
            final Sale s = sw.sale;
            holder.no.setText("Trans #" + s.saleNo);
            holder.time.setText(DateUtil.formatDate(s.createdAt) + " " + DateUtil.formatTime(s.createdAt));
            holder.items.setText((sw.items == null ? 0 : sw.items.size()) + " item(s) • "
                    + itemNames(sw));
            holder.total.setText(NumberUtil.money(s.total));
            if (s.notes != null && !s.notes.isEmpty()) {
                holder.note.setVisibility(View.VISIBLE);
                holder.note.setText("Note: " + s.notes);
            } else {
                holder.note.setVisibility(View.GONE);
            }
            holder.itemView.setOnClickListener(v -> recall(s));
            holder.delete.setOnClickListener(v -> deleteHeld(s));
        }

        @Override
        public int getItemCount() {
            return visible.size();
        }

        private String itemNames(SaleWithItems sw) {
            if (sw.items == null || sw.items.isEmpty()) return "";
            StringBuilder sb = new StringBuilder();
            int shown = 0;
            for (SaleItem it : sw.items) {
                if (shown++ == 2) break;
                if (sb.length() > 0) sb.append(", ");
                sb.append(it.productName);
            }
            if (sw.items.size() > 3) sb.append(" +").append(sw.items.size() - 3).append(" more");
            return sb.toString();
        }

        class VH extends RecyclerView.ViewHolder {
            final TextView no, time, items, note, total;
            final View delete;

            VH(@NonNull View itemView) {
                super(itemView);
                no = itemView.findViewById(R.id.held_no);
                time = itemView.findViewById(R.id.held_time);
                items = itemView.findViewById(R.id.held_items);
                note = itemView.findViewById(R.id.held_note);
                total = itemView.findViewById(R.id.held_total);
                delete = itemView.findViewById(R.id.btn_held_delete);
            }
        }
    }
}