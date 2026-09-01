package com.patechltd.salexfypos.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.entity.PaymentMethod;

import java.util.ArrayList;
import java.util.List;

public class PayMethodAdapter extends RecyclerView.Adapter<PayMethodAdapter.VH> {

    public interface Listener {
        void onEdit(int position);

        void onToggle(int position, boolean active);

        void onDelete(int position);
    }

    private final List<PaymentMethod> items = new ArrayList<>();
    private final Listener listener;

    public PayMethodAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<PaymentMethod> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pay_method, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        PaymentMethod m = items.get(position);
        holder.name.setText(m.name != null && !m.name.isEmpty() ? m.name : m.id);
        String id = m.id == null ? "" : m.id;
        holder.subtitle.setText(m.isCredit ? "On credit • " + id : id);
        holder.active.setChecked(m.active);
        holder.active.setOnClickListener(v -> listener.onToggle(position, holder.active.isChecked()));
        holder.delete.setVisibility(m.isSystem ? View.GONE : View.VISIBLE);
        holder.delete.setOnClickListener(v -> listener.onDelete(position));
        holder.itemView.setOnClickListener(v -> listener.onEdit(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name, subtitle, delete;
        final SwitchMaterial active;

        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.method_name);
            subtitle = itemView.findViewById(R.id.method_subtitle);
            delete = itemView.findViewById(R.id.method_delete);
            active = itemView.findViewById(R.id.method_active);
        }
    }
}