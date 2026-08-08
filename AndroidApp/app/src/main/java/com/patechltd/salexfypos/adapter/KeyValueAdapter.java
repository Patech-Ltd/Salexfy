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

public class KeyValueAdapter extends RecyclerView.Adapter<KeyValueAdapter.VH> {

    public interface Listener {
        void onClick(int position);
    }

    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public static class Row {
        public String title;
        public String subtitle;
        public String value;
        public int valueColor;

        public Row(String title, String subtitle, String value, int valueColor) {
            this.title = title;
            this.subtitle = subtitle;
            this.value = value;
            this.valueColor = valueColor;
        }
    }

    private final List<Row> items = new ArrayList<>();

    public void submit(List<Row> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report_row, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Row row = items.get(position);
        holder.title.setText(row.title);
        holder.subtitle.setText(row.subtitle);
        holder.value.setText(row.value);
        if (row.valueColor != 0) holder.value.setTextColor(row.valueColor);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(position);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView title, subtitle, value;

        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            subtitle = itemView.findViewById(R.id.subtitle);
            value = itemView.findViewById(R.id.value);
        }
    }
}
