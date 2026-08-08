package com.patechltd.salexfypos.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.entity.Purchase;
import com.patechltd.salexfypos.util.DateUtil;
import com.patechltd.salexfypos.util.NumberUtil;

import java.util.ArrayList;
import java.util.List;

public class PurchaseAdapter extends RecyclerView.Adapter<PurchaseAdapter.VH> {

    public interface Listener {
        void onClick(Purchase purchase);
    }

    private final List<Purchase> items = new ArrayList<>();
    private final Listener listener;
    private final java.util.Map<String, String> supplierNames = new java.util.HashMap<>();

    public PurchaseAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setSupplierNames(java.util.Map<String, String> names) {
        supplierNames.clear();
        supplierNames.putAll(names);
    }

    public void submit(List<Purchase> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_purchase, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Purchase p = items.get(position);
        holder.invoiceNo.setText(p.invoiceNo);
        holder.date.setText(DateUtil.format(p.purchaseDate));
        holder.supplier.setText("Supplier: " + (p.supplierId == null ? "—" : supplierNames.getOrDefault(p.supplierId, "—")));
        holder.total.setText(NumberUtil.money(p.total));
        holder.status.setText("COMPLETE".equals(p.status) ? "Complete" : p.status);
        holder.itemView.setOnClickListener(v -> listener.onClick(p));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView invoiceNo, date, supplier, total, status;

        VH(@NonNull View itemView) {
            super(itemView);
            invoiceNo = itemView.findViewById(R.id.invoice_no);
            date = itemView.findViewById(R.id.date);
            supplier = itemView.findViewById(R.id.supplier);
            total = itemView.findViewById(R.id.total);
            status = itemView.findViewById(R.id.status);
        }
    }
}
