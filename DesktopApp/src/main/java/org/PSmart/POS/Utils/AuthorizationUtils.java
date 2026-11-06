package org.PSmart.POS.Utils;

import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.Role;
import org.PSmart.POS.Entities.RoleType;
import org.PSmart.POS.Entities.User;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class AuthorizationUtils {

    public static boolean hasPermission(User user, RoleType roleType, boolean requiresChange) {
        if (user == null || user.getRoles() == null) return false;
        for (Role role : user.getRoles()) {
            if (role.getRoleType() == roleType) {
                if (requiresChange) {
                    return Boolean.TRUE.equals(role.getChange());
                }
                return Boolean.TRUE.equals(role.getView());
            }
        }
        return false;
    }

    public static boolean hasRole(User user, RoleType roleType) {
        if (user == null || user.getRoles() == null) return false;
        for (Role role : user.getRoles()) {
            if (role.getRoleType() == roleType) {
                return true;
            }
        }
        return false;
    }

    public static User promptReAuthorization(Component parent, String actionDescription) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel infoLabel = new JLabel("Authorization required to: " + actionDescription);
        infoLabel.setFont(new Font("Courier", Font.BOLD, 14));
        infoLabel.setForeground(new Color(220, 53, 69));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(infoLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1; gbc.gridx = 0;
        panel.add(new JLabel("Username:"), gbc);
        JTextField usernameField = new JTextField(20);
        gbc.gridx = 1;
        panel.add(usernameField, gbc);

        gbc.gridy = 2; gbc.gridx = 0;
        panel.add(new JLabel("Password:"), gbc);
        JPasswordField passwordField = new JPasswordField(20);
        gbc.gridx = 1;
        panel.add(passwordField, gbc);

        int result = JOptionPane.showConfirmDialog(parent, panel,
                "Authorize Action", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String username = usernameField.getText().trim().toLowerCase();
            String password = new String(passwordField.getPassword()).trim();

            if (username.isEmpty() || password.isEmpty()) return null;

            try {
                String hql = "FROM User u WHERE u.username = :username AND u.deleted = false";
                User authUser = DatabaseUtils.getInstance().getEmf().createEntityManager()
                        .createQuery(hql, User.class)
                        .setParameter("username", username)
                        .getSingleResult();

                if (authUser != null && authUser.getPassword().equals(password)) {
                    return authUser;
                }
            } catch (Exception e) {
                // User not found
            }
        }
        return null;
    }

    public static boolean authorizeAction(Component parent, User currentUser, RoleType roleType,
                                         String actionDescription) {
        if (hasPermission(currentUser, roleType, true)) {
            return true;
        }

        User authUser = promptReAuthorization(parent, actionDescription);
        if (authUser == null) {
            JOptionPane.showMessageDialog(parent, "Authorization failed.", "Access Denied", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (!hasPermission(authUser, roleType, true)) {
            JOptionPane.showMessageDialog(parent,
                    "User '" + authUser.getFullname() + "' does not have permission for: " + actionDescription,
                    "Insufficient Permissions", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    public static boolean authorizeView(Component parent, User currentUser, RoleType roleType,
                                       String actionDescription) {
        if (hasPermission(currentUser, roleType, false)) {
            return true;
        }

        User authUser = promptReAuthorization(parent, actionDescription);
        if (authUser == null) {
            JOptionPane.showMessageDialog(parent, "Authorization failed.", "Access Denied", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (!hasPermission(authUser, roleType, false)) {
            JOptionPane.showMessageDialog(parent,
                    "User '" + authUser.getFullname() + "' does not have permission for: " + actionDescription,
                    "Insufficient Permissions", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }
}
