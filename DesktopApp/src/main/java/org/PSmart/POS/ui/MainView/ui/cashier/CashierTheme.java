package org.PSmart.POS.ui.MainView.ui.cashier;

import org.PSmart.POS.Utils.MyUtils;

import javax.swing.*;
import java.awt.*;
import java.util.function.IntConsumer;

/**
 * One colour language for the whole cashier flow:
 * BLUE = actions, NAVY = selected / emphasis, SLATE = neutral, GREEN = money received (Pay only),
 * RED = remove / errors only.
 */
final class CashierTheme {

    static final Color BLUE = new Color(25, 118, 211);     // same as the main menu
    static final Color NAVY = new Color(0, 0, 137);        // same as form labels
    static final Color SLATE = new Color(52, 58, 64);      // same as "+ Add New Item"
    static final Color GREEN = new Color(40, 167, 69);
    static final Color RED = new Color(220, 53, 69);
    static final Color MUTED = new Color(108, 117, 125);
    static final Color BORDER = new Color(222, 226, 230);
    static final Color PAGE = new Color(245, 245, 250);
    static final Color ROW_ALT = new Color(248, 249, 252);
    static final Color CHIP_OFF = new Color(233, 236, 239);

    private CashierTheme() {}

    static JButton button(String text, Color bg, int w, int h, int fontSize) {
        JButton b = MyUtils.createButton(text, bg);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Courier", Font.BOLD, fontSize));
        b.setPreferredSize(new Dimension(w, h));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** Full-width variant for vertical stacks. */
    static JButton wide(String text, Color bg, int h, int fontSize) {
        JButton b = button(text, bg, 10, h, fontSize);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        return b;
    }

    /** Outlined button (white fill) for secondary actions. */
    static JButton outline(String text, Color accent, int h, int fontSize) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(getModel().isArmed() ? CHIP_OFF : Color.WHITE);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(accent);
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                super.paintComponent(g);
            }
        };
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setForeground(accent);
        b.setFont(new Font("Courier", Font.BOLD, fontSize));
        b.setPreferredSize(new Dimension(10, h));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    static JLabel label(String text, int size, boolean bold, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Courier", bold ? Font.BOLD : Font.PLAIN, size));
        l.setForeground(color);
        return l;
    }

    static String money(double v) {
        return String.format("%,.2f", v);
    }

    /** Compact two-option (or more) toggle: selected = navy, others light. */
    static final class Segmented extends JPanel {
        private final JButton[] buttons;
        private int selected;

        Segmented(String[] options, int selected, IntConsumer onChange) {
            super(new GridLayout(1, options.length, 2, 0));
            setOpaque(false);
            this.selected = selected;
            buttons = new JButton[options.length];
            for (int i = 0; i < options.length; i++) {
                final int idx = i;
                JButton b = new JButton(options[i]) {
                    @Override protected void paintComponent(Graphics g) {
                        g.setColor(getBackground());
                        g.fillRect(0, 0, getWidth(), getHeight());
                        super.paintComponent(g);
                    }
                };
                b.setContentAreaFilled(false);
                b.setBorderPainted(false);
                b.setFocusPainted(false);
                b.setMargin(new Insets(0, 6, 0, 6));
                b.setFont(new Font("Courier", Font.BOLD, 13));
                b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                b.addActionListener(e -> { select(idx); if (onChange != null) onChange.accept(idx); });
                buttons[i] = b;
                add(b);
            }
            select(selected);
        }

        void select(int idx) {
            selected = idx;
            for (int i = 0; i < buttons.length; i++) {
                boolean on = i == idx;
                buttons[i].setBackground(on ? NAVY : CHIP_OFF);
                buttons[i].setForeground(on ? Color.WHITE : SLATE);
            }
        }

        int getSelected() { return selected; }
    }
}
