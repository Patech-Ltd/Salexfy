package org.PSmart.POS.ui.MainView.ui.stock.current;

import jakarta.persistence.EntityManager;
import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.StockMovement;
import org.PSmart.POS.Utils.ExcelExporter;
import org.PSmart.POS.Utils.Components.PTextField;
import org.PSmart.POS.Utils.Components.forms.Ui;
import org.PSmart.POS.Utils.IconsUtils;
import org.PSmart.POS.ui.MainView.ui.stock.StockPanel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/** Audit history of manual adjustments and stock-take variances (latest 1000). */
public class StockMovementsPanel extends JPanel {

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Date", "Product", "Type", "Change", "Before", "After", "Reason", "Ref", "By", "Notes"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JComboBox<String> type = new JComboBox<>(new String[]{"All", "ADJUSTMENT", "STOCK_TAKE"});
    private final JTextField search = new JTextField(16);
    private PTextField searchField;
    private final JLabel status = new JLabel(" ");
    private final List<UUID> ids = new ArrayList<>();
    private final StockPanel stockPanel;
    private JTable table;

    public StockMovementsPanel(StockPanel stockPanel) {
        this.stockPanel = stockPanel;
        setLocation(0, 0);
        setSize(stockPanel.getPlayground().getSize());
        setLayout(null);
        setBackground(Ui.PAGE);
        int w = getWidth(), h = getHeight();

        PTextField searchBox = new PTextField(Ui.SLATE, new Color(0, 0, 255), "Search Product",
                IconsUtils.getIcon("search.png"));
        searchBox.setLocation(w / 6, 10);
        searchBox.setSize(w - w / 3, 60);
        add(searchBox);
        searchField = searchBox;

        JLabel tl = new JLabel("Type:");
        tl.setFont(new Font("Courier", Font.BOLD, 13));
        tl.setBounds(24, 80, 50, 26);
        add(tl);
        type.setBounds(76, 78, 160, 30);
        add(type);
        JButton go = Ui.primary("Refresh");
        JButton export = Ui.neutral("Export Excel");
        JButton open = Ui.primary("Open / Cancel entry");
        open.setBounds(w - 20 - 150 - 160 - 210, 74, 200, 32);
        open.addActionListener(e -> openSelected());
        add(open);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { if (e.getClickCount() == 2) openSelected(); }
        });
        go.setBounds(w - 20 - 150 - 160, 74, 150, 32);
        export.setBounds(w - 20 - 150, 74, 150, 32);
        add(go);
        add(export);

        table = new JTable(model);
        Ui.styleTable(table);
        JScrollPane sp = new JScrollPane(table);
        sp.setBounds(20, 112, w - 40, h - 112 - 40);
        add(sp);
        status.setFont(new Font("Courier", Font.BOLD, 13));
        status.setForeground(Color.RED);
        status.setBounds(24, h - 30, w - 48, 22);
        add(status);

        go.addActionListener(e -> load());
        type.addActionListener(e -> load());
        searchBox.getField().addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyReleased(java.awt.event.KeyEvent e) { load(); }
        });
        export.addActionListener(e -> {
            String[] hd = new String[model.getColumnCount()];
            for (int i = 0; i < hd.length; i++) hd[i] = model.getColumnName(i);
            List<Object[]> rows = new ArrayList<>();
            for (int r = 0; r < model.getRowCount(); r++) {
                Object[] row = new Object[hd.length];
                for (int c = 0; c < hd.length; c++) row[c] = model.getValueAt(r, c);
                rows.add(row);
            }
            ExcelExporter.exportToExcel(this, "Stock-Movements-" + java.time.LocalDate.now(), hd, rows);
        });
        SwingUtilities.invokeLater(this::load);
    }

    private void load() {
        final String t = String.valueOf(type.getSelectedItem());
        final String q = searchField == null ? "" : searchField.getText().trim().toLowerCase();
        new SwingWorker<List<StockMovement>, Void>() {
            @Override protected List<StockMovement> doInBackground() {
                EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
                try {
                    return em.createQuery("SELECT m FROM StockMovement m JOIN FETCH m.item WHERE m.deleted=false "
                            + ("All".equals(t) ? "" : "AND m.type = :t ") + "ORDER BY m.createdAt DESC", StockMovement.class)
                            .setParameter("t", t).setMaxResults(1000).getResultList();
                } catch (IllegalArgumentException noParam) {
                    return em.createQuery("SELECT m FROM StockMovement m JOIN FETCH m.item WHERE m.deleted=false "
                            + "ORDER BY m.createdAt DESC", StockMovement.class).setMaxResults(1000).getResultList();
                } finally {
                    em.close();
                }
            }
            @Override protected void done() {
                try {
                    SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                    model.setRowCount(0);
                    ids.clear();
                    for (StockMovement m : get()) {
                        String name = m.getItem() == null ? "" : m.getItem().getName();
                        if (!q.isEmpty() && (name == null || !name.toLowerCase().contains(q))) continue;
                        double d = m.getQuantityDelta() == null ? 0 : m.getQuantityDelta();
                        ids.add(m.getUid());
                        model.addRow(new Object[]{f.format(new Date(m.getCreatedAt() == null ? 0 : m.getCreatedAt())),
                                name, m.getType(), (d > 0 ? "+" : "") + trim(d), trim(m.getQuantityBefore()),
                                trim(m.getQuantityAfter()), m.getReason(), m.getReference(), m.getCreatedByName(), m.getNotes()});
                    }
                } catch (Exception ex) {
                    status.setText("Could not load movements: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void openSelected() {
        int r = table.getSelectedRow();
        if (r < 0 || r >= ids.size()) { status.setText("Select an entry in the list first."); return; }
        StockMovementViewForm.open(ids.get(r), stockPanel.getDashBoard().getLoggedUser(), this::load);
    }

    private static String trim(Double v) {
        if (v == null) return "";
        return v == Math.floor(v) ? String.format("%.0f", v) : String.format("%.2f", v);
    }
}
