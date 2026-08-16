package com.patechltd.salexfypos.print;

import com.patechltd.salexfypos.db.entity.Sale;
import com.patechltd.salexfypos.db.entity.SaleItem;
import com.patechltd.salexfypos.db.entity.SalePayment;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.Prefs;

import android.content.Context;

import java.util.List;

/**
 * Builds a printable ESC/POS byte stream from a completed sale.
 */
public final class ReceiptPrinter {

    public static final int WIDTH_80 = 42;
    public static final int WIDTH_58 = 30;

    private ReceiptPrinter() {
    }

    public static byte[] buildReceiptBytes(Context context, Sale sale, List<SaleItem> items) {
        return buildReceiptBytes(context, sale, items, null);
    }

    public static byte[] buildReceiptBytes(Context context, Sale sale, List<SaleItem> items,
                                           List<SalePayment> payments) {
        int width = Prefs.getInt(context, Prefs.KEY_PRINTER_WIDTH, WIDTH_58);
        String shop = Prefs.getString(context, Prefs.KEY_SHOP_NAME, "My Shop");
        String phone = Prefs.getString(context, Prefs.KEY_SHOP_PHONE, "");
        String address = Prefs.getString(context, Prefs.KEY_SHOP_ADDRESS, "");
        String footer = Prefs.getString(context, Prefs.KEY_RECEIPT_FOOTER, "");
        String currency = Prefs.currency(context);

        EscPos p = new EscPos();
        p.init();

        p.line(shop, EscPos.ALIGN_CENTER, true);
        if (address != null && !address.isEmpty()) p.line(center(address, width), EscPos.ALIGN_CENTER, false);
        if (phone != null && !phone.isEmpty()) p.line(center("Tel: " + phone, width), EscPos.ALIGN_CENTER, false);
        p.blank();
        p.divider('-', width);
        p.line("Sale No: " + safe(sale.saleNo));
        p.line("Cashier: " + safe(sale.cashierName));
        p.line("Date: " + com.patechltd.salexfypos.util.DateUtil.format(sale.saleDate));
        if (sale.customerName != null && !sale.customerName.isEmpty()) {
            p.line("Customer: " + safe(sale.customerName));
        }
        p.divider('-', width);

        for (SaleItem item : items) {
            p.line(truncate(safe(item.productName), width));
            String qtyPrice = "  " + NumberUtil.qty(item.qty) + " x " + NumberUtil.money(item.unitPrice);
            String lineTotal = NumberUtil.money(item.lineTotal);
            int pad = Math.max(1, width - qtyPrice.length() - lineTotal.length());
            StringBuilder sb = new StringBuilder();
            sb.append(qtyPrice);
            for (int i = 0; i < pad; i++) sb.append(' ');
            sb.append(lineTotal);
            p.line(sb.toString());
        }

        p.divider('-', width);
        p.line("Subtotal: " + NumberUtil.money(sale.subtotal));
        if (sale.taxAmount > 0) {
            p.line("Tax (" + safe(String.valueOf(Prefs.taxPercent(context))) + "%): "
                    + NumberUtil.money(sale.taxAmount));
        }
        p.line("TOTAL: " + NumberUtil.money(sale.total), EscPos.ALIGN_CENTER, true);
        p.divider('-', width);

        paymentBlock(p, width, sale, payments);
        p.blank();

        if (footer != null && !footer.isEmpty()) {
            p.line(center(footer, width), EscPos.ALIGN_CENTER, false);
            p.blank();
        }
        p.feed(3);
        p.cut();
        return p.toByteArray();
    }

    private static void paymentBlock(EscPos p, int width, Sale sale, List<SalePayment> payments) {
        if (payments != null && !payments.isEmpty()) {
            for (SalePayment payment : payments) {
                String label = com.patechltd.salexfypos.model.PaymentMethod.labelOf(payment.method);
                if (payment.customerName != null && !payment.customerName.isEmpty()) {
                    label += " (" + safe(payment.customerName) + ")";
                }
                String text = label + ": " + NumberUtil.money(payment.amount);
                p.line(truncate(text, width));
            }
            if (sale.paidAmount > 0) p.line("Paid: " + NumberUtil.money(sale.paidAmount));
            if (sale.changeAmount > 0) p.line("Change: " + NumberUtil.money(sale.changeAmount));
            double balance = sale.total - sale.paidAmount;
            if (balance > 0.001) p.line("Balance: " + NumberUtil.money(balance));
        } else if ("CREDIT".equals(sale.paymentMethod)) {
            p.line("Payment: ON CREDIT");
        } else {
            p.line("Payment: " + com.patechltd.salexfypos.model.PaymentMethod.labelOf(sale.paymentMethod));
            p.line("Paid: " + NumberUtil.money(sale.paidAmount));
            p.line("Change: " + NumberUtil.money(sale.changeAmount));
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String center(String text, int width) {
        if (text.length() >= width) return truncate(text, width);
        int left = (width - text.length()) / 2;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < left; i++) sb.append(' ');
        sb.append(text);
        return sb.toString();
    }

    private static String truncate(String text, int width) {
        if (text.length() <= width) return text;
        return text.substring(0, Math.max(1, width - 1)) + "~";
    }
}
