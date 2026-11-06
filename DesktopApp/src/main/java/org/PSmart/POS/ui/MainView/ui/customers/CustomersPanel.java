package org.PSmart.POS.ui.MainView.ui.customers;

import jakarta.persistence.EntityManager;
import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.User;
import org.PSmart.POS.Utils.Components.forms.ListPage;
import org.PSmart.POS.Utils.Components.forms.Ui;
import org.PSmart.POS.Utils.ExcelExporter;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.*;
import java.util.List;

/** Customers: balances owed, credit limits and loyalty points; collect payments and view statements. */
public class CustomersPanel extends ListPage {

    private final User user;
    private final List<Object[]> all = new ArrayList<>();
    private final List<String> names = new ArrayList<>();

    public CustomersPanel() { this(null); }

    public CustomersPanel(User user) {
        super("+   Add New Customer", "Search customer (name or phone)",
                new String[]{"Customer", "Phone", "Owes", "Credit limit", "Points", "Status"});
        this.user = user;
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        right.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        for (int c : new int[]{2, 3, 4}) table.getColumnModel().getColumn(c).setCellRenderer(right);
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
                if (!s) comp.setForeground("OWES".equals(String.valueOf(v)) ? Color.RED : new Color(30, 110, 50));
                return comp;
            }
        });
        JButton edit = Ui.primary("Edit");
        JButton pay = Ui.primary("Collect Payment");
        JButton stmt = Ui.primary("Statement");
        JButton export = Ui.neutral("Export Excel");
        edit.addActionListener(e -> onOpen());
        pay.addActionListener(e -> { Object id = selectedId(); if (id != null) CustomerForms.collect((UUID) id, user, this::load); });
        stmt.addActionListener(e -> { Object id = selectedId(); if (id != null) CustomerForms.ledger((UUID) id, names.get(table.getSelectedRow()), user); });
        export.addActionListener(e -> export());
        for (JButton b : new JButton[]{edit, pay, stmt, export}) actions.add(b);
        actions.add(status);
        addBar.addActionListener(e -> CustomerForms.edit(null, user, this::load));
        SwingUtilities.invokeLater(this::load);
    }

    void load() {
        new SwingWorker<List<Object[]>, Void>() {
            @Override protected List<Object[]> doInBackground() {
                EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
                try {
                    List<Object[]> out = new ArrayList<>();
                    for (Object o : em.createQuery("SELECT c.uid, c.fullname, c.phone, c.currentBalance, c.creditLimit, c.loyaltyPoints "
                            + "FROM Customer c WHERE c.deleted=false ORDER BY c.fullname").getResultList()) out.add((Object[]) o);
                    return out;
                } finally {
                    em.close();
                }
            }
            @Override protected void done() {
                try { all.clear(); all.addAll(get()); render(); } catch (Exception ex) { flash("Could not load customers: " + ex.getMessage()); }
            }
        }.execute();
    }

    @Override protected void render() {
        String q = search.getText().trim().toLowerCase();
        model.setRowCount(0);
        ids.clear();
        names.clear();
        double owedTotal = 0;
        for (Object[] r : all) {
            if (!q.isEmpty() && !(r[1] + " " + r[2]).toLowerCase().contains(q)) continue;
            double bal = r[3] == null ? 0 : ((Number) r[3]).doubleValue();
            double lim = r[4] == null ? 0 : ((Number) r[4]).doubleValue();
            double pts = r[5] == null ? 0 : ((Number) r[5]).doubleValue();
            if (bal > 0) owedTotal += bal;
            ids.add(r[0]);
            names.add(String.valueOf(r[1]));
            model.addRow(new Object[]{r[1], r[2] == null ? "" : r[2], String.format("%,.2f", bal),
                    lim > 0 ? String.format("%,.2f", lim) : "no limit", pts > 0 ? String.format("%.0f", pts) : "-", bal > 0.004 ? "OWES" : "Clear"});
        }
        footer.setText(model.getRowCount() + " customers   |   total owed to you: " + String.format("%,.2f", owedTotal));
    }

    @Override protected void onOpen() {
        Object id = selectedId();
        if (id != null) CustomerForms.edit((UUID) id, user, this::load);
    }

    private void export() {
        String[] h = new String[model.getColumnCount()];
        for (int i = 0; i < h.length; i++) h[i] = model.getColumnName(i);
        List<Object[]> out = new ArrayList<>();
        for (int r = 0; r < model.getRowCount(); r++) {
            Object[] row = new Object[h.length];
            for (int c = 0; c < h.length; c++) row[c] = model.getValueAt(r, c);
            out.add(row);
        }
        ExcelExporter.exportToExcel(this, "Customers-" + java.time.LocalDate.now(), h, out);
    }
}
