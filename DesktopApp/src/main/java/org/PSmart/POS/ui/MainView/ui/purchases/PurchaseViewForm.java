package org.PSmart.POS.ui.MainView.ui.purchases;

import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.*;
import org.PSmart.POS.Repositories.PurchaseQueries;
import org.PSmart.POS.Repositories.TxnService;
import org.PSmart.POS.Utils.Components.PTextField;
import org.PSmart.POS.Utils.Components.forms.FormFrame;
import org.PSmart.POS.Utils.Components.forms.TxnUi;
import org.PSmart.POS.Utils.Components.forms.Ui;
import org.PSmart.POS.Utils.MyUtils;
import org.PSmart.POS.Utils.ReceiptPrinter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.UUID;

/** One purchase: lines, money owed, edit (supplier / invoice / notes / paid) and cancel (also takes the stock back out). */
public final class PurchaseViewForm {

    private PurchaseViewForm() {}

    public static void open(UUID id, User user, Runnable onChanged) {
        Purchase p = PurchaseQueries.load(id);
        Object[] meta = TxnService.purchaseMeta(id);
        FormFrame f = new FormFrame("Purchase", "Purchase details", 900);
        if (p == null || meta == null) {
            f.note("This purchase could not be found.");
            f.secondary("Close", f::dispose);
            f.open();
            return;
        }
        boolean cancelled = Boolean.TRUE.equals(meta[6]);
        f.setTitle("Purchase " + "PUR-" + String.format("%06d", id.hashCode() & 0xFFFFFF));
        double total = p.getTotal() == null ? 0 : p.getTotal();
        double paid = p.getPaidAmount() == null ? total : p.getPaidAmount();

        f.info("Reference", "PUR-" + String.format("%06d", id.hashCode() & 0xFFFFFF) + (cancelled ? "   [CANCELLED]" : ""));
        f.info("Date", TxnUi.when(p.getCreatedAt()));
        f.info("Recorded by", p.getCreatedBy() == null ? "-" : (p.getCreatedBy().getFullname() != null ? p.getCreatedBy().getFullname() : p.getCreatedBy().getUsername()));
        if (cancelled) TxnUi.cancelledBanner(f, (Long) meta[0], (String) meta[2], (String) meta[1]);
        else TxnUi.editedLine(f, (Long) meta[3], (String) meta[4]);

        DefaultTableModel m = new DefaultTableModel(new String[]{"Item", "Qty", "Unit cost", "Total"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        if (p.getItems() != null) for (PurchaseItem i : p.getItems()) {
            double q = i.getQuantity() == null ? 0 : i.getQuantity();
            m.addRow(new Object[]{i.getItem() == null ? "(item)" : i.getItem().getName(),
                    q == Math.floor(q) ? String.format("%.0f", q) : String.format("%.2f", q), TxnUi.money(i.getUnitPrice()), TxnUi.money(i.getTotal())});
        }
        JTable t = new JTable(m);
        Ui.styleTable(t);
        t.getColumnModel().getColumn(0).setPreferredWidth(360);
        f.custom(new JScrollPane(t), Math.min(280, 60 + m.getRowCount() * 30));
        f.info("Total", TxnUi.money(total));
        f.info("Paid", TxnUi.money(paid));
        f.info("Still owed", TxnUi.money(Math.max(0, total - paid)));

        StoreConfig cfg = DatabaseUtils.getInstance().getStoreConfigRepository().getStoreConfig();
        final StoreConfig config = cfg != null ? cfg : StoreConfig.defaultConfig();

        if (cancelled) {
            f.info("Supplier", p.getSupplierName() == null ? "-" : p.getSupplierName());
            f.info("Invoice #", p.getInvoiceNumber() == null ? "-" : p.getInvoiceNumber());
            f.secondary("Close", f::dispose);
            f.secondary("Preview Invoice", () -> ReceiptPrinter.previewPurchase(f, p, config));
            f.open();
            return;
        }

        FormFrame.Section edit = f.section("Edit details (supplier, invoice number, notes, amount paid)", false);
        PTextField supplier = edit.field("Supplier", p.getSupplierName());
        PTextField invoice = edit.field("Invoice number", p.getInvoiceNumber());
        PTextField notes = edit.field("Notes", p.getNotes());
        PTextField paidBox = edit.decimal("Amount paid to supplier", String.format("%.2f", paid));
        f.note("Quantities and prices can't be edited. To correct them, cancel this purchase and receive the stock again.");

        f.primary("Save Changes", () -> {
            edit.setOpen(true);
            double np = MyUtils.getDouble(paidBox.getText().trim().isEmpty() ? "0" : paidBox.getText().trim());
            String by = TxnUi.authorise(f, user, RoleType.PURCHASE, "edit this purchase");
            if (by == null) return;
            String err = TxnService.updatePurchase(id, supplier.getText(), invoice.getText(), notes.getText(), np, by);
            if (err != null) { f.error(err); return; }
            Ui.toast("Purchase updated");
            f.dispose();
            if (onChanged != null) onChanged.run();
        });
        TxnUi.cancelFlow(f, user, RoleType.PURCHASE, "purchase",
                meta[5] != null ? "The stock this purchase added is taken back out and the supplier balance is reduced." : "The supplier balance is reduced (this older purchase isn't linked to a stock delivery, so stock is not changed).",
                (reason, by) -> TxnService.cancelPurchase(id, reason, by), onChanged);
        f.secondary("Preview Invoice", () -> ReceiptPrinter.previewPurchase(f, p, config));
        f.secondary("Close", f::dispose);
        f.open();
    }
}
