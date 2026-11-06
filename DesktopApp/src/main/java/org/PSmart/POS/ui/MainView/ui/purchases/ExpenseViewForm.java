package org.PSmart.POS.ui.MainView.ui.purchases;

import org.PSmart.POS.Entities.Expense;
import org.PSmart.POS.Entities.RoleType;
import org.PSmart.POS.Entities.User;
import org.PSmart.POS.Repositories.TxnService;
import org.PSmart.POS.Utils.Components.Inputs;
import org.PSmart.POS.Utils.Components.PTextField;
import org.PSmart.POS.Utils.Components.forms.FormFrame;
import org.PSmart.POS.Utils.Components.forms.TxnUi;
import org.PSmart.POS.Utils.Components.forms.Ui;
import org.PSmart.POS.Utils.MyUtils;

import javax.swing.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.UUID;

/** One expense: details, edit (needs the Expenses permission) and cancel with a recorded reason. */
public final class ExpenseViewForm {

    private static final String[] CATEGORIES = {"Rent", "Utilities", "Salaries", "Transport", "Office Supplies", "Marketing", "Maintenance", "Other"};
    private static final String[] PAYMENTS = {"Cash", "M-Pesa", "Card", "Bank Transfer"};

    private ExpenseViewForm() {}

    public static void open(UUID id, User user, Runnable onChanged) {
        Expense e = TxnService.expense(id);
        FormFrame f = new FormFrame("Expense", "Expense details", 560);
        if (e == null) {
            f.note("This expense could not be found.");
            f.secondary("Close", f::dispose);
            f.open();
            return;
        }
        f.setTitle("Expense " + e.getReferenceNumber());
        boolean cancelled = e.isDeleted();
        f.info("Reference", e.getReferenceNumber() + (cancelled ? "   [CANCELLED]" : ""));
        f.info("Recorded by", e.getCreatedBy() == null ? "-" : (e.getCreatedBy().getFullname() != null ? e.getCreatedBy().getFullname() : e.getCreatedBy().getUsername()));
        if (cancelled) {
            TxnUi.cancelledBanner(f, e.getCancelledAt(), e.getCancelledBy(), e.getCancelReason());
            f.info("Date", TxnUi.when(e.getCreatedAt()));
            f.info("Category", e.getCategory() == null ? "-" : e.getCategory());
            f.info("Description", e.getDescription() == null ? "-" : e.getDescription());
            f.info("Amount", TxnUi.money(e.getAmount()));
            f.info("Paid with", e.getPaymentMethod() == null ? "-" : e.getPaymentMethod());
            f.secondary("Close", f::dispose);
            f.open();
            return;
        }
        TxnUi.editedLine(f, e.getEditedAt(), e.getEditedBy());

        int ci = Math.max(0, Arrays.asList(CATEGORIES).indexOf(e.getCategory()));
        JComboBox<String> category = f.combo("Category", CATEGORIES, ci);
        PTextField desc = f.field("Description", e.getDescription());
        PTextField amount = f.decimal("Amount", e.getAmount() == null ? "" : String.format("%.2f", e.getAmount()));
        JComboBox<String> payment = f.combo("Paid with", PAYMENTS, Math.max(0, Arrays.asList(PAYMENTS).indexOf(e.getPaymentMethod())));
        LocalDate day = e.getCreatedAt() == null ? LocalDate.now() : Instant.ofEpochMilli(e.getCreatedAt()).atZone(ZoneId.systemDefault()).toLocalDate();
        org.jdatepicker.JDatePicker date = f.date("Date", day);
        FormFrame.Section more = f.section("More details (receipt number, notes)", e.getReceiptNumber() != null || e.getNotes() != null);
        PTextField receipt = more.field("Receipt / reference number", e.getReceiptNumber());
        PTextField notes = more.field("Notes", e.getNotes());

        f.primary("Save Changes", () -> {
            double amt = MyUtils.getDouble(amount.getText().trim().isEmpty() ? "0" : amount.getText().trim());
            LocalDate d = Inputs.dateOf(date);
            if (d == null) d = day;
            if (d.isAfter(LocalDate.now())) { f.error("The date can't be in the future."); return; }
            String by = TxnUi.authorise(f, user, RoleType.EXPENSE, "edit this expense");
            if (by == null) return;
            long when = d.equals(day) && e.getCreatedAt() != null ? e.getCreatedAt()
                    : d.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            String err = TxnService.updateExpense(id, (String) category.getSelectedItem(), desc.getText(), amt,
                    (String) payment.getSelectedItem(), receipt.getText(), notes.getText(), when, by);
            if (err != null) { f.error(err); return; }
            Ui.toast("Expense updated");
            f.dispose();
            if (onChanged != null) onChanged.run();
        });
        TxnUi.cancelFlow(f, user, RoleType.EXPENSE, "expense", "The expense stops counting in your totals and reports.",
                (reason, by) -> TxnService.cancelExpense(id, reason, by), onChanged);
        f.secondary("Close", f::dispose);
        f.open();
    }
}
