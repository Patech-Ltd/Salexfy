package com.patechltd.salexfypos.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.StockRow;
import com.patechltd.salexfypos.util.NumberUtil;

import java.util.ArrayList;
import java.util.List;

public class AddStockAdapter extends RecyclerView.Adapter<AddStockAdapter.VH> {

    public interface OnRowClick {
        void onRowClick(StockRow row);
    }

    private final List<StockRow> items = new ArrayList<>();
    private final OnRowClick listener;

    public AddStockAdapter(OnRowClick listener) {
        this.listener = listener;
    }

    public void submit(List<StockRow> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_add_stock, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        final StockRow row = items.get(position);
        String unit = row.unitLabel == null ? "" : row.unitLabel;
        holder.name.setText(row.name);
        holder.current.setText("Current " + NumberUtil.qty(row.currentQty)
                + (unit.isEmpty() ? "" : " " + unit));
        holder.itemView.setOnClickListener(v -> listener.onRowClick(row));
        holder.btn.setOnClickListener(v -> listener.onRowClick(row));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name, current;
        final View btn;

        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.add_name);
            current = itemView.findViewById(R.id.add_current);
            btn = itemView.findViewById(R.id.btn_add_row);
        }
    }
}