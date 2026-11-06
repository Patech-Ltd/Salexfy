package org.PSmart.POS.ui.MainView.ui.cashier;

import javax.swing.*;
import java.awt.*;

import static org.PSmart.POS.ui.MainView.ui.cashier.CashierTheme.*;

/** Shown after a sale is saved: receipt number, change to hand back, and the next step. */
final class SaleCompletePage extends JPanel {

    SaleCompletePage(String receiptNo, double total, String paidBy, double change, double onCredit,
                     String customerLine, long pointsEarned, Runnable onNewSale, Runnable onPrint, Runnable onPreview, Runnable onInvoice, Runnable onViewSales) {
        setLayout(new GridBagLayout());
        setBackground(Color.WHITE);

        JPanel card = new JPanel();
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JComponent tick = new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GREEN);
                g2.fillOval(0, 0, 84, 84);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawPolyline(new int[]{24, 38, 62}, new int[]{44, 58, 28}, 3);
                g2.dispose();
            }
        };
        tick.setPreferredSize(new Dimension(84, 84));
        tick.setMaximumSize(new Dimension(84, 84));
        tick.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(tick);
        card.add(Box.createVerticalStrut(16));

        card.add(center(label("Sale Complete", 30, true, NAVY)));
        card.add(Box.createVerticalStrut(4));
        card.add(center(label("Receipt " + receiptNo, 15, false, MUTED)));
        card.add(Box.createVerticalStrut(22));

        card.add(center(label("TOTAL", 12, true, MUTED)));
        card.add(center(label("KES " + money(total), 40, true, NAVY)));
        card.add(Box.createVerticalStrut(12));

        if (change > 0.005) {
            JLabel ch = label("Change to give:  KES " + money(change), 26, true, GREEN.darker());
            ch.setOpaque(true);
            ch.setBackground(new Color(232, 246, 236));
            ch.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
            card.add(center(ch));
            card.add(Box.createVerticalStrut(12));
        }
        card.add(center(label("Paid by " + paidBy, 15, false, SLATE)));
        if (onCredit > 0.005) {
            card.add(center(label("Added to " + customerLine + "'s account: KES " + money(onCredit), 15, true, RED)));
        } else if (customerLine != null && !customerLine.isEmpty()) {
            card.add(center(label("Customer: " + customerLine, 15, false, SLATE)));
        }
        if (pointsEarned > 0) {
            card.add(center(label("+" + pointsEarned + " loyalty points earned", 14, true, BLUE)));
        }
        card.add(Box.createVerticalStrut(28));

        JButton next = button("NEW SALE   (Enter)", BLUE, 320, 60, 20);
        next.addActionListener(e -> onNewSale.run());
        card.add(center(next));
        card.add(Box.createVerticalStrut(12));

        JPanel sec = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        sec.setOpaque(false);
        JButton print = button("Print receipt", SLATE, 170, 40, 14);
        print.addActionListener(e -> onPrint.run());
        JButton prev = button("Preview", SLATE, 120, 40, 14);
        prev.addActionListener(e -> onPreview.run());
        JButton inv = button("Invoice (A4)", SLATE, 150, 40, 14);
        inv.addActionListener(e -> onInvoice.run());
        JButton view = button("View all sales", SLATE, 170, 40, 14);
        view.addActionListener(e -> onViewSales.run());
        sec.add(print);
        sec.add(prev);
        sec.add(inv);
        sec.add(view);
        card.add(center(sec));

        add(card);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), "newSale");
        getActionMap().put("newSale", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { onNewSale.run(); }
        });
    }

    private static JComponent center(JComponent c) {
        c.setAlignmentX(Component.CENTER_ALIGNMENT);
        return c;
    }
}
