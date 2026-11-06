package org.PSmart.POS.ui.MainView.ui.customers;

import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.Customer;
import org.PSmart.POS.Entities.User;
import org.PSmart.POS.Repositories.CustomerRepository;
import org.PSmart.POS.Utils.Components.PTextField;
import org.PSmart.POS.Utils.IconsUtils;
import org.PSmart.POS.Utils.MyUtils;
import org.PSmart.POS.tests.AutoClosingNotification;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CustomersPanel extends JPanel {

    private CustomerRepository customerRepository;
    private PTextField searchField;
    private DefaultTableModel tableModel;
    private JTable customersTable;
    private List<Customer> allCustomers;

    public CustomersPanel() {
        super();
        this.customerRepository = DatabaseUtils.getInstance().getCustomerRepository();
        draw();
        loadCustomers();
    }

    private void draw() {
        setLayout(new BorderLayout(0, 0));
        setBackground(new Color(245, 245, 250));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTableArea(), BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBackground(new Color(52, 58, 64));
        header.setPreferredSize(new Dimension(0, 70));
        header.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));

        JLabel title = new JLabel("Customers");
        title.setFont(new Font("Courier", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 15));
        rightPanel.setOpaque(false);

        searchField = new PTextField(new Color(200, 200, 200), Color.WHITE, "Search...", IconsUtils.getIcon("search.png"));
        searchField.setPreferredSize(new Dimension(250, 45));
        searchField.getField().addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                filterCustomers();
            }
        });
        rightPanel.add(searchField);

        JButton addBtn = new JButton("+ Add Customer");
        addBtn.setFont(new Font("Courier", Font.BOLD, 13));
        addBtn.setBackground(new Color(40, 167, 69));
        addBtn.setForeground(Color.WHITE);
        addBtn.setBorderPainted(false);
        addBtn.setFocusPainted(false);
        addBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addBtn.addActionListener(e -> openAddCustomerDialog());
        rightPanel.add(addBtn);

        JButton ledgerBtn = new JButton("View Ledger");
        ledgerBtn.setFont(new Font("Courier", Font.BOLD, 13));
        ledgerBtn.setBackground(new Color(23, 162, 184));
        ledgerBtn.setForeground(Color.WHITE);
        ledgerBtn.setBorderPainted(false);
        ledgerBtn.setFocusPainted(false);
        ledgerBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        ledgerBtn.addActionListener(e -> viewSelectedLedger());
        rightPanel.add(ledgerBtn);

        JButton payBtn = new JButton("Collect Payment");
        payBtn.setFont(new Font("Courier", Font.BOLD, 13));
        payBtn.setBackground(new Color(0, 123, 255));
        payBtn.setForeground(Color.WHITE);
        payBtn.setBorderPainted(false);
        payBtn.setFocusPainted(false);
        payBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        payBtn.addActionListener(e -> collectSelectedPayment());
        rightPanel.add(payBtn);

        header.add(rightPanel, BorderLayout.EAST);

        return header;
    }

    private JScrollPane buildTableArea() {
        String[] columns = {"Name", "Phone", "Email", "Address", "Credit Limit", "Balance", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        customersTable = new JTable(tableModel);
        customersTable.setRowHeight(42);
        customersTable.setFont(new Font("Courier", Font.PLAIN, 13));
        customersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customersTable.setGridColor(new Color(240, 240, 240));
        customersTable.setBackground(Color.WHITE);

        JTableHeader header = customersTable.getTableHeader();
        header.setFont(new Font("Courier", Font.BOLD, 13));
        header.setBackground(new Color(52, 58, 64));
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 38));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < columns.length; i++) {
            customersTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        customersTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String s = value != null ? value.toString() : "";
                if (!isSelected) {
                    c.setForeground(s.contains("-") ? new Color(220, 53, 69) : new Color(60, 60, 60));
                }
                return c;
            }
        });

        customersTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    String s = value != null ? value.toString() : "";
                    c.setForeground("OWES".equals(s) ? new Color(220, 53, 69) : new Color(40, 167, 69));
                    if ("OWES".equals(s)) c.setFont(c.getFont().deriveFont(Font.BOLD));
                }
                return c;
            }
        });

        customersTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = customersTable.rowAtPoint(e.getPoint());
                if (row < 0) return;
                if (e.getClickCount() == 1) {
                    customersTable.setRowSelectionInterval(row, row);
                    openCustomerCard(allCustomers.get(row));
                } else if (e.getClickCount() == 2) {
                    editSelectedCustomer();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(customersTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        scrollPane.getViewport().setBackground(Color.WHITE);

        return scrollPane;
    }

    private void loadCustomers() {
        allCustomers = customerRepository.findAllCustomers();
        populateTable(allCustomers);
    }

    private void populateTable(List<Customer> customers) {
        tableModel.setRowCount(0);
        for (Customer c : customers) {
            double balance = c.getCurrentBalance() != null ? c.getCurrentBalance() : 0.0;
            double limit = c.getCreditLimit() != null ? c.getCreditLimit() : 0.0;
            String status = balance > 0.001 ? "OWES" : "Paid";
            tableModel.addRow(new Object[]{
                    c.getFullname(),
                    c.getPhone() != null ? c.getPhone() : "—",
                    c.getEmail() != null ? c.getEmail() : "—",
                    c.getAddress() != null ? c.getAddress() : "—",
                    "KES " + MyUtils.formatCurrency(String.valueOf(limit)),
                    "KES " + MyUtils.formatCurrency(String.valueOf(balance)),
                    status
            });
        }
    }

    private void filterCustomers() {
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            populateTable(allCustomers);
            return;
        }
        List<Customer> filtered = new ArrayList<>();
        for (Customer c : allCustomers) {
            if (c.getSearchString().contains(query)) {
                filtered.add(c);
            }
        }
        populateTable(filtered);
    }

    private void openAddCustomerDialog() {
        CustomerDialog dialog = new CustomerDialog((Frame) SwingUtilities.getWindowAncestor(this), null);
        dialog.setSaveAction(this::loadCustomers);
        dialog.setVisible(true);
    }

    private void editSelectedCustomer() {
        int row = customersTable.getSelectedRow();
        if (row < 0 || row >= allCustomers.size()) return;

        Customer customer = allCustomers.get(row);
        CustomerDialog dialog = new CustomerDialog((Frame) SwingUtilities.getWindowAncestor(this), customer);
        dialog.setSaveAction(this::loadCustomers);
        dialog.setDeleteAction(this::loadCustomers);
        dialog.setVisible(true);
    }

    private Customer selectedCustomerRow() {
        int row = customersTable.getSelectedRow();
        if (row < 0 || row >= allCustomers.size()) {
            JOptionPane.showMessageDialog(this, "Select a customer first.", "Select Customer", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return allCustomers.get(row);
    }

    private void viewSelectedLedger() {
        Customer c = selectedCustomerRow();
        if (c == null) return;
        new LedgerDialog((Frame) SwingUtilities.getWindowAncestor(this), c, this::loadCustomers).setVisible(true);
    }

    private void collectSelectedPayment() {
        Customer c = selectedCustomerRow();
        if (c == null) return;
        double balance = c.getCurrentBalance() != null ? c.getCurrentBalance() : 0.0;
        if (balance <= 0.001) {
            JOptionPane.showMessageDialog(this, "This customer has no outstanding balance.", "Collect Payment",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        new PaymentDialog((Frame) SwingUtilities.getWindowAncestor(this), c, this::loadCustomers).setVisible(true);
    }

    /** Open a full customer card (profile) with ledger/payment/edit actions. */
    private void openCustomerCard(Customer c) {
        CustomerCardDialog dialog = new CustomerCardDialog((Frame) SwingUtilities.getWindowAncestor(this), c);
        dialog.setVisible(true);
        loadCustomers();
    }

    /** Customer profile card opened from a table row. Shows details + actions. */
    private class CustomerCardDialog extends JDialog {
        private final Customer customer;

        CustomerCardDialog(java.awt.Window owner, Customer customer) {
            super(owner, "Customer Card", Dialog.ModalityType.APPLICATION_MODAL);
            this.customer = customer;
            setSize(520, 560);
            setResizable(false);
            setLocationRelativeTo(owner);
            buildUI();
        }

        private JLabel kv(String label, String value) {
            JLabel l = new JLabel("<html><b>" + label + ":</b> " + value + "</html>");
            l.setFont(new Font("Courier", Font.PLAIN, 14));
            l.setForeground(new Color(52, 58, 64));
            l.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
            return l;
        }

        private void buildUI() {
            setLayout(new BorderLayout(12, 12));
            ((JPanel) getContentPane()).setBackground(Color.WHITE);

            JPanel header = new JPanel();
            header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
            header.setBackground(new Color(25, 118, 211));
            header.setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));

            JLabel nameL = new JLabel(customer.getFullname());
            nameL.setFont(new Font("Courier", Font.BOLD, 24));
            nameL.setForeground(Color.WHITE);
            nameL.setAlignmentX(Component.LEFT_ALIGNMENT);
            header.add(nameL);

            double balance = customer.getCurrentBalance() != null ? customer.getCurrentBalance() : 0.0;
            JLabel balanceL = new JLabel(balance > 0.001
                    ? "OWES  KES " + String.format("%,.2f", balance)
                    : "No outstanding balance");
            balanceL.setFont(new Font("Courier", Font.BOLD, 16));
            balanceL.setForeground(balance > 0.001 ? new Color(255, 205, 97) : new Color(200, 255, 210));
            balanceL.setAlignmentX(Component.LEFT_ALIGNMENT);
            balanceL.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
            header.add(balanceL);

            add(header, BorderLayout.NORTH);

            JPanel body = new JPanel();
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.setBackground(Color.WHITE);
            body.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));

            body.add(kv("Phone", safe(customer.getPhone())));
            body.add(kv("Email", safe(customer.getEmail())));
            body.add(kv("Address", safe(customer.getAddress())));
            body.add(kv("Credit Limit", customer.getCreditLimit() != null
                    ? "KES " + String.format("%,.2f", customer.getCreditLimit()) : "—"));
            body.add(kv("Used Credit", balance > 0.001
                    ? "KES " + String.format("%,.2f", balance) : "KES 0.00"));
            body.add(kv("Status", balance > 0.001 ? "OWES" : "PAID"));

            JPanel buttons = new JPanel(new GridLayout(1, 3, 10, 10));
            buttons.setBackground(Color.WHITE);
            buttons.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

            buttons.add(makeCardButton("View Ledger", new Color(23, 162, 184), () -> {
                new LedgerDialog(this, customer, () -> { dispose(); loadCustomers(); }).setVisible(true);
            }));
            buttons.add(makeCardButton("Collect Payment", new Color(0, 123, 255), () -> {
                new PaymentDialog(this, customer, () -> { dispose(); loadCustomers(); }).setVisible(true);
            }));
            buttons.add(makeCardButton("Edit", new Color(108, 117, 125), () -> {
                CustomerDialog d = new CustomerDialog((Frame) SwingUtilities.getWindowAncestor(this), customer);
                d.setSaveAction(() -> { dispose(); loadCustomers(); });
                d.setDeleteAction(() -> { dispose(); loadCustomers(); });
                d.setVisible(true);
            }));

            body.add(buttons);

            JLabel hint = new JLabel("Tip: double-click a row to edit directly.");
            hint.setFont(new Font("Courier", Font.PLAIN, 11));
            hint.setForeground(new Color(150, 150, 150));
            hint.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
            body.add(hint);

            add(body, BorderLayout.CENTER);

            JButton close = new JButton("Close");
            close.setFont(new Font("Courier", Font.BOLD, 14));
            close.setForeground(Color.WHITE);
            close.setBackground(new Color(108, 117, 125));
            close.setBorderPainted(false);
            close.setFocusPainted(false);
            close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            close.addActionListener(e -> dispose());
            JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            foot.setBackground(Color.WHITE);
            foot.setBorder(BorderFactory.createEmptyBorder(0, 20, 18, 20));
            foot.add(close);
            add(foot, BorderLayout.SOUTH);
        }

        private JButton makeCardButton(String text, Color bg, Runnable action) {
            JButton b = new JButton(text);
            b.setFont(new Font("Courier", Font.BOLD, 13));
            b.setBackground(bg);
            b.setForeground(Color.WHITE);
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addActionListener(e -> action.run());
            return b;
        }

        private String safe(Object o) {
            return o == null || o.toString().trim().isEmpty() ? "—" : o.toString();
        }
    }

    /** List a single customer's full credit ledger and lets the user record a payment. */
    private class LedgerDialog extends JDialog {
        private final Customer customer;
        private final Runnable afterSave;

        LedgerDialog(java.awt.Window owner, Customer customer, Runnable afterSave) {
            super(owner, "Credit Ledger - " + customer.getFullname(), Dialog.ModalityType.APPLICATION_MODAL);
            this.customer = customer;
            this.afterSave = afterSave;
            setSize(760, 520);
            setLocationRelativeTo(owner);
            buildUI();
        }

        private void buildUI() {
            setLayout(new BorderLayout(8, 8));

            JPanel summary = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 12));
            summary.setBackground(Color.WHITE);
            summary.add(summaryLabel("Balance", customer.getCurrentBalance(), true));
            summary.add(summaryLabel("Credit Limit", customer.getCreditLimit(), false));
            summary.add(recordPaymentBtn());
            add(summary, BorderLayout.NORTH);

            String[] cols = {"Date", "Type", "Amount", "Balance After", "Cashier", "Reference"};
            DefaultTableModel model = new DefaultTableModel(cols, 0) {
                public boolean isCellEditable(int r, int c) { return false; }
            };
            JTable table = new JTable(model);
            table.setRowHeight(36);
            table.setFont(new Font("Courier", Font.PLAIN, 13));
            JTableHeader th = table.getTableHeader();
            th.setFont(new Font("Courier", Font.BOLD, 13));
            th.setBackground(new Color(52, 58, 64));
            th.setForeground(Color.WHITE);
            DefaultTableCellRenderer center = new DefaultTableCellRenderer();
            center.setHorizontalAlignment(JLabel.CENTER);
            for (int i = 0; i < cols.length; i++) table.getColumnModel().getColumn(i).setCellRenderer(center);
            JScrollPane sp = new JScrollPane(table);
            sp.getViewport().setBackground(Color.WHITE);
            add(sp, BorderLayout.CENTER);

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MMM-yyyy HH:mm");
            List<org.PSmart.POS.Entities.CreditTransaction> txs =
                    DatabaseUtils.getInstance().getCreditRepository().findByCustomer(customer.getUid());
            for (org.PSmart.POS.Entities.CreditTransaction tx : txs) {
                String date = sdf.format(new java.util.Date(tx.getCreatedAt()));
                Color typeColor = new Color(40, 167, 69);
                model.addRow(new Object[]{
                        date,
                        tx.getType(),
                        "KES " + String.format("%.2f", tx.getAmount()),
                        "KES " + String.format("%.2f", tx.getBalanceAfter()),
                        tx.getCashier() != null ? tx.getCashier().getFullname() : "—",
                        tx.getNotes() != null ? tx.getNotes() : "—"
                });
                if (tx.getType().equals("PAYMENT")) typeColor = new Color(23, 162, 184);
            }
            table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
                public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean f, int r, int c) {
                    Component cc = super.getTableCellRendererComponent(t, v, sel, f, r, c);
                    if (!sel) cc.setForeground("PAYMENT".equals(v) ? new Color(23, 162, 184) : new Color(40, 167, 69));
                    return cc;
                }
            });
        }

        private JPanel summaryLabel(String title, Double value, boolean bold) {
            JPanel p = new JPanel(new BorderLayout(8, 0));
            p.setOpaque(false);
            JLabel t = new JLabel(title + ":");
            t.setFont(new Font("Courier", Font.PLAIN, 13));
            t.setForeground(new Color(100, 100, 100));
            String v = "KES 0.00";
            if (value != null) v = "KES " + String.format("%.2f", value);
            JLabel val = new JLabel(v);
            val.setFont(new Font("Courier", bold ? Font.BOLD : Font.PLAIN, bold ? 18 : 15));
            val.setForeground(bold ? new Color(220, 53, 69) : new Color(60, 60, 60));
            p.add(t, BorderLayout.NORTH);
            p.add(val, BorderLayout.CENTER);
            return p;
        }

        private JButton recordPaymentBtn() {
            JButton b = new JButton("Record Payment");
            b.setFont(new Font("Courier", Font.BOLD, 14));
            b.setBackground(new Color(0, 123, 255));
            b.setForeground(Color.WHITE);
            b.setFocusPainted(false);
            b.setBorderPainted(false);
            b.setPreferredSize(new Dimension(180, 42));
            b.addActionListener(e -> {
                new PaymentDialog(this, customer, () -> { afterSave.run(); dispose(); }).setVisible(true);
            });
            return b;
        }
    }

    /** Records a debt payment against a customer's balance. */
    private class PaymentDialog extends JDialog {
        private final Customer customer;
        private final Runnable afterSave;
        private JTextField amountField, referenceField;
        private JComboBox<String> methodCombo;

        PaymentDialog(java.awt.Window owner, Customer customer, Runnable afterSave) {
            super(owner, "Collect Payment - " + customer.getFullname(), Dialog.ModalityType.APPLICATION_MODAL);
            this.customer = customer;
            this.afterSave = afterSave;
            setSize(440, 360);
            setLocationRelativeTo(owner);
            buildUI();
        }

        private void buildUI() {
            setLayout(new BorderLayout(8, 8));

            double balance = customer.getCurrentBalance() != null ? customer.getCurrentBalance() : 0.0;
            JPanel form = new JPanel(new GridBagLayout());
            form.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));
            form.setBackground(Color.WHITE);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 4, 6, 4);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;

            amountField = addPaymentRow(form, gbc, 0, "Amount (KES):", String.format("%.2f", balance));
            methodCombo = new JComboBox<>(new String[]{"Cash", "M-Pesa", "Card", "Bank Transfer"});
            methodCombo.setFont(new Font("Courier", Font.PLAIN, 13));
            gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
            form.add(new JLabel("Payment Method:"), gbc);
            gbc.gridx = 1; gbc.weightx = 0.7;
            form.add(methodCombo, gbc);
            referenceField = addPaymentRow(form, gbc, 2, "Reference/Note:", "");
            add(form, BorderLayout.CENTER);

            JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
            btns.setBackground(Color.WHITE);
            JButton cancel = new JButton("Cancel");
            cancel.addActionListener(e -> dispose());
            btns.add(cancel);
            JButton save = new JButton("Record Payment");
            save.setBackground(new Color(40, 167, 69));
            save.setForeground(Color.WHITE);
            save.setFocusPainted(false);
            save.setBorderPainted(false);
            save.addActionListener(e -> onRecord());
            btns.add(save);
            add(btns, BorderLayout.SOUTH);
        }

        private JTextField addPaymentRow(JPanel p, GridBagConstraints gbc, int row, String label, String def) {
            JTextField tf = new JTextField(def);
            tf.setFont(new Font("Courier", Font.PLAIN, 13));
            gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
            p.add(new JLabel(label), gbc);
            gbc.gridx = 1; gbc.weightx = 0.7;
            p.add(tf, gbc);
            return tf;
        }

        private void onRecord() {
            double amount;
            try {
                amount = Double.parseDouble(amountField.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Enter a valid amount.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            double balance = customer.getCurrentBalance() != null ? customer.getCurrentBalance() : 0.0;
            if (amount <= 0) {
                JOptionPane.showMessageDialog(this, "Amount must be greater than zero.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (amount > balance + 0.001) {
                int opt = JOptionPane.showConfirmDialog(this,
                        "Amount exceeds the current balance (KES " + String.format("%.2f", balance)
                                + "). Continue?", "Overpayment", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (opt != JOptionPane.YES_OPTION) return;
            }

            User cashier = null;
            try {
                String hql = "FROM User u WHERE u.deleted=false";
                cashier = DatabaseUtils.getInstance().getEmf().createEntityManager()
                        .createQuery(hql, User.class).getResultList().stream().findFirst().orElse(null);
            } catch (Exception ignored) {}

            String note = methodCombo.getSelectedItem() + " payment" +
                    (referenceField.getText().trim().isEmpty() ? "" : " - " + referenceField.getText().trim());
            try {
                DatabaseUtils.getInstance().getCreditRepository().recordPayment(customer, amount, cashier, note);
                AutoClosingNotification.sendNotification("Payment recorded - KES " + String.format("%.2f", amount));
                dispose();
                if (afterSave != null) afterSave.run();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Could not record payment: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private class CustomerDialog extends JDialog {
        private Customer existing;
        private JTextField fullnameField;
        private JTextField phoneField;
        private JTextField emailField;
        private JTextField addressField;
        private JTextField creditLimitField;
        private JTextArea notesArea;
        private Runnable saveAction;
        private Runnable deleteAction;

        public CustomerDialog(Frame owner, Customer customer) {
            super(owner, customer == null ? "Add Customer" : "Edit Customer", true);
            this.existing = customer;
            buildUI();
            if (customer != null) populateFields();
        }

        private void buildUI() {
            setSize(480, 480);
            setLocationRelativeTo(getOwner());
            setLayout(new BorderLayout(8, 8));
            setResizable(false);

            JPanel form = new JPanel(new GridBagLayout());
            form.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));
            form.setBackground(Color.WHITE);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4, 4, 4, 4);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;

            fullnameField = addFormRow(form, gbc, 0, "Full Name *");
            phoneField = addFormRow(form, gbc, 1, "Phone");
            emailField = addFormRow(form, gbc, 2, "Email");
            addressField = addFormRow(form, gbc, 3, "Address");
            creditLimitField = addFormRow(form, gbc, 4, "Credit Limit");

            notesArea = new JTextArea(3, 20);
            notesArea.setFont(new Font("Courier", Font.PLAIN, 13));
            gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0.3;
            form.add(new JLabel("Notes:"), gbc);
            gbc.gridx = 1; gbc.weightx = 0.7;
            form.add(new JScrollPane(notesArea), gbc);

            add(form, BorderLayout.CENTER);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
            buttons.setBackground(Color.WHITE);

            if (existing != null) {
                JButton deleteBtn = new JButton("Delete");
                deleteBtn.setBackground(new Color(220, 53, 69));
                deleteBtn.setForeground(Color.WHITE);
                deleteBtn.addActionListener(e -> onDelete());
                buttons.add(deleteBtn);
            }

            JButton cancelBtn = new JButton("Cancel");
            cancelBtn.addActionListener(e -> dispose());
            buttons.add(cancelBtn);

            JButton saveBtn = new JButton(existing != null ? "Save" : "Create");
            saveBtn.setBackground(new Color(52, 58, 64));
            saveBtn.setForeground(Color.WHITE);
            saveBtn.addActionListener(e -> onSave());
            buttons.add(saveBtn);

            add(buttons, BorderLayout.SOUTH);
        }

        private JTextField addFormRow(JPanel p, GridBagConstraints gbc, int row, String label) {
            JTextField tf = new JTextField();
            tf.setFont(new Font("Courier", Font.PLAIN, 13));
            gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
            p.add(new JLabel(label + ":"), gbc);
            gbc.gridx = 1; gbc.weightx = 0.7;
            p.add(tf, gbc);
            return tf;
        }

        private void populateFields() {
            fullnameField.setText(existing.getFullname());
            phoneField.setText(existing.getPhone() != null ? existing.getPhone() : "");
            emailField.setText(existing.getEmail() != null ? existing.getEmail() : "");
            addressField.setText(existing.getAddress() != null ? existing.getAddress() : "");
            creditLimitField.setText(existing.getCreditLimit() != null ? existing.getCreditLimit().toString() : "");
            notesArea.setText(existing.getNotes() != null ? existing.getNotes() : "");
        }

        private void onSave() {
            String name = fullnameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Name is required.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Customer c = existing != null ? existing : Customer.builder().build();
            c.setFullname(name);
            c.setPhone(phoneField.getText().trim().isEmpty() ? null : phoneField.getText().trim());
            c.setEmail(emailField.getText().trim().isEmpty() ? null : emailField.getText().trim());
            c.setAddress(addressField.getText().trim().isEmpty() ? null : addressField.getText().trim());
            c.setCreditLimit(MyUtils.getDouble(creditLimitField.getText().trim()));
            c.setNotes(notesArea.getText().trim().isEmpty() ? null : notesArea.getText().trim());
            if (c.getCurrentBalance() == null) c.setCurrentBalance(0.0);

            customerRepository.save(c);
            AutoClosingNotification.sendNotification("Customer saved");
            dispose();
            if (saveAction != null) saveAction.run();
        }

        private void onDelete() {
            int confirm = JOptionPane.showConfirmDialog(this, "Delete this customer?", "Confirm",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                customerRepository.delete(existing);
                AutoClosingNotification.sendNotification("Customer deleted");
                dispose();
                if (deleteAction != null) deleteAction.run();
            }
        }

        public void setSaveAction(Runnable r) { this.saveAction = r; }
        public void setDeleteAction(Runnable r) { this.deleteAction = r; }
    }
}
