package org.PSmart.POS.Utils.Components.GridSearch.CustomerComponents;

import org.PSmart.POS.Entities.Customer;
import org.PSmart.POS.Utils.MyUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** A simple, friendly customer card for the customer grid-search. Shows account status. */
public class CustomerCardJpanel extends JPanel {

    private final Customer customer;
    public Runnable actionRunnable;

    private static final int WIDTH = 260;
    private static final int HEIGHT = 120;

    public CustomerCardJpanel(Customer customer) {
        super();
        this.customer = customer;
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 225, 230)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setMaximumSize(new Dimension(WIDTH, HEIGHT));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setLayout(new BorderLayout(8, 4));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel name = new JLabel(customer.getFullname());
        name.setFont(MyUtils.courierFont(15).deriveFont(Font.BOLD));
        name.setForeground(new Color(30, 30, 30));
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        info.add(name);

        JLabel phone = new JLabel(customer.getPhone() == null || customer.getPhone().trim().isEmpty()
                ? "No phone" : customer.getPhone());
        phone.setFont(MyUtils.courierFont(12));
        phone.setForeground(new Color(130, 130, 130));
        phone.setAlignmentX(Component.LEFT_ALIGNMENT);
        info.add(phone);

        info.add(Box.createVerticalStrut(8));

        double balance = customer.getCurrentBalance() != null ? customer.getCurrentBalance() : 0.0;
        double limit = customer.getCreditLimit() != null ? customer.getCreditLimit() : 0.0;
        double remaining = Math.max(0, limit - balance);

        JLabel balanceLabel = new JLabel("Balance  " + MyUtils.formatCurrency(String.valueOf(balance)));
        balanceLabel.setFont(MyUtils.courierFont(13).deriveFont(Font.BOLD));
        balanceLabel.setForeground(balance > 0.001 ? new Color(220, 53, 69) : new Color(40, 167, 69));
        balanceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        info.add(balanceLabel);

        JLabel limitLabel = new JLabel("Limit  " + MyUtils.formatCurrency(String.valueOf(limit))
                + "   Remaining  " + MyUtils.formatCurrency(String.valueOf(remaining)));
        limitLabel.setFont(MyUtils.courierFont(12));
        limitLabel.setForeground(new Color(90, 90, 90));
        limitLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        info.add(limitLabel);

        add(info, BorderLayout.CENTER);
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(25, 118, 211), 3),
                        BorderFactory.createEmptyBorder(10, 12, 10, 12)));
                if (actionRunnable != null) actionRunnable.run();
            }
        });
    }

    public Customer getCustomer() {
        return customer;
    }
}