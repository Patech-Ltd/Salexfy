package org.PSmart.POS.ui.MainView.ui.admin;

import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.StoreConfig;
import org.PSmart.POS.Repositories.StoreConfigRepository;
import org.PSmart.POS.Utils.MyUtils;
import org.PSmart.POS.tests.AutoClosingNotification;

import javax.print.PrintService;
import javax.swing.*;
import java.awt.*;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AdminSettings extends JPanel {

    private static final Color DARK   = new Color(52, 58, 64);
    private static final Color IDLE   = new Color(25, 118, 211);
    private static final Color ACTIVE = new Color(0, 123, 255);
    private static final Color BG     = new Color(245, 245, 250);

    private final StoreConfigRepository repo =
            DatabaseUtils.getInstance().getStoreConfigRepository();
    private StoreConfig config;

    private JPanel cards;
    private final List<JButton> navButtons = new ArrayList<>();
    private JPanel navSidebar;

    // Store
    private JTextField storeNameField, storeAddressField, storePhoneField, storeEmailField, currencyField;
    // Tax
    private JCheckBox useTaxCheckbox, taxInclusiveCheckbox;
    private JTextField taxNameField, taxRateField;
    // Turnover tax / adjusting
    private JCheckBox useTurnoverTaxCheckbox;
    private JTextField turnoverTaxField, adjustingPctField;
    // Receipt & Printer
    private JTextArea receiptFooterArea;
    private JComboBox<String> printerCombo;
    // Payment methods
    private JPanel paymentListPanel;
    private JTextField newMethodField;
    private final List<PaymentMethodCard> paymentCards = new ArrayList<>();

    public AdminSettings() {
        loadConfig();
        draw();
        populate();
    }

    private void loadConfig() {
        config = repo.getStoreConfig();
        if (config == null) config = StoreConfig.defaultConfig();
        if (config.getPaymentMethods() == null) config.setPaymentMethods(new ArrayList<>());
    }

    private void draw() {
        setLayout(new BorderLayout());
        setBackground(BG);

        add(buildSidebar(), BorderLayout.WEST);

        cards = new JPanel(new CardLayout());
        cards.setBackground(Color.WHITE);
        cards.add(buildStoreCard(), "STORE");
        cards.add(buildTaxCard(), "TAX");
        cards.add(buildPrinterCard(), "PRINTER");
        cards.add(buildPaymentCard(), "PAYMENT");
        cards.add(buildBackupCard(), "BACKUP");
        add(cards, BorderLayout.CENTER);

        showCard("STORE");
    }

    // ------------------------------------------------------------------ sidebar

    private JPanel buildSidebar() {
        navSidebar = new JPanel();
        navSidebar.setLayout(new BoxLayout(navSidebar, BoxLayout.Y_AXIS));
        navSidebar.setBackground(DARK);
        navSidebar.setPreferredSize(new Dimension(220, 0));
        navSidebar.setBorder(BorderFactory.createEmptyBorder(20, 12, 20, 12));

        JLabel head = new JLabel("Settings");
        head.setFont(MyUtils.courierFont(20));
        head.setForeground(Color.WHITE);
        head.setAlignmentX(Component.LEFT_ALIGNMENT);
        navSidebar.add(head);
        navSidebar.add(Box.createVerticalStrut(24));

        addNav("  Store", "STORE");
        addNav("  Tax & Pricing", "TAX");
        addNav("  Receipt & Printer", "PRINTER");
        addNav("  Payment Methods", "PAYMENT");
        addNav("  Backup & Sync", "BACKUP");

        navSidebar.add(Box.createVerticalGlue());
        return navSidebar;
    }

    private void addNav(String label, String card) {
        JButton b = new JButton(label);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        b.setPreferredSize(new Dimension(Integer.MAX_VALUE, 44));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setFont(MyUtils.courierFont(15));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBackground(IDLE);
        b.addActionListener(e -> showCard(card));
        navButtons.add(b);
        navSidebar.add(b);
        navSidebar.add(Box.createVerticalStrut(6));
    }

    private void showCard(String card) {
        ((CardLayout) cards.getLayout()).show(cards, card);
        int i = switch (card) {
            case "TAX" -> 1;
            case "PRINTER" -> 2;
            case "PAYMENT" -> 3;
            case "BACKUP" -> 4;
            default -> 0;
        };
        for (int k = 0; k < navButtons.size(); k++)
            navButtons.get(k).setBackground(k == i ? ACTIVE : IDLE);
    }

    private JPanel sectionWrap(JLabel title, Component form, Runnable save) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(Color.WHITE);
        wrap.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        title.setFont(MyUtils.courierFont(22));
        title.setForeground(DARK);
        top.add(title, BorderLayout.WEST);

        JButton saveBtn = new JButton("Save");
        saveBtn.setFont(MyUtils.courierFont(14));
        saveBtn.setBackground(new Color(40, 167, 69));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFocusPainted(false);
        saveBtn.setBorderPainted(false);
        saveBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        saveBtn.setPreferredSize(new Dimension(120, 38));
        saveBtn.addActionListener(e -> save.run());
        top.add(saveBtn, BorderLayout.EAST);

        wrap.add(top, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        wrap.add(scroll, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel formPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        return p;
    }

    private JPanel fieldRow(String label, JComponent input) {
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        row.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));

        JLabel l = new JLabel(label);
        l.setFont(MyUtils.courierFont(14));
        l.setForeground(new Color(90, 90, 90));
        l.setPreferredSize(new Dimension(220, 36));
        row.add(l, BorderLayout.WEST);

        input.setFont(new Font("Courier", Font.PLAIN, 14));
        input.setPreferredSize(new Dimension(0, 36));
        row.add(input, BorderLayout.CENTER);
        return row;
    }

    // ------------------------------------------------------------------ STORE

    private JPanel buildStoreCard() {
        JPanel form = formPanel();
        storeNameField    = new JTextField();
        storeAddressField = new JTextField();
        storePhoneField   = new JTextField();
        storeEmailField   = new JTextField();
        currencyField     = new JTextField();
        form.add(fieldRow("Store Name", storeNameField));
        form.add(fieldRow("Address", storeAddressField));
        form.add(fieldRow("Phone", storePhoneField));
        form.add(fieldRow("Email", storeEmailField));
        form.add(fieldRow("Currency", currencyField));

        return sectionWrap(new JLabel("Store"), form, this::saveStore);
    }

    private void saveStore() {
        config.setStoreName(storeNameField.getText().trim());
        config.setStoreAddress(storeAddressField.getText().trim());
        config.setStorePhone(storePhoneField.getText().trim());
        config.setStoreEmail(storeEmailField.getText().trim());
        config.setCurrency(currencyField.getText().trim());
        repo.saveOrUpdate(config);
        AutoClosingNotification.sendNotification("Store settings saved");
    }

    // ------------------------------------------------------------------ TAX

    private JPanel buildTaxCard() {
        JPanel form = formPanel();
        useTaxCheckbox = new JCheckBox("Enable tax on sales");
        taxInclusiveCheckbox = new JCheckBox("Tax included in selling price");
        useTaxCheckbox.setOpaque(false);
        taxInclusiveCheckbox.setOpaque(false);
        useTaxCheckbox.setFont(MyUtils.courierFont(14));
        taxInclusiveCheckbox.setFont(MyUtils.courierFont(14));
        taxInclusiveCheckbox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        useTaxCheckbox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        taxNameField = new JTextField();
        taxRateField = new JTextField();
        useTurnoverTaxCheckbox = new JCheckBox("Enable turnover tax");
        useTurnoverTaxCheckbox.setOpaque(false);
        useTurnoverTaxCheckbox.setFont(MyUtils.courierFont(14));
        useTurnoverTaxCheckbox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        turnoverTaxField = new JTextField();
        adjustingPctField = new JTextField();

        form.add(useTaxCheckbox);
        form.add(Box.createVerticalStrut(10));
        form.add(taxInclusiveCheckbox);
        form.add(Box.createVerticalStrut(12));
        form.add(useTurnoverTaxCheckbox);
        form.add(Box.createVerticalStrut(12));
        form.add(fieldRow("Tax Name (e.g. VAT)", taxNameField));
        form.add(fieldRow("Tax Rate (%)", taxRateField));
        form.add(fieldRow("Turnover Tax Rate (%)", turnoverTaxField));
        form.add(fieldRow("Adjusting %", adjustingPctField));

        return sectionWrap(new JLabel("Tax & Pricing"), form, this::saveTax);
    }

    private void saveTax() {
        config.setUseTax(useTaxCheckbox.isSelected());
        config.setTaxInclusiveInPrice(taxInclusiveCheckbox.isSelected());
        config.setTaxName(taxNameField.getText().trim());
        config.setTaxRate(MyUtils.getDouble(taxRateField.getText().trim()));
        config.setUseTurnoverTax(useTurnoverTaxCheckbox.isSelected());
        config.setTurnoverTaxRate(MyUtils.getDouble(turnoverTaxField.getText().trim()));
        config.setAdjustingPct(MyUtils.getDouble(adjustingPctField.getText().trim()));
        repo.saveOrUpdate(config);
        AutoClosingNotification.sendNotification("Tax settings saved");
    }

    // ------------------------------------------------------------------ PRINTER

    private JPanel buildPrinterCard() {
        JPanel form = formPanel();

        printerCombo = new JComboBox<>(listPrinters());
        printerCombo.setFont(new Font("Courier", Font.PLAIN, 14));
        printerCombo.setPreferredSize(new Dimension(0, 36));
        printerCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        form.add(fieldRow("Default Printer", printerCombo));
        form.add(Box.createVerticalStrut(10));

        receiptFooterArea = new JTextArea(4, 40);
        receiptFooterArea.setFont(new Font("Courier", Font.PLAIN, 14));
        receiptFooterArea.setLineWrap(true);
        JPanel foot = fieldRow("Receipt Footer", new JScrollPane(receiptFooterArea));
        foot.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        form.add(foot);

        return sectionWrap(new JLabel("Receipt & Printer"), form, this::savePrinter);
    }

    private String[] listPrinters() {
        List<String> names = new ArrayList<>();
        names.add("(System Default)");
        try {
            PrintService[] services = PrinterJob.lookupPrintServices();
            for (PrintService s : services) names.add(s.getName());
        } catch (Exception ignored) {}
        return names.toArray(new String[0]);
    }

    private void savePrinter() {
        config.setDefaultPrinter(printerCombo.getSelectedItem() == null
                ? "" : printerCombo.getSelectedItem().toString());
        config.setReceiptFooter(receiptFooterArea.getText());
        repo.saveOrUpdate(config);
        AutoClosingNotification.sendNotification("Receipt & printer settings saved");
    }

    // ------------------------------------------------------------------ PAYMENT

    private JPanel buildPaymentCard() {
        JPanel form = formPanel();

        JLabel hint = new JLabel("Payment methods are saved to your receipt. Tap a method to expand details.");
        hint.setFont(MyUtils.courierFont(12));
        hint.setForeground(new Color(120, 120, 120));
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        hint.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        form.add(hint);

        paymentListPanel = new JPanel();
        paymentListPanel.setLayout(new BoxLayout(paymentListPanel, BoxLayout.Y_AXIS));
        paymentListPanel.setOpaque(false);

        JScrollPane listScroll = new JScrollPane(paymentListPanel);
        listScroll.setBorder(BorderFactory.createEmptyBorder());
        listScroll.getViewport().setOpaque(false);
        listScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        listScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        listScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));
        listScroll.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel listWrapper = new JPanel(new BorderLayout());
        listWrapper.setOpaque(false);
        listWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));
        listWrapper.add(listScroll, BorderLayout.CENTER);
        form.add(listWrapper);
        form.add(Box.createVerticalStrut(12));

        JPanel addRow = new JPanel(new BorderLayout(10, 0));
        addRow.setOpaque(false);
        addRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        newMethodField = new JTextField();
        newMethodField.setPreferredSize(new Dimension(0, 36));
        newMethodField.setFont(new Font("Courier", Font.PLAIN, 14));
        addRow.add(newMethodField, BorderLayout.CENTER);
        JButton addBtn = actionBtn("Add Method", new Color(40, 167, 69), this::addPayment);
        addBtn.setPreferredSize(new Dimension(140, 36));
        addRow.add(addBtn, BorderLayout.EAST);
        form.add(addRow);

        refreshPaymentCards();

        return sectionWrap(new JLabel("Payment Methods"), form, this::savePayment);
    }

    private void refreshPaymentCards() {
        paymentListPanel.removeAll();
        paymentCards.clear();
        for (String method : config.getPaymentMethods()) {
            PaymentMethodCard c = new PaymentMethodCard(method);
            paymentCards.add(c);
            paymentListPanel.add(c);
            paymentListPanel.add(Box.createVerticalStrut(8));
        }
        paymentListPanel.revalidate();
        paymentListPanel.repaint();
    }

    private void addPayment() {
        String m = newMethodField.getText().trim();
        if (m.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter a payment method name", "Payment Method",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (config.getPaymentMethods().contains(m)) {
            JOptionPane.showMessageDialog(this, "That payment method already exists.", "Payment Method",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (config.getPaymentMethods() == null) config.setPaymentMethods(new ArrayList<>());
        config.getPaymentMethods().add(m);
        if (config.getPaymentMethodDetails() == null) config.setPaymentMethodDetails(new HashMap<>());
        config.getPaymentMethodDetails().put(m, "FIXED|0.0|true");
        newMethodField.setText("");
        refreshPaymentCards();
    }

    private void removePayment(String method) {
        config.getPaymentMethods().remove(method);
        if (config.getPaymentMethodDetails() != null) config.getPaymentMethodDetails().remove(method);
        refreshPaymentCards();
    }

    private void savePayment() {
        for (PaymentMethodCard c : paymentCards) c.apply();
        repo.saveOrUpdate(config);
        refreshPaymentCards();
        AutoClosingNotification.sendNotification("Payment methods saved");
    }

    /** Detail encoding: "FEE_TYPE|FEE_VALUE|ENABLED" */
    private String[] parseDetail(String method) {
        String d = (config.getPaymentMethodDetails() == null)
                ? null : config.getPaymentMethodDetails().get(method);
        if (d == null || d.isEmpty()) return new String[]{"FIXED", "0.0", "true"};
        String[] parts = d.split("\\|");
        return new String[]{parts[0], parts.length > 1 ? parts[1] : "0.0", parts.length > 2 ? parts[2] : "true"};
    }

    // ------------------------------------------------------------------

    private class PaymentMethodCard extends JPanel {
        private final String method;
        private final JPanel header;
        private final JPanel body;
        private JCheckBox enabledBox;
        private JComboBox<String> feeTypeCombo;
        private JTextField feeValueField;
        private JTextField providerField;
        private boolean expanded;

        PaymentMethodCard(String method) {
            this.method = method;
            setLayout(new BorderLayout());
            setOpaque(false);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
            setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

            header = new JPanel(new BorderLayout(10, 0));
            header.setBackground(Color.WHITE);
            header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            header.setPreferredSize(new Dimension(Integer.MAX_VALUE, 44));
            header.setBorder(BorderFactory.createLineBorder(new Color(225, 225, 230), 1));

            JPanel nameArea = new JPanel(new BorderLayout());
            nameArea.setOpaque(false);
            JLabel name = new JLabel(method);
            name.setFont(MyUtils.courierFont(15).deriveFont(Font.BOLD));
            name.setForeground(DARK);
            nameArea.add(name, BorderLayout.CENTER);

            String[] parts = parseDetail(method);
            JLabel sub = new JLabel(buildSubText(parts));
            sub.setFont(MyUtils.courierFont(11));
            sub.setForeground(new Color(130, 130, 130));
            nameArea.add(sub, BorderLayout.SOUTH);
            header.add(nameArea, BorderLayout.CENTER);

            JLabel caret = new JLabel(" \u25BC ");
            caret.setFont(MyUtils.courierFont(14));
            caret.setForeground(ACTIVE);
            caret.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            header.add(caret, BorderLayout.EAST);
            header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            header.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    toggle();
                }
            });

            add(header, BorderLayout.NORTH);

            body = new JPanel();
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.setBackground(new Color(250, 250, 252));
            body.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(225, 225, 230), 1),
                    BorderFactory.createEmptyBorder(10, 14, 14, 14)));

            enabledBox = new JCheckBox("Enable this payment method");
            enabledBox.setOpaque(false);
            enabledBox.setFont(MyUtils.courierFont(13));
            enabledBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

            feeValueField = new JTextField();
            feeTypeCombo = new JComboBox<>(new String[]{"FIXED", "PERCENTAGE"});
            feeTypeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            providerField = new JTextField();

            body.add(enabledBox);
            body.add(Box.createVerticalStrut(8));
            body.add(detailRow("Fee Type", feeTypeCombo));
            body.add(detailRow("Fee Value (" + currency() + " or %)", feeValueField));
            body.add(detailRow("Provider / Notes", providerField));
            body.add(Box.createVerticalStrut(6));

            JPanel cardActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            cardActions.setOpaque(false);
            cardActions.add(actionBtn("Delete", new Color(220, 53, 69),
                    () -> removePayment(method)));
            body.add(cardActions);

            loadDetails(parts);
            body.setVisible(false);
            add(body, BorderLayout.CENTER);
        }

        private String buildSubText(String[] parts) {
            boolean enabled = Boolean.parseBoolean(parts[2]);
            String fee = ("PERCENTAGE".equals(parts[0]) ? parts[1] + "%" : currency() + " " + parts[1]);
            return (enabled ? "Active" : "Inactive") + "  \u00b7  fee " + fee;
        }

        private String currency() {
            return config.getCurrency() == null || config.getCurrency().isEmpty()
                    ? "KES" : config.getCurrency();
        }

        private JPanel detailRow(String label, JComponent input) {
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            row.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
            JLabel l = new JLabel(label);
            l.setFont(MyUtils.courierFont(12));
            l.setForeground(new Color(90, 90, 90));
            l.setPreferredSize(new Dimension(200, 34));
            row.add(l, BorderLayout.WEST);
            input.setFont(new Font("Courier", Font.PLAIN, 13));
            input.setPreferredSize(new Dimension(0, 34));
            row.add(input, BorderLayout.CENTER);
            return row;
        }

        private void loadDetails(String[] parts) {
            enabledBox.setSelected(Boolean.parseBoolean(parts[2]));
            feeTypeCombo.setSelectedItem(parts[0].equals("PERCENTAGE") ? "PERCENTAGE" : "FIXED");
            feeValueField.setText(parts[1]);
        }

        private void toggle() {
            expanded = !expanded;
            body.setVisible(expanded);
            revalidate();
            repaint();
        }

        public void apply() {
            if (config.getPaymentMethodDetails() == null) config.setPaymentMethodDetails(new HashMap<>());
            String feeType = feeTypeCombo.getSelectedItem() == null ? "FIXED" : feeTypeCombo.getSelectedItem().toString();
            String fee = feeValueField.getText().trim();
            if (fee.isEmpty()) fee = "0.0";
            config.getPaymentMethodDetails().put(method,
                    feeType + "|" + fee + "|" + enabledBox.isSelected());
        }
    }

    // ------------------------------------------------------------------ BACKUP & SYNC

    private JCheckBox autoBackupCheckbox, syncEnabledCheckbox;
    private JTextField autoBackupMinutesField, backupKeepField;
    private JTextField syncUrlField, syncUserField, syncPassField;
    private JLabel syncStatusField, lastSyncField, lastBackupField;
    private JList<String> backupList;
    private DefaultListModel<String> backupListModel;

    private JPanel buildBackupCard() {
        JPanel form = formPanel();

        JLabel hint = new JLabel("Schedule local JSON backups and push them to your server. Fields are validated before saving.");
        hint.setFont(MyUtils.courierFont(12));
        hint.setForeground(new Color(120, 120, 120));
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        hint.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        form.add(hint);

        // --- Automatic backup ---
        JLabel backupTitle = new JLabel("Automatic Local Backup");
        backupTitle.setFont(MyUtils.courierFont(16));
        backupTitle.setForeground(DARK);
        backupTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(backupTitle);
        form.add(Box.createVerticalStrut(6));

        autoBackupCheckbox = new JCheckBox("Enable automatic backups");
        autoBackupCheckbox.setOpaque(false);
        autoBackupCheckbox.setFont(MyUtils.courierFont(14));
        autoBackupCheckbox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        form.add(autoBackupCheckbox);
        form.add(Box.createVerticalStrut(8));

        autoBackupMinutesField = new JTextField();
        form.add(fieldRow("Interval (minutes)", autoBackupMinutesField));

        backupKeepField = new JTextField();
        form.add(fieldRow("Keep latest (count)", backupKeepField));

        JPanel backupRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        backupRow.setOpaque(false);
        backupRow.add(actionBtn("Backup Now", new Color(40, 167, 69), this::backupNow));
        backupRow.add(actionBtn("Open Backups Folder", new Color(23, 162, 184), this::openBackupsFolder));
        form.add(backupRow);
        form.add(Box.createVerticalStrut(6));

        lastBackupField = new JLabel("Last backup: never");
        lastBackupField.setFont(MyUtils.courierFont(13));
        lastBackupField.setForeground(new Color(100, 100, 100));
        lastBackupField.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lastBackupField);
        form.add(Box.createVerticalStrut(12));

        // --- Sync ---
        JLabel syncTitle = new JLabel("Cloud / Server Sync");
        syncTitle.setFont(MyUtils.courierFont(16));
        syncTitle.setForeground(DARK);
        syncTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(syncTitle);
        form.add(Box.createVerticalStrut(6));

        syncEnabledCheckbox = new JCheckBox("Enable server sync");
        syncEnabledCheckbox.setOpaque(false);
        syncEnabledCheckbox.setFont(MyUtils.courierFont(14));
        syncEnabledCheckbox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        form.add(syncEnabledCheckbox);
        form.add(Box.createVerticalStrut(8));

        syncUrlField = new JTextField();
        form.add(fieldRow("Server URL (https://...)", syncUrlField));
        syncUserField = new JTextField();
        form.add(fieldRow("Username", syncUserField));
        syncPassField = new JTextField();
        form.add(fieldRow("Password", syncPassField));

        JPanel syncRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        syncRow.setOpaque(false);
        syncRow.add(actionBtn("Sync Now", new Color(0, 123, 255), this::syncNow));
        syncRow.add(actionBtn("Test Connection", new Color(108, 117, 125), this::testConnection));
        form.add(syncRow);
        form.add(Box.createVerticalStrut(8));

        syncStatusField = new JLabel("Status: Not configured");
        syncStatusField.setFont(MyUtils.courierFont(13));
        syncStatusField.setForeground(new Color(100, 100, 100));
        syncStatusField.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(syncStatusField);

        lastSyncField = new JLabel("Last sync: never");
        lastSyncField.setFont(MyUtils.courierFont(13));
        lastSyncField.setForeground(new Color(100, 100, 100));
        lastSyncField.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lastSyncField);
        form.add(Box.createVerticalStrut(14));

        // --- backup file list ---
        JLabel filesTitle = new JLabel("Backup Files");
        filesTitle.setFont(MyUtils.courierFont(16));
        filesTitle.setForeground(DARK);
        filesTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(filesTitle);
        form.add(Box.createVerticalStrut(6));

        backupListModel = new DefaultListModel<>();
        backupList = new JList<>(backupListModel);
        backupList.setFont(new Font("Courier", Font.PLAIN, 12));
        JScrollPane listScroll = new JScrollPane(backupList);
        listScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        listScroll.setPreferredSize(new Dimension(0, 120));
        form.add(listScroll);

        JPanel fileRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        fileRow.setOpaque(false);
        fileRow.add(actionBtn("Upload Selected", new Color(40, 167, 69), this::uploadSelected));
        form.add(fileRow);

        return sectionWrap(new JLabel("Backup & Sync"), form, this::saveBackup);
    }

    private void refreshBackupList() {
        backupListModel.clear();
        for (java.io.File f : org.PSmart.POS.Utils.BackupService.listBackups()) {
            backupListModel.addElement(f.getName() + "  (" + (f.length() / 1024) + " KB)");
        }
    }

    private void addBackupHint(String msg, Color color) { }

    private void backupNow() {
        try {
            int keep = MyUtils.getInt(backupKeepField.getText().trim());
            org.PSmart.POS.Utils.BackupService.createBackupAndRotate(keep > 0 ? keep : 10);
            config.setLastBackupAt(System.currentTimeMillis());
            repo.saveOrUpdate(config);
            refreshBackupList();
            lastBackupField.setText("Last backup: " + new java.text.SimpleDateFormat("dd-MMM-yyyy HH:mm").format(new java.util.Date()));
            org.PSmart.POS.tests.AutoClosingNotification.sendNotification("Backup created successfully");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Backup failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openBackupsFolder() {
        try {
            java.io.File dir = new java.io.File(org.PSmart.POS.Utils.BackupService.BACKUP_DIR);
            dir.mkdirs();
            Desktop.getDesktop().open(dir);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void syncNow() {
        if (!saveValidation()) return;
        saveFieldsToConfig();
        repo.saveOrUpdate(config);
        syncStatusField.setText("Status: Syncing...");
        new Thread(() -> {
            try {
                java.io.File latest = org.PSmart.POS.Utils.BackupService.createBackup();
                String result = org.PSmart.POS.Utils.SyncService.syncNow(config, latest);
                config.setLastSyncStatus(org.PSmart.POS.Utils.SyncService.STATUS_OK);
                config.setLastSyncAt(System.currentTimeMillis());
                repo.saveOrUpdate(config);
                SwingUtilities.invokeLater(() -> {
                    syncStatusField.setText("Status: " + result);
                    lastSyncField.setText("Last sync: " + nowStr());
                });
            } catch (Exception ex) {
                config.setLastSyncStatus(org.PSmart.POS.Utils.SyncService.STATUS_FAIL + ": " + ex.getMessage());
                repo.saveOrUpdate(config);
                SwingUtilities.invokeLater(() -> {
                    syncStatusField.setText("Status: FAILED — " + ex.getMessage());
                    JOptionPane.showMessageDialog(this, "Sync failed: " + ex.getMessage(), "Sync Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void testConnection() {
        if (!saveValidation()) return;
        saveFieldsToConfig();
        repo.saveOrUpdate(config);
        if (!Boolean.TRUE.equals(config.getSyncEnabled())) {
            JOptionPane.showMessageDialog(this, "Enable server sync to test the connection.", "Test Connection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(config.getSyncServerUrl());
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("HEAD");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                String user = config.getSyncUsername();
                if (user != null && !user.isEmpty()) {
                    String token = java.util.Base64.getEncoder().encodeToString(
                            (user + ":" + (config.getSyncPassword() == null ? "" : config.getSyncPassword())).getBytes());
                    conn.setRequestProperty("Authorization", "Basic " + token);
                }
                int code = conn.getResponseCode();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Connection OK (HTTP " + code + ")", "Test Connection", JOptionPane.INFORMATION_MESSAGE));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Connection failed: " + ex.getMessage(), "Test Connection", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    private void uploadSelected() {
        int idx = backupList.getSelectedIndex();
        if (idx < 0) {
            JOptionPane.showMessageDialog(this, "Select a backup file to upload first.", "Upload", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!saveValidation()) return;
        saveFieldsToConfig();
        repo.saveOrUpdate(config);
        java.io.File chosen = org.PSmart.POS.Utils.BackupService.listBackups().get(idx);
        new Thread(() -> {
            try {
                String result = org.PSmart.POS.Utils.SyncService.syncNow(config, chosen);
                config.setLastSyncStatus(org.PSmart.POS.Utils.SyncService.STATUS_OK);
                config.setLastSyncAt(System.currentTimeMillis());
                repo.saveOrUpdate(config);
                SwingUtilities.invokeLater(() -> {
                    syncStatusField.setText("Status: " + result);
                    lastSyncField.setText("Last sync: " + nowStr());
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Upload failed: " + ex.getMessage(), "Upload", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    private void saveFieldsToConfig() {
        config.setAutoBackupEnabled(autoBackupCheckbox.isSelected());
        config.setAutoBackupMinutes(MyUtils.getInt(autoBackupMinutesField.getText().trim()));
        config.setBackupKeepCount(Math.max(1, MyUtils.getInt(backupKeepField.getText().trim())));
        config.setSyncEnabled(syncEnabledCheckbox.isSelected());
        config.setSyncServerUrl(syncUrlField.getText().trim());
        config.setSyncUsername(syncUserField.getText().trim());
        config.setSyncPassword(syncPassField.getText().trim());
    }

    private void saveBackup() {
        if (!saveValidation()) return;
        saveFieldsToConfig();
        config.setAutoBackupMinutes(Math.max(1, MyUtils.getInt(autoBackupMinutesField.getText().trim())));
        config.setBackupKeepCount(Math.max(1, Math.min(100, MyUtils.getInt(backupKeepField.getText().trim()))));
        repo.saveOrUpdate(config);
        AutoClosingNotification.sendNotification("Backup & sync settings saved");
    }

    private boolean saveValidation() {
        boolean ok = true;
        if (autoBackupCheckbox.isSelected() && MyUtils.getInt(autoBackupMinutesField.getText().trim()) < 1) {
            JOptionPane.showMessageDialog(this, "Auto-backup interval must be at least 1 minute.", "Validation", JOptionPane.WARNING_MESSAGE);
            ok = false;
        }
        int keep = MyUtils.getInt(backupKeepField.getText().trim());
        if (keep < 1 || keep > 100) {
            JOptionPane.showMessageDialog(this, "Keep count must be between 1 and 100.", "Validation", JOptionPane.WARNING_MESSAGE);
            ok = false;
        }
        if (syncEnabledCheckbox.isSelected()) {
            String url = syncUrlField.getText().trim();
            if (url.isEmpty() || !(url.startsWith("http://") || url.startsWith("https://"))) {
                JOptionPane.showMessageDialog(this, "Server URL must start with http:// or https://", "Validation", JOptionPane.WARNING_MESSAGE);
                ok = false;
            }
        }
        return ok;
    }

    private String nowStr() {
        return new java.text.SimpleDateFormat("dd-MMM-yyyy HH:mm").format(new java.util.Date());
    }

    private JButton actionBtn(String text, Color bg, Runnable action) {
        JButton b = new JButton(text);
        b.setFont(MyUtils.courierFont(13));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> action.run());
        return b;
    }

    // ------------------------------------------------------------------ populate

    private void populate() {
        storeNameField.setText(config.getStoreName() != null ? config.getStoreName() : "");
        storeAddressField.setText(config.getStoreAddress() != null ? config.getStoreAddress() : "");
        storePhoneField.setText(config.getStorePhone() != null ? config.getStorePhone() : "");
        storeEmailField.setText(config.getStoreEmail() != null ? config.getStoreEmail() : "");
        currencyField.setText(config.getCurrency() != null ? config.getCurrency() : "");
        useTaxCheckbox.setSelected(Boolean.TRUE.equals(config.getUseTax()));
        taxInclusiveCheckbox.setSelected(Boolean.TRUE.equals(config.getTaxInclusiveInPrice()));
        taxNameField.setText(config.getTaxName() != null ? config.getTaxName() : "VAT");
        taxRateField.setText(config.getTaxRate() != null ? config.getTaxRate().toString() : "16");
        useTurnoverTaxCheckbox.setSelected(Boolean.TRUE.equals(config.getUseTurnoverTax()));
        turnoverTaxField.setText(config.getTurnoverTaxRate() != null ? config.getTurnoverTaxRate().toString() : "0");
        adjustingPctField.setText(config.getAdjustingPct() != null ? config.getAdjustingPct().toString() : "0");
        receiptFooterArea.setText(config.getReceiptFooter() != null ? config.getReceiptFooter() : "");
        if (config.getDefaultPrinter() != null && !config.getDefaultPrinter().isEmpty()) {
            for (int i = 0; i < printerCombo.getItemCount(); i++) {
                if (printerCombo.getItemAt(i).equals(config.getDefaultPrinter())) {
                    printerCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
    }
}