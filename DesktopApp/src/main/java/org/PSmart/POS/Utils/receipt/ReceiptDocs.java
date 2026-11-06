package org.PSmart.POS.Utils.receipt;

import org.PSmart.POS.Entities.*;
import org.PSmart.POS.Utils.receipt.Receipt.Align;
import org.PSmart.POS.Utils.receipt.Receipt.Paper;

import java.text.SimpleDateFormat;
import java.util.Date;

/** Builds the printable documents: sale receipt, sale invoice, purchase invoice, payment receipts. */
public final class ReceiptDocs {

    private ReceiptDocs() {}

    private static String money(Double v) { return String.format("%,.2f", v == null ? 0.0 : v); }

    private static String cur(StoreConfig c) {
        return c != null && c.getCurrency() != null && !c.getCurrency().isBlank() ? c.getCurrency().trim() : "KES";
    }

    private static String date(Long t) {
        return new SimpleDateFormat("dd-MMM-yyyy HH:mm").format(new Date(t == null ? System.currentTimeMillis() : t));
    }

    private static boolean has(String s) { return s != null && !s.isBlank(); }

    private static String qty(Double q) {
        double v = q == null ? 0 : q;
        return v == Math.floor(v) ? String.format("%.0f", v) : String.format("%.2f", v);
    }

    private static String methodLabel(String m) {
        if (m == null) return "Cash";
        return m.replace("MPESA", "M-Pesa").replace("CASH", "Cash").replace("CARD", "Card")
                .replace("CREDIT", "Credit").replace("+", " + ");
    }

    private static String taxLabel(StoreConfig c) {
        String n = c != null && has(c.getTaxName()) ? c.getTaxName().trim() : "Tax";
        if (c != null && c.getTaxRate() != null && c.getTaxRate() > 0) n += " " + Math.round(c.getTaxRate()) + "%";
        return n;
    }

    // ---------------------------------------------------------------- shared header (thermal)

    private static void thermalHeader(Receipt r, StoreConfig c) {
        String name = c != null && has(c.getStoreName()) ? c.getStoreName() : "PSMART POS";
        r.center(name.toUpperCase(), 6, true);
        if (c != null) {
            if (has(c.getStoreAddress())) r.center(c.getStoreAddress(), -2, false);
            if (has(c.getStorePhone())) r.center("Tel: " + c.getStorePhone(), -2, false);
            if (has(c.getStoreEmail())) r.center(c.getStoreEmail(), -2, false);
            if (has(c.getTaxPin())) r.center("PIN: " + c.getTaxPin(), -2, true);
            if (has(c.getReceiptHeader())) { r.gap(4); r.center(c.getReceiptHeader(), -2, false); }
        }
        r.gap(6);
    }

    private static void thermalFooter(Receipt r, StoreConfig c) {
        r.rule('-');
        String footer = c != null && has(c.getReceiptFooter()) ? c.getReceiptFooter() : "Thank you for shopping with us!";
        r.center(footer, 0, true);
        r.gap(4);
        r.muted("Printed " + new SimpleDateFormat("dd-MMM-yyyy HH:mm").format(new Date()));
        r.muted("PSMART POS");
        r.gap(30);   // feed past the cutter
    }

    // ---------------------------------------------------------------- sale receipt

    public static Receipt saleReceipt(Sale s, StoreConfig c) {
        Paper paper = c == null ? Paper.ROLL_80 : Paper.fromSetting(c.getReceiptPaper());
        if (paper == Paper.A4) paper = Paper.ROLL_80;      // the receipt is always a roll document
        Receipt r = new Receipt(paper, "Receipt " + s.getReceiptNumber());
        String cur = cur(c);

        thermalHeader(r, c);
        r.rule('=');
        r.center("SALES RECEIPT", 4, true);
        r.box(s.getReceiptNumber(), 2);
        r.rule('-');
        r.kv("Date", date(s.getCreatedAt()));
        if (s.getCashier() != null) r.kv("Served by", name(s.getCashier()));
        if (s.getCustomer() != null) r.kv("Customer", s.getCustomer().getFullname());
        r.rule('-');

        double qtyTotal = 0;
        int lines = 0;
        if (s.getItems() != null) {
            for (SaleItem it : s.getItems()) {
                if (it.isDeleted()) continue;
                lines++;
                qtyTotal += it.getQuantity() == null ? 0 : it.getQuantity();
                String nm = it.getItem() == null ? "(item)" : it.getItem().getName();
                String line = qty(it.getQuantity()) + " x " + money(it.getUnitPrice());
                if (it.getDiscount() != null && it.getDiscount() > 0.004) line += "  (-" + money(it.getDiscount()) + ")";
                r.item(nm, line, money(it.getTotal()));
            }
        }
        r.rule('-');
        r.kv(lines + " item(s), qty " + qty(qtyTotal), "", -2, false);
        r.kv("Subtotal", money(s.getSubtotal()));
        if (s.getVatAmount() != null && s.getVatAmount() > 0.004) r.kv(taxLabel(c), money(s.getVatAmount()));
        if (s.getDiscount() != null && s.getDiscount() > 0.004) r.kv("Discount", "-" + money(s.getDiscount()));
        r.gap(4);
        r.banner("TOTAL " + cur, money(s.getTotal()));
        r.gap(2);

        r.kv("Payment", methodLabel(s.getPaymentMethod()), 0, true);
        double paid = s.getAmountPaid() == null ? 0 : s.getAmountPaid();
        double total = s.getTotal() == null ? 0 : s.getTotal();
        if (paid > 0.004) r.kv("Amount paid", money(paid));
        if (s.getChange() != null && s.getChange() > 0.004) r.kv("Change", money(s.getChange()), 2, true);
        if ("CREDIT".equalsIgnoreCase(s.getStatus()) || (s.getPaymentMethod() != null && s.getPaymentMethod().contains("CREDIT"))) {
            double onCredit = Math.max(0, total - Math.min(paid, total));
            r.gap(4);
            r.kv("ON CREDIT", money(onCredit), 0, true);
            if (s.getCustomer() != null && s.getCustomer().getCurrentBalance() != null) {
                r.kv("Account balance", money(s.getCustomer().getCurrentBalance()), 0, false);
            }
        }
        thermalFooter(r, c);
        return r;
    }

    private static String name(User u) {
        return u.getFullname() != null && !u.getFullname().isBlank() ? u.getFullname() : u.getUsername();
    }

    // ---------------------------------------------------------------- sale invoice (A4)

    public static Receipt saleInvoice(Sale s, StoreConfig c) {
        Receipt r = new Receipt(Paper.A4, "Invoice " + s.getReceiptNumber());
        String cur = cur(c);
        boolean unpaid = "CREDIT".equalsIgnoreCase(s.getStatus());
        double total = s.getTotal() == null ? 0 : s.getTotal();
        double paid = s.getAmountPaid() == null ? 0 : Math.min(s.getAmountPaid(), total);
        double due = Math.max(0, total - paid);

        String store = c != null && has(c.getStoreName()) ? c.getStoreName() : "PSMART POS";
        java.util.List<String> left = new java.util.ArrayList<>();
        left.add(store.toUpperCase());
        if (c != null) {
            if (has(c.getStoreAddress())) left.add(c.getStoreAddress());
            if (has(c.getStorePhone())) left.add("Tel: " + c.getStorePhone());
            if (has(c.getStoreEmail())) left.add(c.getStoreEmail());
            if (has(c.getTaxPin())) left.add("PIN: " + c.getTaxPin());
        }
        r.twoColumn(left.toArray(new String[0]), true,
                new String[]{"INVOICE", "No: " + s.getReceiptNumber(), "Date: " + date(s.getCreatedAt()),
                        due > 0.004 ? "Status: UNPAID" : "Status: PAID"}, true, 4);
        r.gap(14);
        r.rule('_');

        r.text("BILL TO", Align.LEFT, -2, true);
        if (s.getCustomer() != null) {
            Customer cu = s.getCustomer();
            r.text(cu.getFullname(), Align.LEFT, 2, true);
            if (has(cu.getPhone())) r.left("Tel: " + cu.getPhone());
            if (has(cu.getEmail())) r.left(cu.getEmail());
            if (has(cu.getAddress())) r.left(cu.getAddress());
        } else {
            r.text("Walk-in customer", Align.LEFT, 0, false);
        }
        if (s.getCashier() != null) r.text("Served by: " + name(s.getCashier()), Align.LEFT, -4, false);
        r.gap(16);

        int[] wts = {6, 46, 10, 18, 14, 18};
        Align[] al = {Align.CENTER, Align.LEFT, Align.CENTER, Align.RIGHT, Align.RIGHT, Align.RIGHT};
        r.table(wts, al, new String[]{"#", "Description", "Qty", "Unit Price", "Discount", "Amount (" + cur + ")"}, true, false);
        int n = 0;
        if (s.getItems() != null) {
            for (SaleItem it : s.getItems()) {
                if (it.isDeleted()) continue;
                n++;
                r.table(wts, al, new String[]{String.valueOf(n), it.getItem() == null ? "(item)" : it.getItem().getName(),
                        qty(it.getQuantity()), money(it.getUnitPrice()),
                        it.getDiscount() != null && it.getDiscount() > 0.004 ? money(it.getDiscount()) : "-",
                        money(it.getTotal())}, false, n % 2 == 0);
            }
        }
        r.gap(10);
        r.rule('_');
        r.kvIndented("Subtotal", money(s.getSubtotal()), 0, false, 0.55);
        if (s.getVatAmount() != null && s.getVatAmount() > 0.004) r.kvIndented(taxLabel(c), money(s.getVatAmount()), 0, false, 0.55);
        if (s.getDiscount() != null && s.getDiscount() > 0.004) r.kvIndented("Discount", "-" + money(s.getDiscount()), 0, false, 0.55);
        r.gap(4);
        r.kvIndented("TOTAL " + cur, money(total), 4, true, 0.55);
        r.kvIndented("Paid (" + methodLabel(s.getPaymentMethod()) + ")", money(paid), 0, false, 0.55);
        r.kvIndented("BALANCE DUE", money(due), 4, true, 0.55);
        r.gap(30);
        String footer = c != null && has(c.getReceiptFooter()) ? c.getReceiptFooter() : "Thank you for your business!";
        r.center(footer, 0, true);
        r.muted("Generated by PSMART POS on " + date(System.currentTimeMillis()));
        return r;
    }

    // ---------------------------------------------------------------- purchase invoice (A4)

    public static Receipt purchaseInvoice(Purchase p, StoreConfig c) {
        Receipt r = new Receipt(Paper.A4, "Purchase " + (has(p.getInvoiceNumber()) ? p.getInvoiceNumber() : ""));
        String cur = cur(c);
        double total = p.getTotal() == null ? 0 : p.getTotal();
        double paid = p.getPaidAmount() == null ? total : p.getPaidAmount();
        double owed = Math.max(0, total - paid);

        String store = c != null && has(c.getStoreName()) ? c.getStoreName() : "PSMART POS";
        r.twoColumn(new String[]{store.toUpperCase(), c != null && has(c.getStoreAddress()) ? c.getStoreAddress() : "",
                        c != null && has(c.getStorePhone()) ? "Tel: " + c.getStorePhone() : ""}, true,
                new String[]{"PURCHASE INVOICE", has(p.getInvoiceNumber()) ? "Supplier Inv: " + p.getInvoiceNumber() : "",
                        "Date: " + date(p.getCreatedAt()), owed > 0.004 ? "Status: PART PAID" : "Status: PAID"}, true, 4);
        r.gap(14);
        r.rule('_');
        r.text("SUPPLIER", Align.LEFT, -2, true);
        r.text(has(p.getSupplierName()) ? p.getSupplierName() : "(not specified)", Align.LEFT, 2, true);
        if (p.getCreatedBy() != null) r.text("Recorded by: " + name(p.getCreatedBy()), Align.LEFT, -4, false);
        if (has(p.getNotes())) r.text("Notes: " + p.getNotes(), Align.LEFT, -4, false);
        r.gap(16);

        int[] wts = {6, 58, 12, 20, 22};
        Align[] al = {Align.CENTER, Align.LEFT, Align.CENTER, Align.RIGHT, Align.RIGHT};
        r.table(wts, al, new String[]{"#", "Item", "Qty", "Unit Cost", "Amount (" + cur + ")"}, true, false);
        int n = 0;
        if (p.getItems() != null) {
            for (PurchaseItem it : p.getItems()) {
                if (it.isDeleted()) continue;
                n++;
                r.table(wts, al, new String[]{String.valueOf(n), it.getItem() == null ? "(item)" : it.getItem().getName(),
                        qty(it.getQuantity()), money(it.getUnitPrice()), money(it.getTotal())}, false, n % 2 == 0);
            }
        }
        r.gap(10);
        r.rule('_');
        r.kvIndented("Subtotal", money(p.getSubtotal()), 0, false, 0.55);
        if (p.getTaxAmount() != null && p.getTaxAmount() > 0.004) r.kvIndented("Tax", money(p.getTaxAmount()), 0, false, 0.55);
        r.gap(4);
        r.kvIndented("TOTAL " + cur, money(total), 4, true, 0.55);
        r.kvIndented("Paid to supplier", money(paid), 0, false, 0.55);
        r.kvIndented("BALANCE OWED", money(owed), 4, true, 0.55);
        r.gap(30);
        r.muted("Generated by PSMART POS on " + date(System.currentTimeMillis()));
        return r;
    }

    // ---------------------------------------------------------------- payment receipts (thermal)

    public static Receipt supplierPayment(String supplier, double amount, String method, String note,
                                          double balanceAfter, Long when, String by, StoreConfig c) {
        Paper paper = c == null ? Paper.ROLL_80 : Paper.fromSetting(c.getReceiptPaper());
        if (paper == Paper.A4) paper = Paper.ROLL_80;
        Receipt r = new Receipt(paper, "Supplier payment");
        thermalHeader(r, c);
        r.rule('=');
        r.center("SUPPLIER PAYMENT", 4, true);
        r.rule('-');
        r.kv("Date", date(when));
        r.kv("Supplier", supplier);
        r.kv("Method", method == null ? "Cash" : method);
        if (has(by)) r.kv("Paid by", by);
        if (has(note)) r.kv("Note", note);
        r.gap(4);
        r.banner("PAID " + cur(c), money(amount));
        r.kv("Balance still owed", money(balanceAfter), 0, true);
        r.gap(10);
        r.rule('-');
        r.gap(24);
        r.text("Signature: ______________________", Align.LEFT, -2, false);
        r.gap(10);
        r.muted("PSMART POS");
        r.gap(30);
        return r;
    }

    public static Receipt customerPayment(String customer, double amount, String method, double balanceAfter,
                                          Long when, String by, StoreConfig c) {
        Paper paper = c == null ? Paper.ROLL_80 : Paper.fromSetting(c.getReceiptPaper());
        if (paper == Paper.A4) paper = Paper.ROLL_80;
        Receipt r = new Receipt(paper, "Payment received");
        thermalHeader(r, c);
        r.rule('=');
        r.center("PAYMENT RECEIVED", 4, true);
        r.rule('-');
        r.kv("Date", date(when));
        r.kv("Customer", customer);
        r.kv("Method", method == null ? "Cash" : method);
        if (has(by)) r.kv("Received by", by);
        r.gap(4);
        r.banner("RECEIVED " + cur(c), money(amount));
        r.kv("Account balance", money(balanceAfter), 0, true);
        r.gap(8);
        String footer = c != null && has(c.getReceiptFooter()) ? c.getReceiptFooter() : "Thank you!";
        r.center(footer, 0, true);
        r.gap(8);
        r.muted("PSMART POS");
        r.gap(30);
        return r;
    }
}
