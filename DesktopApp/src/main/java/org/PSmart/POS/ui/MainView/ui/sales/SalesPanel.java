package org.PSmart.POS.ui.MainView.ui.sales;

import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.StoreConfig;
import org.PSmart.POS.Entities.User;
import org.PSmart.POS.Repositories.SaleQueries;
import org.PSmart.POS.Utils.Components.PTextField;
import org.PSmart.POS.Utils.Components.PaginationBar;
import org.PSmart.POS.Utils.Components.forms.Ui;
import org.PSmart.POS.Utils.ExcelExporter;
import org.PSmart.POS.Utils.IconsUtils;
import org.PSmart.POS.Utils.ReceiptPrinter;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.List;

/**
 * Sales: find any sale fast (receipt #, customer, period, status, payment, cashier), review it,
 * reprint it, or cancel it with a recorded reason. Data comes from SaleQueries (projection rows,
 * short-lived sessions), so paging stays quick however many sales exist.
 */
public class SalesPanel extends JPanel {

    private static final int PAGE_SIZE = 20;
    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final Color RED = new Color(200, 30, 45);

    private final User user;

    private final JPanel header = new JPanel(null);
    private final JPanel cards = new JPanel(new GridLayout(1, 4, 14, 0));
    private final JLabel todayRevenue = new JLabel("-", SwingConstants.LEFT);
    private final JLabel todayCount = new JLabel("-", SwingConstants.LEFT);
    private final JLabel filteredTotal = new JLabel("-", SwingConstants.LEFT);
    private final JLabel filteredCount = new JLabel("-", SwingConstants.LEFT);

    private final PTextField search = new PTextField(Ui.SLATE, new Color(0, 0, 255), "Search receipt # or customer",
            IconsUtils.getIcon("search.png"));
    private final JComboBox<String> period = new JComboBox<>(new String[]{"All Time", "Today", "This Week", "This Month", "Custom Range"});
    private final JComboBox<String> status = new JComboBox<>(new String[]{"All", "Completed", "Credit", "Voided"});
    private final JComboBox<String> payment = new JComboBox<>(new String[]{"All", "Cash", "M-Pesa", "Card", "Credit"});
    private final JComboBox<String> cashier = new JComboBox<>(new String[]{"All"});
    private final org.jdatepicker.JDatePicker fromField = org.PSmart.POS.Utils.Components.Inputs.datePicker(null);
    private final org.jdatepicker.JDatePicker toField = org.PSmart.POS.Utils.Components.Inputs.datePicker(null);
    private final List<JComponent> labels = new ArrayList<>();
    private final List<UUID> cashierIds = new ArrayList<>();

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Receipt #", "Date / Time", "Customer", "Cashier", "Items", "Total", "Payment", "Status"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JScrollPane scroll = new JScrollPane(table);
    private final PaginationBar pagination;
    private final JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
    private final JLabel info = new JLabel(" ");
    private final JLabel flash = new JLabel(" ");

    private List<SaleQueries.Row> rows = new ArrayList<>();
    private int page = 0;
    private int loadToken = 0;

    public SalesPanel() { this(null); }

    public SalesPanel(User user) {
        super(null);
        this.user = user;
        setBackground(Ui.PAGE);
        buildHeader();
        buildCards();
        buildFilters();
        buildTable();
        pagination = new PaginationBar(p -> { page = p; load(); });
        pagination.setPageSize(PAGE_SIZE);
        add(pagination);
        buildActions();
        loadCashiers();
        SwingUtilities.invokeLater(this::reload);
        layoutUi();
    }

    @Override
    public void setBounds(int x, int y, int w, int h) {
        super.setBounds(x, y, w, h);
        if (w > 0) layoutUi();
    }

    // ------------------------------------------------------------------ build

    private void buildHeader() {
        header.setBackground(Ui.SLATE);
        JLabel title = new JLabel("Sales");
        title.setFont(new Font("Courier", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        title.setBounds(20, 0, 300, 60);
        header.add(title);
        add(header);
    }

    private JPanel card(String caption, JLabel value) {
        JPanel c = new JPanel(new BorderLayout());
        c.setBackground(Color.WHITE);
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 5, 0, 0, new Color(0, 0, 137)),
                BorderFactory.createEmptyBorder(8, 14, 8, 10)));
        JLabel cap = new JLabel(caption);
        cap.setFont(new Font("Courier", Font.PLAIN, 12));
        cap.setForeground(new Color(108, 117, 125));
        value.setFont(new Font("Courier", Font.BOLD, 24));
        value.setForeground(new Color(0, 0, 137));
        c.add(cap, BorderLayout.NORTH);
        c.add(value, BorderLayout.CENTER);
        return c;
    }

    private void buildCards() {
        cards.setOpaque(false);
        cards.add(card("Today's sales", todayRevenue));
        cards.add(card("Today's receipts", todayCount));
        cards.add(card("Total for current filter", filteredTotal));
        cards.add(card("Receipts for current filter", filteredCount));
        add(cards);
    }

    private JLabel tag(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Courier", Font.BOLD, 12));
        l.setForeground(new Color(52, 58, 64));
        labels.add(l);
        add(l);
        return l;
    }

    private void buildFilters() {
        add(search);
        search.getField().addKeyListener(new java.awt.event.KeyAdapter() {
            private javax.swing.Timer debounce;
            @Override public void keyReleased(java.awt.event.KeyEvent e) {
                if (debounce != null) debounce.stop();
                debounce = new javax.swing.Timer(250, ev -> reload());
                debounce.setRepeats(false);
                debounce.start();
            }
        });
        for (JComboBox<String> c : List.of(period, status, payment, cashier)) {
            c.setFont(new Font("Courier", Font.PLAIN, 13));
            add(c);
        }
        period.addActionListener(e -> {
            boolean custom = "Custom Range".equals(period.getSelectedItem());
            fromField.setVisible(custom);
            toField.setVisible(custom);
            for (JComponent l : labels) if (l.getName() != null && l.getName().equals("custom")) l.setVisible(custom);
            if (!custom) reload();
            layoutUi();
        });
        status.addActionListener(e -> reload());
        payment.addActionListener(e -> reload());
        cashier.addActionListener(e -> reload());

        for (org.jdatepicker.JDatePicker f : List.of(fromField, toField)) {
            f.setVisible(false);
            f.addActionListener(e -> reload());
            add(f);
        }
        tag("Period");
        tag("Status");
        tag("Payment");
        tag("Cashier");
        JLabel fl = tag("From");
        JLabel tl = tag("To");
        fl.setName("custom");
        tl.setName("custom");
        fl.setVisible(false);
        tl.setVisible(false);
    }

    private void buildTable() {
        Ui.styleTable(table);
        table.setRowHeight(34);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        DefaultTableCellRenderer base = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean f, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, f, row, col);
                boolean voided = row < rows.size() && rows.get(row).voided();
                if (!sel) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 252));
                    c.setForeground(voided ? RED : new Color(40, 40, 40));
                }
                if (voided) {
                    Font fo = c.getFont();
                    java.util.Map<java.awt.font.TextAttribute, Object> at = new HashMap<>(fo.getAttributes());
                    if (col == 5) at.put(java.awt.font.TextAttribute.STRIKETHROUGH, java.awt.font.TextAttribute.STRIKETHROUGH_ON);
                    c.setFont(fo.deriveFont(at));
                }
                setHorizontalAlignment(col == 4 ? CENTER : col == 5 ? RIGHT : LEFT);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return c;
            }
        };
        for (int i = 0; i < model.getColumnCount(); i++) table.getColumnModel().getColumn(i).setCellRenderer(base);
        int[] w = {110, 150, 200, 150, 60, 120, 120, 110};
        for (int i = 0; i < w.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { if (e.getClickCount() == 2) view(); }
        });
        add(scroll);
    }

    private void buildActions() {
        actions.setOpaque(false);
        JButton view = Ui.primary("View / Cancel");
        JButton print = Ui.primary("Print Receipt");
        JButton inv = Ui.neutral("Invoice");
        JButton export = Ui.neutral("Export Excel");
        JButton refresh = Ui.neutral("Refresh");
        view.addActionListener(e -> view());
        print.addActionListener(e -> withSale(r -> {
            StoreConfig cfg = config();
            ReceiptPrinter.printReceipt(org.PSmart.POS.Repositories.SaleQueries.load(r.uid), cfg);
        }));
        inv.addActionListener(e -> withSale(r -> ReceiptPrinter.previewInvoice(this, SaleQueries.load(r.uid), config())));
        export.addActionListener(e -> export());
        refresh.addActionListener(e -> reload());
        for (JButton b : new JButton[]{view, print, inv, export, refresh}) actions.add(b);
        info.setFont(new Font("Courier", Font.BOLD, 13));
        add(actions);
        add(info);
        flash.setFont(new Font("Courier", Font.BOLD, 13));
        flash.setForeground(Color.RED);
        add(flash);
    }

    private void layoutUi() {
        int w = getWidth(), h = getHeight();
        if (w <= 1 || h <= 1) return;
        header.setBounds(0, 0, w, 60);
        cards.setBounds(20, 70, w - 40, 76);
        int y = 156;
        int fx = 20;
        search.setLocation(fx, y - 4);
        search.setSize(Math.max(300, w / 4), 60);
        fx += Math.max(300, w / 4) + 16;
        int cw = 150;
        placeCombo(period, labels.get(0), fx, y + 22, cw);
        fx += cw + 12;
        placeCombo(status, labels.get(1), fx, y + 22, 130);
        fx += 142;
        placeCombo(payment, labels.get(2), fx, y + 22, 130);
        fx += 142;
        placeCombo(cashier, labels.get(3), fx, y + 22, 170);
        fx += 182;
        if (fromField.isVisible()) {
            labels.get(4).setBounds(fx, y, 160, 18);
            fromField.setBounds(fx, y + 22, 150, 30);
            fx += 162;
            labels.get(5).setBounds(fx, y, 60, 18);
            toField.setBounds(fx, y + 22, 150, 30);
        }
        scroll.setBounds(20, 226, w - 40, h - 226 - 108);
        pagination.setBounds(20, h - 104, w - 40, 44);
        actions.setBounds(20, h - 58, w - 400, 44);
        info.setBounds(w - 380, h - 54, 360, 22);
        info.setHorizontalAlignment(SwingConstants.RIGHT);
        flash.setBounds(20, h - 22, w - 40, 20);
        revalidate();
        repaint();
    }

    private void placeCombo(JComboBox<String> c, JComponent label, int x, int y, int w) {
        label.setBounds(x, y - 22, w, 18);
        c.setBounds(x, y, w, 30);
    }

    // ------------------------------------------------------------------ data

    private StoreConfig config() {
        StoreConfig c = DatabaseUtils.getInstance().getStoreConfigRepository().getStoreConfig();
        return c != null ? c : StoreConfig.defaultConfig();
    }

    private void loadCashiers() {
        new SwingWorker<Map<UUID, String>, Void>() {
            @Override protected Map<UUID, String> doInBackground() { return SaleQueries.cashiers(); }
            @Override protected void done() {
                try {
                    cashierIds.clear();
                    cashier.removeAllItems();
                    cashier.addItem("All");
                    for (Map.Entry<UUID, String> e : get().entrySet()) {
                        cashierIds.add(e.getKey());
                        cashier.addItem(e.getValue());
                    }
                } catch (Exception ignored) {
                    // cashier filter is optional
                }
            }
        }.execute();
    }

    private SaleQueries.Filter filter() {
        SaleQueries.Filter f = new SaleQueries.Filter();
        f.keyword = search.getText().trim();
        f.status = String.valueOf(status.getSelectedItem());
        f.payment = String.valueOf(payment.getSelectedItem());
        int ci = cashier.getSelectedIndex() - 1;
        if (ci >= 0 && ci < cashierIds.size()) f.cashierId = cashierIds.get(ci);
        LocalDate today = LocalDate.now();
        String p = String.valueOf(period.getSelectedItem());
        LocalDate from = null, to = null;
        switch (p) {
            case "Today": from = today; to = today; break;
            case "This Week": from = today.with(DayOfWeek.MONDAY); to = today; break;
            case "This Month": from = today.withDayOfMonth(1); to = today; break;
            case "Custom Range":
                from = org.PSmart.POS.Utils.Components.Inputs.dateOf(fromField);
                to = org.PSmart.POS.Utils.Components.Inputs.dateOf(toField);
                break;
            default: break;
        }
        if (from != null) f.from = from.atStartOfDay(ZONE).toInstant().toEpochMilli();
        if (to != null) f.to = to.plusDays(1).atStartOfDay(ZONE).toInstant().toEpochMilli() - 1;
        return f;
    }

    private void note(String msg) {
        flash.setText(msg);
        javax.swing.Timer t = new javax.swing.Timer(5000, e -> flash.setText(" "));
        t.setRepeats(false);
        t.start();
    }

    private void reload() {
        page = 0;
        load();
    }

    private void load() {
        final int token = ++loadToken;
        final SaleQueries.Filter f = filter();
        final int pg = page;
        new SwingWorker<Object[], Void>() {
            @Override protected Object[] doInBackground() {
                // one retry: a pooled connection can be stale after the database was idle
                for (int attempt = 0; ; attempt++) {
                    try {
                        List<SaleQueries.Row> r = SaleQueries.rows(f, pg, PAGE_SIZE);
                        double[] s = SaleQueries.summary(f);
                        SaleQueries.Filter today = new SaleQueries.Filter();
                        today.from = LocalDate.now().atStartOfDay(ZONE).toInstant().toEpochMilli();
                        double[] t = SaleQueries.summary(today);
                        return new Object[]{r, s, t};
                    } catch (RuntimeException e) {
                        if (attempt >= 1) throw e;
                        try { Thread.sleep(300); } catch (InterruptedException ignored) { }
                    }
                }
            }
            @Override protected void done() {
                if (token != loadToken) return;   // a newer request superseded this one
                try {
                    @SuppressWarnings("unchecked") List<SaleQueries.Row> r = (List<SaleQueries.Row>) get()[0];
                    double[] s = (double[]) get()[1];
                    double[] t = (double[]) get()[2];
                    render(r, s, t);
                } catch (Exception ex) {
                    Throwable x = ex;
                    while (x.getCause() != null) x = x.getCause();
                    note("Could not load sales: " + x.getMessage() + "  (press Refresh to retry)");
                }
            }
        }.execute();
    }

    private void render(List<SaleQueries.Row> r, double[] summary, double[] today) {
        rows = r;
        model.setRowCount(0);
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm");
        for (SaleQueries.Row row : r) {
            model.addRow(new Object[]{row.receipt, sdf.format(new Date(row.createdAt)), row.customer, row.cashier, row.items,
                    String.format("%,.2f", row.total), row.payment.replace("MPESA", "M-Pesa").replace("+", " + "),
                    row.voided() ? "CANCELLED" : row.status});
        }
        long total = (long) summary[0];
        pagination.setTotalRows(total);
        pagination.setPage(page);
        todayRevenue.setText("KES " + String.format("%,.2f", today[2]));
        todayCount.setText(String.valueOf((long) today[1]));
        filteredTotal.setText("KES " + String.format("%,.2f", summary[2]));
        filteredCount.setText(String.valueOf((long) summary[1]));
        info.setText(total == 0 ? "No sales match these filters"
                : (total > summary[1] ? (long) (total - summary[1]) + " cancelled sale(s) in this view" : " "));
    }

    // ------------------------------------------------------------------ actions

    private void withSale(java.util.function.Consumer<SaleQueries.Row> action) {
        int r = table.getSelectedRow();
        if (r < 0 || r >= rows.size()) {
            note("Select a sale in the list first.");
            return;
        }
        try {
            action.accept(rows.get(r));
        } catch (Exception e) {
            note("Could not open the sale: " + e.getMessage());
        }
    }

    private void view() {
        withSale(r -> new SaleViewForm(r.uid, user, this::reload));
    }

    private void export() {
        new SwingWorker<List<Object[]>, Void>() {
            @Override protected List<Object[]> doInBackground() {
                List<Object[]> out = new ArrayList<>();
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm");
                SaleQueries.Filter f = filter();
                for (int p = 0; ; p++) {
                    List<SaleQueries.Row> batch = SaleQueries.rows(f, p, 500);
                    for (SaleQueries.Row r : batch) {
                        out.add(new Object[]{r.receipt, sdf.format(new Date(r.createdAt)), r.customer, r.cashier, r.items,
                                r.subtotal, r.vat, r.total, r.payment, r.voided() ? "CANCELLED" : r.status});
                    }
                    if (batch.size() < 500) break;
                }
                return out;
            }
            @Override protected void done() {
                try {
                    ExcelExporter.exportToExcel(SalesPanel.this, "Sales_" + LocalDate.now(),
                            new String[]{"Receipt #", "Date/Time", "Customer", "Cashier", "Items", "Subtotal", "VAT", "Total", "Payment", "Status"}, get());
                } catch (Exception ex) {
                    note("Export failed: " + ex.getMessage());
                }
            }
        }.execute();
    }
}
