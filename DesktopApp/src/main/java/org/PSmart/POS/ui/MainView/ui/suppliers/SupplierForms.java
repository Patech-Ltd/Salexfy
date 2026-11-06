package org.PSmart.POS.ui.MainView.ui.suppliers;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.Purchase;
import org.PSmart.POS.Entities.Supplier;
import org.PSmart.POS.Entities.SupplierPayment;
import org.PSmart.POS.Entities.User;
import org.PSmart.POS.Utils.Components.PTextField;
import org.PSmart.POS.Utils.Components.forms.FormFrame;
import org.PSmart.POS.Utils.Components.forms.Ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.text.SimpleDateFormat;
import java.util.*;

/** Data-entry forms for the Suppliers page (same look as Add Item / Edit User). */
final class SupplierForms {

    private SupplierForms() {}

    static String key(String n) { return n == null ? "" : n.trim().toLowerCase(); }

    static String money(double v) { return String.format("%,.2f", v); }

    private static String nz(String s) { return s == null ? "" : s; }

    // ------------------------------------------------------------------ add / edit

    static void edit(String existingName, Set<String> takenNames, Runnable onSaved) {
        Supplier current = null;
        if (existingName != null) {
            EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
            try {
                for (Supplier s : em.createQuery("FROM Supplier s WHERE s.deleted=false", Supplier.class).getResultList()) {
                    if (key(s.getName()).equals(key(existingName))) { current = s; break; }
                }
            } finally {
                em.close();
            }
        }
        final Supplier cur = current;
        FormFrame f = new FormFrame(existingName == null ? "Add New Supplier" : "Edit Supplier " + existingName,
                existingName == null ? "Add New Supplier" : "Edit Supplier", 640);
        PTextField name = f.field("Supplier Name", existingName == null ? "" : existingName);
        PTextField phone = f.field("Phone", cur == null ? "" : nz(cur.getPhone()));
        FormFrame.Section more = f.section("More details (email, address, notes)", cur != null
                && (!nz(cur.getEmail()).isEmpty() || !nz(cur.getAddress()).isEmpty() || !nz(cur.getNotes()).isEmpty()));
        PTextField email = more.field("Email", cur == null ? "" : nz(cur.getEmail()));
        PTextField address = more.field("Address", cur == null ? "" : nz(cur.getAddress()));
        PTextField notes = more.field("Notes", cur == null ? "" : nz(cur.getNotes()));
        f.errorLine();

        f.secondary("Cancel", f::dispose);
        f.primary(existingName == null ? "Add Supplier" : "Save Supplier", () -> {
            String n = name.getText().trim();
            if (n.isEmpty()) { f.error("Supplier name is required."); return; }
            boolean renamed = existingName != null && !key(n).equals(key(existingName));
            if ((existingName == null || renamed) && takenNames.contains(key(n))) {
                f.error("A supplier named \"" + n + "\" already exists.");
                return;
            }
            EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                Supplier s = cur == null ? new Supplier() : em.find(Supplier.class, cur.getUid());
                s.setName(n);
                s.setPhone(phone.getText().trim());
                s.setEmail(email.getText().trim());
                s.setAddress(address.getText().trim());
                s.setNotes(notes.getText().trim());
                if (s.getCreatedAt() == null) s.setCreatedAt(System.currentTimeMillis());
                if (cur == null) em.persist(s);
                if (renamed) {
                    em.createQuery("UPDATE Purchase p SET p.supplierName=:n WHERE LOWER(p.supplierName)=:o")
                            .setParameter("n", n).setParameter("o", key(existingName)).executeUpdate();
                    em.createQuery("UPDATE SupplierPayment p SET p.supplierName=:n WHERE LOWER(p.supplierName)=:o")
                            .setParameter("n", n).setParameter("o", key(existingName)).executeUpdate();
                }
                tx.commit();
                Ui.toast("Supplier saved - " + n);
                f.dispose();
                if (onSaved != null) onSaved.run();
            } catch (Exception ex) {
                if (tx.isActive()) tx.rollback();
                f.error("Could not save: " + ex.getMessage());
            } finally {
                em.close();
            }
        });
        f.open();
    }

    // ------------------------------------------------------------------ pay

    static void pay(String supplierName, double owing, User user, Runnable onSaved) {
        String[] methods = {"Cash", "M-Pesa", "Bank", "Cheque", "Other"};
        FormFrame f = new FormFrame("Pay Supplier - " + supplierName, "Pay Supplier", 640);
        f.info("Supplier", supplierName);
        f.info("Balance owed", money(owing));
        PTextField amount = f.decimal("Amount paid", String.format("%.2f", owing));
        FormFrame.Choice method = f.choice("Payment method", methods, 0, null);
        FormFrame.Section more = f.section("Add a note (optional)", false);
        PTextField note = more.field("Note", "");
        f.errorLine();

        f.secondary("Cancel", f::dispose);
        f.primary("Record Payment", () -> {
            double a;
            try {
                a = Math.round(Double.parseDouble(amount.getText().trim().replace(",", "")) * 100.0) / 100.0;
            } catch (NumberFormatException e) {
                f.error("Enter a valid amount.");
                return;
            }
            if (a <= 0) { f.error("Amount must be greater than zero."); return; }
            if (a > owing + 0.009) { f.error("Amount is more than the balance owed (" + money(owing) + ")."); return; }
            if (org.PSmart.POS.Utils.SupervisorApproval.require(f, user, org.PSmart.POS.Entities.RoleType.PURCHASE, "pay a supplier") == null) {
                f.error("Paying a supplier needs a supervisor's approval."); return;
            }
            EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                em.persist(SupplierPayment.builder().supplierName(supplierName).amount(a)
                        .method(methods[method.getSelected()]).notes(note.getText().trim())
                        .createdByName(user == null ? null : (user.getFullname() != null ? user.getFullname() : user.getUsername()))
                        .createdAt(System.currentTimeMillis()).build());
                tx.commit();
                Ui.toast("Paid " + money(a) + " to " + supplierName);
                f.dispose();
                if (onSaved != null) onSaved.run();
            } catch (Exception ex) {
                if (tx.isActive()) tx.rollback();
                f.error("Could not save: " + ex.getMessage());
            } finally {
                em.close();
            }
        });
        f.open();
    }

    // ------------------------------------------------------------------ statement

    static void statement(String name) {
        DefaultTableModel m = new DefaultTableModel(new String[]{"Date", "Type", "Reference", "Change", "Balance"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        double balance = 0;
        EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
        try {
            List<Object[]> ev = new ArrayList<>();
            for (Purchase p : em.createQuery("FROM Purchase p WHERE p.deleted=false AND LOWER(p.supplierName)=:n", Purchase.class)
                    .setParameter("n", key(name)).getResultList()) {
                double total = p.getTotal() == null ? 0 : p.getTotal();
                double paid = p.getPaidAmount() == null ? total : p.getPaidAmount();
                ev.add(new Object[]{p.getCreatedAt() == null ? 0L : p.getCreatedAt(), "Purchase",
                        (p.getInvoiceNumber() == null ? "" : "Inv " + p.getInvoiceNumber() + " - ")
                                + "total " + money(total) + ", paid " + money(paid), total - paid});
            }
            for (SupplierPayment sp : em.createQuery("FROM SupplierPayment sp WHERE sp.deleted=false AND LOWER(sp.supplierName)=:n",
                    SupplierPayment.class).setParameter("n", key(name)).getResultList()) {
                ev.add(new Object[]{sp.getCreatedAt() == null ? 0L : sp.getCreatedAt(), "Payment",
                        nz(sp.getMethod()) + (nz(sp.getNotes()).isEmpty() ? "" : " - " + sp.getNotes()), -sp.getAmount()});
            }
            ev.sort(Comparator.comparingLong(a -> (Long) a[0]));
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            for (Object[] e : ev) {
                double d = (Double) e[3];
                balance += d;
                m.addRow(new Object[]{df.format(new Date((Long) e[0])), e[1], e[2], (d > 0 ? "+" : "") + money(d), money(balance)});
            }
        } finally {
            em.close();
        }
        FormFrame f = new FormFrame("Statement - " + name, "Statement", 900);
        f.info("Supplier", name);
        f.info("Balance owed", money(balance));
        JTable t = new JTable(m);
        Ui.styleTable(t);
        t.getColumnModel().getColumn(2).setPreferredWidth(360);
        f.custom(new JScrollPane(t), 360);
        f.secondary("Close", f::dispose);
        f.open();
    }
}
