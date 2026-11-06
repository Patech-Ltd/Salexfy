package org.PSmart.POS.ui.MainView.ui.stock.current;

import jakarta.persistence.EntityManager;
import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.StockMovement;
import org.PSmart.POS.Entities.User;
import org.PSmart.POS.Utils.Components.PTextField;
import org.PSmart.POS.Utils.Components.forms.Ui;
import org.PSmart.POS.Utils.IconsUtils;
import org.PSmart.POS.Utils.StockService;
import org.PSmart.POS.ui.MainView.ui.stock.StockPanel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * Physical stock count. Type the counted quantity per product (blank = not counted).
 * "Review" shows the variance summary; "Confirm" books one STOCK_TAKE movement per variance.
 */
public class StockTakePanel extends JPanel {

    private static final int COL_COUNTED = 2;

    private final StockPanel stockPanel;
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Product", "System Qty", "Counted", "Variance"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return c == COL_COUNTED; }
    };
    private final JTable table = new JTable(model);
    private final PTextField search = new PTextField(Ui.SLATE, new Color(0, 0, 255), "Search Product",
            IconsUtils.getIcon("search.png"));
    private final JLabel summary = new JLabel(" ");
    private final JLabel status = new JLabel(" ");
    private final JButton apply = Ui.primary("Review Variances");
    private final JButton restart = Ui.neutral("Start New Count");
    private final JButton cancelReview = Ui.neutral("Back to Count");

    private final List<Object[]> items = new ArrayList<>();   // {id, name, systemQty}
    private final List<UUID> ids = new ArrayList<>();
    private final List<Double> system = new ArrayList<>();
    private final Map<UUID, String> counted = new HashMap<>();
    private boolean reviewing;
    private boolean updating;

    public StockTakePanel(StockPanel stockPanel) {
        this.stockPanel = stockPanel;
        setLocation(0, 0);
        setSize(stockPanel.getPlayground().getSize());
        setLayout(null);
        setBackground(Ui.PAGE);
        int w = getWidth(), h = getHeight();

        search.setLocation(w / 6, 10);
        search.setSize(w - w / 3, 60);
        add(search);

        JLabel hint = new JLabel("Type the quantity you counted. Leave blank for products you have not counted.");
        hint.setFont(new Font("Courier", Font.PLAIN, 13));
        hint.setBounds(24, 76, w - 520, 26);
        add(hint);
        restart.setBounds(w - 20 - 170, 74, 170, 32);
        add(restart);

        Ui.styleTable(table);
        table.getColumnModel().getColumn(COL_COUNTED).setCellEditor(org.PSmart.POS.Utils.Components.Inputs.decimalCellEditor(14));
        JScrollPane sp = new JScrollPane(table);
        sp.setBounds(20, 112, w - 40, h - 112 - 64);
        add(sp);

        summary.setFont(new Font("Courier", Font.BOLD, 14));
        summary.setBounds(24, h - 54, w - 600, 24);
        add(summary);
        status.setFont(new Font("Courier", Font.BOLD, 13));
        status.setForeground(Color.RED);
        status.setBounds(24, h - 30, w - 600, 22);
        add(status);
        apply.setBounds(w - 20 - 250, h - 54, 250, 38);
        cancelReview.setBounds(w - 20 - 250 - 190, h - 54, 180, 38);
        cancelReview.setVisible(false);
        add(apply);
        add(cancelReview);

        model.addTableModelListener(e -> {
            if (updating || e.getColumn() != COL_COUNTED || e.getFirstRow() < 0 || e.getFirstRow() >= ids.size()) return;
            int r = e.getFirstRow();
            Object val = model.getValueAt(r, COL_COUNTED);
            String v = val == null ? "" : String.valueOf(val).trim();
            if (v.isEmpty()) counted.remove(ids.get(r)); else counted.put(ids.get(r), v);
            disarm();
            refreshVariance(r);
            updateSummary();
        });
        search.getField().addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyReleased(java.awt.event.KeyEvent e) { render(); }
        });
        restart.addActionListener(e -> { counted.clear(); disarm(); load(); flash("New count started."); });
        cancelReview.addActionListener(e -> disarm());
        apply.addActionListener(e -> onApply());
        load();
    }

    private void flash(String msg) {
        status.setText(msg);
        javax.swing.Timer t = new javax.swing.Timer(5000, e -> status.setText(" "));
        t.setRepeats(false);
        t.start();
    }

    private void disarm() {
        reviewing = false;
        apply.setText("Review Variances");
        cancelReview.setVisible(false);
    }

    private void load() {
        new SwingWorker<List<Object[]>, Void>() {
            @Override protected List<Object[]> doInBackground() {
                EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
                try {
                    Map<UUID, Double> oh = StockService.onHandAll(em);
                    List<Object[]> out = new ArrayList<>();
                    for (Object o : em.createQuery("SELECT i.uid, i.name FROM Item i WHERE i.deleted=false ORDER BY i.name")
                            .getResultList()) {
                        Object[] r = (Object[]) o;
                        out.add(new Object[]{r[0], r[1], oh.getOrDefault((UUID) r[0], 0.0)});
                    }
                    return out;
                } finally {
                    em.close();
                }
            }
            @Override protected void done() {
                try {
                    items.clear();
                    items.addAll(get());
                    render();
                } catch (Exception ex) {
                    Throwable t = ex;
                    while (t.getCause() != null) t = t.getCause();
                    flash("Could not load products: " + t.getMessage());
                }
            }
        }.execute();
    }

    private void render() {
        String q = search.getText().trim().toLowerCase();
        updating = true;
        model.setRowCount(0);
        ids.clear();
        system.clear();
        for (Object[] it : items) {
            if (!q.isEmpty() && !String.valueOf(it[1]).toLowerCase().contains(q)) continue;
            UUID id = (UUID) it[0];
            ids.add(id);
            system.add((Double) it[2]);
            model.addRow(new Object[]{it[1], fmt((Double) it[2]), counted.getOrDefault(id, ""), ""});
        }
        updating = false;
        for (int r = 0; r < ids.size(); r++) refreshVariance(r);
        updateSummary();
    }

    private static Double parsed(String s) {
        try {
            double v = Double.parseDouble(s.trim().replace(",", ""));
            return v < 0 ? null : v;
        } catch (Exception e) {
            return null;
        }
    }

    private void refreshVariance(int r) {
        String c = counted.get(ids.get(r));
        Double v = c == null ? null : parsed(c);
        String text = c == null ? "" : v == null ? "invalid" : fmtSigned(v - system.get(r));
        updating = true;
        model.setValueAt(text, r, 3);
        updating = false;
    }

    private List<UUID> variances() {
        List<UUID> todo = new ArrayList<>();
        for (Object[] it : items) {
            String c = counted.get(it[0]);
            Double v = c == null ? null : parsed(c);
            if (v != null && Math.abs(v - (Double) it[2]) > 1e-9) todo.add((UUID) it[0]);
        }
        return todo;
    }

    private void updateSummary() {
        int bad = 0;
        for (String v : counted.values()) if (parsed(v) == null) bad++;
        summary.setText("Counted " + counted.size() + " of " + items.size() + "   |   With variance: "
                + variances().size() + (bad > 0 ? "   |   INVALID entries: " + bad : ""));
    }

    private void onApply() {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        for (String v : counted.values()) {
            if (parsed(v) == null) {
                flash("Fix the invalid counted quantities first.");
                return;
            }
        }
        List<UUID> todo = variances();
        if (todo.isEmpty()) {
            flash("No variances to book - every counted product matches the system.");
            return;
        }
        if (!reviewing) {
            reviewing = true;
            apply.setText("Confirm & Book " + todo.size());
            cancelReview.setVisible(true);
            flash("Review the variances, then press Confirm to update stock.");
            return;
        }
        final User user = stockPanel.getDashBoard().getLoggedUser();
        final User approver = org.PSmart.POS.Utils.SupervisorApproval.require(this, user, org.PSmart.POS.Entities.RoleType.STOCK, "book a stock take");
        if (approver == null) { flash("Booking a stock take needs a supervisor's approval."); return; }
        final String ref = "ST-" + new SimpleDateFormat("yyyyMMdd-HHmm").format(new Date());
        int ok = 0, failed = 0;
        for (UUID id : todo) {
            try {
                EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
                double current;
                try { current = StockService.onHand(em, id); } finally { em.close(); }
                double delta = parsed(counted.get(id)) - current;   // against the LIVE quantity
                if (Math.abs(delta) > 1e-9) {
                    StockService.record(id, StockMovement.STOCK_TAKE, delta, "Stock take",
                            approver.getUid().equals(user == null ? null : user.getUid()) ? null : "approved by " + approver.getFullname(), ref, user);
                }
                ok++;
            } catch (Exception ex) {
                failed++;
            }
        }
        Ui.toast("Stock take " + ref + ": " + ok + " booked" + (failed > 0 ? ", " + failed + " failed" : ""));
        counted.clear();
        disarm();
        load();
        flash(failed > 0 ? failed + " failed - run the count again to retry." : "Stock take " + ref + " booked.");
    }

    private static String fmt(double v) {
        return v == Math.floor(v) ? String.format("%,.0f", v) : String.format("%,.2f", v);
    }

    private static String fmtSigned(double v) {
        return (v > 0 ? "+" : "") + fmt(v);
    }
}
