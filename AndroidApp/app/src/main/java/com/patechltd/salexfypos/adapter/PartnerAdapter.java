package com.patechltd.salexfypos.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.patechltd.salexfypos.R;

import java.util.ArrayList;
import java.util.List;

public class PartnerAdapter extends RecyclerView.Adapter<PartnerAdapter.VH> {

    public interface Listener {
        void onClick(int position);
    }

    public static class Row {
        public String title;
        public String subtitle;
        public String phone;
        public String amount;
        public int amountColor;
    }

    private final List<Row> items = new ArrayList<>();
    private final Listener listener;

    public PartnerAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Row> rows) {
        items.clear();
        items.addAll(rows);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_partner, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Row row = items.get(position);
        holder.name.setText(row.title);
        holder.avatar.setText(initials(row.title));
        if (row.phone != null && !row.phone.isEmpty()) {
            holder.phone.setVisibility(View.VISIBLE);
            holder.phone.setText(row.phone);
        } else {
            holder.phone.setVisibility(View.GONE);
        }
        holder.subtitle.setText(row.subtitle == null ? "" : row.subtitle);
        if (row.amount != null && !row.amount.isEmpty()) {
            holder.amount.setVisibility(View.VISIBLE);
            holder.amount.setText(row.amount);
            holder.amount.setTextColor(row.amountColor != 0 ? row.amountColor
                    : holder.itemView.getContext().getResources().getColor(R.color.accent_positive));
        } else {
            holder.amount.setVisibility(View.GONE);
        }
        holder.itemView.setOnClickListener(v -> listener.onClick(position));
    }

    private String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView avatar, name, phone, subtitle, amount;

        VH(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.partner_avatar);
            name = itemView.findViewById(R.id.partner_name);
            phone = itemView.findViewById(R.id.partner_phone);
            subtitle = itemView.findViewById(R.id.partner_subtitle);
            amount = itemView.findViewById(R.id.partner_amount);
        }
    }
}
