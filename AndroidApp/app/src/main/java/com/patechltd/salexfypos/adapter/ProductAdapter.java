package com.patechltd.salexfypos.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.ProductStock;
import com.patechltd.salexfypos.util.NumberUtil;

import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.VH> {

    public interface Listener {
        void onClick(ProductStock item);
    }

    private final List<ProductStock> items = new ArrayList<>();
    private final Listener listener;

    public ProductAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<ProductStock> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ProductStock ps = items.get(position);
        holder.name.setText(ps.product.name);
        StringBuilder meta = new StringBuilder();
        if (ps.brandName != null && !ps.brandName.isEmpty() && !"No Brand".equals(ps.brandName)) {
            meta.append(ps.brandName);
        }
        if (ps.categoryName != null && !ps.categoryName.isEmpty()) {
            if (meta.length() > 0) meta.append(" • ");
            meta.append(ps.categoryName);
        }
        holder.meta.setText(meta.length() == 0 ? "Uncategorised" : meta.toString());
        holder.barcode.setText(ps.product.barcode == null || ps.product.barcode.isEmpty()
                ? "No barcode" : "Barcode: " + ps.product.barcode);
        String unit = ps.product.retailUnit == null ? "" : " / " + ps.product.retailUnit;
        holder.price.setText(NumberUtil.money(ps.product.retailPrice) + unit);
        holder.stock.setText("Stock: " + NumberUtil.qty(ps.currentQty));
        holder.lowStock.setVisibility(ps.currentQty <= ps.product.reorderLevel ? View.VISIBLE : View.GONE);
        holder.itemView.setOnClickListener(v -> listener.onClick(ps));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name, meta, barcode, price, stock, lowStock;

        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.product_name);
            meta = itemView.findViewById(R.id.product_meta);
            barcode = itemView.findViewById(R.id.product_barcode);
            price = itemView.findViewById(R.id.retail_price);
            stock = itemView.findViewById(R.id.stock_qty);
            lowStock = itemView.findViewById(R.id.low_stock);
        }
    }
}
