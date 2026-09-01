package com.patechltd.salexfypos.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.util.NumberUtil;

import java.util.ArrayList;
import java.util.List;

public class StockTakeLineAdapter extends RecyclerView.Adapter<StockTakeLineAdapter.VH> {

    public interface Listener {
        void onClick(int position);
    }

    public static class Line {
        public String productId;
        public String name;
        public double systemQty;
        public double countedQty;
        public String unit;
    }

    private final List<Line> items = new ArrayList<>();
    private final Listener listener;
    private boolean readOnly;

    public StockTakeLineAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        notifyDataSetChanged();
    }

    public void submit(List<Line> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    public List<Line> getItems() {
        return items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stock_take_line, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Line line = items.get(position);
        holder.name.setText(line.name);
        holder.sub.setText("Unit: " + (line.unit == null ? "" : line.unit));
        holder.counted.setText("Counted: " + NumberUtil.qty(line.countedQty));
        holder.system.setText("System: " + NumberUtil.qty(line.systemQty));
        holder.itemView.setOnClickListener(readOnly ? null : v -> listener.onClick(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name, sub, counted, system;

        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.product_name);
            sub = itemView.findViewById(R.id.product_sub);
            counted = itemView.findViewById(R.id.counted);
            system = itemView.findViewById(R.id.system);
        }
    }
}
