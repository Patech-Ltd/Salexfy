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

public class RoleAdapter extends RecyclerView.Adapter<RoleAdapter.VH> {

    public interface Listener {
        void onClick(int position);
    }

    public static class Row {
        public String name;
        public String subtitle;
    }

    private final List<Row> items = new ArrayList<>();
    private final Listener listener;

    public RoleAdapter(Listener listener) {
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
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_role, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Row row = items.get(position);
        holder.name.setText(row.name);
        holder.subtitle.setText(row.subtitle);
        holder.itemView.setOnClickListener(v -> listener.onClick(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name, subtitle;

        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.role_name);
            subtitle = itemView.findViewById(R.id.role_subtitle);
        }
    }
}
