package org.PSmart.POS.ui.MainView.ui.suppliers;

import jakarta.persistence.EntityManager;
import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.Supplier;
import org.PSmart.POS.Entities.User;
import org.PSmart.POS.Utils.Components.PTextField;
import org.PSmart.POS.Utils.Components.forms.Ui;
import org.PSmart.POS.Utils.ExcelExporter;
import org.PSmart.POS.Utils.IconsUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

import static org.PSmart.POS.ui.MainView.ui.suppliers.SupplierForms.key;
import static org.PSmart.POS.ui.MainView.ui.suppliers.SupplierForms.money;

/**
 * Suppliers list page in the same layout as All Items: "+ Add New" bar, search, list.
 * All data entry happens in forms (SupplierForms), never in pop-up dialogs.
 */
public class SuppliersPanel extends JPanel {

    private final User user;
    private final JButton addBar = Ui.addBar("+   Add New Supplier");
    private final PTextField search = new PTextField(Ui.SLATE, new Color(0, 0, 255), "Search Supplier",
            IconsUtils.getIcon("search.png"));
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Supplier", "Phone", "Purchased", "Paid", "Balance Owed"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JScrollPane scroll = new JScrollPane(table);
    private final JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
    private final JLabel status = new JLabel(" ");
    private final JLabel totals = new JLabel(" ");

    private final List<Object[]> rows = new ArrayList<>();      // {name, phone, purchased, paid}
    private final List<String> shown = new ArrayList<>();

    public SuppliersPanel(User user) {
        super(null);
        this.user = user;
        setBackground(Ui.PAGE);

        add(addBar);
        add(search);

        Ui.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        right.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        for (int c = 2; c <= 4; c++) table.getColumnModel().getColumn(c).setCellRenderer(right);
        add(scroll);

        actions.setOpaque(false);
        JButton edit = Ui.primary("Edit");
        JButton pay = Ui.primary("Record Payment");
        JButton stmt = Ui.primary("Statement");
        JButton export = Ui.neutral("Export Excel");
        for (JButton b : new JButton[]{edit, pay, stmt, export}) actions.add(b);
        actions.add(status);
        add(actions);

        totals.setFont(new Font("Courier", Font.BOLD, 14));
        add(totals);
        status.setFont(new Font("Courier", Font.BOLD, 13));
        status.setForeground(Color.RED);

        addBar.addActionListener(e -> SupplierForms.edit(null, names(), this::load));
        edit.addActionListener(e -> withSelection(n -> SupplierForms.edit(n, names(), this::load)));
        pay.addActionListener(e -> withSelection(n -> {
            double owing = owed(n);
            if (owing <= 0.009) flash("Nothing is owed to " + n + ".");
            else SupplierForms.pay(n, owing, user, this::load);
        }));
        stmt.addActionListener(e -> withSelection(SupplierForms::statement));
        export.addActionListener(e -> export());
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) withSelection(SupplierForms::statement);
            }
        });
        search.getField().addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyReleased(java.awt.event.KeyEvent e) { render(); }
        });

        SwingUtilities.invokeLater(this::load);
        layoutUi();
    }

    @Override
    public void setBounds(int x, int y, int w, int h) {
        super.setBounds(x, y, w, h);
        if (w > 0) layoutUi();
    }

    private void layoutUi() {
        int w = getWidth(), h = getHeight();
        if (w <= 1 || h <= 1) return;
        addBar.setBounds(w / 4, 10, w / 2, 35);
        search.setLocation(w / 6, 50);
        search.setSize(w - w / 3, 60);
        scroll.setBounds(20, 120, w - 40, h - 120 - 90);
        actions.setBounds(20, h - 84, w - 40, 44);
        totals.setBounds(24, h - 38, w - 48, 24);
        revalidate();
        repaint();
    }

    private void flash(String msg) {
        status.setText(msg);
        javax.swing.Timer t = new javax.swing.Timer(4000, e -> status.setText(" "));
        t.setRepeats(false);
        t.start();
    }

    private void withSelection(java.util.function.Consumer<String> action) {
        int r = table.getSelectedRow();
        if (r < 0 || r >= shown.size()) {
            flash("Select a supplier in the list first.");
            return;
        }
        action.accept(shown.get(r));
    }

    private Set<String> names() {
        Set<String> s = new HashSet<>();
        for (Object[] r : rows) s.add(key(String.valueOf(r[0])));
        return s;
    }

    private double owed(String name) {
        for (Object[] r : rows) if (key(String.valueOf(r[0])).equals(key(name))) return (Double) r[2] - (Double) r[3];
        return 0;
    }

    // ------------------------------------------------------------------ data

    private void load() {
        new SwingWorker<List<Object[]>, Void>() {
            @Override protected List<Object[]> doInBackground() {
                EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
                try {
                    Map<String, Object[]> map = new TreeMap<>();
                    for (Supplier s : em.createQuery("FROM Supplier s WHERE s.deleted=false", Supplier.class).getResultList()) {
                        map.put(key(s.getName()), new Object[]{s.getName(), s.getPhone() == null ? "" : s.getPhone(), 0.0, 0.0});
                    }
                    for (Object o : em.createQuery(
                            "SELECT p.supplierName, COALESCE(SUM(p.total),0), COALESCE(SUM(COALESCE(p.paidAmount, p.total)),0) "
                                    + "FROM Purchase p WHERE p.deleted=false AND p.supplierName IS NOT NULL GROUP BY p.supplierName")
                            .getResultList()) {
                        Object[] r = (Object[]) o;
                        Object[] e = map.computeIfAbsent(key((String) r[0]), k -> new Object[]{r[0], "", 0.0, 0.0});
                        e[2] = (Double) e[2] + ((Number) r[1]).doubleValue();
                        e[3] = (Double) e[3] + ((Number) r[2]).doubleValue();
                    }
                    for (Object o : em.createQuery(
                            "SELECT sp.supplierName, COALESCE(SUM(sp.amount),0) FROM SupplierPayment sp "
                                    + "WHERE sp.deleted=false GROUP BY sp.supplierName").getResultList()) {
                        Object[] r = (Object[]) o;
                        Object[] e = map.computeIfAbsent(key((String) r[0]), k -> new Object[]{r[0], "", 0.0, 0.0});
                        e[3] = (Double) e[3] + ((Number) r[1]).doubleValue();
                    }
                    return new ArrayList<>(map.values());
                } finally {
                    em.close();
                }
            }
            @Override protected void done() {
                try {
                    rows.clear();
                    rows.addAll(get());
                    render();
                } catch (Exception ex) {
                    flash("Could not load suppliers: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void render() {
        String q = search.getText().trim().toLowerCase();
        model.setRowCount(0);
        shown.clear();
        double owedTotal = 0;
        for (Object[] r : rows) {
            String name = String.valueOf(r[0]);
            if (!q.isEmpty() && !name.toLowerCase().contains(q) && !String.valueOf(r[1]).toLowerCase().contains(q)) continue;
            double purchased = (Double) r[2], paid = (Double) r[3], owed = purchased - paid;
            owedTotal += Math.max(0, owed);
            shown.add(name);
            model.addRow(new Object[]{name, r[1], money(purchased), money(paid), money(owed)});
        }
        totals.setText(String.format("%d suppliers   |   Total owed to suppliers: %,.2f", model.getRowCount(), owedTotal));
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
        ExcelExporter.exportToExcel(this, "Suppliers-" + java.time.LocalDate.now(), h, out);
    }
}
