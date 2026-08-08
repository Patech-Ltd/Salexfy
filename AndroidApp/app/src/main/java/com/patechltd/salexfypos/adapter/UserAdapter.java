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

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.VH> {

    public interface Listener {
        void onClick(int position);
    }

    public static class Row {
        public String fullName;
        public String roleName;
        public boolean active;
        public String username;
    }

    private final List<Row> items = new ArrayList<>();
    private final Listener listener;

    public UserAdapter(Listener listener) {
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
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Row row = items.get(position);
        holder.name.setText(row.fullName);
        holder.role.setText(row.username + " · " + row.roleName);
        holder.status.setText(row.active ? "Active" : "Inactive");
        holder.status.setBackgroundResource(row.active ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
        holder.status.setTextColor(row.active ? 0xFFFFFFFF : 0xFF64748B);
        String initial = row.fullName == null || row.fullName.isEmpty() ? "?" : row.fullName.substring(0, 1).toUpperCase();
        holder.avatar.setText(initial);
        holder.itemView.setOnClickListener(v -> listener.onClick(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView avatar, name, role, status;

        VH(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatar);
            name = itemView.findViewById(R.id.name);
            role = itemView.findViewById(R.id.role);
            status = itemView.findViewById(R.id.status);
        }
    }
}
