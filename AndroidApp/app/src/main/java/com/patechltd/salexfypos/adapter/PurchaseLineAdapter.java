package com.patechltd.salexfypos.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.entity.PurchaseItem;
import com.patechltd.salexfypos.util.NumberUtil;

import java.util.ArrayList;
import java.util.List;

public class PurchaseLineAdapter extends RecyclerView.Adapter<PurchaseLineAdapter.VH> {

    public interface Listener {
        void onClick(int position);

        void onRemove(int position);
    }

    private final List<PurchaseItem> items = new ArrayList<>();
    private final Listener listener;

    public PurchaseLineAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<PurchaseItem> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    public List<PurchaseItem> getItems() {
        return items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_purchase_line, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        PurchaseItem item = items.get(position);
        holder.name.setText(item.productName);
        holder.sub.setText(NumberUtil.qty(item.qty) + " " + item.unitLabel
                + " × " + NumberUtil.money(item.unitPrice));
        holder.lineTotal.setText(NumberUtil.money(item.lineTotal));
        holder.itemView.setOnClickListener(v -> listener.onClick(position));
        holder.remove.setOnClickListener(v -> listener.onRemove(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name, sub, lineTotal;
        final ImageButton remove;

        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.product_name);
            sub = itemView.findViewById(R.id.product_sub);
            lineTotal = itemView.findViewById(R.id.line_total);
            remove = itemView.findViewById(R.id.btn_remove);
        }
    }
}
