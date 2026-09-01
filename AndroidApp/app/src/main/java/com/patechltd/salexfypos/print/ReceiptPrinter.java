package com.patechltd.salexfypos.print;

import com.patechltd.salexfypos.db.entity.Sale;
import com.patechltd.salexfypos.db.entity.SaleItem;
import com.patechltd.salexfypos.db.entity.SalePayment;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.Prefs;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a printable ESC/POS byte stream (and a matching plain-text preview)
 * from a completed sale. The two outputs share the same line model so the
 * on-screen preview always matches what the printer produces.
 */
public final class ReceiptPrinter {

    public static final int WIDTH_80 = 42;
    public static final int WIDTH_58 = 30;

    public static final int ALIGN_LEFT = 0;
    public static final int ALIGN_CENTER = 1;

    public static class Line {
        public final String text;
        public final int align;
        public final boolean bold;

        Line(String text, int align, boolean bold) {
            this.text = text;
            this.align = align;
            this.bold = bold;
        }
    }

    public static class Doc {
        public final int width;
        public final List<Line> lines = new ArrayList<>();

        Doc(int width) {
            this.width = width;
        }

        void add(String text) {
            lines.add(new Line(text, ALIGN_LEFT, false));
        }

        void add(String text, int align, boolean bold) {
            lines.add(new Line(text, align, bold));
        }

        void blank() {
            lines.add(new Line("", ALIGN_LEFT, false));
        }

        void divider() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < width; i++) sb.append('-');
            lines.add(new Line(sb.toString(), ALIGN_LEFT, false));
        }
    }

    private ReceiptPrinter() {
    }

    public static Doc build(Context context, Sale sale, List<SaleItem> items) {
        return build(context, sale, items, null);
    }

    public static Doc build(Context context, Sale sale, List<SaleItem> items,
                            List<SalePayment> payments) {
        int width = Prefs.getInt(context, Prefs.KEY_PRINTER_WIDTH, WIDTH_58);
        String shop = Prefs.getString(context, Prefs.KEY_SHOP_NAME, "My Shop");
        String phone = Prefs.getString(context, Prefs.KEY_SHOP_PHONE, "");
        String address = Prefs.getString(context, Prefs.KEY_SHOP_ADDRESS, "");
        String footer = Prefs.getString(context, Prefs.KEY_RECEIPT_FOOTER, "");

        Doc doc = new Doc(width);

        doc.add(truncate(shop, width), ALIGN_CENTER, true);
        if (address != null && !address.isEmpty()) {
            doc.add(center(truncate(address, width), width), ALIGN_CENTER, false);
        }
        if (phone != null && !phone.isEmpty()) {
            doc.add(center(truncate("Tel: " + phone, width), width), ALIGN_CENTER, false);
        }
        doc.blank();
        doc.divider();
        doc.add("Sale No: " + safe(sale.saleNo));
        doc.add("Cashier: " + safe(sale.cashierName));
        doc.add("Date: " + com.patechltd.salexfypos.util.DateUtil.format(sale.saleDate));
        if (sale.customerName != null && !sale.customerName.isEmpty()) {
            doc.add("Customer: " + safe(sale.customerName));
        }
        doc.divider();

        for (SaleItem item : items) {
            doc.add(truncate(safe(item.productName), width));
            String qtyPrice = "  " + NumberUtil.qty(item.qty)
                    + (item.unitLabel == null || item.unitLabel.isEmpty()
                    ? "" : " " + item.unitLabel)
                    + " x " + NumberUtil.money(item.unitPrice);
            String lineTotal = NumberUtil.money(item.lineTotal);
            doc.add(padPair(qtyPrice, lineTotal, width));
        }

        doc.divider();
        doc.add(padPair("Subtotal", NumberUtil.money(sale.subtotal), width));
        if (sale.discount > 0) {
            doc.add(padPair("Discount", "-" + NumberUtil.money(sale.discount), width));
        }
        if (sale.taxAmount > 0) {
            doc.add(padPair("Tax (" + safe(String.valueOf(Prefs.taxPercent(context))) + "%)",
                    NumberUtil.money(sale.taxAmount), width));
        }
        doc.add(padPair("TOTAL", NumberUtil.money(sale.total), width), ALIGN_CENTER, true);
        doc.divider();

        paymentBlock(doc, width, sale, payments);
        doc.blank();

        if (footer != null && !footer.isEmpty()) {
            doc.add(center(truncate(footer, width), width), ALIGN_CENTER, false);
            doc.blank();
        }
        return doc;
    }

    private static void paymentBlock(Doc doc, int width, Sale sale, List<SalePayment> payments) {
        if (payments != null && !payments.isEmpty()) {
            for (SalePayment payment : payments) {
                String label = com.patechltd.salexfypos.model.PaymentMethod.labelOf(payment.method);
                if (payment.customerName != null && !payment.customerName.isEmpty()) {
                    label += " (" + safe(payment.customerName) + ")";
                }
                doc.add(truncate(padPair(label, NumberUtil.money(payment.amount), width), width));
            }
            if (sale.paidAmount > 0) doc.add("Paid: " + NumberUtil.money(sale.paidAmount));
            if (sale.changeAmount > 0) doc.add("Change: " + NumberUtil.money(sale.changeAmount));
            double balance = sale.total - sale.paidAmount;
            if (balance > 0.001) doc.add("Balance: " + NumberUtil.money(balance));
        } else if ("CREDIT".equals(sale.paymentMethod)) {
            doc.add("Payment: ON CREDIT");
            if (sale.customerName != null && !sale.customerName.isEmpty()) {
                doc.add("Customer: " + safe(sale.customerName));
            }
            if (sale.paidAmount > 0) doc.add("Paid now: " + NumberUtil.money(sale.paidAmount));
            doc.add("Balance: " + NumberUtil.money(sale.total - sale.paidAmount));
        } else {
            doc.add("Payment: " + com.patechltd.salexfypos.model.PaymentMethod.labelOf(sale.paymentMethod));
            doc.add("Paid: " + NumberUtil.money(sale.paidAmount));
            doc.add("Change: " + NumberUtil.money(sale.changeAmount));
        }
    }

    public static byte[] buildReceiptBytes(Context context, Sale sale, List<SaleItem> items) {
        return buildReceiptBytes(context, sale, items, null);
    }

    public static byte[] buildReceiptBytes(Context context, Sale sale, List<SaleItem> items,
                                           List<SalePayment> payments) {
        Doc doc = build(context, sale, items, payments);
        EscPos p = new EscPos();
        p.init();
        for (Line line : doc.lines) {
            if (line.text.isEmpty()) {
                p.blank();
            } else {
                p.line(line.text, line.align == ALIGN_CENTER ? EscPos.ALIGN_CENTER : EscPos.ALIGN_LEFT,
                        line.bold);
            }
        }
        p.feed(3);
        p.cut();
        return p.toByteArray();
    }

    /** Plain-text version of the receipt, ideal for on-screen preview. */
    public static String buildReceiptText(Context context, Sale sale, List<SaleItem> items,
                                          List<SalePayment> payments) {
        Doc doc = build(context, sale, items, payments);
        StringBuilder sb = new StringBuilder();
        for (Line line : doc.lines) {
            sb.append(line.text).append('\n');
        }
        return sb.toString();
    }

    private static String padPair(String left, String right, int width) {
        int pad = Math.max(1, width - left.length() - right.length());
        StringBuilder sb = new StringBuilder();
        sb.append(truncate(left, width - right.length() - 1));
        for (int i = 0; i < pad; i++) sb.append(' ');
        sb.append(right);
        return truncate(sb.toString(), width);
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
