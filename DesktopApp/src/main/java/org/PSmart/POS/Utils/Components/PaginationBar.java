package org.PSmart.POS.Utils.Components;

import org.PSmart.POS.Utils.Palette;

import javax.swing.*;
import java.awt.*;

/**
 * A compact, styled pagination bar. Emits events via a listener so the owner
 * reloads its page. Built on null layout with relative positioning and the POS palette.
 */
public class PaginationBar extends JPanel {

    public interface PageChangeListener {
        void onPageChanged(int newPage);
    }

    private final JLabel infoLabel;
    private final JButton prevBtn;
    private final JButton pageLabel;
    private final JButton nextBtn;

    private int page = 0;
    private int pageSize = 20;
    private long totalRows = 0;

    public PaginationBar(PageChangeListener listener) {
        setLayout(null);
        setBackground(Palette.CARD);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Palette.BORDER_SUBTLE));

        infoLabel = new JLabel("Showing 0 - 0 of 0", SwingConstants.CENTER);
        infoLabel.setFont(Palette.courier(13));
        infoLabel.setForeground(Palette.GRAY);

        prevBtn = navButton("‹ Prev");
        pageLabel = pageButton("Page 1");
        nextBtn = navButton("Next ›");

        prevBtn.addActionListener(e -> {
            if (page > 0) {
                setPage(page - 1);
                if (listener != null) listener.onPageChanged(page);
            }
        });
        nextBtn.addActionListener(e -> {
            if (page < maxPage()) {
                setPage(page + 1);
                if (listener != null) listener.onPageChanged(page);
            }
        });

        add(infoLabel);
        add(prevBtn);
        add(pageLabel);
        add(nextBtn);
    }

    private JButton navButton(String text) {
        return text.startsWith("Next")
                ? make(text, Palette.BLUE_BTN)
                : make(text, Palette.GRAY);
    }

    private JButton pageButton(String text) {
        return make(text, Palette.HEADER);
    }

    private JButton make(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(Palette.courier(13));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBackground(bg);
        return b;
    }

    /** setPage triggers the change listener itself; updates state + UI. */
    public void setPage(int p) {
        page = Math.max(0, Math.min(p, maxPage()));
        render();
    }

    public void setPageSize(int size) { this.pageSize = Math.max(1, size); render(); }

    public void setTotalRows(long total) { this.totalRows = Math.max(0, total); render(); }

    public int getPage() { return page; }

    private int maxPage() {
        if (totalRows <= 0) return 0;
        return (int) ((totalRows - 1) / pageSize);
    }

    private void render() {
        infoLabel.setText(displayRange());
        pageLabel.setText("Page " + (page + 1));
    }

    private String displayRange() {
        if (totalRows == 0) return "Showing 0 - 0 of 0";
        long from = (long) page * pageSize + 1;
        long to = Math.min((long) (page + 1) * pageSize, totalRows);
        return "Showing " + from + " - " + to + " of " + totalRows;
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        layoutChildren();
    }

    private void layoutChildren() {
        int w = getWidth(), h = getHeight();
        // info on left, pager on right
        int btnW = 90, btnH = Math.min(38, h - 8);
        int pagerW = btnW * 2 + 110;
        int rightX = w - pagerW - 12;

        infoLabel.setBounds(16, 0, Math.max(0, rightX - 16), h);

        pageLabel.setBounds(rightX + btnW, (h - btnH) / 2, 110, btnH);
        prevBtn.setBounds(rightX, (h - btnH) / 2, btnW, btnH);
        nextBtn.setBounds(rightX + btnW + 112, (h - btnH) / 2, btnW, btnH);
    }
}