package org.PSmart.POS.ui.MainView.ui.cashier;

import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.*;
import org.PSmart.POS.Repositories.CartRepository;
import org.PSmart.POS.Repositories.CustomerRepository;
import org.PSmart.POS.Repositories.HeldOrderRepository;
import org.PSmart.POS.Repositories.ItemRepository;
import org.PSmart.POS.Utils.Components.GridSearch.ItemComponents.GridItemJpanel;
import org.PSmart.POS.Utils.Components.GridSearch.ItemComponents.GridItemSelectedRunnable;
import org.PSmart.POS.Utils.Components.GridSearch.ItemComponents.GridtemsSearch;
import org.PSmart.POS.Utils.Components.ImageLabels.IconLabel;
import org.PSmart.POS.Utils.Components.PTextField;
import org.PSmart.POS.Utils.Components.Runnables.ItemSelectedRunnable;
import org.PSmart.POS.Utils.Components.cart.CartLine;
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
import java.math.RoundingMode;
import java.util.*;
import java.util.List;

public class CashierPanel extends JPanel {

    private PTextField barcodeField;
    private PTextField searchField;

    private DefaultTableModel cartTableModel;
    private JTable cartTable;

    private JLabel itemCountLabel;
    private JLabel subtotalValueLabel;
    private JLabel vatValueLabel;
    private JLabel discountValueLabel;
    private JLabel totalLabel;

    private JButton holdSaleBtn;
    private JButton recallSaleBtn;
    private JButton clearBtn;
    private JButton payBtn;
    private JButton modeBtn;
    private JButton unitModeBtn;
    private JButton selectedCustomerButton;
    private boolean wholesaleMode = false;
    private JPanel cartPanel;
    private JScrollPane tableCartScroll;
    private JScrollPane componentCartScroll;
    private JLabel heldCountLabel;
    private JPanel heldListPanel;
    private JPanel recentSalesPanel;
    private JLabel recentSalesCountLabel;

    private Customer selectedCustomer;

    private final User loggedUser;
    private final org.PSmart.POS.ui.MainView.DashBoard dash;

    private List<CartItem> cartItems = new ArrayList<>();
    private Map<UUID, StockFeedItem> heldSales = new LinkedHashMap<>();

    public CashierPanel(User loggedUser) {
        this(loggedUser, null);
    }

    public CashierPanel(User loggedUser, org.PSmart.POS.ui.MainView.DashBoard dash) {
        super();
        this.loggedUser = loggedUser;
        this.dash = dash;
        draw();
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

    /**
     * Writes the current cart items to the database so an in-progress sale
     * survives an accidental close, crash or program restart.
     */
    private void persistCart() {
        try {
            CartRepository repo = DatabaseUtils.getInstance().getCartRepository();
            List<org.PSmart.POS.Entities.CartLine> lines = new ArrayList<>();
            for (CartItem ci : cartItems) {
                org.PSmart.POS.Entities.CartLine line = org.PSmart.POS.Entities.CartLine.builder()
                        .item(ci.item)
                        .quantity(ci.quantity)
                        .unitPrice(ci.unitPrice)
                        .buyingPrice(ci.buyingPrice)
                        .discount(ci.discount)
                        .updatedAt(System.currentTimeMillis())
                        .build();
                lines.add(line);
            }
            repo.replaceCart(lines);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** Restores any saved cart lines from the previous session. */
    private void loadPersistedCart() {
        try {
            CartRepository repo = DatabaseUtils.getInstance().getCartRepository();
            List<org.PSmart.POS.Entities.CartLine> lines = repo.findActiveCart();
            for (org.PSmart.POS.Entities.CartLine l : lines) {
                if (l.getItem() == null) continue;
                CartItem ci = new CartItem();
                ci.item = l.getItem();
                ci.quantity = l.getQuantity() != null ? l.getQuantity() : 1.0;
                ci.unitPrice = l.getUnitPrice() != null ? l.getUnitPrice() : l.getItem().getSellingPrice();
                ci.buyingPrice = l.getBuyingPrice() != null ? l.getBuyingPrice()
                        : (l.getItem().getBuyingPrice() != null ? l.getItem().getBuyingPrice() : 0.0);
                ci.discount = l.getDiscount() != null ? l.getDiscount() : 0.0;
                cartItems.add(ci);
            }
            refreshCartTable();
            if (!cartItems.isEmpty()) {
                AutoClosingNotification.sendNotification("Restored your pending cart (" + cartItems.size() + " lines)");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void draw() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 250));

        add(buildTopBar(), BorderLayout.NORTH);
        add(buildCartHost(), BorderLayout.CENTER);
        add(buildRightPanel(), BorderLayout.EAST);
        add(buildBottomBar(), BorderLayout.SOUTH);
    }

    private JPanel buildTopBar() {
        JPanel topBar = new JPanel(new BorderLayout(10, 0));
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)));
        topBar.setPreferredSize(new Dimension(0, 70));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftPanel.setOpaque(false);

        barcodeField = new PTextField(new Color(52, 58, 64), new Color(0, 0, 200), "Scan Barcode", IconsUtils.getIcon("search.png"));
        barcodeField.setPreferredSize(new Dimension(350, 60));
        barcodeField.getField().setFont(new Font("Courier", Font.BOLD, 22));
        barcodeField.getField().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleBarcodeScan();
                }
            }
        });
        leftPanel.add(barcodeField);
        barcodeField.iconAction(this::openItemSearch);

        IconLabel gridBtn = new IconLabel(null, 30, 30);
        gridBtn.setImage(IconsUtils.getIcon("grid.png"));
        gridBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        gridBtn.setToolTipText("Browse Items Grid");
        gridBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openItemGrid();
            }
        });
        leftPanel.add(gridBtn);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightPanel.setOpaque(false);

        modeBtn = new JButton("Mode: Table");
        modeBtn.setFont(new Font("Courier", Font.BOLD, 12));
        modeBtn.setBackground(new Color(108, 117, 125));
        modeBtn.setForeground(Color.WHITE);
        modeBtn.setFocusPainted(false);
        modeBtn.setBorderPainted(false);
        modeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        modeBtn.setPreferredSize(new Dimension(110, 38));
        modeBtn.addActionListener(e -> toggleMode());
        rightPanel.add(modeBtn);

        unitModeBtn = new JButton("Unit: Retail");
        unitModeBtn.setFont(new Font("Courier", Font.BOLD, 12));
        unitModeBtn.setBackground(new Color(23, 162, 184));
        unitModeBtn.setForeground(Color.WHITE);
        unitModeBtn.setFocusPainted(false);
        unitModeBtn.setBorderPainted(false);
        unitModeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        unitModeBtn.setPreferredSize(new Dimension(130, 38));
        unitModeBtn.addActionListener(e -> toggleUnitMode());
        rightPanel.add(unitModeBtn);

        JButton customerBtn = new JButton("Walk-in Customer");
        customerBtn.setFont(MyUtils.arialFont(14).deriveFont(Font.BOLD));
        customerBtn.setBackground(new Color(39, 144, 255));
        customerBtn.setForeground(Color.WHITE);
        customerBtn.setFocusPainted(false);
        customerBtn.setBorderPainted(false);
        customerBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        customerBtn.setPreferredSize(new Dimension(200, 38));
        customerBtn.setBorder(BorderFactory.createLineBorder(new Color(25, 118, 210), 1));
        customerBtn.addActionListener(e -> selectCustomer());
        selectedCustomerButton = customerBtn;
        rightPanel.add(customerBtn);

        topBar.add(leftPanel, BorderLayout.CENTER);
        topBar.add(rightPanel, BorderLayout.EAST);

        return topBar;
    }

    private JScrollPane buildCartArea() {
        String[] columns = {"#", "Item", "Unit", "Qty", "Price", "Total", ""};
        cartTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2 || column == 3 || column == 6;
            }
        };

        cartTable = new JTable(cartTableModel);
        cartTable.setRowHeight(42);
        cartTable.setFont(new Font("Courier", Font.PLAIN, 15));
        cartTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cartTable.setGridColor(new Color(240, 240, 240));
        cartTable.setBackground(Color.WHITE);
        cartTable.setShowGrid(true);
        cartTable.setIntercellSpacing(new Dimension(1, 1));

        JTableHeader header = cartTable.getTableHeader();
        header.setFont(new Font("Courier", Font.BOLD, 14));
        header.setBackground(new Color(52, 58, 64));
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 38));

        cartTable.getColumnModel().getColumn(0).setPreferredWidth(35);
        cartTable.getColumnModel().getColumn(1).setPreferredWidth(260);
        cartTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        cartTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        cartTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        cartTable.getColumnModel().getColumn(5).setPreferredWidth(110);
        cartTable.getColumnModel().getColumn(6).setPreferredWidth(40);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);

        DefaultTableCellRenderer stripedRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    boolean even = (row % 2 == 0);
                    c.setBackground(even ? Color.WHITE : new Color(248, 249, 252));
                    c.setForeground(new Color(60, 60, 60));
                }
                return c;
            }
        };
        stripedRenderer.setHorizontalAlignment(JLabel.LEFT);

        for (int i = 0; i < columns.length; i++) {
            if (i == 0) cartTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            else if (i == 6) cartTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            else cartTable.getColumnModel().getColumn(i).setCellRenderer(stripedRenderer);
        }

        cartTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JButton btn = new JButton("X");
                btn.setForeground(new Color(220, 53, 69));
                btn.setFont(new Font("Courier", Font.BOLD, 12));
                btn.setBorderPainted(false);
                btn.setContentAreaFilled(false);
                btn.setFocusPainted(false);
                return btn;
            }
        });

        cartTable.getColumnModel().getColumn(6).setCellEditor(new DefaultCellEditor(new JCheckBox()) {
            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                JButton btn = new JButton("X");
                btn.setForeground(new Color(220, 53, 69));
                btn.setFont(new Font("Courier", Font.BOLD, 12));
                btn.setBorderPainted(false);
                btn.setContentAreaFilled(false);
                btn.setFocusPainted(false);
                btn.addActionListener(e -> {
                    removeCartItem(row);
                    fireEditingStopped();
                });
                return btn;
            }
        });

        cartTable.getColumnModel().getColumn(2).setCellRenderer(new UnitCellRenderer());
        cartTable.getColumnModel().getColumn(2).setCellEditor(new UnitCellEditor());

        cartTable.getColumnModel().getColumn(3).setCellRenderer(new QuantityCellRenderer());
        cartTable.getColumnModel().getColumn(3).setCellEditor(new QuantityCellEditor());

        JScrollPane scrollPane = new JScrollPane(cartTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);

        tableCartScroll = scrollPane;
        return scrollPane;
    }

    /** Renderer for the unit column. */
    private class UnitCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel l = new JLabel(value == null ? "Retail" : value.toString());
            l.setFont(new Font("Courier", Font.PLAIN, 13));
            l.setHorizontalAlignment(JLabel.CENTER);
            l.setBackground(isSelected ? new Color(25, 118, 211) : row % 2 == 0 ? Color.WHITE : new Color(248, 249, 252));
            l.setOpaque(true);
            l.setForeground(isSelected ? Color.WHITE : new Color(40, 40, 40));
            l.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)));
            return l;
        }
    }

    /** Combo editor to change the sellable unit (retail/wholesale) for a cart line. */
    private class UnitCellEditor extends DefaultCellEditor {
        private int editingRow;

        UnitCellEditor() {
            super(new JComboBox<String>());
            setClickCountToStart(1);
            JComboBox<String> combo = (JComboBox<String>) getComponent();
            combo.setFont(new Font("Courier", Font.PLAIN, 13));
            combo.addActionListener(e -> {
                JComboBox<String> c = (JComboBox<String>) getCellEditorValue();
                // handled via editor value on stop
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            editingRow = row;
            JComboBox<String> combo = (JComboBox<String>) getComponent();
            combo.removeAllItems();
            if (row >= 0 && row < cartItems.size()) {
                Item item = cartItems.get(row).item;
                for (UnitOption u : availableUnits(item)) combo.addItem(u.label);
                Object cur = value;
                if (cur != null) combo.setSelectedItem(cur.toString());
            }
            return combo;
        }

        @Override
        public boolean stopCellEditing() {
            JComboBox<String> combo = (JComboBox<String>) getComponent();
            Object sel = combo.getSelectedItem();
            if (sel != null) setCartUnit(editingRow, sel.toString());
            return super.stopCellEditing();
        }
    }

    /** Renderer showing [ - qty + ] as a compact control. */
    private class QuantityCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return buildQtyBar(value == null ? "1" : value.toString(), false, row);
        }
    }

    /** Editor with minus / plus buttons to adjust cart quantity. */
    private class QuantityCellEditor extends DefaultCellEditor {
        private final JPanel bar;
        private final JTextField field;
        private int editingRow;

        QuantityCellEditor() {
            super(new JTextField());
            setClickCountToStart(1);
            field = new JTextField();
            field.setHorizontalAlignment(JTextField.CENTER);
            field.setFont(new Font("Courier", Font.BOLD, 14));
            bar = buildQtyPanel();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            editingRow = row;
            field.setText(value == null ? "1" : value.toString());
            return bar;
        }

        @Override
        public Object getCellEditorValue() {
            return field.getText().trim();
        }

        private JPanel buildQtyPanel() {
            JPanel p = new JPanel(new BorderLayout(2, 0));
            p.setBackground(Color.WHITE);

            JButton minus = qtyButton("-");
            minus.addActionListener(e -> {
                double cur = MyUtils.getDouble(field.getText());
                cur = Math.max(1, cur - 1);
                field.setText(String.format("%.0f", cur));
                updateItemQuantity(editingRow, cur);
            });

            JButton plus = qtyButton("+");
            plus.addActionListener(e -> {
                double cur = MyUtils.getDouble(field.getText());
                cur = Math.max(1, cur + 1);
                field.setText(String.format("%.0f", cur));
                updateItemQuantity(editingRow, cur);
            });

            p.add(minus, BorderLayout.WEST);
            p.add(field, BorderLayout.CENTER);
            p.add(plus, BorderLayout.EAST);
            return p;
        }

        private JButton qtyButton(String label) {
            JButton b = new JButton(label);
            b.setFont(new Font("Courier", Font.BOLD, 16));
            b.setFocusPainted(false);
            b.setBorderPainted(false);
            b.setBackground(new Color(52, 58, 64));
            b.setForeground(Color.WHITE);
            b.setPreferredSize(new Dimension(34, 34));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return b;
        }
    }

    private JPanel buildQtyBar(String value, boolean isSelected, int row) {
        JPanel p = new JPanel(new BorderLayout(2, 0));
        p.setBackground(isSelected ? new Color(25, 118, 211) : row % 2 == 0 ? Color.WHITE : new Color(248, 249, 252));

        JButton minus = new JButton("-");
        minus.setFont(new Font("Courier", Font.BOLD, 14));
        minus.setBorderPainted(false);
        minus.setContentAreaFilled(false);
        minus.setFocusPainted(false);
        minus.setForeground(new Color(52, 58, 64));
        minus.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        minus.addActionListener(e -> {
            double cur = MyUtils.getDouble(value);
            updateItemQuantity(row, Math.max(1, cur - 1));
        });

        JLabel val = new JLabel(value, JLabel.CENTER);
        val.setFont(new Font("Courier", Font.BOLD, 14));
        val.setForeground(isSelected ? Color.WHITE : new Color(40, 40, 40));

        JButton plus = new JButton("+");
        plus.setFont(new Font("Courier", Font.BOLD, 14));
        plus.setBorderPainted(false);
        plus.setContentAreaFilled(false);
        plus.setFocusPainted(false);
        plus.setForeground(new Color(52, 58, 64));
        plus.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        plus.addActionListener(e -> {
            double cur = MyUtils.getDouble(value);
            updateItemQuantity(row, Math.max(1, cur + 1));
        });

        p.add(minus, BorderLayout.WEST);
        p.add(val, BorderLayout.CENTER);
        p.add(plus, BorderLayout.EAST);
        return p;
    }

    private void updateItemQuantity(int row, double qty) {
        if (row < 0 || row >= cartItems.size()) return;
        if (qty <= 0) qty = 1;
        cartItems.get(row).quantity = qty;
        refreshCartTable();
        persistCart();
    }

    private JPanel buildCartHost() {
        buildCartArea();            // builds tableCartScroll
        buildComponentCart();       // builds componentCartScroll

        cartPanel = new JPanel(new CardLayout());
        cartPanel.setBackground(Color.WHITE);
        cartPanel.add(tableCartScroll, "TABLE");
        cartPanel.add(componentCartScroll, "COMPONENT");

        useComponentMode = false;
        setModeUI();
        return cartPanel;
    }

    private boolean useComponentMode;
    private JPanel componentCartList;

    private void toggleMode() {
        useComponentMode = !useComponentMode;
        setModeUI();
    }

    private void toggleUnitMode() {
        wholesaleMode = !wholesaleMode;
        if (wholesaleMode) {
            unitModeBtn.setText("Unit: Wholesale");
            unitModeBtn.setBackground(new Color(0, 123, 255));
        } else {
            unitModeBtn.setText("Unit: Retail");
            unitModeBtn.setBackground(new Color(23, 162, 184));
        }
    }

    private void setModeUI() {
        CardLayout cl = (CardLayout) cartPanel.getLayout();
        if (useComponentMode && modeBtn != null) {
            modeBtn.setText("Mode: Table");
            modeBtn.setBackground(new Color(0, 123, 255));
        } else if (modeBtn != null) {
            modeBtn.setText("Mode: Cart Lines");
            modeBtn.setBackground(new Color(108, 117, 125));
        }
        cl.show(cartPanel, useComponentMode ? "COMPONENT" : "TABLE");
        if (useComponentMode) renderComponentCart();
    }

    private void buildComponentCart() {
        componentCartList = new JPanel();
        componentCartList.setLayout(new BoxLayout(componentCartList, BoxLayout.Y_AXIS));
        componentCartList.setBackground(Color.WHITE);
        componentCartList.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        componentCartScroll = new JScrollPane(componentCartList);
        componentCartScroll.setBorder(BorderFactory.createEmptyBorder());
        componentCartScroll.getViewport().setBackground(Color.WHITE);
        componentCartScroll.getVerticalScrollBar().setUnitIncrement(14);
    }

    /** Render the cart as clean cashier cart-lines (unit selector + qty +/− + total). */
    private void renderComponentCart() {
        if (componentCartList == null) return;
        componentCartList.removeAll();
        if (cartItems.isEmpty()) {
            JLabel empty = new JLabel("Cart is empty - scan or search an item above.");
            empty.setFont(new Font("Courier", Font.PLAIN, 13));
            empty.setForeground(new Color(150, 150, 150));
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            componentCartList.add(empty);
        } else {
            for (int i = 0; i < cartItems.size(); i++) {
                componentCartList.add(new CashierCartLine(i, cartItems.get(i)));
                componentCartList.add(Box.createVerticalStrut(8));
            }
        }
        componentCartList.revalidate();
        componentCartList.repaint();
    }

    private JPanel buildRightPanel() {
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setPreferredSize(new Dimension(300, 0));
        rightPanel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(220, 220, 220)));

        JPanel summaryPanel = new JPanel();
        summaryPanel.setLayout(new BoxLayout(summaryPanel, BoxLayout.Y_AXIS));
        summaryPanel.setBackground(Color.WHITE);
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel summaryTitle = new JLabel("ORDER SUMMARY");
        summaryTitle.setFont(new Font("Courier", Font.BOLD, 14));
        summaryTitle.setForeground(new Color(108, 117, 125));
        summaryTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        summaryTitle.setHorizontalAlignment(SwingConstants.CENTER);
        summaryTitle.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(228, 230, 235)));
        summaryPanel.setBorder(BorderFactory.createCompoundBorder(
                summaryPanel.getBorder(),
                BorderFactory.createEmptyBorder(0, 0, 10, 0)));
        summaryPanel.add(summaryTitle);
        summaryPanel.add(Box.createVerticalStrut(12));

        itemCountLabel = new JLabel("Items: 0");
        itemCountLabel.setFont(new Font("Courier", Font.BOLD, 16));
        itemCountLabel.setForeground(new Color(100, 100, 100));
        itemCountLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        itemCountLabel.setHorizontalAlignment(SwingConstants.CENTER);
        summaryPanel.add(itemCountLabel);
        summaryPanel.add(Box.createVerticalStrut(15));

        subtotalValueLabel = addSummaryRow(summaryPanel, "Subtotal", "0.00");
        summaryPanel.add(Box.createVerticalStrut(6));

        String taxName = getStoreConfig().getTaxName() != null
                ? getStoreConfig().getTaxName() : "Tax";
        vatValueLabel = addSummaryRow(summaryPanel, taxName + " (" + Math.round(getStoreConfig().getTaxRate()) + "%)", "0.00");
        summaryPanel.add(Box.createVerticalStrut(6));

        discountValueLabel = addSummaryRow(summaryPanel, "Discount", "-0.00");
        discountValueLabel.setForeground(new Color(220, 53, 69));

        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        summaryPanel.add(Box.createVerticalStrut(12));
        summaryPanel.add(sep);
        summaryPanel.add(Box.createVerticalStrut(10));

        totalLabel = new JLabel("TOTAL: KES 0.00");
        totalLabel.setFont(new Font("Courier", Font.BOLD, 20));
        totalLabel.setForeground(Color.WHITE);
        totalLabel.setOpaque(true);
        totalLabel.setBackground(new Color(40, 167, 69));
        totalLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        totalLabel.setHorizontalAlignment(SwingConstants.CENTER);
        totalLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        totalLabel.setPreferredSize(new Dimension(0, 56));
        totalLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(33, 136, 56), 1),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)));
        summaryPanel.add(Box.createVerticalStrut(6));
        summaryPanel.add(totalLabel);

        rightPanel.add(summaryPanel);

        JSeparator sep2 = new JSeparator();
        sep2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        rightPanel.add(sep2);

        JPanel actionPanel = new JPanel();
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));
        actionPanel.setBackground(Color.WHITE);
        actionPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));

        holdSaleBtn = createActionButton("Hold Sale", new Color(255, 193, 7));
        holdSaleBtn.addActionListener(e -> holdSale());
        actionPanel.add(holdSaleBtn);
        actionPanel.add(Box.createVerticalStrut(8));

        recallSaleBtn = createActionButton("Recall Sale", new Color(23, 162, 184));
        recallSaleBtn.addActionListener(e -> recallSale());
        actionPanel.add(recallSaleBtn);
        actionPanel.add(Box.createVerticalStrut(8));

        clearBtn = createActionButton("Clear Cart", new Color(108, 117, 125));
        clearBtn.addActionListener(e -> clearCart());
        actionPanel.add(clearBtn);
        actionPanel.add(Box.createVerticalStrut(12));

        payBtn = createActionButton("PAY NOW", new Color(40, 167, 69));
        payBtn.setFont(new Font("Courier", Font.BOLD, 20));
        payBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
        payBtn.addActionListener(e -> processPayment());
        actionPanel.add(payBtn);

        rightPanel.add(actionPanel);

        JSeparator sep3 = new JSeparator();
        sep3.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        rightPanel.add(sep3);

        rightPanel.add(buildHeldSection());

        refreshHeldList();
        return rightPanel;
    }

    private JPanel buildHeldSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBackground(Color.WHITE);
        section.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel t = new JLabel("HELD ORDERS");
        t.setFont(new Font("Courier", Font.BOLD, 13));
        t.setForeground(new Color(108, 117, 125));
        heldCountLabel = new JLabel("0");
        heldCountLabel.setFont(new Font("Courier", Font.BOLD, 13));
        heldCountLabel.setForeground(Color.WHITE);
        heldCountLabel.setOpaque(true);
        heldCountLabel.setBackground(new Color(23, 162, 184));
        heldCountLabel.setHorizontalAlignment(SwingConstants.CENTER);
        heldCountLabel.setPreferredSize(new Dimension(30, 24));
        head.add(t, BorderLayout.CENTER);
        head.add(heldCountLabel, BorderLayout.EAST);
        head.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        section.add(head);
        section.add(Box.createVerticalStrut(8));

        heldListPanel = new JPanel();
        heldListPanel.setLayout(new BoxLayout(heldListPanel, BoxLayout.Y_AXIS));
        heldListPanel.setBackground(Color.WHITE);

        JScrollPane scroll = new JScrollPane(heldListPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        scroll.setPreferredSize(new Dimension(0, 240));
        section.add(scroll);

        return section;
    }

    private void refreshHeldList() {
        heldListPanel.removeAll();
        try {
            List<HeldOrder> held = DatabaseUtils.getInstance().getHeldOrderRepository().findAllHeld();
            heldCountLabel.setText(String.valueOf(held.size()));
            if (held.isEmpty()) {
                JLabel empty = new JLabel("No held orders");
                empty.setFont(new Font("Courier", Font.PLAIN, 12));
                empty.setForeground(new Color(150, 150, 150));
                empty.setAlignmentX(Component.LEFT_ALIGNMENT);
                heldListPanel.add(empty);
            } else {
                for (HeldOrder h : held) {
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

    private JPanel buildHeldCard(HeldOrder h) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(new Color(248, 249, 252));
        card.setBorder(BorderFactory.createLineBorder(new Color(228, 230, 235)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setFont(new Font("Courier", Font.PLAIN, 12));

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MMM HH:mm");
        String time = sdf.format(new java.util.Date(h.getCreatedAt()));
        String who = h.getCustomer() != null ? h.getCustomer().getFullname() : "Walk-in";
        String caption = h.getNote() != null && !h.getNote().isEmpty() ? h.getNote() : who + " • " + time;

        JLabel captionL = new JLabel(caption);
        captionL.setFont(new Font("Courier", Font.BOLD, 12));
        captionL.setForeground(new Color(52, 58, 64));

        double total = h.getTotal() != null ? h.getTotal() : 0;
        JLabel amountL = new JLabel("KES " + String.format("%.2f", total));
        amountL.setFont(new Font("Courier", Font.BOLD, 14));
        amountL.setForeground(new Color(40, 167, 69));

        JPanel top = new JPanel(new BorderLayout(6, 0));
        top.setOpaque(false);
        top.add(captionL, BorderLayout.CENTER);
        top.add(amountL, BorderLayout.EAST);

        // Item snapshot summary (e.g. "3 pcs Coca-Cola, 2 pcs Bread")
        String itemsSum = "";
        if (h.getItems() != null) {
            List<String> parts = new ArrayList<>();
            int count = 0;
            for (HeldOrderItem hi : h.getItems()) {
                if (count == 3) { parts.add("+ more"); break; }
                if (hi.getItem() != null) {
                    parts.add(String.format("%.0f x %s", hi.getQuantity() != null ? hi.getQuantity() : 0, hi.getItem().getName()));
                }
                count++;
            }
            itemsSum = String.join(", ", parts);
        }
        JLabel itemsL = new JLabel(itemsSum + (h.getItems() != null ? "  (" + h.getItems().size() + " line(s))" : ""));
        itemsL.setFont(new Font("Courier", Font.PLAIN, 11));
        itemsL.setForeground(new Color(108, 117, 125));

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.add(top);
        body.add(Box.createVerticalStrut(2));
        body.add(itemsL);

        JButton discard = new JButton("x");
        discard.setFont(new Font("Courier", Font.BOLD, 11));
        discard.setForeground(new Color(220, 53, 69));
        discard.setContentAreaFilled(false);
        discard.setBorderPainted(false);
        discard.setFocusPainted(false);
        discard.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        discard.setToolTipText("Discard this held order");
        discard.addActionListener(e -> discardHeld(h));

        card.add(body, BorderLayout.CENTER);
        card.add(discard, BorderLayout.EAST);
        card.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                recallHeld(h);
            }
        });
        return card;
    }

    private void discardHeld(HeldOrder h) {
        int opt = JOptionPane.showConfirmDialog(this,
                "Discard held order from " + (h.getCustomer() != null ? h.getCustomer().getFullname() : "Walk-in") + "?",
                "Discard", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt == JOptionPane.YES_OPTION) {
            try {
                DatabaseUtils.getInstance().getHeldOrderRepository().discardHeld(h.getUid());
                AutoClosingNotification.sendNotification("Held order discarded");
                refreshHeldList();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Could not discard: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void recallHeld(HeldOrder order) {
        if (order.getItems() == null) return;
        clearCart();
        for (HeldOrderItem hi : order.getItems()) {
            if (hi.getItem() == null) continue;
            CartItem ci = new CartItem();
            ci.item = hi.getItem();
            ci.quantity = hi.getQuantity() != null ? hi.getQuantity() : 1.0;
            ci.unitPrice = hi.getUnitPrice() != null ? hi.getUnitPrice() : hi.getItem().getSellingPrice();
            ci.buyingPrice = hi.getBuyingPrice() != null ? hi.getBuyingPrice()
                    : (hi.getItem().getBuyingPrice() != null ? hi.getItem().getBuyingPrice() : 0.0);
            ci.discount = hi.getDiscount() != null ? hi.getDiscount() : 0.0;
            addItemToCart(ci.item, ci.quantity);
        }
        selectedCustomer = order.getCustomer();
        try {
            DatabaseUtils.getInstance().getHeldOrderRepository().discardHeld(order.getUid());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        refreshHeldList();
        AutoClosingNotification.sendNotification("Sale recalled to cart");
    }

    private JLabel addSummaryRow(JPanel parent, String labelText, String valueText) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Courier", Font.PLAIN, 16));
        lbl.setForeground(new Color(100, 100, 100));

        JLabel val = new JLabel("KES " + valueText);
        val.setFont(new Font("Courier", Font.BOLD, 16));
        val.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.EAST);
        parent.add(row);

        return val;
    }

    private JPanel buildBottomBar() {
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(new Color(52, 58, 64));
        bottomBar.setPreferredSize(new Dimension(0, 50));

        JLabel statusLabel = new JLabel("  PSMART POS - Ready");
        statusLabel.setFont(new Font("Courier", Font.BOLD, 13));
        statusLabel.setForeground(Color.WHITE);
        bottomBar.add(statusLabel, BorderLayout.WEST);

        bottomBar.add(buildRecentSalesStrip(), BorderLayout.EAST);

        return bottomBar;
    }

    private JPanel buildRecentSalesStrip() {
        recentSalesPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        recentSalesPanel.setOpaque(false);
        recentSalesPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 12));

        JLabel tag = new JLabel("Recent Sales:");
        tag.setFont(new Font("Courier", Font.BOLD, 12));
        tag.setForeground(new Color(200, 210, 220));
        recentSalesPanel.add(tag);

        recentSalesCountLabel = new JLabel("0");
        recentSalesCountLabel.setFont(new Font("Courier", Font.BOLD, 14));
        recentSalesCountLabel.setForeground(new Color(80, 224, 150));
        recentSalesPanel.add(recentSalesCountLabel);

        JButton viewAll = new JButton("View All Sales");
        viewAll.setFont(new Font("Courier", Font.BOLD, 12));
        viewAll.setBackground(new Color(23, 162, 184));
        viewAll.setForeground(Color.WHITE);
        viewAll.setFocusPainted(false);
        viewAll.setBorderPainted(false);
        viewAll.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        viewAll.addActionListener(e -> openAllSales());
        recentSalesPanel.add(viewAll);

        return recentSalesPanel;
    }

    private void openAllSales() {
        if (dash == null) return;
        dash.openTab(2);
    }

    /** Refresh the compact recent-sales snapshot on the status bar. */
    private void refreshRecentSales() {
        try {
            List<Sale> recent = DatabaseUtils.getInstance().getSaleRepository().findRecentSales(5);
            recentSalesPanel.removeAll();

            JLabel tag = new JLabel("Recent Sales:");
            tag.setFont(new Font("Courier", Font.BOLD, 12));
            tag.setForeground(new Color(200, 210, 220));
            recentSalesPanel.add(tag);

            if (recent == null || recent.isEmpty()) {
                JLabel none = new JLabel("none");
                none.setFont(new Font("Courier", Font.PLAIN, 12));
                none.setForeground(new Color(150, 160, 170));
                recentSalesPanel.add(none);
                recentSalesCountLabel.setText("0");
            } else {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm");
                for (Sale s : recent) {
                    String time = s.getCreatedAt() != null ? sdf.format(new java.util.Date(s.getCreatedAt())) : "";
                    String txt = (s.getReceiptNumber() == null ? "R" : s.getReceiptNumber()) + "  KES "
                            + String.format("%.0f", s.getTotal() == null ? 0 : s.getTotal());
                    JLabel chip = new JLabel(time + "  " + txt);
                    chip.setFont(new Font("Courier", Font.PLAIN, 11));
                    chip.setForeground(new Color(240, 240, 245));
                    chip.setBackground(new Color(73, 80, 87));
                    chip.setOpaque(true);
                    chip.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
                    recentSalesPanel.add(chip);
                }
                recentSalesCountLabel.setText(String.valueOf(recent.size()));
                // re-add the count next to the tag
                JLabel cnt = new JLabel("(" + recent.size() + ")");
                cnt.setFont(new Font("Courier", Font.BOLD, 14));
                cnt.setForeground(new Color(80, 224, 150));
                recentSalesPanel.add(cnt);
            }

            JButton viewAll = new JButton("View All Sales");
            viewAll.setFont(new Font("Courier", Font.BOLD, 12));
            viewAll.setBackground(new Color(23, 162, 184));
            viewAll.setForeground(Color.WHITE);
            viewAll.setFocusPainted(false);
            viewAll.setBorderPainted(false);
            viewAll.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            viewAll.addActionListener(e -> openAllSales());
            recentSalesPanel.add(viewAll);

            recentSalesPanel.revalidate();
            recentSalesPanel.repaint();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private JButton createActionButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Courier", Font.BOLD, 14));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btn.setPreferredSize(new Dimension(0, 42));
        return btn;
    }

    private void handleBarcodeScan() {
        String code = barcodeField.getText().trim();
        if (code.isEmpty()) return;

        Item item = ItemRepository.findByBarCode(code);
        if (item != null) {
            addItemToCart(item, 1.0);
            barcodeField.setText("");
            barcodeField.grabFocus();
        } else {
            AutoClosingNotification.sendNotification("Item not found: " + code);
        }
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

    private void updateItemDiscount(int row, String discText) {
        if (row >= 0 && row < cartItems.size()) {
            cartItems.get(row).discount = MyUtils.getDouble(discText);
            refreshCartTable();
            persistCart();
        }
    }

    /** A sellable unit (retail piece or a wholesale/sub unit). */
    private static class UnitOption {
        final String label;
        final double pricePerPackage;
        final double unitsPerPackage; // retail pieces represented by one package
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
                units.add(new UnitOption(n + " (" + (int) div + "÷)",
                        s.getUnitPrice() != null ? s.getUnitPrice() : retail / div, 1.0 / div));
            }
        }
        return units;
    }

    private UnitOption defaultUnitFor(Item item) {
        List<UnitOption> units = availableUnits(item);
        if (wholesaleMode && units.size() > 1 && item.getWholeScales() != null && item.getWholeScales().length > 0) {
            return units.get(1); // first wholesale unit
        }
        return units.get(0); // retail
    }

    /** Switch a cart line to a different unit, keeping the total retail pieces the same. */
    private void setCartUnit(int row, String unitLabel) {
        if (row < 0 || row >= cartItems.size()) return;
        CartItem ci = cartItems.get(row);
        List<UnitOption> units = availableUnits(ci.item);
        for (UnitOption u : units) {
            if (u.label.equals(unitLabel)) {
                double totalRetailPieces = ci.quantity * ci.unitsPerPackage;
                ci.unitName = u.label;
                ci.unitPrice = u.pricePerPackage;
                ci.unitsPerPackage = u.unitsPerPackage;
                ci.quantity = totalRetailPieces / u.unitsPerPackage;
                if (ci.quantity < 1) ci.quantity = 1;
                ci.buyingPrice = ci.item.getBuyingPrice() != null ? ci.item.getBuyingPrice() : 0.0;
                refreshCartTable();
                persistCart();
                return;
            }
        }
    }

    private void refreshCartTable() {
        cartTableModel.setRowCount(0);
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        double itemCount = 0;

        for (int i = 0; i < cartItems.size(); i++) {
            CartItem ci = cartItems.get(i);
            BigDecimal lineTotal = BigDecimal.valueOf(ci.unitPrice)
                    .multiply(BigDecimal.valueOf(ci.quantity))
                    .subtract(BigDecimal.valueOf(ci.discount));
            if (lineTotal.compareTo(BigDecimal.ZERO) < 0) lineTotal = BigDecimal.ZERO;

            cartTableModel.addRow(new Object[]{
                    i + 1,
                    ci.item.getName(),
                    ci.unitName == null ? "Retail" : ci.unitName,
                    String.format("%.0f", ci.quantity),
                    String.format("KES %.2f", ci.unitPrice),
                    String.format("KES %.2f", lineTotal.doubleValue()),
                    "X"
            });

            subtotal = subtotal.add(BigDecimal.valueOf(ci.unitPrice).multiply(BigDecimal.valueOf(ci.quantity)));
            totalDiscount = totalDiscount.add(BigDecimal.valueOf(ci.discount));
            itemCount += ci.quantity;
        }

        StoreConfig config = getStoreConfig();
        double vat = config.calculateTax(subtotal.doubleValue(), totalDiscount.doubleValue());
        double total = config.calculateTotal(subtotal.doubleValue(), totalDiscount.doubleValue());
        if (total < 0) total = 0;

        itemCountLabel.setText("Items: " + (int) itemCount);
        subtotalValueLabel.setText("KES " + String.format("%.2f", subtotal.doubleValue()));
        vatValueLabel.setText("KES " + String.format("%.2f", vat));
        discountValueLabel.setText("-KES " + String.format("%.2f", totalDiscount.doubleValue()));
        totalLabel.setText("TOTAL: KES " + String.format("%.2f", total));
        if (useComponentMode) renderComponentCart();
    }

    private void openItemGrid() {
        GridtemsSearch gridtemsSearch = new GridtemsSearch();
        gridtemsSearch.setSubmitAction(e -> {
            addGridSelection(gridtemsSearch.getSelectedCartsItems());
            gridtemsSearch.dispose();
        });
    }

    private void openItemSearch() {
        SearchItemListPrompt prompt = new SearchItemListPrompt();
        prompt.itemSelectedRunnable = new ItemSelectedRunnable() {
            @Override
            public void run() {
                prompt.dispose();
                addItemToCart(getClickedItem(), 1.0);
                AutoClosingNotification.sendNotification("Added: " + getClickedItem().getName());
            }
        };
    }

    private void addGridSelection(List<GridItemJpanel> selected) {
        if (selected == null) return;
        for (GridItemJpanel panel : selected) {
            Double qty = MyUtils.getDouble(panel.getQuantity().getText());
            if (qty > 0 && panel.getItem() != null) {
                addItemToCart(panel.getItem(), qty);
            }
        }
    }

    private void selectCustomer() {
        new org.PSmart.POS.Utils.Components.GridSearch.CustomerComponents.CustomerGridSearch(chosen -> {
            selectedCustomer = chosen;
            if (chosen == null) {
                if (selectedCustomerButton != null) selectedCustomerButton.setText("Walk-in Customer");
                AutoClosingNotification.sendNotification("Sale as Walk-in");
            } else {
                double bal = chosen.getCurrentBalance() != null ? chosen.getCurrentBalance() : 0.0;
                if (selectedCustomerButton != null) {
                    selectedCustomerButton.setText(chosen.getFullname()
                            + (bal > 0.001 ? "  [owes KES " + String.format("%.2f", bal) + "]" : ""));
                }
                AutoClosingNotification.sendNotification("Customer: " + chosen.getFullname());
            }
        });
    }

    private void holdSale() {
        if (cartItems.isEmpty()) {
            AutoClosingNotification.sendNotification("Cart is empty");
            return;
        }

        HeldOrderRepository repo = DatabaseUtils.getInstance().getHeldOrderRepository();
        long current = repo.heldCount();
        if (current >= HeldOrderRepository.MAX_HELD) {
            JOptionPane.showMessageDialog(this, "You have reached the maximum of " + HeldOrderRepository.MAX_HELD
                    + " held orders. Recall or complete one before holding another.", "Hold Sale",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        StoreConfig config = getStoreConfig();
        double subtotal = 0, totalDiscount = 0;
        for (CartItem ci : cartItems) {
            subtotal += ci.unitPrice * ci.quantity;
            totalDiscount += ci.discount;
        }
        double vat = config.calculateTax(subtotal, totalDiscount);
        double total = config.calculateTotal(subtotal, totalDiscount);
        if (total < 0) total = 0;

        HeldOrder order = HeldOrder.builder()
                .createdAt(System.currentTimeMillis())
                .customer(selectedCustomer)
                .subtotal(subtotal)
                .discount(totalDiscount)
                .vatAmount(vat)
                .total(total)
                .build();

        List<HeldOrderItem> items = new ArrayList<>();
        for (CartItem ci : cartItems) {
            HeldOrderItem hi = HeldOrderItem.builder()
                    .order(order)
                    .item(ci.item)
                    .quantity(ci.quantity)
                    .unitPrice(ci.unitPrice)
                    .buyingPrice(ci.buyingPrice)
                    .discount(ci.discount)
                    .build();
            items.add(hi);
        }
        order.setItems(items);

        try {
            repo.saveHeld(order);
            clearCart();
            AutoClosingNotification.sendNotification("Sale held - " + (current + 1) + " of " + HeldOrderRepository.MAX_HELD);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not hold sale: " + ex.getMessage(), "Hold Sale", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void recallSale() {
        HeldOrderRepository repo = DatabaseUtils.getInstance().getHeldOrderRepository();
        List<HeldOrder> held = repo.findAllHeld();
        if (held.isEmpty()) {
            AutoClosingNotification.sendNotification("No held sales");
            return;
        }

        String[] options = new String[held.size()];
        for (int i = 0; i < held.size(); i++) {
            HeldOrder h = held.get(i);
            String who = h.getCustomer() != null ? h.getCustomer().getFullname() : "Walk-in";
            String date = new java.text.SimpleDateFormat("dd-MMM HH:mm").format(new java.util.Date(h.getCreatedAt()));
            int n = h.getItems() != null ? h.getItems().size() : 0;
            options[i] = who + "  •  " + date + "  •  " + n + " item(s)  •  KES "
                    + String.format("%.2f", h.getTotal() != null ? h.getTotal() : 0);
        }

        String chosen = (String) JOptionPane.showInputDialog(this, "Select a held sale to recall:", "Recall Sale",
                JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (chosen == null) return;

        int idx = java.util.Arrays.asList(options).indexOf(chosen);
        HeldOrder order = held.get(idx);
        if (order.getItems() != null) {
            for (HeldOrderItem hi : order.getItems()) {
                if (hi.getItem() == null) continue;
                CartItem ci = new CartItem();
                ci.item = hi.getItem();
                ci.quantity = hi.getQuantity() != null ? hi.getQuantity() : 1.0;
                ci.unitPrice = hi.getUnitPrice() != null ? hi.getUnitPrice() : hi.getItem().getSellingPrice();
                ci.buyingPrice = hi.getBuyingPrice() != null ? hi.getBuyingPrice()
                        : (hi.getItem().getBuyingPrice() != null ? hi.getItem().getBuyingPrice() : 0.0);
                ci.discount = hi.getDiscount() != null ? hi.getDiscount() : 0.0;
                addItemToCart(ci.item, ci.quantity);
            }
        }
        selectedCustomer = order.getCustomer();
        repo.discardHeld(order.getUid());
        AutoClosingNotification.sendNotification("Sale recalled");
    }

    private void clearCart() {
        cartItems.clear();
        refreshCartTable();
        selectedCustomer = null;
        persistCart();
    }

    private void processPayment() {
        if (cartItems.isEmpty()) {
            AutoClosingNotification.sendNotification("Cart is empty");
            return;
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        for (CartItem ci : cartItems) {
            subtotal = subtotal.add(BigDecimal.valueOf(ci.unitPrice).multiply(BigDecimal.valueOf(ci.quantity)));
            totalDiscount = totalDiscount.add(BigDecimal.valueOf(ci.discount));
        }
        StoreConfig config = getStoreConfig();
        double subtotalD = subtotal.doubleValue();
        double discountD = totalDiscount.doubleValue();
        double vat = config.calculateTax(subtotalD, discountD);
        double total = config.calculateTotal(subtotalD, discountD);
        if (total < 0) total = 0;
        BigDecimal totalBD = BigDecimal.valueOf(total);

        PaymentDialog dialog = new PaymentDialog(this, totalBD, selectedCustomer);
        dialog.setVisible(true);
        if (!dialog.isConfirmed()) return;

        BigDecimal cashAmt = dialog.getCashAmt();
        BigDecimal mpesaAmt = dialog.getMpesaAmt();
        BigDecimal cardAmt = dialog.getCardAmt();
        BigDecimal creditAmt = dialog.getCreditAmt();
        double creditD = creditAmt.doubleValue();
        boolean onCredit = creditD > 0;

        BigDecimal tendered = cashAmt.add(mpesaAmt).add(cardAmt).add(creditAmt);
        BigDecimal amountPaid = tendered.subtract(creditAmt);
        BigDecimal cashPortion = totalBD.subtract(mpesaAmt).subtract(cardAmt).subtract(creditAmt);
        if (cashPortion.compareTo(BigDecimal.ZERO) < 0) cashPortion = BigDecimal.ZERO;
        BigDecimal change = cashAmt.subtract(cashPortion);
        if (change.compareTo(BigDecimal.ZERO) < 0) change = BigDecimal.ZERO;

        String methodPart = "";
        if (cashAmt.compareTo(BigDecimal.ZERO) > 0) methodPart += "CASH";
        if (mpesaAmt.compareTo(BigDecimal.ZERO) > 0) methodPart += (methodPart.isEmpty() ? "" : "+") + "MPESA";
        if (cardAmt.compareTo(BigDecimal.ZERO) > 0) methodPart += (methodPart.isEmpty() ? "" : "+") + "CARD";
        if (onCredit) methodPart += (methodPart.isEmpty() ? "" : "+") + "CREDIT";
        if (methodPart.isEmpty()) methodPart = "CASH";
        String paymentMethod = methodPart;

        if (onCredit && selectedCustomer != null && selectedCustomer.getCreditLimit() != null) {
            double current = selectedCustomer.getCurrentBalance() == null ? 0.0 : selectedCustomer.getCurrentBalance();
            if (current + creditD > selectedCustomer.getCreditLimit()) {
                JOptionPane.showMessageDialog(this,
                        "Customer has exceeded the credit limit. Current debt: KES " + String.format("%.2f", current)
                                + " / Limit: KES " + String.format("%.2f", selectedCustomer.getCreditLimit()),
                        "Credit Limit", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        try {
            Sale sale = Sale.builder()
                    .createdAt(System.currentTimeMillis())
                    .cashier(loggedUser)
                    .subtotal(subtotalD)
                    .discount(discountD)
                    .vatAmount(vat)
                    .total(total)
                    .amountPaid(amountPaid.doubleValue())
                    .change(change.doubleValue())
                    .paymentMethod(paymentMethod)
                    .status(onCredit ? "CREDIT" : "COMPLETED")
                    .customer(selectedCustomer)
                    .build();

            List<SaleItem> saleItems = new ArrayList<>();
            for (CartItem ci : cartItems) {
                SaleItem si = SaleItem.builder()
                        .item(ci.item)
                        .quantity(ci.quantity)
                        .unitPrice(ci.unitPrice)
                        .buyingPrice(ci.buyingPrice)
                        .discount(ci.discount)
                        .total(ci.unitPrice * ci.quantity - ci.discount)
                        .sale(sale)
                        .build();
                saleItems.add(si);
            }
            sale.setItems(saleItems);

            DatabaseUtils.getInstance().getSaleRepository().save(sale);

            if (onCredit && selectedCustomer != null) {
                DatabaseUtils.getInstance().getCreditRepository().recordCreditSale(sale, selectedCustomer, loggedUser);
            }

            try {
                ReceiptPrinter.printReceipt(sale, config);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            AutoClosingNotification.sendNotification(
                    (onCredit ? "Credit sale " : "Sale completed") + " - KES " + String.format("%.2f", total));
            clearCart();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error saving sale: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    /**
     * Friendly split-payment form. Supports Cash + M-Pesa + Card + Credit for the
     * same transaction, validates the split against the total, shows live remaining /
     * change before payment is confirmed.
     */
    private static class PaymentDialog extends JDialog {
        private final BigDecimal total;
        private final Customer customer;
        private boolean confirmed = false;
        private final JTextField cashField = new JTextField("0.00");
        private final JTextField mpesaField = new JTextField("0.00");
        private final JTextField cardField = new JTextField("0.00");
        private final JTextField creditField = new JTextField("0.00");
        private final JLabel remainingLabel = new JLabel();
        private final JButton balanceBtn;
        private final JButton payBtn;
        private final java.awt.Frame ownerFrame;

        PaymentDialog(java.awt.Component owner, BigDecimal total, Customer customer) {
            super((java.awt.Frame) SwingUtilities.getWindowAncestor(owner)
                    instanceof java.awt.Frame ? (java.awt.Frame) SwingUtilities.getWindowAncestor(owner) : null,
                    "Split Payment", true);
            this.ownerFrame = (java.awt.Frame) SwingUtilities.getWindowAncestor(owner);
            this.total = total;
            this.customer = customer;
            this.balanceBtn = new JButton("Balance");
            this.payBtn = new JButton("PAY & FINISH");
            setSize(560, 460);
            setResizable(false);
            setLocationRelativeTo(ownerFrame);
            buildUI();
        }

        private void buildUI() {
            setLayout(new BorderLayout(12, 12));
            ((JPanel) getContentPane()).setBackground(Color.WHITE);

            JPanel header = new JPanel(new GridLayout(0, 1, 0, 6));
            header.setBorder(BorderFactory.createEmptyBorder(16, 16, 4, 16));
            header.setBackground(Color.WHITE);
            JLabel title = new JLabel("Payment Breakdown");
            title.setFont(new Font("Courier", Font.BOLD, 20));
            title.setForeground(new Color(33, 37, 41));
            JLabel totalL = new JLabel("Total Due:  KES " + String.format("%.2f", total.doubleValue()));
            totalL.setFont(new Font("Courier", Font.BOLD, 18));
            totalL.setForeground(new Color(232, 62, 62));
            JLabel hint = new JLabel("Split the amount across one or more methods.");
            hint.setFont(new Font("Courier", Font.PLAIN, 12));
            hint.setForeground(new Color(108, 117, 125));
            header.add(title);
            header.add(totalL);
            header.add(hint);
            add(header, BorderLayout.NORTH);

            JPanel form = new JPanel(new GridLayout(4, 2, 10, 10));
            form.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
            form.setBackground(Color.WHITE);
            form.add(makeLabel("Cash Received"));
            form.add(makeMoneyField(cashField, new Color(40, 167, 69)));
            form.add(makeLabel("M-Pesa"));
            form.add(makeMoneyField(mpesaField, new Color(0, 123, 255)));
            form.add(makeLabel("Card"));
            form.add(makeMoneyField(cardField, new Color(102, 16, 242)));
            form.add(makeLabel("Credit (on account)"));
            form.add(makeMoneyField(creditField, new Color(255, 193, 7)));
            add(form, BorderLayout.CENTER);

            remainingLabel.setFont(new Font("Courier", Font.BOLD, 16));
            remainingLabel.setHorizontalAlignment(SwingConstants.CENTER);
            remainingLabel.setBorder(BorderFactory.createEmptyBorder(0, 16, 6, 16));
            add(remainingLabel, BorderLayout.SOUTH);

            JPanel foot = new JPanel(new BorderLayout(12, 12));
            foot.setBorder(BorderFactory.createEmptyBorder(4, 16, 16, 16));
            foot.setBackground(Color.WHITE);

            JTextField[] fields = {cashField, mpesaField, cardField, creditField};
            for (JTextField f : fields) {
                f.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                    public void insertUpdate(javax.swing.event.DocumentEvent e) { recalc(); }
                    public void removeUpdate(javax.swing.event.DocumentEvent e) { recalc(); }
                    public void changedUpdate(javax.swing.event.DocumentEvent e) { recalc(); }
                });
            }

            balanceBtn.setFont(new Font("Courier", Font.BOLD, 14));
            balanceBtn.setBackground(new Color(23, 162, 184));
            balanceBtn.setForeground(Color.WHITE);
            balanceBtn.setFocusPainted(false);
            balanceBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            balanceBtn.addActionListener(e -> {
                BigDecimal used = sumAll();
                BigDecimal rem = total.subtract(used);
                if (rem.compareTo(BigDecimal.ZERO) <= 0) return;
                if (customer != null) {
                    creditField.setText(rem.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
                } else {
                    cashField.setText(rem.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
                }
                recalc();
            });

            payBtn.setFont(new Font("Courier", Font.BOLD, 16));
            payBtn.setBackground(new Color(40, 167, 69));
            payBtn.setForeground(Color.WHITE);
            payBtn.setFocusPainted(false);
            payBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            payBtn.addActionListener(e -> finish());

            foot.add(balanceBtn, BorderLayout.WEST);
            foot.add(payBtn, BorderLayout.CENTER);
            add(foot, BorderLayout.SOUTH);

            JButton cancel = new JButton("Cancel");
            cancel.setFont(new Font("Courier", Font.PLAIN, 14));
            cancel.setBackground(new Color(108, 117, 125));
            cancel.setForeground(Color.WHITE);
            cancel.setFocusPainted(false);
            cancel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            cancel.addActionListener(e -> dispose());
            balanceBtn.setPreferredSize(new Dimension(180, 46));

            recalc();
        }

        private JLabel makeLabel(String t) {
            JLabel l = new JLabel(t);
            l.setFont(new Font("Courier", Font.BOLD, 14));
            l.setForeground(new Color(52, 58, 64));
            return l;
        }

        private JTextField makeMoneyField(JTextField t, Color accent) {
            t.setFont(new Font("Courier", Font.BOLD, 18));
            t.setHorizontalAlignment(JTextField.RIGHT);
            t.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                    BorderFactory.createEmptyBorder(8, 8, 8, 8)));
            return t;
        }

        private BigDecimal parse(JTextField f) {
            try {
                return new BigDecimal(f.getText().trim().isEmpty() ? "0" : f.getText().trim());
            } catch (NumberFormatException ex) {
                return BigDecimal.ZERO;
            }
        }

        private BigDecimal sumAll() {
            return parse(cashField).add(parse(mpesaField)).add(parse(cardField)).add(parse(creditField));
        }

        private BigDecimal sumNonCredit() {
            return parse(cashField).add(parse(mpesaField)).add(parse(cardField));
        }

        private void recalc() {
            BigDecimal used = sumAll();
            BigDecimal remaining = total.subtract(used);
            javax.swing.Timer t = new javax.swing.Timer(200, e -> {
                payBtn.setEnabled(remaining.compareTo(BigDecimal.ZERO) <= 0);
                if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                    remainingLabel.setText("Remaining:  KES " + remaining.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
                    remainingLabel.setForeground(new Color(220, 53, 69));
                } else {
                    BigDecimal over = sumNonCredit().subtract(total);
                    if (over.compareTo(BigDecimal.ZERO) > 0) {
                        remainingLabel.setText("Change Due:  KES " + over.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() + "   (all methods entered)");
                        remainingLabel.setForeground(new Color(40, 167, 69));
                    } else {
                        remainingLabel.setText("Complete - remaining: KES 0.00");
                        remainingLabel.setForeground(new Color(40, 167, 69));
                    }
                }
            });
            t.setRepeats(false);
            t.start();
        }

        private void finish() {
            BigDecimal used = sumAll();
            if (used.compareTo(total) < 0) {
                JOptionPane.showMessageDialog(this,
                        "Payment split is KES " + total.subtract(used).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
                                + " short. Add the remaining amount or use Balance.",
                        "Incomplete Payment", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (parse(creditField).compareTo(BigDecimal.ZERO) > 0 && customer == null) {
                JOptionPane.showMessageDialog(this,
                        "Credit requires a selected customer. Choose a customer or pay by another method.",
                        "No Customer", JOptionPane.WARNING_MESSAGE);
                return;
            }
            confirmed = true;
            dispose();
        }

        boolean isConfirmed() { return confirmed; }
        BigDecimal getCashAmt() { return parse(cashField); }
        BigDecimal getMpesaAmt() { return parse(mpesaField); }
        BigDecimal getCardAmt() { return parse(cardField); }
        BigDecimal getCreditAmt() { return parse(creditField); }
    }

    private static class CartItem {
        Item item;
        double quantity;          // number of packages (pieces or cartons)
        double unitPrice;         // price per package
        double buyingPrice;       // cost per full retail piece
        double discount;
        String unitName;          // "Retail", "Carton", etc.
        double unitsPerPackage;   // retail pieces represented by one package (1 = retail)
    }

    /** A card-style cashier cart line: unit selector + qty +/− + line total + remove. */
    private class CashierCartLine extends JPanel {
        private final JComboBox<String> unitCombo = new JComboBox<>();
        private final JLabel qtyLabel = new JLabel("1", SwingConstants.CENTER);
        private final JLabel lineTotalLabel = new JLabel("", SwingConstants.RIGHT);
        private final JLabel unitPriceLabel = new JLabel("", SwingConstants.LEFT);
        private final CartItem ci;

        CashierCartLine(int row, CartItem ci) {
            this.ci = ci;
            setLayout(new BorderLayout(10, 4));
            setBackground(new Color(250, 252, 254));
            setBorder(BorderFactory.createLineBorder(new Color(222, 226, 230), 1));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
            setPreferredSize(new Dimension(0, 70));

            JLabel name = new JLabel(ci.item.getName());
            name.setFont(new Font("Courier", Font.BOLD, 13));
            name.setForeground(new Color(33, 37, 41));
            name.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(233, 236, 239)));

            List<UnitOption> units = availableUnits(ci.item);
            for (UnitOption u : units) unitCombo.addItem(u.label);
            unitCombo.setSelectedItem(ci.unitName);
            unitCombo.setFont(new Font("Courier", Font.PLAIN, 12));
            unitCombo.setBackground(Color.WHITE);
            unitCombo.setBorder(BorderFactory.createLineBorder(new Color(206, 212, 218)));
            unitCombo.addActionListener(e -> {
                Object sel = unitCombo.getSelectedItem();
                if (sel != null) setCartUnit(currentRow(), sel.toString());
            });

            JButton minus = unitButton("-");
            JButton plus = unitButton("+");
            minus.addActionListener(e -> updateItemQuantity(currentRow(), Math.max(1, cartItems.get(currentRow()).quantity - 1)));
            plus.addActionListener(e -> updateItemQuantity(currentRow(), cartItems.get(currentRow()).quantity + 1));

            JButton remove = new JButton("Remove");
            remove.setFont(new Font("Courier", Font.BOLD, 11));
            remove.setBackground(new Color(220, 53, 69));
            remove.setForeground(Color.WHITE);
            remove.setFocusPainted(false);
            remove.setBorderPainted(false);
            remove.addActionListener(e -> removeCartItem(currentRow()));

            JPanel unitQtyPrice = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            unitQtyPrice.setOpaque(false);
            unitQtyPrice.add(unitCombo);
            unitQtyPrice.add(Box.createHorizontalStrut(12));
            unitQtyPrice.add(minus);
            unitQtyPrice.add(qtyLabel);
            unitQtyPrice.add(plus);
            unitQtyPrice.add(Box.createHorizontalStrut(16));
            unitQtyPrice.add(unitPriceLabel);

            JPanel center = new JPanel(new BorderLayout());
            center.setOpaque(false);
            center.add(unitQtyPrice, BorderLayout.WEST);

            JPanel right = new JPanel(new BorderLayout());
            right.setOpaque(false);
            right.add(lineTotalLabel, BorderLayout.NORTH);
            right.add(remove, BorderLayout.SOUTH);

            add(name, BorderLayout.NORTH);
            add(center, BorderLayout.CENTER);
            add(right, BorderLayout.EAST);
            refreshLine(ci);
        }

        private int currentRow() {
            return cartItems.indexOf(ci);
        }

        private void refreshLine(CartItem ci) {
            double lineTotal = ci.unitPrice * ci.quantity - ci.discount;
            if (lineTotal < 0) lineTotal = 0;
            qtyLabel.setText(String.format("%.0f", ci.quantity));
            unitPriceLabel.setText("@" + MyUtils.formatCurrency(String.valueOf(ci.unitPrice)));
            lineTotalLabel.setText(MyUtils.formatCurrency(String.valueOf(lineTotal)));
            lineTotalLabel.setFont(new Font("Courier", Font.BOLD, 14));
            lineTotalLabel.setForeground(new Color(0, 32, 74));
        }

        private JButton unitButton(String label) {
            JButton b = new JButton(label);
            b.setFont(new Font("Courier", Font.BOLD, 14));
            b.setBackground(new Color(23, 162, 184));
            b.setForeground(Color.WHITE);
            b.setFocusPainted(false);
            b.setBorderPainted(false);
            b.setPreferredSize(new Dimension(30, 26));
            return b;
        }
    }
}
