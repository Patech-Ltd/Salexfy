package org.PSmart.POS.ui.MainView.ui.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.RoleProfile;
import org.PSmart.POS.Entities.RoleType;
import org.PSmart.POS.Entities.User;
import org.PSmart.POS.Utils.Components.PTextField;
import org.PSmart.POS.Utils.Components.forms.FormFrame;
import org.PSmart.POS.Utils.Components.forms.Ui;
import org.PSmart.POS.Utils.RoleService;

import javax.swing.*;
import java.util.*;

/** Create or edit a role: a name and what it can view / change. Saved in the database. */
final class RoleForm extends FormFrame {

    private boolean deleteArmed;

    RoleForm(UUID roleId, User logged, Runnable onSaved) {
        super(roleId == null ? "Add New Role" : "Edit Role", roleId == null ? "Add New Role" : "Edit Role", 700);
        if (org.PSmart.POS.Utils.SupervisorApproval.require(null, logged, RoleType.ADMIN, "manage roles") == null) {
            note("Managing roles needs an administrator's approval.");
            secondary("Close", this::dispose);
            open();
            return;
        }
        final RoleProfile existing = roleId == null ? null : find(roleId);
        PTextField name = field("Role name (e.g. Supervisor)", existing == null ? "" : existing.getName());
        PTextField desc = field("What is this role for?", existing == null || existing.getDescription() == null ? "" : existing.getDescription());
        Map<RoleType, boolean[]> p = existing == null ? new EnumMap<>(RoleType.class) : RoleService.decode(existing.getPermissions());
        boolean[][] state = new boolean[UserForm.AREAS.length][2];
        for (int i = 0; i < UserForm.AREAS.length; i++) {
            boolean[] f = p.get(UserForm.AREAS[i]);
            if (f != null) { state[i][0] = f[0]; state[i][1] = f[1]; }
        }
        note("View lets someone open the page. Change lets them add, edit or delete on it.");
        final JCheckBox[][] boxes = matrix(UserForm.AREA_NAMES, new String[]{"View", "Change"}, state);
        for (JCheckBox[] row : boxes) {
            row[1].addActionListener(e -> { if (row[1].isSelected()) row[0].setSelected(true); });
        }
        final JCheckBox applyNow = check("Also update people who already have this role", true);
        errorLine();

        secondary("Cancel", this::dispose);
        primary(existing == null ? "Add Role" : "Save Role", () -> {
            String n = name.getText().trim();
            if (n.isEmpty()) { error("Give the role a name."); return; }
            Map<RoleType, boolean[]> chosen = new EnumMap<>(RoleType.class);
            for (int i = 0; i < UserForm.AREAS.length; i++) chosen.put(UserForm.AREAS[i], new boolean[]{boxes[i][0].isSelected() || boxes[i][1].isSelected(), boxes[i][1].isSelected()});
            if (chosen.values().stream().noneMatch(f -> f[0] || f[1])) { error("Tick at least one permission."); return; }
            EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
            EntityTransaction tx = em.getTransaction();
            try {
                jakarta.persistence.TypedQuery<Long> dq = em.createQuery("SELECT COUNT(r) FROM RoleProfile r WHERE r.deleted=false AND LOWER(r.name)=:n"
                        + (existing == null ? "" : " AND r.uid <> :id"), Long.class).setParameter("n", n.toLowerCase());
                if (existing != null) dq.setParameter("id", existing.getUid());
                if (dq.getSingleResult() > 0) { error("A role called \"" + n + "\" already exists."); return; }
                tx.begin();
                RoleProfile rp = existing == null ? new RoleProfile() : em.find(RoleProfile.class, existing.getUid());
                String oldName = rp.getName();
                rp.setName(n);
                rp.setDescription(desc.getText().trim());
                rp.setPermissions(RoleService.encode(chosen));
                if (existing == null) em.persist(rp);
                tx.commit();
                if (existing != null && applyNow.isSelected()) {
                    EntityManager em2 = DatabaseUtils.getInstance().getEmf().createEntityManager();
                    try {
                        for (UUID uid : em2.createQuery("SELECT u.uid FROM User u WHERE u.roleProfile=:n AND u.deleted=false", UUID.class)
                                .setParameter("n", oldName).getResultList()) {
                            Double rate = em2.find(User.class, uid).getCommissionRate();
                            RoleService.applyPermissions(uid, chosen, n, rate);
                        }
                    } finally {
                        em2.close();
                    }
                } else if (existing != null && !n.equals(oldName)) {
                    EntityManager em3 = DatabaseUtils.getInstance().getEmf().createEntityManager();
                    try {
                        em3.getTransaction().begin();
                        em3.createQuery("UPDATE User u SET u.roleProfile=:n WHERE u.roleProfile=:o").setParameter("n", n).setParameter("o", oldName).executeUpdate();
                        em3.getTransaction().commit();
                    } finally {
                        em3.close();
                    }
                }
                Ui.toast("Role saved - " + n);
                dispose();
                if (onSaved != null) onSaved.run();
            } catch (Exception ex) {
                if (tx.isActive()) tx.rollback();
                error("Could not save: " + ex.getMessage());
            } finally {
                em.close();
            }
        });
        if (existing != null) {
            final JButton[] db = new JButton[1];
            db[0] = danger("Delete Role", () -> {
                if (!deleteArmed) { deleteArmed = true; db[0].setText("Tap again to confirm"); return; }
                EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
                try {
                    long users = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.deleted=false AND u.roleProfile=:n", Long.class)
                            .setParameter("n", existing.getName()).getSingleResult();
                    if (users > 0) { error(users + " user(s) have this role. Give them another role first."); deleteArmed = false; db[0].setText("Delete Role"); return; }
                    em.getTransaction().begin();
                    em.find(RoleProfile.class, existing.getUid()).setDeleted(true);
                    em.getTransaction().commit();
                    Ui.toast("Role deleted");
                    dispose();
                    if (onSaved != null) onSaved.run();
                } catch (Exception ex) {
                    error("Could not delete: " + ex.getMessage());
                } finally {
                    em.close();
                }
            });
        }
        open();
    }

    private static RoleProfile find(UUID id) {
        EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
        try {
            return em.find(RoleProfile.class, id);
        } finally {
            em.close();
        }
    }
}
