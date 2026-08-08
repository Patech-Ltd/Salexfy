package com.patechltd.salexfypos.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.entity.StockTake;
import com.patechltd.salexfypos.util.DateUtil;

import java.util.ArrayList;
import java.util.List;

public class StockTakeAdapter extends RecyclerView.Adapter<StockTakeAdapter.VH> {

    public interface Listener {
        void onClick(StockTake stockTake);
    }

    private final List<StockTake> items = new ArrayList<>();
    private final Listener listener;

    public StockTakeAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<StockTake> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stock_take, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        StockTake t = items.get(position);
        holder.name.setText(t.name);
        holder.date.setText(DateUtil.format(t.stockTakeDate));
        holder.status.setText("COMPLETE".equals(t.status) ? "Completed" : "In progress");
        holder.status.setTextColor(holder.itemView.getContext().getColor(
                "COMPLETE".equals(t.status) ? R.color.success : R.color.warning));
        holder.itemView.setOnClickListener(v -> listener.onClick(t));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name, date, status;

        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            date = itemView.findViewById(R.id.date);
            status = itemView.findViewById(R.id.status);
        }
    }
}
