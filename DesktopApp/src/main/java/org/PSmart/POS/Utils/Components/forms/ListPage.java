package org.PSmart.POS.Utils.Components.forms;

import org.PSmart.POS.Utils.Components.PTextField;
import org.PSmart.POS.Utils.IconsUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Shared list-page skeleton: "+ Add New" bar, search, table, action buttons, inline status. */
public abstract class ListPage extends JPanel {

    protected final JButton addBar;
    protected final PTextField search;
    protected final DefaultTableModel model;
    protected final JTable table;
    protected final JScrollPane scroll;
    protected final JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
    protected final JLabel status = new JLabel(" ");
    protected final JLabel footer = new JLabel(" ");
    protected final List<Object> ids = new ArrayList<>();

    public ListPage(String addText, String searchLabel, String[] cols) {
        super(null);
        setBackground(Ui.PAGE);
        addBar = Ui.addBar(addText);
        search = new PTextField(Ui.SLATE, new Color(0, 0, 255), searchLabel, IconsUtils.getIcon("search.png"));
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        Ui.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scroll = new JScrollPane(table);
        actions.setOpaque(false);
        status.setFont(new Font("Courier", Font.BOLD, 13));
        status.setForeground(Color.RED);
        footer.setFont(new Font("Courier", Font.BOLD, 14));
        add(addBar);
        add(search);
        add(scroll);
        add(actions);
        add(footer);
        search.getField().addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyReleased(java.awt.event.KeyEvent e) { render(); }
        });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { if (e.getClickCount() == 2) onOpen(); }
        });
    }

    protected abstract void render();

    protected abstract void onOpen();

    @Override
    public void setBounds(int x, int y, int w, int h) {
        super.setBounds(x, y, w, h);
        layoutUi();
    }

    public void layoutUi() {
        int w = getWidth(), h = getHeight();
        if (w <= 1 || h <= 1) return;
        addBar.setBounds(w / 4, 10, w / 2, 35);
        search.setLocation(w / 6, 50);
        search.setSize(w - w / 3, 60);
        scroll.setBounds(20, 120, w - 40, h - 120 - 96);
        actions.setBounds(20, h - 90, w - 300, 44);
        status.setBounds(20, h - 44, w - 40, 20);
        footer.setBounds(24, h - 24, w - 48, 22);
        revalidate();
        repaint();
    }

    protected void flash(String msg) {
        status.setText(msg);
        javax.swing.Timer t = new javax.swing.Timer(4000, e -> status.setText(" "));
        t.setRepeats(false);
        t.start();
    }

    protected Object selectedId() {
        int r = table.getSelectedRow();
        if (r < 0 || r >= ids.size()) {
            flash("Select a row in the list first.");
            return null;
        }
        return ids.get(r);
    }
}
