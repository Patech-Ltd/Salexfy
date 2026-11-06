package org.PSmart.POS.ui.MainView.ui.stock.lists;

import jakarta.persistence.EntityManager;
import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.Item;
import org.PSmart.POS.Repositories.ItemRepository;
import org.PSmart.POS.Utils.Components.forms.ListPage;
import org.PSmart.POS.Utils.Components.forms.Ui;
import org.PSmart.POS.Utils.StockService;
import org.PSmart.POS.ui.MainView.ui.stock.Items.popups.EditItem;
import org.PSmart.POS.ui.MainView.ui.stock.StockPanel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.*;
import java.util.List;

/** All Items: one scannable list with price, margin and stock; Add / Edit open the item form. */
public class ItemsListPanel extends ListPage {

    private final StockPanel stockPanel;
    private final List<Object[]> all = new ArrayList<>();
    private EditItem editor;

    public ItemsListPanel(StockPanel stockPanel) {
        super("+   Add New Item", "Search item (name, barcode, category)",
                new String[]{"Product", "Category", "Barcode", "Cost", "Price", "Margin", "On Hand", "Status"});
        this.stockPanel = stockPanel;
        setLocation(0, 0);
        setSize(stockPanel.getPlayground().getSize());
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        right.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        for (int c : new int[]{3, 4, 5, 6}) table.getColumnModel().getColumn(c).setCellRenderer(right);
        table.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
                if (!s) comp.setForeground("OK".equals(String.valueOf(v)) ? new Color(30, 110, 50) : Color.RED);
                return comp;
            }
        });
        JButton edit = Ui.primary("Edit Item");
        edit.addActionListener(e -> onOpen());
        actions.add(edit);
        actions.add(status);
        addBar.addActionListener(e -> openEditor(null));
        layoutUi();
        SwingUtilities.invokeLater(this::load);
    }

    private void openEditor(UUID id) {
        try {
            if (editor != null) editor.dispose();
            Item item = id == null ? null : ItemRepository.findById(id);
            editor = new EditItem(item, stockPanel.getDashBoard().getDbsession(), stockPanel.getDashBoard().getLoggedUser());
            editor.setSaveAction(this::load);
        } catch (Exception ex) {
            flash("Could not open the item: " + ex.getMessage());
        }
    }

    void load() {
        new SwingWorker<List<Object[]>, Void>() {
            @Override protected List<Object[]> doInBackground() {
                EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
                try {
                    Map<UUID, Double> oh = StockService.onHandAll(em);
                    List<Object[]> out = new ArrayList<>();
                    for (Object o : em.createQuery("SELECT i.uid, i.name, c.name, i.barcode, i.buyingPrice, i.sellingPrice, i.reorderLevel "
                            + "FROM Item i LEFT JOIN i.category c WHERE i.deleted=false ORDER BY i.name").getResultList()) {
                        Object[] r = (Object[]) o;
                        out.add(new Object[]{r[0], r[1], r[2] == null ? "" : r[2], r[3] == null ? "" : r[3],
                                r[4] == null ? 0.0 : ((Number) r[4]).doubleValue(), r[5] == null ? 0.0 : ((Number) r[5]).doubleValue(),
                                r[6] == null ? 0.0 : ((Number) r[6]).doubleValue(), oh.getOrDefault((UUID) r[0], 0.0)});
                    }
                    return out;
                } finally {
                    em.close();
                }
            }
            @Override protected void done() {
                try { all.clear(); all.addAll(get()); render(); } catch (Exception ex) { flash("Could not load items: " + ex.getMessage()); }
            }
        }.execute();
    }

    @Override protected void render() {
        String q = search.getText().trim().toLowerCase();
        model.setRowCount(0);
        ids.clear();
        int low = 0;
        for (Object[] r : all) {
            String hay = (r[1] + " " + r[2] + " " + r[3]).toLowerCase();
            if (!q.isEmpty() && !hay.contains(q)) continue;
            double cost = (Double) r[4], price = (Double) r[5], reorder = (Double) r[6], oh = (Double) r[7];
            String st = oh <= 0 ? "OUT" : (reorder > 0 && oh < reorder ? "LOW" : "OK");
            if (!"OK".equals(st)) low++;
            ids.add(r[0]);
            model.addRow(new Object[]{r[1], r[2], r[3], String.format("%,.2f", cost), String.format("%,.2f", price),
                    price > 0 ? String.format("%.0f%%", (price - cost) * 100 / price) : "-",
                    oh == Math.floor(oh) ? String.format("%,.0f", oh) : String.format("%,.2f", oh), st});
        }
        footer.setText(model.getRowCount() + " items   |   " + low + " low or out of stock");
    }

    @Override protected void onOpen() {
        Object id = selectedId();
        if (id != null) openEditor((UUID) id);
    }
}
