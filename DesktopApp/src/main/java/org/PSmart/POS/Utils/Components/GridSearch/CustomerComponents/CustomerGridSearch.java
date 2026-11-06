package org.PSmart.POS.Utils.Components.GridSearch.CustomerComponents;

import org.PSmart.POS.Entities.Customer;
import org.PSmart.POS.Utils.Components.PTextField;
import org.PSmart.POS.Utils.IconsUtils;
import org.PSmart.POS.Utils.MyUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Friendly, grid-based customer picker (mirrors the items grid search).
 * Shows searchable customer cards with credit limit, balance and remaining credit,
 * plus a Walk-in option. Selection is delivered to the provided consumer.
 */
public class CustomerGridSearch extends JFrame {

    private PTextField search;
    private JPanel playground;
    private final List<Customer> allCustomers;
    private final HashMap<UUID, CustomerCardJpanel> cards = new HashMap<>();
    private JLabel selectedBanner;
    private Customer selected;

    private final Consumer<Customer> onSelected;
    private static final Dimension SCREEN = Toolkit.getDefaultToolkit().getScreenSize();

    public CustomerGridSearch(Consumer<Customer> onSelected) {
        super();
        this.onSelected = onSelected;
        allCustomers = new ArrayList<>(
                org.PSmart.POS.Database.DatabaseUtils.getInstance().getCustomerRepository().findAllCustomers());
        draw();
    }

    private void draw() {
        getContentPane().setLayout(new BorderLayout(10, 10));
        setTitle("Select Customer");
        setSize(700, SCREEN.height < 900 ? 560 : 680);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(240, 240, 245));

        search = new PTextField(new Color(52, 58, 64), new Color(0, 0, 200), "Search name or phone",
                IconsUtils.getIcon("search.png"));
        search.setPreferredSize(new Dimension(0, 56));
        search.getField().addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                new Thread(() -> filter(search.getText())).start();
            }
        });

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setBackground(new Color(240, 240, 245));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(14, 14, 0, 14));
        searchPanel.add(search, BorderLayout.CENTER);
        add(searchPanel, BorderLayout.NORTH);

        playground = new JPanel(new GridLayout(0, 3, 12, 12));
        playground.setBackground(new Color(240, 240, 245));

        JScrollPane scroll = new JScrollPane(playground);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(new Color(240, 240, 245));
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        add(scroll, BorderLayout.CENTER);

        selectedBanner = new JLabel("  No customer selected - Sale will be WALK-IN",
                new ImageIcon(IconsUtils.getIcon("user.png")), SwingConstants.LEFT);
        selectedBanner.setFont(MyUtils.courierFont(14));
        selectedBanner.setForeground(new Color(40, 40, 60));
        selectedBanner.setOpaque(true);
        selectedBanner.setBackground(new Color(224, 230, 245));
        selectedBanner.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JButton walkIn = MyUtils.createButton("Sale as Walk-in", new Color(108, 117, 125));
        walkIn.setFont(MyUtils.courierFont(15));
        walkIn.setForeground(Color.WHITE);
        walkIn.setFocusPainted(false);
        walkIn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        walkIn.addActionListener(e -> close(null));

        JButton select = MyUtils.createButton("Select Customer", new Color(40, 167, 69));
        select.setFont(MyUtils.courierFont(15));
        select.setForeground(Color.WHITE);
        select.setFocusPainted(false);
        select.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        select.addActionListener(e -> close(selected));

        JButton cancel = MyUtils.createButton("Cancel", new Color(220, 53, 69));
        cancel.setFont(MyUtils.courierFont(15));
        cancel.setForeground(Color.WHITE);
        cancel.setFocusPainted(false);
        cancel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancel.addActionListener(e -> dispose());

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 8));
        actionRow.setBackground(new Color(240, 240, 245));
        actionRow.add(walkIn);
        actionRow.add(select);
        actionRow.add(cancel);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(240, 240, 245));
        footer.add(selectedBanner, BorderLayout.CENTER);
        footer.add(actionRow, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH);
        refreshFooter();

        filter("");
        setVisible(true);
    }

    private void refreshFooter() {
        if (selected == null) {
            selectedBanner.setText("  No customer selected - Sale will be WALK-IN");
        } else {
            double balance = selected.getCurrentBalance() != null ? selected.getCurrentBalance() : 0.0;
            double limit = selected.getCreditLimit() != null ? selected.getCreditLimit() : 0.0;
            selectedBanner.setText("  " + selected.getFullname()
                    + "   |   Balance " + MyUtils.formatCurrency(String.valueOf(balance))
                    + "   |   Limit " + MyUtils.formatCurrency(String.valueOf(limit)));
        }
    }

    private void filter(String str) {
        String q = str == null ? "" : str.trim().toLowerCase();
        playground.removeAll();
        for (Customer c : allCustomers) {
            String name = c.getFullname() == null ? "" : c.getFullname().toLowerCase();
            String phone = c.getPhone() == null ? "" : c.getPhone().toLowerCase();
            if (!q.isEmpty() && !name.contains(q) && !phone.contains(q)) continue;

            CustomerCardJpanel p = cards.get(c.getUid());
            boolean created = false;
            if (p == null) {
                p = new CustomerCardJpanel(c);
                cards.put(c.getUid(), p);
                created = true;
            }
            final CustomerCardJpanel card = p;
            if (created) {
                card.actionRunnable = () -> {
                    selected = card.getCustomer();
                    refreshFooter();
                };
            }
            playground.add(card);
        }
        playground.revalidate();
        playground.repaint();
    }

    private void close(Customer chosen) {
        dispose();
        if (onSelected != null) onSelected.accept(chosen);
    }
}