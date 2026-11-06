package org.PSmart.POS.ui.MainView.ui.admin;

import org.PSmart.POS.Entities.Role;
import org.PSmart.POS.Entities.RoleType;
import org.PSmart.POS.Entities.User;
import org.PSmart.POS.Utils.MyUtils;
import org.hibernate.Session;

import javax.swing.*;
import java.awt.*;

public class UserCard extends JFrame {

    private static final Color DARK   = new Color(52, 58, 64);
    private static final Color ACCENT = new Color(0, 0, 137);
    private static final Color BG     = new Color(245, 245, 250);

    private final User user;
    private final Session session;
    private final User loggedUser;

    private Runnable editAction;
    private Runnable deleteAction;

    public UserCard(User user, Session session, User loggedUser) {
        this.user       = user;
        this.session    = session;
        this.loggedUser = loggedUser;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setTitle("User - " + user.getUsername());
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG);
        setSize(480, 640);
        setLocationRelativeTo(null);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildScrollableBody(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        setVisible(true);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(DARK);
        header.setPreferredSize(new Dimension(0, 60));
        header.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 24));

        JLabel title = new JLabel("User Profile");
        title.setFont(MyUtils.courierFont(22));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        return header;
    }

    private JPanel buildScrollableBody() {
        JPanel body = buildBodyContent();

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildBodyContent() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(BG);
        body.setBorder(BorderFactory.createEmptyBorder(30, 40, 10, 40));

        // Avatar (initial-based placeholder circle)
        body.add(roundAvatar());
        body.add(Box.createVerticalStrut(16));

        JLabel name = new JLabel(user.getFullname(), SwingConstants.CENTER);
        name.setFont(new Font("Courier", Font.BOLD, 26));
        name.setForeground(DARK);
        name.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(name);

        body.add(Box.createVerticalStrut(4));

        JLabel username = new JLabel("@" + user.getUsername(), SwingConstants.CENTER);
        username.setFont(new Font("Courier", Font.PLAIN, 16));
        username.setForeground(Color.GRAY);
        username.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(username);

        body.add(Box.createVerticalStrut(24));

        body.add(infoRow("Phone", user.getPhone() != null ? user.getPhone() : "—"));
        body.add(Box.createVerticalStrut(10));
        body.add(infoRow("Status", user.isDeleted() ? "Inactive" : "Active",
                user.isDeleted() ? new Color(220, 53, 69) : new Color(40, 167, 69)));

        body.add(Box.createVerticalStrut(24));

        JLabel rolesTitle = new JLabel("Roles & Permissions", SwingConstants.CENTER);
        rolesTitle.setFont(MyUtils.courierFont(18));
        rolesTitle.setForeground(ACCENT);
        rolesTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(rolesTitle);
        body.add(Box.createVerticalStrut(12));

        body.add(roleList());

        body.add(Box.createVerticalGlue());
        return body;
    }

    private JComponent roundAvatar() {
        String initial = user.getFullname() != null && !user.getFullname().trim().isEmpty()
                ? user.getFullname().trim().substring(0, 1).toUpperCase() : "?";
        JPanel circle = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 123, 255));
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
            }
        };
        circle.setOpaque(false);
        circle.setPreferredSize(new Dimension(110, 110));
        circle.setMaximumSize(new Dimension(110, 110));
        circle.setAlignmentX(Component.CENTER_ALIGNMENT);
        circle.setLayout(new BorderLayout());

        JLabel l = new JLabel(initial, SwingConstants.CENTER);
        l.setFont(new Font("Courier", Font.BOLD, 48));
        l.setForeground(Color.WHITE);
        circle.add(l);
        return circle;
    }

    private JPanel infoRow(String label, String value) {
        return infoRow(label, value, DARK);
    }

    private JPanel infoRow(String label, String value, Color valueColor) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JLabel l = new JLabel(label);
        l.setFont(new Font("Courier", Font.BOLD, 14));
        l.setForeground(Color.GRAY);

        JLabel v = new JLabel(value, SwingConstants.RIGHT);
        v.setFont(new Font("Courier", Font.BOLD, 14));
        v.setForeground(valueColor);

        row.add(l, BorderLayout.WEST);
        row.add(v, BorderLayout.EAST);
        return row;
    }

    private JPanel roleList() {
        JPanel wrap = new JPanel();
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setOpaque(false);

        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            JLabel none = new JLabel("No roles assigned", SwingConstants.CENTER);
            none.setFont(new Font("Courier", Font.PLAIN, 14));
            none.setForeground(Color.GRAY);
            none.setAlignmentX(Component.CENTER_ALIGNMENT);
            wrap.add(none);
            return wrap;
        }

        for (Role r : user.getRoles()) {
            wrap.add(roleRow(r));
            wrap.add(Box.createVerticalStrut(8));
        }
        return wrap;
    }

    private JPanel roleRow(Role r) {
        RoleType type = r.getRoleType();
        boolean view   = Boolean.TRUE.equals(r.getView());
        boolean change = Boolean.TRUE.equals(r.getChange());

        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(186, 209, 250)),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        JLabel role = new JLabel(capitalize(type.name()));
        role.setFont(new Font("Courier", Font.BOLD, 15));
        role.setForeground(ACCENT);
        row.add(role, BorderLayout.WEST);

        JPanel perms = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        perms.setOpaque(false);
        perms.add(permChip("View", view ? new Color(40, 167, 69) : new Color(200, 200, 200), view));
        perms.add(permChip("Change", change ? new Color(0, 123, 255) : new Color(200, 200, 200), change));
        row.add(perms, BorderLayout.EAST);

        return row;
    }

    private JLabel permChip(String label, Color bg, boolean enabled) {
        JLabel l = new JLabel(" " + label + " ", SwingConstants.CENTER);
        l.setOpaque(true);
        l.setBackground(bg);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("Courier", Font.BOLD, 12));
        return l;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 16));
        footer.setBackground(BG);
        footer.setBorder(BorderFactory.createEmptyBorder(8, 0, 20, 0));

        JButton deleteBtn = new JButton("Delete");
        deleteBtn.setFont(MyUtils.courierFont(15));
        deleteBtn.setBackground(new Color(220, 53, 69));
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setFocusPainted(false);
        deleteBtn.setBorderPainted(false);
        deleteBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        deleteBtn.setPreferredSize(new Dimension(160, 44));
        deleteBtn.addActionListener(e -> {
            if (deleteAction != null) deleteAction.run();
        });
        footer.add(deleteBtn);

        JButton editBtn = new JButton("Edit User");
        editBtn.setFont(MyUtils.courierFont(15));
        editBtn.setBackground(new Color(0, 123, 255));
        editBtn.setForeground(Color.WHITE);
        editBtn.setFocusPainted(false);
        editBtn.setBorderPainted(false);
        editBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        editBtn.setPreferredSize(new Dimension(160, 44));
        editBtn.addActionListener(e -> {
            if (editAction != null) editAction.run();
        });
        footer.add(editBtn);

        return footer;
    }

    public void setEditAction(Runnable r)   { this.editAction = r; }
    public void setDeleteAction(Runnable r) { this.deleteAction = r; }
}
