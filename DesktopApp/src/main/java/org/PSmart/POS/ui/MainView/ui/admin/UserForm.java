package org.PSmart.POS.ui.MainView.ui.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.RoleProfile;
import org.PSmart.POS.Entities.RoleType;
import org.PSmart.POS.Entities.User;
import org.PSmart.POS.Utils.Components.PPasswordTextField;
import org.PSmart.POS.Utils.Components.PTextField;
import org.PSmart.POS.Utils.Components.forms.FormFrame;
import org.PSmart.POS.Utils.Components.forms.Ui;
import org.PSmart.POS.Utils.PasswordUtil;
import org.PSmart.POS.Utils.RoleService;

import javax.swing.*;
import java.util.*;

/** Add / edit a user: pick a role (permissions fill in), optional commission, optional fine-tuning. */
final class UserForm extends FormFrame {

    static final RoleType[] AREAS = RoleType.values();
    static final String[] AREA_NAMES = {"Admin (users)", "Stock / Inventory", "Cashier & Sales", "Expenses", "Purchases", "Settings", "Reports"};

    private boolean removeArmed;

    UserForm(UUID userId, User logged, Runnable onSaved) {
        super(userId == null ? "Add New User" : "Edit User", userId == null ? "Add New User" : "Edit User", 760);
        if (org.PSmart.POS.Utils.SupervisorApproval.require(null, logged, RoleType.ADMIN, "manage users") == null) {
            note("Adding or changing users needs an administrator's approval.");
            secondary("Close", this::dispose);
            open();
            return;
        }
        final User existing = load(userId);
        final List<RoleProfile> profiles = RoleService.profiles();
        final List<String> roleNames = new ArrayList<>();
        for (RoleProfile p : profiles) roleNames.add(p.getName());
        roleNames.add("Custom");

        PTextField name = field("Full Name", existing == null ? "" : existing.getFullname());
        PTextField username = field("Username", existing == null ? "" : existing.getUsername());
        PTextField phone = field("Phone", existing == null || existing.getPhone() == null ? "" : existing.getPhone());
        PPasswordTextField password = password(existing == null ? "Password" : "New password (leave blank to keep the current one)");

        String current = existing == null || existing.getRoleProfile() == null ? roleNames.get(0) : existing.getRoleProfile();
        int sel = Math.max(0, roleNames.indexOf(current));
        final JComboBox<String> role = combo("Role  (sets what this person can open and change)", roleNames.toArray(new String[0]), sel);
        final JLabel roleHint = note(describe(profiles, roleNames.get(sel)));

        final Section perms = section("Fine-tune permissions", false);
        final Map<RoleType, boolean[]> startPerms = existing != null ? RoleService.permissionsOf(existing.getUid())
                : permsOfProfile(profiles, roleNames.get(sel));
        boolean[][] state = new boolean[AREAS.length][2];
        for (int i = 0; i < AREAS.length; i++) {
            boolean[] f = startPerms.get(AREAS[i]);
            if (f != null) { state[i][0] = f[0]; state[i][1] = f[1]; }
        }
        final JCheckBox[][] boxes = matrixInSection(perms, state);

        final Section comm = section("Commission", existing != null && existing.getCommissionRate() != null);
        final PTextField rate = comm.decimal("Commission rate % (blank = store default)",
                existing == null || existing.getCommissionRate() == null ? "" : String.valueOf(existing.getCommissionRate()));
        final JCheckBox active = check("This user can sign in", existing == null || !existing.isDeleted());
        errorLine();

        final boolean[] programmatic = {false};
        role.addActionListener(e -> {
            String n = String.valueOf(role.getSelectedItem());
            roleHint.setText(wrapHtml(describe(profiles, n)));
            if (!"Custom".equals(n)) {
                programmatic[0] = true;
                Map<RoleType, boolean[]> p = permsOfProfile(profiles, n);
                for (int i = 0; i < AREAS.length; i++) {
                    boolean[] f = p.get(AREAS[i]);
                    boxes[i][0].setSelected(f != null && f[0]);
                    boxes[i][1].setSelected(f != null && f[1]);
                }
                programmatic[0] = false;
            }
        });
        for (JCheckBox[] row : boxes) for (JCheckBox b : row) b.addActionListener(e -> {
            if (!programmatic[0]) role.setSelectedItem("Custom");
        });

        secondary("Cancel", this::dispose);
        primary(existing == null ? "Add User" : "Save User", () -> {
            String fn = name.getText().trim(), un = username.getText().trim(), pw = password.getText().trim();
            if (fn.isEmpty() || un.isEmpty()) { error("Full name and username are required."); return; }
            if (existing == null && pw.length() < 4) { error("Choose a password of at least 4 characters."); return; }
            if (!pw.isEmpty() && pw.length() < 4) { error("The password must be at least 4 characters."); return; }
            Double rateVal = null;
            if (!rate.getText().trim().isEmpty()) {
                try {
                    rateVal = Double.parseDouble(rate.getText().trim().replace(",", ""));
                    if (rateVal < 0 || rateVal > 100) throw new NumberFormatException();
                } catch (NumberFormatException ex) { error("Commission rate must be between 0 and 100."); return; }
            }
            Map<RoleType, boolean[]> chosen = new EnumMap<>(RoleType.class);
            for (int i = 0; i < AREAS.length; i++) chosen.put(AREAS[i], new boolean[]{boxes[i][0].isSelected() || boxes[i][1].isSelected(), boxes[i][1].isSelected()});
            boolean anyPerm = chosen.values().stream().anyMatch(f -> f[0] || f[1]);
            if (!anyPerm) { error("Give this user at least one permission (pick a role)."); return; }
            if (existing != null && existing.getUid().equals(logged.getUid()) && !active.isSelected()) {
                error("You cannot deactivate your own account."); return;
            }
            boolean willBeAdmin = chosen.get(RoleType.ADMIN)[1] && active.isSelected();
            if (!willBeAdmin && otherAdmins(existing == null ? null : existing.getUid()) == 0) {
                error("There must always be at least one active administrator."); return;
            }
            try {
                UUID id = persist(existing, fn, un, phone.getText().trim(), pw, !active.isSelected());
                RoleService.applyPermissions(id, chosen, String.valueOf(role.getSelectedItem()), rateVal);
                Ui.toast("User saved - " + fn);
                dispose();
                if (onSaved != null) onSaved.run();
            } catch (Exception ex) {
                Throwable t = ex;
                while (t.getCause() != null) t = t.getCause();
                String m = String.valueOf(t.getMessage());
                error(m.toLowerCase().contains("unique") || m.toLowerCase().contains("duplicate")
                        ? "That username is already taken." : "Could not save: " + m);
            }
        });
        if (existing != null && !existing.getUid().equals(logged.getUid())) {
            final JButton[] rb = new JButton[1];
            rb[0] = danger("Remove User", () -> {
                if (!removeArmed) { removeArmed = true; rb[0].setText("Tap again to confirm"); return; }
                if (otherAdmins(existing.getUid()) == 0 && RoleService.canChange(existing, RoleType.ADMIN)) {
                    error("You cannot remove the only administrator."); return;
                }
                EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
                EntityTransaction tx = em.getTransaction();
                try {
                    tx.begin();
                    em.find(User.class, existing.getUid()).setDeleted(true);
                    tx.commit();
                    Ui.toast("User removed");
                    dispose();
                    if (onSaved != null) onSaved.run();
                } catch (Exception ex) {
                    if (tx.isActive()) tx.rollback();
                    error("Could not remove: " + ex.getMessage());
                } finally {
                    em.close();
                }
            });
        }
        open();
    }

    private JCheckBox[][] matrixInSection(Section s, boolean[][] state) {
        // matrix rows live outside the section API: add and then hide/show with the section toggle
        JCheckBox[][] b = matrix(AREA_NAMES, new String[]{"View", "Change"}, state);
        s.attach(lastRowComponent());
        return b;
    }

    private static String wrapHtml(String s) { return "<html>" + s + "</html>"; }

    private static String describe(List<RoleProfile> ps, String name) {
        for (RoleProfile p : ps) if (p.getName().equals(name)) return p.getDescription() == null ? "" : p.getDescription();
        return "Permissions below were adjusted by hand for this person.";
    }

    private static Map<RoleType, boolean[]> permsOfProfile(List<RoleProfile> ps, String name) {
        for (RoleProfile p : ps) if (p.getName().equals(name)) return RoleService.decode(p.getPermissions());
        return new EnumMap<>(RoleType.class);
    }

    private static User load(UUID id) {
        if (id == null) return null;
        EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
        try {
            return em.find(User.class, id);
        } finally {
            em.close();
        }
    }

    /** Active administrators other than the given user. */
    private static long otherAdmins(UUID exclude) {
        EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
        try {
            String q = "SELECT COUNT(DISTINCT r.user.uid) FROM Role r WHERE r.roleType=:t AND r.change=true AND r.user.deleted=false"
                    + (exclude == null ? "" : " AND r.user.uid <> :ex");
            jakarta.persistence.TypedQuery<Long> tq = em.createQuery(q, Long.class).setParameter("t", RoleType.ADMIN);
            if (exclude != null) tq.setParameter("ex", exclude);
            return tq.getSingleResult();
        } finally {
            em.close();
        }
    }

    private static UUID persist(User existing, String fn, String un, String phone, String pw, boolean deleted) {
        EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            User u = existing == null ? new User() : em.find(User.class, existing.getUid());
            u.setFullname(fn);
            u.setUsername(un);
            u.setPhone(phone.isEmpty() ? null : phone);
            if (!pw.isEmpty()) u.setPassword(PasswordUtil.hash(pw));
            u.setDeleted(deleted);
            if (existing == null) em.persist(u);
            tx.commit();
            return u.getUid();
        } catch (RuntimeException e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
