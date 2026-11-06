package org.PSmart.POS.ui.MainView.ui.settings;

import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.StoreConfig;
import org.PSmart.POS.Utils.MyUtils;
import org.PSmart.POS.tests.AutoClosingNotification;

import javax.swing.*;
import java.awt.*;

public class SettingsPanel extends JPanel {

    private StoreConfig config;

    private JTextField storeNameField;
    private JTextField storeAddressField;
    private JTextField storePhoneField;
    private JTextField storeEmailField;
    private JCheckBox useTaxCheckbox;
    private JCheckBox taxInclusiveCheckbox;
    private JTextField taxNameField;
    private JTextField taxRateField;
    private JTextField currencyField;
    private JTextField receiptFooterField;
    private JCheckBox useBarcodeCheckbox;
    private JTextField lowStockField;

    public SettingsPanel() {
        super();
        loadConfig();
        draw();
        populateFields();
    }

    private void loadConfig() {
        config = DatabaseUtils.getInstance().getStoreConfigRepository().getStoreConfig();
        if (config == null) config = StoreConfig.defaultConfig();
    }

    private void draw() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 250));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildForm(), BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(52, 58, 64));
        header.setPreferredSize(new Dimension(0, 60));
        header.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        JLabel title = new JLabel("Settings");
        title.setFont(new Font("Courier", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);
        return header;
    }

    private JPanel buildForm() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(new Color(245, 245, 250));
        main.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JPanel cardsPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        cardsPanel.setOpaque(false);

        cardsPanel.add(buildStoreDetailsCard());
        cardsPanel.add(buildTaxCard());

        main.add(cardsPanel, BorderLayout.NORTH);

        JPanel bottomCard = buildMiscCard();
        main.add(bottomCard, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.setOpaque(false);
        JButton saveBtn = new JButton("Save Settings");
        saveBtn.setFont(new Font("Courier", Font.BOLD, 16));
        saveBtn.setBackground(new Color(40, 167, 69));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFocusPainted(false);
        saveBtn.setBorderPainted(false);
        saveBtn.setPreferredSize(new Dimension(200, 45));
        saveBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        saveBtn.addActionListener(e -> saveConfig());
        btnPanel.add(saveBtn);
        main.add(btnPanel, BorderLayout.SOUTH);

        return main;
    }

    private JPanel buildStoreDetailsCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)));

        JLabel cardTitle = new JLabel("Store Details");
        cardTitle.setFont(new Font("Courier", Font.BOLD, 16));
        cardTitle.setForeground(new Color(52, 58, 64));
        card.add(cardTitle);
        card.add(Box.createVerticalStrut(15));

        storeNameField = addFieldRow(card, "Store Name:");
        storeAddressField = addFieldRow(card, "Address:");
        storePhoneField = addFieldRow(card, "Phone:");
        storeEmailField = addFieldRow(card, "Email:");

        return card;
    }

    private JPanel buildTaxCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)));

        JLabel cardTitle = new JLabel("Tax & Pricing");
        cardTitle.setFont(new Font("Courier", Font.BOLD, 16));
        cardTitle.setForeground(new Color(52, 58, 64));
        card.add(cardTitle);
        card.add(Box.createVerticalStrut(15));

        useTaxCheckbox = new JCheckBox("Enable Tax (VAT)");
        useTaxCheckbox.setFont(new Font("Courier", Font.PLAIN, 13));
        useTaxCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(useTaxCheckbox);
        card.add(Box.createVerticalStrut(8));

        taxInclusiveCheckbox = new JCheckBox("Tax included in selling price");
        taxInclusiveCheckbox.setFont(new Font("Courier", Font.PLAIN, 13));
        taxInclusiveCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(taxInclusiveCheckbox);
        card.add(Box.createVerticalStrut(12));

        taxNameField = addFieldRow(card, "Tax Name (e.g. VAT):");
        taxRateField = addFieldRow(card, "Tax Rate (%):");

        return card;
    }

    private JPanel buildMiscCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)));

        JLabel cardTitle = new JLabel("Other Settings");
        cardTitle.setFont(new Font("Courier", Font.BOLD, 16));
        cardTitle.setForeground(new Color(52, 58, 64));
        card.add(cardTitle);
        card.add(Box.createVerticalStrut(15));

        JPanel row = new JPanel(new GridLayout(1, 4, 15, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

        JPanel currencyPanel = createMiniField("Currency (e.g. KES):");
        currencyField = getField(currencyPanel);
        row.add(currencyPanel);

        JPanel lowStockPanel = createMiniField("Low Stock Alert Level:");
        lowStockField = getField(lowStockPanel);
        row.add(lowStockPanel);

        JPanel footerPanel = createMiniField("Receipt Footer Message:");
        receiptFooterField = getField(footerPanel);
        row.add(footerPanel);

        useBarcodeCheckbox = new JCheckBox("Use Barcode Scanner");
        useBarcodeCheckbox.setFont(new Font("Courier", Font.PLAIN, 13));
        JPanel checkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        checkPanel.setOpaque(false);
        checkPanel.add(useBarcodeCheckbox);
        row.add(checkPanel);

        card.add(row);

        return card;
    }

    private JPanel createMiniField(String label) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(new Font("Courier", Font.BOLD, 11));
        JTextField tf = new JTextField();
        tf.setFont(new Font("Courier", Font.PLAIN, 13));
        tf.setPreferredSize(new Dimension(0, 30));
        p.add(l, BorderLayout.NORTH);
        p.add(tf, BorderLayout.CENTER);
        return p;
    }

    private JTextField getField(JPanel p) {
        for (Component c : p.getComponents())
            if (c instanceof JTextField) return (JTextField) c;
        return null;
    }

    private JTextField addFieldRow(JPanel parent, String labelText) {
        JPanel row = new JPanel(new BorderLayout(0, 3));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Courier", Font.BOLD, 12));
        label.setForeground(new Color(80, 80, 80));

        JTextField field = new JTextField();
        field.setFont(new Font("Courier", Font.PLAIN, 13));
        field.setPreferredSize(new Dimension(0, 30));

        row.add(label, BorderLayout.NORTH);
        row.add(field, BorderLayout.CENTER);
        parent.add(row);
        parent.add(Box.createVerticalStrut(10));

        return field;
    }

    private void populateFields() {
        storeNameField.setText(config.getStoreName() != null ? config.getStoreName() : "");
        storeAddressField.setText(config.getStoreAddress() != null ? config.getStoreAddress() : "");
        storePhoneField.setText(config.getStorePhone() != null ? config.getStorePhone() : "");
        storeEmailField.setText(config.getStoreEmail() != null ? config.getStoreEmail() : "");
        useTaxCheckbox.setSelected(config.getUseTax() != null && config.getUseTax());
        taxInclusiveCheckbox.setSelected(config.getTaxInclusiveInPrice() != null && config.getTaxInclusiveInPrice());
        taxNameField.setText(config.getTaxName() != null ? config.getTaxName() : "VAT");
        taxRateField.setText(config.getTaxRate() != null ? config.getTaxRate().toString() : "16");
        currencyField.setText(config.getCurrency() != null ? config.getCurrency() : "KES");
        receiptFooterField.setText(config.getReceiptFooter() != null ? config.getReceiptFooter() : "");
        useBarcodeCheckbox.setSelected(config.getUseBarcodeScanner() != null && config.getUseBarcodeScanner());
        lowStockField.setText(config.getLowStockAlertLevel() != null ? config.getLowStockAlertLevel().toString() : "10");
    }

    private void saveConfig() {
        config.setStoreName(storeNameField.getText().trim());
        config.setStoreAddress(storeAddressField.getText().trim());
        config.setStorePhone(storePhoneField.getText().trim());
        config.setStoreEmail(storeEmailField.getText().trim());
        config.setUseTax(useTaxCheckbox.isSelected());
        config.setTaxInclusiveInPrice(taxInclusiveCheckbox.isSelected());
        config.setTaxName(taxNameField.getText().trim());
        config.setTaxRate(MyUtils.getDouble(taxRateField.getText().trim()));
        config.setCurrency(currencyField.getText().trim());
        config.setReceiptFooter(receiptFooterField.getText().trim());
        config.setUseBarcodeScanner(useBarcodeCheckbox.isSelected());
        config.setLowStockAlertLevel(MyUtils.getInt(lowStockField.getText().trim()));

        DatabaseUtils.getInstance().getStoreConfigRepository().saveOrUpdate(config);
        AutoClosingNotification.sendNotification("Settings saved");
    }
}
