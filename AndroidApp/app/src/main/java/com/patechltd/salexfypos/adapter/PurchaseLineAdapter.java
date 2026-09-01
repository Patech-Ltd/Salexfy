package com.patechltd.salexfypos.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.entity.Product;
import com.patechltd.salexfypos.db.entity.PurchaseItem;
import com.patechltd.salexfypos.util.NumberUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PurchaseLineAdapter extends RecyclerView.Adapter<PurchaseLineAdapter.VH> {

    public interface Listener {
        void onClick(int position);

        void onRemove(int position);

        void onEditCost(int position);
    }

    private final List<PurchaseItem> items = new ArrayList<>();
    private final Map<String, Product> products = new HashMap<>();
    private final Listener listener;
    private boolean readOnly;
    private boolean showProfit = true;

    public PurchaseLineAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public void setShowProfit(boolean showProfit) {
        this.showProfit = showProfit;
    }

    public void setProducts(Map<String, Product> map) {
        products.clear();
        if (map != null) products.putAll(map);
        notifyDataSetChanged();
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
        holder.sub.setText(NumberUtil.qty(item.qty) + " " + safe(item.unitLabel)
                + " × " + NumberUtil.money(item.unitPrice));
        holder.lineTotal.setText(NumberUtil.money(item.lineTotal));
        holder.remove.setVisibility(readOnly ? View.GONE : View.VISIBLE);
        holder.cost.setVisibility(readOnly ? View.GONE : View.VISIBLE);
        holder.cost.setText(NumberUtil.money(item.unitPrice));
        holder.cost.setOnClickListener(v -> {
            if (!readOnly) listener.onEditCost(position);
        });
        Product p = item.productId == null ? null : products.get(item.productId);
        if (p == null || item.productId == null || !showProfit) {
            holder.profit.setVisibility(View.GONE);
        } else {
            holder.profit.setText(buildProfitText(item, p));
            holder.profit.setVisibility(View.VISIBLE);
            holder.profit.setTextColor(holder.profit.getResources().getColor(
                    profitOf(item, p) < 0 ? R.color.error : R.color.success));
        }
        holder.itemView.setOnClickListener(v -> {
            if (!readOnly) listener.onClick(position);
        });
        holder.remove.setOnClickListener(v -> listener.onRemove(position));
    }

    private String buildProfitText(PurchaseItem item, Product p) {
        int factor = factorOf(p);
        boolean wholesale = item.isWholesale && p.wholesalePrice > 0;
        double buyPer = wholesale ? item.unitPrice / factor : item.unitPrice;
        double sellPer = p.retailPrice > 0 ? p.retailPrice
                : (p.wholesalePrice > 0 ? p.wholesalePrice / factor : 0);
        String pieceUnit = p.retailUnit != null && !p.retailUnit.isEmpty() ? p.retailUnit : "Pcs";
        StringBuilder sb = new StringBuilder();
        if (wholesale) {
            sb.append("Buy ").append(NumberUtil.money(item.unitPrice)).append("/")
                    .append(safe(p.wholesaleUnit))
                    .append(" → ").append(NumberUtil.money(buyPer)).append("/").append(pieceUnit);
        } else {
            sb.append("Buy ").append(NumberUtil.money(item.unitPrice)).append("/").append(pieceUnit);
        }
        if (sellPer > 0) {
            double profit = sellPer - buyPer;
            sb.append("  •  Sell ").append(NumberUtil.money(sellPer)).append("/").append(pieceUnit)
                    .append("  •  Profit ").append(NumberUtil.money(profit)).append("/").append(pieceUnit);
        }
        return sb.toString();
    }

    private double profitOf(PurchaseItem item, Product p) {
        int factor = factorOf(p);
        boolean wholesale = item.isWholesale && p.wholesalePrice > 0;
        double buyPer = wholesale ? item.unitPrice / factor : item.unitPrice;
        double sellPer = p.retailPrice > 0 ? p.retailPrice
                : (p.wholesalePrice > 0 ? p.wholesalePrice / factor : 0);
        return sellPer - buyPer;
    }

    private int factorOf(Product p) {
        return p != null && p.wholesaleFactor > 0 ? p.wholesaleFactor : 1;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name, sub, lineTotal;
        final TextView profit;
        final TextView cost;
        final ImageButton remove;

        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.product_name);
            sub = itemView.findViewById(R.id.product_sub);
            lineTotal = itemView.findViewById(R.id.line_total);
            remove = itemView.findViewById(R.id.btn_remove);
            cost = itemView.findViewById(R.id.btn_cost);
            profit = itemView.findViewById(R.id.product_profit);
        }
    }
}
