package org.PSmart.POS.ui.MainView.ui.sales;

import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.*;
import org.PSmart.POS.Repositories.SaleQueries;
import org.PSmart.POS.Utils.AuthorizationUtils;
import org.PSmart.POS.Utils.Components.PTextField;
import org.PSmart.POS.Utils.Components.forms.FormFrame;
import org.PSmart.POS.Utils.Components.forms.Ui;
import org.PSmart.POS.Utils.ReceiptPrinter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/** Review one sale: items, money, receipt/invoice, and cancel (void) with a recorded reason. */
final class SaleViewForm extends FormFrame {

    private boolean voidArmed;

    SaleViewForm(UUID saleId, User user, Runnable onChanged) {
        super("Sale", "Sale details", 1000);
        Sale s = SaleQueries.load(saleId);
        if (s == null) {
            note("This sale could not be found. It may have been removed.");
            secondary("Close", this::dispose);
            open();
            return;
        }
        setTitle("Sale " + s.getReceiptNumber());
        StoreConfig cfg = DatabaseUtils.getInstance().getStoreConfigRepository().getStoreConfig();
        final StoreConfig config = cfg != null ? cfg : StoreConfig.defaultConfig();
        boolean voided = s.getStatus() != null && SaleQueries.VOID_STATUSES.contains(s.getStatus().toUpperCase());

        info("Receipt", s.getReceiptNumber() + (voided ? "   [CANCELLED]" : ""));
        info("Date", new SimpleDateFormat("dd-MMM-yyyy HH:mm").format(new Date(s.getCreatedAt() == null ? 0 : s.getCreatedAt())));
        info("Served by", s.getCashier() == null ? "-" : (s.getCashier().getFullname() == null ? s.getCashier().getUsername() : s.getCashier().getFullname()));
        info("Customer", s.getCustomer() == null ? "Walk-in" : s.getCustomer().getFullname());
        info("Payment", (s.getPaymentMethod() == null ? "-" : s.getPaymentMethod().replace("MPESA", "M-Pesa").replace("+", " + "))
                + "   status: " + (s.getStatus() == null ? "-" : s.getStatus()));
        if (voided) {
            JLabel v = info("Cancelled", new SimpleDateFormat("dd-MMM-yyyy HH:mm").format(new Date(s.getVoidedAt() == null ? 0 : s.getVoidedAt()))
                    + " by " + (s.getVoidedByName() == null ? "-" : s.getVoidedByName()) + " - " + (s.getVoidReason() == null ? "" : s.getVoidReason()));
            v.setForeground(Color.RED);
        }

        DefaultTableModel m = new DefaultTableModel(new String[]{"Item", "Qty", "Price", "Discount", "Total", "Profit"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        double profit = 0;
        if (s.getItems() != null) {
            for (SaleItem it : s.getItems()) {
                if (it.isDeleted()) continue;
                double q = it.getQuantity() == null ? 0 : it.getQuantity();
                double tot = it.getTotal() == null ? 0 : it.getTotal();
                double cost = (it.getBuyingPrice() == null ? 0 : it.getBuyingPrice()) * q;
                profit += tot - cost;
                m.addRow(new Object[]{it.getItem() == null ? "(item)" : it.getItem().getName(),
                        q == Math.floor(q) ? String.format("%.0f", q) : String.format("%.2f", q),
                        money(it.getUnitPrice()), it.getDiscount() != null && it.getDiscount() > 0.004 ? money(it.getDiscount()) : "",
                        money(it.getTotal()), money(tot - cost)});
            }
        }
        JTable t = new JTable(m);
        Ui.styleTable(t);
        t.getColumnModel().getColumn(0).setPreferredWidth(320);
        custom(new JScrollPane(t), Math.min(300, 60 + m.getRowCount() * 30));

        info("Subtotal", money(s.getSubtotal()));
        if (s.getVatAmount() != null && s.getVatAmount() > 0.004) info("VAT", money(s.getVatAmount()));
        if (s.getDiscount() != null && s.getDiscount() > 0.004) info("Discount", "-" + money(s.getDiscount()));
        info("TOTAL", money(s.getTotal()));
        info("Paid", money(s.getAmountPaid()) + (s.getChange() != null && s.getChange() > 0.004 ? "   change " + money(s.getChange()) : ""));
        if (AuthorizationUtils.hasPermission(user, RoleType.ADMIN, false) || AuthorizationUtils.hasPermission(user, RoleType.REPORTS, false)) {
            info("Gross profit", money(profit));
        }

        final Section voidSection = section("Cancel this sale", false);
        final PTextField reason = voidSection.field("Reason for cancelling (required)", "");
        errorLine();

        secondary("Close", this::dispose);
        primary("Print Receipt", () -> ReceiptPrinter.printReceipt(s, config));
        secondary("Preview", () -> ReceiptPrinter.previewReceipt(this, s, config));
        secondary("Invoice", () -> ReceiptPrinter.previewInvoice(this, s, config));
        if (!voided) {
            final JButton[] btn = new JButton[1];
            btn[0] = danger("Cancel Sale", () -> {
                if (!voidArmed) {
                    voidArmed = true;
                    voidSection.setOpen(true);
                    btn[0].setText("Confirm Cancel");
                    error("Enter the reason, then press Confirm Cancel. Stock and the customer's account are restored.");
                    reason.getField().requestFocusInWindow();
                    return;
                }
                if (reason.getText().trim().length() < 3) { error("Enter a reason for cancelling this sale."); return; }
                User approver = org.PSmart.POS.Utils.SupervisorApproval.require(this, user, RoleType.CASHIER, "cancel a sale");
                if (approver == null) { error("Cancelling a sale needs a supervisor's approval."); return; }
                String err = SaleQueries.voidSale(saleId, reason.getText(), user, approver);
                if (err != null) { error(err); return; }
                Ui.toast("Sale " + s.getReceiptNumber() + " cancelled");
                dispose();
                if (onChanged != null) onChanged.run();
            });
        }
        open();
    }

    private static String money(Double v) { return String.format("%,.2f", v == null ? 0.0 : v); }
}
