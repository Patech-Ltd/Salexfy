package com.patechltd.salexfypos.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.entity.StockMovement;
import com.patechltd.salexfypos.model.MovementType;
import com.patechltd.salexfypos.util.DateUtil;
import com.patechltd.salexfypos.util.NumberUtil;

import java.util.ArrayList;
import java.util.List;

public class StockMovementAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ROW = 0;
    private static final int TYPE_LOADING = 1;

    private final List<StockMovement> items = new ArrayList<>();
    private boolean loading = false;

    public void submit(List<StockMovement> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    /** Appends a page and only invalidates the new range, so scrolling stays cheap. */
    public void append(List<StockMovement> list) {
        if (list == null || list.isEmpty()) return;
        int start = items.size();
        items.addAll(list);
        notifyItemRangeInserted(start, list.size());
    }

    public void setLoading(boolean loading) {
        if (this.loading == loading) return;
        this.loading = loading;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return position < items.size() ? TYPE_ROW : TYPE_LOADING;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_LOADING) {
            return new LoadingVH(inflater.inflate(R.layout.item_loading, parent, false));
        }
        return new VH(inflater.inflate(R.layout.item_stock_movement, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof VH) {
            bindMovement((VH) holder, items.get(position));
        }
    }

    private void bindMovement(VH holder, StockMovement m) {
        String unit = m.unitLabel == null ? "" : m.unitLabel;
        String unitSuffix = unit.isEmpty() ? "" : " " + unit;

        String typeLabel;
        int typeColor;
        try {
            MovementType type = MovementType.valueOf(m.movementType);
            typeLabel = type.getLabel();
            switch (type) {
                case PURCHASE: typeColor = R.color.success; break;
                case SALE: typeColor = R.color.error; break;
                case SALE_VOID: typeColor = R.color.info; break;
                case ADJUSTMENT: typeColor = R.color.info; break;
                case STOCK_TAKE: typeColor = R.color.warning; break;
                default: typeColor = R.color.info; break;
            }
        } catch (Exception e) {
            typeLabel = m.movementType;
            typeColor = R.color.text_secondary;
        }
        holder.type.setText(typeLabel);
        holder.type.setTextColor(holder.itemView.getContext().getColor(typeColor));

        double signedQty = m.stockAfter - m.stockBefore;
        if (signedQty == 0) signedQty = m.qty;
        boolean add = signedQty >= 0;
        holder.qty.setText((add ? "+" : "-")
                + NumberUtil.qty(Math.abs(signedQty)) + unitSuffix);
        holder.qty.setTextColor(holder.itemView.getContext().getColor(
                add ? R.color.success : R.color.error));

        holder.balance.setText("Prev " + NumberUtil.qty(m.stockBefore) + unitSuffix
                + "  ->  New " + NumberUtil.qty(m.stockAfter) + unitSuffix);

        StringBuilder meta = new StringBuilder();
        meta.append(DateUtil.format(m.createdAt));
        if (m.note != null && !m.note.isEmpty()) meta.append("  •  ").append(m.note);
        if (m.createdBy != null && !m.createdBy.isEmpty())
            meta.append("  •  by ").append(m.createdBy);
        if (m.refId != null && !m.refId.isEmpty())
            meta.append("  •  ref ").append(m.refId);
        holder.meta.setText(meta.toString());
    }

    @Override
    public int getItemCount() {
        return items.size() + (loading ? 1 : 0);
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView type, qty, balance, meta;

        VH(@NonNull View itemView) {
            super(itemView);
            type = itemView.findViewById(R.id.mvm_type);
            qty = itemView.findViewById(R.id.mvm_qty);
            balance = itemView.findViewById(R.id.mvm_balance);
            meta = itemView.findViewById(R.id.mvm_meta);
        }
    }

    static class LoadingVH extends RecyclerView.ViewHolder {
        LoadingVH(@NonNull View itemView) {
            super(itemView);
        }
    }
}