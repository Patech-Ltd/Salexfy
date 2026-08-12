package com.patechltd.salexfypos.ui.sell;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.SaleWithItems;
import com.patechltd.salexfypos.db.entity.Sale;
import com.patechltd.salexfypos.db.entity.SaleItem;
import com.patechltd.salexfypos.util.DateUtil;
import com.patechltd.salexfypos.util.NumberUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HeldListDialog {

    public interface Callback {
        void onRecall(Sale sale);

        void onDelete(Sale sale);
    }

    private final Context context;
    private final List<SaleWithItems> held;
    private final Callback callback;

    private String query = "";

    public HeldListDialog(Context context, List<SaleWithItems> held, Callback callback) {
        this.context = context;
        this.held = held;
        this.callback = callback;
    }

    public void show() {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_held_list, null);
        EditText search = view.findViewById(R.id.held_search);
        RecyclerView list = view.findViewById(R.id.held_list);
        list.setLayoutManager(new LinearLayoutManager(context));

        HeldAdapter adapter = new HeldAdapter();
        list.setAdapter(adapter);

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                query = s.toString().trim().toLowerCase(Locale.ROOT);
                adapter.refresh();
            }
        });

        new MaterialAlertDialogBuilder(context)
                .setTitle("Held transactions (" + held.size() + ")")
                .setView(view)
                .setNegativeButton("Close", null)
                .show();
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

    private List<SaleWithItems> matches() {
        List<SaleWithItems> out = new ArrayList<>();
        for (SaleWithItems sw : held) {
            Sale s = sw.sale;
            if (query.isEmpty()) {
                out.add(sw);
                continue;
            }
            boolean hit = (s.saleNo != null && s.saleNo.toLowerCase(Locale.ROOT).contains(query));
            if (!hit && (s.notes != null && s.notes.toLowerCase(Locale.ROOT).contains(query))) hit = true;
            if (!hit) {
                for (SaleItem it : sw.items) {
                    if (it.productName != null && it.productName.toLowerCase(Locale.ROOT).contains(query)) {
                        hit = true;
                        break;
                    }
                }
            }
            if (hit) out.add(sw);
        }
        return out;
    }

    private class HeldAdapter extends RecyclerView.Adapter<HeldAdapter.VH> {

        private final List<SaleWithItems> items = new ArrayList<>();

        HeldAdapter() {
            refresh();
        }

        void refresh() {
            items.clear();
            items.addAll(matches());
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
            final SaleWithItems sw = items.get(position);
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
            holder.itemView.setOnClickListener(v -> {
                if (callback != null) callback.onRecall(s);
            });
            holder.delete.setOnClickListener(v -> {
                if (callback != null) callback.onDelete(s);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
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
