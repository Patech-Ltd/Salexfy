package org.PSmart.POS.ui.MainView.ui.purchases;

import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.*;
import org.PSmart.POS.Repositories.ExpenseRepository;
import org.PSmart.POS.Repositories.PurchaseRepository;
import org.PSmart.POS.Utils.AuthorizationUtils;
import org.PSmart.POS.Utils.MyUtils;
import org.PSmart.POS.tests.AutoClosingNotification;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class PurchasesPanel extends JPanel {

    private JTabbedPane tabbedPane;
    private JPanel purchasesTab;
    private JPanel expensesTab;

    private final User sessionUser;

    public PurchasesPanel() {
        this(null);
    }

    public PurchasesPanel(User sessionUser) {
        super();
        this.sessionUser = sessionUser;
        draw();
    }

    private void draw() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 250));

        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setFont(new Font("Courier", Font.BOLD, 16));

        purchasesTab = new PurchasesSubPanel();
        expensesTab = new ExpensesSubPanel();

        tabbedPane.addTab("Purchases", purchasesTab);
        tabbedPane.addTab("Expenses", expensesTab);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private class PurchasesSubPanel extends JPanel {
        private DefaultTableModel tableModel;
        private JTable table;
        private PurchaseRepository repo = DatabaseUtils.getInstance().getPurchaseRepository();

        PurchasesSubPanel() {
            setLayout(new BorderLayout());
            setBackground(new Color(245, 245, 250));
            add(buildHeader(), BorderLayout.NORTH);
            add(buildSummary(), BorderLayout.CENTER);
            add(buildTable(), BorderLayout.SOUTH);
            loadPurchases();
        }

        private JPanel buildHeader() {
            JPanel h = new JPanel(new BorderLayout());
            h.setBackground(new Color(52, 58, 64));
            h.setPreferredSize(new Dimension(0, 55));
            h.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
            JLabel t = new JLabel("Purchase Invoices");
            t.setFont(new Font("Courier", Font.BOLD, 20));
            t.setForeground(Color.WHITE);
            h.add(t, BorderLayout.WEST);

            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
            right.setOpaque(false);

            JButton newBtn = new JButton("+ New Purchase");
            newBtn.setFont(new Font("Courier", Font.BOLD, 13));
            newBtn.setBackground(new Color(40, 167, 69));
            newBtn.setForeground(Color.WHITE);
            newBtn.setFocusPainted(false);
            newBtn.setBorderPainted(false);
            newBtn.addActionListener(e -> openPurchaseDialog());
            right.add(newBtn);

            JButton refreshBtn = new JButton("Refresh");
            refreshBtn.setFont(new Font("Courier", Font.BOLD, 13));
            refreshBtn.setBackground(new Color(39, 144, 255));
            refreshBtn.setForeground(Color.WHITE);
            refreshBtn.setFocusPainted(false);
            refreshBtn.setBorderPainted(false);
            refreshBtn.addActionListener(e -> loadPurchases());
            right.add(refreshBtn);

            JButton printBtn = new JButton("Print / Preview Invoice");
            printBtn.setFont(new Font("Courier", Font.BOLD, 13));
            printBtn.setBackground(new Color(52, 58, 64));
            printBtn.setForeground(Color.WHITE);
            printBtn.setFocusPainted(false);
            printBtn.setBorderPainted(false);
            printBtn.setToolTipText("Select a purchase in the list first (or double-click it)");
            printBtn.addActionListener(e -> previewSelectedPurchase());
            right.add(printBtn);

            h.add(right, BorderLayout.EAST);
            return h;
        }

        private List<UUID> shownPurchases = new ArrayList<>();
        private JLabel countValL, spendValL, todayValL;

        private void previewSelectedPurchase() {
            int r = table.getSelectedRow();
            if (r < 0 || r >= shownPurchases.size()) {
                AutoClosingNotification.sendNotification("Select a purchase in the list first");
                return;
            }
            StoreConfig cfg = DatabaseUtils.getInstance().getStoreConfigRepository().getStoreConfig();
            Purchase full = org.PSmart.POS.Repositories.PurchaseQueries.load(shownPurchases.get(r));
            if (full == null) { AutoClosingNotification.sendNotification("That purchase could not be found"); return; }
            org.PSmart.POS.Utils.ReceiptPrinter.previewPurchase(this, full,
                    cfg != null ? cfg : StoreConfig.defaultConfig());
        }

        private JPanel buildSummary() {
            JPanel cards = new JPanel(new GridLayout(1, 3, 15, 0));
            cards.setBackground(new Color(245, 245, 250));
            cards.setBorder(BorderFactory.createEmptyBorder(10, 20, 5, 20));
            cards.setPreferredSize(new Dimension(0, 80));

            JLabel countVal = new JLabel("0", SwingConstants.CENTER);
            countValL = countVal;
            countVal.setFont(new Font("Courier", Font.BOLD, 22));
            countVal.setForeground(new Color(0, 123, 255));
            JPanel c1 = makeCard("Total Purchases", countVal);

            JLabel spendVal = new JLabel("KES 0.00", SwingConstants.CENTER);
            spendValL = spendVal;
            spendVal.setFont(new Font("Courier", Font.BOLD, 22));
            spendVal.setForeground(new Color(111, 66, 193));
            JPanel c2 = makeCard("Total Spend", spendVal);

            JLabel todayVal = new JLabel("0", SwingConstants.CENTER);
            todayValL = todayVal;
            todayVal.setFont(new Font("Courier", Font.BOLD, 22));
            todayVal.setForeground(new Color(40, 167, 69));
            JPanel c3 = makeCard("Today", todayVal);

            cards.add(c1);
            cards.add(c2);
            cards.add(c3);

            return cards;
        }

        private JPanel makeCard(String title, JLabel value) {
            JPanel c = new JPanel(new BorderLayout());
            c.setBackground(Color.WHITE);
            c.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));
            JLabel tl = new JLabel("  " + title);
            tl.setFont(new Font("Courier", Font.PLAIN, 12));
            tl.setForeground(Color.GRAY);
            c.add(tl, BorderLayout.NORTH);
            c.add(value, BorderLayout.CENTER);
            return c;
        }

        private JScrollPane buildTable() {
            String[] cols = {"Ref #", "Date", "Supplier", "Invoice #", "Items", "Total", "Status"};
            tableModel = new DefaultTableModel(cols, 0) {
                public boolean isCellEditable(int r, int c) { return false; }
            };
            table = new JTable(tableModel);
            table.setRowHeight(38);
            table.setFont(new Font("Courier", Font.PLAIN, 13));
            JTableHeader h = table.getTableHeader();
            h.setFont(new Font("Courier", Font.BOLD, 13));
            h.setBackground(new Color(52, 58, 64));
            h.setForeground(Color.WHITE);

            DefaultTableCellRenderer center = new DefaultTableCellRenderer();
            center.setHorizontalAlignment(JLabel.CENTER);
            for (int i = 0; i < cols.length; i++) table.getColumnModel().getColumn(i).setCellRenderer(center);

            table.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 2) previewSelectedPurchase();
                }
            });
            JScrollPane sp = new JScrollPane(table);
            sp.setBorder(BorderFactory.createEmptyBorder(0, 20, 15, 20));
            sp.setPreferredSize(new Dimension(0, 400));
            sp.getViewport().setBackground(Color.WHITE);
            return sp;
        }

        void loadPurchases() {
            tableModel.setRowCount(0);
            shownPurchases = new ArrayList<>();
            List<org.PSmart.POS.Repositories.PurchaseQueries.Row> list;
            try {
                list = org.PSmart.POS.Repositories.PurchaseQueries.rows();
            } catch (Exception ex) {
                AutoClosingNotification.sendNotification("Could not load purchases: " + ex.getMessage());
                return;
            }
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
            long startOfDay = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            double spend = 0;
            int today = 0;
            for (org.PSmart.POS.Repositories.PurchaseQueries.Row p : list) {
                shownPurchases.add(p.uid);
                spend += p.total;
                if (p.createdAt >= startOfDay) today++;
                double owed = Math.max(0, p.total - p.paid);
                tableModel.addRow(new Object[]{
                        p.ref, p.createdAt > 0 ? sdf.format(new Date(p.createdAt)) : "N/A",
                        p.supplier.isEmpty() ? "N/A" : p.supplier, p.invoice.isEmpty() ? "N/A" : p.invoice, p.items,
                        "KES " + MyUtils.formatCurrency(String.valueOf(p.total)),
                        owed > 0.004 ? "OWING " + MyUtils.formatCurrency(String.valueOf(owed)) : "PAID"
                });
            }
            if (countValL != null) countValL.setText(String.valueOf(list.size()));
            if (spendValL != null) spendValL.setText("KES " + MyUtils.formatCurrency(String.valueOf(spend)));
            if (todayValL != null) todayValL.setText(String.valueOf(today));
        }

        private void openPurchaseDialog() {
            if (!AuthorizationUtils.authorizeAction(this, getLoggedUser(), RoleType.PURCHASE, "Create Purchase")) return;
            new org.PSmart.POS.ui.MainView.ui.stock.receive.ReceiveStockForm(getLoggedUser(), this::loadPurchases);
        }

        private User getLoggedUser() {
            if (sessionUser != null) return sessionUser;
            try {
                String hql = "FROM User u WHERE u.deleted=false";
                return DatabaseUtils.getInstance().getEmf().createEntityManager()
                        .createQuery(hql, User.class).getResultList().stream().findFirst().orElse(null);
            } catch (Exception e) { return null; }
        }
    }

    private class ExpensesSubPanel extends JPanel {
        private DefaultTableModel tableModel;
        private JTable table;
        private ExpenseRepository repo = DatabaseUtils.getInstance().getExpenseRepository();
        private JLabel totalVal, todayVal;

        ExpensesSubPanel() {
            setLayout(new BorderLayout());
            setBackground(new Color(245, 245, 250));
            add(buildHeader(), BorderLayout.NORTH);
            add(buildSummary(), BorderLayout.CENTER);
            add(buildTable(), BorderLayout.SOUTH);
            loadExpenses();
        }

        private JPanel buildHeader() {
            JPanel h = new JPanel(new BorderLayout());
            h.setBackground(new Color(52, 58, 64));
            h.setPreferredSize(new Dimension(0, 55));
            h.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
            JLabel t = new JLabel("Expenses");
            t.setFont(new Font("Courier", Font.BOLD, 20));
            t.setForeground(Color.WHITE);
            h.add(t, BorderLayout.WEST);

            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
            right.setOpaque(false);

            JButton newBtn = new JButton("+ New Expense");
            newBtn.setFont(new Font("Courier", Font.BOLD, 13));
            newBtn.setBackground(new Color(220, 53, 69));
            newBtn.setForeground(Color.WHITE);
            newBtn.setFocusPainted(false);
            newBtn.setBorderPainted(false);
            newBtn.addActionListener(e -> openExpenseForm());
            right.add(newBtn);

            JButton refreshBtn = new JButton("Refresh");
            refreshBtn.setFont(new Font("Courier", Font.BOLD, 13));
            refreshBtn.setBackground(new Color(39, 144, 255));
            refreshBtn.setForeground(Color.WHITE);
            refreshBtn.setFocusPainted(false);
            refreshBtn.setBorderPainted(false);
            refreshBtn.addActionListener(e -> loadExpenses());
            right.add(refreshBtn);

            h.add(right, BorderLayout.EAST);
            return h;
        }

        private JPanel buildSummary() {
            JPanel cards = new JPanel(new GridLayout(1, 2, 15, 0));
            cards.setBackground(new Color(245, 245, 250));
            cards.setBorder(BorderFactory.createEmptyBorder(10, 20, 5, 20));
            cards.setPreferredSize(new Dimension(0, 80));

            totalVal = new JLabel("KES 0.00", SwingConstants.CENTER);
            totalVal.setFont(new Font("Courier", Font.BOLD, 22));
            totalVal.setForeground(new Color(220, 53, 69));
            JPanel c1 = new JPanel(new BorderLayout());
            c1.setBackground(Color.WHITE);
            c1.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));
            c1.add(new JLabel("  Total Expenses", SwingConstants.LEFT), BorderLayout.NORTH);
            c1.add(totalVal, BorderLayout.CENTER);

            todayVal = new JLabel("KES 0.00", SwingConstants.CENTER);
            todayVal.setFont(new Font("Courier", Font.BOLD, 22));
            todayVal.setForeground(new Color(255, 193, 7));
            JPanel c2 = new JPanel(new BorderLayout());
            c2.setBackground(Color.WHITE);
            c2.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));
            c2.add(new JLabel("  Today's Expenses", SwingConstants.LEFT), BorderLayout.NORTH);
            c2.add(todayVal, BorderLayout.CENTER);

            cards.add(c1);
            cards.add(c2);
            return cards;
        }

        private JScrollPane buildTable() {
            String[] cols = {"Ref #", "Date", "Category", "Description", "Amount", "Payment", "Status"};
            tableModel = new DefaultTableModel(cols, 0) {
                public boolean isCellEditable(int r, int c) { return false; }
            };
            table = new JTable(tableModel);
            table.setRowHeight(38);
            table.setFont(new Font("Courier", Font.PLAIN, 13));
            JTableHeader h = table.getTableHeader();
            h.setFont(new Font("Courier", Font.BOLD, 13));
            h.setBackground(new Color(52, 58, 64));
            h.setForeground(Color.WHITE);

            DefaultTableCellRenderer center = new DefaultTableCellRenderer();
            center.setHorizontalAlignment(JLabel.CENTER);
            for (int i = 0; i < cols.length; i++) table.getColumnModel().getColumn(i).setCellRenderer(center);

            JScrollPane sp = new JScrollPane(table);
            sp.setBorder(BorderFactory.createEmptyBorder(0, 20, 15, 20));
            sp.setPreferredSize(new Dimension(0, 400));
            sp.getViewport().setBackground(Color.WHITE);
            return sp;
        }

        void loadExpenses() {
            tableModel.setRowCount(0);
            List<Expense> list = repo.findAllExpenses();
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
            double total = 0, today = 0;
            String todayKey = sdf.format(new Date());
            for (Expense e : list) {
                double a = e.getAmount() == null ? 0 : e.getAmount();
                total += a;
                if (e.getCreatedAt() != null && sdf.format(new Date(e.getCreatedAt())).equals(todayKey)) today += a;
                String date = e.getCreatedAt() != null ? sdf.format(new Date(e.getCreatedAt())) : "N/A";
                tableModel.addRow(new Object[]{
                        e.getReferenceNumber(), date,
                        e.getCategory() != null ? e.getCategory() : "N/A",
                        e.getDescription() != null ? e.getDescription() : "N/A",
                        "KES " + MyUtils.formatCurrency(String.valueOf(e.getAmount() != null ? e.getAmount() : 0)),
                        e.getPaymentMethod() != null ? e.getPaymentMethod() : "Cash",
                        "Completed"
                });
            }
            totalVal.setText("KES " + MyUtils.formatCurrency(String.valueOf(total)));
            todayVal.setText("KES " + MyUtils.formatCurrency(String.valueOf(today)));
        }

        private void openExpenseForm() {
            if (!AuthorizationUtils.authorizeAction(this, getLoggedUser(), RoleType.EXPENSE, "Create Expense")) return;
            ExpenseForm.open(getLoggedUser(), this::loadExpenses);
        }

        private User getLoggedUser() {
            if (sessionUser != null) return sessionUser;
            try {
                String hql = "FROM User u WHERE u.deleted=false";
                return DatabaseUtils.getInstance().getEmf().createEntityManager()
                        .createQuery(hql, User.class).getResultList().stream().findFirst().orElse(null);
            } catch (Exception e) { return null; }
        }
    }

    private class PurchaseDialog extends JDialog {
        private JTextField supplierField, invoiceField, barcodeField, paidField;
        private JCheckBox addToStock;
        private JTextArea notesArea;
        private DefaultTableModel itemsModel;
        private JTable itemsTable;
        private List<Item> allItems;
        private Runnable onSave;

        PurchaseDialog(Frame owner) {
            super(owner, "New Purchase Invoice", true);
            allItems = org.PSmart.POS.Repositories.ItemRepository.findAll();
            setSize(800, 600);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout(8, 8));
            buildForm();
        }

        private void buildForm() {
            JPanel top = new JPanel(new GridLayout(1, 5, 10, 0));
            top.setBorder(BorderFactory.createEmptyBorder(10, 15, 5, 15));
            top.setBackground(Color.WHITE);

            supplierField = makeField(top, "Supplier Name");
            invoiceField = makeField(top, "Invoice #");
            paidField = makeField(top, "Paid Now (blank = full)");
            addToStock = new JCheckBox("<html>Add these items<br>to stock</html>", true);
            addToStock.setFont(new Font("Courier", Font.BOLD, 12));
            addToStock.setOpaque(false);
            top.add(addToStock);

            notesArea = new JTextArea(2, 20);
            notesArea.setFont(new Font("Courier", Font.PLAIN, 12));
            JPanel np = new JPanel(new BorderLayout());
            np.setOpaque(false);
            np.add(new JLabel("Notes"), BorderLayout.NORTH);
            np.add(new JScrollPane(notesArea), BorderLayout.CENTER);
            top.add(np);

            JPanel scan = new JPanel(new BorderLayout(4, 4));
            scan.setOpaque(false);
            scan.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
            JLabel scanHint = new JLabel("Barcode / Scan to add item");
            scanHint.setFont(new Font("Courier", Font.BOLD, 11));
            barcodeField = new JTextField();
            barcodeField.setFont(new Font("Courier", Font.PLAIN, 14));
            barcodeField.addActionListener(e -> scanBarcode());
            scan.add(scanHint, BorderLayout.NORTH);
            scan.add(barcodeField, BorderLayout.CENTER);
            scan.setAlignmentX(Component.LEFT_ALIGNMENT);

            add(top, BorderLayout.NORTH);

            String[] cols = {"Item", "Qty", "Unit Price", "Total"};
            itemsModel = new DefaultTableModel(cols, 0) {
                public boolean isCellEditable(int r, int c) { return true; }
            };
            itemsTable = new JTable(itemsModel);
            itemsTable.setRowHeight(30);
            itemsTable.setFont(new Font("Courier", Font.PLAIN, 13));
            JScrollPane sp = new JScrollPane(itemsTable);
            sp.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));

            JPanel middle = new JPanel(new BorderLayout(0, 4));
            middle.setOpaque(false);
            middle.add(scan, BorderLayout.NORTH);
            middle.add(sp, BorderLayout.CENTER);

            JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
            btns.setBackground(Color.WHITE);

            JButton addItemBtn = new JButton("+ Add Item");
            addItemBtn.addActionListener(e -> addPurchaseItem());
            btns.add(addItemBtn);

            JButton removeBtn = new JButton("- Remove");
            removeBtn.setBackground(new Color(220, 53, 69));
            removeBtn.setForeground(Color.WHITE);
            removeBtn.addActionListener(e -> {
                int r = itemsTable.getSelectedRow();
                if (r >= 0) itemsModel.removeRow(r);
            });
            btns.add(removeBtn);

            JButton saveBtn = new JButton("Save Purchase");
            saveBtn.setBackground(new Color(40, 167, 69));
            saveBtn.setForeground(Color.WHITE);
            saveBtn.setFocusPainted(false);
            saveBtn.setBorderPainted(false);
            saveBtn.addActionListener(e -> savePurchase());
            btns.add(saveBtn);

            JButton cancelBtn = new JButton("Cancel");
            cancelBtn.addActionListener(e -> dispose());
            btns.add(cancelBtn);

            middle.add(btns, BorderLayout.SOUTH);

            add(middle, BorderLayout.CENTER);
        }

        private void scanBarcode() {
            String code = barcodeField.getText().trim();
            if (code.isEmpty()) return;
            Item item = org.PSmart.POS.Repositories.ItemRepository.findByBarCode(code);
            if (item == null) {
                AutoClosingNotification.sendNotification("No item with barcode: " + code);
                return;
            }
            for (int i = 0; i < itemsModel.getRowCount(); i++) {
                Item existing = (Item) itemsModel.getValueAt(i, 0);
                if (existing.getUid().equals(item.getUid())) {
                    double qty = MyUtils.getDouble(itemsModel.getValueAt(i, 1).toString()) + 1;
                    double price = MyUtils.getDouble(itemsModel.getValueAt(i, 2).toString());
                    itemsModel.setValueAt(qty, i, 1);
                    itemsModel.setValueAt(qty * price, i, 3);
                    barcodeField.setText("");
                    return;
                }
            }
            double price = item.getBuyingPrice() != null ? item.getBuyingPrice() : 0.0;
            itemsModel.addRow(new Object[]{item, 1.0, price, price});
            barcodeField.setText("");
        }

        private JTextField makeField(JPanel parent, String label) {
            JPanel p = new JPanel(new BorderLayout(0, 2));
            p.setOpaque(false);
            JLabel l = new JLabel(label);
            l.setFont(new Font("Courier", Font.BOLD, 11));
            JTextField tf = new JTextField();
            tf.setFont(new Font("Courier", Font.PLAIN, 13));
            p.add(l, BorderLayout.NORTH);
            p.add(tf, BorderLayout.CENTER);
            parent.add(p);
            return tf;
        }

        private void addPurchaseItem() {
            if (allItems.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No items yet. Create items in Stock/Inventory first.",
                        "Add Item", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            AddPurchaseItemDialog dialog = new AddPurchaseItemDialog(this);
            dialog.setOnAdd((Item item, double qty, double unitPrice, double lineTotal, String label) -> {
                itemsModel.addRow(new Object[]{item, qty, unitPrice, lineTotal});
                AutoClosingNotification.sendNotification(label);
            });
            dialog.setVisible(true);
        }

    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }

    /**
     * Friendly, single-form "Add item to purchase" dialog. No stacked pop-ups.
     * Lets the user search/click an item, pick a unit (piece or carton), and see a
     * live line total before adding — carton mode auto-splits into per-piece price.
     */
    private class AddPurchaseItemDialog extends JDialog {

        interface Pipeline {
            void apply(Item item, double qty, double unitPrice, double lineTotal, String label);
        }

        private Pipeline onAdd;
        private Item selected;
        private JLabel totalLabel;
        private JComboBox<String> unitCombo;
        private JTextField qtyField;
        private JTextField cartonPriceField;
        private JLabel piecePriceHint;

        AddPurchaseItemDialog(JDialog owner) {
            super(owner, "Add Item to Purchase", true);
            setSize(760, 560);
            setLocationRelativeTo(owner);
            buildUI();
        }

        void setOnAdd(Pipeline consumer) {
            this.onAdd = consumer;
        }

        private void buildUI() {
            setLayout(new BorderLayout(12, 12));

            JPanel left = buildPicker();
            JPanel right = buildDetails();
            add(left, BorderLayout.CENTER);
            add(right, BorderLayout.EAST);
        }

        private JPanel buildPicker() {
            JPanel panel = new JPanel(new BorderLayout(0, 8));
            panel.setBackground(Color.WHITE);
            panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

            JTextField search = new JTextField();
            search.setFont(new Font("Courier", Font.PLAIN, 15));
            search.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200)),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            search.setToolTipText("Type to search by name, barcode or supplier");
            search.addKeyListener(new KeyAdapter() {
                public void keyReleased(KeyEvent e) { refreshList(search.getText()); }
            });
            panel.add(search, BorderLayout.NORTH);

            JPanel grid = new JPanel(new GridLayout(0, 2, 8, 8));
            grid.setBackground(Color.WHITE);
            JScrollPane scroll = new JScrollPane(grid);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            scroll.getViewport().setBackground(Color.WHITE);
            panel.add(scroll, BorderLayout.CENTER);

            refreshList("");
            refreshGrid = grid;
            return panel;
        }

        private JPanel refreshGrid;
        private void refreshList(String query) {
            if (refreshGrid == null) return;
            refreshGrid.removeAll();
            String q = query.trim().toLowerCase();
            java.util.List<Item> shown = new ArrayList<>();
            for (Item i : allItems) {
                if (q.isEmpty() || i.getSearchString().contains(q)) shown.add(i);
            }
            for (Item i : shown) {
                JPanel card = itemCard(i);
                refreshGrid.add(card);
            }
            refreshGrid.revalidate();
            refreshGrid.repaint();
        }

        private JPanel itemCard(Item i) {
            JPanel card = new JPanel(new BorderLayout(8, 2));
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(228, 230, 235)),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)));
            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JLabel name = new JLabel(i.getName());
            name.setFont(new Font("Courier", Font.BOLD, 13));
            name.setForeground(new Color(52, 58, 64));

            JLabel sub = new JLabel("Buy: KES " + round2(i.getBuyingPrice() != null ? i.getBuyingPrice() : 0.0)
                    + "  •  Sell: KES " + round2(i.getSellingPrice() != null ? i.getSellingPrice() : 0.0));
            sub.setFont(new Font("Courier", Font.PLAIN, 12));
            sub.setForeground(new Color(108, 117, 125));

            JPanel text = new JPanel(new GridLayout(2, 1));
            text.setOpaque(false);
            text.add(name);
            text.add(sub);
            card.add(text, BorderLayout.CENTER);

            card.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    selectItem(i);
                    highlightCard(card);
                }
            });
            return card;
        }

        private JPanel highlightedCard;
        private void highlightCard(JPanel card) {
            if (highlightedCard != null) {
                highlightedCard.setBackground(Color.WHITE);
                highlightedCard.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(228, 230, 235)),
                        BorderFactory.createEmptyBorder(10, 10, 10, 10)));
            }
            highlightedCard = card;
            card.setBackground(new Color(39, 144, 255));
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0, 123, 255)),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        }

        private void selectItem(Item i) {
            selected = i;
            piecePriceHint.setText("Item: " + i.getName());
            qtyField.setText("1");
            boolean hasCarton = i.getWholeScales() != null && i.getWholeScales().length > 0
                    && i.getWholeScales()[0].getItems() != null && i.getWholeScales()[0].getItems() > 0;
            if (hasCarton) {
                unitCombo.setEnabled(true);
                boolean combo = unitCombo.getSelectedIndex() == 1;
                cartonPriceField.setEnabled(combo);
            } else {
                unitCombo.setSelectedIndex(0);
                unitCombo.setEnabled(false);
                cartonPriceField.setEnabled(false);
            }
            updateTotal();
        }

        private JPanel buildDetails() {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBackground(new Color(52, 58, 64));
            panel.setPreferredSize(new Dimension(280, 0));

            JPanel body = new JPanel();
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.setBackground(new Color(52, 58, 64));
            body.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

            JLabel head = new JLabel("Item Details");
            head.setFont(new Font("Courier", Font.BOLD, 16));
            head.setForeground(Color.WHITE);
            head.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(head);
            body.add(Box.createVerticalStrut(10));

            piecePriceHint = new JLabel("Select an item to begin");
            piecePriceHint.setFont(new Font("Courier", Font.PLAIN, 13));
            piecePriceHint.setForeground(new Color(220, 220, 220));
            piecePriceHint.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(piecePriceHint);
            body.add(Box.createVerticalStrut(14));

            body.add(label("Received by"));
            unitCombo = new JComboBox<>(new String[]{"Pieces", "Cartons"});
            unitCombo.setFont(new Font("Courier", Font.PLAIN, 13));
            unitCombo.setEnabled(false);
            unitCombo.addActionListener(e -> updateTotal());
            body.add(unitCombo);
            body.add(Box.createVerticalStrut(10));

            body.add(label(selectedUnitLabel()));
            qtyField = new JTextField("1");
            qtyField.setFont(new Font("Courier", Font.PLAIN, 15));
            qtyField.setHorizontalAlignment(SwingConstants.RIGHT);
            qtyField.addKeyListener(new KeyAdapter() {
                public void keyReleased(KeyEvent e) { updateTotal(); }
            });
            body.add(qtyField);
            body.add(Box.createVerticalStrut(10));

            body.add(label("Price per carton (KES)"));
            cartonPriceField = new JTextField("0.0");
            cartonPriceField.setFont(new Font("Courier", Font.PLAIN, 13));
            cartonPriceField.setHorizontalAlignment(SwingConstants.RIGHT);
            cartonPriceField.setEnabled(false);
            cartonPriceField.addKeyListener(new KeyAdapter() {
                public void keyReleased(KeyEvent e) { updateTotal(); }
            });
            body.add(cartonPriceField);

            body.add(Box.createVerticalStrut(18));

            totalLabel = new JLabel("Total: KES 0.00");
            totalLabel.setFont(new Font("Courier", Font.BOLD, 20));
            totalLabel.setForeground(Color.WHITE);
            totalLabel.setOpaque(true);
            totalLabel.setBackground(new Color(40, 167, 69));
            totalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            totalLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            body.add(totalLabel);
            body.add(Box.createVerticalStrut(18));

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            buttons.setOpaque(false);

            JButton cancel = new JButton("Cancel");
            cancel.setFont(new Font("Courier", Font.BOLD, 13));
            cancel.addActionListener(e -> dispose());
            buttons.add(cancel);

            JButton add = new JButton("Add to Purchase");
            add.setFont(new Font("Courier", Font.BOLD, 13));
            add.setBackground(new Color(40, 167, 69));
            add.setForeground(Color.WHITE);
            add.setFocusPainted(false);
            add.setBorderPainted(false);
            add.addActionListener(e -> onAddClick());
            buttons.add(add);

            body.add(buttons);

            panel.add(body, BorderLayout.CENTER);
            return panel;
        }

        private JLabel label(String text) {
            JLabel l = new JLabel(text);
            l.setFont(new Font("Courier", Font.PLAIN, 12));
            l.setForeground(new Color(200, 200, 200));
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            return l;
        }

        private String selectedUnitLabel() {
            if (unitCombo != null && unitCombo.getSelectedIndex() == 1) return "Number of cartons";
            if (selected != null) {
                boolean hasCarton = selected.getWholeScales() != null && selected.getWholeScales().length > 0
                        && selected.getWholeScales()[0].getItems() != null && selected.getWholeScales()[0].getItems() > 0;
                return hasCarton ? "Number of pieces (carton available)" : "Quantity";
            }
            return "Quantity";
        }

        private void updateTotal() {
            if (selected == null) return;
            boolean carton = unitCombo != null && unitCombo.getSelectedIndex() == 1;
            double qty = parseSafe(qtyField.getText());
            double total;

            if (carton) {
                WholeScale ws = selected.getWholeScales() != null && selected.getWholeScales().length > 0
                        ? selected.getWholeScales()[0] : null;
                double unitsPerCarton = ws != null && ws.getItems() != null ? ws.getItems() : 1.0;
                double pricePerCarton = parseSafe(cartonPriceField.getText());
                total = qty * pricePerCarton;
            } else {
                double piecePrice = selected.getBuyingPrice() != null ? selected.getBuyingPrice() : 0.0;
                total = qty * piecePrice;
            }

            if (total < 0) total = 0;
            totalLabel.setText("Total: KES " + String.format("%.2f", total));
        }

        private double parseSafe(String s) {
            try {
                return MyUtils.getDouble(s);
            } catch (Exception e) {
                return 0.0;
            }
        }

        private void onAddClick() {
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "Pick an item from the list.", "Add Item",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            boolean carton = unitCombo.getSelectedIndex() == 1;
            double qty = parseSafe(qtyField.getText());
            if (qty <= 0) {
                JOptionPane.showMessageDialog(this, "Quantity must be greater than zero.", "Validation",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (carton) {
                WholeScale ws = selected.getWholeScales() != null && selected.getWholeScales().length > 0
                        ? selected.getWholeScales()[0] : null;
                double unitsPerCarton = ws != null && ws.getItems() != null ? ws.getItems() : 1.0;
                double pricePerCarton = parseSafe(cartonPriceField.getText());
                double pieceBuying = unitsPerCarton > 0 ? pricePerCarton / unitsPerCarton : 0.0;
                double totalUnits = Math.round(qty * unitsPerCarton * 100.0) / 100.0;
                double lineTotal = qty * pricePerCarton;
                if (onAdd != null) {
                    onAdd.apply(selected, totalUnits, round2(pieceBuying), round2(lineTotal),
                            String.format("%.0f carton(s) = %.0f pcs @ KES %.2f each", qty, totalUnits, pieceBuying));
                }
            } else {
                double piecePrice = selected.getBuyingPrice() != null ? selected.getBuyingPrice() : 0.0;
                double lineTotal = qty * piecePrice;
                if (onAdd != null) {
                    onAdd.apply(selected, qty, piecePrice, round2(lineTotal),
                            "Added " + selected.getName() + " x" + qty);
                }
            }
            dispose();
        }
    }

        private void savePurchase() {
            String supplier = supplierField.getText().trim();
            if (supplier.isEmpty() && itemsModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "Add at least one item.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            double subtotal = 0;
            List<PurchaseItem> pItems = new ArrayList<>();
            for (int i = 0; i < itemsModel.getRowCount(); i++) {
                Item item = (Item) itemsModel.getValueAt(i, 0);
                double qty = MyUtils.getDouble(itemsModel.getValueAt(i, 1).toString());
                double price = MyUtils.getDouble(itemsModel.getValueAt(i, 2).toString());
                PurchaseItem pi = PurchaseItem.builder().item(item).quantity(qty).unitPrice(price).total(qty * price).build();
                pItems.add(pi);
                subtotal += qty * price;

                if (price > 0 && (item.getBuyingPrice() == null || Math.abs(item.getBuyingPrice() - price) > 0.001)) {
                    item.setBuyingPrice(price);
                    org.PSmart.POS.Repositories.ItemRepository.update(item);
                }
            }

            StoreConfig sc = DatabaseUtils.getInstance().getStoreConfigRepository().getStoreConfig();
            double tax = 0;
            if (sc != null && sc.getUseTax() != null && sc.getUseTax() && sc.getTaxRate() != null) {
                if (sc.getTaxInclusiveInPrice() == null || !sc.getTaxInclusiveInPrice()) {
                    tax = subtotal * sc.getTaxRate() / 100.0;
                }
            }

            User user = null;
            try {
                String hql = "FROM User u WHERE u.deleted=false";
                user = DatabaseUtils.getInstance().getEmf().createEntityManager()
                        .createQuery(hql, User.class).getResultList().stream().findFirst().orElse(null);
            } catch (Exception ignored) {}

            double grandTotal = subtotal + tax;
            double paidNow = grandTotal;
            String paidText = paidField.getText().trim().replace(",", "");
            if (!supplier.isEmpty() && !paidText.isEmpty()) {
                paidNow = MyUtils.getDouble(paidText);
                if (paidNow < 0 || paidNow > grandTotal + 0.009) {
                    paidField.setBackground(new Color(255, 220, 220));
                    paidField.setToolTipText("Must be between 0 and the invoice total " + String.format("%,.2f", grandTotal));
                    AutoClosingNotification.sendNotification("Paid amount must be between 0 and " + String.format("%,.2f", grandTotal));
                    return;
                }
            }

            Purchase purchase = Purchase.builder()
                    .createdAt(System.currentTimeMillis())
                    .createdBy(user)
                    .supplierName(supplier.isEmpty() ? null : supplier)
                    .invoiceNumber(invoiceField.getText().trim().isEmpty() ? null : invoiceField.getText().trim())
                    .notes(notesArea.getText().trim().isEmpty() ? null : notesArea.getText().trim())
                    .subtotal(subtotal)
                    .taxAmount(tax)
                    .total(subtotal + tax)
                    .paidAmount(paidNow)
                    .status("COMPLETED")
                    .items(pItems)
                    .build();

            for (PurchaseItem pi : pItems) pi.setPurchase(purchase);

            DatabaseUtils.getInstance().getPurchaseRepository().save(purchase);
            if (addToStock.isSelected()) receiveIntoStock(purchase, pItems);
            AutoClosingNotification.sendNotification("Purchase saved - KES " + MyUtils.formatCurrency(String.valueOf(purchase.getTotal())));
            dispose();
            if (onSave != null) onSave.run();
        }

        /** Books the purchase as a stock delivery so on-hand goes up (same record the Stock Received page shows). */
        private void receiveIntoStock(Purchase purchase, List<PurchaseItem> lines) {
            jakarta.persistence.EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
            try {
                em.getTransaction().begin();
                List<StockFeedItem> feedItems = new ArrayList<>();
                for (PurchaseItem pi : lines) {
                    if (pi.getItem() == null || pi.getQuantity() == null || pi.getQuantity() <= 0) continue;
                    StockFeedItem fi = StockFeedItem.builder().item(em.getReference(Item.class, pi.getItem().getUid()))
                            .quantity(pi.getQuantity()).buyingPrice(pi.getUnitPrice()).build();
                    em.persist(fi);
                    feedItems.add(fi);
                }
                if (!feedItems.isEmpty()) {
                    StockFeed feed = StockFeed.builder().doneOn(System.currentTimeMillis())
                            .comment("Purchase " + (purchase.getInvoiceNumber() == null ? "" : purchase.getInvoiceNumber() + " ")
                                    + (purchase.getSupplierName() == null ? "" : "from " + purchase.getSupplierName()))
                            .stockFeedItems(feedItems).build();
                    em.persist(feed);
                }
                em.getTransaction().commit();
            } catch (Exception ex) {
                if (em.getTransaction().isActive()) em.getTransaction().rollback();
                AutoClosingNotification.sendNotification("Purchase saved, but stock was not updated: " + ex.getMessage());
            } finally {
                em.close();
            }
        }

        void setOnSave(Runnable r) { this.onSave = r; }
    }
}
