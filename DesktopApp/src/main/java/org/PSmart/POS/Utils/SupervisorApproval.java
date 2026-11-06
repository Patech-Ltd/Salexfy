package org.PSmart.POS.Utils;

import jakarta.persistence.EntityManager;
import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.RoleType;
import org.PSmart.POS.Entities.User;
import org.PSmart.POS.Utils.Components.PPasswordTextField;
import org.PSmart.POS.Utils.Components.PTextField;
import org.PSmart.POS.Utils.Components.forms.FormFrame;

import javax.swing.*;
import java.awt.*;

/**
 * "Ask a supervisor": when the signed-in user is not allowed to do something, someone who is allowed
 * types their own username and password and the action goes ahead. The approver is returned so it can be
 * written to the record (who did it, who approved it). Three wrong attempts close the prompt.
 */
public final class SupervisorApproval {

    private SupervisorApproval() {}

    /**
     * @return the user who authorised the action: {@code current} if their role already allows it, an approving
     * supervisor otherwise, or null if the request was cancelled or refused.
     */
    public static User require(Component parent, User current, RoleType area, String action) {
        if (RoleService.canChange(current, area)) return current;
        return prompt(parent, current, area, action);
    }

    private static String areaName(RoleType t) {
        switch (t) {
            case ADMIN: return "manage users";
            case STOCK: return "manage stock";
            case CASHIER: return "work with sales";
            case EXPENSE: return "manage expenses";
            case PURCHASE: return "manage purchases";
            case SETTINGS: return "change settings";
            default: return "view and change reports";
        }
    }

    private static User prompt(Component parent, User current, RoleType area, String action) {
        final User[] approved = new User[1];
        final int[] tries = {0};
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        final JDialog d = new JDialog(owner, "Supervisor approval", Dialog.ModalityType.APPLICATION_MODAL);
        d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        d.setResizable(false);
        d.getContentPane().setBackground(Color.WHITE);
        d.setLayout(null);
        d.setSize(560, 460);

        JLabel head = new JLabel("Supervisor approval needed");
        head.setFont(MyUtils.courierFont(22));
        head.setForeground(FormFrame.NAVY);
        head.setBounds(30, 22, 500, 34);
        d.add(head);

        String who = current == null ? "You" : (current.getFullname() != null ? current.getFullname() : current.getUsername());
        JLabel why = new JLabel("<html>" + esc(who) + " cannot <b>" + esc(action) + "</b>. Ask someone who can " + areaName(area)
                + " to sign in here with their own username and password.</html>");
        why.setFont(new Font("Courier", Font.PLAIN, 14));
        why.setForeground(FormFrame.MUTED);
        why.setBounds(30, 62, 500, 58);
        d.add(why);

        PTextField user = new PTextField(FormFrame.NAVY, FormFrame.INK, "Supervisor username", null);
        user.setLocation(30, 130);
        user.setSize(500, 65);
        d.add(user);
        PPasswordTextField pass = new PPasswordTextField(FormFrame.NAVY, FormFrame.INK, "Supervisor password");
        pass.setLocation(30, 205);
        pass.setSize(500, 65);
        d.add(pass);

        JLabel error = new JLabel(" ");
        error.setFont(new Font("Courier", Font.BOLD, 13));
        error.setForeground(Color.RED);
        error.setBounds(30, 284, 500, 24);
        d.add(error);

        JButton cancel = MyUtils.createButton("Cancel", FormFrame.SLATE);
        cancel.setForeground(Color.WHITE);
        cancel.setFont(MyUtils.courierFont(18));
        cancel.setBounds(30, 330, 220, 48);
        cancel.addActionListener(e -> d.dispose());
        d.add(cancel);

        JButton ok = MyUtils.createButton("Approve", Color.BLUE);
        ok.setForeground(Color.WHITE);
        ok.setFont(MyUtils.courierFont(18));
        ok.setBounds(310, 330, 220, 48);
        Runnable attempt = () -> {
            String u = user.getText().trim();
            String p = pass.getText();
            if (u.isEmpty() || p.isEmpty()) { error.setText("Enter the supervisor's username and password."); return; }
            User found = lookup(u);
            if (found == null || !PasswordUtil.matches(found.getPassword(), p)) {
                tries[0]++;
                if (tries[0] >= 3) { d.dispose(); return; }
                error.setText("Wrong username or password (" + (3 - tries[0]) + " attempt" + (3 - tries[0] == 1 ? "" : "s") + " left).");
                pass.setText("");
                return;
            }
            if (current != null && found.getUid().equals(current.getUid())) {
                error.setText("That is you. Someone else with permission must approve."); return;
            }
            if (!RoleService.canChange(found, area)) {
                error.setText(found.getFullname() + " is not allowed to " + areaName(area) + "."); return;
            }
            approved[0] = found;
            d.dispose();
        };
        ok.addActionListener(e -> attempt.run());
        user.getField().addActionListener(e -> pass.getTextField().requestFocusInWindow());
        pass.setAction(attempt);
        d.add(ok);

        d.setLocationRelativeTo(owner);
        SwingUtilities.invokeLater(() -> user.getField().requestFocusInWindow());
        d.setVisible(true);       // blocks until closed
        return approved[0];
    }

    private static User lookup(String username) {
        EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
        try {
            var l = em.createQuery("FROM User u WHERE LOWER(u.username)=:n AND u.deleted=false", User.class)
                    .setParameter("n", username.toLowerCase()).getResultList();
            return l.isEmpty() ? null : l.get(0);
        } finally {
            em.close();
        }
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;");
    }

    /** "Grace (approved by Mary)" for audit columns; just the user when nobody else approved. */
    public static String credit(User actor, User approver) {
        String a = actor == null ? null : (actor.getFullname() != null ? actor.getFullname() : actor.getUsername());
        if (approver == null || actor == null || approver.getUid().equals(actor.getUid())) return a;
        String b = approver.getFullname() != null ? approver.getFullname() : approver.getUsername();
        return a + " (approved by " + b + ")";
    }
}
