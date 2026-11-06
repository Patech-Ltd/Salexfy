package org.PSmart.POS.Utils.receipt;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tiny layout engine for printed documents. Content is laid out in "dots" (203 dpi, the
 * resolution of thermal heads) and scaled to the printer, so the same document renders
 * identically on a 58 mm roll, an 80 mm roll, an A4 sheet, or in the on-screen preview.
 */
public final class Receipt implements Printable {

    public enum Paper {
        ROLL_58("58 mm roll", 384, 48.0, true, 20),
        ROLL_80("80 mm roll", 576, 72.0, true, 22),
        A4("A4 sheet", 1500, 190.0, false, 26);

        final String label;
        final int dots;          // printable width in dots
        final double widthMm;    // printable width in millimetres
        final boolean roll;
        final int baseFont;

        Paper(String label, int dots, double widthMm, boolean roll, int baseFont) {
            this.label = label;
            this.dots = dots;
            this.widthMm = widthMm;
            this.roll = roll;
            this.baseFont = baseFont;
        }

        public String label() { return label; }

        public static Paper fromSetting(String s) {
            if (s == null) return ROLL_80;
            switch (s.trim()) {
                case "58": return ROLL_58;
                case "A4": return A4;
                default: return ROLL_80;
            }
        }
    }

    public enum Align { LEFT, CENTER, RIGHT }

    private static final double DPI = 203.0;

    private interface Row {
        int layout(Graphics2D g, int width, int y, boolean paint);
    }

    private final Paper paper;
    private final List<Row> rows = new ArrayList<>();
    private final String title;
    private int totalHeight = -1;

    public Receipt(Paper paper, String title) {
        this.paper = paper;
        this.title = title;
    }

    public Paper paper() { return paper; }

    public String title() { return title; }

    private int base() { return paper.baseFont; }

    private Font font(int size, boolean bold) {
        return new Font(Font.MONOSPACED, bold ? Font.BOLD : Font.PLAIN, size);
    }

    // ------------------------------------------------------------------ builder

    public Receipt gap(int dots) {
        rows.add((g, w, y, paint) -> y + dots);
        return this;
    }

    public Receipt text(String text, Align align, int sizeDelta, boolean bold) {
        final int size = base() + sizeDelta;
        rows.add((g, w, y, paint) -> {
            g.setFont(font(size, bold));
            FontMetrics fm = g.getFontMetrics();
            for (String line : wrap(text == null ? "" : text, fm, w)) {
                if (paint) {
                    int x = align == Align.CENTER ? (w - fm.stringWidth(line)) / 2
                            : align == Align.RIGHT ? w - fm.stringWidth(line) : 0;
                    g.drawString(line, x, y + fm.getAscent());
                }
                y += fm.getHeight();
            }
            return y;
        });
        return this;
    }

    public Receipt center(String text, int sizeDelta, boolean bold) { return text(text, Align.CENTER, sizeDelta, bold); }

    public Receipt left(String text) { return text(text, Align.LEFT, 0, false); }

    public Receipt muted(String text) { return text(text, Align.CENTER, -4, false); }

    /** "Label ........ value" on one line; value wins the space, label wraps if needed. */
    public Receipt kv(String label, String value, int sizeDelta, boolean bold) {
        return kvIndented(label, value, sizeDelta, bold, 0.0);
    }

    /** Same, but confined to the right-hand part of the page (A4 totals block): indent = 0..1 of the width. */
    public Receipt kvIndented(String label, String value, int sizeDelta, boolean bold, double indent) {
        final int size = base() + sizeDelta;
        rows.add((g, w0, y, paint) -> {
            int off = (int) (w0 * indent);
            int w = w0 - off;
            g.setFont(font(size, bold));
            FontMetrics fm = g.getFontMetrics();
            String v = value == null ? "" : value;
            int vw = fm.stringWidth(v);
            String l = label == null ? "" : label;
            int avail = Math.max(10, w - vw - fm.charWidth(' ') * 2);
            List<String> ll = wrap(l, fm, avail);
            if (paint) {
                g.drawString(ll.get(0), off, y + fm.getAscent());
                g.drawString(v, off + w - vw, y + fm.getAscent());
            }
            y += fm.getHeight();
            for (int i = 1; i < ll.size(); i++) {
                if (paint) g.drawString(ll.get(i), off, y + fm.getAscent());
                y += fm.getHeight();
            }
            return y;
        });
        return this;
    }

    /** Two blocks side by side (A4 header: business on the left, document details on the right). */
    public Receipt twoColumn(String[] leftLines, boolean leftBold, String[] rightLines, boolean rightBold, int sizeDelta) {
        final int size = base() + sizeDelta;
        rows.add((g, w, y, paint) -> {
            int half = w / 2 - 10;
            g.setFont(font(size, false));
            FontMetrics fm = g.getFontMetrics();
            int yl = y, yr = y;
            for (int i = 0; i < leftLines.length; i++) {
                g.setFont(font(size, leftBold && i == 0));
                FontMetrics f = g.getFontMetrics();
                for (String line : wrap(leftLines[i], f, half)) {
                    if (paint) g.drawString(line, 0, yl + f.getAscent());
                    yl += f.getHeight();
                }
            }
            for (int i = 0; i < rightLines.length; i++) {
                g.setFont(font(size, rightBold && i == 0));
                FontMetrics f = g.getFontMetrics();
                for (String line : wrap(rightLines[i], f, half)) {
                    if (paint) g.drawString(line, w - f.stringWidth(line), yr + f.getAscent());
                    yr += f.getHeight();
                }
            }
            return Math.max(yl, yr);
        });
        return this;
    }

    public Receipt kv(String label, String value) { return kv(label, value, 0, false); }

    /** style: '-' dashed, '=' double, '_' solid. */
    public Receipt rule(char style) {
        rows.add((g, w, y, paint) -> {
            if (paint) {
                g.setColor(Color.BLACK);
                if (style == '-') {
                    g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{8f, 6f}, 0f));
                    g.drawLine(0, y + 6, w, y + 6);
                } else if (style == '=') {
                    g.setStroke(new BasicStroke(2f));
                    g.drawLine(0, y + 4, w, y + 4);
                    g.drawLine(0, y + 10, w, y + 10);
                } else {
                    g.setStroke(new BasicStroke(3f));
                    g.drawLine(0, y + 6, w, y + 6);
                }
                g.setStroke(new BasicStroke(1f));
            }
            return y + 14;
        });
        return this;
    }

    /** Inverted bar, e.g. the grand total. */
    public Receipt banner(String left, String right) {
        final int size = base() + 6;
        rows.add((g, w, y, paint) -> {
            g.setFont(font(size, true));
            FontMetrics fm = g.getFontMetrics();
            int h = fm.getHeight() + 14;
            if (paint) {
                g.setColor(Color.BLACK);
                g.fill(new RoundRectangle2D.Double(0, y, w, h, 10, 10));
                g.setColor(Color.WHITE);
                g.drawString(left, 10, y + 7 + fm.getAscent());
                g.drawString(right, w - 10 - fm.stringWidth(right), y + 7 + fm.getAscent());
                g.setColor(Color.BLACK);
            }
            return y + h + 4;
        });
        return this;
    }

    /** Bordered centred box (receipt number, status stamp). */
    public Receipt box(String text, int sizeDelta) {
        final int size = base() + sizeDelta;
        rows.add((g, w, y, paint) -> {
            g.setFont(font(size, true));
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(text) + 40;
            int h = fm.getHeight() + 10;
            if (paint) {
                int x = (w - tw) / 2;
                g.setStroke(new BasicStroke(3f));
                g.draw(new RoundRectangle2D.Double(x, y, tw, h, 10, 10));
                g.setStroke(new BasicStroke(1f));
                g.drawString(text, x + 20, y + 5 + fm.getAscent());
            }
            return y + h + 6;
        });
        return this;
    }

    /**
     * Roll-friendly item: the name on its own line(s), then "qty x unit .... total".
     */
    public Receipt item(String name, String qtyPrice, String total) {
        rows.add((g, w, y, paint) -> {
            g.setFont(font(base(), true));
            FontMetrics fm = g.getFontMetrics();
            for (String line : wrap(name, fm, w)) {
                if (paint) g.drawString(line, 0, y + fm.getAscent());
                y += fm.getHeight();
            }
            g.setFont(font(base() - 2, false));
            fm = g.getFontMetrics();
            if (paint) {
                g.drawString("  " + qtyPrice, 0, y + fm.getAscent());
                g.setFont(font(base(), true));
                FontMetrics fb = g.getFontMetrics();
                g.drawString(total, w - fb.stringWidth(total), y + fb.getAscent());
            }
            return y + g.getFontMetrics(font(base(), true)).getHeight() + 4;
        });
        return this;
    }

    /**
     * Table row for A4 documents. widths are weights; aligns per column; header draws a shaded bar.
     */
    public Receipt table(int[] weights, Align[] aligns, String[] cells, boolean header, boolean zebra) {
        rows.add((g, w, y, paint) -> {
            g.setFont(font(base() - 2, header));
            FontMetrics fm = g.getFontMetrics();
            int sum = 0;
            for (int x : weights) sum += x;
            int[] cw = new int[weights.length];
            int[] cx = new int[weights.length];
            int acc = 0;
            for (int i = 0; i < weights.length; i++) {
                cw[i] = w * weights[i] / sum;
                cx[i] = acc;
                acc += cw[i];
            }
            int lines = 1;
            List<List<String>> wrapped = new ArrayList<>();
            for (int i = 0; i < cells.length; i++) {
                List<String> l = wrap(cells[i] == null ? "" : cells[i], fm, cw[i] - 14);
                wrapped.add(l);
                lines = Math.max(lines, l.size());
            }
            int h = lines * fm.getHeight() + 12;
            if (paint) {
                if (header) {
                    g.setColor(new Color(40, 40, 40));
                    g.fillRect(0, y, w, h);
                    g.setColor(Color.WHITE);
                } else if (zebra) {
                    g.setColor(new Color(240, 240, 240));
                    g.fillRect(0, y, w, h);
                    g.setColor(Color.BLACK);
                } else {
                    g.setColor(Color.BLACK);
                }
                for (int i = 0; i < cells.length; i++) {
                    int ly = y + 6;
                    for (String line : wrapped.get(i)) {
                        int x = aligns[i] == Align.RIGHT ? cx[i] + cw[i] - 8 - fm.stringWidth(line)
                                : aligns[i] == Align.CENTER ? cx[i] + (cw[i] - fm.stringWidth(line)) / 2 : cx[i] + 8;
                        g.drawString(line, x, ly + fm.getAscent());
                        ly += fm.getHeight();
                    }
                }
                g.setColor(Color.BLACK);
            }
            return y + h;
        });
        return this;
    }

    // ------------------------------------------------------------------ helpers

    static List<String> wrap(String text, FontMetrics fm, int width) {
        List<String> out = new ArrayList<>();
        for (String para : text.split("\n", -1)) {
            String[] words = para.split(" ");
            StringBuilder cur = new StringBuilder();
            for (String word : words) {
                String cand = cur.length() == 0 ? word : cur + " " + word;
                if (fm.stringWidth(cand) <= width) {
                    cur = new StringBuilder(cand);
                } else {
                    if (cur.length() > 0) out.add(cur.toString());
                    cur = new StringBuilder();
                    // hard-split a word longer than the line
                    String w = word;
                    while (fm.stringWidth(w) > width && w.length() > 1) {
                        int n = w.length();
                        while (n > 1 && fm.stringWidth(w.substring(0, n)) > width) n--;
                        out.add(w.substring(0, n));
                        w = w.substring(n);
                    }
                    cur.append(w);
                }
            }
            out.add(cur.toString());
        }
        return out;
    }

    private Graphics2D measureGraphics() {
        BufferedImage bi = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bi.createGraphics();
        hints(g);
        return g;
    }

    private static void hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

    /** Total content height in dots. */
    public int heightDots() {
        if (totalHeight < 0) {
            Graphics2D g = measureGraphics();
            int y = 0;
            for (Row r : rows) y = r.layout(g, paper.dots, y, false);
            totalHeight = y;
            g.dispose();
        }
        return totalHeight;
    }

    public int widthDots() { return paper.dots; }

    /** Renders the whole document (one tall image) for on-screen preview. */
    public BufferedImage toImage(int margin) {
        int h = heightDots() + margin * 2;
        BufferedImage img = new BufferedImage(paper.dots + margin * 2, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        hints(g);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.setColor(Color.BLACK);
        g.translate(margin, margin);
        int y = 0;
        for (Row r : rows) y = r.layout(g, paper.dots, y, true);
        g.dispose();
        return img;
    }

    // ------------------------------------------------------------------ printing

    /** Page format sized to this document (roll: one page as tall as the content). */
    public PageFormat pageFormat(PageFormat base) {
        double scale = 72.0 / DPI;
        java.awt.print.Paper p = new java.awt.print.Paper();
        double w = paper.dots * scale;
        if (paper.roll) {
            double h = heightDots() * scale + 24;
            p.setSize(w + 8, h);
            p.setImageableArea(4, 6, w, h - 12);
        } else {
            p.setSize(595, 842);               // A4 in points
            p.setImageableArea(28, 28, 595 - 56, 842 - 56);
        }
        base.setPaper(p);
        return base;
    }

    /** Row index ranges per printed page. */
    private List<int[]> pages(double pageHeightPts) {
        List<int[]> pages = new ArrayList<>();
        if (paper.roll) {
            pages.add(new int[]{0, rows.size()});
            return pages;
        }
        double scale = (pageHeightPts) / (72.0 / DPI);   // available height in dots
        Graphics2D g = measureGraphics();
        int start = 0, y = 0;
        for (int i = 0; i < rows.size(); i++) {
            int ny = rows.get(i).layout(g, paper.dots, y, false);
            if (ny > scale && i > start) {
                pages.add(new int[]{start, i});
                start = i;
                y = rows.get(i).layout(g, paper.dots, 0, false);
            } else {
                y = ny;
            }
        }
        pages.add(new int[]{start, rows.size()});
        g.dispose();
        return pages;
    }

    @Override
    public int print(Graphics graphics, PageFormat pf, int pageIndex) throws PrinterException {
        List<int[]> pages = pages(pf.getImageableHeight());
        if (pageIndex >= pages.size()) return NO_SUCH_PAGE;
        Graphics2D g = (Graphics2D) graphics;
        hints(g);
        g.translate(pf.getImageableX(), pf.getImageableY());
        double scale = 72.0 / DPI;
        if (!paper.roll) {
            // fit the 1500-dot content to the printable width
            scale = Math.min(scale, pf.getImageableWidth() / paper.dots);
        }
        g.scale(scale, scale);
        g.setColor(Color.BLACK);
        int y = 0;
        int[] range = pages.get(pageIndex);
        for (int i = range[0]; i < range[1]; i++) y = rows.get(i).layout(g, paper.dots, y, true);
        return PAGE_EXISTS;
    }
}
