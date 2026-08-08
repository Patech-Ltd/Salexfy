package com.patechltd.salexfypos.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.entity.SaleItem;
import com.patechltd.salexfypos.util.NumberUtil;

import java.util.ArrayList;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.VH> {

    public interface Listener {
        void onPlus(int position);

        void onMinus(int position);

        void onRemove(int position);

        void onClick(int position);
    }

    private final List<SaleItem> items = new ArrayList<>();
    private final Listener listener;

    public CartAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<SaleItem> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    public List<SaleItem> getItems() {
        return items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        SaleItem item = items.get(position);
        holder.name.setText(item.productName);
        String unit = item.isWholesale ? "(Wholesale)" : "(Retail)";
        String barcode = item.barcode == null || item.barcode.isEmpty() ? "" : " • " + item.barcode;
        holder.sub.setText(unit + barcode);
        holder.qty.setText(NumberUtil.qty(item.qty));
        holder.lineTotal.setText(NumberUtil.money(item.lineTotal));
        holder.unitPrice.setText("@" + NumberUtil.money(item.unitPrice));
        holder.plus.setOnClickListener(v -> listener.onPlus(position));
        holder.minus.setOnClickListener(v -> listener.onMinus(position));
        holder.remove.setOnClickListener(v -> listener.onRemove(position));
        holder.itemView.setOnClickListener(v -> listener.onClick(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name, sub, qty, lineTotal, unitPrice;
        final View minus, plus, remove;

        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.product_name);
            sub = itemView.findViewById(R.id.product_sub);
            qty = itemView.findViewById(R.id.qty);
            lineTotal = itemView.findViewById(R.id.line_total);
            unitPrice = itemView.findViewById(R.id.unit_price);
            minus = itemView.findViewById(R.id.btn_minus);
            plus = itemView.findViewById(R.id.btn_plus);
            remove = itemView.findViewById(R.id.btn_remove);
        }
    }
}
