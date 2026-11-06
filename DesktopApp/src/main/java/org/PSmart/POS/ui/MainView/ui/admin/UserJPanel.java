package org.PSmart.POS.ui.MainView.ui.admin;

import org.PSmart.POS.Entities.Role;
import org.PSmart.POS.Entities.User;
import org.PSmart.POS.ui.MainView.ui.admin.AdminPanel;
import org.hibernate.Session;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.stream.Collectors;

public class UserJPanel extends JPanel {

    private User user;
    private final Session session;
    private final User loggedUser;

    private JLabel nameLabel;
    private JLabel usernameLabel;
    private JLabel phoneLabel;
    private JLabel rolesLabel;
    private JLabel statusLabel;

    public AdminPanel adminPanel;       // back-reference so rows can trigger refresh
    public Runnable clickRunnable;

    public UserJPanel(User user, Session session, User loggedUser) {
        this.user   = user;
        this.session = session;
        this.loggedUser = loggedUser;
        buildUI();
        feedData();
    }

    private void buildUI() {
        setLayout(new GridLayout(1, 5, 4, 0));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(235, 235, 235)),
                BorderFactory.createEmptyBorder(0, 6, 0, 0)));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        nameLabel     = styledLabel("", SwingConstants.LEFT);
        usernameLabel = styledLabel("", SwingConstants.CENTER);
        phoneLabel    = styledLabel("", SwingConstants.CENTER);
        rolesLabel    = styledLabel("", SwingConstants.CENTER);
        statusLabel   = styledLabel("", SwingConstants.CENTER);

        JPanel rolesPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 12));
        rolesPanel.setOpaque(false);
        rolesLabel = new JLabel("", SwingConstants.CENTER);
        rolesLabel.setFont(new Font("Courier New", Font.PLAIN, 11));
        rolesLabel.setForeground(new Color(52, 58, 64));
        rolesPanel.add(rolesLabel);

        add(nameLabel);
        add(usernameLabel);
        add(phoneLabel);
        add(rolesPanel);
        add(statusLabel);

        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (clickRunnable != null) clickRunnable.run();
            }
            @Override public void mouseEntered(MouseEvent e) {
                setBackground(new Color(235, 245, 255));
            }
            @Override public void mouseExited(MouseEvent e) {
                setBackground(Color.WHITE);
            }
        });
    }

    public void feedData() {
        nameLabel.setText("  " + user.getFullname());
        usernameLabel.setText("@" + user.getUsername());
        phoneLabel.setText(user.getPhone() != null ? user.getPhone() : "—");
        rolesLabel.setText(formatRoles(user.getRoles()));

        if (user.isDeleted()) {
            statusLabel.setText("Inactive");
            statusLabel.setForeground(new Color(220, 53, 69));
        } else {
            statusLabel.setText("Active");
            statusLabel.setForeground(new Color(40, 167, 69));
        }
        repaint();
    }

    private String formatRoles(List<Role> roles) {
        if (roles == null || roles.isEmpty()) return "No roles";
        return roles.stream()
                .map(r -> r.getRoleType().name())
                .collect(Collectors.joining(", "));
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setAdminPanel(AdminPanel adminPanel) {
        this.adminPanel = adminPanel;
    }

    public void setClickRunnable(Runnable r) {
        this.clickRunnable = r;
    }

    private JLabel styledLabel(String text, int alignment) {
        JLabel l = new JLabel(text, alignment);
        l.setFont(new Font("Courier New", Font.PLAIN, 13));
        l.setForeground(new Color(52, 58, 64));
        return l;
    }
}