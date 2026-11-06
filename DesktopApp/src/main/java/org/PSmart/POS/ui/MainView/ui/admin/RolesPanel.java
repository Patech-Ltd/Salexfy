package org.PSmart.POS.ui.MainView.ui.admin;

import jakarta.persistence.EntityManager;
import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.RoleProfile;
import org.PSmart.POS.Entities.RoleType;
import org.PSmart.POS.Entities.User;
import org.PSmart.POS.Utils.Components.forms.ListPage;
import org.PSmart.POS.Utils.Components.forms.Ui;
import org.PSmart.POS.Utils.RoleService;

import javax.swing.*;
import java.util.*;

/** Roles stored in the database: add your own, choose what each can view and change. */
final class RolesPanel extends ListPage {

    private final User logged;
    private final List<Object[]> all = new ArrayList<>();   // {uid, name, description, permissions, users}

    RolesPanel(User logged) {
        super("+   Add New Role", "Search role", new String[]{"Role", "What it is for", "Can open", "People"});
        this.logged = logged;
        JButton edit = Ui.primary("Edit Role");
        edit.addActionListener(e -> onOpen());
        actions.add(edit);
        actions.add(status);
        addBar.addActionListener(e -> new RoleForm(null, logged, this::load));
        SwingUtilities.invokeLater(this::load);
    }

    void load() {
        new SwingWorker<List<Object[]>, Void>() {
            @Override protected List<Object[]> doInBackground() {
                RoleService.seedDefaults();
                EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
                try {
                    List<Object[]> out = new ArrayList<>();
                    for (RoleProfile p : em.createQuery("FROM RoleProfile r WHERE r.deleted=false ORDER BY r.name", RoleProfile.class).getResultList()) {
                        long n = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.deleted=false AND u.roleProfile=:n", Long.class)
                                .setParameter("n", p.getName()).getSingleResult();
                        out.add(new Object[]{p.getUid(), p.getName(), p.getDescription() == null ? "" : p.getDescription(), p.getPermissions(), n});
                    }
                    return out;
                } finally {
                    em.close();
                }
            }
            @Override protected void done() {
                try { all.clear(); all.addAll(get()); render(); } catch (Exception ex) { flash("Could not load roles: " + ex.getMessage()); }
            }
        }.execute();
    }

    @Override protected void render() {
        String q = search.getText().trim().toLowerCase();
        model.setRowCount(0);
        ids.clear();
        for (Object[] r : all) {
            if (!q.isEmpty() && !(r[1] + " " + r[2]).toLowerCase().contains(q)) continue;
            StringBuilder areas = new StringBuilder();
            for (Map.Entry<RoleType, boolean[]> e : RoleService.decode((String) r[3]).entrySet()) {
                if (areas.length() > 0) areas.append(", ");
                areas.append(UserForm.AREA_NAMES[e.getKey().ordinal()].split(" ")[0]).append(e.getValue()[1] ? "" : " (view)");
            }
            ids.add(r[0]);
            model.addRow(new Object[]{r[1], r[2], areas.toString(), r[4]});
        }
        footer.setText(model.getRowCount() + " role(s)");
    }

    @Override protected void onOpen() {
        Object id = selectedId();
        if (id != null) new RoleForm((UUID) id, logged, this::load);
    }
}
