package com.patechltd.salexfypos.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.BillRow;
import com.patechltd.salexfypos.util.NumberUtil;

import java.util.ArrayList;
import java.util.List;

public class BillAdapter extends RecyclerView.Adapter<BillAdapter.VH> {

    public interface Listener {
        void onOpen(int position);

        void onPay(int position);
    }

    private final List<BillRow> items = new ArrayList<>();
    private final Listener listener;
    private final String currency;

    public BillAdapter(Listener listener, String currency) {
        this.listener = listener;
        this.currency = currency == null ? "" : currency;
    }

    public void submit(List<BillRow> rows) {
        items.clear();
        items.addAll(rows);
        notifyDataSetChanged();
    }

    public BillRow getItem(int position) {
        if (position < 0 || position >= items.size()) return null;
        return items.get(position);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bill, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        BillRow bill = items.get(position);
        String supplier = bill.supplierName == null || bill.supplierName.isEmpty()
                ? "General / Others" : bill.supplierName;
        holder.name.setText(supplier != null ? supplier : "General / Others");
        String invoice = bill.invoiceNo == null || bill.invoiceNo.isEmpty()
                ? "Invoice" : bill.invoiceNo;
        holder.sub.setText(invoice + "  •  " + com.patechltd.salexfypos.util.DateUtil.formatDate(bill.purchaseDate));
        holder.paid.setText("Paid " + NumberUtil.money(bill.paidAmount)
                + "  of  " + NumberUtil.money(bill.total));
        holder.balance.setText(currency + " " + NumberUtil.money(bill.balance));
        holder.status.setText(bill.paidAmount > 0.001 ? "PARTIAL" : "UNPAID");
        holder.itemView.setOnClickListener(v -> listener.onOpen(position));
        holder.pay.setOnClickListener(v -> listener.onPay(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name, sub, paid, balance, status;
        final View pay;

        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.bill_name);
            sub = itemView.findViewById(R.id.bill_sub);
            paid = itemView.findViewById(R.id.bill_paid);
            balance = itemView.findViewById(R.id.bill_balance);
            status = itemView.findViewById(R.id.bill_status);
            pay = itemView.findViewById(R.id.btn_pay);
        }
    }
}