package org.PSmart.POS.ui.MainView.ui.admin;

import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.Role;
import org.PSmart.POS.Entities.RoleType;
import org.PSmart.POS.Entities.User;
import org.PSmart.POS.Repositories.RoleRepository;
import org.PSmart.POS.Repositories.UsersRepository;
import org.PSmart.POS.Utils.Components.ImageLabel;
import org.PSmart.POS.Utils.Components.PPasswordTextField;
import org.PSmart.POS.Utils.Components.PTextField;
import org.PSmart.POS.Utils.MyUtils;
import org.PSmart.POS.tests.AutoClosingNotification;
import org.hibernate.Session;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog for both creating a new User and editing an existing one.
 * Pass {@code null} as {@code user} to create a new user.
 * UI mirrors the Add/Edit Item form design for consistency.
 */
public class EditUserDialog extends JFrame {

    private final User existingUser;   // null → create mode
    private final Session session;
    private final User loggedUser;

    private Dimension len = Toolkit.getDefaultToolkit().getScreenSize();

    private final UsersRepository usersRepo = DatabaseUtils.getInstance().getUsersRepository();
    private final RoleRepository roleRepo   = DatabaseUtils.getInstance().getRoleRepository();

    private PTextField fullnameField;
    private PTextField usernameField;
    private PPasswordTextField passwordField;
    private PTextField phoneField;

    private JButton save;
    private JButton delete;

    // Role checkboxes + permission toggles
    private JCheckBox adminCheck, stockCheck, cashierCheck;
    private JCheckBox adminView, adminChange;
    private JCheckBox stockView, stockChange;
    private JCheckBox cashierView, cashierChange;

    private JCheckBox expenseCheck, purchaseCheck, settingsCheck, reportsCheck;
    private JCheckBox expenseView, expenseChange;
    private JCheckBox purchaseView, purchaseChange;
    private JCheckBox settingsView, settingsChange;
    private JCheckBox reportsView, reportsChange;

    private JCheckBox deletedCheck;

    private Runnable saveAction;
    private Runnable deleteAction;

    public EditUserDialog(User user, Session session, User loggedUser) {
        this.existingUser = user;
        this.session      = session;
        this.loggedUser   = loggedUser;
        draw();
        configValues();
        repaint();
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------------ UI

    private void draw() {
        setLayout(null);
        setSize(len.width * 2 / 3, len.height - 100);
        getContentPane().setBackground(Color.WHITE);
        setTitle("Add New User");
        if (existingUser != null)
            setTitle("Edit User " + existingUser.getFullname());

        setVisible(true);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        int baseh = 30;
        int hspacing = 78;

        fullnameField = new PTextField(new Color(0, 0, 137), new Color(0, 0, 255), "Full Name", null);
        fullnameField.setSize(getWidth() * 3 / 5 - getWidth() / 10, baseh);
        fullnameField.setLocation(getWidth() / 20, baseh);
        add(fullnameField);

        usernameField = new PTextField(new Color(0, 0, 137), new Color(0, 0, 255), "Username", null);
        usernameField.setSize(getWidth() * 3 / 5 - getWidth() / 10, baseh);
        usernameField.setLocation(getWidth() / 20, baseh + hspacing);
        add(usernameField);

        passwordField = new PPasswordTextField(new Color(0, 0, 137), new Color(0, 0, 255), "Password");
        passwordField.setSize(getWidth() * 3 / 5 - getWidth() / 10, baseh);
        passwordField.setLocation(getWidth() / 20, baseh + hspacing * 2);
        add(passwordField);

        phoneField = new PTextField(new Color(0, 0, 137), new Color(0, 0, 255), "Phone Number", null);
        phoneField.setSize(getWidth() * 3 / 5 - getWidth() / 10, baseh);
        phoneField.setLocation(getWidth() / 20, baseh + hspacing * 3);
        add(phoneField);

        imageLabel = new ImageLabel(null, 200, 200);
        imageLabel.setLocation(getWidth() - 210, 2);
        imageLabel.setSize(200, 200);
        add(imageLabel);

        uploadImage = new JButton("Choose Photo");
        uploadImage.setSize(190, 30);
        uploadImage.setLocation(getWidth() - 210, 215);
        add(uploadImage);

        // Roles section
        baseh += hspacing * 4;
        JLabel rolesTitle = new JLabel("Roles & Permissions", JLabel.CENTER);
        rolesTitle.setFont(MyUtils.courierFont(18));
        rolesTitle.setForeground(new Color(0, 0, 137));
        rolesTitle.setSize(getWidth() * 3 / 5, 30);
        rolesTitle.setLocation(getWidth() / 20, baseh);
        add(rolesTitle);

        baseh += 40;

        adminCheck  = new JCheckBox("ADMIN");
        adminView   = new JCheckBox("View");
        adminChange = new JCheckBox("Change");
        baseh = addRoleRow(baseh, adminCheck, adminView, adminChange);

        stockCheck  = new JCheckBox("STOCK");
        stockView   = new JCheckBox("View");
        stockChange = new JCheckBox("Change");
        baseh = addRoleRow(baseh, stockCheck, stockView, stockChange);

        cashierCheck  = new JCheckBox("CASHIER");
        cashierView   = new JCheckBox("View");
        cashierChange = new JCheckBox("Change");
        baseh = addRoleRow(baseh, cashierCheck, cashierView, cashierChange);

        expenseCheck  = new JCheckBox("EXPENSE");
        expenseView   = new JCheckBox("View");
        expenseChange = new JCheckBox("Change");
        baseh = addRoleRow(baseh, expenseCheck, expenseView, expenseChange);

        purchaseCheck  = new JCheckBox("PURCHASE");
        purchaseView   = new JCheckBox("View");
        purchaseChange = new JCheckBox("Change");
        baseh = addRoleRow(baseh, purchaseCheck, purchaseView, purchaseChange);

        settingsCheck  = new JCheckBox("SETTINGS");
        settingsView   = new JCheckBox("View");
        settingsChange = new JCheckBox("Change");
        baseh = addRoleRow(baseh, settingsCheck, settingsView, settingsChange);

        reportsCheck  = new JCheckBox("REPORTS");
        reportsView   = new JCheckBox("View");
        reportsChange = new JCheckBox("Change");
        baseh = addRoleRow(baseh, reportsCheck, reportsView, reportsChange);

        if (existingUser != null) {
            baseh += 12;
            deletedCheck = new JCheckBox("Mark as Deleted / Inactive");
            deletedCheck.setFont(MyUtils.courierFont(15));
            deletedCheck.setForeground(new Color(220, 53, 69));
            deletedCheck.setBounds(getWidth() / 20, baseh, getWidth() * 3 / 5, 30);
            add(deletedCheck);
            baseh += 35;
        }

        baseh += 25;

        if (existingUser != null) {
            delete = MyUtils.createButton("Delete User", Color.RED);
            delete.setSize(250, 50);
            delete.setLocation(getWidth() / 4 - 125, baseh);
            delete.setForeground(Color.WHITE);
            delete.setFont(MyUtils.courierFont(25));
            add(delete);
        }

        save = MyUtils.createButton(existingUser != null ? "Save User" : "Add User", Color.BLUE);
        save.setSize(250, 50);
        if (existingUser != null)
            save.setLocation(getWidth() - getWidth() / 4 - 125, baseh);
        else
            save.setLocation(getWidth() / 2 - 125, baseh);
        save.setForeground(Color.WHITE);
        save.setFont(MyUtils.courierFont(25));
        add(save);

        baseh += 80;
        setSize(len.width * 2 / 3, baseh);
        repaint();
    }

    private ImageLabel imageLabel;
    private JButton uploadImage;

    private int addRoleRow(int baseh, JCheckBox role, JCheckBox viewCb, JCheckBox changeCb) {
        int rowWidth = getWidth() * 3 / 5;
        int xLeft = getWidth() / 20;

        role.setFont(MyUtils.courierFont(15));
        role.setBackground(Color.WHITE);
        role.setBounds(xLeft, baseh, 160, 28);

        viewCb.setFont(MyUtils.courierFont(14));
        viewCb.setBackground(Color.WHITE);
        viewCb.setBounds(xLeft + 180, baseh, 90, 28);

        changeCb.setFont(MyUtils.courierFont(14));
        changeCb.setBackground(Color.WHITE);
        changeCb.setBounds(xLeft + 290, baseh, 120, 28);

        // enable view/change only when the role is checked
        viewCb.setEnabled(false);
        changeCb.setEnabled(false);
        role.addActionListener(e -> {
            viewCb.setEnabled(role.isSelected());
            changeCb.setEnabled(role.isSelected());
            if (!role.isSelected()) {
                viewCb.setSelected(false);
                changeCb.setSelected(false);
            }
        });

        add(role);
        add(viewCb);
        add(changeCb);

        return baseh + 34;
    }

    // ------------------------------------------------------------------ populate (edit mode)

    private void configValues() {
        if (existingUser == null) {
            if (delete != null) delete.setEnabled(false);
        } else {
            fullnameField.setText(existingUser.getFullname());
            usernameField.setText(existingUser.getUsername());
            phoneField.setText(existingUser.getPhone() != null ? existingUser.getPhone() : "");
            if (deletedCheck != null) deletedCheck.setSelected(existingUser.isDeleted());

            List<Role> roles = existingUser.getRoles();
            if (roles != null) {
                for (Role r : roles) {
                    switch (r.getRoleType()) {
                        case ADMIN -> {
                            adminCheck.setSelected(true);
                            adminView.setEnabled(true);   adminView.setSelected(Boolean.TRUE.equals(r.getView()));
                            adminChange.setEnabled(true); adminChange.setSelected(Boolean.TRUE.equals(r.getChange()));
                        }
                        case STOCK -> {
                            stockCheck.setSelected(true);
                            stockView.setEnabled(true);   stockView.setSelected(Boolean.TRUE.equals(r.getView()));
                            stockChange.setEnabled(true); stockChange.setSelected(Boolean.TRUE.equals(r.getChange()));
                        }
                        case CASHIER -> {
                            cashierCheck.setSelected(true);
                            cashierView.setEnabled(true);   cashierView.setSelected(Boolean.TRUE.equals(r.getView()));
                            cashierChange.setEnabled(true); cashierChange.setSelected(Boolean.TRUE.equals(r.getChange()));
                        }
                        case EXPENSE -> {
                            expenseCheck.setSelected(true);
                            expenseView.setEnabled(true);   expenseView.setSelected(Boolean.TRUE.equals(r.getView()));
                            expenseChange.setEnabled(true); expenseChange.setSelected(Boolean.TRUE.equals(r.getChange()));
                        }
                        case PURCHASE -> {
                            purchaseCheck.setSelected(true);
                            purchaseView.setEnabled(true);   purchaseView.setSelected(Boolean.TRUE.equals(r.getView()));
                            purchaseChange.setEnabled(true); purchaseChange.setSelected(Boolean.TRUE.equals(r.getChange()));
                        }
                        case SETTINGS -> {
                            settingsCheck.setSelected(true);
                            settingsView.setEnabled(true);   settingsView.setSelected(Boolean.TRUE.equals(r.getView()));
                            settingsChange.setEnabled(true); settingsChange.setSelected(Boolean.TRUE.equals(r.getChange()));
                        }
                        case REPORTS -> {
                            reportsCheck.setSelected(true);
                            reportsView.setEnabled(true);   reportsView.setSelected(Boolean.TRUE.equals(r.getView()));
                            reportsChange.setEnabled(true); reportsChange.setSelected(Boolean.TRUE.equals(r.getChange()));
                        }
                    }
                }
            }
        }

        if (delete != null) {
            delete.addActionListener(e -> onDelete());
        }
        save.addActionListener(e -> onSave());
    }

    // ------------------------------------------------------------------ actions

    private void onSave() {
        if(!org.PSmart.POS.Utils.AuthorizationUtils.authorizeAction(this, loggedUser,
                RoleType.ADMIN, existingUser != null ? "Edit User" : "Create User")) return;

        String fullname = fullnameField.getText().trim();
        String username = usernameField.getText().trim();
        String phone    = phoneField.getText().trim();
        String password = passwordField.getText().trim();

        if (fullname.isEmpty() || username.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Full name and username are required.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        User u = existingUser != null ? existingUser : new User();
        u.setFullname(fullname);
        u.setUsername(username);
        u.setPhone(phone.isEmpty() ? null : phone);
        if (!password.isEmpty()) u.setPassword(password); // hash in production!
        if (deletedCheck != null) u.setDeleted(deletedCheck.isSelected());

        usersRepo.save(u);

        // Rebuild roles: delete old ones, persist new
        if (existingUser != null && existingUser.getRoles() != null) {
            for (Role old : existingUser.getRoles()) roleRepo.delete(old);
        }

        List<Role> newRoles = new ArrayList<>();
        if (adminCheck.isSelected())   newRoles.add(makeRole(u, RoleType.ADMIN,   adminView.isSelected(),   adminChange.isSelected()));
        if (stockCheck.isSelected())   newRoles.add(makeRole(u, RoleType.STOCK,   stockView.isSelected(),   stockChange.isSelected()));
        if (cashierCheck.isSelected()) newRoles.add(makeRole(u, RoleType.CASHIER, cashierView.isSelected(), cashierChange.isSelected()));
        if (expenseCheck.isSelected()) newRoles.add(makeRole(u, RoleType.EXPENSE, expenseView.isSelected(), expenseChange.isSelected()));
        if (purchaseCheck.isSelected()) newRoles.add(makeRole(u, RoleType.PURCHASE, purchaseView.isSelected(), purchaseChange.isSelected()));
        if (settingsCheck.isSelected()) newRoles.add(makeRole(u, RoleType.SETTINGS, settingsView.isSelected(), settingsChange.isSelected()));
        if (reportsCheck.isSelected()) newRoles.add(makeRole(u, RoleType.REPORTS, reportsView.isSelected(), reportsChange.isSelected()));

        for (Role r : newRoles) roleRepo.save(r);

        AutoClosingNotification.sendNotification("User saved");
        dispose();
        if (saveAction != null) saveAction.run();
    }

    private Role makeRole(User u, RoleType type, boolean view, boolean change) {
        Role r = new Role();
        r.setUser(u);
        r.setRoleType(type);
        r.setView(view);
        r.setChange(change);
        return r;
    }

    private void onDelete() {
        if(!org.PSmart.POS.Utils.AuthorizationUtils.authorizeAction(this, loggedUser,
                RoleType.ADMIN, "Delete User")) return;

        int confirm = JOptionPane.showConfirmDialog(this,
                "Permanently delete this user?", "Confirm Delete",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            if (existingUser.getRoles() != null)
                existingUser.getRoles().forEach(roleRepo::delete);
            existingUser.setDeleted(true);   // soft-delete; use usersRepo.delete() for hard-delete
            usersRepo.save(existingUser);
            AutoClosingNotification.sendNotification("User deleted");
            dispose();
            if (deleteAction != null) deleteAction.run();
        }
    }

    // ------------------------------------------------------------------ setters

    public void setSaveAction(Runnable r)   { this.saveAction = r; }
    public void setDeleteAction(Runnable r) { this.deleteAction = r; }
}
