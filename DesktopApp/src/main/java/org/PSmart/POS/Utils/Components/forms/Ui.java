package org.PSmart.POS.Utils.Components.forms;

import org.PSmart.POS.Utils.MyUtils;
import org.PSmart.POS.tests.AutoClosingNotification;

import javax.swing.*;
import java.awt.*;

/** Shared look for list-page actions: the app's blue + slate buttons and non-blocking toasts. */
public final class Ui {

    public static final Color BLUE = new Color(25, 118, 211);   // same blue as the main menu
    public static final Color SLATE = new Color(52, 58, 64);    // same slate as "+ Add New Item"
    public static final Color PAGE = new Color(191, 191, 191);  // page background used by Stock pages

    private Ui() {}

    public static JButton button(String text, Color bg) {
        JButton b = MyUtils.createButton(text, bg);
        b.setForeground(Color.WHITE);
        b.setFont(MyUtils.courierFont(14));
        b.setPreferredSize(new Dimension(Math.max(120, text.length() * 10 + 28), 34));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JButton primary(String text) { return button(text, BLUE); }

    public static JButton neutral(String text) { return button(text, SLATE); }

    /** Wide slate "+ Add New ..." bar, like the one on All Items. */
    public static JButton addBar(String text) {
        JButton b = new JButton(text);
        b.setBackground(SLATE);
        b.setForeground(Color.WHITE);
        b.setFont(MyUtils.courierFont(20));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static void toast(String message) {
        try {
            AutoClosingNotification.sendNotification(message);
        } catch (Exception ignored) {
            // never fail an action because a toast could not show
        }
    }

    /** Standard table styling used by list pages. */
    public static void styleTable(JTable t) {
        t.setRowHeight(30);
        t.setFont(new Font("Courier", Font.PLAIN, 14));
        t.setGridColor(new Color(230, 230, 230));
        t.setSelectionBackground(new Color(0, 0, 137));
        t.setSelectionForeground(Color.WHITE);
        t.getTableHeader().setFont(new Font("Courier", Font.BOLD, 14));
        t.getTableHeader().setBackground(SLATE);
        t.getTableHeader().setForeground(Color.WHITE);
        t.getTableHeader().setReorderingAllowed(false);
        // breathing room inside cells (columns with their own renderer keep theirs)
        javax.swing.table.DefaultTableCellRenderer pad = new javax.swing.table.DefaultTableCellRenderer();
        pad.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        t.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(tb, v, sel, f, r, c);
                ((JLabel) comp).setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return comp;
            }
        });
    }
}
