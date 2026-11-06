package org.PSmart.POS.ui.MainView.ui.reports;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Utils.ExcelExporter;
import org.PSmart.POS.Utils.Palette;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.List;

/**
 * Reports hub mirroring the Android app: Summary (P&amp;L), Daily trend, Product sales,
 * Payment methods, Stock &amp; low stock, Expenses, Debtors. Every tab exports to Excel.
 * Read-only: it only runs aggregate SELECTs and never modifies data.
 */
public class ReportsPanel extends JPanel {

    private static final List<String> VOID_STATUSES = List.of("VOID", "VOIDED", "CANCELLED", "CANCELED");
    private static final ZoneId ZONE = ZoneId.systemDefault();

    private final JComboBox<String> rangeCombo = new JComboBox<>(
            new String[]{"Today", "This Week", "This Month", "This Year", "All Time", "Custom Range"});
    private final org.jdatepicker.JDatePicker fromField = org.PSmart.POS.Utils.Components.Inputs.datePicker(null);
    private final org.jdatepicker.JDatePicker toField = org.PSmart.POS.Utils.Components.Inputs.datePicker(null);
    private final JLabel statusLabel = new JLabel(" ");
    private final JTabbedPane tabs = new JTabbedPane();

    private final Map<String, DefaultTableModel> models = new LinkedHashMap<>();
    private JPanel header;
    private JPanel filterBar;

    private final org.PSmart.POS.Entities.User sessionUser;

    public ReportsPanel() {
        this(null);
    }

    public ReportsPanel(org.PSmart.POS.Entities.User sessionUser) {
        super(null);
        this.sessionUser = sessionUser;
        setBackground(Palette.BG);
        buildHeader();
        buildFilterBar();
        buildTabs();
        add(tabs);
        SwingUtilities.invokeLater(this::refresh);
        layoutUi();
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        if (width > 0) layoutUi();
    }

    private void layoutUi() {
        if (getWidth() <= 1 || getHeight() <= 1) return;
        header.setBounds(0, 0, getWidth(), 60);
        filterBar.setBounds(20, 70, getWidth() - 40, 50);
        tabs.setBounds(20, 130, getWidth() - 40, getHeight() - 140);
        revalidate();
        repaint();
    }

    // ------------------------------------------------------------------ UI

    private void buildHeader() {
        header = new JPanel(null);
        header.setBackground(Palette.HEADER);
        JLabel title = new JLabel("Reports");
        title.setFont(Palette.courier(22));
        title.setForeground(Color.WHITE);
        title.setBounds(20, 0, 300, 60);
        header.add(title);
        add(header);
    }

    private void buildFilterBar() {
        filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        filterBar.setBackground(Palette.CARD);
        filterBar.setBorder(BorderFactory.createLineBorder(Palette.BORDER_CARD));

        filterBar.add(new JLabel("Period:"));
        filterBar.add(rangeCombo);
        filterBar.add(new JLabel("From:"));
        filterBar.add(fromField);
        filterBar.add(new JLabel("To:"));
        filterBar.add(toField);
        fromField.setEnabled(false);
        toField.setEnabled(false);
        rangeCombo.addActionListener(e -> {
            boolean custom = "Custom Range".equals(rangeCombo.getSelectedItem());
            fromField.setEnabled(custom);
            toField.setEnabled(custom);
            if (!custom) refresh();
        });

        JButton run = org.PSmart.POS.Utils.Components.forms.Ui.primary("Run");
        run.addActionListener(e -> refresh());
        filterBar.add(run);

        JButton payComm = org.PSmart.POS.Utils.Components.forms.Ui.primary("Pay Commission");
        payComm.setToolTipText("Staff tab: record a commission payout for the selected person");
        payComm.addActionListener(e -> payCommission());
        filterBar.add(payComm);
        JButton export = org.PSmart.POS.Utils.Components.forms.Ui.neutral("Export tab to Excel");
        export.addActionListener(e -> exportCurrent());
        filterBar.add(export);

        statusLabel.setForeground(Palette.GRAY);
        filterBar.add(statusLabel);
        add(filterBar);
    }

    private JButton button(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Courier", Font.BOLD, 12));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorderPainted(false);
        return b;
    }

    private void buildTabs() {
        tabs.setFont(new Font("Courier", Font.BOLD, 13));
        addTab("Summary", "Measure", "Amount");
        addTab("Daily Sales", "Date", "Receipts", "Sales", "Avg Receipt");
        addTab("Product Sales", "Product", "Qty Sold", "Revenue", "Cost", "Profit", "Margin %");
        addTab("Payment Methods", "Method", "Receipts", "Total");
        addTab("Stock", "Product", "On Hand", "Reorder Level", "Status", "Cost Value", "Retail Value");
        addTab("Expenses", "Category", "Entries", "Total");
        addTab("Debtors", "Customer", "Phone", "Balance", "Credit Limit");
        addTab("Staff", "Staff", "Receipts", "Sales", "Avg Receipt", "Profit", "Cancelled", "Rate %", "Commission", "Paid Out", "Balance Owed");
    }

    private void addTab(String name, String... cols) {
        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        models.put(name, m);
        JTable t = new JTable(m);
        t.setFont(new Font("Courier", Font.PLAIN, 13));
        t.setRowHeight(26);
        t.setAutoCreateRowSorter(false);
        t.getTableHeader().setFont(new Font("Courier", Font.BOLD, 13));
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        right.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        for (int c = 1; c < cols.length; c++) {
            if (!"Status".equals(cols[c]) && !"Phone".equals(cols[c])) {
                t.getColumnModel().getColumn(c).setCellRenderer(right);
            }
        }
        tabs.addTab(name, new JScrollPane(t));
    }

    // ------------------------------------------------------------------ data

    private long[] range() {
        LocalDate today = LocalDate.now();
        LocalDate from;
        LocalDate to = today;
        String sel = String.valueOf(rangeCombo.getSelectedItem());
        switch (sel) {
            case "Today": from = today; break;
            case "This Week": from = today.with(DayOfWeek.MONDAY); break;
            case "This Month": from = today.withDayOfMonth(1); break;
            case "This Year": from = today.withDayOfYear(1); break;
            case "Custom Range":
                from = org.PSmart.POS.Utils.Components.Inputs.dateOf(fromField);
                to = org.PSmart.POS.Utils.Components.Inputs.dateOf(toField);
                if (from == null || to == null || to.isBefore(from)) return null;
                break;
            default:
                return new long[]{0L, Long.MAX_VALUE};
        }
        long f = from.atStartOfDay(ZONE).toInstant().toEpochMilli();
        long t = to.plusDays(1).atStartOfDay(ZONE).toInstant().toEpochMilli() - 1;
        return new long[]{f, t};
    }

    private void refresh() {
        final long[] r = range();
        if (r == null) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText("Pick both dates from the calendar (From must not be after To).");
            return;
        }
        statusLabel.setForeground(Palette.GRAY);
        statusLabel.setText("Loading...");
        new SwingWorker<Map<String, List<Object[]>>, Void>() {
            @Override
            protected Map<String, List<Object[]>> doInBackground() {
                return load(r[0], r[1]);
            }

            @Override
            protected void done() {
                try {
                    Map<String, List<Object[]>> data = get();
                    for (Map.Entry<String, List<Object[]>> e : data.entrySet()) {
                        DefaultTableModel m = models.get(e.getKey());
                        m.setRowCount(0);
                        for (Object[] row : e.getValue()) m.addRow(row);
                    }
                    statusLabel.setForeground(Palette.GRAY);
                    statusLabel.setText("Updated " + new java.text.SimpleDateFormat("HH:mm:ss").format(new Date()));
                } catch (Exception ex) {
                    Throwable t = ex;
                    while (t.getCause() != null) t = t.getCause();
                    statusLabel.setForeground(Color.RED);
                    statusLabel.setText("Could not load reports: " + t.getMessage());
                }
            }
        }.execute();
    }

    private Map<String, List<Object[]>> load(long from, long to) {
        EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
        try {
            Map<String, List<Object[]>> out = new LinkedHashMap<>();

            // ---- core aggregates
            Object[] s = single(em.createQuery(
                    "SELECT COUNT(s), COALESCE(SUM(s.total),0), COALESCE(SUM(s.vatAmount),0), COALESCE(SUM(s.discount),0) "
                            + "FROM Sale s WHERE s.deleted=false AND (s.status IS NULL OR UPPER(s.status) NOT IN :bad) "
                            + "AND s.createdAt BETWEEN :f AND :t")
                    .setParameter("bad", VOID_STATUSES).setParameter("f", from).setParameter("t", to));
            long receipts = ((Number) s[0]).longValue();
            double sales = num(s[1]), vat = num(s[2]), discount = num(s[3]);

            Object[] c = single(em.createQuery(
                    "SELECT COALESCE(SUM(si.buyingPrice * si.quantity),0) FROM SaleItem si "
                            + "WHERE si.deleted=false AND si.sale.deleted=false "
                            + "AND (si.sale.status IS NULL OR UPPER(si.sale.status) NOT IN :bad) "
                            + "AND si.sale.createdAt BETWEEN :f AND :t")
                    .setParameter("bad", VOID_STATUSES).setParameter("f", from).setParameter("t", to));
            double cogs = num(c[0]);

            double expenses = scalar(em.createQuery(
                    "SELECT COALESCE(SUM(e.amount),0) FROM Expense e WHERE e.deleted=false AND e.createdAt BETWEEN :f AND :t")
                    .setParameter("f", from).setParameter("t", to));
            double purchases = scalar(em.createQuery(
                    "SELECT COALESCE(SUM(p.total),0) FROM Purchase p WHERE p.deleted=false AND p.createdAt BETWEEN :f AND :t")
                    .setParameter("f", from).setParameter("t", to));
            double outstanding = scalar(em.createQuery(
                    "SELECT COALESCE(SUM(c.currentBalance),0) FROM Customer c WHERE c.deleted=false AND c.currentBalance > 0"));

            double gross = sales - vat - cogs;
            List<Object[]> summary = new ArrayList<>();
            summary.add(new Object[]{"Receipts", String.valueOf(receipts)});
            summary.add(new Object[]{"Gross sales (incl. VAT)", money(sales)});
            summary.add(new Object[]{"VAT collected", money(vat)});
            summary.add(new Object[]{"Discounts given", money(discount)});
            summary.add(new Object[]{"Net sales (excl. VAT)", money(sales - vat)});
            summary.add(new Object[]{"Cost of goods sold", money(cogs)});
            summary.add(new Object[]{"Gross profit", money(gross)});
            summary.add(new Object[]{"Gross margin %", sales - vat > 0 ? String.format("%.1f%%", gross * 100 / (sales - vat)) : "-"});
            summary.add(new Object[]{"Expenses", money(expenses)});
            summary.add(new Object[]{"NET PROFIT (gross profit - expenses)", money(gross - expenses)});
            summary.add(new Object[]{"Purchases made in period", money(purchases)});
            summary.add(new Object[]{"Customer credit outstanding (now)", money(outstanding)});
            out.put("Summary", summary);

            // ---- daily
            List<?> rows = em.createQuery(
                    "SELECT s.createdAt, s.total FROM Sale s WHERE s.deleted=false "
                            + "AND (s.status IS NULL OR UPPER(s.status) NOT IN :bad) AND s.createdAt BETWEEN :f AND :t")
                    .setParameter("bad", VOID_STATUSES).setParameter("f", from).setParameter("t", to).getResultList();
            TreeMap<LocalDate, double[]> byDay = new TreeMap<>(Comparator.reverseOrder());
            for (Object o : rows) {
                Object[] r = (Object[]) o;
                if (r[0] == null) continue;
                LocalDate d = java.time.Instant.ofEpochMilli(((Number) r[0]).longValue()).atZone(ZONE).toLocalDate();
                double[] acc = byDay.computeIfAbsent(d, k -> new double[2]);
                acc[0] += 1;
                acc[1] += num(r[1]);
            }
            List<Object[]> daily = new ArrayList<>();
            for (Map.Entry<LocalDate, double[]> e : byDay.entrySet()) {
                double[] a = e.getValue();
                daily.add(new Object[]{e.getKey().toString(), (long) a[0], money(a[1]), money(a[0] > 0 ? a[1] / a[0] : 0)});
            }
            out.put("Daily Sales", daily);

            // ---- products
            List<?> prod = em.createQuery(
                    "SELECT si.item.name, SUM(si.quantity), SUM(si.total), SUM(si.buyingPrice * si.quantity) FROM SaleItem si "
                            + "WHERE si.deleted=false AND si.sale.deleted=false "
                            + "AND (si.sale.status IS NULL OR UPPER(si.sale.status) NOT IN :bad) "
                            + "AND si.sale.createdAt BETWEEN :f AND :t "
                            + "GROUP BY si.item.uid, si.item.name ORDER BY SUM(si.total) DESC")
                    .setParameter("bad", VOID_STATUSES).setParameter("f", from).setParameter("t", to).getResultList();
            List<Object[]> products = new ArrayList<>();
            for (Object o : prod) {
                Object[] r = (Object[]) o;
                double rev = num(r[2]), cost = num(r[3]), profit = rev - cost;
                products.add(new Object[]{r[0], qty(num(r[1])), money(rev), money(cost), money(profit),
                        rev > 0 ? String.format("%.1f", profit * 100 / rev) : "-"});
            }
            out.put("Product Sales", products);

            // ---- payment methods
            List<?> pay = em.createQuery(
                    "SELECT COALESCE(s.paymentMethod,'UNKNOWN'), COUNT(s), COALESCE(SUM(s.total),0) FROM Sale s "
                            + "WHERE s.deleted=false AND (s.status IS NULL OR UPPER(s.status) NOT IN :bad) "
                            + "AND s.createdAt BETWEEN :f AND :t GROUP BY s.paymentMethod ORDER BY SUM(s.total) DESC")
                    .setParameter("bad", VOID_STATUSES).setParameter("f", from).setParameter("t", to).getResultList();
            List<Object[]> payments = new ArrayList<>();
            for (Object o : pay) {
                Object[] r = (Object[]) o;
                payments.add(new Object[]{r[0], ((Number) r[1]).longValue(), money(num(r[2]))});
            }
            out.put("Payment Methods", payments);

            // ---- stock (snapshot; ignores the period)
            Map<UUID, Double> onHandMap = org.PSmart.POS.Utils.StockService.onHandAll(em);
            List<Object[]> stock = new ArrayList<>();
            for (Object o : em.createQuery(
                    "SELECT i.uid, i.name, i.reorderLevel, i.buyingPrice, i.sellingPrice FROM Item i "
                            + "WHERE i.deleted=false ORDER BY i.name").getResultList()) {
                Object[] it = (Object[]) o;
                double onHand = onHandMap.getOrDefault((UUID) it[0], 0.0);
                double reorder = it[2] == null ? 0 : num(it[2]);
                String status = onHand <= 0 ? "OUT" : (reorder > 0 && onHand < reorder ? "LOW" : "OK");
                stock.add(new Object[]{it[1], qty(onHand), qty(reorder), status,
                        money(Math.max(0, onHand) * num(it[3])), money(Math.max(0, onHand) * num(it[4]))});
            }
            stock.sort(Comparator.comparingInt((Object[] r) -> "OK".equals(r[3]) ? 1 : 0));
            out.put("Stock", stock);

            // ---- expenses
            List<?> ex = em.createQuery(
                    "SELECT COALESCE(e.category,'Uncategorised'), COUNT(e), COALESCE(SUM(e.amount),0) FROM Expense e "
                            + "WHERE e.deleted=false AND e.createdAt BETWEEN :f AND :t "
                            + "GROUP BY e.category ORDER BY SUM(e.amount) DESC")
                    .setParameter("f", from).setParameter("t", to).getResultList();
            List<Object[]> exp = new ArrayList<>();
            for (Object o : ex) {
                Object[] r = (Object[]) o;
                exp.add(new Object[]{r[0], ((Number) r[1]).longValue(), money(num(r[2]))});
            }
            out.put("Expenses", exp);

            // ---- debtors
            List<?> db = em.createQuery(
                    "SELECT c.fullname, c.phone, c.currentBalance, c.creditLimit FROM Customer c "
                            + "WHERE c.deleted=false AND c.currentBalance > 0 ORDER BY c.currentBalance DESC").getResultList();
            List<Object[]> debtors = new ArrayList<>();
            for (Object o : db) {
                Object[] r = (Object[]) o;
                debtors.add(new Object[]{r[0], r[1] == null ? "" : r[1], money(num(r[2])),
                        r[3] == null || num(r[3]) <= 0 ? "Unlimited" : money(num(r[3]))});
            }
            out.put("Debtors", debtors);
            out.put("Staff", staffRows(em, from, to));
            return out;
        } finally {
            em.close();
        }
    }

    // ------------------------------------------------------------------ staff performance + commission

    private volatile List<Object[]> staffMeta = new ArrayList<>();   // {uid, name, owed}

    private Map<UUID, double[]> perStaff(EntityManager em, long from, long to) {
        // {receipts, sales, profit, cancelled}
        Map<UUID, double[]> m = new LinkedHashMap<>();
        for (Object o : em.createQuery("SELECT u.uid, COUNT(s), COALESCE(SUM(s.total),0) FROM Sale s JOIN s.cashier u "
                + "WHERE s.deleted=false AND (s.status IS NULL OR UPPER(s.status) NOT IN :bad) AND s.createdAt BETWEEN :f AND :t GROUP BY u.uid")
                .setParameter("bad", VOID_STATUSES).setParameter("f", from).setParameter("t", to).getResultList()) {
            Object[] r = (Object[]) o;
            m.computeIfAbsent((UUID) r[0], k -> new double[4]);
            m.get((UUID) r[0])[0] = num(r[1]);
            m.get((UUID) r[0])[1] = num(r[2]);
        }
        for (Object o : em.createQuery("SELECT s.cashier.uid, COALESCE(SUM(si.total - si.buyingPrice * si.quantity),0) FROM SaleItem si JOIN si.sale s "
                + "WHERE si.deleted=false AND s.deleted=false AND s.cashier IS NOT NULL AND (s.status IS NULL OR UPPER(s.status) NOT IN :bad) "
                + "AND s.createdAt BETWEEN :f AND :t GROUP BY s.cashier.uid")
                .setParameter("bad", VOID_STATUSES).setParameter("f", from).setParameter("t", to).getResultList()) {
            Object[] r = (Object[]) o;
            m.computeIfAbsent((UUID) r[0], k -> new double[4]);
            m.get((UUID) r[0])[2] = num(r[1]);
        }
        for (Object o : em.createQuery("SELECT s.cashier.uid, COUNT(s) FROM Sale s WHERE s.deleted=false AND s.cashier IS NOT NULL "
                + "AND UPPER(s.status) IN :bad AND s.createdAt BETWEEN :f AND :t GROUP BY s.cashier.uid")
                .setParameter("bad", VOID_STATUSES).setParameter("f", from).setParameter("t", to).getResultList()) {
            Object[] r = (Object[]) o;
            m.computeIfAbsent((UUID) r[0], k -> new double[4]);
            m.get((UUID) r[0])[3] = num(r[1]);
        }
        return m;
    }

    private List<Object[]> staffRows(EntityManager em, long from, long to) {
        org.PSmart.POS.Entities.StoreConfig cfg = DatabaseUtils.getInstance().getStoreConfigRepository().getStoreConfig();
        boolean on = cfg != null && Boolean.TRUE.equals(cfg.getCommissionEnabled());
        boolean byProfit = cfg != null && "PROFIT".equals(cfg.getCommissionBasis());
        double defRate = cfg != null && cfg.getCommissionRate() != null ? cfg.getCommissionRate() : 0;

        Map<UUID, double[]> period = perStaff(em, from, to);
        Map<UUID, double[]> life = perStaff(em, 0L, Long.MAX_VALUE);
        Map<UUID, Object[]> users = new HashMap<>();
        for (Object o : em.createQuery("SELECT u.uid, u.fullname, u.commissionRate FROM User u").getResultList()) {
            Object[] r = (Object[]) o;
            users.put((UUID) r[0], r);
        }
        Map<UUID, Double> paid = new HashMap<>();
        for (Object o : em.createQuery("SELECT p.userUid, COALESCE(SUM(p.amount),0) FROM CommissionPayout p WHERE p.deleted=false GROUP BY p.userUid").getResultList()) {
            Object[] r = (Object[]) o;
            paid.put((UUID) r[0], num(r[1]));
        }
        Set<UUID> ids = new LinkedHashSet<>(period.keySet());
        ids.addAll(life.keySet());
        List<Object[]> rows = new ArrayList<>();
        List<Object[]> meta = new ArrayList<>();
        for (UUID id : ids) {
            double[] p = period.getOrDefault(id, new double[4]);
            double[] l = life.getOrDefault(id, new double[4]);
            Object[] u = users.get(id);
            String name = u == null ? "(removed user)" : String.valueOf(u[1]);
            double rate = u != null && u[2] != null ? num(u[2]) : defRate;
            double earnedPeriod = on ? (byProfit ? p[2] : p[1]) * rate / 100.0 : 0;
            double earnedLife = on ? (byProfit ? l[2] : l[1]) * rate / 100.0 : 0;
            double paidOut = paid.getOrDefault(id, 0.0);
            double owed = Math.max(0, earnedLife - paidOut);
            rows.add(new Object[]{name, (long) p[0], money(p[1]), money(p[0] > 0 ? p[1] / p[0] : 0), money(p[2]), (long) p[3],
                    on ? String.format("%.1f", rate) : "-", on ? money(earnedPeriod) : "off", on ? money(paidOut) : "-", on ? money(owed) : "-"});
            meta.add(new Object[]{id, name, owed});
        }
        staffMeta = meta;
        return rows;
    }

    private void payCommission() {
        if (!"Staff".equals(tabs.getTitleAt(tabs.getSelectedIndex()))) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText("Open the Staff tab and select a person first.");
            return;
        }
        JTable t = (JTable) ((JScrollPane) tabs.getSelectedComponent()).getViewport().getView();
        int r = t.getSelectedRow();
        if (r < 0 || r >= staffMeta.size()) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText("Select a person in the Staff list first.");
            return;
        }
        Object[] m = staffMeta.get(r);
        if (org.PSmart.POS.Utils.SupervisorApproval.require(this, sessionUser, org.PSmart.POS.Entities.RoleType.REPORTS, "pay commission") == null) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText("Paying commission needs approval from someone who can.");
            return;
        }
        new CommissionPayForm((UUID) m[0], (String) m[1], (Double) m[2], this::refresh);
    }

    private void exportCurrent() {
        String name = tabs.getTitleAt(tabs.getSelectedIndex());
        DefaultTableModel m = models.get(name);
        String[] headers = new String[m.getColumnCount()];
        for (int i = 0; i < headers.length; i++) headers[i] = m.getColumnName(i);
        List<Object[]> rows = new ArrayList<>();
        for (int r = 0; r < m.getRowCount(); r++) {
            Object[] row = new Object[headers.length];
            for (int c = 0; c < headers.length; c++) row[c] = m.getValueAt(r, c);
            rows.add(row);
        }
        ExcelExporter.exportToExcel(this, "Report-" + name.replace(' ', '_') + "-" + LocalDate.now(), headers, rows);
    }

    // ------------------------------------------------------------------ helpers

    private static Object[] single(Query q) {
        Object o = q.getSingleResult();
        return o instanceof Object[] ? (Object[]) o : new Object[]{o};
    }

    private static double scalar(Query q) {
        return num(q.getSingleResult());
    }

    private static double num(Object o) {
        return o instanceof Number ? ((Number) o).doubleValue() : 0;
    }

    private static String money(double v) {
        return String.format("%,.2f", v);
    }

    private static String qty(double v) {
        return v == Math.floor(v) ? String.format("%,.0f", v) : String.format("%,.2f", v);
    }
}
