package org.PSmart.POS.ui.MainView.ui.cashier;

import org.PSmart.POS.Entities.Customer;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

import static org.PSmart.POS.ui.MainView.ui.cashier.CashierTheme.*;

/**
 * In-page checkout (replaces the old pop-up). Start with Cash only; add M-Pesa, Card or Credit
 * only when needed (progressive disclosure). Everything validates inline.
 */
final class PaymentPage extends JPanel {

    static final class Result {
        double cash, mpesa, card, credit;
        boolean pointsRedeemed;
    }

    private static final int CASH = 0, MPESA = 1, CARD = 2, CREDIT = 3;
    private static final String[] NAMES = {"Cash", "M-Pesa", "Card", "Credit (on account)"};

    private final double baseDue;
    private final Customer customer;
    private final double pointsUsable, pointsWorth;
    private final double creditAvailable;      // Double.MAX_VALUE = unlimited
    private final Runnable onBack;
    private final Consumer<Result> onConfirm;

    private double due;
    private final JTextField[] fields = new JTextField[4];
    private final JPanel[] rows = new JPanel[4];
    private final JButton[] chips = new JButton[4];
    private final boolean[] active = new boolean[4];

    private final JLabel dueLabel = new JLabel();
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel errorLabel = new JLabel(" ");
    private final JButton payBtn;
    private final JPanel rowsPanel = new JPanel();
    private final JPanel quickCash = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private JCheckBox usePoints;
    private boolean busy;

    PaymentPage(double due, Customer customer, double pointsUsable, double pointsWorth,
                double creditAvailable, Runnable onBack, Consumer<Result> onConfirm) {
        this.baseDue = due;
        this.due = due;
        this.customer = customer;
        this.pointsUsable = pointsUsable;
        this.pointsWorth = pointsWorth;
        this.creditAvailable = creditAvailable;
        this.onBack = onBack;
        this.onConfirm = onConfirm;

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(22, 34, 18, 34));

        payBtn = button("PAY & FINISH", GREEN, 400, 56, 19);
        add(buildTop(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        for (int i = 0; i < 4; i++) rows[i].setVisible(false);
        setActive(CASH, true, due);
        refresh();
    }

    // ------------------------------------------------------------------ build

    private JPanel buildTop() {
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(label("Payment", 26, true, NAVY));
        String who = customer == null ? "Walk-in customer"
                : customer.getFullname() + (customer.getCurrentBalance() != null && customer.getCurrentBalance() > 0.005
                ? "   -   owes KES " + money(customer.getCurrentBalance()) : "");
        JLabel w = label(who, 14, false, MUTED);
        w.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        left.add(w);
        top.add(left, BorderLayout.WEST);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        JLabel dl = label("AMOUNT DUE", 12, true, MUTED);
        dl.setAlignmentX(Component.RIGHT_ALIGNMENT);
        dueLabel.setFont(new Font("Courier", Font.BOLD, 38));
        dueLabel.setForeground(NAVY);
        dueLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        right.add(dl);
        right.add(dueLabel);
        top.add(right, BorderLayout.EAST);

        top.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                BorderFactory.createEmptyBorder(0, 0, 14, 0)));
        return top;
    }

    private JPanel buildCenter() {
        JPanel c = new JPanel();
        c.setOpaque(false);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        if (pointsUsable >= 1) {
            usePoints = new JCheckBox("Use " + String.format("%.0f", pointsUsable) + " loyalty points   (-KES "
                    + money(pointsWorth) + ")");
            usePoints.setFont(new Font("Courier", Font.BOLD, 14));
            usePoints.setForeground(NAVY);
            usePoints.setOpaque(false);
            usePoints.setAlignmentX(Component.LEFT_ALIGNMENT);
            usePoints.addActionListener(e -> {
                due = usePoints.isSelected() ? Math.max(0, baseDue - pointsWorth) : baseDue;
                refresh();
            });
            c.add(usePoints);
            c.add(Box.createVerticalStrut(12));
        }

        JLabel pw = label("Pay with  (tap to add or remove a method)", 14, true, SLATE);
        pw.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.add(pw);
        c.add(Box.createVerticalStrut(8));

        JPanel chipRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        chipRow.setOpaque(false);
        chipRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        chipRow.setPreferredSize(new Dimension(0, 46));
        chipRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        for (int i = 0; i < 4; i++) {
            final int m = i;
            JButton b = new JButton(NAMES[i]) {
                @Override protected void paintComponent(Graphics g) {
                    g.setColor(getBackground());
                    g.fillRect(0, 0, getWidth(), getHeight());
                    super.paintComponent(g);
                }
            };
            b.setContentAreaFilled(false);
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setFont(new Font("Courier", Font.BOLD, 14));
            b.setPreferredSize(new Dimension(i == CREDIT ? 210 : 130, 40));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addActionListener(e -> toggle(m));
            chips[i] = b;
            chipRow.add(b);
        }
        c.add(chipRow);
        c.add(Box.createVerticalStrut(14));

        rowsPanel.setOpaque(false);
        rowsPanel.setLayout(new BoxLayout(rowsPanel, BoxLayout.Y_AXIS));
        rowsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (int i = 0; i < 4; i++) {
            rows[i] = buildRow(i);
            rowsPanel.add(rows[i]);
            rowsPanel.add(Box.createVerticalStrut(8));
        }
        c.add(rowsPanel);

        quickCash.setOpaque(false);
        quickCash.setAlignmentX(Component.LEFT_ALIGNMENT);
        quickCash.add(label("Quick cash:", 13, true, MUTED));
        quickCash.add(quick("Exact", -1));
        for (int amt : new int[]{100, 200, 500, 1000, 2000}) quickCash.add(quick(String.valueOf(amt), amt));
        c.add(Box.createVerticalStrut(2));
        c.add(quickCash);
        c.add(Box.createVerticalStrut(16));

        statusLabel.setFont(new Font("Courier", Font.BOLD, 26));
        statusLabel.setOpaque(true);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(14, 10, 14, 10));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        c.add(statusLabel);
        c.add(Box.createVerticalStrut(8));

        errorLabel.setFont(new Font("Courier", Font.BOLD, 14));
        errorLabel.setForeground(RED);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.add(errorLabel);
        c.add(Box.createVerticalGlue());
        return c;
    }

    private JPanel buildRow(int m) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(true);
        row.setBackground(ROW_ALT);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        row.setPreferredSize(new Dimension(0, 64));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel name = label(NAMES[m], 16, true, SLATE);
        name.setPreferredSize(new Dimension(200, 40));
        row.add(name, BorderLayout.WEST);

        JTextField f = new JTextField();
        f.setFont(new Font("Courier", Font.BOLD, 24));
        f.setHorizontalAlignment(JTextField.RIGHT);
        f.setForeground(NAVY);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, NAVY),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)));
        f.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
        });
        f.addActionListener(e -> confirm());
        org.PSmart.POS.Utils.Components.Inputs.decimal(f);
        fields[m] = f;
        row.add(f, BorderLayout.CENTER);

        JPanel acts = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        acts.setOpaque(false);
        JButton rest = outline("Fill rest", BLUE, 34, 12);
        rest.setPreferredSize(new Dimension(100, 34));
        rest.addActionListener(e -> {
            double r = remainingExcluding(m);
            f.setText(r > 0.005 ? String.format("%.2f", r) : "");
            f.requestFocusInWindow();
        });
        acts.add(rest);
        row.add(acts, BorderLayout.EAST);

        if (m == CREDIT) {
            String hint = customer == null ? "" : (creditAvailable == Double.MAX_VALUE
                    ? "No credit limit" : "Available credit: KES " + money(creditAvailable));
            JLabel h = label(hint, 12, false, MUTED);
            row.add(h, BorderLayout.SOUTH);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 84));
            row.setPreferredSize(new Dimension(0, 84));
        }
        return row;
    }

    private JButton quick(String text, int amount) {
        JButton b = outline(text, BLUE, 32, 13);
        b.setPreferredSize(new Dimension(amount < 0 ? 80 : 70, 32));
        b.setMaximumSize(new Dimension(amount < 0 ? 80 : 70, 32));
        b.addActionListener(e -> {
            double v = amount < 0 ? Math.max(0, due - others(CASH)) : amount;
            fields[CASH].setText(String.format("%.2f", v));
            fields[CASH].requestFocusInWindow();
        });
        return b;
    }

    private JPanel buildFooter() {
        JPanel f = new JPanel(new BorderLayout());
        f.setOpaque(false);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
                BorderFactory.createEmptyBorder(14, 0, 0, 0)));
        JButton back = button("<  Back to cart  (Esc)", SLATE, 260, 56, 16);
        back.addActionListener(e -> { if (!busy) onBack.run(); });
        f.add(back, BorderLayout.WEST);
        payBtn.addActionListener(e -> confirm());
        f.add(payBtn, BorderLayout.EAST);
        return f;
    }

    // ------------------------------------------------------------------ behaviour

    private void toggle(int m) {
        if (busy) return;
        if (active[m]) {
            int count = 0;
            for (boolean a : active) if (a) count++;
            if (count <= 1) {
                error("At least one payment method is needed.");
                return;
            }
            setActive(m, false, 0);
        } else {
            if (m == CREDIT && customer == null) {
                error("Select a customer (top right of the cashier) to sell on credit.");
                return;
            }
            double rest = Math.max(0, due - others(m));
            setActive(m, true, rest);
        }
        refresh();
    }

    private void setActive(int m, boolean on, double prefill) {
        active[m] = on;
        rows[m].setVisible(on);
        if (on) {
            fields[m].setText(prefill > 0.005 ? String.format("%.2f", prefill) : "");
            fields[m].requestFocusInWindow();
            fields[m].selectAll();
        } else {
            fields[m].setText("");
        }
        quickCash.setVisible(active[CASH]);
    }

    private double val(int m) {
        if (!active[m]) return 0;
        try {
            String t = fields[m].getText().trim().replace(",", "");
            return t.isEmpty() ? 0 : Math.max(0, Double.parseDouble(t));
        } catch (NumberFormatException e) {
            return -1;     // invalid marker
        }
    }

    private double others(int except) {
        double s = 0;
        for (int i = 0; i < 4; i++) if (i != except) s += Math.max(0, val(i));
        return s;
    }

    private double remainingExcluding(int m) {
        return Math.max(0, due - others(m));
    }

    private void error(String msg) {
        errorLabel.setText(msg == null || msg.isEmpty() ? " " : msg);
    }

    private String check() {
        for (int i = 0; i < 4; i++) if (active[i] && val(i) < 0) return "Enter a valid amount for " + NAMES[i] + ".";
        double cash = val(CASH), mp = val(MPESA), card = val(CARD), credit = val(CREDIT);
        double sum = cash + mp + card + credit;
        if (sum < due - 0.005) return null;                   // not complete yet, no error
        if (credit > 0.005) {
            if (customer == null) return "Credit needs a customer.";
            double maxCredit = Math.max(0, due - (cash + mp + card));
            if (credit > maxCredit + 0.005) return "Credit is more than the amount still due (max " + money(maxCredit) + ").";
            if (credit > creditAvailable + 0.005) return "Over the credit limit - available credit is KES " + money(Math.max(0, creditAvailable)) + ".";
        }
        double over = sum - due;
        if (over > 0.005 && cash + 0.005 < over) return "Overpayment can only be returned as cash change.";
        return null;
    }

    private void refresh() {
        dueLabel.setText("KES " + money(due));
        for (int i = 0; i < 4; i++) {
            chips[i].setBackground(active[i] ? NAVY : CHIP_OFF);
            chips[i].setForeground(active[i] ? Color.WHITE : SLATE);
        }
        double sum = 0;
        boolean invalid = false;
        for (int i = 0; i < 4; i++) {
            double v = val(i);
            if (v < 0) invalid = true; else sum += v;
        }
        double remaining = due - sum;
        String problem = invalid ? "Enter a valid amount." : check();
        error(problem);
        boolean complete = !invalid && remaining <= 0.005 && problem == null;
        if (remaining > 0.005) {
            statusLabel.setText("Remaining  KES " + money(remaining));
            statusLabel.setForeground(RED);
            statusLabel.setBackground(new Color(253, 236, 238));
        } else {
            double change = Math.max(0, sum - due);
            statusLabel.setText(change > 0.005 ? "Change due  KES " + money(change) : "Fully paid");
            statusLabel.setForeground(GREEN.darker());
            statusLabel.setBackground(new Color(232, 246, 236));
        }
        payBtn.setEnabled(complete && !busy);
        payBtn.setText(complete ? "PAY & FINISH  KES " + money(due) : "PAY & FINISH");
        revalidate();
        repaint();
    }

    /** F4 / Enter / button. */
    void confirm() {
        if (busy || !payBtn.isEnabled()) {
            if (!busy && payBtn.getText().startsWith("PAY") && errorLabel.getText().trim().isEmpty()) {
                error("Add payments that cover the amount due.");
            }
            return;
        }
        busy = true;
        payBtn.setEnabled(false);
        Result r = new Result();
        r.cash = Math.max(0, val(CASH));
        r.mpesa = Math.max(0, val(MPESA));
        r.card = Math.max(0, val(CARD));
        r.credit = Math.max(0, val(CREDIT));
        r.pointsRedeemed = usePoints != null && usePoints.isSelected();
        onConfirm.accept(r);
    }

    void back() { if (!busy) onBack.run(); }

    void unlock() { busy = false; refresh(); }

    void focusFirst() { SwingUtilities.invokeLater(() -> { fields[CASH].requestFocusInWindow(); fields[CASH].selectAll(); }); }
}
