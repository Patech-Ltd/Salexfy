package org.PSmart.POS.ui.MainView.ui.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.RoleType;
import org.PSmart.POS.Entities.User;
import org.PSmart.POS.Repositories.SaleQueries;
import org.PSmart.POS.Utils.Components.forms.Ui;
import org.PSmart.POS.Utils.PasswordUtil;
import org.PSmart.POS.Utils.RoleService;
import org.PSmart.POS.Utils.SupervisorApproval;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/** A real profile page: avatar header, key numbers, permissions, recent sales, and inline account actions. */
final class UserDetailForm extends JFrame {

    private static final Color NAVY = new Color(0, 0, 137);
    private static final Color BLUE = new Color(25, 118, 211);
    private static final Color SLATE = new Color(52, 58, 64);
    private static final Color MUTED = new Color(108, 117, 125);
    private static final Color BORDER = new Color(222, 226, 230);
    private static final Color PAGE = new Color(245, 245, 250);
    private static final Color GREEN = new Color(30, 110, 50);
    private static final Color RED = new Color(200, 30, 45);
    private static final Color AMBER = new Color(214, 137, 16);

    private final UUID id;
    private final User logged;
    private final Runnable onChanged;
    private final User u;
    private final boolean self, inactive;
    private final long[] act;
    private final JLabel message = new JLabel(" ");
    private final JPanel actionHost = new JPanel(new CardLayout());
    private final JPanel actionWrap = new JPanel(new BorderLayout());

    UserDetailForm(UUID id, User logged, Runnable onChanged) {
        super("User profile");
        this.id = id;
        this.logged = logged;
        this.onChanged = onChanged;
        this.u = load(id);
        this.self = u != null && logged != null && u.getUid().equals(logged.getUid());
        this.inactive = u != null && u.isDeleted();
        this.act = u == null ? new long[6] : activity(id);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(PAGE);
        Dimension s = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(1120, s.width - 40), Math.min(800, s.height - 60));
        setLocationRelativeTo(null);
        if (u == null) {
            setLayout(new FlowLayout());
            add(new JLabel("This user could not be found."));
            setVisible(true);
            return;
        }
        setTitle("Profile - " + nz(u.getFullname()));

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(PAGE);
        body.setBorder(new EmptyBorder(16, 20, 20, 20));
        body.add(header());
        body.add(gap(12));
        actionWrap.setOpaque(false);
        actionWrap.setVisible(false);
        actionWrap.setAlignmentX(0f);
        actionWrap.add(actionHost, BorderLayout.CENTER);
        body.add(actionWrap);
        body.add(gap(12));
        body.add(stats());
        body.add(gap(12));
        JPanel cols = new JPanel(new GridBagLayout());
        cols.setOpaque(false);
        cols.setAlignmentX(0f);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL; g.anchor = GridBagConstraints.NORTH; g.weightx = 0.5; g.weighty = 0; g.gridy = 0; g.insets = new Insets(0, 0, 0, 6);
        cols.add(permissionsCard(), g);
        g.insets = new Insets(0, 6, 0, 0);
        g.gridx = 1;
        cols.add(recentSalesCard(), g);
        body.add(cols);
        JScrollPane sp = new JScrollPane(body, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(18);
        add(sp);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setVisible(true);
    }

    // ------------------------------------------------------------------ pieces

    private static Component gap(int h) { return Box.createRigidArea(new Dimension(0, h)); }

    private JPanel card(String title) {
        JPanel c = new JPanel(new BorderLayout(0, 8)) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(Color.WHITE);
                g.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 14, 14));
                g.setColor(BORDER);
                g.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 14, 14));
                g.dispose();
            }
        };
        c.setOpaque(false);
        c.setBorder(new EmptyBorder(14, 16, 14, 16));
        if (title != null) {
            JLabel t = new JLabel(title);
            t.setFont(new Font("Courier", Font.BOLD, 16));
            t.setForeground(NAVY);
            c.add(t, BorderLayout.NORTH);
        }
        return c;
    }

    private JComponent header() {
        JPanel h = card(null);
        h.setLayout(new BorderLayout(18, 0));
        h.setBorder(new EmptyBorder(18, 20, 18, 20));
        h.setAlignmentX(0f);
        h.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

        String name = nz(u.getFullname()).isEmpty() ? nz(u.getUsername()) : u.getFullname();
        h.add(new Avatar(initials(name)), BorderLayout.WEST);

        JPanel mid = new JPanel();
        mid.setOpaque(false);
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
        JLabel nm = new JLabel(name + (self ? "  (you)" : ""));
        nm.setFont(new Font("Courier", Font.BOLD, 28));
        nm.setForeground(NAVY);
        JLabel sub = new JLabel("@" + nz(u.getUsername()) + (u.getPhone() == null || u.getPhone().isEmpty() ? "" : "   |   " + u.getPhone()));
        sub.setFont(new Font("Courier", Font.PLAIN, 15));
        sub.setForeground(MUTED);
        JPanel pills = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pills.setOpaque(false);
        pills.add(new Pill(u.getRoleProfile() == null ? "Full access" : u.getRoleProfile(), BLUE));
        pills.add(new Pill(inactive ? "Inactive" : "Active", inactive ? RED : GREEN));
        pills.add(new Pill(PasswordUtil.isHashed(u.getPassword()) ? "Password secure" : "Password not protected", PasswordUtil.isHashed(u.getPassword()) ? GREEN : RED));
        nm.setAlignmentX(0f); sub.setAlignmentX(0f); pills.setAlignmentX(0f);
        mid.add(Box.createVerticalGlue());
        mid.add(nm);
        mid.add(sub);
        mid.add(Box.createRigidArea(new Dimension(0, 10)));
        mid.add(pills);
        mid.add(Box.createVerticalGlue());
        h.add(mid, BorderLayout.CENTER);

        JPanel btns = new JPanel(new GridLayout(2, 2, 8, 8));
        btns.setOpaque(false);
        JButton edit = filled("Edit profile", BLUE);
        edit.addActionListener(e -> {
            dispose();
            new UserForm(id, logged, () -> {
                if (onChanged != null) onChanged.run();
                new UserDetailForm(id, logged, onChanged);
            });
        });
        JButton reset = outline("Reset password", BLUE);
        reset.addActionListener(e -> showAction("reset"));
        btns.add(edit);
        btns.add(reset);
        if (!self) {
            JButton toggle = outline(inactive ? "Reactivate" : "Deactivate", SLATE);
            toggle.addActionListener(e -> showAction("toggle"));
            JButton del = outline("Delete user", RED);
            del.addActionListener(e -> showAction("delete"));
            btns.add(toggle);
            btns.add(del);
        }
        h.add(btns, BorderLayout.EAST);
        return h;
    }

    private JComponent stats() {
        JPanel row = new JPanel(new GridLayout(1, 5, 12, 0));
        row.setOpaque(false);
        row.setAlignmentX(0f);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
        row.add(stat("Sales rung up", String.valueOf(act[0]), NAVY));
        row.add(stat("Sales value", "KES " + String.format("%,.0f", act[3] / 100.0), GREEN));
        row.add(stat("Cancelled sales", String.valueOf(act[1]), act[1] > 0 ? AMBER : SLATE));
        row.add(stat("Last sale", act[2] == 0 ? "-" : new SimpleDateFormat("dd MMM, HH:mm").format(new Date(act[2])), SLATE));
        row.add(stat("Commission", u.getCommissionRate() == null ? "store default" : u.getCommissionRate() + " %", BLUE));
        return row;
    }

    private JPanel stat(String label, String value, Color c) {
        JPanel p = card(null);
        p.setLayout(new BorderLayout(0, 4));
        p.setBorder(new EmptyBorder(12, 16, 12, 16));
        JLabel l = new JLabel(label);
        l.setFont(new Font("Courier", Font.PLAIN, 13));
        l.setForeground(MUTED);
        JLabel v = new JLabel(value);
        v.setFont(new Font("Courier", Font.BOLD, 22));
        v.setForeground(c);
        p.add(l, BorderLayout.NORTH);
        p.add(v, BorderLayout.CENTER);
        return p;
    }

    private JPanel permissionsCard() {
        JPanel c = card("What this person can do");
        Map<RoleType, boolean[]> perms = RoleService.permissionsOf(u.getUid());
        boolean all = perms.isEmpty();
        JPanel grid = new JPanel(new GridLayout(0, 2, 8, 8));
        grid.setOpaque(false);
        RoleType[] areas = RoleType.values();
        for (int i = 0; i < areas.length; i++) {
            boolean[] f = perms.get(areas[i]);
            boolean change = all || (f != null && f[1]);
            boolean view = change || (f != null && f[0]);
            JPanel cell = new JPanel(new BorderLayout(0, 4));
            cell.setOpaque(true);
            cell.setBackground(new Color(248, 249, 252));
            cell.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER, 1, true), new EmptyBorder(8, 10, 8, 10)));
            JLabel n = new JLabel(UserForm.AREA_NAMES[i]);
            n.setFont(new Font("Courier", Font.BOLD, 13));
            n.setForeground(SLATE);
            JPanel pl = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            pl.setOpaque(false);
            pl.add(change ? new Pill("Full access", GREEN) : view ? new Pill("View only", BLUE) : new Pill("No access", MUTED));
            cell.add(n, BorderLayout.NORTH);
            cell.add(pl, BorderLayout.CENTER);
            grid.add(cell);
        }
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(grid, BorderLayout.NORTH);
        c.add(wrap, BorderLayout.CENTER);
        return c;
    }

    private JPanel recentSalesCard() {
        JPanel c = card("Recent sales");
        DefaultTableModel m = new DefaultTableModel(new String[]{"Receipt", "Date", "Total", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int cc) { return false; }
        };
        for (Object[] r : recentSales(id)) m.addRow(r);
        if (m.getRowCount() == 0) {
            JLabel none = new JLabel("No sales yet.");
            none.setFont(new Font("Courier", Font.PLAIN, 14));
            none.setForeground(MUTED);
            none.setBorder(new EmptyBorder(8, 0, 24, 0));
            c.add(none, BorderLayout.CENTER);
        } else {
            JTable t = new JTable(m);
            Ui.styleTable(t);
            t.setRowHeight(28);
            JScrollPane sp = new JScrollPane(t);
            sp.setBorder(BorderFactory.createLineBorder(BORDER));
            sp.setPreferredSize(new Dimension(300, 30 + 28 * Math.min(8, m.getRowCount())));
            c.add(sp, BorderLayout.CENTER);
        }
        long other = act[4] + act[5];
        JLabel foot = new JLabel("Also recorded: " + act[4] + " purchase(s), " + act[5] + " expense(s)" + (other == 0 ? "" : ""));
        foot.setFont(new Font("Courier", Font.PLAIN, 13));
        foot.setForeground(MUTED);
        c.add(foot, BorderLayout.SOUTH);
        return c;
    }

    // ------------------------------------------------------------------ inline actions

    private void showAction(String which) {
        actionHost.removeAll();
        message.setText(" ");
        switch (which) {
            case "reset": actionHost.add(resetCard(), "x"); break;
            case "toggle": actionHost.add(toggleCard(), "x"); break;
            default: actionHost.add(deleteCard(), "x");
        }
        actionWrap.setVisible(true);
        actionHost.revalidate();
        actionHost.repaint();
        revalidate();
    }

    private JPanel actionCard(String title, Color accent) {
        JPanel c = card(title);
        ((JLabel) ((BorderLayout) c.getLayout()).getLayoutComponent(BorderLayout.NORTH)).setForeground(accent);
        message.setFont(new Font("Courier", Font.BOLD, 13));
        message.setForeground(RED);
        return c;
    }

    private JPanel buttonRow(JComponent... bs) {
        JPanel r = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        r.setOpaque(false);
        for (JComponent b : bs) r.add(b);
        return r;
    }

    private JTextField box() {
        JTextField f = new JTextField(16);
        f.setFont(new Font("Courier", Font.PLAIN, 16));
        f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER, 1, true), new EmptyBorder(6, 10, 6, 10)));
        return f;
    }

    private JPanel resetCard() {
        JPanel c = actionCard("Reset password", BLUE);
        JTextField a = box(), b = box();
        JPanel fields = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        fields.setOpaque(false);
        fields.add(label("New password"));
        fields.add(a);
        fields.add(label("Again"));
        fields.add(b);
        JButton gen = outline("Generate", SLATE);
        gen.addActionListener(e -> {
            String t = temp();
            a.setText(t);
            b.setText(t);
            message.setForeground(GREEN);
            message.setText("Temporary password: " + t + "  - press Save password, then tell the user.");
        });
        JButton save = filled("Save password", BLUE);
        save.addActionListener(e -> {
            String p1 = a.getText().trim(), p2 = b.getText().trim();
            message.setForeground(RED);
            if (p1.length() < 4) { message.setText("The password must be at least 4 characters."); return; }
            if (!p1.equals(p2)) { message.setText("The two passwords do not match."); return; }
            if (!approved("reset a user's password")) return;
            String err = update(id, em -> em.find(User.class, id).setPassword(PasswordUtil.hash(p1)));
            if (err != null) { message.setText(err); return; }
            Ui.toast("Password reset for " + nz(u.getFullname()));
            reopen();
        });
        JButton cancel = outline("Cancel", SLATE);
        cancel.addActionListener(e -> actionWrap.setVisible(false));
        JPanel body = new JPanel(new GridLayout(0, 1, 0, 10));
        body.setOpaque(false);
        body.add(fields);
        body.add(buttonRow(gen, save, cancel));
        body.add(message);
        c.add(body, BorderLayout.CENTER);
        return c;
    }

    private JPanel toggleCard() {
        JPanel c = actionCard(inactive ? "Reactivate this user" : "Deactivate this user", SLATE);
        JLabel t = label(inactive ? nz(u.getFullname()) + " will be able to sign in again."
                : nz(u.getFullname()) + " will no longer be able to sign in. Their sales and history stay on record.");
        JButton go = filled(inactive ? "Reactivate" : "Yes, deactivate", SLATE);
        go.addActionListener(e -> {
            message.setForeground(RED);
            if (!inactive && lastAdmin(u)) { message.setText("This is the only active administrator, so it can't be deactivated."); return; }
            if (!approved((inactive ? "reactivate" : "deactivate") + " a user")) return;
            String err = update(id, em -> em.find(User.class, id).setDeleted(!inactive));
            if (err != null) { message.setText(err); return; }
            Ui.toast(nz(u.getFullname()) + (inactive ? " can sign in again" : " can no longer sign in"));
            reopen();
        });
        JButton cancel = outline("Cancel", SLATE);
        cancel.addActionListener(e -> actionWrap.setVisible(false));
        JPanel body = new JPanel(new GridLayout(0, 1, 0, 10));
        body.setOpaque(false);
        body.add(t);
        body.add(buttonRow(go, cancel));
        body.add(message);
        c.add(body, BorderLayout.CENTER);
        return c;
    }

    private JPanel deleteCard() {
        final long records = act[0] + act[1] + act[4] + act[5];
        JPanel c = actionCard("Delete this user", RED);
        JPanel body = new JPanel(new GridLayout(0, 1, 0, 10));
        body.setOpaque(false);
        JButton cancel = outline("Cancel", SLATE);
        cancel.addActionListener(e -> actionWrap.setVisible(false));
        if (records > 0) {
            JLabel t = label("<html><b>This user can't be deleted.</b> They have " + records
                    + " sale / purchase / expense record(s), and deleting them would break your history. "
                    + "Deactivate them instead - they can't sign in, and everything stays on record.</html>");
            JButton deact = filled("Deactivate instead", SLATE);
            deact.addActionListener(e -> showAction("toggle"));
            body.add(t);
            body.add(buttonRow(deact, cancel));
        } else {
            JLabel t = label("<html><b style='color:#c81e2d'>WARNING:</b> deleting " + esc(nz(u.getFullname()))
                    + " is <b>permanent and can't be undone</b>. Their account and permissions are removed completely.</html>");
            JButton go = filled("Delete permanently", RED);
            go.addActionListener(e -> {
                message.setForeground(RED);
                if (lastAdmin(u)) { message.setText("This is the only active administrator, so it can't be deleted."); return; }
                if (!approved("delete a user")) return;
                String err = update(id, em -> {
                    em.createQuery("DELETE FROM Role r WHERE r.user.uid=:u").setParameter("u", id).executeUpdate();
                    em.remove(em.find(User.class, id));
                });
                if (err != null) { message.setText(err); return; }
                Ui.toast("User deleted");
                dispose();
                if (onChanged != null) onChanged.run();
            });
            body.add(t);
            body.add(buttonRow(go, cancel));
        }
        body.add(message);
        c.add(body, BorderLayout.CENTER);
        return c;
    }

    private boolean approved(String verb) {
        if (SupervisorApproval.require(this, logged, RoleType.ADMIN, verb) != null) return true;
        message.setForeground(RED);
        message.setText("You need an administrator's approval to " + verb + ".");
        return false;
    }

    private void reopen() {
        dispose();
        if (onChanged != null) onChanged.run();
        new UserDetailForm(id, logged, onChanged);
    }

    // ------------------------------------------------------------------ widgets

    private static JLabel label(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Courier", Font.PLAIN, 14));
        l.setForeground(SLATE);
        return l;
    }

    private static JButton filled(String text, Color bg) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? bg.darker() : bg);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setForeground(Color.WHITE);
        return styled(b);
    }

    private static JButton outline(String text, Color c) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(c.getRed(), c.getGreen(), c.getBlue(), 24) : Color.WHITE);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.setColor(c);
                g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setForeground(c);
        return styled(b);
    }

    private static JButton styled(JButton b) {
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setRolloverEnabled(true);
        b.setFont(new Font("Courier", Font.BOLD, 14));
        b.setPreferredSize(new Dimension(170, 38));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static String initials(String name) {
        String[] p = name.trim().split("\\s+");
        String a = p.length > 0 && !p[0].isEmpty() ? p[0].substring(0, 1) : "?";
        String b = p.length > 1 ? p[1].substring(0, 1) : "";
        return (a + b).toUpperCase();
    }

    private static String esc(String s) { return s.replace("&", "&amp;").replace("<", "&lt;"); }

    private static final class Avatar extends JComponent {
        private final String text;
        Avatar(String text) { this.text = text; setPreferredSize(new Dimension(96, 96)); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int d = Math.min(getWidth(), getHeight());
            g.setColor(NAVY);
            g.fillOval(0, 0, d, d);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Courier", Font.BOLD, 36));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, (d - fm.stringWidth(text)) / 2, (d + fm.getAscent()) / 2 - fm.getDescent() / 2 - 2);
            g.dispose();
        }
    }

    private static final class Pill extends JComponent {
        private final String text;
        private final Color color;
        Pill(String text, Color color) {
            this.text = text;
            this.color = color;
            setFont(new Font("Courier", Font.BOLD, 12));
            FontMetrics fm = getFontMetrics(getFont());
            setPreferredSize(new Dimension(fm.stringWidth(text) + 22, 24));
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 30));
            g.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
            g.setColor(color);
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, (getHeight() + fm.getAscent()) / 2 - 2);
            g.dispose();
        }
    }

    private static List<Object[]> recentSales(UUID id) {
        EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
        try {
            List<Object[]> out = new ArrayList<>();
            SimpleDateFormat df = new SimpleDateFormat("dd MMM HH:mm");
            for (Object o : em.createQuery("SELECT s.uid, s.createdAt, s.total, s.status FROM Sale s WHERE s.cashier.uid=:id AND s.deleted=false "
                    + "ORDER BY s.createdAt DESC").setParameter("id", id).setMaxResults(8).getResultList()) {
                Object[] r = (Object[]) o;
                out.add(new Object[]{"RCP-" + String.format("%06d", ((UUID) r[0]).hashCode() & 0xFFFFFF),
                        r[1] == null ? "" : df.format(new Date(((Number) r[1]).longValue())),
                        String.format("%,.2f", r[2] == null ? 0.0 : ((Number) r[2]).doubleValue()), r[3] == null ? "" : r[3]});
            }
            return out;
        } catch (Exception e) {
            return new ArrayList<>();
        } finally { em.close(); }
    }

    // ------------------------------------------------------------------ data

    private static String nz(String s) { return s == null ? "" : s; }

    private static String temp() {
        String chars = "abcdefghjkmnpqrstuvwxyz23456789";
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) sb.append(chars.charAt(r.nextInt(chars.length())));
        return sb.toString();
    }

    private static User load(UUID id) {
        EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
        try { return em.find(User.class, id); } finally { em.close(); }
    }

    /** {sales, cancelled, lastSaleAt, totalInCents, purchases, expenses}. */
    private static long[] activity(UUID id) {
        EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
        try {
            long[] a = new long[6];
            Object[] s = (Object[]) em.createQuery("SELECT COUNT(s), COALESCE(SUM(s.total),0), MAX(s.createdAt) FROM Sale s "
                    + "WHERE s.cashier.uid=:id AND s.deleted=false AND (s.status IS NULL OR UPPER(s.status) NOT IN :bad)")
                    .setParameter("id", id).setParameter("bad", SaleQueries.VOID_STATUSES).getSingleResult();
            a[0] = ((Number) s[0]).longValue();
            a[3] = Math.round(((Number) s[1]).doubleValue() * 100);
            a[2] = s[2] == null ? 0 : ((Number) s[2]).longValue();
            a[1] = em.createQuery("SELECT COUNT(s) FROM Sale s WHERE s.cashier.uid=:id AND UPPER(s.status) IN :bad", Long.class)
                    .setParameter("id", id).setParameter("bad", SaleQueries.VOID_STATUSES).getSingleResult();
            a[4] = em.createQuery("SELECT COUNT(p) FROM Purchase p WHERE p.createdBy.uid=:id", Long.class).setParameter("id", id).getSingleResult();
            a[5] = em.createQuery("SELECT COUNT(e) FROM Expense e WHERE e.createdBy.uid=:id", Long.class).setParameter("id", id).getSingleResult();
            return a;
        } catch (Exception ex) {
            return new long[6];
        } finally { em.close(); }
    }

    /** True when this user is the only active administrator. */
    private static boolean lastAdmin(User u) {
        if (!RoleService.canChange(u, RoleType.ADMIN) || RoleService.permissionsOf(u.getUid()).isEmpty()) return false;
        EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
        try {
            long others = em.createQuery("SELECT COUNT(DISTINCT r.user.uid) FROM Role r WHERE r.roleType=:t AND r.change=true "
                    + "AND r.user.deleted=false AND r.user.uid <> :ex", Long.class)
                    .setParameter("t", RoleType.ADMIN).setParameter("ex", u.getUid()).getSingleResult();
            return others == 0;
        } finally { em.close(); }
    }

    private interface Work { void run(EntityManager em); }

    private static String update(UUID id, Work w) {
        EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            w.run(em);
            tx.commit();
            return null;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            Throwable t = e;
            while (t.getCause() != null) t = t.getCause();
            return "Could not save: " + t.getMessage();
        } finally { em.close(); }
    }
}
