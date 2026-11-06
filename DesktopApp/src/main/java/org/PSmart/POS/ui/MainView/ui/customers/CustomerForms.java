package org.PSmart.POS.ui.MainView.ui.customers;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.*;
import org.PSmart.POS.Utils.Components.PTextField;
import org.PSmart.POS.Utils.Components.forms.FormFrame;
import org.PSmart.POS.Utils.Components.forms.Ui;
import org.PSmart.POS.Utils.ReceiptPrinter;
import org.PSmart.POS.Utils.receipt.ReceiptDocs;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/** Add/edit customer, collect a debt payment, and the account statement - all as forms. */
final class CustomerForms {

    private CustomerForms() {}

    private static String money(double v) { return String.format("%,.2f", v); }

    private static String nz(String s) { return s == null ? "" : s; }

    // ------------------------------------------------------------------ add / edit

    static void edit(UUID id, Runnable onSaved) {
        edit(id, null, onSaved);
    }

    static void edit(UUID id, User by, Runnable onSaved) {
        Customer cur = null;
        if (id != null) {
            EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
            try { cur = em.find(Customer.class, id); } finally { em.close(); }
        }
        final Customer c = cur;
        FormFrame f = new FormFrame(c == null ? "Add New Customer" : "Edit Customer", c == null ? "Add New Customer" : "Edit Customer", 640);
        PTextField name = f.field("Customer Name", c == null ? "" : nz(c.getFullname()));
        PTextField phone = f.field("Phone", c == null ? "" : nz(c.getPhone()));
        FormFrame.Section credit = f.section("Credit & loyalty", c != null && c.getCreditLimit() != null && c.getCreditLimit() > 0);
        PTextField limit = credit.decimal("Credit limit (0 or blank = no limit)", c == null || c.getCreditLimit() == null ? "" : String.valueOf(c.getCreditLimit()));
        FormFrame.Section more = f.section("More details (email, address, notes)",
                c != null && (!nz(c.getEmail()).isEmpty() || !nz(c.getAddress()).isEmpty() || !nz(c.getNotes()).isEmpty()));
        PTextField email = more.field("Email", c == null ? "" : nz(c.getEmail()));
        PTextField address = more.field("Address", c == null ? "" : nz(c.getAddress()));
        PTextField notes = more.field("Notes", c == null ? "" : nz(c.getNotes()));
        if (c != null) {
            f.info("Owes now", money(c.getCurrentBalance() == null ? 0 : c.getCurrentBalance()));
            if (c.getLoyaltyPoints() != null && c.getLoyaltyPoints() > 0) f.info("Loyalty points", String.format("%.0f", c.getLoyaltyPoints()));
        }
        f.errorLine();
        f.secondary("Cancel", f::dispose);
        f.primary(c == null ? "Add Customer" : "Save Customer", () -> {
            if (name.getText().trim().isEmpty()) { f.error("Customer name is required."); return; }
            Double lim = null;
            if (!limit.getText().trim().isEmpty()) {
                try {
                    lim = Double.parseDouble(limit.getText().trim().replace(",", ""));
                    if (lim < 0) throw new NumberFormatException();
                } catch (NumberFormatException e) { f.error("Credit limit must be a number, 0 or more."); return; }
            }
            EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                Customer x = c == null ? new Customer() : em.find(Customer.class, c.getUid());
                x.setFullname(name.getText().trim());
                x.setPhone(phone.getText().trim());
                x.setEmail(email.getText().trim());
                x.setAddress(address.getText().trim());
                x.setNotes(notes.getText().trim());
                x.setCreditLimit(lim);
                if (c == null) { x.setCurrentBalance(0.0); em.persist(x); }
                tx.commit();
                Ui.toast("Customer saved - " + x.getFullname());
                f.dispose();
                if (onSaved != null) onSaved.run();
            } catch (Exception ex) {
                if (tx.isActive()) tx.rollback();
                f.error("Could not save: " + ex.getMessage());
            } finally {
                em.close();
            }
        });
        if (c != null) {
            final JButton[] del = new JButton[1];
            final boolean[] armed = {false};
            del[0] = f.danger("Delete Customer", () -> {
                double bal = c.getCurrentBalance() == null ? 0 : c.getCurrentBalance();
                if (bal > 0.004) { f.error("This customer still owes " + money(bal) + ". Collect the balance first."); return; }
                if (!armed[0]) { armed[0] = true; del[0].setText("Tap again to confirm"); return; }
                if (org.PSmart.POS.Utils.SupervisorApproval.require(f, by, org.PSmart.POS.Entities.RoleType.CASHIER, "delete a customer") == null) {
                    f.error("Deleting a customer needs a supervisor's approval."); return;
                }
                EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
                try {
                    em.getTransaction().begin();
                    em.find(Customer.class, c.getUid()).setDeleted(true);
                    em.getTransaction().commit();
                    Ui.toast("Customer deleted");
                    f.dispose();
                    if (onSaved != null) onSaved.run();
                } catch (Exception ex) {
                    f.error("Could not delete: " + ex.getMessage());
                } finally {
                    em.close();
                }
            });
        }
        f.open();
    }

    // ------------------------------------------------------------------ collect payment

    static void collect(UUID id, User by, Runnable onSaved) {
        EntityManager em0 = DatabaseUtils.getInstance().getEmf().createEntityManager();
        Customer c;
        try { c = em0.find(Customer.class, id); } finally { em0.close(); }
        final double owed = c == null || c.getCurrentBalance() == null ? 0 : c.getCurrentBalance();
        FormFrame f = new FormFrame("Collect Payment", "Collect Payment", 640);
        if (c == null) { f.note("Customer not found."); f.secondary("Close", f::dispose); f.open(); return; }
        f.info("Customer", nz(c.getFullname()));
        f.info("Owes", money(owed));
        if (owed <= 0.004) {
            f.note("This customer has no outstanding balance.");
            f.secondary("Close", f::dispose);
            f.open();
            return;
        }
        PTextField amount = f.decimal("Amount received", String.format("%.2f", owed));
        String[] methods = {"Cash", "M-Pesa", "Bank", "Cheque"};
        FormFrame.Choice method = f.choice("Payment method", methods, 0, null);
        FormFrame.Section more = f.section("Add a note (optional)", false);
        PTextField note = more.field("Note", "");
        StoreConfig cfg = DatabaseUtils.getInstance().getStoreConfigRepository().getStoreConfig();
        JCheckBox print = f.check("Print a receipt", cfg == null || !Boolean.FALSE.equals(cfg.getAutoPrintReceipt()));
        f.errorLine();
        f.secondary("Cancel", f::dispose);
        f.primary("Record Payment", () -> {
            double a;
            try {
                a = Math.round(Double.parseDouble(amount.getText().trim().replace(",", "")) * 100.0) / 100.0;
            } catch (NumberFormatException e) { f.error("Enter a valid amount."); return; }
            if (a <= 0) { f.error("Amount must be greater than zero."); return; }
            if (a > owed + 0.009) { f.error("That is more than the balance owed (" + money(owed) + ")."); return; }
            EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                Customer m = em.find(Customer.class, id);
                double bal = m.getCurrentBalance() == null ? 0 : m.getCurrentBalance();
                double after = Math.max(0, bal - a);
                em.persist(CreditTransaction.builder().customer(m).type("PAYMENT").amount(a).balanceAfter(after)
                        .cashier(by == null ? null : em.getReference(User.class, by.getUid())).createdAt(System.currentTimeMillis())
                        .notes((methods[method.getSelected()]) + (note.getText().trim().isEmpty() ? "" : " - " + note.getText().trim())).build());
                m.setCurrentBalance(after);
                tx.commit();
                Ui.toast("Received " + money(a) + " from " + m.getFullname());
                if (print.isSelected()) {
                    ReceiptPrinter.print(ReceiptDocs.customerPayment(m.getFullname(), a, methods[method.getSelected()], after,
                            System.currentTimeMillis(), by == null ? null : (by.getFullname() != null ? by.getFullname() : by.getUsername()),
                            cfg != null ? cfg : StoreConfig.defaultConfig()), cfg);
                }
                f.dispose();
                if (onSaved != null) onSaved.run();
            } catch (Exception ex) {
                if (tx.isActive()) tx.rollback();
                f.error("Could not record the payment: " + ex.getMessage());
            } finally {
                em.close();
            }
        });
        f.open();
    }

    // ------------------------------------------------------------------ statement

    static void ledger(UUID id, String name, User user) {
        DefaultTableModel m = new DefaultTableModel(new String[]{"Date", "Type", "Amount", "Balance", "Details"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        double owed = 0;
        final List<UUID> rowIds = new java.util.ArrayList<>();
        final List<Boolean> cancellable = new java.util.ArrayList<>();
        EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            List<Object[]> rows = em.createQuery("SELECT t.createdAt, t.type, t.amount, t.balanceAfter, t.notes, t.uid, t.cancelled FROM CreditTransaction t "
                    + "WHERE t.deleted=false AND t.customer.uid=:id ORDER BY t.createdAt ASC", Object[].class).setParameter("id", id).getResultList();
            for (Object[] r : rows) {
                String type = String.valueOf(r[1]);
                double amt = r[2] == null ? 0 : ((Number) r[2]).doubleValue();
                double bal = r[3] == null ? 0 : ((Number) r[3]).doubleValue();
                boolean wasCancelled = Boolean.TRUE.equals(r[6]);
                rowIds.add((UUID) r[5]);
                cancellable.add("PAYMENT".equals(type) && !wasCancelled);
                String label = "CHARGE".equals(type) ? "On credit" : "PAYMENT".equals(type) ? (wasCancelled ? "Payment (cancelled)" : "Payment")
                        : "PAYMENT_VOID".equals(type) ? "Payment cancelled" : "Cancelled sale";
                boolean plus = "CHARGE".equals(type) || "PAYMENT_VOID".equals(type);
                m.addRow(new Object[]{r[0] == null ? "" : df.format(new Date(((Number) r[0]).longValue())),
                        label, (plus ? "+" : "-") + money(amt), money(bal), r[4] == null ? "" : r[4]});
                owed = bal;
            }
        } finally {
            em.close();
        }
        FormFrame f = new FormFrame("Statement - " + name, "Account Statement", 900);
        f.info("Customer", name);
        f.info("Owes now", money(owed));
        JTable t = new JTable(m);
        Ui.styleTable(t);
        t.getColumnModel().getColumn(4).setPreferredWidth(340);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        f.custom(new JScrollPane(t), 380);
        f.note("To undo a payment received by mistake, select it in the table and cancel it. The amount becomes owed again.");
        FormFrame.Section sec = f.section("Cancel a payment", false);
        PTextField reason = sec.field("Reason for cancelling (required)", "");
        f.errorLine();
        f.danger("Cancel Selected Payment", () -> {
            sec.setOpen(true);
            int r = t.getSelectedRow();
            if (r < 0 || r >= rowIds.size() || !cancellable.get(r)) { f.error("Select a payment that hasn't been cancelled."); return; }
            if (reason.getText().trim().length() < 3) { f.error("Enter a reason for cancelling (at least 3 letters)."); return; }
            String by = org.PSmart.POS.Utils.Components.forms.TxnUi.authorise(f, user, RoleType.CASHIER, "cancel a customer payment");
            if (by == null) return;
            String err = org.PSmart.POS.Repositories.TxnService.cancelCustomerPayment(rowIds.get(r), reason.getText().trim(), by);
            if (err != null) { f.error(err); return; }
            Ui.toast("Payment cancelled - balance owed updated");
            f.dispose();
            ledger(id, name, user);
        });
        f.secondary("Close", f::dispose);
        f.open();
    }
}
