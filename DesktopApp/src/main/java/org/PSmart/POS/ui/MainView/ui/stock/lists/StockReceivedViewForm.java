package org.PSmart.POS.ui.MainView.ui.stock.lists;

import org.PSmart.POS.Entities.RoleType;
import org.PSmart.POS.Entities.User;
import org.PSmart.POS.Repositories.TxnService;
import org.PSmart.POS.Utils.Components.PTextField;
import org.PSmart.POS.Utils.Components.forms.FormFrame;
import org.PSmart.POS.Utils.Components.forms.TxnUi;
import org.PSmart.POS.Utils.Components.forms.Ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/** One stock delivery: lines, edit the note, or cancel it (stock goes back down; refused if it was already sold). */
public final class StockReceivedViewForm {

    private StockReceivedViewForm() {}

    public static void open(UUID id, User user, Runnable onChanged) {
        Object[] meta = TxnService.feedMeta(id);
        FormFrame f = new FormFrame("Stock received", "Stock received", 900);
        if (meta == null) {
            f.note("This delivery could not be found.");
            f.secondary("Close", f::dispose);
            f.open();
            return;
        }
        String ref = "REC-" + id.toString().substring(0, 8).toUpperCase();
        boolean cancelled = Boolean.TRUE.equals(meta[2]);
        f.setTitle("Stock received " + ref);
        f.info("Reference", ref + (cancelled ? "   [CANCELLED]" : ""));
        f.info("Received", TxnUi.when(meta[0] == null ? null : ((Number) meta[0]).longValue()));
        if (cancelled) TxnUi.cancelledBanner(f, meta[3] == null ? null : ((Number) meta[3]).longValue(), (String) meta[5], (String) meta[4]);
        else TxnUi.editedLine(f, meta[6] == null ? null : ((Number) meta[6]).longValue(), (String) meta[7]);
        Object[] purchase = TxnService.purchaseOfFeed(id);
        if (purchase != null) {
            double total = purchase[2] == null ? 0 : ((Number) purchase[2]).doubleValue();
            double paid = purchase[3] == null ? total : ((Number) purchase[3]).doubleValue();
            f.info("Supplier", purchase[1] == null ? "-" : String.valueOf(purchase[1]));
            f.info("Purchase total", TxnUi.money(total) + "   paid " + TxnUi.money(paid) + "   owed " + TxnUi.money(Math.max(0, total - paid)));
        }

        DefaultTableModel m = new DefaultTableModel(new String[]{"Item", "Qty", "Cost", "Selling price", "Expiry"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
        List<Object[]> lines = TxnService.feedLines(id);
        for (Object[] r : lines) {
            double q = r[1] == null ? 0 : ((Number) r[1]).doubleValue();
            m.addRow(new Object[]{r[0] == null ? "(item)" : r[0], q == Math.floor(q) ? String.format("%.0f", q) : String.format("%.2f", q),
                    TxnUi.money(r[2] == null ? null : ((Number) r[2]).doubleValue()), TxnUi.money(r[3] == null ? null : ((Number) r[3]).doubleValue()),
                    r[4] == null ? "" : df.format(new Date(((Number) r[4]).longValue()))});
        }
        JTable t = new JTable(m);
        Ui.styleTable(t);
        t.getColumnModel().getColumn(0).setPreferredWidth(360);
        f.custom(new JScrollPane(t), Math.min(300, 60 + m.getRowCount() * 30));

        if (cancelled) {
            f.info("Note", meta[1] == null ? "-" : String.valueOf(meta[1]));
            f.secondary("Close", f::dispose);
            f.open();
            return;
        }
        FormFrame.Section edit = f.section("Edit note", false);
        PTextField note = edit.field("Note", meta[1] == null ? "" : String.valueOf(meta[1]));
        f.note("Quantities and costs can't be edited. To correct them, cancel this delivery and receive the stock again.");
        f.primary("Save Note", () -> {
            edit.setOpen(true);
            String by = TxnUi.authorise(f, user, RoleType.STOCK, "edit this delivery");
            if (by == null) return;
            String err = TxnService.updateFeedNote(id, note.getText(), by);
            if (err != null) { f.error(err); return; }
            Ui.toast("Note saved");
            f.dispose();
            if (onChanged != null) onChanged.run();
        });
        TxnUi.cancelFlow(f, user, RoleType.STOCK, "delivery",
                purchase != null ? "The stock is taken back out and the linked purchase is cancelled too (supplier balance reduced)." : "The stock is taken back out.",
                (reason, by) -> TxnService.cancelFeed(id, reason, by), onChanged);
        f.secondary("Close", f::dispose);
        f.open();
    }
}
