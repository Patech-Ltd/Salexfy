package org.PSmart.POS.ui.MainView.ui.stock;

import org.PSmart.POS.ui.MainView.DashBoard;
import org.PSmart.POS.ui.MainView.ui.stock.current.CurrentStockPanel;
import org.PSmart.POS.ui.MainView.ui.stock.current.StockMovementsPanel;
import org.PSmart.POS.ui.MainView.ui.stock.current.StockTakePanel;
import org.PSmart.POS.ui.MainView.ui.stock.lists.CategoriesListPanel;
import org.PSmart.POS.ui.MainView.ui.stock.lists.ItemsListPanel;
import org.PSmart.POS.ui.MainView.ui.stock.lists.StockReceivedPanel;
import org.PSmart.POS.Utils.Components.forms.Ui;
import org.PSmart.POS.Utils.MyUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * Stock Management: one title bar with tabs (Overview, Items, Categories, Received, Stock Take, Movements)
 * and the whole remaining screen for the page. Pages are rebuilt whenever the window changes size, so
 * they always fill the space.
 */
public class StockPanel extends JPanel {

    private static final String[] TABS = {"Overview", "All Items", "Categories", "Stock Received", "Stock Take", "Movements"};
    private static final Color ACTIVE = new Color(0, 0, 137);

    private final DashBoard dashBoard;
    private final JPanel playground = new JPanel(null);
    private final JButton[] tabButtons = new JButton[TABS.length];
    private int current = 0;
    private javax.swing.Timer resizeTimer;

    public StockPanel(DashBoard dashBoard) {
        super(new BorderLayout());
        this.dashBoard = dashBoard;
        setBackground(Ui.PAGE);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Ui.SLATE);
        bar.setPreferredSize(new Dimension(0, 56));
        bar.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 16));
        JLabel title = new JLabel("Stock Management");
        title.setFont(MyUtils.courierFont(22));
        title.setForeground(Color.WHITE);
        bar.add(title, BorderLayout.WEST);

        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        tabs.setOpaque(false);
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            JButton b = MyUtils.createButton(TABS[i], Ui.BLUE);
            b.setForeground(Color.WHITE);
            b.setFont(MyUtils.courierFont(14));
            b.setPreferredSize(new Dimension(TABS[i].length() * 10 + 40, 36));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addActionListener(e -> showTab(idx));
            tabButtons[i] = b;
            tabs.add(b);
        }
        bar.add(tabs, BorderLayout.EAST);
        add(bar, BorderLayout.NORTH);

        playground.setBackground(Ui.PAGE);
        add(playground, BorderLayout.CENTER);

        // build (and rebuild on resize) the current page once the playground has its real size
        playground.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                if (playground.getWidth() <= 0 || playground.getHeight() <= 0) return;
                if (resizeTimer != null) resizeTimer.stop();
                resizeTimer = new javax.swing.Timer(120, ev -> showTab(current));
                resizeTimer.setRepeats(false);
                resizeTimer.start();
            }
        });
        highlight(0);
    }

    private void highlight(int idx) {
        for (int i = 0; i < tabButtons.length; i++) tabButtons[i].setBackground(i == idx ? ACTIVE : Ui.BLUE);
    }

    /** Opens a tab by index. */
    public void showTab(int idx) {
        current = idx;
        highlight(idx);
        if (playground.getWidth() <= 0) return;      // the resize listener will build it once sized
        JPanel page;
        switch (idx) {
            case 1: page = new ItemsListPanel(this); break;
            case 2: page = new CategoriesListPanel(this); break;
            case 3: page = new StockReceivedPanel(this); break;
            case 4: page = new StockTakePanel(this); break;
            case 5: page = new StockMovementsPanel(this); break;
            default: page = new CurrentStockPanel(this); break;
        }
        playground.removeAll();
        playground.add(page);
        playground.revalidate();
        playground.repaint();
    }

    public DashBoard getDashBoard() {
        return dashBoard;
    }

    public JPanel getPlayground() {
        return playground;
    }

    /** Kept for older callers. */
    public void setActiveTab(int tab) {
        highlight(tab);
    }

    /** Kept for older callers (the tab list used to be a row of runnables). */
    public Runnable[] getRunnables() {
        Runnable[] r = new Runnable[TABS.length];
        for (int i = 0; i < r.length; i++) {
            final int idx = i;
            r[i] = () -> showTab(idx);
        }
        return r;
    }
}
