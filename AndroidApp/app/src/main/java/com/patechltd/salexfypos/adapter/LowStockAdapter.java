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

public class LowStockAdapter extends RecyclerView.Adapter<LowStockAdapter.VH> {

    public interface Listener {
        void onClick(StockRow row);
    }

    private final List<StockRow> items = new ArrayList<>();
    private final Listener listener;

    public LowStockAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<StockRow> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_low_stock, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        StockRow row = items.get(position);
        holder.name.setText(row.name);
        String bc = row.barcode == null || row.barcode.isEmpty() ? "No barcode" : row.barcode;
        holder.sub.setText(bc);
        holder.qty.setText(NumberUtil.qty(row.currentQty) + " " + (row.unitLabel == null ? "" : row.unitLabel));
        holder.reorder.setText("Reorder at " + NumberUtil.qty(row.reorderLevel));
        double shortage = Math.max(0, row.reorderLevel - row.currentQty);
        if (shortage > 0) {
            holder.shortage.setText("Short by " + NumberUtil.qty(shortage));
            holder.shortage.setVisibility(View.VISIBLE);
        } else {
            holder.shortage.setVisibility(View.GONE);
        }
        holder.itemView.setOnClickListener(v -> listener.onClick(row));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name, sub, qty, reorder, shortage;

        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.product_name);
            sub = itemView.findViewById(R.id.product_sub);
            qty = itemView.findViewById(R.id.qty);
            reorder = itemView.findViewById(R.id.reorder);
            shortage = itemView.findViewById(R.id.shortage);
        }
    }
}