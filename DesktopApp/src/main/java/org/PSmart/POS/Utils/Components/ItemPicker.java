package org.PSmart.POS.Utils.Components;

import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.Item;
import org.PSmart.POS.Utils.ImageResultRunnable;
import org.PSmart.POS.Utils.MyUtils;
import org.PSmart.POS.Utils.StockService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * One item picker for the whole app (replaces the old grid / list pop-ups).
 * Search box + category chips + card grid or list, stock badges, tap a card to add one,
 * +/- steppers appear on picked items only, and a footer that shows what is picked.
 * Enter with nothing picked adds the top match and finishes (fast keyboard flow).
 */
public final class ItemPicker extends JFrame {

    private static final Color BLUE = new Color(25, 118, 211);
    private static final Color NAVY = new Color(0, 0, 137);
    private static final Color SLATE = new Color(52, 58, 64);
    private static final Color MUTED = new Color(108, 117, 125);
    private static final Color BORDER = new Color(222, 226, 230);
    private static final Color PAGE = new Color(245, 245, 250);
    private static final Color CHIP_OFF = new Color(233, 236, 239);
    private static final Color OK = new Color(30, 110, 50);
    private static final Color WARN = new Color(214, 137, 16);
    private static final Color BAD = new Color(200, 30, 45);

    /** A picked item with its quantity. */
    public static final class Pick {
        public final Item item;
        public final double quantity;
        Pick(Item item, double quantity) { this.item = item; this.quantity = quantity; }
    }

    private final Consumer<List<Pick>> onSubmit;
    private final String submitLabel;
    private boolean listMode;

    private List<Item> items = new ArrayList<>();
    private Map<UUID, Double> onHand = new HashMap<>();
    private final Map<UUID, Double> qty = new LinkedHashMap<>();
    private final Map<UUID, BufferedImage> images = new HashMap<>();
    private final Set<UUID> imageAsked = new HashSet<>();
    private String category = null;

    private final JTextField search = new JTextField();
    private final JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
    private final CardArea area = new CardArea();
    private final JLabel summary = new JLabel(" ");
    private final JLabel status = new JLabel("Loading items...");
    private JButton gridBtn, listBtn, submitBtn;
    private final List<Item> shown = new ArrayList<>();

    public ItemPicker(String title, String submitLabel, boolean listMode, Consumer<List<Pick>> onSubmit) {
        super(title);
        this.submitLabel = submitLabel;
        this.listMode = listMode;
        this.onSubmit = onSubmit;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(PAGE);
        setLayout(new BorderLayout());
        Dimension s = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.max(820, (int) (s.width * 0.72)), Math.max(560, (int) (s.height * 0.8)));
        setLocationRelativeTo(null);

        add(buildTop(), BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(area, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(24);
        sp.getViewport().setBackground(PAGE);
        add(sp, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        setVisible(true);
        search.requestFocusInWindow();
        load();
    }

    // ------------------------------------------------------------------ chrome

    private JComponent buildTop() {
        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setBackground(Color.WHITE);
        top.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER), new EmptyBorder(12, 16, 8, 16)));

        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        search.setFont(new Font("Courier", Font.PLAIN, 18));
        search.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true), new EmptyBorder(8, 12, 8, 12)));
        search.setToolTipText("Type a name, barcode, category or supplier");
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refilter(); }
        });
        search.addActionListener(e -> enterPressed());
        row.add(label("Search", 15, true, NAVY), BorderLayout.WEST);
        row.add(search, BorderLayout.CENTER);

        JPanel toggles = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        toggles.setOpaque(false);
        gridBtn = chip("Grid", !listMode);
        listBtn = chip("List", listMode);
        gridBtn.addActionListener(e -> setMode(false));
        listBtn.addActionListener(e -> setMode(true));
        toggles.add(gridBtn);
        toggles.add(listBtn);
        row.add(toggles, BorderLayout.EAST);
        top.add(row, BorderLayout.NORTH);

        chips.setOpaque(false);
        JScrollPane cs = new JScrollPane(chips, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        cs.setBorder(null);
        cs.setOpaque(false);
        cs.getViewport().setOpaque(false);
        cs.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 8));
        cs.setPreferredSize(new Dimension(10, 48));
        top.add(cs, BorderLayout.CENTER);
        return top;
    }

    private JComponent buildFooter() {
        JPanel f = new JPanel(new BorderLayout(10, 0));
        f.setBackground(Color.WHITE);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER), new EmptyBorder(10, 16, 10, 16)));
        JPanel left = new JPanel(new GridLayout(2, 1));
        left.setOpaque(false);
        summary.setFont(new Font("Courier", Font.BOLD, 16));
        summary.setForeground(NAVY);
        status.setFont(new Font("Courier", Font.PLAIN, 13));
        status.setForeground(MUTED);
        left.add(summary);
        left.add(status);
        f.add(left, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JButton clear = button("Clear", SLATE, 110);
        clear.addActionListener(e -> { qty.clear(); area.repaint(); updateSummary(); });
        JButton cancel = button("Cancel", SLATE, 120);
        cancel.addActionListener(e -> dispose());
        submitBtn = button(submitLabel, BLUE, 220);
        submitBtn.addActionListener(e -> submit());
        right.add(clear);
        right.add(cancel);
        right.add(submitBtn);
        f.add(right, BorderLayout.EAST);
        updateSummary();
        return f;
    }

    private static JLabel label(String t, int size, boolean bold, Color c) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Courier", bold ? Font.BOLD : Font.PLAIN, size));
        l.setForeground(c);
        return l;
    }

    private static JButton button(String text, Color bg, int w) {
        JButton b = MyUtils.createButton(text, bg);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Courier", Font.BOLD, 15));
        b.setPreferredSize(new Dimension(w, 40));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton chip(String text, boolean on) {
        JButton b = new JButton(text);
        b.setFont(new Font("Courier", Font.BOLD, 14));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setMargin(new Insets(6, 14, 6, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        styleChip(b, on);
        return b;
    }

    private static void styleChip(JButton b, boolean on) {
        b.setBackground(on ? NAVY : CHIP_OFF);
        b.setForeground(on ? Color.WHITE : SLATE);
    }

    // ------------------------------------------------------------------ data

    private void load() {
        new SwingWorker<Object[], Void>() {
            @Override protected Object[] doInBackground() {
                List<Item> list;
                Map<UUID, Double> stock = new HashMap<>();
                jakarta.persistence.EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
                try {
                    list = em.createQuery("FROM Item i WHERE i.deleted=false ORDER BY i.name", Item.class).getResultList();
                    stock = StockService.onHandAll(em);
                } finally { em.close(); }
                return new Object[]{list, stock};
            }
            @Override @SuppressWarnings("unchecked") protected void done() {
                try {
                    Object[] r = get();
                    items = (List<Item>) r[0];
                    onHand = (Map<UUID, Double>) r[1];
                    buildChips();
                    refilter();
                } catch (Exception ex) {
                    status.setText("Could not load items: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void buildChips() {
        chips.removeAll();
        TreeSet<String> cats = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Item i : items) if (i.getCategory() != null && i.getCategory().getName() != null) cats.add(i.getCategory().getName());
        addChip("All", null);
        for (String c : cats) addChip(c, c);
        chips.revalidate();
        chips.repaint();
    }

    private void addChip(String text, String cat) {
        JButton b = chip(text, Objects.equals(cat, category));
        b.putClientProperty("cat", cat);
        b.addActionListener(e -> {
            category = cat;
            for (Component c : chips.getComponents())
                styleChip((JButton) c, Objects.equals(((JButton) c).getClientProperty("cat"), cat));
            refilter();
        });
        chips.add(b);
    }

    private void refilter() {
        String q = search.getText().trim().toLowerCase();
        shown.clear();
        for (Item i : items) {
            if (category != null && (i.getCategory() == null || !category.equals(i.getCategory().getName()))) continue;
            if (!q.isEmpty() && !i.getSearchString().contains(q)) continue;
            shown.add(i);
        }
        area.rebuild();
        status.setText(shown.size() + " of " + items.size() + " items"
                + (category != null ? " in " + category : "") + "  |  click to add, Esc to close");
    }

    private void setMode(boolean list) {
        listMode = list;
        styleChip(gridBtn, !list);
        styleChip(listBtn, list);
        area.rebuild();
    }

    private void updateSummary() {
        double units = 0, value = 0;
        int lines = 0;
        for (Map.Entry<UUID, Double> e : qty.entrySet()) {
            if (e.getValue() <= 0) continue;
            lines++;
            units += e.getValue();
            for (Item i : items) if (i.getUid().equals(e.getKey())) {
                value += e.getValue() * (i.getSellingPrice() == null ? 0 : i.getSellingPrice());
                break;
            }
        }
        summary.setText(lines == 0 ? "Nothing picked yet"
                : lines + (lines == 1 ? " item" : " items") + " picked  -  " + fmt(units) + " units  -  KES "
                + MyUtils.formatCurrency(String.valueOf(value)));
        submitBtn.setEnabled(lines > 0);
    }

    private void change(Item it, double delta) {
        double v = Math.max(0, qty.getOrDefault(it.getUid(), 0.0) + delta);
        if (v == 0) qty.remove(it.getUid()); else qty.put(it.getUid(), v);
        area.repaint();
        updateSummary();
    }

    private void enterPressed() {
        if (qty.isEmpty() && !shown.isEmpty()) change(shown.get(0), 1);
        submit();
    }

    private void submit() {
        List<Pick> out = new ArrayList<>();
        for (Map.Entry<UUID, Double> e : qty.entrySet()) {
            if (e.getValue() <= 0) continue;
            for (Item i : items) if (i.getUid().equals(e.getKey())) { out.add(new Pick(i, e.getValue())); break; }
        }
        if (out.isEmpty()) return;
        dispose();
        onSubmit.accept(out);
    }

    private static String fmt(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.format("%.2f", d);
    }

    private void askImage(Item it) {
        if (!imageAsked.add(it.getUid())) return;
        it.getImage(new ImageResultRunnable() {
            @Override public void run() {
                BufferedImage src = getBufferedImage();
                if (src == null) return;
                int h = 72, w = Math.max(1, src.getWidth() * h / Math.max(1, src.getHeight()));
                BufferedImage th = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = th.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(src, 0, 0, w, h, null);
                g.dispose();
                SwingUtilities.invokeLater(() -> { images.put(it.getUid(), th); area.repaint(); });
            }
        });
    }

    // ------------------------------------------------------------------ cards

    private final class CardArea extends JPanel implements Scrollable {
        private final List<Card> cards = new ArrayList<>();

        CardArea() { setLayout(null); setBackground(PAGE); }

        void rebuild() {
            removeAll();
            cards.clear();
            for (Item i : shown) {
                Card c = new Card(i);
                cards.add(c);
                add(c);
            }
            doLayout();
            revalidate();
            repaint();
        }

        @Override public void doLayout() {
            int w = Math.max(300, getParent() == null ? getWidth() : getParent().getWidth());
            int pad = 16, gap = 12;
            if (listMode) {
                int y = pad;
                for (Card c : cards) { c.setBounds(pad, y, w - 2 * pad, 64); y += 64 + 8; }
                setPreferredSize(new Dimension(w, y + pad));
            } else {
                int cw = 190, ch = 184;
                int cols = Math.max(1, (w - pad + gap) / (cw + gap));
                int extra = (w - pad * 2 - cols * cw - (cols - 1) * gap) / cols; // spread leftover width
                int rw = cw + Math.max(0, extra);
                int x = pad, y = pad, col = 0;
                for (Card c : cards) {
                    c.setBounds(x, y, rw, ch);
                    if (++col == cols) { col = 0; x = pad; y += ch + gap; } else x += rw + gap;
                }
                int rows = (cards.size() + cols - 1) / cols;
                setPreferredSize(new Dimension(w, pad * 2 + rows * (ch + gap)));
            }
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (cards.isEmpty()) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(MUTED);
                g2.setFont(new Font("Courier", Font.BOLD, 18));
                String m = items.isEmpty() ? "No items yet" : "No items match your search";
                g2.drawString(m, (getWidth() - g2.getFontMetrics().stringWidth(m)) / 2, 90);
            }
        }

        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height - 40; }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private final class Card extends JComponent {
        private final Item item;
        private boolean hover;

        Card(Item item) {
            this.item = item;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) {
                    double q = qty.getOrDefault(item.getUid(), 0.0);
                    if (q > 0 && minusZone().contains(e.getPoint())) change(item, -1);
                    else change(item, 1);
                }
            });
        }

        private Rectangle stepper() {
            return listMode ? new Rectangle(getWidth() - 150, 14, 138, 36) : new Rectangle(8, getHeight() - 40, getWidth() - 16, 32);
        }
        private Rectangle minusZone() { Rectangle r = stepper(); return new Rectangle(r.x, r.y, 44, r.height); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            askImage(item);
            double q = qty.getOrDefault(item.getUid(), 0.0);
            boolean picked = q > 0;
            int w = getWidth(), h = getHeight();

            g.setColor(picked ? new Color(232, 241, 252) : Color.WHITE);
            g.fill(new RoundRectangle2D.Double(0, 0, w - 1, h - 1, 14, 14));
            g.setColor(picked ? BLUE : hover ? new Color(160, 170, 182) : BORDER);
            g.setStroke(new BasicStroke(picked ? 2f : 1f));
            g.draw(new RoundRectangle2D.Double(1, 1, w - 3, h - 3, 14, 14));

            double have = onHand.getOrDefault(item.getUid(), 0.0);
            double reorder = item.getReorderLevel() == null ? 0 : item.getReorderLevel();
            String badge; Color bc;
            if (have <= 0) { badge = "Out of stock"; bc = BAD; }
            else if (reorder > 0 && have <= reorder) { badge = "Low: " + fmt(have); bc = WARN; }
            else { badge = fmt(have) + " in stock"; bc = OK; }
            String price = "KES " + MyUtils.formatCurrency(String.valueOf(item.getSellingPrice() == null ? 0 : item.getSellingPrice()));
            String sub = item.getBarcode() != null && !item.getBarcode().isBlank() ? item.getBarcode()
                    : item.getCategory() != null ? item.getCategory().getName() : "";

            Font bold = new Font("Courier", Font.BOLD, listMode ? 16 : 14);
            Font plain = new Font("Courier", Font.PLAIN, 12);
            if (listMode) {
                drawThumb(g, 10, 10, 44, 44);
                g.setFont(bold); g.setColor(SLATE);
                g.drawString(ellipsize(g, item.getName(), w - 54 - 12 - 330), 66, 28);
                g.setFont(plain); g.setColor(MUTED);
                g.drawString(ellipsize(g, sub == null ? "" : sub, 260), 66, 48);
                g.setFont(bold); g.setColor(BLUE);
                g.drawString(price, w - 150 - 12 - 120 - 130 + 40, 28);
                drawBadge(g, badge, bc, w - 150 - 12 - 120 - 130 + 40, 36);
            } else {
                drawThumb(g, (w - 76) / 2, 8, 76, 60);
                g.setFont(bold); g.setColor(SLATE);
                g.drawString(ellipsize(g, item.getName(), w - 20), 10, 86);
                g.setFont(bold); g.setColor(BLUE);
                g.drawString(price, 10, 104);
                drawBadge(g, badge, bc, 10, 112);
            }

            if (picked) {
                Rectangle r = stepper();
                g.setColor(BLUE);
                g.fill(new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 10, 10));
                g.setColor(Color.WHITE);
                g.setFont(new Font("Courier", Font.BOLD, 20));
                FontMetrics fm = g.getFontMetrics();
                int ty = r.y + (r.height + fm.getAscent()) / 2 - 3;
                g.drawString("-", r.x + 16, ty);
                g.drawString("+", r.x + r.width - 26, ty);
                String qs = fmt(q);
                g.drawString(qs, r.x + (r.width - fm.stringWidth(qs)) / 2, ty);
            }
            g.dispose();
        }

        private void drawBadge(Graphics2D g, String text, Color c, int x, int y) {
            g.setFont(new Font("Courier", Font.BOLD, 11));
            FontMetrics fm = g.getFontMetrics();
            int bw = fm.stringWidth(text) + 14;
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 28));
            g.fill(new RoundRectangle2D.Double(x, y, bw, 18, 18, 18));
            g.setColor(c);
            g.drawString(text, x + 7, y + 13);
        }

        private void drawThumb(Graphics2D g, int x, int y, int w, int h) {
            BufferedImage img = images.get(item.getUid());
            if (img != null) {
                double s = Math.min((double) w / img.getWidth(), (double) h / img.getHeight());
                int iw = (int) (img.getWidth() * s), ih = (int) (img.getHeight() * s);
                g.drawImage(img, x + (w - iw) / 2, y + (h - ih) / 2, iw, ih, null);
                return;
            }
            int d = Math.min(w, h);
            g.setColor(new Color(225, 232, 244));
            g.fillOval(x + (w - d) / 2, y + (h - d) / 2, d, d);
            g.setColor(NAVY);
            g.setFont(new Font("Courier", Font.BOLD, d / 2));
            String l = item.getName() == null || item.getName().isBlank() ? "?" : item.getName().substring(0, 1).toUpperCase();
            FontMetrics fm = g.getFontMetrics();
            g.drawString(l, x + (w - fm.stringWidth(l)) / 2, y + (h + fm.getAscent()) / 2 - fm.getDescent() / 2 - 1);
        }

        private String ellipsize(Graphics2D g, String s, int max) {
            if (s == null) return "";
            FontMetrics fm = g.getFontMetrics();
            if (fm.stringWidth(s) <= max) return s;
            while (s.length() > 1 && fm.stringWidth(s + "...") > max) s = s.substring(0, s.length() - 1);
            return s + "...";
        }
    }
}
