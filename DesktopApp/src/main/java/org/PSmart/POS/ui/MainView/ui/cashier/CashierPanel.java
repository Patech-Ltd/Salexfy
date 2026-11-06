package org.PSmart.POS.ui.MainView.ui.cashier;

import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.*;
import org.PSmart.POS.Repositories.CartRepository;
import org.PSmart.POS.Repositories.HeldOrderRepository;
import org.PSmart.POS.Repositories.ItemRepository;
import org.PSmart.POS.Utils.Components.GridSearch.ItemComponents.GridItemJpanel;
import org.PSmart.POS.Utils.Components.GridSearch.ItemComponents.GridtemsSearch;
import org.PSmart.POS.Utils.Components.ImageLabels.IconLabel;
import org.PSmart.POS.Utils.Components.PTextField;
import org.PSmart.POS.Utils.Components.Runnables.ItemSelectedRunnable;
import org.PSmart.POS.Utils.Components.listSearch.SearchItemListPrompt;
import org.PSmart.POS.Utils.IconsUtils;
import org.PSmart.POS.Utils.MyUtils;
import org.PSmart.POS.Utils.ReceiptPrinter;
import org.PSmart.POS.tests.AutoClosingNotification;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;

import static org.PSmart.POS.ui.MainView.ui.cashier.CashierTheme.*;

/**
 * Cashier. One page, three steps, no pop-ups:
 *   CART (scan / search / grid)  ->  PAY (in-page split payment)  ->  DONE (change + next sale).
 * Shortcuts: F2 scan, F4 pay / confirm, Esc back, Delete removes the selected line.
 */
public class CashierPanel extends JPanel {

    private static final int COL_NUM = 0, COL_ITEM = 1, COL_UNIT = 2, COL_QTY = 3, COL_PRICE = 4,
            COL_DISC = 5, COL_TOTAL = 6, COL_X = 7;

    private PTextField barcodeField;
    private DefaultTableModel cartTableModel;
    private JTable cartTable;

    private JLabel itemCountLabel;
    private JLabel subtotalValueLabel;
    private JLabel vatLabelName;
    private JLabel vatValueLabel;
    private JLabel discountValueLabel;
    private JLabel totalLabel;

    private JButton holdSaleBtn, recallSaleBtn, clearBtn, payBtn;
    private CashierTheme.Segmented viewToggle, unitToggle;
    private JButton customerBtn;
    private boolean wholesaleMode = false;
    private boolean useComponentMode;
    private boolean refreshing;

    private JPanel centerCards;          // CART | PAY | DONE
    private JPanel cartCards;            // EMPTY | TABLE | COMPONENT
    private JScrollPane tableCartScroll, componentCartScroll;
    private JPanel componentCartList;
    private JLabel heldCountLabel;
    private JPanel heldListPanel;
    private JPanel heldSection;
    private JPanel recentSalesPanel;
    private JLabel statusHint;

    private String step = "CART";
    private PaymentPage paymentPage;
    private Sale lastSale;
    private StoreConfig lastConfig;

    private Customer selectedCustomer;
    private final User loggedUser;
    private final org.PSmart.POS.ui.MainView.DashBoard dash;
    private final List<CartItem> cartItems = new ArrayList<>();

    // ---------------------------------------------------------------- checkout snapshot
    private double ckSubtotal, ckDiscount, ckVat, ckTotal, ckPointsUsable, ckPointsWorth;

    public CashierPanel(User loggedUser) {
        this(loggedUser, null);
    }

    public CashierPanel(User loggedUser, org.PSmart.POS.ui.MainView.DashBoard dash) {
        super();
        this.loggedUser = loggedUser;
        this.dash = dash;
        draw();
        installShortcuts();
        SwingUtilities.invokeLater(() -> {
            loadPersistedCart();
            refreshRecentSales();
            barcodeField.grabFocus();
        });
    }

    private StoreConfig getStoreConfig() {
        StoreConfig cfg = DatabaseUtils.getInstance().getStoreConfigRepository().getStoreConfig();
        return cfg != null ? cfg : StoreConfig.defaultConfig();
    }

    private void toast(String msg) {
        AutoClosingNotification.sendNotification(msg);
    }

    // ================================================================== persistence of the open cart

    private void persistCart() {
        try {
            CartRepository repo = DatabaseUtils.getInstance().getCartRepository();
            List<org.PSmart.POS.Entities.CartLine> lines = new ArrayList<>();
            for (CartItem ci : cartItems) {
                lines.add(org.PSmart.POS.Entities.CartLine.builder()
                        .item(ci.item).quantity(ci.quantity).unitPrice(ci.unitPrice)
                        .buyingPrice(ci.buyingPrice).discount(ci.discount)
                        .updatedAt(System.currentTimeMillis()).build());
            }
            repo.replaceCart(lines);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void loadPersistedCart() {
        try {
            CartRepository repo = DatabaseUtils.getInstance().getCartRepository();
            for (org.PSmart.POS.Entities.CartLine l : repo.findActiveCart()) {
                if (l.getItem() == null) continue;
                CartItem ci = new CartItem();
                ci.item = l.getItem();
                ci.quantity = l.getQuantity() != null ? l.getQuantity() : 1.0;
                ci.unitPrice = l.getUnitPrice() != null ? l.getUnitPrice() : l.getItem().getSellingPrice();
                ci.buyingPrice = l.getBuyingPrice() != null ? l.getBuyingPrice()
                        : (l.getItem().getBuyingPrice() != null ? l.getItem().getBuyingPrice() : 0.0);
                ci.discount = l.getDiscount() != null ? l.getDiscount() : 0.0;
                ci.unitName = "Retail (1 pc)";
                ci.unitsPerPackage = 1.0;
                cartItems.add(ci);
            }
            refreshCartTable();
            if (!cartItems.isEmpty()) toast("Restored your pending cart (" + cartItems.size() + " lines)");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // ================================================================== layout

    private void draw() {
        setLayout(new BorderLayout());
        setBackground(PAGE);

        centerCards = new JPanel(new CardLayout());
        centerCards.setBackground(Color.WHITE);
        centerCards.add(buildCartHost(), "CART");

        add(buildTopBar(), BorderLayout.NORTH);
        add(centerCards, BorderLayout.CENTER);
        add(buildRightPanel(), BorderLayout.EAST);
        add(buildBottomBar(), BorderLayout.SOUTH);
        refreshCustomerChip();
    }

    private void installShortcuts() {
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();
        im.put(KeyStroke.getKeyStroke("F2"), "scan");
        am.put("scan", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if ("CART".equals(step)) barcodeField.grabFocus();
            }
        });
        im.put(KeyStroke.getKeyStroke("F4"), "pay");
        am.put("pay", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if ("CART".equals(step)) startCheckout();
                else if ("PAY".equals(step) && paymentPage != null) paymentPage.confirm();
            }
        });
        im.put(KeyStroke.getKeyStroke("ESCAPE"), "back");
        am.put("back", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if ("PAY".equals(step) && paymentPage != null) paymentPage.back();
            }
        });
    }

    // ---------------------------------------------------------------- top bar

    private JPanel buildTopBar() {
        JPanel topBar = new JPanel(new BorderLayout(10, 0));
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)));
        topBar.setPreferredSize(new Dimension(0, 74));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        barcodeField = new PTextField(SLATE, new Color(0, 0, 200), "Scan barcode (F2) or press the magnifier to search",
                IconsUtils.getIcon("search.png"));
        barcodeField.setPreferredSize(new Dimension(520, 60));
        barcodeField.getField().setFont(new Font("Courier", Font.BOLD, 22));
        barcodeField.getField().addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) handleBarcodeScan();
            }
        });
        left.add(barcodeField);
        barcodeField.iconAction(this::openItemSearch);

        IconLabel gridBtn = new IconLabel(null, 30, 30);
        gridBtn.setImage(IconsUtils.getIcon("grid.png"));
        gridBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        gridBtn.setToolTipText("Browse items grid");
        gridBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { openItemGrid(); }
        });
        left.add(gridBtn);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        right.setOpaque(false);

        right.add(label("View", 12, true, MUTED));
        viewToggle = new CashierTheme.Segmented(new String[]{"Table", "Cards"}, 0, i -> {
            useComponentMode = i == 1;
            updateCartView();
        });
        viewToggle.setPreferredSize(new Dimension(130, 34));
        right.add(viewToggle);

        right.add(label("Unit", 12, true, MUTED));
        unitToggle = new CashierTheme.Segmented(new String[]{"Retail", "Wholesale"}, 0, i -> wholesaleMode = i == 1);
        unitToggle.setPreferredSize(new Dimension(190, 34));
        right.add(unitToggle);

        customerBtn = button("Walk-in Customer", BLUE, 300, 38, 14);
        customerBtn.setToolTipText("Choose a customer (needed for credit and loyalty)");
        customerBtn.addActionListener(e -> selectCustomer());
        right.add(customerBtn);

        topBar.add(left, BorderLayout.CENTER);
        topBar.add(right, BorderLayout.EAST);
        return topBar;
    }

    private void refreshCustomerChip() {
        if (customerBtn == null) return;
        if (selectedCustomer == null) {
            customerBtn.setText("Walk-in Customer");
            return;
        }
        double bal = selectedCustomer.getCurrentBalance() != null ? selectedCustomer.getCurrentBalance() : 0.0;
        StringBuilder sb = new StringBuilder(selectedCustomer.getFullname());
        if (bal > 0.005) sb.append("  (owes ").append(String.format("%,.0f", bal)).append(")");
        if (selectedCustomer.getLoyaltyPoints() != null && selectedCustomer.getLoyaltyPoints() >= 1
                && Boolean.TRUE.equals(getStoreConfig().getLoyaltyEnabled())) {
            sb.append("  *").append(String.format("%.0f", selectedCustomer.getLoyaltyPoints())).append("pts");
        }
        customerBtn.setText(sb.toString());
    }

    private void selectCustomer() {
        if (!"CART".equals(step)) return;
        new org.PSmart.POS.Utils.Components.GridSearch.CustomerComponents.CustomerGridSearch(chosen -> {
            selectedCustomer = chosen;
            refreshCustomerChip();
            toast(chosen == null ? "Sale as Walk-in" : "Customer: " + chosen.getFullname());
        });
    }

    // ---------------------------------------------------------------- cart host (empty / table / cards)

    private JPanel buildCartHost() {
        buildCartArea();
        buildComponentCart();

        cartCards = new JPanel(new CardLayout());
        cartCards.setBackground(Color.WHITE);
        cartCards.add(buildEmptyState(), "EMPTY");
        cartCards.add(tableCartScroll, "TABLE");
        cartCards.add(componentCartScroll, "COMPONENT");
        updateCartView();
        return cartCards;
    }

    private JPanel buildEmptyState() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        JPanel c = new JPanel();
        c.setOpaque(false);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));

        JLabel t = label("Start a sale", 28, true, NAVY);
        t.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel s = label("Scan a barcode, search for an item, or browse the grid.", 15, false, MUTED);
        s.setAlignmentX(Component.CENTER_ALIGNMENT);
        c.add(t);
        c.add(Box.createVerticalStrut(6));
        c.add(s);
        c.add(Box.createVerticalStrut(22));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        row.setOpaque(false);
        JButton scan = button("Scan barcode  (F2)", BLUE, 230, 48, 15);
        scan.addActionListener(e -> barcodeField.grabFocus());
        JButton search = button("Search items", BLUE, 200, 48, 15);
        search.addActionListener(e -> openItemSearch());
        JButton grid = button("Browse grid", BLUE, 200, 48, 15);
        grid.addActionListener(e -> openItemGrid());
        row.add(scan);
        row.add(search);
        row.add(grid);
        c.add(row);
        p.add(c);
        return p;
    }

    private void updateCartView() {
        if (cartCards == null) return;
        CardLayout cl = (CardLayout) cartCards.getLayout();
        if (cartItems.isEmpty()) cl.show(cartCards, "EMPTY");
        else cl.show(cartCards, useComponentMode ? "COMPONENT" : "TABLE");
        if (useComponentMode) renderComponentCart();
    }

    // ---------------------------------------------------------------- cart table

    private JScrollPane buildCartArea() {
        String[] columns = {"#", "Item", "Unit", "Qty", "Price", "Discount", "Total", ""};
        cartTableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) {
                return column == COL_UNIT || column == COL_QTY || column == COL_DISC || column == COL_X;
            }
        };

        cartTable = new JTable(cartTableModel);
        cartTable.setRowHeight(46);
        cartTable.setFont(new Font("Courier", Font.PLAIN, 15));
        cartTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cartTable.setGridColor(new Color(240, 240, 240));
        cartTable.setBackground(Color.WHITE);
        cartTable.setSelectionBackground(new Color(222, 234, 250));
        cartTable.setSelectionForeground(Color.BLACK);
        cartTable.setIntercellSpacing(new Dimension(1, 1));

        JTableHeader header = cartTable.getTableHeader();
        header.setFont(new Font("Courier", Font.BOLD, 14));
        header.setBackground(SLATE);
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 38));
        header.setReorderingAllowed(false);

        int[] widths = {40, 280, 160, 130, 110, 100, 120, 44};
        for (int i = 0; i < widths.length; i++) cartTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(JLabel.CENTER);
        DefaultTableCellRenderer left = striped(JLabel.LEFT, false);
        DefaultTableCellRenderer right = striped(JLabel.RIGHT, false);
        DefaultTableCellRenderer rightBold = striped(JLabel.RIGHT, true);
        cartTable.getColumnModel().getColumn(COL_NUM).setCellRenderer(center);
        cartTable.getColumnModel().getColumn(COL_ITEM).setCellRenderer(striped(JLabel.LEFT, true));
        cartTable.getColumnModel().getColumn(COL_PRICE).setCellRenderer(right);
        cartTable.getColumnModel().getColumn(COL_DISC).setCellRenderer(right);
        cartTable.getColumnModel().getColumn(COL_TOTAL).setCellRenderer(rightBold);

        cartTable.getColumnModel().getColumn(COL_X).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                JButton b = new JButton("x");
                b.setForeground(RED);
                b.setFont(new Font("Courier", Font.BOLD, 16));
                b.setBorderPainted(false);
                b.setContentAreaFilled(false);
                b.setFocusPainted(false);
                b.setToolTipText("Remove line");
                return b;
            }
        });
        cartTable.getColumnModel().getColumn(COL_X).setCellEditor(new DefaultCellEditor(new JCheckBox()) {
            @Override public Component getTableCellEditorComponent(JTable t, Object v, boolean s, int row, int c) {
                JButton b = new JButton("x");
                b.setForeground(RED);
                b.setFont(new Font("Courier", Font.BOLD, 16));
                b.setBorderPainted(false);
                b.setContentAreaFilled(false);
                b.setFocusPainted(false);
                b.addActionListener(e -> { fireEditingStopped(); removeCartItem(row); });
                return b;
            }
        });

        cartTable.getColumnModel().getColumn(COL_UNIT).setCellRenderer(new UnitCellRenderer());
        cartTable.getColumnModel().getColumn(COL_UNIT).setCellEditor(new UnitCellEditor());
        cartTable.getColumnModel().getColumn(COL_QTY).setCellRenderer(new QuantityCellRenderer());
        cartTable.getColumnModel().getColumn(COL_QTY).setCellEditor(new QuantityCellEditor());

        JTextField discField = new JTextField();
        discField.setHorizontalAlignment(JTextField.RIGHT);
        discField.setFont(new Font("Courier", Font.PLAIN, 15));
        org.PSmart.POS.Utils.Components.Inputs.decimal(discField);
        cartTable.getColumnModel().getColumn(COL_DISC).setCellEditor(new DefaultCellEditor(discField));
        cartTableModel.addTableModelListener(e -> {
            if (refreshing || e.getColumn() != COL_DISC || e.getFirstRow() < 0) return;
            final int row = e.getFirstRow();
            final String text = String.valueOf(cartTableModel.getValueAt(row, COL_DISC));
            SwingUtilities.invokeLater(() -> updateItemDiscount(row, text));
        });

        cartTable.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("DELETE"), "removeLine");
        cartTable.getActionMap().put("removeLine", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (cartTable.isEditing()) return;
                int r = cartTable.getSelectedRow();
                if (r >= 0) removeCartItem(r);
            }
        });

        JScrollPane sp = new JScrollPane(cartTable);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Color.WHITE);
        tableCartScroll = sp;
        return sp;
    }

    private DefaultTableCellRenderer striped(int align, boolean bold) {
        DefaultTableCellRenderer r = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean f, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, f, row, col);
                if (!sel) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : ROW_ALT);
                    c.setForeground(new Color(40, 40, 40));
                }
                c.setFont(new Font("Courier", bold ? Font.BOLD : Font.PLAIN, 15));
                ((JLabel) c).setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return c;
            }
        };
        r.setHorizontalAlignment(align);
        return r;
    }

    private class UnitCellRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean sel, boolean focus, int row, int column) {
            JLabel l = new JLabel(value == null ? "Retail" : value.toString());
            l.setFont(new Font("Courier", Font.PLAIN, 13));
            l.setHorizontalAlignment(JLabel.CENTER);
            l.setBackground(sel ? new Color(222, 234, 250) : row % 2 == 0 ? Color.WHITE : ROW_ALT);
            l.setOpaque(true);
            l.setForeground(new Color(40, 40, 40));
            return l;
        }
    }

    @SuppressWarnings("unchecked")
    private class UnitCellEditor extends DefaultCellEditor {
        private int editingRow;

        UnitCellEditor() {
            super(new JComboBox<String>());
            setClickCountToStart(1);
            ((JComboBox<String>) getComponent()).setFont(new Font("Courier", Font.PLAIN, 13));
        }

        @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean sel, int row, int column) {
            editingRow = row;
            JComboBox<String> combo = (JComboBox<String>) getComponent();
            combo.removeAllItems();
            if (row >= 0 && row < cartItems.size()) {
                for (UnitOption u : availableUnits(cartItems.get(row).item)) combo.addItem(u.label);
                if (value != null) combo.setSelectedItem(value.toString());
            }
            return combo;
        }

        @Override public boolean stopCellEditing() {
            JComboBox<String> combo = (JComboBox<String>) getComponent();
            Object sel = combo.getSelectedItem();
            boolean ok = super.stopCellEditing();
            if (sel != null) {
                final String label = sel.toString();
                final int row = editingRow;
                SwingUtilities.invokeLater(() -> setCartUnit(row, label));
            }
            return ok;
        }
    }

    private class QuantityCellRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean f, int row, int col) {
            JPanel p = new JPanel(new BorderLayout(2, 0));
            p.setBackground(sel ? new Color(222, 234, 250) : row % 2 == 0 ? Color.WHITE : ROW_ALT);
            p.add(flatBtn("-"), BorderLayout.WEST);
            JLabel v = new JLabel(value == null ? "1" : value.toString(), JLabel.CENTER);
            v.setFont(new Font("Courier", Font.BOLD, 15));
            p.add(v, BorderLayout.CENTER);
            p.add(flatBtn("+"), BorderLayout.EAST);
            return p;
        }
    }

    private JButton flatBtn(String t) {
        JButton b = new JButton(t);
        b.setFont(new Font("Courier", Font.BOLD, 16));
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setForeground(NAVY);
        b.setMargin(new Insets(0, 6, 0, 6));
        return b;
    }

    private class QuantityCellEditor extends DefaultCellEditor {
        private final JPanel bar;
        private final JTextField field = new JTextField();
        private int editingRow;

        QuantityCellEditor() {
            super(new JTextField());
            setClickCountToStart(1);
            field.setHorizontalAlignment(JTextField.CENTER);
            field.setFont(new Font("Courier", Font.BOLD, 15));
            org.PSmart.POS.Utils.Components.Inputs.decimal(field);
            bar = new JPanel(new BorderLayout(2, 0));
            bar.setBackground(Color.WHITE);
            JButton minus = qtyBtn("-");
            JButton plus = qtyBtn("+");
            minus.addActionListener(e -> bump(-1));
            plus.addActionListener(e -> bump(1));
            bar.add(minus, BorderLayout.WEST);
            bar.add(field, BorderLayout.CENTER);
            bar.add(plus, BorderLayout.EAST);
        }

        private void bump(int d) {
            double cur = MyUtils.getDouble(field.getText());
            double next = Math.max(1, cur + d);
            field.setText(String.format("%.0f", next));
            final int row = editingRow;
            SwingUtilities.invokeLater(() -> updateItemQuantity(row, next));
        }

        private JButton qtyBtn(String label) {
            JButton b = MyUtils.createButton(label, SLATE);
            b.setForeground(Color.WHITE);
            b.setFont(new Font("Courier", Font.BOLD, 16));
            b.setPreferredSize(new Dimension(34, 34));
            return b;
        }

        @Override public Component getTableCellEditorComponent(JTable t, Object value, boolean sel, int row, int col) {
            editingRow = row;
            field.setText(value == null ? "1" : value.toString());
            return bar;
        }

        @Override public Object getCellEditorValue() { return field.getText().trim(); }

        @Override public boolean stopCellEditing() {
            double q = MyUtils.getDouble(field.getText());
            final int row = editingRow;
            boolean ok = super.stopCellEditing();
            if (q > 0) SwingUtilities.invokeLater(() -> updateItemQuantity(row, q));
            return ok;
        }
    }

    private void updateItemQuantity(int row, double qty) {
        if (row < 0 || row >= cartItems.size()) return;
        if (qty <= 0) qty = 1;
        cartItems.get(row).quantity = qty;
        refreshCartTable();
        persistCart();
    }

    private void updateItemDiscount(int row, String text) {
        if (row < 0 || row >= cartItems.size()) return;
        CartItem ci = cartItems.get(row);
        double d = MyUtils.getDouble(text.replace("KES", "").replace(",", "").trim());
        double gross = ci.unitPrice * ci.quantity;
        if (d < 0) d = 0;
        if (d > gross) {
            d = gross;
            toast("Discount cannot be more than the line total");
        }
        ci.discount = d;
        refreshCartTable();
        persistCart();
    }

    // ---------------------------------------------------------------- cards view

    private void buildComponentCart() {
        componentCartList = new JPanel();
        componentCartList.setLayout(new BoxLayout(componentCartList, BoxLayout.Y_AXIS));
        componentCartList.setBackground(Color.WHITE);
        componentCartList.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        componentCartScroll = new JScrollPane(componentCartList);
        componentCartScroll.setBorder(BorderFactory.createEmptyBorder());
        componentCartScroll.getViewport().setBackground(Color.WHITE);
        componentCartScroll.getVerticalScrollBar().setUnitIncrement(14);
    }

    private void renderComponentCart() {
        if (componentCartList == null) return;
        componentCartList.removeAll();
        for (int i = 0; i < cartItems.size(); i++) {
            componentCartList.add(new CashierCartLine(cartItems.get(i)));
            componentCartList.add(Box.createVerticalStrut(8));
        }
        componentCartList.revalidate();
        componentCartList.repaint();
    }

    // ---------------------------------------------------------------- right panel

    private JPanel buildRightPanel() {
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setPreferredSize(new Dimension(340, 0));
        rightPanel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER));

        JPanel summary = new JPanel();
        summary.setLayout(new BoxLayout(summary, BoxLayout.Y_AXIS));
        summary.setBackground(Color.WHITE);
        summary.setBorder(BorderFactory.createEmptyBorder(16, 20, 12, 20));

        JLabel title = label("ORDER SUMMARY", 13, true, MUTED);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        summary.add(title);
        summary.add(Box.createVerticalStrut(10));

        itemCountLabel = addSummaryRow(summary, "Items", "0", false);
        subtotalValueLabel = addSummaryRow(summary, "Subtotal", "0.00", true);
        String taxName = getStoreConfig().getTaxName() != null ? getStoreConfig().getTaxName() : "Tax";
        Double rate = getStoreConfig().getTaxRate();
        vatValueLabel = addSummaryRow(summary, taxName + (rate == null ? "" : " " + Math.round(rate) + "%"), "0.00", true);
        discountValueLabel = addSummaryRow(summary, "Discount", "0.00", true);

        summary.add(Box.createVerticalStrut(10));
        totalLabel = new JLabel("KES 0.00", SwingConstants.CENTER);
        totalLabel.setFont(new Font("Courier", Font.BOLD, 30));
        totalLabel.setForeground(Color.WHITE);
        totalLabel.setOpaque(true);
        totalLabel.setBackground(NAVY);
        totalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        totalLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        totalLabel.setPreferredSize(new Dimension(0, 64));
        JLabel totalCaption = label("TOTAL", 12, true, MUTED);
        totalCaption.setAlignmentX(Component.LEFT_ALIGNMENT);
        summary.add(totalCaption);
        summary.add(Box.createVerticalStrut(4));
        summary.add(totalLabel);
        rightPanel.add(summary);

        JPanel actions = new JPanel();
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        actions.setBackground(Color.WHITE);
        actions.setBorder(BorderFactory.createEmptyBorder(4, 20, 14, 20));

        payBtn = wide("PAY NOW  (F4)", GREEN, 60, 20);
        payBtn.addActionListener(e -> startCheckout());
        actions.add(payBtn);
        actions.add(Box.createVerticalStrut(10));

        JPanel two = new JPanel(new GridLayout(1, 2, 8, 0));
        two.setOpaque(false);
        two.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        two.setAlignmentX(Component.LEFT_ALIGNMENT);
        holdSaleBtn = outline("Hold", SLATE, 40, 14);
        holdSaleBtn.setToolTipText("Park this sale and serve someone else");
        holdSaleBtn.addActionListener(e -> holdSale());
        recallSaleBtn = outline("Recall", SLATE, 40, 14);
        recallSaleBtn.setToolTipText("Bring back a held sale");
        recallSaleBtn.addActionListener(e -> recallSale());
        two.add(holdSaleBtn);
        two.add(recallSaleBtn);
        actions.add(two);
        actions.add(Box.createVerticalStrut(8));

        clearBtn = outline("Clear cart", RED, 36, 13);
        clearBtn.addActionListener(e -> clearCartTwoStep());
        actions.add(clearBtn);
        rightPanel.add(actions);

        heldSection = buildHeldSection();
        rightPanel.add(heldSection);
        refreshHeldList();
        return rightPanel;
    }

    private JLabel addSummaryRow(JPanel parent, String labelText, String valueText, boolean money) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lbl = label(labelText, 14, false, MUTED);
        JLabel val = label(money ? "KES " + valueText : valueText, 15, true, SLATE);
        val.setHorizontalAlignment(SwingConstants.RIGHT);
        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.CENTER);
        parent.add(row);
        parent.add(Box.createVerticalStrut(4));
        return val;
    }

    private boolean clearArmed;

    private void clearCartTwoStep() {
        if (cartItems.isEmpty()) {
            toast("Cart is already empty");
            return;
        }
        if (!clearArmed) {
            clearArmed = true;
            clearBtn.setText("Tap again to clear");
            javax.swing.Timer t = new javax.swing.Timer(3000, e -> { clearArmed = false; clearBtn.setText("Clear cart"); });
            t.setRepeats(false);
            t.start();
            return;
        }
        clearArmed = false;
        clearBtn.setText("Clear cart");
        clearCart();
        toast("Cart cleared");
    }

    // ---------------------------------------------------------------- held orders

    private JPanel buildHeldSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBackground(Color.WHITE);
        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel t = label("HELD SALES", 13, true, MUTED);
        heldCountLabel = new JLabel("0", SwingConstants.CENTER);
        heldCountLabel.setFont(new Font("Courier", Font.BOLD, 13));
        heldCountLabel.setForeground(Color.WHITE);
        heldCountLabel.setOpaque(true);
        heldCountLabel.setBackground(NAVY);
        heldCountLabel.setPreferredSize(new Dimension(30, 24));
        head.add(t, BorderLayout.CENTER);
        head.add(heldCountLabel, BorderLayout.EAST);
        head.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        section.add(head);
        section.add(Box.createVerticalStrut(8));

        heldListPanel = new JPanel();
        heldListPanel.setLayout(new BoxLayout(heldListPanel, BoxLayout.Y_AXIS));
        heldListPanel.setBackground(Color.WHITE);
        JScrollPane scroll = new JScrollPane(heldListPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setPreferredSize(new Dimension(0, 260));
        section.add(scroll);
        return section;
    }

    private void refreshHeldList() {
        heldListPanel.removeAll();
        try {
            List<org.PSmart.POS.Repositories.HeldQueries.Held> held = org.PSmart.POS.Repositories.HeldQueries.list();
            heldCountLabel.setText(String.valueOf(held.size()));
            if (held.isEmpty()) {
                JLabel empty = label("No held sales. Use Hold to park a sale.", 12, false, MUTED);
                empty.setAlignmentX(Component.LEFT_ALIGNMENT);
                heldListPanel.add(empty);
            } else {
                for (org.PSmart.POS.Repositories.HeldQueries.Held h : held) {
                    heldListPanel.add(buildHeldCard(h));
                    heldListPanel.add(Box.createVerticalStrut(6));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            heldCountLabel.setText("0");
        }
        heldListPanel.revalidate();
        heldListPanel.repaint();
    }

    private JPanel buildHeldCard(org.PSmart.POS.Repositories.HeldQueries.Held h) {
        JPanel card = new JPanel(new BorderLayout(6, 0));
        card.setBackground(ROW_ALT);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(6, 8, 6, 4)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setToolTipText("Click to bring this sale back to the cart");

        String time = new java.text.SimpleDateFormat("dd-MMM HH:mm").format(new Date(h.createdAt));
        String caption = h.note != null && !h.note.isEmpty() ? h.note : h.customerName;

        JLabel cap = label(caption, 13, true, SLATE);
        JLabel amt = label("KES " + money(h.total), 13, true, NAVY);
        JPanel top = new JPanel(new BorderLayout(6, 0));
        top.setOpaque(false);
        top.add(cap, BorderLayout.CENTER);
        top.add(amt, BorderLayout.EAST);

        JLabel sub = label(time + "  -  " + h.summary, 11, false, MUTED);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(top);
        body.add(Box.createVerticalStrut(2));
        body.add(sub);

        JButton discard = new JButton("x");
        discard.setFont(new Font("Courier", Font.BOLD, 13));
        discard.setForeground(RED);
        discard.setContentAreaFilled(false);
        discard.setBorderPainted(false);
        discard.setFocusPainted(false);
        discard.setMargin(new Insets(0, 4, 0, 4));
        discard.setToolTipText("Discard this held sale (tap twice)");
        final boolean[] armed = {false};
        discard.addActionListener(e -> {
            if (!armed[0]) {
                armed[0] = true;
                discard.setText("Sure?");
                javax.swing.Timer t = new javax.swing.Timer(3000, ev -> { armed[0] = false; discard.setText("x"); });
                t.setRepeats(false);
                t.start();
                return;
            }
            try {
                DatabaseUtils.getInstance().getHeldOrderRepository().discardHeld(h.uid);
                toast("Held sale discarded");
                refreshHeldList();
            } catch (Exception ex) {
                toast("Could not discard: " + ex.getMessage());
            }
        });

        card.add(body, BorderLayout.CENTER);
        card.add(discard, BorderLayout.EAST);
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { recallHeld(h); }
        });
        return card;
    }

    private void recallHeld(org.PSmart.POS.Repositories.HeldQueries.Held order) {
        if (!"CART".equals(step)) return;
        if (!cartItems.isEmpty()) {
            toast("Finish, hold or clear the current cart first");
            return;
        }
        try {
            for (org.PSmart.POS.Repositories.HeldQueries.Line l : org.PSmart.POS.Repositories.HeldQueries.lines(order.uid)) {
                Item item = org.PSmart.POS.Repositories.HeldQueries.item(l.itemId);
                if (item != null) addItemToCart(item, l.quantity);
            }
            selectedCustomer = org.PSmart.POS.Repositories.HeldQueries.customer(order.customerId);
            refreshCustomerChip();
            DatabaseUtils.getInstance().getHeldOrderRepository().discardHeld(order.uid);
        } catch (Exception ex) {
            ex.printStackTrace();
            toast("Could not bring the held sale back: " + ex.getMessage());
            return;
        }
        refreshHeldList();
        toast("Held sale brought back to the cart");
    }

    private void holdSale() {
        if (!"CART".equals(step)) return;
        if (cartItems.isEmpty()) {
            toast("Cart is empty - nothing to hold");
            return;
        }
        HeldOrderRepository repo = DatabaseUtils.getInstance().getHeldOrderRepository();
        long current = repo.heldCount();
        if (current >= HeldOrderRepository.MAX_HELD) {
            toast("Held sales are full (" + HeldOrderRepository.MAX_HELD + "). Bring one back or discard it first.");
            return;
        }
        StoreConfig config = getStoreConfig();
        double subtotal = 0, totalDiscount = 0;
        for (CartItem ci : cartItems) {
            subtotal += ci.unitPrice * ci.quantity;
            totalDiscount += ci.discount;
        }
        double vat = config.calculateTax(subtotal, totalDiscount);
        double total = Math.max(0, config.calculateTotal(subtotal, totalDiscount));

        HeldOrder order = HeldOrder.builder().createdAt(System.currentTimeMillis()).customer(selectedCustomer)
                .subtotal(subtotal).discount(totalDiscount).vatAmount(vat).total(total).build();
        List<HeldOrderItem> items = new ArrayList<>();
        for (CartItem ci : cartItems) {
            items.add(HeldOrderItem.builder().order(order).item(ci.item).quantity(ci.quantity)
                    .unitPrice(ci.unitPrice).buyingPrice(ci.buyingPrice).discount(ci.discount).build());
        }
        order.setItems(items);
        try {
            repo.saveHeld(order);
            clearCart();
            refreshHeldList();
            toast("Sale held (" + (current + 1) + " of " + HeldOrderRepository.MAX_HELD + ")");
        } catch (Exception ex) {
            toast("Could not hold sale: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void recallSale() {
        if (!"CART".equals(step)) return;
        List<org.PSmart.POS.Repositories.HeldQueries.Held> held = org.PSmart.POS.Repositories.HeldQueries.list();
        if (held.isEmpty()) {
            toast("No held sales to bring back");
        } else if (held.size() == 1) {
            recallHeld(held.get(0));
        } else {
            toast("Choose a held sale from the list on the right");
            heldSection.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(3, 0, 0, 0, BLUE), BorderFactory.createEmptyBorder(10, 16, 12, 16)));
            javax.swing.Timer t = new javax.swing.Timer(1800, e -> heldSection.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER), BorderFactory.createEmptyBorder(12, 16, 12, 16))));
            t.setRepeats(false);
            t.start();
        }
    }

    // ---------------------------------------------------------------- bottom bar

    private JPanel buildBottomBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(SLATE);
        bar.setPreferredSize(new Dimension(0, 46));
        statusHint = new JLabel("   F2 Scan     F4 Pay     Esc Back     Delete Remove line");
        statusHint.setFont(new Font("Courier", Font.PLAIN, 13));
        statusHint.setForeground(new Color(200, 210, 220));
        bar.add(statusHint, BorderLayout.WEST);
        recentSalesPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        recentSalesPanel.setOpaque(false);
        bar.add(recentSalesPanel, BorderLayout.EAST);
        return bar;
    }

    private void refreshRecentSales() {
        try {
            List<org.PSmart.POS.Repositories.SaleQueries.Row> recent = org.PSmart.POS.Repositories.SaleQueries.rows(new org.PSmart.POS.Repositories.SaleQueries.Filter(), 0, 4);
            recentSalesPanel.removeAll();
            JLabel tag = new JLabel("Recent sales:");
            tag.setFont(new Font("Courier", Font.BOLD, 12));
            tag.setForeground(new Color(200, 210, 220));
            recentSalesPanel.add(tag);
            if (recent == null || recent.isEmpty()) {
                JLabel none = new JLabel("none yet");
                none.setFont(new Font("Courier", Font.PLAIN, 12));
                none.setForeground(new Color(150, 160, 170));
                recentSalesPanel.add(none);
            } else {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm");
                for (org.PSmart.POS.Repositories.SaleQueries.Row s : recent) {
                    String time = sdf.format(new Date(s.createdAt));
                    JLabel chip = new JLabel(time + "  KES " + String.format("%,.0f", s.total));
                    chip.setFont(new Font("Courier", Font.PLAIN, 12));
                    chip.setForeground(Color.WHITE);
                    chip.setBackground(new Color(73, 80, 87));
                    chip.setOpaque(true);
                    chip.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
                    recentSalesPanel.add(chip);
                }
            }
            JButton viewAll = button("View all sales", BLUE, 140, 28, 12);
            viewAll.addActionListener(e -> openAllSales());
            recentSalesPanel.add(viewAll);
            recentSalesPanel.revalidate();
            recentSalesPanel.repaint();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void openAllSales() {
        if (dash != null) dash.openTab(2);
    }

    // ================================================================== cart operations

    private void handleBarcodeScan() {
        if (!"CART".equals(step)) return;
        String code = barcodeField.getText().trim();
        if (code.isEmpty()) return;
        Item item = ItemRepository.findByBarCode(code);
        if (item != null) {
            addItemToCart(item, 1.0);
            barcodeField.setText("");
        } else {
            toast("No item with barcode " + code);
        }
        barcodeField.grabFocus();
    }

    public void addItemToCart(Item item, Double qty) {
        for (CartItem ci : cartItems) {
            if (ci.item.getUid().equals(item.getUid())) {
                ci.quantity += qty;
                refreshCartTable();
                persistCart();
                return;
            }
        }
        UnitOption def = defaultUnitFor(item);
        CartItem ci = new CartItem();
        ci.item = item;
        ci.quantity = qty;
        ci.unitName = def.label;
        ci.unitPrice = def.pricePerPackage;
        ci.unitsPerPackage = def.unitsPerPackage;
        ci.buyingPrice = item.getBuyingPrice() != null ? item.getBuyingPrice() : 0.0;
        ci.discount = 0.0;
        cartItems.add(ci);
        refreshCartTable();
        persistCart();
    }

    private void removeCartItem(int row) {
        if (row >= 0 && row < cartItems.size()) {
            cartItems.remove(row);
            refreshCartTable();
            persistCart();
        }
    }

    private void clearCart() {
        cartItems.clear();
        selectedCustomer = null;
        refreshCustomerChip();
        refreshCartTable();
        persistCart();
    }

    private static class UnitOption {
        final String label;
        final double pricePerPackage;
        final double unitsPerPackage;
        UnitOption(String label, double pricePerPackage, double unitsPerPackage) {
            this.label = label;
            this.pricePerPackage = pricePerPackage;
            this.unitsPerPackage = unitsPerPackage;
        }
        public String toString() { return label; }
    }

    private List<UnitOption> availableUnits(Item item) {
        List<UnitOption> units = new ArrayList<>();
        double retail = item.getSellingPrice() != null ? item.getSellingPrice() : 0.0;
        units.add(new UnitOption("Retail (1 pc)", retail, 1.0));
        if (item.getWholeScales() != null) {
            for (WholeScale w : item.getWholeScales()) {
                if (w == null || w.isDeleted()) continue;
                String n = w.getName() == null || w.getName().isEmpty() ? "Wholesale" : w.getName();
                double per = w.getItems() != null && w.getItems() > 0 ? w.getItems() : 1.0;
                units.add(new UnitOption(n + " (" + (int) per + " pcs)",
                        w.getUnitPrice() != null ? w.getUnitPrice() : retail * per, per));
            }
        }
        if (item.getSubScales() != null) {
            for (SubScale s : item.getSubScales()) {
                if (s == null || s.isDeleted()) continue;
                String n = s.getName() == null || s.getName().isEmpty() ? "Sub-Unit" : s.getName();
                double div = s.getDivisions() != null && s.getDivisions() > 0 ? s.getDivisions() : 1.0;
                units.add(new UnitOption(n + " (1/" + (int) div + ")",
                        s.getUnitPrice() != null ? s.getUnitPrice() : retail / div, 1.0 / div));
            }
        }
        return units;
    }

    private UnitOption defaultUnitFor(Item item) {
        List<UnitOption> units = availableUnits(item);
        if (wholesaleMode && units.size() > 1 && item.getWholeScales() != null && item.getWholeScales().length > 0) {
            return units.get(1);
        }
        return units.get(0);
    }

    private void setCartUnit(int row, String unitLabel) {
        if (row < 0 || row >= cartItems.size()) return;
        CartItem ci = cartItems.get(row);
        for (UnitOption u : availableUnits(ci.item)) {
            if (u.label.equals(unitLabel)) {
                double totalRetailPieces = ci.quantity * ci.unitsPerPackage;
                ci.unitName = u.label;
                ci.unitPrice = u.pricePerPackage;
                ci.unitsPerPackage = u.unitsPerPackage;
                ci.quantity = Math.max(1, totalRetailPieces / u.unitsPerPackage);
                ci.buyingPrice = ci.item.getBuyingPrice() != null ? ci.item.getBuyingPrice() : 0.0;
                refreshCartTable();
                persistCart();
                return;
            }
        }
    }

    private void refreshCartTable() {
        refreshing = true;
        try {
            cartTableModel.setRowCount(0);
            BigDecimal subtotal = BigDecimal.ZERO, totalDiscount = BigDecimal.ZERO;
            double itemCount = 0;
            for (int i = 0; i < cartItems.size(); i++) {
                CartItem ci = cartItems.get(i);
                BigDecimal gross = BigDecimal.valueOf(ci.unitPrice).multiply(BigDecimal.valueOf(ci.quantity));
                BigDecimal lineTotal = gross.subtract(BigDecimal.valueOf(ci.discount));
                if (lineTotal.compareTo(BigDecimal.ZERO) < 0) lineTotal = BigDecimal.ZERO;
                cartTableModel.addRow(new Object[]{i + 1, ci.item.getName(),
                        ci.unitName == null ? "Retail" : ci.unitName,
                        String.format("%.0f", ci.quantity), String.format("%,.2f", ci.unitPrice),
                        ci.discount > 0.005 ? String.format("%,.2f", ci.discount) : "",
                        String.format("%,.2f", lineTotal.doubleValue()), "x"});
                subtotal = subtotal.add(gross);
                totalDiscount = totalDiscount.add(BigDecimal.valueOf(ci.discount));
                itemCount += ci.quantity;
            }
            StoreConfig config = getStoreConfig();
            double vat = config.calculateTax(subtotal.doubleValue(), totalDiscount.doubleValue());
            double total = Math.max(0, config.calculateTotal(subtotal.doubleValue(), totalDiscount.doubleValue()));
            itemCountLabel.setText(String.valueOf((int) itemCount));
            subtotalValueLabel.setText("KES " + money(subtotal.doubleValue()));
            vatValueLabel.setText("KES " + money(vat));
            discountValueLabel.setText("KES " + money(totalDiscount.doubleValue()));
            totalLabel.setText("KES " + money(total));
            payBtn.setEnabled(!cartItems.isEmpty());
        } finally {
            refreshing = false;
        }
        updateCartView();
    }

    private void openItemGrid() {
        if (!"CART".equals(step)) return;
        new org.PSmart.POS.Utils.Components.ItemPicker("Browse items", "Add to sale", false, this::addPicks);
    }

    private void openItemSearch() {
        if (!"CART".equals(step)) return;
        new org.PSmart.POS.Utils.Components.ItemPicker("Search items", "Add to sale", true, this::addPicks);
    }

    private void addPicks(List<org.PSmart.POS.Utils.Components.ItemPicker.Pick> picks) {
        int n = 0;
        for (org.PSmart.POS.Utils.Components.ItemPicker.Pick p : picks) {
            if (p.quantity > 0 && p.item != null) { addItemToCart(p.item, p.quantity); n++; }
        }
        if (n > 0) toast(n == 1 ? "Added: " + picks.get(0).item.getName() : "Added " + n + " items");
    }

    // ================================================================== checkout flow

    private void showStep(String s) {
        step = s;
        ((CardLayout) centerCards.getLayout()).show(centerCards, s);
        boolean cart = "CART".equals(s);
        payBtn.setEnabled(cart && !cartItems.isEmpty());
        holdSaleBtn.setEnabled(cart);
        recallSaleBtn.setEnabled(cart);
        clearBtn.setEnabled(cart);
        customerBtn.setEnabled(cart);
        viewToggle.setEnabled(cart);
        barcodeField.setEnabled(cart);
        if (cart) barcodeField.grabFocus();
    }

    private void removeCard(String name) {
        for (Component c : centerCards.getComponents()) {
            if (c.getName() != null && c.getName().equals(name)) centerCards.remove(c);
        }
    }

    private void startCheckout() {
        if (!"CART".equals(step)) return;
        if (cartItems.isEmpty()) {
            toast("Add items to the cart first");
            return;
        }
        if (cartTable.isEditing()) cartTable.getCellEditor().stopCellEditing();

        BigDecimal subtotal = BigDecimal.ZERO, totalDiscount = BigDecimal.ZERO;
        for (CartItem ci : cartItems) {
            subtotal = subtotal.add(BigDecimal.valueOf(ci.unitPrice).multiply(BigDecimal.valueOf(ci.quantity)));
            totalDiscount = totalDiscount.add(BigDecimal.valueOf(ci.discount));
        }
        StoreConfig config = getStoreConfig();
        ckSubtotal = subtotal.doubleValue();
        ckDiscount = totalDiscount.doubleValue();
        ckVat = config.calculateTax(ckSubtotal, ckDiscount);
        ckTotal = Math.max(0, config.calculateTotal(ckSubtotal, ckDiscount));
        if (!stockAllowsSale(config)) return;

        ckPointsUsable = 0;
        ckPointsWorth = 0;
        if (Boolean.TRUE.equals(config.getLoyaltyEnabled()) && selectedCustomer != null && ckTotal > 0) {
            double avail = selectedCustomer.getLoyaltyPoints() == null ? 0 : selectedCustomer.getLoyaltyPoints();
            double pv = config.getLoyaltyPointValue() == null ? 1.0 : config.getLoyaltyPointValue();
            if (avail >= 1 && pv > 0) {
                ckPointsUsable = Math.min(Math.floor(avail), Math.ceil(ckTotal / pv));
                ckPointsWorth = Math.min(ckTotal, Math.round(ckPointsUsable * pv * 100.0) / 100.0);
            }
        }

        double creditAvailable = Double.MAX_VALUE;
        if (selectedCustomer != null && selectedCustomer.getCreditLimit() != null && selectedCustomer.getCreditLimit() > 0) {
            double bal = selectedCustomer.getCurrentBalance() == null ? 0 : selectedCustomer.getCurrentBalance();
            creditAvailable = Math.max(0, selectedCustomer.getCreditLimit() - bal);
        }

        removeCard("PAY");
        paymentPage = new PaymentPage(ckTotal, selectedCustomer, ckPointsUsable, ckPointsWorth, creditAvailable,
                this::backToCart, this::finalizeSale);
        paymentPage.setName("PAY");
        centerCards.add(paymentPage, "PAY");
        lastConfig = config;
        showStep("PAY");
        paymentPage.focusFirst();
    }

    private void backToCart() {
        showStep("CART");
    }

    private void finalizeSale(PaymentPage.Result r) {
        StoreConfig config = lastConfig != null ? lastConfig : getStoreConfig();
        double total = ckTotal;
        double discount = ckDiscount;
        double pointsUsed = 0;
        if (r.pointsRedeemed) {
            pointsUsed = ckPointsUsable;
            total = Math.max(0, total - ckPointsWorth);
            discount += ckPointsWorth;
        }
        double paidNow = r.cash + r.mpesa + r.card;
        double change = Math.max(0, (r.cash + r.mpesa + r.card + r.credit) - total);
        change = Math.min(change, r.cash);
        boolean onCredit = r.credit > 0.005;

        StringBuilder method = new StringBuilder();
        if (r.cash > 0) method.append("CASH");
        if (r.mpesa > 0) method.append(method.length() > 0 ? "+" : "").append("MPESA");
        if (r.card > 0) method.append(method.length() > 0 ? "+" : "").append("CARD");
        if (onCredit) method.append(method.length() > 0 ? "+" : "").append("CREDIT");
        if (method.length() == 0) method.append("CASH");

        try {
            Sale sale = Sale.builder()
                    .createdAt(System.currentTimeMillis()).cashier(loggedUser)
                    .subtotal(ckSubtotal).discount(discount).vatAmount(ckVat).total(total)
                    .amountPaid(paidNow).change(change).paymentMethod(method.toString())
                    .status(onCredit ? "CREDIT" : "COMPLETED").customer(selectedCustomer)
                    .pointsEarned(selectedCustomer != null && Boolean.TRUE.equals(config.getLoyaltyEnabled())
                            ? Math.floor(Math.max(0, total - r.credit)
                            * (config.getLoyaltyPointsPerMoney() == null ? 0.01 : config.getLoyaltyPointsPerMoney())) : 0.0)
                    .pointsRedeemed(pointsUsed).build();
            List<SaleItem> saleItems = new ArrayList<>();
            for (CartItem ci : cartItems) {
                saleItems.add(SaleItem.builder().item(ci.item).quantity(ci.quantity).unitPrice(ci.unitPrice)
                        .buyingPrice(ci.buyingPrice).discount(ci.discount)
                        .total(ci.unitPrice * ci.quantity - ci.discount).sale(sale).build());
            }
            sale.setItems(saleItems);
            DatabaseUtils.getInstance().getSaleRepository().save(sale);

            if (onCredit && selectedCustomer != null) {
                DatabaseUtils.getInstance().getCreditRepository().recordCreditSale(sale, selectedCustomer, loggedUser);
            }
            long earned = 0;
            double rate = config.getLoyaltyPointsPerMoney() == null ? 0.01 : config.getLoyaltyPointsPerMoney();
            if (selectedCustomer != null && Boolean.TRUE.equals(config.getLoyaltyEnabled())) {
                earned = (long) Math.floor(Math.max(0, total - r.credit) * rate);
            }
            applyLoyalty(config, pointsUsed, total - r.credit);

            if (!Boolean.FALSE.equals(config.getAutoPrintReceipt())) {
                try {
                    ReceiptPrinter.printReceipt(sale, config);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    toast("Sale saved, but the receipt could not be printed");
                }
            }

            lastSale = sale;
            String paidBy = method.toString().replace("MPESA", "M-Pesa").replace("CASH", "Cash").replace("CARD", "Card")
                    .replace("CREDIT", "Credit").replace("+", " + ");
            String customerName = selectedCustomer == null ? "" : selectedCustomer.getFullname();
            String receipt = sale.getReceiptNumber() == null ? "-" : sale.getReceiptNumber();
            final double fTotal = total, fChange = change, fCredit = r.credit;
            final long fEarned = earned;

            cartItems.clear();
            selectedCustomer = null;
            refreshCustomerChip();
            refreshCartTable();
            persistCart();

            removeCard("DONE");
            SaleCompletePage done = new SaleCompletePage(receipt, fTotal, paidBy, fChange, fCredit, customerName, fEarned,
                    this::newSale,
                    () -> { try { if (lastSale != null) ReceiptPrinter.printReceipt(lastSale, config); } catch (Exception ex) { toast("Could not print receipt"); } },
                    () -> { if (lastSale != null) ReceiptPrinter.previewReceipt(this, lastSale, config); },
                    () -> { if (lastSale != null) ReceiptPrinter.previewInvoice(this, lastSale, config); },
                    this::openAllSales);
            done.setName("DONE");
            centerCards.add(done, "DONE");
            showStep("DONE");
            payBtn.setEnabled(false);
            refreshRecentSales();
        } catch (Exception ex) {
            ex.printStackTrace();
            toast("Could not save the sale: " + ex.getMessage());
            if (paymentPage != null) paymentPage.unlock();
        }
    }

    private void newSale() {
        showStep("CART");
        refreshHeldList();
        barcodeField.grabFocus();
    }

    // ---------------------------------------------------------------- stock + loyalty

    private boolean stockAllowsSale(StoreConfig config) {
        try {
            Map<UUID, Double> need = new LinkedHashMap<>();
            Map<UUID, String> names = new HashMap<>();
            for (CartItem ci : cartItems) {
                if (ci.item == null) continue;
                need.merge(ci.item.getUid(), ci.quantity * (ci.unitsPerPackage <= 0 ? 1 : ci.unitsPerPackage), Double::sum);
                names.put(ci.item.getUid(), ci.item.getName());
            }
            jakarta.persistence.EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
            Map<UUID, Double> onHand;
            try {
                onHand = org.PSmart.POS.Utils.StockService.onHandAll(em);
            } finally {
                em.close();
            }
            List<String> short_ = new ArrayList<>();
            for (Map.Entry<UUID, Double> e : need.entrySet()) {
                double have = onHand.getOrDefault(e.getKey(), 0.0);
                if (e.getValue() > have + 1e-9) {
                    short_.add(names.get(e.getKey()) + " (need " + String.format("%.0f", e.getValue())
                            + ", have " + String.format("%.0f", have) + ")");
                }
            }
            if (short_.isEmpty()) return true;
            String list = short_.size() > 2 ? short_.get(0) + "; " + short_.get(1) + "; +" + (short_.size() - 2) + " more"
                    : String.join("; ", short_);
            if (Boolean.TRUE.equals(config.getBlockOutOfStockSales())) {
                toast("Not enough stock: " + list);
                return false;
            }
            toast("Warning - above stock on hand: " + list);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return true;   // never block a sale because the stock check itself failed
        }
    }

    private void applyLoyalty(StoreConfig config, double pointsUsed, double paidAmount) {
        if (selectedCustomer == null || !Boolean.TRUE.equals(config.getLoyaltyEnabled())) return;
        try {
            double rate = config.getLoyaltyPointsPerMoney() == null ? 0.01 : config.getLoyaltyPointsPerMoney();
            double earned = Math.floor(Math.max(0, paidAmount) * rate);
            Customer fresh = DatabaseUtils.getInstance().getCustomerRepository()
                    .findById(selectedCustomer.getUid()).orElse(selectedCustomer);
            double pts = fresh.getLoyaltyPoints() == null ? 0 : fresh.getLoyaltyPoints();
            fresh.setLoyaltyPoints(Math.max(0, pts - pointsUsed) + earned);
            fresh.setTotalSpent((fresh.getTotalSpent() == null ? 0 : fresh.getTotalSpent()) + Math.max(0, paidAmount));
            DatabaseUtils.getInstance().getCustomerRepository().update(fresh);
            selectedCustomer = fresh;
        } catch (Exception ex) {
            ex.printStackTrace();   // the sale is already saved; a loyalty failure must not undo it
        }
    }

    // ================================================================== models

    private static class CartItem {
        Item item;
        double quantity;
        double unitPrice;
        double buyingPrice;
        double discount;
        String unitName;
        double unitsPerPackage = 1;
    }

    /** Card-style cart line used by the "Cards" view. */
    private class CashierCartLine extends JPanel {
        private final JComboBox<String> unitCombo = new JComboBox<>();
        private final JLabel qtyLabel = new JLabel("1", SwingConstants.CENTER);
        private final JLabel lineTotalLabel = new JLabel("", SwingConstants.RIGHT);
        private final JLabel unitPriceLabel = new JLabel("", SwingConstants.LEFT);
        private final CartItem ci;

        CashierCartLine(CartItem ci) {
            this.ci = ci;
            setLayout(new BorderLayout(10, 4));
            setBackground(ROW_ALT);
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));
            setPreferredSize(new Dimension(0, 82));

            JLabel name = label(ci.item.getName(), 15, true, SLATE);
            for (UnitOption u : availableUnits(ci.item)) unitCombo.addItem(u.label);
            unitCombo.setSelectedItem(ci.unitName);
            unitCombo.setFont(new Font("Courier", Font.PLAIN, 12));
            unitCombo.addActionListener(e -> {
                Object sel = unitCombo.getSelectedItem();
                if (sel != null && !sel.toString().equals(ci.unitName)) setCartUnit(cartItems.indexOf(ci), sel.toString());
            });

            JButton minus = stepBtn("-");
            JButton plus = stepBtn("+");
            minus.addActionListener(e -> updateItemQuantity(cartItems.indexOf(ci), Math.max(1, ci.quantity - 1)));
            plus.addActionListener(e -> updateItemQuantity(cartItems.indexOf(ci), ci.quantity + 1));
            qtyLabel.setFont(new Font("Courier", Font.BOLD, 16));
            qtyLabel.setPreferredSize(new Dimension(44, 26));

            JButton remove = outline("Remove", RED, 28, 12);
            remove.setPreferredSize(new Dimension(90, 28));
            remove.addActionListener(e -> removeCartItem(cartItems.indexOf(ci)));

            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            controls.setOpaque(false);
            controls.add(unitCombo);
            controls.add(Box.createHorizontalStrut(10));
            controls.add(minus);
            controls.add(qtyLabel);
            controls.add(plus);
            controls.add(Box.createHorizontalStrut(14));
            controls.add(unitPriceLabel);

            JPanel right = new JPanel(new BorderLayout(0, 4));
            right.setOpaque(false);
            right.add(lineTotalLabel, BorderLayout.NORTH);
            right.add(remove, BorderLayout.SOUTH);

            add(name, BorderLayout.NORTH);
            add(controls, BorderLayout.CENTER);
            add(right, BorderLayout.EAST);

            double lineTotal = Math.max(0, ci.unitPrice * ci.quantity - ci.discount);
            qtyLabel.setText(String.format("%.0f", ci.quantity));
            unitPriceLabel.setText("@ " + money(ci.unitPrice));
            unitPriceLabel.setFont(new Font("Courier", Font.PLAIN, 13));
            lineTotalLabel.setText("KES " + money(lineTotal));
            lineTotalLabel.setFont(new Font("Courier", Font.BOLD, 16));
            lineTotalLabel.setForeground(NAVY);
        }

        private JButton stepBtn(String t) {
            JButton b = CashierTheme.button(t, SLATE, 34, 28, 16);
            return b;
        }
    }
}
