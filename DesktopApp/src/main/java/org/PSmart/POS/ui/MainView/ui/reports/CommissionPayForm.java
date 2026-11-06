package org.PSmart.POS.ui.MainView.ui.reports;

import jakarta.persistence.EntityManager;
import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.CommissionPayout;
import org.PSmart.POS.Utils.Components.PTextField;
import org.PSmart.POS.Utils.Components.forms.FormFrame;
import org.PSmart.POS.Utils.Components.forms.Ui;

import java.util.UUID;

/** Record a commission payout against what is owed to a staff member. */
final class CommissionPayForm extends FormFrame {

    CommissionPayForm(UUID userId, String name, double owed, Runnable onSaved) {
        super("Pay Commission - " + name, "Pay Commission", 640);
        info("Staff", name);
        info("Commission owed", String.format("%,.2f", owed));
        if (owed <= 0.004) {
            note("Nothing is owed to " + name + " right now.");
            secondary("Close", this::dispose);
            open();
            return;
        }
        PTextField amount = decimal("Amount paid", String.format("%.2f", owed));
        String[] methods = {"Cash", "M-Pesa", "Bank", "Other"};
        Choice method = choice("Payment method", methods, 0, null);
        Section more = section("Add a note (optional)", false);
        PTextField note = more.field("Note", "");
        errorLine();
        secondary("Cancel", this::dispose);
        primary("Record Payout", () -> {
            double a;
            try {
                a = Math.round(Double.parseDouble(amount.getText().trim().replace(",", "")) * 100.0) / 100.0;
            } catch (NumberFormatException e) { error("Enter a valid amount."); return; }
            if (a <= 0) { error("Amount must be greater than zero."); return; }
            if (a > owed + 0.009) { error("That is more than the commission owed (" + String.format("%,.2f", owed) + ")."); return; }
            EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
            try {
                em.getTransaction().begin();
                em.persist(CommissionPayout.builder().userUid(userId).userName(name).amount(a)
                        .method(methods[method.getSelected()]).notes(note.getText().trim()).createdAt(System.currentTimeMillis()).build());
                em.getTransaction().commit();
                Ui.toast("Paid " + String.format("%,.2f", a) + " commission to " + name);
                dispose();
                if (onSaved != null) onSaved.run();
            } catch (Exception ex) {
                if (em.getTransaction().isActive()) em.getTransaction().rollback();
                error("Could not save: " + ex.getMessage());
            } finally {
                em.close();
            }
        });
        open();
    }
}
