package org.PSmart.POS.ui.MainView.ui.purchases;

import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.Expense;
import org.PSmart.POS.Entities.User;
import org.PSmart.POS.Utils.Components.Inputs;
import org.PSmart.POS.Utils.Components.PTextField;
import org.PSmart.POS.Utils.Components.forms.FormFrame;
import org.PSmart.POS.Utils.Components.forms.Ui;
import org.PSmart.POS.Utils.MyUtils;

import javax.swing.*;
import java.time.LocalDate;
import java.time.ZoneId;

/** "New Expense" form window (same style as the Supplier / Item forms; no pop-ups). */
public final class ExpenseForm {

    private static final String[] CATEGORIES =
            {"Rent", "Utilities", "Salaries", "Transport", "Office Supplies", "Marketing", "Maintenance", "Other"};
    private static final String[] PAYMENTS = {"Cash", "M-Pesa", "Card", "Bank Transfer"};

    private ExpenseForm() {}

    public static void open(User user, Runnable onSaved) {
        FormFrame f = new FormFrame("New Expense", "New Expense", 520);

        JComboBox<String> category = f.combo("Category", CATEGORIES, 0);
        PTextField desc = f.field("Description (what was it for?)", null);
        PTextField amount = f.decimal("Amount", null);
        JComboBox<String> payment = f.combo("Paid with", PAYMENTS, 0);
        org.jdatepicker.JDatePicker date = f.date("Date", LocalDate.now());

        FormFrame.Section more = f.section("More details (receipt number, notes)", false);
        PTextField receipt = more.field("Receipt / reference number", null);
        PTextField notes = more.field("Notes", null);

        f.errorLine();

        Runnable save = () -> {
            String d = desc.getText().trim();
            double amt = MyUtils.getDouble(amount.getText().trim().isEmpty() ? "0" : amount.getText().trim());
            if (d.isEmpty()) { f.error("Enter a description."); desc.requestFocus(); return; }
            if (amt <= 0) { f.error("Enter an amount greater than zero."); amount.requestFocus(); return; }
            LocalDate day = Inputs.dateOf(date);
            if (day == null) day = LocalDate.now();
            if (day.isAfter(LocalDate.now())) { f.error("The date can't be in the future."); return; }
            long when = day.equals(LocalDate.now()) ? System.currentTimeMillis()
                    : day.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            try {
                Expense e = Expense.builder()
                        .createdAt(when)
                        .createdBy(user)
                        .category((String) category.getSelectedItem())
                        .description(d)
                        .amount(amt)
                        .paymentMethod((String) payment.getSelectedItem())
                        .receiptNumber(receipt.getText().trim().isEmpty() ? null : receipt.getText().trim())
                        .notes(notes.getText().trim().isEmpty() ? null : notes.getText().trim())
                        .build();
                DatabaseUtils.getInstance().getExpenseRepository().save(e);
            } catch (Exception ex) {
                f.error("Could not save: " + ex.getMessage());
                return;
            }
            Ui.toast("Expense saved");
            f.dispose();
            if (onSaved != null) onSaved.run();
        };

        f.primary("Save Expense", save);
        f.secondary("Cancel", f::dispose);
        f.open();
        desc.requestFocus();
    }
}
