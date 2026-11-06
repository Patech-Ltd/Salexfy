package org.PSmart.POS.ui.MainView.ui.stock.lists;

import jakarta.persistence.EntityManager;
import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Utils.Components.forms.ListPage;
import org.PSmart.POS.ui.MainView.ui.stock.StockPanel;
import org.PSmart.POS.ui.MainView.ui.stock.stockfeed.popup.FeedStockFrame;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.text.SimpleDateFormat;
import java.util.*;

/** History of stock received (goods-in). "+ Receive Stock" opens the receiving form. */
public class StockReceivedPanel extends ListPage {

    private final StockPanel stockPanel;
    private final List<Object[]> all = new ArrayList<>();
    private FeedStockFrame frame;
    private JCheckBox showCancelled;

    public StockReceivedPanel(StockPanel stockPanel) {
        super("+   Receive Stock", "Search by note or reference", new String[]{"Date", "Reference", "Lines", "Quantity", "Cost value", "Note"});
        this.stockPanel = stockPanel;
        setLocation(0, 0);
        setSize(stockPanel.getPlayground().getSize());
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        right.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        for (int c : new int[]{2, 3, 4}) table.getColumnModel().getColumn(c).setCellRenderer(right);
        actions.add(status);
        addBar.addActionListener(e -> {
            new org.PSmart.POS.ui.MainView.ui.stock.receive.ReceiveStockForm(
                    stockPanel.getDashBoard().getLoggedUser(), this::load);
        });
        JButton open = org.PSmart.POS.Utils.Components.forms.Ui.primary("Open / Edit / Cancel");
        open.setPreferredSize(new java.awt.Dimension(220, 34));
        open.addActionListener(e -> onOpen());
        actions.add(open, 0);
        showCancelled = new JCheckBox("Show cancelled");
        showCancelled.setOpaque(false);
        showCancelled.setFont(new java.awt.Font("Courier", java.awt.Font.BOLD, 13));
        showCancelled.addActionListener(e -> load());
        actions.add(showCancelled, 1);
        JButton refresh = org.PSmart.POS.Utils.Components.forms.Ui.primary("Refresh");
        refresh.addActionListener(e -> load());
        actions.add(refresh, 0);
        layoutUi();
        SwingUtilities.invokeLater(this::load);
    }

    void load() {
        new SwingWorker<List<Object[]>, Void>() {
            @Override protected List<Object[]> doInBackground() {
                return org.PSmart.POS.Repositories.TxnService.feedRows(showCancelled != null && showCancelled.isSelected());
            }
            @Override protected void done() {
                try { all.clear(); all.addAll(get()); render(); } catch (Exception ex) { flash("Could not load stock received: " + ex.getMessage()); }
            }
        }.execute();
    }

    @Override protected void render() {
        String q = search.getText().trim().toLowerCase();
        model.setRowCount(0);
        ids.clear();
        SimpleDateFormat f = new SimpleDateFormat("dd-MMM-yyyy HH:mm");
        double value = 0;
        for (Object[] r : all) {
            String ref = "REC-" + r[0].toString().substring(0, 8).toUpperCase() + (r.length > 6 && r[6] != null || (showCancelled != null && showCancelled.isSelected()) ? " (cancelled)" : "");
            String note = r[2] == null ? "" : String.valueOf(r[2]);
            if (!q.isEmpty() && !(ref + " " + note).toLowerCase().contains(q)) continue;
            double qty = ((Number) r[4]).doubleValue(), cost = ((Number) r[5]).doubleValue();
            value += cost;
            ids.add(r[0]);
            model.addRow(new Object[]{r[1] == null ? "" : f.format(new Date(((Number) r[1]).longValue())), ref, r[3],
                    qty == Math.floor(qty) ? String.format("%,.0f", qty) : String.format("%,.2f", qty), String.format("%,.2f", cost), note});
        }
        footer.setText(model.getRowCount() + " deliveries   |   cost value " + String.format("%,.2f", value));
    }

    @Override protected void onOpen() {
        Object id = selectedId();
        if (id == null) return;
        StockReceivedViewForm.open((java.util.UUID) id, stockPanel.getDashBoard().getLoggedUser(), this::load);
    }
}
