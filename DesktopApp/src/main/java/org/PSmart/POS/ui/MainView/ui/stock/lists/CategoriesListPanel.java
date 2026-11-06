package org.PSmart.POS.ui.MainView.ui.stock.lists;

import jakarta.persistence.EntityManager;
import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.ItemCategory;
import org.PSmart.POS.Utils.Components.forms.ListPage;
import org.PSmart.POS.Utils.Components.forms.Ui;
import org.PSmart.POS.Utils.EditItemRunnable;
import org.PSmart.POS.ui.MainView.ui.stock.Categories.popups.EditCategory;
import org.PSmart.POS.ui.MainView.ui.stock.StockPanel;

import javax.swing.*;
import java.util.*;

/** Categories with how many items each holds. */
public class CategoriesListPanel extends ListPage {

    private final StockPanel stockPanel;
    private final List<Object[]> all = new ArrayList<>();
    private EditCategory editor;

    public CategoriesListPanel(StockPanel stockPanel) {
        super("+   Add New Category", "Search category", new String[]{"Category", "Description", "Items"});
        this.stockPanel = stockPanel;
        setLocation(0, 0);
        setSize(stockPanel.getPlayground().getSize());
        JButton edit = Ui.primary("Edit Category");
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
            ItemCategory cat = null;
            if (id != null) {
                EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
                try { cat = em.find(ItemCategory.class, id); } finally { em.close(); }
            }
            editor = new EditCategory(cat, stockPanel.getDashBoard().getDbsession(), stockPanel.getDashBoard().getLoggedUser());
            editor.setEditCategoryRunnable(new EditItemRunnable() {
                @Override public void run() { load(); }
            });
        } catch (Exception ex) {
            flash("Could not open the category: " + ex.getMessage());
        }
    }

    void load() {
        new SwingWorker<List<Object[]>, Void>() {
            @Override protected List<Object[]> doInBackground() {
                EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
                try {
                    List<Object[]> out = new ArrayList<>();
                    for (Object o : em.createQuery("SELECT c.uid, c.name, c.Description, "
                            + "(SELECT COUNT(i) FROM Item i WHERE i.category = c AND i.deleted=false) "
                            + "FROM ItemCategory c WHERE c.deleted=false ORDER BY c.name").getResultList()) out.add((Object[]) o);
                    return out;
                } finally {
                    em.close();
                }
            }
            @Override protected void done() {
                try { all.clear(); all.addAll(get()); render(); } catch (Exception ex) { flash("Could not load categories: " + ex.getMessage()); }
            }
        }.execute();
    }

    @Override protected void render() {
        String q = search.getText().trim().toLowerCase();
        model.setRowCount(0);
        ids.clear();
        for (Object[] r : all) {
            if (!q.isEmpty() && !(r[1] + " " + r[2]).toLowerCase().contains(q)) continue;
            ids.add(r[0]);
            model.addRow(new Object[]{r[1], r[2] == null ? "" : r[2], r[3]});
        }
        footer.setText(model.getRowCount() + " categories");
    }

    @Override protected void onOpen() {
        Object id = selectedId();
        if (id != null) openEditor((UUID) id);
    }
}
