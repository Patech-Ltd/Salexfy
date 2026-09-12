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

public class BackupAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onRestore(Row row);
    }

    public static class Row {
        public boolean header;
        public String title;
        public String subtitle;
        public String restoreLabel;
        public Object payload;

        public Row(String title) {
            this.header = true;
            this.title = title;
        }

        public Row(String title, String subtitle, String restoreLabel, Object payload) {
            this.header = false;
            this.title = title;
            this.subtitle = subtitle;
            this.restoreLabel = restoreLabel;
            this.payload = payload;
        }
    }

    private final List<Row> items = new ArrayList<>();
    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Row> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).header ? 0 : 1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == 0) {
            return new HeaderVH(inflater.inflate(R.layout.item_backup_header, parent, false));
        }
        return new ItemVH(inflater.inflate(R.layout.item_backup_row, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = items.get(position);
        if (holder instanceof HeaderVH) {
            ((HeaderVH) holder).title.setText(row.title);
            return;
        }
        ItemVH vh = (ItemVH) holder;
        vh.title.setText(row.title);
        vh.subtitle.setText(row.subtitle);
        vh.btnRestore.setOnClickListener(v -> {
            if (listener != null) listener.onRestore(row);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderVH extends RecyclerView.ViewHolder {
        final TextView title;

        HeaderVH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.section_title);
        }
    }

    static class ItemVH extends RecyclerView.ViewHolder {
        final TextView title, subtitle;
        final com.google.android.material.button.MaterialButton btnRestore;

        ItemVH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.backup_title);
            subtitle = itemView.findViewById(R.id.backup_badge);
            btnRestore = itemView.findViewById(R.id.btn_restore);
        }
    }
}