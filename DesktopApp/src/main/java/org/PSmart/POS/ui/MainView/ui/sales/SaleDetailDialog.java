package org.PSmart.POS.ui.MainView.ui.sales;

import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.Sale;
import org.PSmart.POS.Entities.SaleItem;
import org.PSmart.POS.Entities.StoreConfig;
import org.PSmart.POS.Repositories.SaleRepository;
import org.PSmart.POS.Utils.MyUtils;
import org.PSmart.POS.Utils.Palette;
import org.PSmart.POS.Utils.ReceiptPrinter;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Card-style sale detail: header, item table with buy/sell prices + per-line profit,
 * per-row Edit/Delete actions, live totals, "Print Receipt" and "Delete Sale". Any edit
 * reprocesses the sale totals from its items.
 */
public class SaleDetailDialog extends JDialog {

    private final SaleRepository repo = DatabaseUtils.getInstance().getSaleRepository();
    private Sale sale;
    private final Runnable onChanged;

    private DefaultTableModel itemModel;
    private JTable itemTable;
    private JLabel subtotalLabel;
    private JLabel vatLabel;
    private JLabel discountLabel;
    private JLabel profitLabel;
    private JLabel totalLabel;
    private JLabel statusLabel;

    private static final String[] COLS =
            {"Item", "Qty", "Buy", "Sell", "Discount", "Line Total", "Profit", ""};

    public SaleDetailDialog(Component parent, Sale sale, Runnable onChanged) {
        super((Frame) SwingUtilities.getWindowAncestor(parent), "Sale Details", true);
        this.sale = sale;
        this.onChanged = onChanged;
        draw();
        load();
        setLocationRelativeTo(parent);
    }

    private void draw() {
        setLayout(null);
        getContentPane().setBackground(Palette.CARD);
        setSize(940, 640);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Header
        JPanel header = new JPanel(null);
        header.setBackground(Palette.HEADER);
        header.setBounds(0, 0, 940, 64);
        JLabel title = new JLabel("Sale Details — " + sale.getReceiptNumber());
        title.setFont(Palette.courier(20));
        title.setForeground(Color.WHITE);
        title.setBounds(20, 0, 480, 64);
        statusLabel = new JLabel(sale.getStatus());
        statusLabel.setFont(Palette.courier(14));
        statusLabel.setForeground(sale.getStatus() != null && sale.getStatus().equalsIgnoreCase("COMPLETED")
                ? new Color(40, 167, 69) : Palette.AMBER);
        statusLabel.setBounds(720, 0, 120, 64);
        JButton closeBtn = new JButton("Close");
        closeBtn.setFont(Palette.courier(13));
        closeBtn.setBackground(Palette.GRAY);
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setFocusPainted(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setBounds(840, 16, 80, 32);
        closeBtn.addActionListener(e -> dispose());
        header.add(title);
        header.add(statusLabel);
        header.add(closeBtn);
        add(header);

        // Meta info
        JPanel meta = new JPanel(null);
        meta.setBackground(Palette.CARD);
        meta.setBounds(0, 64, 940, 70);
        meta.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Palette.BORDER_SUBTLE));
        String date = sale.getCreatedAt() != null
                ? new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").format(new Date(sale.getCreatedAt())) : "N/A";
        String cashier = sale.getCashier() != null ? sale.getCashier().getFullname() : "N/A";
        String customer = sale.getCustomer() != null ? sale.getCustomer().getFullname() : "Walk-in";
        String payment = sale.getPaymentMethod() != null ? sale.getPaymentMethod() : "N/A";
        add(metaField(meta, "Date", date, 20, 0, 460, 34));
        add(metaField(meta, "Payment", payment, 20, 34, 460, 33));
        add(metaField(meta, "Cashier", cashier, 480, 0, 460, 34));
        add(metaField(meta, "Customer", customer, 480, 34, 460, 33));
        add(meta);

        // Items table
        itemModel = new DefaultTableModel(COLS, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        itemTable = new JTable(itemModel);
        itemTable.setRowHeight(36);
        itemTable.setFont(new Font("Courier", Font.PLAIN, 13));
        itemTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemTable.setGridColor(Palette.GRID);
        itemTable.setShowGrid(true);

        JTableHeader th = itemTable.getTableHeader();
        th.setFont(Palette.courier(13));
        th.setBackground(Palette.HEADER);
        th.setForeground(Color.WHITE);
        th.setPreferredSize(new Dimension(0, 34));

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(JLabel.CENTER);
        itemTable.getColumnModel().getColumn(0).setPreferredWidth(300);
        for (int i = 1; i < COLS.length - 1; i++)
            itemTable.getColumnModel().getColumn(i).setCellRenderer(center);

        // Action buttons column
        itemTable.getColumnModel().getColumn(COLS.length - 1).setCellRenderer(new ActionCellRenderer());
        itemTable.getColumnModel().getColumn(COLS.length - 1).setCellEditor(new ActionCellEditor());

        JScrollPane scroll = new JScrollPane(itemTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setBounds(20, 150, 900, 330);
        scroll.getViewport().setBackground(Color.WHITE);
        add(scroll);

        // Totals panel
        JPanel totals = new JPanel(null);
        totals.setBackground(Palette.BG);
        totals.setBounds(20, 492, 900, 110);
        totals.setBorder(BorderFactory.createLineBorder(Palette.BORDER_CARD));

        subtotalLabel = addTotal(totals, "Subtotal", 20, 10, 300, 30);
        vatLabel = addTotal(totals, "VAT", 20, 40, 300, 30);
        discountLabel = addTotal(totals, "Discount", 20, 70, 300, 30);
        profitLabel = addTotal(totals, "Gross Profit", 480, 10, 400, 30);
        profitLabel.setForeground(new Color(111, 66, 193));
        totalLabel = addTotal(totals, "Total", 480, 40, 400, 30);
        totalLabel.setFont(Palette.courier(22));
        totalLabel.setForeground(new Color(40, 167, 69));

        JButton printBtn = btn("Print Receipt", Palette.BLUE_BTN, 700, 78, 180, 28);
        printBtn.addActionListener(e -> printReceipt());
        totals.add(printBtn);

        add(totals);

        // Footer buttons
        JButton deleteSaleBtn = btn("Delete Sale", Palette.RED, 20, 610, 160, 40);
        deleteSaleBtn.addActionListener(e -> deleteSale());
        add(deleteSaleBtn);

        JButton refreshBtn = btn("Refresh", Palette.GRAY, 190, 610, 120, 40);
        refreshBtn.addActionListener(e -> { reload(); });
        add(refreshBtn);
    }

    private JLabel metaField(JPanel parent, String key, String value, int x, int y, int w, int h) {
        JLabel l = new JLabel(key + ":  " + value);
        l.setFont(new Font("Courier", Font.BOLD, 13));
        l.setForeground(Palette.NAVY);
        l.setBounds(x, y, w, h);
        parent.add(l);
        return l;
    }

    private JLabel addTotal(JPanel parent, String label, int x, int y, int w, int h) {
        JLabel l = new JLabel(label + ":  0.00");
        l.setFont(new Font("Courier", Font.BOLD, 15));
        l.setForeground(Palette.GRAY_SOFT);
        l.setBounds(x, y, w, h);
        parent.add(l);
        return l;
    }

    private JButton btn(String text, Color bg, int x, int y, int w, int h) {
        JButton b = new JButton(text);
        b.setFont(Palette.courier(13));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBounds(x, y, w, h);
        return b;
    }

    private void load() {
        sale = repo.findSaleById(sale.getUid());
        if (sale == null) { dispose(); return; }
        reload();
    }

    private void reload() {
        itemModel.setRowCount(0);
        double grossProfit = 0;
        List<SaleItem> items = sale.getItems();
        if (items != null) {
            for (SaleItem si : items) {
                if (si.isDeleted()) continue;
                double sell = si.getUnitPrice() != null ? si.getUnitPrice() : 0;
                double buy = si.getBuyingPrice() != null ? si.getBuyingPrice() : 0;
                double qty = si.getQuantity() != null ? si.getQuantity() : 0;
                double disc = si.getDiscount() != null ? si.getDiscount() : 0;
                double line = sell * qty - disc;
                double profit = (sell - buy) * qty - disc;
                if (line < 0) line = 0;
                if (profit < 0) profit = 0;
                grossProfit += profit;
                itemModel.addRow(new Object[]{
                        si.getItem() != null ? si.getItem().getName() : "?",
                        String.format("%.0f", qty),
                        "KES " + MyUtils.formatCurrency(String.valueOf(buy)),
                        "KES " + MyUtils.formatCurrency(String.valueOf(sell)),
                        "KES " + MyUtils.formatCurrency(String.valueOf(disc)),
                        "KES " + MyUtils.formatCurrency(String.valueOf(line)),
                        "KES " + MyUtils.formatCurrency(String.valueOf(profit)),
                        "Edit|X"
                });
            }
        }

        subtotalLabel.setText("Subtotal:  KES " + MyUtils.formatCurrency(String.valueOf(sale.getSubtotal())));
        vatLabel.setText("VAT:  KES " + MyUtils.formatCurrency(String.valueOf(sale.getVatAmount())));
        discountLabel.setText("Discount:  KES " + MyUtils.formatCurrency(String.valueOf(sale.getDiscount())));
        profitLabel.setText("Gross Profit:  KES " + MyUtils.formatCurrency(String.valueOf(grossProfit)));
        totalLabel.setText("Total:  KES " + MyUtils.formatCurrency(String.valueOf(sale.getTotal())));
        itemTable.clearSelection();
    }

    private void editItem(int row) {
        if (row < 0 || sale == null || sale.getItems() == null) return;
        List<SaleItem> live = sale.getItems();
        int idx = 0;
        for (SaleItem si : live) {
            if (si.isDeleted()) continue;
            if (idx == row) {
                SaleItem updated = editItemDialog(si);
                if (updated != null) {
                    saveItem(updated);
                }
                return;
            }
            idx++;
        }
    }

    private SaleItem editItemDialog(SaleItem si) {
        JTextField qty = new JTextField(String.valueOf(si.getQuantity().longValue()));
        qty.setFont(new Font("Courier", Font.PLAIN, 14));
        JTextField sell = new JTextField(si.getUnitPrice() != null ? String.valueOf(si.getUnitPrice()) : "0");
        sell.setFont(new Font("Courier", Font.PLAIN, 14));
        JTextField disc = new JTextField(si.getDiscount() != null ? String.valueOf(si.getDiscount()) : "0");
        disc.setFont(new Font("Courier", Font.PLAIN, 14));

        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.add(new JLabel("Quantity:"));
        form.add(qty);
        form.add(new JLabel("Selling Price:"));
        form.add(sell);
        form.add(new JLabel("Discount (KES):"));
        form.add(disc);

        int out = JOptionPane.showConfirmDialog(this, form,
                "Edit Item — " + (si.getItem() != null ? si.getItem().getName() : "Item"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (out != JOptionPane.OK_OPTION) return null;

        si.setQuantity(MyUtils.getDouble(qty.getText()));
        si.setUnitPrice(MyUtils.getDouble(sell.getText()));
        si.setDiscount(MyUtils.getDouble(disc.getText()));
        return si;
    }

    private void saveItem(SaleItem updated) {
        int out = JOptionPane.showConfirmDialog(this,
                "Save item changes? Totals will be recalculated.", "Confirm Edit",
                JOptionPane.YES_NO_OPTION);
        if (out != JOptionPane.YES_OPTION) return;
        try {
            repo.reprocessSale(sale);
            sale = repo.findSaleById(sale.getUid());
            reload();
            notifyChanged();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteItem(int row) {
        if (row < 0 || sale == null || sale.getItems() == null) return;
        List<SaleItem> live = sale.getItems();
        int idx = 0;
        SaleItem target = null;
        for (SaleItem si : live) {
            if (si.isDeleted()) continue;
            if (idx == row) { target = si; break; }
            idx++;
        }
        if (target == null) return;
        int out = JOptionPane.showConfirmDialog(this,
                "Remove this item from the sale?", "Remove Item", JOptionPane.YES_NO_OPTION);
        if (out != JOptionPane.YES_OPTION) return;
        try {
            repo.removeSaleItem(sale, target);
            sale = repo.findSaleById(sale.getUid());
            reload();
            notifyChanged();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Remove failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSale() {
        int out = JOptionPane.showConfirmDialog(this,
                "Delete this sale permanently?", "Delete Sale", JOptionPane.YES_NO_OPTION);
        if (out != JOptionPane.YES_OPTION) return;
        try {
            repo.deleteSale(sale);
            notifyChanged();
            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Delete failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void printReceipt() {
        try {
            StoreConfig cfg = DatabaseUtils.getInstance().getStoreConfigRepository().getStoreConfig();
            ReceiptPrinter.printReceipt(sale, cfg != null ? cfg : StoreConfig.defaultConfig());
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Print failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void notifyChanged() {
        if (onChanged != null) onChanged.run();
    }

    // --- Cell editor/renderer for "Edit|X" ---
    private class ActionCellRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int row, int col) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
            p.setBackground(Color.WHITE);
            JButton edit = small("Edit", Palette.BLUE_BTN);
            JButton del = small("X", Palette.RED);
            p.add(edit);
            p.add(del);
            return p;
        }
    }

    private class ActionCellEditor extends DefaultCellEditor {
        ActionCellEditor() { super(new JCheckBox()); }
        public Component getTableCellEditorComponent(JTable t, Object v, boolean sel, int row, int col) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
            p.setBackground(Color.WHITE);
            JButton edit = small("Edit", Palette.BLUE_BTN);
            edit.addActionListener(e -> { fireEditingStopped(); editItem(row); });
            JButton del = small("X", Palette.RED);
            del.addActionListener(e -> { fireEditingStopped(); deleteItem(row); });
            p.add(edit);
            p.add(del);
            return p;
        }
    }

    private JButton small(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Courier", Font.BOLD, 12));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(text.length() > 1 ? 50 : 30, 24));
        return b;
    }
}