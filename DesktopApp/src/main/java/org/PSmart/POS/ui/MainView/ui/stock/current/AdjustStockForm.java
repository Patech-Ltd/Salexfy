package org.PSmart.POS.ui.MainView.ui.stock.current;

import org.PSmart.POS.Entities.RoleType;
import org.PSmart.POS.Entities.StockMovement;
import org.PSmart.POS.Entities.User;
import org.PSmart.POS.Utils.Components.PTextField;
import org.PSmart.POS.Utils.Components.forms.FormFrame;
import org.PSmart.POS.Utils.Components.forms.Ui;
import org.PSmart.POS.Utils.StockService;

import javax.swing.*;
import java.util.UUID;

/** Adjust stock form: add / remove / set exact count, with reason and optional note. */
final class AdjustStockForm extends FormFrame {

    private static final String[] REASONS = {"Correction", "Damaged", "Expired", "Lost", "Returned", "Other"};

    AdjustStockForm(UUID itemId, String itemName, double current, User user, Runnable onSaved) {
        super("Adjust Stock - " + itemName, "Adjust Stock", 640);

        info("Product", itemName);
        info("On hand now", fmt(current));

        final JLabel[] after = new JLabel[1];
        final Choice[] mode = new Choice[1];
        final PTextField[] qty = new PTextField[1];

        mode[0] = choice("What do you want to do?", new String[]{"Add stock", "Remove stock", "Set exact count"}, 0,
                i -> refresh(after[0], mode[0], qty[0], current));
        qty[0] = decimal("Quantity", "");
        after[0] = info("Stock after this change", fmt(current));
        qty[0].getField().addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyReleased(java.awt.event.KeyEvent e) { refresh(after[0], mode[0], qty[0], current); }
        });

        final Choice reason = choice("Reason", REASONS, 0, null);
        Section more = section("Add a note (optional)", false);
        final PTextField note = more.field("Note", "");
        errorLine();

        secondary("Cancel", this::dispose);
        primary("Save", () -> {
            Double q = parse(qty[0].getText());
            int m = mode[0].getSelected();
            if (q == null || q < 0 || (q == 0 && m != 2)) {
                error("Enter a quantity greater than zero.");
                return;
            }
            double delta = m == 0 ? q : m == 1 ? -q : q - current;
            if (Math.abs(delta) < 1e-9) {
                error("No change - the count already matches.");
                return;
            }
            if (current + delta < 0) {
                error("Cannot remove more than on hand (" + fmt(current) + "). Use 'Set exact count' to correct.");
                return;
            }
            User approver = org.PSmart.POS.Utils.SupervisorApproval.require(this, user, RoleType.STOCK, "adjust stock");
            if (approver == null) { error("Adjusting stock needs a supervisor's approval."); return; }
            String approvedNote = approver.getUid().equals(user == null ? null : user.getUid()) ? "" : " [approved by " + approver.getFullname() + "]";
            try {
                StockService.record(itemId, StockMovement.ADJUSTMENT, delta, REASONS[reason.getSelected()],
                        note.getText().trim() + approvedNote, null, user);
                Ui.toast("Stock adjusted: " + itemName + " now " + fmt(current + delta));
                dispose();
                if (onSaved != null) onSaved.run();
            } catch (Exception ex) {
                error("Could not save: " + ex.getMessage());
            }
        });
        open();
    }

    private void refresh(JLabel after, Choice mode, PTextField qty, double current) {
        Double q = parse(qty.getText());
        String text;
        if (q == null) {
            text = fmt(current);
        } else {
            int m = mode.getSelected();
            text = fmt(m == 0 ? current + q : m == 1 ? current - q : q);
        }
        after.setText("<html><span style='color:#6c757d'>Stock after this change&nbsp;&nbsp;</span><b>" + text + "</b></html>");
    }

    private static Double parse(String s) {
        try {
            return s == null || s.trim().isEmpty() ? null : Double.parseDouble(s.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String fmt(double v) {
        return v == Math.floor(v) ? String.format("%,.0f", v) : String.format("%,.2f", v);
    }
}
