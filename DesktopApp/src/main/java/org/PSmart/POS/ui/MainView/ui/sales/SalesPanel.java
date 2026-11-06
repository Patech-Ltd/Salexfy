package org.PSmart.POS.ui.MainView.ui.sales;

import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.Sale;
import org.PSmart.POS.Repositories.SaleRepository;
import org.PSmart.POS.Utils.Components.PaginationBar;
import org.PSmart.POS.Utils.ExcelExporter;
import org.PSmart.POS.Utils.MyUtils;
import org.PSmart.POS.Utils.Palette;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Sales Reports — professional, card-based. Searchable by receipt# / customer,
 * filterable by date range, paginated with progressive loading, exportable to Excel,
 * and each sale opens a printable + editable details dialog.
 */
public class SalesPanel extends JPanel {

    private static final int PAGE_SIZE = 20;

    private final SaleRepository saleRepository = DatabaseUtils.getInstance().getSaleRepository();

    private DefaultTableModel salesTableModel;
    private JTable salesTable;

    private JLabel todayRevenueLabel;
    private JLabel todayCountLabel;
    private JLabel totalSalesLabel;
    private JLabel pageCountLabel;

    private JTextField searchField;
    private JComboBox<String> rangeCombo;
    private PaginationBar paginationBar;

    private int page = 0;
    private String keyword = "";
    private long totalRows = 0;
    private boolean loading = false;

    private static final String[] COLS =
            {"Receipt #", "Date/Time", "Items", "Subtotal", "VAT", "Total", "Payment", "Status"};

    public SalesPanel() {
        super();
        setLayout(null);
        setBackground(Palette.BG);
        buildComponents();
        SwingUtilities.invokeLater(this::applyFilters);
        layoutUi();
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        if (width > 0) layoutUi();
    }

    /** Re-positions all children. Safe to call repeatedly once width>0. */
    private void layoutUi() {
        if (getWidth() <= 1 || getHeight() <= 1) return;

        if (mainHeader != null) mainHeader.setBounds(0, 0, getWidth(), 60);
        if (summaryBar != null) summaryBar.setBounds(20, 70, getWidth() - 40, 100);
        if (filterBar != null) filterBar.setBounds(20, 184, getWidth() - 40, 58);
        if (tableScroll != null) tableScroll.setBounds(20, 258, getWidth() - 40, getHeight() - 258 - 66);
        if (paginationBar != null) paginationBar.setBounds(20, getHeight() - 60, getWidth() - 40, 52);

        revalidate();
        repaint();
    }

    // ---------------------------------------------------------------- component fields
    private JPanel mainHeader;
    private JPanel summaryBar;
    private JPanel filterBar;
    private JScrollPane tableScroll;

    private void buildComponents() {
        buildHeader();
        buildSummaryBar();
        buildFilterBar();
        buildTable();
        buildPagination();
    }

    // ---------------------------------------------------------------- header
    private void buildHeader() {
        mainHeader = new JPanel(null);
        mainHeader.setBackground(Palette.HEADER);

        JLabel title = new JLabel("Sales Reports");
        title.setFont(Palette.courier(22));
        title.setForeground(Color.WHITE);
        title.setBounds(20, 0, 300, 60);
        mainHeader.add(title);

        JButton exportBtn = btn("Export to Excel", Palette.GREEN);
        exportBtn.addActionListener(e -> exportCurrent());
        mainHeader.add(exportBtn);

        JButton refreshBtn = btn("Refresh", Palette.BLUE_BTN);
        refreshBtn.addActionListener(e -> applyFilters());
        mainHeader.add(refreshBtn);

        JLabel tag = new JLabel("PSMART POS", SwingConstants.RIGHT);
        tag.setFont(Palette.courier(12));
        tag.setForeground(new Color(200, 200, 200));
        tag.setBounds(20, 40, 500, 20);
        mainHeader.add(tag);

        add(mainHeader);
    }

    private JButton btn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Courier", Font.BOLD, 12));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setSize(132, 32);
        return b;
    }

    // ---------------------------------------------------------------- summary cards
    private void buildSummaryBar() {
        summaryBar = new JPanel(new GridLayout(1, 3, 14, 0));
        summaryBar.setBackground(Palette.BG);

        todayRevenueLabel = new JLabel("KES 0.00", SwingConstants.CENTER);
        JPanel revCard = card("Today's Revenue", todayRevenueLabel, Palette.GREEN);

        todayCountLabel = new JLabel("0", SwingConstants.CENTER);
        JPanel cntCard = card("Today's Sales", todayCountLabel, Palette.BLUE_ACTIVE);

        totalSalesLabel = new JLabel("0", SwingConstants.CENTER);
        JPanel totCard = card("Total Sales (All Time)", totalSalesLabel, Palette.PURPLE);

        summaryBar.add(revCard);
        summaryBar.add(cntCard);
        summaryBar.add(totCard);
        add(summaryBar);
    }

    private JPanel card(String title, JLabel value, Color accent) {
        JPanel c = new JPanel(null);
        c.setBackground(Color.WHITE);
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Palette.BORDER_CARD),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));

        JPanel stripe = new JPanel();
        stripe.setBackground(accent);
        stripe.setBounds(0, 0, 5, 100);
        c.add(stripe);

        JLabel t = new JLabel(title);
        t.setFont(new Font("Courier", Font.PLAIN, 12));
        t.setForeground(Palette.GRAY_DIM);
        t.setBounds(16, 12, 260, 20);
        c.add(t);

        value.setFont(new Font("Courier", Font.BOLD, 24));
        value.setForeground(accent);
        value.setBounds(16, 38, 280, 40);
        c.add(value);

        return c;
    }

    // ---------------------------------------------------------------- filter bar
    private void buildFilterBar() {
        filterBar = new JPanel(null);
        filterBar.setBackground(Color.WHITE);
        filterBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Palette.BORDER_CARD),
                BorderFactory.createEmptyBorder()));

        searchField = new JTextField();
        searchField.setFont(new Font("Courier", Font.PLAIN, 14));
        searchField.setBounds(16, 12, 360, 32);
        searchField.setBorder(BorderFactory.createLineBorder(Palette.BORDER_SUBTLE));
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) {
                keyword = searchField.getText().trim();
                page = 0;
                loadPage();
            }
        });
        filterBar.add(searchField);

        JLabel searchTag = new JLabel("Search Receipt # or Customer:");
        searchTag.setFont(Palette.courier(13));
        searchTag.setForeground(Palette.GRAY_DIM);
        searchTag.setBounds(16, -16, 360, 20);
        filterBar.add(searchTag);

        rangeCombo = new JComboBox<>(new String[]{"All Time", "Today", "This Week", "This Month", "Custom Range"});
        rangeCombo.setFont(new Font("Courier", Font.PLAIN, 13));
        rangeCombo.setBounds(400, 12, 200, 32);
        rangeCombo.addActionListener(e -> { page = 0; loadPage(); });
        filterBar.add(rangeCombo);

        JLabel rangeTag = new JLabel("Period:");
        rangeTag.setFont(Palette.courier(13));
        rangeTag.setForeground(Palette.GRAY_DIM);
        rangeTag.setBounds(400, -16, 120, 20);
        filterBar.add(rangeTag);

        pageCountLabel = new JLabel("", SwingConstants.RIGHT);
        pageCountLabel.setFont(Palette.courier(13));
        pageCountLabel.setForeground(Palette.GRAY_DIM);
        pageCountLabel.setBounds(0, 12, 0, 32);
        filterBar.add(pageCountLabel);

        add(filterBar);
    }

    // ---------------------------------------------------------------- table
    private void buildTable() {
        salesTableModel = new DefaultTableModel(COLS, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        salesTable = new JTable(salesTableModel);
        salesTable.setRowHeight(40);
        salesTable.setFont(new Font("Courier", Font.PLAIN, 13));
        salesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        salesTable.setGridColor(Palette.GRID);
        salesTable.setShowGrid(true);
        salesTable.setIntercellSpacing(new Dimension(1, 1));
        salesTable.setFocusable(false);

        JTableHeader th = salesTable.getTableHeader();
        th.setFont(Palette.courier(13));
        th.setBackground(Palette.HEADER);
        th.setForeground(Color.WHITE);
        th.setPreferredSize(new Dimension(0, 38));

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(JLabel.CENTER);
        DefaultTableCellRenderer zebra = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, focus, row, col);
                if (!sel) c.setBackground(row % 2 == 0 ? Color.WHITE : Palette.ROW_STRIPE);
                return c;
            }
        };
        zebra.setHorizontalAlignment(JLabel.LEFT);

        for (int i = 0; i < COLS.length; i++) {
            boolean left = i == 0 || i == 1;
            salesTable.getColumnModel().getColumn(i).setCellRenderer(left ? zebra : center);
            salesTable.getColumnModel().getColumn(i).setPreferredWidth(i == 1 ? 170 : 90);
        }

        salesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) openDetails();
            }
        });

        tableScroll = new JScrollPane(salesTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(Color.WHITE);
        add(tableScroll);
    }

    // ---------------------------------------------------------------- pagination
    private void buildPagination() {
        paginationBar = new PaginationBar(newP -> {
            page = newP;
            loadPage();
        });
        paginationBar.setPageSize(PAGE_SIZE);
        add(paginationBar);
    }

    // ---------------------------------------------------------------- data
    private void applyFilters() {
        page = 0;
        loadPage();
    }

    private void loadPage() {
        if (loading) return;
        loading = true;
        new Thread(this::fetch).start();
    }

    private void fetch() {
        try {
            Long from = rangeFrom(rangeCombo != null ? (String) rangeCombo.getSelectedItem() : "All Time");
            Long to = null;

            final List<Sale> rows;
            final long total;

            if (keyword != null && !keyword.isEmpty()) {
                rows = saleRepository.findSalesByKeywordPage(keyword, page, PAGE_SIZE);
                total = saleRepository.countSalesByKeyword(keyword);
            } else if (from != null) {
                to = System.currentTimeMillis();
                rows = saleRepository.findSalesByDateRangePage(from, to, page, PAGE_SIZE);
                total = saleRepository.countSalesByDateRange(from, to);
            } else {
                rows = saleRepository.findSalesPage(page, PAGE_SIZE);
                total = saleRepository.countSales();
            }

            SwingUtilities.invokeLater(() -> render(rows, total));
        } catch (Exception ex) {
            ex.printStackTrace();
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "Error loading sales: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            });
        } finally {
            loading = false;
        }
    }

    private void render(List<Sale> rows, long total) {
        totalRows = total;
        salesTableModel.setRowCount(0);
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm");

        if (rows != null) {
            for (Sale s : rows) {
                String date = s.getCreatedAt() != null ? sdf.format(new Date(s.getCreatedAt())) : "N/A";
                int items = s.getItems() != null ? s.getItems().size() : 0;
                salesTableModel.addRow(new Object[]{
                        s.getReceiptNumber(),
                        date,
                        items,
                        "KES " + MyUtils.formatCurrency(String.valueOf(s.getSubtotal())),
                        "KES " + MyUtils.formatCurrency(String.valueOf(s.getVatAmount())),
                        "KES " + MyUtils.formatCurrency(String.valueOf(s.getTotal())),
                        s.getPaymentMethod(),
                        s.getStatus()
                });
            }
        }

        paginationBar.setTotalRows(total);
        paginationBar.setPage(page);
        pageCountLabel.setText("Row " + (page == 0 ? 1 : page * PAGE_SIZE + 1) + " — " +
                Math.min((page + 1) * PAGE_SIZE, total) + " of " + total +
                (keyword.isEmpty() ? "" : "  (filtered)"));
        refreshSummary();
    }

    private void refreshSummary() {
        try {
            todayRevenueLabel.setText("KES " + MyUtils.formatCurrency(String.valueOf(saleRepository.getTotalSalesToday())));
            todayCountLabel.setText(String.valueOf(saleRepository.getSalesCountToday()));
            SaleRepository repo = saleRepository;
            long c = repo.countSales();
            totalSalesLabel.setText(String.valueOf(c));
        } catch (Exception ex) { /* non-fatal */ }
    }

    private Long rangeFrom(String sel) {
        if (sel == null || sel.equals("All Time")) return null;
        java.util.Calendar cal = java.util.Calendar.getInstance();
        switch (sel) {
            case "Today":
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                cal.set(java.util.Calendar.MINUTE, 0);
                cal.set(java.util.Calendar.SECOND, 0);
                cal.set(java.util.Calendar.MILLISECOND, 0);
                return cal.getTimeInMillis();
            case "This Week":
                cal.set(java.util.Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                cal.set(java.util.Calendar.MINUTE, 0);
                cal.set(java.util.Calendar.SECOND, 0);
                cal.set(java.util.Calendar.MILLISECOND, 0);
                return cal.getTimeInMillis();
            case "This Month":
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                cal.set(java.util.Calendar.MINUTE, 0);
                cal.set(java.util.Calendar.SECOND, 0);
                cal.set(java.util.Calendar.MILLISECOND, 0);
                return cal.getTimeInMillis();
            default:
                return null;
        }
    }

    private void openDetails() {
        int row = salesTable.getSelectedRow();
        if (row < 0) return;
        String receipt = (String) salesTableModel.getValueAt(row, 0);
        java.util.UUID id = findIdByReceipt(receipt);
        if (id == null) return;
        Sale sale = saleRepository.findSaleById(id);
        if (sale != null) {
            new SaleDetailDialog(SwingUtilities.getWindowAncestor(this), sale, this::applyFilters).setVisible(true);
        }
    }

    private java.util.UUID findIdByReceipt(String receipt) {
        List<Sale> sales = safeAll();
        for (Sale s : sales) {
            if (s.getReceiptNumber().equals(receipt)) return s.getUid();
        }
        return null;
    }

    private List<Sale> safeAll() {
        try {
            return new ArrayList<>(saleRepository.findAllSales());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void exportCurrent() {
        java.util.List<Object[]> rows = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm");
        for (Sale s : safeAll()) {
            String date = s.getCreatedAt() != null ? sdf.format(new Date(s.getCreatedAt())) : "N/A";
            int items = s.getItems() != null ? s.getItems().size() : 0;
            rows.add(new Object[]{
                    s.getReceiptNumber(), date, items,
                    s.getSubtotal(), s.getVatAmount(), s.getTotal(),
                    s.getPaymentMethod(), s.getStatus()
            });
        }
        String[] headers = {"Receipt #", "Date/Time", "Items", "Subtotal", "VAT", "Total", "Payment", "Status"};
        ExcelExporter.exportToExcel(this, "SalesReport_" + java.time.LocalDate.now(), headers, rows);
    }
}