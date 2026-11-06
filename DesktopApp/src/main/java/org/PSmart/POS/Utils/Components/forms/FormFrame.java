package org.PSmart.POS.Utils.Components.forms;

import org.PSmart.POS.Utils.Components.PTextField;
import org.PSmart.POS.Utils.MyUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Standard data-entry form window, matching the Add/Edit Item and Edit User forms:
 * white page, Courier labels, floating-label {@link PTextField}s, Blue save / slate cancel.
 * Adds progressive disclosure (collapsible sections), segmented choices and an inline
 * error line, so forms never need modal pop-ups.
 */
public class FormFrame extends JFrame {

    public static final Color NAVY = new Color(0, 0, 137);
    public static final Color INK = new Color(0, 0, 255);
    public static final Color SLATE = new Color(52, 58, 64);
    public static final Color MUTED = new Color(108, 117, 125);

    private static final int GAP = 10;

    private int buttonWidth = 224;
    private int buttonFont = 18;

    /** For forms with many footer buttons: call before adding the buttons. */
    public void compactButtons() { buttonWidth = 166; buttonFont = 14; footer.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 10)); }

    private final int contentWidth;
    private final JPanel content = new JPanel(null);
    private final JScrollPane scroll;
    private final JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 10));
    private final JLabel errorLabel = new JLabel(" ");
    private final List<Row> rows = new ArrayList<>();

    private static final class Row {
        final Component c;
        final int h;
        boolean visible = true;
        Row(Component c, int h) { this.c = c; this.h = h; }
    }

    public FormFrame(String windowTitle, String heading, int contentWidth) {
        this.contentWidth = contentWidth;
        setTitle(windowTitle);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(Color.WHITE);
        setLayout(new BorderLayout());

        content.setBackground(Color.WHITE);
        scroll = new JScrollPane(content, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)));
        add(footer, BorderLayout.SOUTH);

        JLabel h = new JLabel(heading);
        h.setFont(MyUtils.courierFont(24));
        h.setForeground(NAVY);
        addRow(h, 40);

        errorLabel.setFont(new Font("Courier", Font.BOLD, 14));
        errorLabel.setForeground(Color.RED);

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) { relayout(); }
        });
    }

    // ------------------------------------------------------------------ rows

    private Row addRow(Component c, int h) {
        Row r = new Row(c, h);
        rows.add(r);
        content.add(c);
        return r;
    }

    /** Floating-label input, like every field on the Add Item form. */
    public PTextField field(String label, String initial) {
        PTextField f = new PTextField(NAVY, INK, label, null);
        if (initial != null) f.setText(initial);
        addRow(f, 70);
        return f;
    }

    /** The component of the row added last. */
    public Component lastRowComponent() {
        return rows.get(rows.size() - 1).c;
    }

    /** Adds any component (e.g. a table in a scroll pane) as a full-width row of the given height. */
    public void custom(Component c, int height) {
        addRow(c, height);
    }

    /** Number box: digits and one decimal point only. */
    public PTextField decimal(String label, String initial) {
        return org.PSmart.POS.Utils.Components.Inputs.decimal(field(label, initial));
    }

    /** Whole-number box. */
    public PTextField integer(String label, String initial) {
        return org.PSmart.POS.Utils.Components.Inputs.integer(field(label, initial));
    }

    /** Calendar date picker with a label above it. */
    public org.jdatepicker.JDatePicker date(String label, java.time.LocalDate initial) {
        JPanel box = new JPanel(null);
        box.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(new Font("Courier", Font.BOLD, 15));
        l.setForeground(NAVY);
        l.setBounds(0, 0, contentWidth, 22);
        box.add(l);
        org.jdatepicker.JDatePicker p = org.PSmart.POS.Utils.Components.Inputs.datePicker(initial);
        p.setBounds(0, 26, Math.min(contentWidth, 300), 34);
        box.add(p);
        addRow(box, 62);
        return p;
    }

    /** Drop-down with a label above it. */
    public JComboBox<String> combo(String label, String[] options, int selected) {
        JPanel box = new JPanel(null);
        box.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(new Font("Courier", Font.BOLD, 15));
        l.setForeground(NAVY);
        l.setBounds(0, 0, contentWidth, 22);
        box.add(l);
        JComboBox<String> c = new JComboBox<>(options);
        c.setFont(new Font("Courier", Font.PLAIN, 15));
        c.setSelectedIndex(Math.max(0, Math.min(selected, options.length - 1)));
        c.setBounds(0, 26, Math.min(contentWidth, 420), 32);
        box.add(c);
        addRow(box, 62);
        return c;
    }

    public JCheckBox check(String text, boolean selected) {
        JCheckBox c = new JCheckBox(text, selected);
        c.setFont(new Font("Courier", Font.PLAIN, 15));
        c.setOpaque(false);
        c.setForeground(SLATE);
        addRow(c, 30);
        return c;
    }

    /** Masked input styled like the other fields (value via getText()). */
    public org.PSmart.POS.Utils.Components.PPasswordTextField password(String label) {
        org.PSmart.POS.Utils.Components.PPasswordTextField p =
                new org.PSmart.POS.Utils.Components.PPasswordTextField(NAVY, INK, label);
        addRow(p, 70);
        return p;
    }

    /** Grid of check boxes (e.g. permissions: rows x [View, Change]). */
    public JCheckBox[][] matrix(String[] rowNames, String[] colNames, boolean[][] state) {
        JPanel grid = new JPanel(new GridLayout(rowNames.length + 1, colNames.length + 1, 4, 2));
        grid.setOpaque(false);
        grid.add(new JLabel(""));
        for (String c : colNames) {
            JLabel h = new JLabel(c, SwingConstants.CENTER);
            h.setFont(new Font("Courier", Font.BOLD, 13));
            h.setForeground(NAVY);
            grid.add(h);
        }
        JCheckBox[][] boxes = new JCheckBox[rowNames.length][colNames.length];
        for (int r = 0; r < rowNames.length; r++) {
            JLabel n = new JLabel(rowNames[r]);
            n.setFont(new Font("Courier", Font.PLAIN, 14));
            grid.add(n);
            for (int c = 0; c < colNames.length; c++) {
                JCheckBox b = new JCheckBox("", state != null && state[r][c]);
                b.setHorizontalAlignment(SwingConstants.CENTER);
                b.setOpaque(false);
                boxes[r][c] = b;
                grid.add(b);
            }
        }
        addRow(grid, (rowNames.length + 1) * 28);
        return boxes;
    }

    public JLabel info(String label, String value) {
        JLabel l = new JLabel("<html><span style='color:#6c757d'>" + label + "&nbsp;&nbsp;</span><b>"
                + escape(value) + "</b></html>");
        l.setFont(new Font("Courier", Font.PLAIN, 15));
        addRow(l, 28);
        return l;
    }

    public JLabel note(String text) {
        JLabel l = new JLabel("<html>" + escape(text) + "</html>");
        l.setFont(new Font("Courier", Font.PLAIN, 13));
        l.setForeground(MUTED);
        addRow(l, 34);
        return l;
    }

    /** Segmented choice (radio-style buttons). */
    public Choice choice(String label, String[] options, int selected, IntConsumer onChange) {
        JPanel box = new JPanel(null);
        box.setOpaque(false);
        Choice ch = new Choice(options, selected, onChange, box, label);
        addRow(box, 62);
        return ch;
    }

    public final class Choice {
        private final JButton[] buttons;
        private int selected;

        Choice(String[] options, int sel, IntConsumer onChange, JPanel box, String label) {
            this.selected = sel;
            JLabel l = new JLabel(label);
            l.setFont(new Font("Courier", Font.BOLD, 15));
            l.setForeground(NAVY);
            l.setBounds(0, 0, contentWidth, 22);
            box.add(l);
            buttons = new JButton[options.length];
            int w = (contentWidth - (options.length - 1) * 8) / options.length;
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
                b.setMargin(new Insets(0, 2, 0, 2));
                b.setFont(new Font("Courier", Font.BOLD, 13));
                b.setFocusPainted(false);
                b.setBounds(i * (w + 8), 26, w, 32);
                b.addActionListener(e -> { select(idx); if (onChange != null) onChange.accept(idx); });
                buttons[i] = b;
                box.add(b);
            }
            select(sel);
        }

        private void select(int idx) {
            selected = idx;
            for (int i = 0; i < buttons.length; i++) {
                boolean on = i == idx;
                buttons[i].setBackground(on ? NAVY : new Color(235, 236, 240));
                buttons[i].setForeground(on ? Color.WHITE : SLATE);
                buttons[i].setOpaque(true);
                buttons[i].setBorderPainted(false);
            }
        }

        public int getSelected() { return selected; }
    }

    /** Collapsible block: shows only a link until opened (progressive disclosure). */
    public Section section(String title, boolean open) {
        return new Section(title, open);
    }

    public final class Section {
        private final JButton toggle;
        private final String title;
        private final List<Row> children = new ArrayList<>();
        private boolean open;

        Section(String title, boolean open) {
            this.title = title;
            this.open = open;
            toggle = new JButton();
            toggle.setFont(new Font("Courier", Font.BOLD, 14));
            toggle.setForeground(INK);
            toggle.setContentAreaFilled(false);
            toggle.setBorderPainted(false);
            toggle.setFocusPainted(false);
            toggle.setHorizontalAlignment(SwingConstants.LEFT);
            toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            toggle.addActionListener(e -> { this.open = !this.open; apply(); relayout(); });
            addRow(toggle, 30);
            apply();
        }

        private void apply() {
            toggle.setText((open ? "[ - ]  " : "[ + ]  ") + title);
            for (Row r : children) r.visible = open;
        }

        public PTextField field(String label, String initial) {
            PTextField f = new PTextField(NAVY, INK, label, null);
            if (initial != null) f.setText(initial);
            Row r = addRow(f, 70);
            r.visible = open;
            children.add(r);
            return f;
        }

        public PTextField decimal(String label, String initial) {
            return org.PSmart.POS.Utils.Components.Inputs.decimal(field(label, initial));
        }

        public boolean isOpen() { return open; }

        /** Puts the most recently added row inside this section (so it shows/hides with it). */
        public void attach(Component c) {
            for (Row r : rows) {
                if (r.c == c) {
                    r.visible = open;
                    children.add(r);
                }
            }
        }

        public void setOpen(boolean on) {
            this.open = on;
            apply();
            relayout();
        }
    }

    // ------------------------------------------------------------------ footer + errors

    public JButton primary(String text, Runnable action) {
        JButton b = MyUtils.createButton(text, Color.BLUE);
        style(b, action);
        footer.add(b);
        return b;
    }

    public JButton secondary(String text, Runnable action) {
        JButton b = MyUtils.createButton(text, SLATE);
        style(b, action);
        footer.add(b);
        return b;
    }

    public JButton danger(String text, Runnable action) {
        JButton b = MyUtils.createButton(text, Color.RED);
        style(b, action);
        footer.add(b);
        return b;
    }

    private void style(JButton b, Runnable action) {
        b.setForeground(Color.WHITE);
        b.setFont(MyUtils.courierFont(buttonFont));
        b.setPreferredSize(new Dimension(buttonWidth, 44));
        b.addActionListener(e -> action.run());
    }

    /** Inline validation message (no pop-up). Pass null/empty to clear. */
    public void error(String message) {
        errorLabel.setText(message == null || message.isEmpty() ? " " : message);
    }

    /** Places the error line at the current position in the form. Call after adding fields. */
    public void errorLine() {
        addRow(errorLabel, 26);
    }

    // ------------------------------------------------------------------ layout / show

    private void relayout() {
        int y = 16;
        int x = 24;
        for (Row r : rows) {
            r.c.setVisible(r.visible);
            if (!r.visible) continue;
            if (r.c instanceof PTextField) {
                r.c.setLocation(x, y);
                ((PTextField) r.c).setSize(contentWidth, r.h);
            } else if (r.c instanceof org.PSmart.POS.Utils.Components.PPasswordTextField) {
                r.c.setLocation(x, y);
                ((org.PSmart.POS.Utils.Components.PPasswordTextField) r.c).setSize(contentWidth, r.h);
            } else {
                r.c.setBounds(x, y, contentWidth, r.h);
            }
            y += r.h + GAP;
        }
        content.setPreferredSize(new Dimension(contentWidth + 2 * x, y + 8));
        content.revalidate();
        content.repaint();
    }

    /** Sizes the window to its content (capped to the screen) and shows it. */
    public void open() {
        relayout();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int h = Math.min(content.getPreferredSize().height + 80, screen.height - 100);
        setSize(contentWidth + 80, Math.max(h, 360));
        setLocationRelativeTo(null);
        setVisible(true);
        relayout();
        toFront();
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
