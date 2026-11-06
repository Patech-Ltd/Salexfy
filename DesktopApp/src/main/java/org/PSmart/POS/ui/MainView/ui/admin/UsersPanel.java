package org.PSmart.POS.ui.MainView.ui.admin;

import jakarta.persistence.EntityManager;
import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.User;
import org.PSmart.POS.Utils.Components.forms.ListPage;
import org.PSmart.POS.Utils.Components.forms.Ui;

import javax.swing.*;
import java.util.*;

/** Users list: who can sign in, their role and commission rate. */
final class UsersPanel extends ListPage {

    private final User logged;
    private final List<Object[]> all = new ArrayList<>();

    UsersPanel(User logged) {
        super("+   Add New User", "Search user", new String[]{"Name", "Username", "Phone", "Role", "Commission", "Status"});
        this.logged = logged;
        JButton edit = Ui.primary("Open User");
        edit.addActionListener(e -> onOpen());
        actions.add(edit);
        actions.add(status);
        addBar.addActionListener(e -> new UserForm(null, logged, this::load));
        SwingUtilities.invokeLater(this::load);
    }

    void load() {
        new SwingWorker<List<Object[]>, Void>() {
            @Override protected List<Object[]> doInBackground() {
                EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
                try {
                    List<Object[]> out = new ArrayList<>();
                    for (Object o : em.createQuery("SELECT u.uid, u.fullname, u.username, u.phone, u.roleProfile, u.commissionRate, u.deleted "
                            + "FROM User u ORDER BY u.deleted, u.fullname").getResultList()) out.add((Object[]) o);
                    return out;
                } finally {
                    em.close();
                }
            }
            @Override protected void done() {
                try { all.clear(); all.addAll(get()); render(); } catch (Exception ex) { flash("Could not load users: " + ex.getMessage()); }
            }
        }.execute();
    }

    @Override protected void render() {
        String q = search.getText().trim().toLowerCase();
        model.setRowCount(0);
        ids.clear();
        int active = 0;
        for (Object[] u : all) {
            String hay = (u[1] + " " + u[2] + " " + u[3]).toLowerCase();
            if (!q.isEmpty() && !hay.contains(q)) continue;
            boolean off = Boolean.TRUE.equals(u[6]);
            if (!off) active++;
            ids.add(u[0]);
            model.addRow(new Object[]{u[1], u[2], u[3] == null ? "" : u[3], u[4] == null ? "(no role set)" : u[4],
                    u[5] == null ? "store default" : u[5] + " %", off ? "Inactive" : "Active"});
        }
        footer.setText(active + " active user(s)");
    }

    @Override protected void onOpen() {
        Object id = selectedId();
        if (id != null) new UserDetailForm((UUID) id, logged, this::load);
    }
}
