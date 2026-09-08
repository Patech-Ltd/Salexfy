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

public class StockAdapter extends RecyclerView.Adapter<StockAdapter.VH> {

    private final List<StockRow> items = new ArrayList<>();

    public void submit(List<StockRow> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stock, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        StockRow row = items.get(position);
        holder.name.setText(row.name);
        String bc = row.barcode == null || row.barcode.isEmpty() ? "No barcode" : row.barcode;
        holder.barcode.setText(bc);
        holder.qty.setText(NumberUtil.qty(row.currentQty) + " " + (row.unitLabel == null ? "" : row.unitLabel));
        holder.reorder.setText("Reorder at " + NumberUtil.qty(row.reorderLevel));
        holder.qty.setTextColor(holder.itemView.getContext().getColor(
                row.currentQty <= row.reorderLevel ? R.color.error : R.color.amount_text));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name, barcode, qty, reorder;

        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.product_name);
            barcode = itemView.findViewById(R.id.product_barcode);
            qty = itemView.findViewById(R.id.qty);
            reorder = itemView.findViewById(R.id.reorder);
        }
    }
}
