package com.patechltd.salexfypos.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.patechltd.salexfypos.R;

import java.util.ArrayList;
import java.util.List;

public class SimpleStringAdapter extends RecyclerView.Adapter<SimpleStringAdapter.VH> {

    public interface Listener {
        void onClick(int position);

        void onEdit(int position);

        void onDelete(int position);
    }

    private final List<String> items = new ArrayList<>();
    private final Listener listener;
    private final boolean editable;

    public SimpleStringAdapter(Listener listener, boolean editable) {
        this.listener = listener;
        this.editable = editable;
    }

    public void submit(List<String> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_string_row, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.text.setText(items.get(position));
        holder.itemView.setOnClickListener(v -> listener.onClick(position));
        if (editable) {
            holder.edit.setVisibility(View.VISIBLE);
            holder.delete.setVisibility(View.VISIBLE);
            holder.edit.setOnClickListener(v -> listener.onEdit(position));
            holder.delete.setOnClickListener(v -> listener.onDelete(position));
        } else {
            holder.edit.setVisibility(View.GONE);
            holder.delete.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView text;
        final ImageButton edit, delete;

        VH(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.text);
            edit = itemView.findViewById(R.id.edit);
            delete = itemView.findViewById(R.id.delete);
        }
    }
}
