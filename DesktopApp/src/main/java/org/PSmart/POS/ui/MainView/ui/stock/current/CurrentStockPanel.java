package org.PSmart.POS.ui.MainView.ui.stock.current;

import jakarta.persistence.EntityManager;
import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.User;
import org.PSmart.POS.Utils.Components.PTextField;
import org.PSmart.POS.Utils.Components.forms.Ui;
import org.PSmart.POS.Utils.ExcelExporter;
import org.PSmart.POS.Utils.IconsUtils;
import org.PSmart.POS.Utils.StockService;
import org.PSmart.POS.ui.MainView.ui.stock.StockPanel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

/** Live stock levels. Select a row and press Adjust Stock (or double-click) to open the form. */
public class CurrentStockPanel extends JPanel {

    private final StockPanel stockPanel;
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Product", "Category", "On Hand", "Reorder Level", "Status", "Cost Value", "Retail Value"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final PTextField search = new PTextField(Ui.SLATE, new Color(0, 0, 255), "Search Product",
            IconsUtils.getIcon("search.png"));
    private final JCheckBox lowOnly = new JCheckBox("Show only low / out of stock");
    private final JLabel totals = new JLabel(" ");
    private final JLabel kProducts = new JLabel("-"), kCost = new JLabel("-"), kRetail = new JLabel("-"), kAttention = new JLabel("-");
    private final JLabel status = new JLabel(" ");
    private final List<UUID> rowIds = new ArrayList<>();
    private final List<Object[]> all = new ArrayList<>();  // {id, name, cat, onHand, reorder, cost, sell}

    public CurrentStockPanel(StockPanel stockPanel) {
        this.stockPanel = stockPanel;
        setLocation(0, 0);
        setSize(stockPanel.getPlayground().getSize());
        setLayout(null);
        setBackground(Ui.PAGE);
        int w = getWidth(), h = getHeight();

        JPanel cards = new JPanel(new GridLayout(1, 4, 14, 0));
        cards.setOpaque(false);
        cards.add(card("Products", kProducts, new Color(0, 0, 137)));
        cards.add(card("Stock value at cost", kCost, new Color(0, 0, 137)));
        cards.add(card("Stock value at selling price", kRetail, new Color(0, 0, 137)));
        cards.add(card("Low or out of stock", kAttention, new Color(200, 30, 45)));
        cards.setBounds(20, 12, w - 40, 76);
        add(cards);

        search.setLocation(20, 96);
        search.setSize(Math.max(300, w / 3), 60);
        add(search);

        lowOnly.setOpaque(false);
        lowOnly.setFont(new Font("Courier", Font.BOLD, 13));
        lowOnly.setBounds(Math.max(300, w / 3) + 40, 124, 320, 26);
        add(lowOnly);

        JButton adjust = Ui.primary("Adjust Stock");
        JButton export = Ui.neutral("Export Excel");
        adjust.setBounds(w - 20 - 150 - 160, 120, 150, 32);
        export.setBounds(w - 20 - 150, 120, 150, 32);
        add(adjust);
        add(export);

        Ui.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        right.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        for (int c : new int[]{2, 3, 5, 6}) table.getColumnModel().getColumn(c).setCellRenderer(right);
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
                if (!s) comp.setForeground("OK".equals(String.valueOf(v)) ? new Color(30, 110, 50) : Color.RED);
                return comp;
            }
        });
        JScrollPane sp = new JScrollPane(table);
        sp.setBounds(20, 164, w - 40, h - 164 - 60);
        add(sp);

        totals.setFont(new Font("Courier", Font.BOLD, 14));
        totals.setBounds(24, h - 50, w - 300, 24);
        add(totals);
        status.setFont(new Font("Courier", Font.BOLD, 13));
        status.setForeground(Color.RED);
        status.setBounds(w - 420, h - 50, 400, 24);
        status.setHorizontalAlignment(SwingConstants.RIGHT);
        add(status);

        adjust.addActionListener(e -> adjustSelected());
        export.addActionListener(e -> export());
        lowOnly.addActionListener(e -> applyFilter());
        search.getField().addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyReleased(java.awt.event.KeyEvent e) { applyFilter(); }
        });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) adjustSelected();
            }
        });
        SwingUtilities.invokeLater(this::reload);
    }

    private JPanel card(String caption, JLabel value, Color accent) {
        JPanel c = new JPanel(new BorderLayout());
        c.setBackground(Color.WHITE);
        c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 5, 0, 0, accent),
                BorderFactory.createEmptyBorder(8, 14, 8, 10)));
        JLabel cap = new JLabel(caption);
        cap.setFont(new Font("Courier", Font.PLAIN, 12));
        cap.setForeground(new Color(108, 117, 125));
        value.setFont(new Font("Courier", Font.BOLD, 24));
        value.setForeground(accent);
        c.add(cap, BorderLayout.NORTH);
        c.add(value, BorderLayout.CENTER);
        return c;
    }

    private User user() { return stockPanel.getDashBoard().getLoggedUser(); }

    private void flash(String msg) {
        status.setText(msg);
        javax.swing.Timer t = new javax.swing.Timer(4000, e -> status.setText(" "));
        t.setRepeats(false);
        t.start();
    }

    void reload() {
        new SwingWorker<List<Object[]>, Void>() {
            @Override protected List<Object[]> doInBackground() {
                EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
                try {
                    Map<UUID, Double> onHand = StockService.onHandAll(em);
                    List<Object[]> out = new ArrayList<>();
                    for (Object o : em.createQuery(
                            "SELECT i.uid, i.name, c.name, i.reorderLevel, i.buyingPrice, i.sellingPrice "
                                    + "FROM Item i LEFT JOIN i.category c WHERE i.deleted=false ORDER BY i.name").getResultList()) {
                        Object[] r = (Object[]) o;
                        out.add(new Object[]{r[0], r[1], r[2] == null ? "" : r[2],
                                onHand.getOrDefault((UUID) r[0], 0.0),
                                r[3] == null ? 0.0 : ((Number) r[3]).doubleValue(),
                                r[4] == null ? 0.0 : ((Number) r[4]).doubleValue(),
                                r[5] == null ? 0.0 : ((Number) r[5]).doubleValue()});
                    }
                    return out;
                } finally {
                    em.close();
                }
            }
            @Override protected void done() {
                try {
                    all.clear();
                    all.addAll(get());
                    applyFilter();
                } catch (Exception ex) {
                    flash("Could not load stock: " + root(ex));
                }
            }
        }.execute();
    }

    private static String root(Throwable t) {
        while (t.getCause() != null) t = t.getCause();
        return t.getMessage();
    }

    private static String status(double onHand, double reorder) {
        return onHand <= 0 ? "OUT" : (reorder > 0 && onHand < reorder ? "LOW" : "OK");
    }

    private void updateCards() {
        double cost = 0, retail = 0;
        int attention = 0;
        for (Object[] r : all) {
            double oh = (Double) r[3], ro = (Double) r[4];
            cost += Math.max(0, oh) * (Double) r[5];
            retail += Math.max(0, oh) * (Double) r[6];
            if (!"OK".equals(status(oh, ro))) attention++;
        }
        kProducts.setText(String.valueOf(all.size()));
        kCost.setText(String.format("%,.2f", cost));
        kRetail.setText(String.format("%,.2f", retail));
        kAttention.setText(String.valueOf(attention));
    }

    private void applyFilter() {
        updateCards();
        String q = search.getText().trim().toLowerCase();
        model.setRowCount(0);
        rowIds.clear();
        double cost = 0, retail = 0;
        int low = 0;
        for (Object[] r : all) {
            double oh = (Double) r[3], ro = (Double) r[4];
            String st = status(oh, ro);
            if (!q.isEmpty() && !String.valueOf(r[1]).toLowerCase().contains(q)
                    && !String.valueOf(r[2]).toLowerCase().contains(q)) continue;
            if (lowOnly.isSelected() && "OK".equals(st)) continue;
            double cv = Math.max(0, oh) * (Double) r[5], rv = Math.max(0, oh) * (Double) r[6];
            cost += cv;
            retail += rv;
            if (!"OK".equals(st)) low++;
            rowIds.add((UUID) r[0]);
            model.addRow(new Object[]{r[1], r[2], fmt(oh), fmt(ro), st,
                    String.format("%,.2f", cv), String.format("%,.2f", rv)});
        }
        totals.setText(String.format("%d items  |  %d low/out  |  Stock cost %,.2f  |  Retail value %,.2f",
                model.getRowCount(), low, cost, retail));
    }

    private static String fmt(double v) {
        return v == Math.floor(v) ? String.format("%,.0f", v) : String.format("%,.2f", v);
    }

    private void adjustSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= rowIds.size()) {
            flash("Select a product in the list first.");
            return;
        }
        UUID id = rowIds.get(row);
        double current = 0;
        for (Object[] r : all) if (id.equals(r[0])) current = (Double) r[3];
        new AdjustStockForm(id, String.valueOf(model.getValueAt(row, 0)), current, user(), this::reload);
    }

    private void export() {
        String[] h = new String[model.getColumnCount()];
        for (int i = 0; i < h.length; i++) h[i] = model.getColumnName(i);
        List<Object[]> rows = new ArrayList<>();
        for (int r = 0; r < model.getRowCount(); r++) {
            Object[] row = new Object[h.length];
            for (int c = 0; c < h.length; c++) row[c] = model.getValueAt(r, c);
            rows.add(row);
        }
        ExcelExporter.exportToExcel(this, "Current-Stock-" + java.time.LocalDate.now(), h, rows);
    }
}
