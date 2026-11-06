package org.PSmart.POS.ui.MainView.ui.stock.receive;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.*;
import org.PSmart.POS.Utils.Components.PTextField;
import org.PSmart.POS.Utils.Components.forms.FormFrame;
import org.PSmart.POS.Utils.Components.forms.Ui;
import org.PSmart.POS.Utils.MyUtils;
import org.PSmart.POS.Utils.ReceiptPrinter;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * Receive stock = record a supplier delivery/invoice: supplier, invoice no., items with cost, selling price
 * and expiry, what was paid now and what is still owed. One save books the purchase, raises stock and
 * updates product prices.
 */
public final class ReceiveStockForm extends JFrame {

    private static final int C_ITEM = 0, C_QTY = 1, C_COST = 2, C_SELL = 3, C_EXP = 4, C_TOTAL = 5, C_X = 6;
    private static final Color NAVY = FormFrame.NAVY;

    private static final class ItemRef {
        UUID uid; String name; String barcode; double cost; double sell;
        @Override public String toString() { return name; }
    }

    private final User user;
    private final Runnable onSaved;
    private final List<ItemRef> catalog = new ArrayList<>();
    private final List<ItemRef> lineItems = new ArrayList<>();

    private final JComboBox<String> supplier = new JComboBox<>();
    private final PTextField invoice = new PTextField(NAVY, FormFrame.INK, "Supplier invoice no. (optional)", null);
    private final org.jdatepicker.JDatePicker datePick = org.PSmart.POS.Utils.Components.Inputs.datePicker(java.time.LocalDate.now());
    private final JLabel dateCaption = new JLabel("Date received");
    private final PTextField search = new PTextField(NAVY, FormFrame.INK, "Add item: scan barcode or type a name, press Enter", null);
    private final JList<ItemRef> suggestions = new JList<>(new DefaultListModel<>());
    private final JScrollPane suggestScroll = new JScrollPane(suggestions);
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Item", "Qty", "Cost (each)", "Selling price", "Expiry (dd-MM-yyyy)", "Line total", ""}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return c >= C_QTY && c <= C_EXP; }
    };
    private final JTable table = new JTable(model);
    private final JLabel subtotalL = new JLabel(), taxL = new JLabel(), totalL = new JLabel(), balanceL = new JLabel();
    private final JLabel subV = new JLabel("", SwingConstants.RIGHT), taxV = new JLabel("", SwingConstants.RIGHT),
            totV = new JLabel("", SwingConstants.RIGHT), balV = new JLabel("", SwingConstants.RIGHT);
    private final JTextField paid = new JTextField();
    private final JTextArea notes = new JTextArea(2, 30);
    private final JLabel error = new JLabel(" ");
    private final JCheckBox printIt = new JCheckBox("Print the purchase invoice after saving");
    private boolean paidTouched;
    private boolean updating;
    private StoreConfig config;

    public ReceiveStockForm(User user, Runnable onSaved) {
        super("Receive Stock");
        this.user = user;
        this.onSaved = onSaved;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(Color.WHITE);
        setLayout(null);
        config = DatabaseUtils.getInstance().getStoreConfigRepository().getStoreConfig();
        if (config == null) config = StoreConfig.defaultConfig();
        loadCatalogAndSuppliers();
        build();
        Dimension scr = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(1180, scr.width - 60), Math.min(780, scr.height - 80));
        setLocationRelativeTo(null);
        layoutAll();
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) { layoutAll(); }
        });
        setVisible(true);
        search.getField().requestFocusInWindow();
    }

    // ------------------------------------------------------------------ data

    private void loadCatalogAndSuppliers() {
        EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
        try {
            for (Object o : em.createQuery("SELECT i.uid, i.name, i.barcode, i.buyingPrice, i.sellingPrice FROM Item i WHERE i.deleted=false ORDER BY i.name").getResultList()) {
                Object[] r = (Object[]) o;
                ItemRef it = new ItemRef();
                it.uid = (UUID) r[0];
                it.name = String.valueOf(r[1]);
                it.barcode = r[2] == null ? "" : String.valueOf(r[2]);
                it.cost = r[3] == null ? 0 : ((Number) r[3]).doubleValue();
                it.sell = r[4] == null ? 0 : ((Number) r[4]).doubleValue();
                catalog.add(it);
            }
            Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            for (Object o : em.createQuery("SELECT s.name FROM Supplier s WHERE s.deleted=false").getResultList()) if (o != null) names.add(String.valueOf(o));
            for (Object o : em.createQuery("SELECT DISTINCT p.supplierName FROM Purchase p WHERE p.deleted=false AND p.supplierName IS NOT NULL").getResultList()) names.add(String.valueOf(o));
            supplier.addItem("");
            for (String n : names) supplier.addItem(n);
        } finally {
            em.close();
        }
    }

    // ------------------------------------------------------------------ UI

    private JLabel heading(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Courier", Font.BOLD, 14));
        l.setForeground(NAVY);
        return l;
    }

    private JLabel supplierCaption, itemsCaption, payCaption, notesCaption, titleL;
    private JScrollPane tableScroll, notesScroll;
    private JPanel totalsBox;
    private JButton cancelBtn, saveBtn, gridBtn;

    private void build() {
        titleL = new JLabel("Receive Stock");
        titleL.setFont(MyUtils.courierFont(24));
        titleL.setForeground(NAVY);
        add(titleL);

        supplierCaption = heading("Supplier  (type a new name to add one)");
        add(supplierCaption);
        supplier.setEditable(true);
        supplier.setFont(new Font("Courier", Font.PLAIN, 16));
        supplier.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { updatePaymentVisibility(); }
        });
        supplier.addActionListener(e -> updatePaymentVisibility());
        add(supplier);
        add(invoice);
        dateCaption.setFont(new Font("Courier", Font.BOLD, 15));
        dateCaption.setForeground(NAVY);
        add(dateCaption);
        add(datePick);

        itemsCaption = heading("Items received");
        add(itemsCaption);
        add(search);
        search.getField().addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) { addFromSearch(); return; }
                if (e.getKeyCode() == KeyEvent.VK_DOWN && suggestScroll.isVisible()) { suggestions.requestFocusInWindow(); suggestions.setSelectedIndex(0); return; }
                refreshSuggestions();
            }
        });
        gridBtn = Ui.neutral("Browse items");
        gridBtn.addActionListener(e -> browseItems());
        add(gridBtn);

        suggestions.setFont(new Font("Courier", Font.PLAIN, 15));
        suggestions.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                ItemRef it = (ItemRef) v;
                JLabel c = (JLabel) super.getListCellRendererComponent(l, it.name + (it.barcode.isEmpty() ? "" : "   [" + it.barcode + "]"), i, s, f);
                c.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
                return c;
            }
        });
        suggestions.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { ItemRef it = suggestions.getSelectedValue(); if (it != null) addItem(it); }
        });
        suggestions.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) { ItemRef it = suggestions.getSelectedValue(); if (it != null) addItem(it); }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) { suggestScroll.setVisible(false); search.getField().requestFocusInWindow(); }
            }
        });
        suggestScroll.setVisible(false);
        suggestScroll.setBorder(BorderFactory.createLineBorder(NAVY));
        add(suggestScroll);

        Ui.styleTable(table);
        table.setRowHeight(34);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        right.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        for (int c : new int[]{C_QTY, C_COST, C_SELL}) {
            table.getColumnModel().getColumn(c).setCellRenderer(right);
            table.getColumnModel().getColumn(c).setCellEditor(org.PSmart.POS.Utils.Components.Inputs.decimalCellEditor(15));
        }
        table.getColumnModel().getColumn(C_EXP).setCellEditor(org.PSmart.POS.Utils.Components.Inputs.dateCellEditor());
        // line total is derived from qty x cost at paint time (the model must not be edited from its own listener)
        table.getColumnModel().getColumn(C_TOTAL).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean f, int r, int c) {
                double q = num(model.getValueAt(r, C_QTY)), cost = num(model.getValueAt(r, C_COST));
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, Double.isNaN(q) || Double.isNaN(cost) ? "?" : fmt2(q * cost), sel, f, r, c);
                l.setHorizontalAlignment(SwingConstants.RIGHT);
                l.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                l.setFont(l.getFont().deriveFont(Font.BOLD));
                return l;
            }
        });
        table.getColumnModel().getColumn(C_X).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, "x", s, f, r, c);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setForeground(Color.RED);
                l.setToolTipText("Remove this line");
                return l;
            }
        });
        int[] w = {360, 90, 120, 130, 150, 130, 40};
        for (int i = 0; i < w.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int r = table.rowAtPoint(e.getPoint()), c = table.columnAtPoint(e.getPoint());
                if (r >= 0 && c == C_X) { if (table.isEditing()) table.getCellEditor().stopCellEditing(); removeLine(r); }
            }
        });
        model.addTableModelListener(e -> { if (!updating) recalc(); });
        tableScroll = new JScrollPane(table);
        add(tableScroll);

        totalsBox = new JPanel(null);
        totalsBox.setOpaque(false);
        payCaption = heading("Payment to supplier");
        for (JLabel l : new JLabel[]{subtotalL, taxL, totalL, balanceL}) { l.setFont(new Font("Courier", Font.BOLD, 15)); }
        totalL.setFont(new Font("Courier", Font.BOLD, 22));
        totalL.setForeground(NAVY);
        totalsBox.add(subtotalL);
        totalsBox.add(taxL);
        totalsBox.add(totalL);
        for (JLabel v : new JLabel[]{subV, taxV, totV, balV}) { v.setFont(new Font("Courier", Font.BOLD, 15)); totalsBox.add(v); }
        totV.setFont(new Font("Courier", Font.BOLD, 22));
        totV.setForeground(NAVY);
        org.PSmart.POS.Utils.Components.Inputs.decimal(paid);
        paid.setFont(new Font("Courier", Font.BOLD, 20));
        paid.setHorizontalAlignment(JTextField.RIGHT);
        paid.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, NAVY), BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        paid.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { if (updating) return; paidTouched = true; recalc(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { if (updating) return; paidTouched = true; recalc(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { }
        });
        totalsBox.add(paid);
        balanceL.setFont(new Font("Courier", Font.BOLD, 17));
        totalsBox.add(balanceL);
        add(payCaption);
        add(totalsBox);

        notesCaption = heading("Notes (optional)");
        notes.setFont(new Font("Courier", Font.PLAIN, 14));
        notes.setLineWrap(true);
        notesScroll = new JScrollPane(notes);
        add(notesCaption);
        add(notesScroll);

        error.setFont(new Font("Courier", Font.BOLD, 14));
        error.setForeground(Color.RED);
        add(error);
        printIt.setOpaque(false);
        printIt.setFont(new Font("Courier", Font.PLAIN, 14));
        add(printIt);

        cancelBtn = MyUtils.createButton("Cancel", FormFrame.SLATE);
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.setFont(MyUtils.courierFont(18));
        cancelBtn.addActionListener(e -> dispose());
        saveBtn = MyUtils.createButton("Save & Receive Stock", Color.BLUE);
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFont(MyUtils.courierFont(18));
        saveBtn.addActionListener(e -> save());
        add(cancelBtn);
        add(saveBtn);
        recalc();
        updatePaymentVisibility();
    }

    private void layoutAll() {
        int W = getContentPane().getWidth(), H = getContentPane().getHeight();
        if (W <= 0) return;
        int m = 24;
        titleL.setBounds(m, 10, 400, 36);
        supplierCaption.setBounds(m, 52, 420, 20);
        supplier.setBounds(m, 76, 420, 34);
        invoice.setLocation(m + 440, 52);
        invoice.setSize(300, 65);
        dateCaption.setBounds(m + 760, 56, 260, 22);
        datePick.setBounds(m + 760, 80, Math.min(260, Math.max(200, W - m - 760 - m)), 34);
        itemsCaption.setBounds(m, 124, 300, 20);
        search.setLocation(m, 140);
        search.setSize(W - 2 * m - 190, 65);
        gridBtn.setBounds(W - m - 170, 168, 170, 34);
        int tableTop = 214, bottomBlock = 232;
        tableScroll.setBounds(m, tableTop, W - 2 * m, H - tableTop - bottomBlock - 70);
        suggestScroll.setBounds(m, 204, W - 2 * m - 190, 190);
        int by = H - bottomBlock - 56;
        // payment block (right) and notes (left)
        payCaption.setBounds(W - m - 440, by, 440, 20);
        totalsBox.setBounds(W - m - 440, by + 22, 440, 190);
        subtotalL.setBounds(0, 0, 200, 22);
        subV.setBounds(200, 0, 240, 22);
        taxL.setBounds(0, 24, 200, 22);
        taxV.setBounds(200, 24, 240, 22);
        totalL.setBounds(0, 48, 200, 32);
        totV.setBounds(200, 48, 240, 32);
        paid.setBounds(200, 88, 240, 38);
        balanceL.setBounds(0, 134, 260, 26);
        balV.setBounds(260, 134, 180, 26);
        notesCaption.setBounds(m, by, 300, 20);
        notesScroll.setBounds(m, by + 22, W - 2 * m - 480, 80);
        printIt.setBounds(m, by + 112, 420, 24);
        error.setBounds(m, H - 98, W - 2 * m, 24);
        cancelBtn.setBounds(W - m - 190 - 12 - 280, H - 66, 190, 48);
        saveBtn.setBounds(W - m - 280, H - 66, 280, 48);
        paidLabelBounds();
        revalidate();
        repaint();
    }

    private JLabel paidLabel;

    private void paidLabelBounds() {
        if (paidLabel == null) {
            paidLabel = new JLabel("Paid now");
            paidLabel.setFont(new Font("Courier", Font.BOLD, 15));
            totalsBox.add(paidLabel);
        }
        paidLabel.setBounds(0, 94, 190, 26);
    }

    private void updatePaymentVisibility() {
        boolean named = !supplierName().isEmpty();
        payCaption.setText(named ? "Payment to supplier" : "Payment (add a supplier name to leave a balance owing)");
        paid.setEnabled(named);
        if (!named) { updating = true; paid.setText(""); updating = false; paidTouched = false; }
        recalc();
    }

    private String supplierName() {
        Object o = supplier.getEditor().getItem();
        return o == null ? "" : o.toString().trim();
    }

    // ------------------------------------------------------------------ lines

    private void refreshSuggestions() {
        String q = search.getText().trim().toLowerCase();
        DefaultListModel<ItemRef> m = (DefaultListModel<ItemRef>) suggestions.getModel();
        m.clear();
        if (q.length() >= 1) {
            for (ItemRef it : catalog) {
                if (it.name.toLowerCase().contains(q) || (!it.barcode.isEmpty() && it.barcode.toLowerCase().contains(q))) {
                    m.addElement(it);
                    if (m.size() >= 8) break;
                }
            }
        }
        suggestScroll.setVisible(!m.isEmpty());
        suggestScroll.setSize(suggestScroll.getWidth(), Math.min(190, 26 * Math.max(1, m.size()) + 6));
        if (suggestScroll.isVisible()) getContentPane().setComponentZOrder(suggestScroll, 0);
        repaint();
    }

    private void addFromSearch() {
        String q = search.getText().trim();
        if (q.isEmpty()) return;
        for (ItemRef it : catalog) if (!it.barcode.isEmpty() && it.barcode.equalsIgnoreCase(q)) { addItem(it); return; }
        DefaultListModel<ItemRef> m = (DefaultListModel<ItemRef>) suggestions.getModel();
        if (!m.isEmpty()) addItem(m.get(0));
        else say("No item matches \"" + q + "\". Add it first under Stock > All Items.");
    }

    private void addItem(ItemRef it) {
        say(null);
        for (int i = 0; i < lineItems.size(); i++) {
            if (lineItems.get(i).uid.equals(it.uid)) {
                double q = num(model.getValueAt(i, C_QTY));
                updating = true;
                model.setValueAt(fmt(q + 1), i, C_QTY);
                updating = false;
                recalc();
                clearSearch();
                return;
            }
        }
        lineItems.add(it);
        updating = true;
        model.addRow(new Object[]{it.name, "1", fmt2(it.cost), fmt2(it.sell), "", fmt2(it.cost), ""});
        updating = false;
        recalc();
        clearSearch();
        int last = model.getRowCount() - 1;
        table.changeSelection(last, C_QTY, false, false);
        table.editCellAt(last, C_QTY);
        Component ed = table.getEditorComponent();
        if (ed != null) { ed.requestFocusInWindow(); if (ed instanceof JTextField) ((JTextField) ed).selectAll(); }
    }

    private void clearSearch() {
        search.setText("");
        suggestScroll.setVisible(false);
        ((DefaultListModel<ItemRef>) suggestions.getModel()).clear();
    }

    private void removeLine(int r) {
        updating = true;
        model.removeRow(r);
        lineItems.remove(r);
        updating = false;
        recalc();
    }

    private void browseItems() {
        try {
            new org.PSmart.POS.Utils.Components.ItemPicker("Browse items", "Add to delivery", false, picks -> {
                for (org.PSmart.POS.Utils.Components.ItemPicker.Pick p : picks) {
                    UUID id = p.item.getUid();
                    for (ItemRef it : catalog) if (it.uid.equals(id)) {
                        addItem(it);
                        int row = lineItems.indexOf(it);
                        updating = true;
                        model.setValueAt(fmt(Math.max(1, p.quantity)), row, C_QTY);
                        updating = false;
                    }
                }
                recalc();
            });
        } catch (Exception ex) {
            say("Could not open the item browser: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------------ maths

    private static double num(Object o) {
        if (o == null) return 0;
        String s = o.toString().trim().replace(",", "");
        if (s.isEmpty()) return 0;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return Double.NaN; }
    }

    private static String fmt(double v) { return v == Math.floor(v) ? String.format("%.0f", v) : String.format("%.2f", v); }
    private static String fmt2(double v) { return String.format("%.2f", v); }
    private static String money(double v) { return String.format("%,.2f", v); }

    private double subtotal() {
        double s = 0;
        for (int i = 0; i < model.getRowCount(); i++) {
            double q = num(model.getValueAt(i, C_QTY)), c = num(model.getValueAt(i, C_COST));
            if (!Double.isNaN(q) && !Double.isNaN(c)) s += q * c;
        }
        return s;
    }

    private double tax(double sub) {
        if (Boolean.TRUE.equals(config.getUseTax()) && config.getTaxRate() != null && !Boolean.TRUE.equals(config.getTaxInclusiveInPrice())) {
            return sub * config.getTaxRate() / 100.0;
        }
        return 0;
    }

    private void recalc() {
        updating = true;
        table.repaint();
        double sub = subtotal(), tax = tax(sub), total = sub + tax;
        subtotalL.setText("Subtotal");
        subV.setText(money(sub));
        taxL.setText(tax > 0 ? (config.getTaxName() == null ? "Tax" : config.getTaxName()) : " ");
        taxV.setText(tax > 0 ? money(tax) : " ");
        totalL.setText("TOTAL");
        totV.setText(money(total));
        if (!paidTouched && paid.isEnabled()) paid.setText(total > 0 ? fmt2(total) : "");
        double p = paid.isEnabled() ? Math.max(0, Double.isNaN(num(paid.getText())) ? 0 : num(paid.getText())) : total;
        double owe = Math.max(0, total - p);
        balanceL.setText("Balance owed to supplier");
        balV.setText(money(owe));
        Color bc = owe > 0.004 ? Color.RED : new Color(30, 110, 50);
        balanceL.setForeground(bc);
        balV.setForeground(bc);
        updating = false;
    }

    private static String pad(String v) { return "  " + String.format("%24s", v); }
    private static String pad2(String v) { return "  " + v; }

    private void say(String msg) {
        error.setText(msg == null || msg.isEmpty() ? " " : msg);
    }

    // ------------------------------------------------------------------ save

    private void save() {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        say(null);
        if (model.getRowCount() == 0) { say("Add at least one item."); return; }
        java.time.LocalDate day = org.PSmart.POS.Utils.Components.Inputs.dateOf(datePick);
        if (day == null) { say("Choose the date the stock was received."); return; }
        long receivedAt = day.atTime(12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();

        List<double[]> nums = new ArrayList<>();
        List<Long> expiries = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            double q = num(model.getValueAt(i, C_QTY)), c = num(model.getValueAt(i, C_COST)), s = num(model.getValueAt(i, C_SELL));
            String name = String.valueOf(model.getValueAt(i, C_ITEM));
            if (Double.isNaN(q) || q <= 0) { say("Line " + (i + 1) + " (" + name + "): enter a quantity above zero."); return; }
            if (Double.isNaN(c) || c < 0) { say("Line " + (i + 1) + " (" + name + "): enter a valid cost."); return; }
            if (Double.isNaN(s) || s < 0) { say("Line " + (i + 1) + " (" + name + "): enter a valid selling price."); return; }
            Long exp = null;
            String e = String.valueOf(model.getValueAt(i, C_EXP) == null ? "" : model.getValueAt(i, C_EXP)).trim();
            if (!e.isEmpty()) {
                try {
                    exp = java.time.LocalDate.parse(e, java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                } catch (Exception ex) { say("Line " + (i + 1) + " (" + name + "): pick the expiry date from the calendar, or leave it empty."); return; }
            }
            nums.add(new double[]{q, c, s});
            expiries.add(exp);
        }
        double sub = subtotal(), tax = tax(sub), total = sub + tax;
        String sup = supplierName();
        double paidNow = total;
        if (!sup.isEmpty()) {
            double p = num(paid.getText());
            if (Double.isNaN(p)) { say("Enter the amount paid now (0 if nothing)."); return; }
            if (p < 0 || p > total + 0.009) { say("Paid now must be between 0 and " + money(total) + "."); return; }
            paidNow = p;
        }

        User approver = org.PSmart.POS.Utils.SupervisorApproval.require(this, user, RoleType.PURCHASE, "receive stock");
        if (approver == null) { say("Receiving stock needs a supervisor's approval."); return; }
        EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Purchase purchase = Purchase.builder().createdAt(receivedAt).createdBy(user == null ? null : em.getReference(User.class, user.getUid()))
                    .supplierName(sup.isEmpty() ? null : sup).invoiceNumber(invoice.getText().trim().isEmpty() ? null : invoice.getText().trim())
                    .notes(notes.getText().trim().isEmpty() ? null : notes.getText().trim())
                    .subtotal(sub).taxAmount(tax).total(total).paidAmount(paidNow).status("COMPLETED").build();
            List<PurchaseItem> pItems = new ArrayList<>();
            List<StockFeedItem> fItems = new ArrayList<>();
            for (int i = 0; i < lineItems.size(); i++) {
                ItemRef ref = lineItems.get(i);
                double[] n = nums.get(i);
                Item item = em.getReference(Item.class, ref.uid);
                pItems.add(PurchaseItem.builder().purchase(purchase).item(item).quantity(n[0]).unitPrice(n[1]).total(n[0] * n[1]).build());
                StockFeedItem fi = StockFeedItem.builder().item(item).quantity(n[0]).buyingPrice(n[1]).sellingPrice(n[2]).expiry(expiries.get(i)).build();
                em.persist(fi);
                fItems.add(fi);
                // keep the product's current prices in step with what was just paid / will be charged
                em.createQuery("UPDATE Item i SET i.buyingPrice=:c, i.sellingPrice=:s WHERE i.uid=:id")
                        .setParameter("c", n[1]).setParameter("s", n[2]).setParameter("id", ref.uid).executeUpdate();
            }
            purchase.setItems(pItems);
            StockFeed feed = StockFeed.builder().doneOn(receivedAt)
                    .comment("Purchase" + (purchase.getInvoiceNumber() == null ? "" : " " + purchase.getInvoiceNumber()) + (sup.isEmpty() ? "" : " from " + sup))
                    .stockFeedItems(fItems).build();
            em.persist(feed);
            purchase.setFeedUid(feed.getUid());   // lets "Cancel purchase" take this delivery back out of stock
            em.persist(purchase);
            tx.commit();

            Ui.toast("Stock received - " + model.getRowCount() + " item(s)" + (total - paidNow > 0.004 ? ", " + money(total - paidNow) + " owed to " + sup : ""));
            if (printIt.isSelected()) ReceiptPrinter.previewPurchase(this, purchase, config);
            dispose();
            if (onSaved != null) onSaved.run();
        } catch (Exception ex) {
            if (tx.isActive()) tx.rollback();
            Throwable t = ex;
            while (t.getCause() != null) t = t.getCause();
            say("Could not save: " + t.getMessage());
        } finally {
            em.close();
        }
    }
}
