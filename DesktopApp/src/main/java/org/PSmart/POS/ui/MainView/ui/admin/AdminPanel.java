package org.PSmart.POS.ui.MainView.ui.admin;

import org.PSmart.POS.Entities.User;
import org.PSmart.POS.ui.MainView.DashBoard;
import org.PSmart.POS.Utils.Components.forms.Ui;
import org.PSmart.POS.Utils.MyUtils;

import javax.swing.*;
import java.awt.*;

/** Admin: Users and Roles. (Store configuration lives under the Settings page.) */
public class AdminPanel extends JPanel {

    private final JPanel cards = new JPanel(new CardLayout());
    private final JButton usersBtn = tab("Users");
    private final JButton rolesBtn = tab("Roles");

    public AdminPanel(DashBoard dash) {
        super(new BorderLayout());
        User logged = dash.getLoggedUser();
        setBackground(Ui.PAGE);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        bar.setBackground(Ui.SLATE);
        bar.add(usersBtn);
        bar.add(rolesBtn);
        add(bar, BorderLayout.NORTH);

        cards.add(new UsersPanel(logged), "USERS");
        cards.add(new RolesPanel(logged), "ROLES");
        add(cards, BorderLayout.CENTER);

        usersBtn.addActionListener(e -> show("USERS"));
        rolesBtn.addActionListener(e -> show("ROLES"));
        show("USERS");
    }

    private static JButton tab(String text) {
        JButton b = MyUtils.createButton(text, Ui.BLUE);
        b.setForeground(Color.WHITE);
        b.setFont(MyUtils.courierFont(16));
        b.setPreferredSize(new Dimension(130, 34));
        return b;
    }

    private void show(String key) {
        ((CardLayout) cards.getLayout()).show(cards, key);
        usersBtn.setBackground("USERS".equals(key) ? new Color(0, 0, 137) : Ui.BLUE);
        rolesBtn.setBackground("ROLES".equals(key) ? new Color(0, 0, 137) : Ui.BLUE);
    }
}
